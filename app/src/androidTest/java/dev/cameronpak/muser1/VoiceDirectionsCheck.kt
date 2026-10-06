package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.graphics.Rect
import android.location.Location
import android.os.Bundle
import android.os.SystemClock
import android.widget.EditText
import android.view.WindowManager
import android.view.inputmethod.InputMethodManager

/** Opt-in synthetic transcript/UI check; does not record audio or submit a Muse turn. */
internal object VoiceDirectionsCheck {
    fun run(test:Instrumentation,result:Bundle){
        val command=checkNotNull(VoiceDirections.parse("東京駅まで車で案内して"))
        val app=test.startActivitySync(Intent(test.targetContext,NavigationActivity::class.java)
            .putExtra("destination_query",command.query).putExtra("travel_mode",command.mode.name)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) as NavigationActivity
        fun field(name:String)=NavigationActivity::class.java.getDeclaredField(name).apply{isAccessible=true}
        fun method(name:String,vararg types:Class<*>)=NavigationActivity::class.java.getDeclaredMethod(name,*types).apply{isAccessible=true}
        try {
            val input=field("destination").get(app) as EditText
            test.runOnMainSync {
                app.window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                check(input.text.toString()=="東京駅")
                check(field("mode").get(app)==TravelMode.DRIVE)
                method("stopLocation").invoke(app)
                method("accept",Location::class.java).invoke(app,Location("public-landmark-fixture").apply{
                    latitude=35.681236;longitude=139.767125;accuracy=5f;elapsedRealtimeNanos=SystemClock.elapsedRealtimeNanos();time=System.currentTimeMillis()
                })
            }
            val end=SystemClock.uptimeMillis()+30000
            var searched=false
            while(SystemClock.uptimeMillis()<end){
                test.runOnMainSync{searched=(field("searchCache").get(app) as Map<*,*>).isNotEmpty()}
                if(searched)break
                Thread.sleep(200)
            }
            check(searched){"Voice destination search did not return candidates"}
            test.uiAutomation.performGlobalAction(android.accessibilityservice.AccessibilityService.GLOBAL_ACTION_BACK)
            Thread.sleep(400)
            test.runOnMainSync{
                input.requestFocus()
                app.getSystemService(InputMethodManager::class.java).showSoftInput(input,InputMethodManager.SHOW_IMPLICIT)
            }
            Thread.sleep(1500)
            test.runOnMainSync {
                check(app.window.decorView.rootWindowInsets.isVisible(android.view.WindowInsets.Type.ime())){"Keyboard did not open"}
                val visible=Rect();check(input.getGlobalVisibleRect(visible))
                check(visible.height()>=input.height-2 && input.height>=48*app.resources.displayMetrics.density){"Destination field clipped"}
                check(app.window.attributes.softInputMode and WindowManager.LayoutParams.SOFT_INPUT_MASK_ADJUST==WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
            }
            result.putString("stream","PASS: synthetic Japanese voice command opened Drive destination search and returned candidates; destination field fully visible with the keyboard visible. No microphone or Muse message. Actual spoken recognition remains a user check.")
        }finally{test.runOnMainSync{app.finish()}}
    }
}
