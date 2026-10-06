package dev.cameronpak.muser1
import android.app.*
import android.content.Intent
import android.os.*
import android.widget.Button

internal object StopVoiceCheck {
    fun run(test:Instrumentation,result:Bundle){
        fun ui(block: () -> Unit) {
            var error: Throwable? = null
            test.runOnMainSync { try { block() } catch (t: Throwable) { error = t } }
            error?.let { throw it }
        }
        test.targetContext.startActivity(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val launchDeadline=SystemClock.uptimeMillis()+10000
        var found:MainActivity?=null
        while(SystemClock.uptimeMillis()<launchDeadline){ui{found=MainActivity.foreground};if(found!=null)break;Thread.sleep(100)}
        val app=checkNotNull(found){"Muse did not resume"}
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply{isAccessible=true}
        val speech=field("speech").get(app) as SpeechOutput
        val local=SpeechOutput::class.java.getDeclaredField("local").apply{isAccessible=true}.get(speech) as AndroidSpeechOutput
        val button=field("quickButton").get(app) as Button
        val quiet=field("quietMode").getBoolean(app)
        try{
            ui{local.speak("음성 정지 버튼을 확인하는 테스트입니다. ".repeat(30))}
            val end=SystemClock.uptimeMillis()+8000
            var shown=false
            while(SystemClock.uptimeMillis()<end){ui{shown=button.text.toString().contains("■")};if(shown)break;Thread.sleep(100)}
            check(shown){"Stop control not shown"}
            ui{button.performClick();check(!speech.hasPlayback){"Playback not stopped"};check(field("replySpeechStopped").getBoolean(app)){"Reply suppression missing"};check(field("quietMode").getBoolean(app)==quiet){"Quiet preference changed"};check(!button.text.toString().contains("■")){"Stop label remained"}}
            result.putString("stream","PASS: queued/local TTS exposed the stop control; tapping cleared playback, suppressed the current reply and restored Quick actions without changing quiet mode. No Muse message or ElevenLabs call. Cloud playback cancellation relies on the existing SpeechOutput.stop path.")
        }finally{ui{speech.stop()}}
    }
}
