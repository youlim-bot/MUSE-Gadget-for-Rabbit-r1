package dev.cameronpak.muser1

import android.app.Instrumentation
import android.content.Intent
import android.os.Bundle
import dev.cameronpak.muser1.transport.MuseConnection

/** Opt-in diagnostic: real handshake only, no chat, task, recording, or credential export. */
internal object ConnectionRecoveryCheck {
    fun run(test:Instrumentation,result:Bundle) {
        fun ui(block:()->Unit) {
            var error:Throwable?=null
            test.runOnMainSync { try { block() }catch(t:Throwable){error=t} }
            error?.let { throw it }
        }
        test.targetContext.startActivity(Intent(test.targetContext,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var activity:MainActivity?=null
        repeat(100) { if(activity==null) { ui { activity=MainActivity.foreground?.takeIf { it.hasWindowFocus() } };if(activity==null)Thread.sleep(100) } }
        val home=checkNotNull(activity){"MainActivity not foreground"}
        val connected=MainActivity::class.java.getDeclaredField("connected").apply { isAccessible=true }
        val connection=MainActivity::class.java.getDeclaredField("connection").apply { isAccessible=true }
        val roomField=MainActivity::class.java.getDeclaredField("petRoom").apply { isAccessible=true }
        fun progress(value:String) { test.sendStatus(0,Bundle().apply { putString("stream",value+"\n") }) }
        fun waitReady() {
            var ready=false
            repeat(360) { if(!ready) { ui { ready=connected.getBoolean(home) };if(!ready)Thread.sleep(250) } }
            check(ready){"Muse did not become ready"}
        }
        progress("Waiting for initial Muse connection")
        waitReady()
        progress("Initial Muse connection ready")
        lateinit var original:MuseConnection
        ui {
            MainActivity::class.java.getDeclaredMethod("showPetRoom").apply { isAccessible=true }.invoke(home)
            check((roomField.get(home) as PetRoom).isShowing)
            check(MainActivity.foreground===home && connected.getBoolean(home))
            original=connection.get(home) as MuseConnection
            original.close() // Controlled socket loss; never send a user message.
            check(connection.get(home)==null){"Closed transport retained"}
            check(!connected.getBoolean(home))
        }
        progress("Closed transport cleared; waiting for automatic recovery")
        waitReady()
        progress("Automatic recovery ready")
        ui {
            check(connection.get(home)!==original){"Closed transport reused"}
            check((roomField.get(home) as PetRoom).isShowing){"Pet room was dismissed"}
            home.petRefresh()
            (roomField.get(home) as PetRoom).dismiss()
        }
        result.putString("stream","PASS: Muse handshake, pet room preserves live session, controlled connection loss clears stale transport, automatic reconnect creates a new ready connection. No microphone, chat messages or agent tasks used.")
    }
}
