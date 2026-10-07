package dev.cameronpak.muser1

import android.app.*
import android.content.*
import android.net.Uri
import java.time.ZonedDateTime

/** Reminders execute on the R1, including without a Muse connection. */
internal object PetPromises {
    const val EXTRA="pet_promise"
    private const val CHANNEL="pet_promises"
    private fun alarm(c:Context,id:String)=PendingIntent.getBroadcast(c,0,Intent(c,PetPromiseReceiver::class.java).setAction("promise").setData(Uri.parse("muse-pet://promise/$id")),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun schedule(c:Context,p:PetPromise){
        val manager=c.getSystemService(AlarmManager::class.java)
        // Inexact idle-capable reminders avoid requesting a new permission for a casual daily activity.
        manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,p.due,alarm(c,p.id))
    }
    fun add(c:Context,p:PetPromise){
        val store=PetActivitiesStore(c);require(store.promises().count { it.seed==p.seed }<6)
        store.putPromise(p);try{schedule(c,p)}catch(e:Exception){store.removePromise(p.id);throw e}
    }
    fun remove(c:Context,id:String){c.getSystemService(AlarmManager::class.java).cancel(alarm(c,id));PetActivitiesStore(c).removePromise(id);c.getSystemService(NotificationManager::class.java).cancel(id,81)}
    fun restore(c:Context){
        val store=PetActivitiesStore(c);val pet=PetStore(c).load(System.currentTimeMillis())
        store.promises().forEach { p ->
            if(p.seed!=pet.seed || pet.dead){remove(c,p.id);return@forEach}
            val next=p.copy(due=PetAdventure.nextPromise(p.hour,p.minute,ZonedDateTime.now()))
            store.putPromise(next);schedule(c,next)
        }
    }
    fun fire(c:Context,id:String){
        val store=PetActivitiesStore(c);val p=store.promises().find { it.id==id }?:return
        val pet=PetStore(c).load(System.currentTimeMillis())
        if(p.seed!=pet.seed || pet.dead){remove(c,id);return}
        if(p.due>System.currentTimeMillis()+1500)return
        val next=p.copy(pending=true,due=PetAdventure.nextPromise(p.hour,p.minute,ZonedDateTime.now()))
        store.putPromise(next);schedule(c,next)
        val manager=c.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel(CHANNEL,UiText.text(c,"우리만의 약속","ふたりの約束","Our promises"),NotificationManager.IMPORTANCE_DEFAULT))
        val open=PendingIntent.getActivity(c,0,Intent(c,MainActivity::class.java).setData(Uri.parse("muse-pet://open/$id")).putExtra(EXTRA,id).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification=Notification.Builder(c,CHANNEL).setSmallIcon(R.drawable.ic_speaker).setContentTitle(UiText.text(c,"뮤즈가 기다려요","Museが待っているよ","Muse is waiting"))
            .setContentText(PetActivityText.promise(c,p.kind)).setContentIntent(open).setAutoCancel(true).setVisibility(Notification.VISIBILITY_PRIVATE).build()
        runCatching { manager.notify(id,81,notification) }
    }
}
class PetPromiseReceiver:BroadcastReceiver(){
    override fun onReceive(c:Context,i:Intent){
        if(i.action=="promise")PetPromises.fire(c,i.data?.lastPathSegment?:return)
        else if(i.action in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED))PetPromises.restore(c)
    }
}
