package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.media.AudioManager
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import java.io.File

/** Offline only: checks real dialog routing without credentials, a microphone, or a Muse request. */
internal object PetControlsCheck {
    fun run(test:Instrumentation,result:Bundle) {
        val ctx=test.targetContext
        check(Build.HARDWARE in listOf("ranchu","goldfish"))
        check(!File(ctx.noBackupFilesDir,"credentials.enc").exists())
        fun ui(block:()->Unit) { var error:Throwable?=null;test.runOnMainSync{try{block()}catch(e:Throwable){error=e}};error?.let{throw it} }
        fun nodes(v:View):List<View> = listOf(v)+if(v is ViewGroup)(0 until v.childCount).flatMap{nodes(v.getChildAt(it))} else emptyList()
        ctx.getSharedPreferences("pet_audio",0).edit().putString("mode","off").putBoolean("effects",false).commit()
        ctx.getSharedPreferences("reply_options",0).edit().putBoolean("quiet",false).commit()
        UiText.set(ctx,DisplayLanguage.KO)
        ctx.startActivity(Intent(ctx,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var home:MainActivity?=null
        repeat(80){if(home==null){ui{home=MainActivity.foreground?.takeIf{it.hasWindowFocus()}};if(home==null)Thread.sleep(100)}}
        val activity=checkNotNull(home)
        lateinit var room:PetRoom
        fun open(){
            ui{MainActivity::class.java.getDeclaredMethod("showPetRoom").apply{isAccessible=true}.invoke(activity);room=PetRoom.foreground!!}
            var focused=false
            repeat(80){if(!focused){ui{focused=room.hasWindowFocus()};if(!focused)Thread.sleep(50)}}
            check(focused){"Pet dialog did not obtain input focus"}
        }
        fun field(name:String)=PetRoom::class.java.getDeclaredField(name).apply{isAccessible=true}
        fun selected()=field("selected").getInt(room)
        fun key(start:Long,action:Int,code:Int,now:Long=SystemClock.uptimeMillis())=room.petKey(KeyEvent(start,now,action,code,0))
        fun capture(name:String){val v=room.window!!.decorView;val b=Bitmap.createBitmap(v.width,v.height,Bitmap.Config.ARGB_8888);v.draw(Canvas(b));File(ctx.cacheDir,"pet-controls-$name.png").outputStream().use{b.compress(Bitmap.CompressFormat.PNG,100,it)};b.recycle()}
        val audio=ctx.getSystemService(AudioManager::class.java)
        val originalVolume=audio.getStreamVolume(AudioManager.STREAM_MUSIC)
        try {
            open()
            ui {
                check(nodes(room.window!!.decorView).filterIsInstance<TextView>().any{it.text.toString()=="대화 보기"})
                fun mute()=nodes(room.window!!.decorView).single{it.contentDescription?.toString()?.startsWith("음성 ")==true}
                mute().performClick()
                check(activity.replyMuted && ctx.getSharedPreferences("reply_options",0).getBoolean("quiet",false))
                check(mute().contentDescription.toString().startsWith("음성 OFF"));capture("muted")
                mute().performClick();check(!activity.replyMuted);capture("voice-on")
                val middle=audio.getStreamMaxVolume(AudioManager.STREAM_MUSIC)/2
                audio.setStreamVolume(AudioManager.STREAM_MUSIC,middle,0)
                check(room.hasWindowFocus()) { "Pet dialog lacks input focus" }
                // AudioService applies the initial stream volume asynchronously on recent Android.
                repeat(20) { if(audio.getStreamVolume(AudioManager.STREAM_MUSIC)!=middle)Thread.sleep(25) }
                check(audio.getStreamVolume(AudioManager.STREAM_MUSIC)==middle) { "Initial volume not yet applied" }
                val before=selected();val down=SystemClock.uptimeMillis()
                key(down,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING)
                key(down,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_UP)
                check(audio.getStreamVolume(AudioManager.STREAM_MUSIC)==middle+1) { "Volume expected=${middle+1} actual=${audio.getStreamVolume(AudioManager.STREAM_MUSIC)} focus=${room.hasWindowFocus()} down=${(field("sideGesture").get(room) as SideButtonGesture).downTime} selected=${selected()} before=$before" }
                check(selected()==before)
                // A delayed hold callback and duplicate down must not start speech after a volume gesture.
                (field("voiceHold").get(room) as Runnable).run()
                key(down,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING)
                key(down,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_PAIRING)
                check(!field("held").getBoolean(room) && !field("voiceOwned").getBoolean(room))
                check(selected()==before)
                check((field("agentPane").get(room) as View).visibility!=View.VISIBLE)
                // Generic rotary events use the same path as DPAD wheel events.
                val next=down+1
                key(next,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING)
                val coords=MotionEvent.PointerCoords().apply{setAxisValue(MotionEvent.AXIS_SCROLL,-1f)}
                val properties=MotionEvent.PointerProperties().apply{id=0}
                val scroll=MotionEvent.obtain(next,next,MotionEvent.ACTION_SCROLL,1,arrayOf(properties),arrayOf(coords),0,0,1f,1f,0,0,android.view.InputDevice.SOURCE_ROTARY_ENCODER,0)
                room.dispatchGenericMotionEvent(scroll);scroll.recycle()
                key(next,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_PAIRING)
                check(audio.getStreamVolume(AudioManager.STREAM_MUSIC)==middle)
                // Focus cancellation and stale release cannot consume a later standalone wheel event.
                key(next+1,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING)
                room.cancelAgentInput()
                key(next+1,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_PAIRING)
                key(next+2,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN)
                check(selected()==(before+1)%4)
                check(audio.getStreamVolume(AudioManager.STREAM_MUSIC)==middle)
                // Even during a mini-game, volume must not change the selected tile or score.
                field("gameEnds").setLong(room,SystemClock.elapsedRealtime()+10000)
                val tile=field("selectedTile").getInt(room)
                key(next+3,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING)
                key(next+3,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_DPAD_DOWN)
                key(next+3,KeyEvent.ACTION_UP,KeyEvent.KEYCODE_PAIRING)
                check(audio.getStreamVolume(AudioManager.STREAM_MUSIC)==middle-1)
                check(field("selectedTile").getInt(room)==tile && field("catches").getInt(room)==0)
                field("gameEnds").setLong(room,0)
                capture("volume")
                activity.toggleReplyMute();room.dismiss()
            }
            open();ui{check(activity.replyMuted);check(nodes(room.window!!.decorView).any{it.contentDescription?.toString()?.startsWith("음성 OFF")==true});room.dismiss()}
            for(language in listOf(DisplayLanguage.JA,DisplayLanguage.EN)) {
                ui{UiText.set(ctx,language)};open();ui{capture(language.name.lowercase());room.dismiss()}
            }
            result.putString("stream","PASS: Pet mute shared and persisted, renamed chat button, side+DPAD and generic wheel volume, delayed hold/duplicate down suppressed, release and focus cancellation safe, normal selection retained, game volume leaves tile/score unchanged. No microphone or live Muse requests.")
        } finally {
            ui{PetRoom.foreground?.dismiss();audio.setStreamVolume(AudioManager.STREAM_MUSIC,originalVolume,0)}
        }
    }
}
