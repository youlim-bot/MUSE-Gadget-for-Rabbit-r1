package dev.cameronpak.muser1

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.Instant
import java.time.ZoneId
import java.util.UUID

internal data class PetEvent(val at:Long,val kind:String,val value:String="")
internal data class PetPromise(val id:String,val seed:Long,val kind:Int,val hour:Int,val minute:Int,val due:Long,val pending:Boolean=false)

/** Device-local, per-companion records. No credentials, images or growth thresholds are copied here. */
internal class PetActivitiesStore(private val context:Context) {
    private val prefs=context.getSharedPreferences("pet_activities",Context.MODE_PRIVATE)
    private fun key(seed:Long,name:String)="${seed}_$name"
    private fun array(key:String)=runCatching { JSONArray(prefs.getString(key,"[]")) }.getOrDefault(JSONArray())
    fun events(seed:Long):List<PetEvent> {
        val a=array(key(seed,"events"));return (0 until a.length()).map { a.getJSONObject(it) }.map { PetEvent(it.getLong("at"),it.getString("kind"),it.optString("value")) }
    }
    fun event(seed:Long,kind:String,value:String="",now:Long=System.currentTimeMillis()) {
        val old=events(seed)
        // A daily diary records meaningful events, not every render or every repeated button press.
        if(old.lastOrNull()?.let { it.kind==kind && it.value==value && now-it.at in 0..60_000 }==true)return
        val a=JSONArray();(old+PetEvent(now,kind,value.take(1200))).takeLast(400).forEach { a.put(JSONObject().put("at",it.at).put("kind",it.kind).put("value",it.value)) }
        check(prefs.edit().putString(key(seed,"events"),a.toString()).commit())
    }
    fun welcome(seed:Long){if(events(seed).isEmpty())event(seed,"welcome")}
    fun memories(seed:Long)=events(seed).filter { it.kind in setOf("welcome","trip","memory","mission","promise","hatch") }.reversed()
    fun trip(seed:Long):PetTrip?=runCatching {
        val o=JSONObject(prefs.getString(key(seed,"trip"),null)?:return null)
        PetTrip(o.getString("id"),seed,o.getInt("place"),o.getLong("started"),o.getLong("due"),o.getString("nature"))
    }.getOrNull()
    fun begin(p:PetState,place:Int,now:Long=System.currentTimeMillis()):Boolean {
        if(!PetAdventure.canLeave(p) || place !in 0..2 || trip(p.seed)!=null)return false
        val o=JSONObject().put("id",UUID.randomUUID().toString()).put("place",place).put("started",now).put("due",now+PetAdventure.durations[place]).put("nature",p.temperament.name)
        check(prefs.edit().putString(key(p.seed,"trip"),o.toString()).commit());event(p.seed,"depart",place.toString(),now);return true
    }
    /** Claim is idempotent, including across process death. Care decay is never bypassed. */
    @Synchronized fun claim(p:PetState,now:Long=System.currentTimeMillis()):Int? {
        val t=trip(p.seed)?:return null
        if(!PetAdventure.ready(t,now))return null
        val item=if(p.dead)-1 else PetAdventure.souvenir(t)
        val a=JSONArray();(events(p.seed)+PetEvent(now,"trip","${t.place}|$item|${t.nature}")).takeLast(400).forEach { a.put(JSONObject().put("at",it.at).put("kind",it.kind).put("value",it.value)) }
        check(prefs.edit().remove(key(p.seed,"trip")).putString(key(p.seed,"events"),a.toString())
            .putString(key(p.seed,"items"),(items(p.seed)+listOfNotNull(item.takeIf { it>=0 })).distinct().joinToString(",")).commit())
        return item
    }
    fun recall(seed:Long){if(trip(seed)!=null){prefs.edit().remove(key(seed,"trip")).commit();event(seed,"recall")}}
    fun items(seed:Long)=prefs.getString(key(seed,"items"),"").orEmpty().split(",").mapNotNull { it.toIntOrNull()?.takeIf { n->n in 0..5 } }.distinct()
    fun decoration(seed:Long)=prefs.getInt(key(seed,"decor"),-1)
    fun decorate(seed:Long,item:Int):Boolean {
        if(item!=-1 && item !in items(seed))return false
        prefs.edit().putInt(key(seed,"decor"),item).commit();event(seed,"decorate",item.toString());return true
    }
    fun mission(seed:Long)=prefs.getInt(key(seed,"mission"),-1)
    fun missionToken(seed:Long)=prefs.getString(key(seed,"mission_token"),null)
    fun chooseMission(seed:Long,target:Int){require(target in 0..2);prefs.edit().putInt(key(seed,"mission"),target).putString(key(seed,"mission_token"),UUID.randomUUID().toString()).commit()}
    fun completeMission(seed:Long,token:String,now:Long=System.currentTimeMillis()):Boolean {
        if(token!=missionToken(seed))return false
        val target=mission(seed);if(target !in 0..2)return false
        val day=Instant.ofEpochMilli(now).atZone(ZoneId.systemDefault()).toLocalDate().toString()
        if(prefs.getString(key(seed,"mission_day"),null)==day)return false
        val item=target+3
        val a=JSONArray();(events(seed)+PetEvent(now,"mission",target.toString())).takeLast(400).forEach { a.put(JSONObject().put("at",it.at).put("kind",it.kind).put("value",it.value)) }
        return prefs.edit().remove(key(seed,"mission_token")).putInt(key(seed,"mission"),-1).putString(key(seed,"mission_day"),day)
            .putString(key(seed,"items"),(items(seed)+item).distinct().joinToString(",")).putString(key(seed,"events"),a.toString()).commit()
    }
    fun missionDoneToday(seed:Long)=prefs.getString(key(seed,"mission_day"),null)==java.time.LocalDate.now().toString()
    fun promises():List<PetPromise> {
        val a=array("promises");return (0 until a.length()).map { a.getJSONObject(it) }.map { PetPromise(it.getString("id"),it.getLong("seed"),it.getInt("kind"),it.getInt("hour"),it.getInt("minute"),it.getLong("due"),it.optBoolean("pending")) }
    }
    @Synchronized fun putPromise(p:PetPromise) { writePromises(promises().filterNot { it.id==p.id }+p) }
    fun removePromise(id:String){writePromises(promises().filterNot { it.id==id })}
    private fun writePromises(list:List<PetPromise>) {
        val a=JSONArray();list.forEach { a.put(JSONObject().put("id",it.id).put("seed",it.seed).put("kind",it.kind).put("hour",it.hour).put("minute",it.minute).put("due",it.due).put("pending",it.pending)) };check(prefs.edit().putString("promises",a.toString()).commit())
    }
}
