package dev.cameronpak.muser1

import android.app.*
import android.content.Intent
import android.os.*
import android.accessibilityservice.AccessibilityService

/** Explicit opt-in hardware check. Creates and deletes only its own short-lived fixtures. */
internal object LocalClockCheck {
    fun run(test:Instrumentation,result:Bundle){
        val c=test.targetContext
        check(LocalClock.exact(c)){"Allow Alarms & reminders before this check"}
        check(c.getSystemService(NotificationManager::class.java).areNotificationsEnabled()){"Allow notifications before this check"}
        val owned=mutableListOf<String>()
        var app:Activity?=null
        try{
            app=test.startActivitySync(Intent(c,ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            val timer=LocalClock.add(c,ClockCommand("timer",seconds=8));owned.add(timer.id)
            LocalClock.change(c,timer.id,"pause")
            val before=LocalClock.entries(c).first{it.id==timer.id}.remaining
            Thread.sleep(1200)
            check(LocalClock.entries(c).first{it.id==timer.id}.state=="paused")
            check(LocalClock.entries(c).first{it.id==timer.id}.remaining==before)
            val cancelled=LocalClock.add(c,ClockCommand("timer",seconds=2));owned.add(cancelled.id);LocalClock.change(c,cancelled.id,"delete")
            LocalClock.change(c,timer.id,"resume")
            check(test.uiAutomation.performGlobalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN))
            val deadline=SystemClock.elapsedRealtime()+20000
            while(SystemClock.elapsedRealtime()<deadline && !ClockRingService.playbackActive)Thread.sleep(200)
            check(LocalClock.entries(c).first{it.id==timer.id}.state=="ringing"){"Timer did not fire while screen off"}
            check(ClockRingService.playbackActive){"Ringtone playback did not start"}
            check(LocalClock.entries(c).none{it.id==cancelled.id})
            check(c.getSystemService(NotificationManager::class.java).activeNotifications.any{it.id==7301})
            LocalClock.change(c,timer.id,"snooze")
            check(LocalClock.remaining(c,LocalClock.entries(c).first{it.id==timer.id}) in 295000..300000)
            Thread.sleep(300);check(!ClockRingService.playbackActive)
            val next=java.time.ZonedDateTime.now().plusMinutes(1).withSecond(0).withNano(0)
            val alarm=LocalClock.add(c,ClockCommand("alarm",hour=next.hour,minute=next.minute));owned.add(alarm.id)
            val alarmDeadline=SystemClock.elapsedRealtime()+70000
            while(SystemClock.elapsedRealtime()<alarmDeadline && !ClockRingService.playbackActive)Thread.sleep(200)
            check(LocalClock.entries(c).first{it.id==alarm.id}.state=="ringing"){"Wall-clock alarm did not fire"}
            check(ClockRingService.playbackActive){"Alarm ringtone did not start"}
            LocalClock.change(c,alarm.id,"stop")
            Thread.sleep(300);check(!ClockRingService.playbackActive)
            result.putString("stream","PASS: R1 local timer persisted, paused/resumed, cancelled fixture remained cancelled, fired after screen lock, ringtone reported active, alarm notification posted, snooze stopped playback and scheduled +5min. Wall-clock alarm also fired at the next minute and stopped. Test entries removed; no microphone/Muse request. Human audible confirmation and reboot test still required.")
        }finally{
            owned.forEach{runCatching{LocalClock.change(c,it,"delete")}}
            app?.let{a->test.runOnMainSync{a.finish()}}
        }
    }
}
