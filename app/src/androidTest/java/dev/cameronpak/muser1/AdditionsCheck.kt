package dev.cameronpak.muser1

import android.app.*
import android.content.*
import android.os.*
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView

internal object AdditionsCheck {
    fun run(test: Instrumentation, result: Bundle, uiOnly:Boolean=false) {
        fun ui(block: () -> Unit) {
            var error: Throwable? = null
            test.runOnMainSync { try { block() } catch (t: Throwable) { error = t } }
            error?.let { throw it }
        }
        val automation=test.getUiAutomation(UiAutomation.FLAG_DONT_SUPPRESS_ACCESSIBILITY_SERVICES)
        val c = test.targetContext
        c.startActivity(Intent(c,MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        var found: MainActivity? = null
        repeat(100) { if(found==null) { ui { found=MainActivity.foreground?.takeIf{it.hasWindowFocus()} }; Thread.sleep(100) } }
        val app=checkNotNull(found)
        fun field(n:String)=MainActivity::class.java.getDeclaredField(n).apply{isAccessible=true}
        fun invoke(n:String)=MainActivity::class.java.getDeclaredMethod(n).apply{isAccessible=true}.invoke(app)
        fun capture(name:String){ automation.takeScreenshot()?.let { b->java.io.File(c.cacheDir,name).outputStream().use{b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,it)};b.recycle() } }
        val pref=c.getSharedPreferences("reading",Context.MODE_PRIVATE)
        val existed=pref.contains("desk_clock");val old=pref.getBoolean("desk_clock",true)
        val prior=LocalClock.entries(c)
        var reminder:String?=null
        var wasConnected=false
        val speech=field("speech").get(app) as SpeechOutput
        try {
            ui { pref.edit().putBoolean("desk_clock",false).commit();invoke("closeDeskClock");wasConnected=field("connected").getBoolean(app) }
            if(uiOnly){
                ui { app.showClockPanel(ClockCommand("reminder")) }
                Thread.sleep(700);capture("reminder-editor-check.png")
                ui {
                    val panel=field("clockPanel").get(app) as ClockPanel
                    val content=ClockPanel::class.java.getDeclaredField("content").apply{isAccessible=true}.get(panel) as ViewGroup
                    (content.getChildAt(1) as android.widget.EditText).setText("테스트 일정")
                    (0 until content.childCount).map{content.getChildAt(it)}.filterIsInstance<android.widget.Button>().first{it.text.toString().matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))}.performClick()
                }
                Thread.sleep(700);capture("reminder-date-check.png")
                ui {
                    val panel=field("clockPanel").get(app) as ClockPanel
                    val content=ClockPanel::class.java.getDeclaredField("content").apply{isAccessible=true}.get(panel) as ViewGroup
                    val row=content.getChildAt(content.childCount-1) as ViewGroup
                    check(row.getChildAt(1).isShown);row.getChildAt(1).performClick()
                    check((content.getChildAt(1) as android.widget.EditText).text.toString()=="테스트 일정")
                    val footer=ClockPanel::class.java.getDeclaredField("footer").apply{isAccessible=true}.get(panel) as ViewGroup
                    check(footer.childCount==1&&footer.isShown)
                }
                result.putString("stream","PASS: reminder editor has fixed save controls; in-Muse date selection opens and returns preserving entered description. No reminder saved.")
                return
            }
            // Microphone is opened briefly and discarded; no speech is sent to Muse.
            repeat(20){if(SideButtonService.instance==null)Thread.sleep(100)}
            if(SideButtonService.instance==null){
                fun shell(command:String)=automation.executeShellCommand(command).use { android.os.ParcelFileDescriptor.AutoCloseInputStream(it).use{input->input.readBytes().toString(Charsets.UTF_8).trim()} }
                val enabled=shell("settings get secure enabled_accessibility_services")
                check(enabled.contains("SideButtonService")){"Side button controls must already be enabled"}
                val others=enabled.split(":").filterNot{it.contains("dev.cameronpak.muser1")}.joinToString(":")
                try {
                    if(others.isBlank())shell("settings delete secure enabled_accessibility_services") else shell("settings put secure enabled_accessibility_services $others")
                    Thread.sleep(500)
                }finally{shell("settings put secure enabled_accessibility_services $enabled")}
                repeat(100){if(SideButtonService.instance==null)Thread.sleep(100)}
            }
            val service=checkNotNull(SideButtonService.instance){"Side button service unavailable"}
            fun key(event:KeyEvent){SideButtonService::class.java.getDeclaredMethod("onKeyEvent",KeyEvent::class.java).apply{isAccessible=true}.invoke(service,event)}
            val local=SpeechOutput::class.java.getDeclaredField("local").apply{isAccessible=true}.get(speech) as AndroidSpeechOutput
            ui {
                field("connected").setBoolean(app,true)
                local.speak("음성 답변 도중 끼어들기 기능을 테스트합니다. ".repeat(30))
            }
            val press=SystemClock.uptimeMillis()
            ui { check(speech.hasPlayback); key(KeyEvent(press,press,KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_PAIRING,0)) }
            Thread.sleep(600)
            ui {
                check(!speech.hasPlayback){"Speech not interrupted"}
                check(field("recording").getBoolean(app)){"Microphone not started"}
                app.finishSideButtonRecording(false)
                key(KeyEvent(press,SystemClock.uptimeMillis(),KeyEvent.ACTION_UP,KeyEvent.KEYCODE_PAIRING,0,0,0,0,KeyEvent.FLAG_CANCELED))
                field("connected").setBoolean(app,wasConnected)
                app.showClockPanel(ClockCommand("reminder"))
            }
            Thread.sleep(500);capture("reminder-editor-check.png")
            ui {
                val panel=field("clockPanel").get(app) as ClockPanel
                val content=ClockPanel::class.java.getDeclaredField("content").apply{isAccessible=true}.get(panel) as ViewGroup
                val date=(0 until content.childCount).map{content.getChildAt(it)}.filterIsInstance<android.widget.Button>().first{it.text.toString().matches(Regex("[0-9]{4}-[0-9]{2}-[0-9]{2}"))}
                date.performClick()
            }
            Thread.sleep(700);capture("reminder-date-check.png")
            ui {
                val panel=field("clockPanel").get(app) as ClockPanel
                val content=ClockPanel::class.java.getDeclaredField("content").apply{isAccessible=true}.get(panel) as ViewGroup
                check((content.getChildAt(0) as TextView).text.toString().isNotBlank())
                (field("clockDialog").get(app) as Dialog).dismiss()
            }
            val label="테스트 일정 · 물 마시기"
            reminder=LocalClock.add(c,ClockCommand("alarm",title=label,at=System.currentTimeMillis()+8000)).id
            check(LocalClock.entries(c).first{it.id==reminder}.title==label)
            val deadline=SystemClock.uptimeMillis()+20000
            while(SystemClock.uptimeMillis()<deadline && LocalClock.entries(c).none{it.id==reminder&&it.state=="ringing"}) Thread.sleep(200)
            check(LocalClock.entries(c).any{it.id==reminder&&it.state=="ringing"}){"Reminder did not fire"}
            Thread.sleep(500)
            check(c.getSystemService(NotificationManager::class.java).activeNotifications.any{it.id==7301&&it.notification.extras.getString(Notification.EXTRA_TITLE)==label}){"Reminder title absent"}
            check(ClockRingService.playbackActive){"Reminder sound not started"}
            capture("reminder-ringing-check.png")
            LocalClock.change(c,reminder!!,"stop");LocalClock.change(c,reminder!!,"delete");reminder=null
            ui {
                (field("clockDialog").get(app) as? Dialog)?.dismiss()
                pref.edit().putBoolean("desk_clock",true).commit()
                check(field("charging").getBoolean(app)){"Connect charger"}
                field("lastInteraction").setLong(app,SystemClock.elapsedRealtime())
            }
            Thread.sleep(21500)
            ui { check((field("deskClock").get(app) as? Dialog)?.isShowing==true){"Automatic desk clock did not open"} }
            capture("desk-clock-check.png")
            ui {
                val dialog=field("deskClock").get(app) as Dialog
                val container=dialog.findViewById<ViewGroup>(android.R.id.content)
                check(container.getChildAt(0).performClick())
            }
            Thread.sleep(300)
            ui { check(field("deskClock").get(app)==null){"Touch did not return to Muse"};check(app.hasWindowFocus()) }
            check(LocalClock.entries(c)==prior){"Existing schedules changed"}
            result.putString("stream","PASS: side-button hold interrupted local speech and started capture; capture discarded without sending. Reminder persisted its title, rang on R1, and showed its title in notification. Charging desk clock opened after 20 seconds idle and touch returned to Muse. Existing clock entries preserved; test reminder deleted.")
        } finally {
            reminder?.let{LocalClock.change(c,it,"delete")}
            ui {
                speech.stop();app.finishSideButtonRecording(false)
                field("connected").setBoolean(app,wasConnected)
                (field("clockDialog").get(app) as? Dialog)?.dismiss()
                invoke("closeDeskClock")
                pref.edit().apply{if(existed)putBoolean("desk_clock",old)else remove("desk_clock")}.commit()
            }
        }
    }
}
