package dev.cameronpak.muser1

import android.app.Activity
import android.os.Bundle
import android.view.View
import android.view.WindowManager

/** Muse-owned lock-screen host; shares every control with the conversation panel. */
class ClockActivity:Activity(){
    private lateinit var panel:ClockPanel
    override fun onCreate(state:Bundle?){
        super.onCreate(state);setShowWhenLocked(true);setTurnScreenOn(true)
        window.decorView.systemUiVisibility=View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        panel=ClockPanel(this){finish()};setContentView(panel.view)
    }
    override fun onResume(){super.onResume();panel.resume()}
    override fun onPause(){panel.pause();super.onPause()}
    override fun onRequestPermissionsResult(code:Int,permissions:Array<out String>,grants:IntArray){super.onRequestPermissionsResult(code,permissions,grants);panel.permissionResult(code,grants)}
}
