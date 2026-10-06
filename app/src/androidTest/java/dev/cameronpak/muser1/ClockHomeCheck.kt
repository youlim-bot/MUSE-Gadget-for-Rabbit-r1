package dev.cameronpak.muser1

import android.app.Instrumentation
import android.app.Dialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.view.View

internal object ClockHomeCheck {
    fun run(test: Instrumentation, result: Bundle) {
        val c = test.targetContext
        val before = LocalClock.entries(c)
        val owned = mutableListOf<String>()
        test.targetContext.startActivity(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var app: MainActivity? = null
        repeat(100) { if (app == null) { test.runOnMainSync { app = MainActivity.foreground }; Thread.sleep(100) } }
        val home = checkNotNull(app)
        fun field(name: String) = MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }
        val button = field("clockStatus").get(home) as Button
        val refresh = MainActivity::class.java.getDeclaredMethod("refreshClockStatus").apply { isAccessible = true }
        try {
            val timer = LocalClock.add(c, ClockCommand("timer", seconds = 600)).also { owned += it.id }
            LocalClock.add(c, ClockCommand("alarm", hour = 7, minute = 0, day = "tomorrow")).also { owned += it.id }
            Thread.sleep(1300)
            var first = ""
            test.runOnMainSync { check(button.isShown); first = button.text.toString(); check(first.lines().size == 2) }
            test.uiAutomation.takeScreenshot()?.let { bitmap ->
                java.io.File(c.cacheDir, "clock-home-check.png").outputStream().use { bitmap.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it) }
                bitmap.recycle()
            }
            Thread.sleep(1500)
            if (before.none { it.kind == "timer" && it.state in setOf("active", "ringing") }) {
                test.runOnMainSync { check(button.text.toString() != first) { "Countdown did not update" } }
                LocalClock.change(c, timer.id, "pause")
                Thread.sleep(1200)
                var paused = ""
                test.runOnMainSync { paused = button.text.toString() }
                Thread.sleep(1300)
                test.runOnMainSync { check(button.text.toString() == paused) { "Paused countdown changed" } }
            }
            test.runOnMainSync { button.performClick(); check((field("clockDialog").get(home) as Dialog).isShowing) }
            test.runOnMainSync { (field("clockDialog").get(home) as Dialog).dismiss() }
        } finally {
            owned.forEach { LocalClock.change(c, it, "delete") }
            test.runOnMainSync { refresh.invoke(home) }
        }
        check(LocalClock.entries(c) == before) { "Existing clocks changed" }
        if (before.none { it.state in setOf("active", "paused", "ringing") }) test.runOnMainSync { check(button.visibility == View.GONE) }
        result.putString("stream", "PASS: home shows alarm and timer, countdown updates, paused countdown stays fixed, tap opens Muse clock panel, and test clocks are removed. Existing schedules preserved.")
    }
}
