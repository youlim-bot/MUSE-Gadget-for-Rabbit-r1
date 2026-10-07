package dev.cameronpak.muser1

import android.app.*
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Build
import android.os.Bundle
import android.view.View
import java.io.File

/** Exercises the real MainActivity stop -> CameraActivity -> result -> resume lifecycle. */
internal object PetCameraReturnCheck {
    fun run(test:Instrumentation,result:Bundle) {
        val c=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish"))
        check(!File(c.noBackupFilesDir,"credentials.enc").exists())
        fun ui(block:()->Unit){var failure:Throwable?=null;test.runOnMainSync { try{block()}catch(t:Throwable){failure=t} };failure?.let { throw it }}
        fun waitFor(description:String,predicate:()->Boolean){
            var ready=false;repeat(100){if(!ready){ui{ready=predicate()};if(!ready)Thread.sleep(70)}};check(ready){description}
        }
        val now=System.currentTimeMillis();val pet=PetState(seed=8877,hatched=false,updatedAt=now)
        PetStore(c).save(pet);val store=PetActivitiesStore(c);store.chooseMission(pet.seed,0)
        val token=store.missionToken(pet.seed)!!
        c.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).commit()
        c.getSharedPreferences("reply_options",0).edit().putBoolean("quiet",true).commit()
        UiText.set(c,DisplayLanguage.KO)
        c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        waitFor("Main did not resume"){MainActivity.foreground?.hasWindowFocus()==true}
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply { isAccessible=true }
        fun petField(name:String)=PetRoom::class.java.getDeclaredField(name).apply { isAccessible=true }
        fun open(home:MainActivity){ui { MainActivity::class.java.getDeclaredMethod("showPetRoom").apply{isAccessible=true}.invoke(home) };waitFor("Pet room not ready"){PetRoom.foreground?.hasWindowFocus()==true}}
        fun launch(home:MainActivity){
            ui { MainActivity::class.java.getDeclaredMethod("launchPetMissionCamera",Long::class.javaPrimitiveType,String::class.java,Int::class.javaPrimitiveType).apply{isAccessible=true}.invoke(home,pet.seed,token,0) }
            waitFor("Camera not foreground"){CameraActivity.foreground?.hasWindowFocus()==true && PetRoom.foreground==null}
        }
        fun capture(name:String){Thread.sleep(200);ui { val view=PetRoom.foreground!!.window!!.decorView;val b=Bitmap.createBitmap(view.width,view.height,Bitmap.Config.ARGB_8888);view.draw(Canvas(b));File(c.cacheDir,"pet-camera-$name.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle() }}
        try {
            var home=MainActivity.foreground!!;open(home);launch(home)
            ui { check(field("petRoom").get(home)==null);CameraActivity.foreground!!.finish() }
            waitFor("Cancel did not restore pet room"){MainActivity.foreground!=null && PetRoom.foreground?.isShowing==true}
            ui {
                val room=PetRoom.foreground!!
                val controller=petField("activities\$delegate").get(room) as Lazy<*>
                val dialog=PetActivities::class.java.getDeclaredField("dialog").apply { isAccessible=true }.get(controller.value) as Dialog
                check(dialog.isShowing){"Cancel did not restore mission page"}
                check(PhotoQuestion.pending==null);check(store.missionToken(pet.seed)==token)
                (controller.value as PetActivities).close();room.dismiss()
            }
            waitFor("Home not focused after cancel check"){MainActivity.foreground?.hasWindowFocus()==true}
            home=MainActivity.foreground!!;open(home);launch(home)
            // Camera's reviewed-send handoff, with fictional bytes and no connection/network request.
            ui { PhotoQuestion.pending=PhotoQuestion.Request(byteArrayOf(1,2,3),"Find something green",petSeed=pet.seed,petMissionToken=token,petMissionTarget=0);CameraActivity.foreground!!.finish() }
            waitFor("Photo did not restore pet dialogue"){MainActivity.foreground!=null && PetRoom.foreground?.let { (petField("agentPane").get(it) as View).visibility==View.VISIBLE }==true}
            ui {
                home=MainActivity.foreground!!;check(PhotoQuestion.pending?.petMissionToken==token)
                check(field("petCameraOrigin").get(home)==null)
                check(field("petRoom").get(home)===PetRoom.foreground)
                check(PetRoom.foreground!!.dialogueState().seed==pet.seed)
                // Deliver a fixture result after returning; it must remain in the pet dialogue.
                val turn=ConversationTurn().also{it.user="Fictional camera mission"}
                field("activeTurn").set(home,turn);field("missionPhoto").set(home,PhotoQuestion.pending);field("missionConversation").set(home,turn)
                MainActivity::class.java.getDeclaredMethod("receiveReply",String::class.java,String::class.java,Boolean::class.javaPrimitiveType).apply{isAccessible=true}
                    .invoke(home,"photo-return","{\"matched\":false,\"reply\":\"초록색 물건이 잘 보이지 않아. 다시 찾아볼까?\"}",true)
                check((petField("agentText").get(PetRoom.foreground) as android.widget.TextView).text.contains("초록색"))
                PhotoQuestion.pending=null
            }
            capture("reply")
            ui { PetRoom.foreground!!.dismiss() }
            waitFor("Home not ready for recreation check"){MainActivity.foreground?.hasWindowFocus()==true}
            home=MainActivity.foreground!!;open(home);launch(home)
            val old=home
            ui { old.recreate() }
            waitFor("Main activity did not recreate while camera open"){old.isDestroyed}
            ui { CameraActivity.foreground!!.finish() }
            waitFor("Recreated main did not restore pet room"){MainActivity.foreground?.let {it!==old}==true && PetRoom.foreground?.isShowing==true}
            ui { check(PetRoom.foreground!!.dialogueState().seed==pet.seed);PetRoom.foreground!!.dismiss() }
            waitFor("Home not ready for ordinary camera check"){MainActivity.foreground?.hasWindowFocus()==true}
            ui { MainActivity.foreground!!.startActivity(Intent(c,CameraActivity::class.java)) }
            waitFor("Ordinary camera not foreground"){CameraActivity.foreground?.hasWindowFocus()==true}
            ui { CameraActivity.foreground!!.finish() }
            waitFor("Ordinary camera did not return home"){MainActivity.foreground?.hasWindowFocus()==true}
            ui { check(PetRoom.foreground==null);check(PhotoQuestion.pending==null) }
            result.putString("stream","PASS: real camera activity round trip restores pet dialogue for a confirmed photo, mission page on cancel, saved origin after parent activity recreation, and normal Home for ordinary camera. Fixture result stays in pet mode. No live photo or microphone upload.")
        } finally { ui { PhotoQuestion.pending=null;CameraActivity.foreground?.finish();PetRoom.foreground?.dismiss() } }
    }
}
