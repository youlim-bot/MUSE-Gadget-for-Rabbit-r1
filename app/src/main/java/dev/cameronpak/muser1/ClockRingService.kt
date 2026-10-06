package dev.cameronpak.muser1

import android.app.*
import android.content.Intent
import android.media.*
import android.os.*

class ClockRingService:Service(){
    companion object { @Volatile internal var playbackActive=false }
    private var sound:Ringtone?=null
    private var focus:AudioFocusRequest?=null
    private var lock:PowerManager.WakeLock?=null
    private val handler=Handler(Looper.getMainLooper())
    override fun onBind(i:Intent?)=null
    override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int {
        val ringing=LocalClock.entries(this).filter{it.state=="ringing"}
        if(ringing.isEmpty()){stopSelf();return START_NOT_STICKY}
        val nm=getSystemService(NotificationManager::class.java)
        val channel=NotificationChannel("local-clock-ring","R1 alarms & timers",NotificationManager.IMPORTANCE_HIGH).apply{setSound(null,null);lockscreenVisibility=Notification.VISIBILITY_PUBLIC}
        nm.createNotificationChannel(channel)
        val stop=PendingIntent.getService(this,1,Intent(this,ClockDismissService::class.java),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n=Notification.Builder(this,"local-clock-ring").setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle(UiText.text(this,"R1 알람·타이머","R1 アラーム・タイマー","R1 alarm / timer"))
            .setContentText(UiText.text(this,"눌러서 중지 또는 5분 뒤 다시","タップして停止・5分後に再通知","Tap to stop or snooze 5 min"))
            .setCategory(Notification.CATEGORY_ALARM).setOngoing(true).setContentIntent(LocalClock.open(this)).setFullScreenIntent(LocalClock.open(this),true)
            .addAction(Notification.Action.Builder(null,UiText.text(this,"끄기","停止","Stop"),stop).build()).build()
        startForeground(7301,n)
        MainActivity.foreground?.showClockPanel()
        if(sound==null){
            focus=AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE).setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).build()).setOnAudioFocusChangeListener{ }.build()
            getSystemService(AudioManager::class.java).requestAudioFocus(focus!!)
            lock=getSystemService(PowerManager::class.java).newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,"Muse:local-clock").apply{acquire(330000)}
            sound=RingtoneManager.getRingtone(this,RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?:RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.apply{audioAttributes=AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build();isLooping=true;play()}
        }
        playbackActive=sound?.isPlaying==true
        handler.removeCallbacksAndMessages(null);handler.postDelayed({LocalClock.entries(this).filter{it.state=="ringing"}.forEach{LocalClock.change(this,it.id,"stop")};stopSelf()},300000)
        return START_STICKY
    }
    override fun onDestroy(){playbackActive=false;handler.removeCallbacksAndMessages(null);sound?.stop();sound=null;focus?.let{getSystemService(AudioManager::class.java).abandonAudioFocusRequest(it)};lock?.let{if(it.isHeld)it.release()};stopForeground(STOP_FOREGROUND_REMOVE);super.onDestroy()}
}
class ClockDismissService:Service(){override fun onBind(i:Intent?)=null;override fun onStartCommand(i:Intent?,flags:Int,startId:Int):Int{LocalClock.entries(this).filter{it.state=="ringing"}.forEach{LocalClock.change(this,it.id,"stop")};stopSelf();return START_NOT_STICKY}}
