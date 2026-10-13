package dev.cameronpak.muser1

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

internal data class PetDiscovery(val id:String,val kind:Int,val at:Long,val question:String,val reply:String)

/** New game state is isolated from care, credentials and the original activity records. */
internal class PetWorldStore(private val context:Context) {
    private val prefs=context.getSharedPreferences("pet_world",Context.MODE_PRIVATE)
    private fun key(seed:Long,name:String)="${seed}_$name"
    private fun array(seed:Long,name:String)=runCatching {JSONArray(prefs.getString(key(seed,name),"[]"))}.getOrDefault(JSONArray())
    private fun save(seed:Long,name:String,a:JSONArray){check(prefs.edit().putString(key(seed,name),a.toString()).commit())}
    fun garden(seed:Long,now:Long=System.currentTimeMillis()):List<GardenPlant?> {
        val a=array(seed,"garden")
        return (0..2).map {i->a.optJSONObject(i)?.let {o->runCatching {
            GardenPlant(o.getLong("seed"),o.getInt("kind"),o.getLong("planted"),o.getLong("observed"),o.getLong("watered"),
                o.getLong("age"),o.getDouble("health"),(o.optJSONArray("days")?:JSONArray()).let {ds->(0 until ds.length()).map {ds.getLong(it)}.toSet()}).advance(now)
        }.getOrNull()} }
    }
    private fun saveGarden(seed:Long,plots:List<GardenPlant?>) {
        val a=JSONArray();plots.forEach {p->a.put(p?.let {JSONObject().put("seed",it.seed).put("kind",it.kind).put("planted",it.planted)
            .put("observed",it.observed).put("watered",it.watered).put("age",it.age).put("health",it.health).put("days",JSONArray(it.careDays.toList()))}?:JSONObject.NULL)}
        save(seed,"garden",a)
    }
    fun plant(seed:Long,slot:Int,kind:Int,now:Long=System.currentTimeMillis()):Boolean {
        if(slot !in 0..2 || kind !in 0..2)return false
        val p=garden(seed,now).toMutableList();if(p[slot]!=null)return false
        p[slot]=GardenPlant(UUID.randomUUID().mostSignificantBits,kind,now);saveGarden(seed,p)
        PetActivitiesStore(context).event(seed,"garden","plant",now);return true
    }
    fun water(seed:Long,slot:Int,now:Long=System.currentTimeMillis()):Boolean {
        if(slot !in 0..2)return false
        val plots=garden(seed,now).toMutableList();val p=plots[slot]?:return false
        val next=p.water(now);plots[slot]=next;saveGarden(seed,plots)
        if(next.watered==p.watered)return false
        PetActivitiesStore(context).event(seed,"garden","water",now);return true
    }
    fun collect(seed:Long,slot:Int,now:Long=System.currentTimeMillis()):Int? {
        if(slot !in 0..2)return null
        val plots=garden(seed,now).toMutableList();val p=plots[slot]?.takeIf {it.blooming}?:return null
        plots[slot]=null
        val flowers=(flowers(seed)+p.flower).distinct()
        // Garden removal and collection ownership commit together, so repeated taps cannot claim twice.
        val a=JSONArray();plots.forEach {plant->a.put(plant?.let {JSONObject().put("seed",it.seed).put("kind",it.kind).put("planted",it.planted).put("observed",it.observed).put("watered",it.watered).put("age",it.age).put("health",it.health).put("days",JSONArray(it.careDays.toList()))}?:JSONObject.NULL)}
        check(prefs.edit().putString(key(seed,"garden"),a.toString()).putString(key(seed,"flowers"),JSONArray(flowers).toString()).commit())
        PetActivitiesStore(context).event(seed,"garden","bloom",now);return p.flower
    }
    fun clearWilted(seed:Long,slot:Int,now:Long=System.currentTimeMillis()):Boolean {
        if(slot !in 0..2)return false
        val plots=garden(seed,now).toMutableList();if(plots[slot]?.wilted!=true)return false
        plots[slot]=null;saveGarden(seed,plots);return true
    }
    fun flowers(seed:Long):List<Int> {val a=array(seed,"flowers");return (0 until a.length()).map {a.getInt(it)}.filter {it in 0..3}.distinct()}
    fun skills(seed:Long):List<LearnedSkill> {
        val a=array(seed,"skills");return (0..2).map {id->val o=a.optJSONObject(id)
            LearnedSkill(id,o?.optString("phrase").orEmpty(),o?.optJSONArray("days")?.let {ds->(0 until ds.length()).map {ds.getLong(it)}.toSet()}?:emptySet(),o?.optLong("last")?:0)
        }
    }
    private fun saveSkills(seed:Long,skills:List<LearnedSkill>) {val a=JSONArray();skills.forEach {a.put(JSONObject().put("phrase",it.phrase).put("days",JSONArray(it.days.toList())).put("last",it.last))};save(seed,"skills",a)}
    fun practice(pet:PetState,id:Int,now:Long=System.currentTimeMillis()):LearnedSkill? {
        if(id !in 0..2 || !pet.hatched || pet.sleeping || pet.dead || pet.ill || PetActivitiesStore(context).trip(pet.seed)!=null)return null
        val skills=skills(pet.seed).toMutableList();val before=skills[id];skills[id]=before.practice(now);saveSkills(pet.seed,skills)
        if(before!=skills[id])PetActivitiesStore(context).event(pet.seed,"skill",id.toString(),now)
        return skills[id]
    }
    fun setPhrase(seed:Long,id:Int,phrase:String):Boolean {
        if(id !in 0..2 || phrase.trim().length>60)return false
        val skills=skills(seed).toMutableList();val normalized=PetSkills.normalize(phrase)
        if(normalized.isNotBlank() && skills.any {it.id!=id && PetSkills.triggers(it).any {p->PetSkills.normalize(p)==normalized}})return false
        skills[id]=skills[id].copy(phrase=phrase.trim());saveSkills(seed,skills);return true
    }
    fun story(seed:Long):List<StoryChoice> {val a=array(seed,"story");return (0 until a.length()).map {val o=a.getJSONObject(it);StoryChoice(o.getInt("chapter"),o.getInt("choice"),o.getLong("at"))}}
    fun choose(seed:Long,choice:Int,now:Long=System.currentTimeMillis()):Boolean {
        val next=PetStory.choose(story(seed),choice,now)?:return false
        val a=JSONArray();next.forEach {a.put(JSONObject().put("chapter",it.chapter).put("choice",it.choice).put("at",it.at))};save(seed,"story",a)
        PetActivitiesStore(context).event(seed,"story",next.size.toString(),now);return true
    }
    fun photos(seed:Long):List<PetDiscovery> {val a=array(seed,"photos");return (0 until a.length()).map {val o=a.getJSONObject(it);PetDiscovery(o.getString("id"),o.getInt("kind"),o.getLong("at"),o.getString("question"),o.optString("reply"))}}
    private fun savePhotos(seed:Long,photos:List<PetDiscovery>){val a=JSONArray();photos.forEach {a.put(JSONObject().put("id",it.id).put("kind",it.kind).put("at",it.at).put("question",it.question).put("reply",it.reply))};save(seed,"photos",a)}
    private fun directory(seed:Long)=File(context.noBackupFilesDir,"pet-discoveries/$seed")
    fun file(seed:Long,id:String):File {require(id.matches(Regex("[a-f0-9-]{36}")));return File(directory(seed),"$id.jpg")}
    fun addPhoto(seed:Long,id:String,kind:Int,jpeg:ByteArray,question:String,now:Long=System.currentTimeMillis()):Boolean {
        if(kind !in 0..2 || jpeg.size !in 4..180000 || jpeg[0]!=0xff.toByte() || jpeg[1]!=0xd8.toByte() || jpeg[jpeg.size-2]!=0xff.toByte() || jpeg.last()!=0xd9.toByte())return false
        val photos=photos(seed);if(photos.any {it.id==id})return true
        if(photos.size>=24)return false
        val target=file(seed,id);target.parentFile!!.mkdirs();val temp=File(target.parentFile,"$id.tmp")
        try {temp.writeBytes(jpeg);check(temp.renameTo(target));savePhotos(seed,photos+PetDiscovery(id,kind,now,question.take(1000),""))}
        catch(error:Exception){temp.delete();target.delete();throw error}
        PetActivitiesStore(context).event(seed,"discovery",id,now);return true
    }
    fun photoReply(seed:Long,id:String,reply:String):Boolean {
        val photos=photos(seed);val p=photos.firstOrNull {it.id==id}?:return false
        if(p.reply.isNotBlank())return false
        savePhotos(seed,photos.map {if(it.id==id)it.copy(reply=reply.take(2400).replace("**",""))else it});return true
    }
    fun deletePhoto(seed:Long,id:String):Boolean {
        val photos=photos(seed);if(photos.none {it.id==id})return false
        savePhotos(seed,photos.filterNot {it.id==id});file(seed,id).delete();return true
    }
    fun context(seed:Long,now:Long=System.currentTimeMillis()):JSONObject = JSONObject()
        .put("garden",JSONArray(garden(seed,now).map {p->when {p==null->"empty";p.wilted->"wilted";p.blooming->"blooming";p.stage>=2->"growing";else->"sprouting"}}))
        .put("collected_flowers",flowers(seed).size).put("story_chapters_read",story(seed).size)
        .put("photo_discoveries",photos(seed).size)
        .put("learned_skills",JSONArray(skills(seed).filter {it.learned}.map {s->JSONObject().put("action",PetSkills.actions[s.id].name.lowercase()).put("triggers",JSONArray(PetSkills.triggers(s)))}))
}
