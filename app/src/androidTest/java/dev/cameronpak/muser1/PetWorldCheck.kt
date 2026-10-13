package dev.cameronpak.muser1

import android.app.*
import android.content.Intent
import android.graphics.*
import android.os.*
import android.view.*
import android.widget.*
import java.io.File
import java.util.UUID

/** Fictional game dates, photos and replies on an unpaired emulator. Never uses live Muse. */
internal object PetWorldCheck {
    fun run(test:Instrumentation,result:Bundle) {
        val c=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish"));check(!File(c.noBackupFilesDir,"credentials.enc").exists())
        fun ui(block:()->Unit){var error:Throwable?=null;test.runOnMainSync {try{block()}catch(t:Throwable){error=t}};error?.let {throw it}}
        fun waitFor(description:String,predicate:()->Boolean){var ready=false;repeat(100){if(!ready){ui{ready=predicate()};if(!ready)Thread.sleep(70)}};if(!ready){test.uiAutomation.takeScreenshot()?.let {b->File(c.cacheDir,"world-failure.png").outputStream().use {b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}};check(ready){description+" pet="+(PetRoom.foreground?.isShowing==true)+" page="+(PetActivities.foreground?.focused==true)+" home="+(MainActivity.foreground?.hasWindowFocus()==true)}}
        fun descendants(view:View):List<View> = listOf(view)+if(view is ViewGroup)(0 until view.childCount).flatMap {descendants(view.getChildAt(it))}else emptyList()
        val now=System.currentTimeMillis();val pet=PetState(seed=991,hatched=true,ageMs=10*PetState.DAY,updatedAt=now)
        c.getSharedPreferences("pet_world",0).edit().clear().commit()
        c.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).commit()
        c.getSharedPreferences("reply_options",0).edit().putBoolean("quiet",true).commit()
        PetStore(c).save(pet);val store=PetWorldStore(c)
        check(store.plant(pet.seed,0,0,now-3*PetState.DAY));check(!store.plant(pet.seed,0,1,now))
        check(store.water(pet.seed,0,now-2*PetState.DAY));check(store.water(pet.seed,0,now-PetState.DAY))
        check(PetWorldStore(c).garden(pet.seed,now)[0]!!.blooming)
        check(store.collect(pet.seed,0,now)!=null);check(store.collect(pet.seed,0,now)==null);check(store.flowers(pet.seed).size==1)
        store.plant(pet.seed,0,0,now-3*PetState.DAY);store.water(pet.seed,0,now-2*PetState.DAY);store.water(pet.seed,0,now-PetState.DAY)
        store.plant(pet.seed,1,1,now-PetState.DAY);store.plant(pet.seed,2,2,now)
        for(id in 0..2){check(store.practice(pet,id,now-2*PetState.DAY)!=null);store.practice(pet,id,now-PetState.DAY);store.practice(pet,id,now)}
        check(PetWorldStore(c).skills(pet.seed).all {it.learned});check(!store.setPhrase(pet.seed,0,"dance"));check(store.setPhrase(pet.seed,0,"안녕 친구"))
        check(store.practice(pet.copy(hatched=false),0)==null)
        check(store.choose(pet.seed,0,now-2*PetState.DAY));check(store.choose(pet.seed,1,now-PetState.DAY));check(!store.choose(pet.seed,0,now-PetState.DAY))
        check(store.garden(992).all {it==null} && store.skills(992).none {it.learned} && store.story(992).isEmpty())
        val bitmap=Bitmap.createBitmap(256,192,Bitmap.Config.ARGB_8888)
        Canvas(bitmap).apply {drawColor(0xffe7efd8.toInt());val brush=Paint(Paint.ANTI_ALIAS_FLAG);brush.color=0xff66836b.toInt();drawCircle(128f,96f,60f,brush);brush.color=Color.WHITE;brush.strokeWidth=5f;drawLine(128f,96f,128f,52f,brush);drawLine(128f,96f,159f,110f,brush)}
        val bytes=java.io.ByteArrayOutputStream().also {bitmap.compress(Bitmap.CompressFormat.JPEG,75,it)}.toByteArray();bitmap.recycle()
        val id=UUID.randomUUID().toString();check(store.addPhoto(pet.seed,id,1,bytes,"Fictional round clock",now))
        check(store.addPhoto(pet.seed,id,1,bytes,"duplicate",now));check(store.photos(pet.seed).size==1)
        check(store.photoReply(pet.seed,id,"A round clock. Fictional fixture explanation."));check(!store.photoReply(pet.seed,id,"Duplicate reply"))
        val deleted=UUID.randomUUID().toString();store.addPhoto(pet.seed,deleted,0,bytes,"Deletion fixture");check(store.deletePhoto(pet.seed,deleted));check(!store.file(pet.seed,deleted).exists())
        repeat(24){check(store.addPhoto(993,UUID.randomUUID().toString(),1,bytes,"Capacity fixture"))};check(!store.addPhoto(993,UUID.randomUUID().toString(),1,bytes,"Overflow"))
        store.photos(993).forEach {store.deletePhoto(993,it.id)}
        c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor("Home did not resume"){MainActivity.foreground?.hasWindowFocus()==true}
        var home=MainActivity.foreground!!;var pages:PetActivities?=null
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply {isAccessible=true}
        fun dialog()=PetActivities::class.java.getDeclaredField("dialog").apply {isAccessible=true}.get(pages) as Dialog
        fun capture(name:String){Thread.sleep(200);ui {val view=dialog().window!!.decorView;check(view.width>0 && view.height>0)
            val image=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(image));File(c.cacheDir,"world-$name.png").outputStream().use {image.compress(Bitmap.CompressFormat.PNG,100,it)};image.recycle();check(view.width in 320..480 && view.height in 400..640){"$name dimensions ${view.width}x${view.height}; requested ${dialog().window!!.attributes.width}x${dialog().window!!.attributes.height}; display ${c.resources.displayMetrics.widthPixels}x${c.resources.displayMetrics.heightPixels}"}
            val header=descendants(view).filterIsInstance<Button>().first {it.text.toString() in listOf("닫기","閉じる","Close")};val rect=Rect();check(header.getGlobalVisibleRect(rect) && rect.top>=0 && rect.bottom<=640)
        }}
        try {
            for(language in DisplayLanguage.entries){
                ui {UiText.set(c,language);MainActivity::class.java.getDeclaredMethod("showPetRoom").apply {isAccessible=true}.invoke(home)}
                waitFor("Pet room not focused"){PetRoom.foreground?.hasWindowFocus()==true}
                val room=PetRoom.foreground!!
                ui {pages=PetActivities(home,room::dialogueState){};pages!!.show()};capture("menu-${language.name.lowercase()}")
                ui{pages!!.garden()};capture("garden-${language.name.lowercase()}")
                ui{pages!!.wheelGames()};capture("games-${language.name.lowercase()}")
                ui{descendants(dialog().window!!.decorView).filterIsInstance<Button>().single {it.text.toString() in listOf("연못 낚시","池で釣り","Pond fishing")}.performClick()}
                waitFor("Fishing page not focused"){pages!!.focused};capture("fishing-${language.name.lowercase()}")
                ui {
                    val down=SystemClock.uptimeMillis();check(pages!!.key(KeyEvent(down,down,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING,0)))
                    check(pages!!.key(KeyEvent(down+1,down+1,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP,0)))
                    check(pages!!.key(KeyEvent(down,down+2,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_PAIRING,0)))
                    check(!home.petInputBusy);pages!!.training()
                };capture("training-${language.name.lowercase()}")
                ui{pages!!.discoveries()};capture("album-${language.name.lowercase()}")
                ui{descendants(dialog().window!!.decorView).filterIsInstance<Button>().first {it.text.toString().contains(" · ")}.performClick()};capture("photo-${language.name.lowercase()}")
                ui{pages!!.story()};capture("story-${language.name.lowercase()}")
                ui{pages!!.close();room.dismiss()}
                waitFor("Home not focused after page checks"){MainActivity.foreground?.hasWindowFocus()==true}
            }
            // Real camera lifecycle, with no network upload. Cancel returns to the album.
            ui{home=MainActivity.foreground!!;MainActivity::class.java.getDeclaredMethod("showPetRoom").apply {isAccessible=true}.invoke(home);field("connected").set(home,true);home.petDiscoveryCamera(pet.seed,1)}
            waitFor("Album camera did not open"){CameraActivity.foreground?.hasWindowFocus()==true && PetRoom.foreground==null}
            ui{CameraActivity.foreground!!.finish()}
            waitFor("Cancel did not return to pet album"){PetActivities.foreground?.focused==true}
            ui{check(PetRoom.foreground!=null);PetActivities.foreground!!.close();PetRoom.foreground!!.dismiss()}
            waitFor("Home not focused after album cancel"){MainActivity.foreground?.hasWindowFocus()==true}
            ui{home=MainActivity.foreground!!;MainActivity::class.java.getDeclaredMethod("showPetRoom").apply {isAccessible=true}.invoke(home);field("connected").set(home,true);home.petDiscoveryCamera(pet.seed,1)}
            waitFor("Album camera did not reopen"){CameraActivity.foreground?.hasWindowFocus()==true && PetRoom.foreground==null}
            var request:PhotoQuestion.Request?=null
            ui {
                val origin=field("petCameraOrigin").get(home) as Bundle
                request=PhotoQuestion.Request(bytes,"Fictional discovery",petSeed=pet.seed,petDiscoveryToken=origin.getString("token"),petDiscoveryKind=1)
                PhotoQuestion.pending=request;CameraActivity.foreground!!.finish()
            }
            waitFor("Photo return did not restore pet dialogue"){PetRoom.foreground?.isShowing==true && MainActivity.foreground!=null}
            ui {
                home=MainActivity.foreground!!;check(field("petCameraOrigin").get(home)==null)
                val photo=request!!;store.addPhoto(pet.seed,photo.petDiscoveryToken!!,1,bytes,photo.question)
                val turn=ConversationTurn().also {it.user="Fictional discovery"}
                field("activeTurn").set(home,turn);field("discoveryConversation").set(home,turn);field("discoveryPhoto").set(home,photo)
                val receive=MainActivity::class.java.getDeclaredMethod("receiveReply",String::class.java,String::class.java,Boolean::class.javaPrimitiveType).apply {isAccessible=true}
                receive.invoke(home,"discovery","A clock with a round dial.",true)
                check(store.photos(pet.seed).last().reply=="A clock with a round dial.")
                MainActivity::class.java.getDeclaredMethod("preparePetDialogue").apply {isAccessible=true}.invoke(home)
                check(field("discoveryPhoto").get(home)==null);PhotoQuestion.pending=null
            }
            result.putString("stream","PASS: five local game features persist per companion; garden elapsed-time/care and one-time harvest, multi-day skills/custom cues, daily story choices, bounded photo store/delete and correlated reply, three-language pages within the OS viewport of a 480x640 emulator, wheel/side routing without microphone, and real camera album cancel/send returns. Synthetic photos/replies only; no live Muse requests.")
        } finally {ui{PhotoQuestion.pending=null;CameraActivity.foreground?.finish();pages?.close();PetActivities.foreground?.close();PetRoom.foreground?.dismiss()}}
    }
}
