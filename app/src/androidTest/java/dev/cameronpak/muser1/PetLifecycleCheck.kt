package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.View
import android.view.ViewGroup
import android.view.accessibility.AccessibilityNodeInfo
import android.widget.Button
import android.widget.TextView
import java.io.File

/** Uses synthetic clock/state only on a disposable, unpaired emulator. No microphone or Muse calls. */
internal object PetLifecycleCheck {
    fun run(test:Instrumentation,result:Bundle,demo:Boolean=false) {
        val ctx=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish")) { "Emulator only" }
        check(!File(ctx.noBackupFilesDir,"credentials.enc").exists()) { "Unpaired emulator only" }
        fun ui(block:()->Unit) { var error:Throwable?=null;test.runOnMainSync{try{block()}catch(e:Throwable){error=e}};error?.let{throw it} }
        fun status(s:String){test.sendStatus(0,Bundle().apply{putString("stream",s+"\n")})}
        fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{nodes(v.getChildAt(it))} else emptyList()
        val prefs=ctx.getSharedPreferences("muse_pet",0)
        prefs.edit().clear().putLong("updated",1L).putBoolean("hatched",true).putInt("xp",120).commit()
        val store=PetStore(ctx);val now=System.currentTimeMillis()
        val migrated=store.load(now)
        check(migrated.hatched && migrated.level==4 && !migrated.dead && migrated.updatedAt==now)
        check(ctx.getSharedPreferences("muse_pet_archive",0).all.isNotEmpty())
        val stateFixture=migrated.copy(health=23.4,warmth=12.0,dietMask=13,curiosity=12.0,vigor=3.0,dead=true,diedAt=now,form=PetForm.WILD)
        store.save(stateFixture);check(store.load(now)==stateFixture){"State round trip failed"}
        store.startNewEgg(now)
        ctx.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).commit()
        UiText.set(ctx,DisplayLanguage.KO)
        ctx.startActivity(Intent(ctx,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var home:MainActivity?=null
        repeat(100){if(home==null){ui{home=MainActivity.foreground?.takeIf{it.hasWindowFocus()}};if(home==null)Thread.sleep(100)}}
        val activity=checkNotNull(home)
        fun captureView(v:View,name:String){val bmp=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(bmp));File(ctx.cacheDir,"pet-$name.png").outputStream().use{bmp.compress(Bitmap.CompressFormat.PNG,100,it)};bmp.recycle()}
        ui{
            val root=activity.window.decorView
            val controls=listOf("카메라 열기","글로 대화","음성 ON: 눌러서 끄기","설정").map{desc->nodes(root).single{it.contentDescription?.toString()==desc}}
            val positions=controls.map{v->IntArray(2).also{v.getLocationOnScreen(it)}[0]}
            check(positions.zipWithNext().all{(a,b)->a<b}){"Bottom icon order incorrect"}
            captureView(root,"home-icons")
        }
        lateinit var room:PetRoom
        fun open(){ui{MainActivity::class.java.getDeclaredMethod("showPetRoom").apply{isAccessible=true}.invoke(activity);room=PetRoom.foreground!!};Thread.sleep(400)}
        fun state()=PetRoom::class.java.getDeclaredField("pet").apply{isAccessible=true}.get(room) as PetState
        fun button(name:String)=nodes(room.window!!.decorView).filterIsInstance<Button>().single{it.text.toString()==name}
        fun capture(name:String){ui{captureView(room.window!!.decorView,name)}}
        fun installState(s:PetState){ui{room.dismiss();store.save(s)};open()}
        fun call(name:String){ui{PetRoom::class.java.getDeclaredMethod(name).apply{isAccessible=true}.invoke(room)}}
        fun clickMenu(text:String){
            var found:AccessibilityNodeInfo?=null
            repeat(50){if(found==null){found=test.uiAutomation.rootInActiveWindow?.findAccessibilityNodeInfosByText(text)?.firstOrNull{it.text?.toString()==text};if(found==null)Thread.sleep(100)}}
            val node=found?:error("Missing menu $text")
            val bounds=android.graphics.Rect();node.getBoundsInScreen(bounds)
            check(!bounds.isEmpty){"Empty bounds for $text"}
            test.uiAutomation.takeScreenshot()?.let{b->File(ctx.cacheDir,"pet-menu.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
            status("Menu $text at $bounds in ${node.packageName}")
            val down=SystemClock.uptimeMillis()
            test.uiAutomation.injectInputEvent(android.view.MotionEvent.obtain(down,down,android.view.MotionEvent.ACTION_DOWN,bounds.exactCenterX(),bounds.exactCenterY(),0),true)
            test.uiAutomation.injectInputEvent(android.view.MotionEvent.obtain(down,down+60,android.view.MotionEvent.ACTION_UP,bounds.exactCenterX(),bounds.exactCenterY(),0),true)
            Thread.sleep(350)
        }
        open();ui{
            check(!state().hatched && button("온기").isEnabled)
            val text=nodes(room.window!!.decorView).filterIsInstance<TextView>().joinToString{"${it.text}"}
            check(!Regex("[0-9]|XP|→").containsMatchIn(text)){"Egg UI reveals progression"}
            button("온기").performClick()
        };capture("egg")
        ui{
            val scene=PetRoom::class.java.getDeclaredField("room").apply{isAccessible=true}.get(room) as PetScene
            fun frame(time:Long):IntArray {scene.previewTime=time;val b=Bitmap.createBitmap(scene.width,scene.height,Bitmap.Config.ARGB_8888);scene.draw(Canvas(b));return IntArray(b.width*b.height).also{b.getPixels(it,0,b.width,0,0,b.width,b.height);b.recycle()}}
            check(!frame(1000).contentEquals(frame(2200)));scene.previewTime=null
        }
        status("Migration, persistence, icon order, hidden egg progress and motion passed")
        val clock=System.currentTimeMillis()
        installState(PetState(updatedAt=clock,incubationMs=72*PetState.HOUR))
        Thread.sleep(3200);ui{check(state().hatched && state().level==1)};capture("baby")
        ui{button("먹이").performClick()};clickMenu("구운 생선")
        ui{check(state().dietMask and (1 shl PetFood.FISH.ordinal)!=0)}
        for(kind in PetGame.entries){
            val start=PetState(updatedAt=System.currentTimeMillis(),hatched=true,food=50.0)
            installState(start)
            ui{button("놀이").performClick()}
            clickMenu(when(kind){PetGame.STARS->"별잡기";PetGame.MEMORY->"반짝임 기억하기";PetGame.RHYTHM->"새와 리듬 맞추기"})
            capture("game-${kind.name.lowercase()}")
            repeat(if(kind==PetGame.MEMORY)3 else 5){n->
                ui{
                    val started=PetRoom::class.java.getDeclaredField("gameStarted").apply{isAccessible=true}
                    val selected=PetRoom::class.java.getDeclaredField("selectedTile").apply{isAccessible=true}
                    val tile=when(kind){
                        PetGame.STARS->PetRoom::class.java.getDeclaredField("star").apply{isAccessible=true}.getInt(room)
                        PetGame.MEMORY->{started.setLong(room,SystemClock.elapsedRealtime()-4000);(PetRoom::class.java.getDeclaredField("melody").apply{isAccessible=true}.get(room) as List<*>)[n] as Int}
                        PetGame.RHYTHM->{started.setLong(room,SystemClock.elapsedRealtime()-800);PetRoom::class.java.getDeclaredField("nextStar").apply{isAccessible=true}.setLong(room,0);1}
                    }
                    selected.setInt(room,tile)
                    PetRoom::class.java.getDeclaredMethod("catchStar").apply{isAccessible=true}.invoke(room)
                }
            }
            ui{check(state().playedAt>0);check(state().mood>80)}
        }
        status("Hatch animation with simulated age, meal menu and all three mini-games passed")
        for((days,label) in listOf(1 to "young",4 to "growing",9 to "adult",21 to "old-friend")) {
            installState(PetState(updatedAt=System.currentTimeMillis(),hatched=true,ageMs=days*PetState.DAY,form=PetForm.EXPLORER,curiosity=40.0))
            capture(label);if(demo)Thread.sleep(1000)
        }
        installState(PetState(updatedAt=System.currentTimeMillis(),hatched=true,health=20.0,food=5.0,warmth=10.0));capture("ill")
        ui{button("돌봄").performClick()};clickMenu("아플 때 약 주기");ui{check(state().medicineAt>0)}
        val dead=PetState(updatedAt=System.currentTimeMillis()-7*PetState.DAY,hatched=true).advance(System.currentTimeMillis())
        installState(dead);capture("memorial")
        ui{check(!button("먹이").isEnabled);check(button("말하기").isEnabled);check(button("글쓰기").isEnabled)}
        call("confirmNewEgg");clickMenu("새 알 맞이하기")
        ui{check(!state().hatched && !state().dead)};capture("new-egg")
        ui{room.dismiss()}
        result.putString("stream","PASS: Migration, full persistence, bottom icons, hidden progress, animated egg/hatch, six-food menu, three mini-games, five life stages, medicine, death with agent controls, archived new egg. Time was simulated; no microphone or live Muse turns.")
    }
}
