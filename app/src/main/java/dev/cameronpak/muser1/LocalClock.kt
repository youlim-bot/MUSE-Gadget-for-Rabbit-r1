package dev.cameronpak.muser1

import android.app.*
import android.content.*
import android.net.Uri
import android.os.*
import android.provider.Settings
import org.json.*
import java.time.*
import java.util.UUID

internal data class ClockEntry(val id:String=UUID.randomUUID().toString(),val kind:String,val due:Long,val elapsed:Long=0,val boot:Int=0,val remaining:Long=0,val state:String="active",val weekdays:Boolean=false,val hour:Int=0,val minute:Int=0,val title:String="")
internal object LocalClock {
    private fun prefs(c:Context)=c.getSharedPreferences("local_clock",Context.MODE_PRIVATE)
    @Synchronized fun entries(c:Context):List<ClockEntry> {
        val a=JSONArray(prefs(c).getString("entries","[]"))
        return (0 until a.length()).map{val o=a.getJSONObject(it);ClockEntry(o.getString("id"),o.getString("kind"),o.getLong("due"),o.optLong("elapsed"),o.optInt("boot"),o.optLong("remaining"),o.optString("state","active"),o.optBoolean("weekdays"),o.optInt("hour"),o.optInt("minute"),o.optString("title"))}
    }
    private fun save(c:Context,list:List<ClockEntry>){val a=JSONArray();list.forEach{a.put(JSONObject().put("id",it.id).put("kind",it.kind).put("due",it.due).put("elapsed",it.elapsed).put("boot",it.boot).put("remaining",it.remaining).put("state",it.state).put("weekdays",it.weekdays).put("hour",it.hour).put("minute",it.minute).put("title",it.title))};check(prefs(c).edit().putString("entries",a.toString()).commit())}
    fun exact(c:Context)=Build.VERSION.SDK_INT<31||c.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
    fun boot(c:Context)=Settings.Global.getInt(c.contentResolver,Settings.Global.BOOT_COUNT,0)
    fun remaining(c:Context,e:ClockEntry):Long=if(e.state=="paused")e.remaining else if(e.kind=="timer"&&e.boot==boot(c))e.elapsed-SystemClock.elapsedRealtime()else e.due-System.currentTimeMillis()
    private fun pending(c:Context,id:String)=PendingIntent.getBroadcast(c,0,Intent(c,ClockReceiver::class.java).setAction("fire").setData(Uri.parse("muse-clock://task/$id")),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    fun open(c:Context)=PendingIntent.getActivity(c,0,Intent(c,ClockActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    private fun schedule(c:Context,e:ClockEntry){
        check(exact(c)){"Permission required"}
        val trigger=if(e.kind=="timer")System.currentTimeMillis()+remaining(c,e).coerceAtLeast(500) else e.due.coerceAtLeast(System.currentTimeMillis()+500)
        c.getSystemService(AlarmManager::class.java).setAlarmClock(AlarmManager.AlarmClockInfo(trigger,open(c)),pending(c,e.id))
    }
    @Synchronized fun add(c:Context,command:ClockCommand):ClockEntry {
        check(exact(c));require(command.kind in setOf("timer","alarm"));if(command.kind=="timer")require(command.seconds in 1..86400);val all=entries(c);require(all.size<40)
        val now=System.currentTimeMillis();val e=if(command.kind=="timer")ClockEntry(kind="timer",due=now+command.seconds*1000,elapsed=SystemClock.elapsedRealtime()+command.seconds*1000,boot=boot(c))else ClockEntry(kind="alarm",due=LocalClockCommand.due(command,ZonedDateTime.now()),weekdays=command.weekdays,hour=command.hour,minute=command.minute,title=command.title.take(160))
        save(c,all+e);try{schedule(c,e)}catch(t:Exception){save(c,all);throw t};return e
    }
    @Synchronized fun change(c:Context,id:String,action:String){
        val list=entries(c);val e=list.find{it.id==id}?:return
        c.getSystemService(AlarmManager::class.java).cancel(pending(c,id))
        val next=when(action){
            "delete"->null
            "pause"->e.copy(state="paused",remaining=remaining(c,e).coerceAtLeast(1000))
            "resume"->e.copy(state="active",due=System.currentTimeMillis()+e.remaining,elapsed=SystemClock.elapsedRealtime()+e.remaining,boot=boot(c))
            "snooze"->e.copy(state="active",due=System.currentTimeMillis()+300000,elapsed=SystemClock.elapsedRealtime()+300000,boot=boot(c))
            "enable"->e.copy(state="active",due=LocalClockCommand.due(ClockCommand("alarm",hour=e.hour,minute=e.minute,weekdays=e.weekdays),ZonedDateTime.now()))
            else->if(e.weekdays&&action=="stop")e.copy(state="active",due=LocalClockCommand.due(ClockCommand("alarm",hour=e.hour,minute=e.minute,weekdays=true),ZonedDateTime.now()))else e.copy(state="off")
        }
        if(next?.state=="active")try{schedule(c,next)}catch(t:Exception){if(e.state=="active")runCatching{schedule(c,e)};throw t}
        save(c,list.filterNot{it.id==id}+listOfNotNull(next))
        if(entries(c).none{it.state=="ringing"})c.stopService(Intent(c,ClockRingService::class.java))
    }
    @Synchronized fun fire(c:Context,id:String):Boolean {
        val all=entries(c);val e=all.find{it.id==id}?:return false
        if(e.state!="active"||remaining(c,e)>1500)return false
        save(c,all.map{if(it.id==id)it.copy(state="ringing")else it});return true
    }
    @Synchronized fun restore(c:Context){if(!exact(c))return;val all=entries(c);val fixed=all.map{e->when{e.state=="ringing"->e.copy(state="off");e.state!="active"->e;e.kind=="timer"&&e.boot!=boot(c)->if(e.due<=System.currentTimeMillis())e.copy(state="off")else e.copy(boot=boot(c),elapsed=SystemClock.elapsedRealtime()+e.due-System.currentTimeMillis());e.kind=="alarm"&&e.due<System.currentTimeMillis()->if(e.weekdays)e.copy(due=LocalClockCommand.due(ClockCommand("alarm",hour=e.hour,minute=e.minute,weekdays=true),ZonedDateTime.now()))else e.copy(state="off");else->e}};save(c,fixed);fixed.filter{it.state=="active"}.forEach{runCatching{schedule(c,it)}}}
}
class ClockReceiver:BroadcastReceiver(){override fun onReceive(c:Context,i:Intent){if(i.action=="fire"){if(LocalClock.fire(c,i.data?.lastPathSegment?:return))c.startForegroundService(Intent(c,ClockRingService::class.java))}else if(i.action in setOf(Intent.ACTION_BOOT_COMPLETED,Intent.ACTION_MY_PACKAGE_REPLACED,Intent.ACTION_TIME_CHANGED,Intent.ACTION_TIMEZONE_CHANGED,AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED))LocalClock.restore(c)}}
