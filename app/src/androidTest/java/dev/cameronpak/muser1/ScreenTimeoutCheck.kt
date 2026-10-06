package dev.cameronpak.muser1

import android.app.AlertDialog
import android.app.Instrumentation
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.WindowManager

internal object ScreenTimeoutCheck {
    fun run(test: Instrumentation, result: Bundle) {
        fun ui(block: () -> Unit) {
            var error: Throwable? = null
            test.runOnMainSync { try { block() } catch (t: Throwable) { error = t } }
            error?.let { throw it }
        }
        val c = test.targetContext
        val prefs = c.getSharedPreferences("reading", Context.MODE_PRIVATE)
        val existed = prefs.contains("keep_screen_awake")
        val original = prefs.getBoolean("keep_screen_awake", false)
        c.startActivity(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var app: MainActivity? = null
        repeat(100) { if (app == null) { ui { app = MainActivity.foreground?.takeIf { it.hasWindowFocus() } }; Thread.sleep(100) } }
        val home = checkNotNull(app)
        fun invoke(name: String) = MainActivity::class.java.getDeclaredMethod(name).apply { isAccessible = true }.invoke(home)
        fun choose(index: Int) {
            invoke("showScreenTimeoutSettings")
            val dialog = MainActivity::class.java.getDeclaredField("controls").apply { isAccessible = true }.get(home) as AlertDialog
            dialog.listView.performItemClick(null, index, index.toLong())
        }
        try {
            ui {
                choose(1)
                check(prefs.getBoolean("keep_screen_awake", false))
                invoke("releaseScreenAwake") // The same path used after speech/recording completes.
                check(home.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON != 0)
            }
            val timeout = Settings.System.getInt(c.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT, 30000)
            check(timeout in 1000..45000) { "Timeout too long for bounded hardware check" }
            Thread.sleep(timeout.toLong() + 5000)
            check(c.getSystemService(PowerManager::class.java).isInteractive) { "Screen turned off" }
            ui {
                check(home.hasWindowFocus())
                choose(0)
                check(!prefs.getBoolean("keep_screen_awake", true))
                check(home.window.attributes.flags and WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON == 0)
            }
            result.putString("stream", "PASS: settings menu toggles screen timeout, keep-awake survives release after speech, device stayed interactive beyond its system timeout, and enabling timeout clears the flag. Original preference restored.")
        } finally {
            ui {
                prefs.edit().apply { if (existed) putBoolean("keep_screen_awake", original) else remove("keep_screen_awake") }.commit()
                invoke("releaseScreenAwake")
            }
        }
    }
}
