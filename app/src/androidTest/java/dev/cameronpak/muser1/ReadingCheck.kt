package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.KeyEvent
import android.widget.ScrollView

internal object ReadingCheck {
    fun run(test: Instrumentation, result: Bundle) {
        fun ui(block: () -> Unit) {
            var failure: Throwable? = null
            test.runOnMainSync { try { block() } catch (t: Throwable) { failure = t } }
            failure?.let { throw it }
        }
        val c = test.targetContext
        c.startActivity(Intent(c, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var found: MainActivity? = null
        repeat(100) { if (found == null) { ui { found = MainActivity.foreground }; Thread.sleep(100) } }
        val app = checkNotNull(found)
        fun field(name: String) = MainActivity::class.java.getDeclaredField(name).apply { isAccessible = true }
        repeat(50) {
            var loaded = false
            ui { loaded = field("historyLoaded").getBoolean(app) }
            if (!loaded) Thread.sleep(100)
        }
        ui { MainActivity::class.java.getDeclaredMethod("disconnect").apply { isAccessible = true }.invoke(app) }
        val screen = field("screen").get(app) as MuseScreen
        val speech = field("speech").get(app) as SpeechOutput
        val scroll = MuseScreen::class.java.getDeclaredField("scroll").apply { isAccessible = true }.get(screen) as ScrollView
        val fixture = (1..8).joinToString("\n\n") { "${it}번째 문장입니다. 뮤즈가 읽는 위치에 맞추어 대화 화면이 이동합니다." }
        val originalCallback = speech.onReading
        var furthest = 0
        try {
            ui {
                speech.stop()
                screen.showConversation("스크롤 테스트", fixture)
                app.window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
            Thread.sleep(500)
            ui {
                repeat(80) {
                    check(app.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_UP)))
                    check(app.dispatchKeyEvent(KeyEvent(KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_UP)))
                }
                check(scroll.scrollY == 0)
                check(app.currentFocus == null || app.currentFocus === screen.message || app.currentFocus === scroll) { "Wheel focused a menu" }
                screen.followSpeech(fixture, fixture.length - 20)
            }
            Thread.sleep(300)
            var autoPosition = 0
            ui {
                autoPosition = scroll.scrollY
                check(autoPosition > 0) { "No scroll: match=${screen.message.text.contains(fixture)}, height=${screen.message.height}, viewport=${scroll.height}" }
                screen.scrollConversation(-1, true)
                autoPosition = scroll.scrollY
                screen.followSpeech(fixture, fixture.length - 5)
            }
            Thread.sleep(300)
            ui {
                check(scroll.scrollY == autoPosition) { "Manual browsing was overridden" }
                screen.jumpToLatest()
                check(scroll.scrollY >= autoPosition)
                screen.endSpeechFollow()
                speech.onReading = { text, offset -> furthest = maxOf(furthest, offset); originalCallback(text, offset) }
                speech.speak(fixture)
            }
            val deadline = SystemClock.uptimeMillis() + 110000
            var moved = false
            while (SystemClock.uptimeMillis() < deadline && !moved) {
                Thread.sleep(250)
                ui { moved = furthest > 100 && scroll.scrollY > 0 }
            }
            check(moved) { "Live playback did not provide advancing text positions" }
            result.putString("stream", "PASS: wheel stays in transcript at top boundary; playback scrolls to text; manual scrolling suspends follow; resume works; live ElevenLabs playback supplied advancing character positions. Synthetic text only; no Muse message sent.")
        } finally {
            ui {
                speech.stop(); speech.onReading = originalCallback
                MainActivity::class.java.getDeclaredMethod("renderConversation").apply { isAccessible = true }.invoke(app)
                MainActivity::class.java.getDeclaredMethod("connect").apply { isAccessible = true }.invoke(app)
                app.window.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            }
        }
    }
}
