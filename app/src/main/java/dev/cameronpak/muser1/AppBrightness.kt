package dev.cameronpak.muser1

import android.app.Activity
import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import java.util.WeakHashMap

/** Main-thread window registry also updates an already open settings/child dialog. */
internal object AppBrightness {
    private val windows=WeakHashMap<Window,Unit>()
    private fun settings(context:Context):ScreenBrightness {
        val prefs=context.getSharedPreferences("display_options",Context.MODE_PRIVATE)
        return ScreenBrightness({runCatching {if(prefs.contains("brightness"))prefs.getInt("brightness",-1)else null}.getOrNull()},
            {prefs.edit().putInt("brightness",it).apply()})
    }
    fun level(context:Context)=settings(context).level()
    fun set(context:Context,level:Int) {
        val chosen=settings(context).set(level)
        windows.keys.toList().forEach {setWindow(it,chosen)}
    }
    fun apply(context:Context,window:Window?) {
        if(window==null)return
        windows[window]=Unit
        setWindow(window,level(context))
    }
    private fun setWindow(window:Window,level:Int) {
        val attrs=window.attributes
        val value=ScreenBrightness.windowValue(level)
        if(attrs.screenBrightness!=value){attrs.screenBrightness=value;window.attributes=attrs}
    }
}

/** Apply before the first frame and again when returning from another activity. */
open class MuseActivity:Activity() {
    override fun onCreate(savedInstanceState:Bundle?) {super.onCreate(savedInstanceState);AppBrightness.apply(this,window)}
    override fun onResume(){super.onResume();AppBrightness.apply(this,window)}
}

open class MuseDialog(context:Context,theme:Int):Dialog(context,theme) {
    override fun onStart(){super.onStart();AppBrightness.apply(context,window)}
}

/** Builder.create is used by both create().show() and Builder.show(). */
internal class MuseAlertDialogBuilder(private val owner:Context):AlertDialog.Builder(owner) {
    override fun create():AlertDialog = super.create().also {AppBrightness.apply(owner,it.window)}
}
