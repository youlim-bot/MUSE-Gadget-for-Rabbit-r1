package dev.cameronpak.muser1
import android.app.*
import android.content.Intent
import android.os.*
import android.widget.Button

internal object StopVoiceCheck {
    fun run(test:Instrumentation,result:Bundle){
        test.targetContext.startActivity(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        val launchDeadline=SystemClock.uptimeMillis()+10000
        var found:MainActivity?=null
        while(SystemClock.uptimeMillis()<launchDeadline){test.runOnMainSync{found=MainActivity.foreground};if(found!=null)break;Thread.sleep(100)}
        val app=checkNotNull(found){"Muse did not resume"}
        fun field(name:String)=MainActivity::class.java.getDeclaredField(name).apply{isAccessible=true}
        val speech=field("speech").get(app) as SpeechOutput
        val local=SpeechOutput::class.java.getDeclaredField("local").apply{isAccessible=true}.get(speech) as AndroidSpeechOutput
        val button=field("quickButton").get(app) as Button
        val quiet=field("quietMode").getBoolean(app)
        try{
            test.runOnMainSync{local.speak("음성 정지 버튼을 확인하는 테스트입니다. ".repeat(30))}
            val end=SystemClock.uptimeMillis()+8000
            var shown=false
            while(SystemClock.uptimeMillis()<end){test.runOnMainSync{shown=button.text.toString().contains("■")};if(shown)break;Thread.sleep(100)}
            check(shown){"Stop control not shown"}
            test.runOnMainSync{button.performClick();check(!speech.hasPlayback);check(field("replySpeechStopped").getBoolean(app));check(field("quietMode").getBoolean(app)==quiet);check(!button.text.toString().contains("■"))}
            result.putString("stream","PASS: queued/local TTS exposed the stop control; tapping cleared playback, suppressed the current reply and restored Quick actions without changing quiet mode. No Muse message or ElevenLabs call. Cloud playback cancellation relies on the existing SpeechOutput.stop path.")
        }finally{test.runOnMainSync{speech.stop()}}
    }
}
