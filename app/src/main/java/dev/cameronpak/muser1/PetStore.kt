package dev.cameronpak.muser1

import android.content.Context
import org.json.JSONObject

class PetStore(private val context: Context) {
    private val prefs = context.getSharedPreferences("muse_pet", Context.MODE_PRIVATE)
    fun load(now:Long):PetState {
        val raw=prefs.getString("life_v2",null)
        if(raw==null) {
            val hadPet=prefs.contains("updated")
            if(hadPet)archive(now,"migration")
            val legacyHatched=prefs.getBoolean("hatched",hadPet)
            val age=if(legacyHatched)when {
                prefs.getInt("xp",0)>=300 -> 21*PetState.DAY
                prefs.getInt("xp",0)>=120 -> 9*PetState.DAY
                prefs.getInt("xp",0)>=30 -> PetState.DAY
                else -> 0L
            } else 0L
            // New rules start now, never charge historic neglect under the old simulation.
            return PetState(updatedAt=now,seed=kotlin.random.Random.nextLong(),hatched=legacyHatched,
                ageMs=age,bond=prefs.getInt("bond",0)).also(::save)
        }
        val j=JSONObject(raw)
        fun d(k:String,default:Double)=j.optDouble(k,default).coerceIn(0.0,100.0)
        fun l(k:String)=j.optLong(k,0L).coerceAtLeast(0L)
        return PetState(food=d("food",80.0),mood=d("mood",80.0),energy=d("energy",80.0),clean=d("clean",80.0),
            health=d("health",100.0),warmth=d("warmth",50.0),bond=j.optInt("bond",0),
            hatched=j.optBoolean("hatched"),sleeping=j.optBoolean("sleeping"),dead=j.optBoolean("dead"),
            updatedAt=j.optLong("updatedAt",now),seed=j.optLong("seed"),ageMs=l("ageMs"),
            incubationMs=l("incubationMs"),eggCare=j.optInt("eggCare"),eggCareAt=l("eggCareAt"),
            starvingMs=l("starvingMs"),lonelyMs=l("lonelyMs"),sleepMs=l("sleepMs"),diedAt=l("diedAt"),
            rewardAt=l("rewardAt"),fedAt=l("fedAt"),playedAt=l("playedAt"),washedAt=l("washedAt"),medicineAt=l("medicineAt"),
            curiosity=j.optDouble("curiosity",0.0),vigor=j.optDouble("vigor",0.0),dietMask=j.optInt("dietMask"),
            sweets=j.optInt("sweets"),meals=j.optInt("meals"),
            form=PetForm.entries.firstOrNull{it.name==j.optString("form")}?:PetForm.UNDECIDED).advance(now)
    }
    fun save(s:PetState) {
        val j=JSONObject()
        mapOf("food" to s.food,"mood" to s.mood,"energy" to s.energy,"clean" to s.clean,
            "health" to s.health,"warmth" to s.warmth,"bond" to s.bond,"hatched" to s.hatched,
            "sleeping" to s.sleeping,"dead" to s.dead,"updatedAt" to s.updatedAt,"seed" to s.seed,
            "ageMs" to s.ageMs,"incubationMs" to s.incubationMs,"eggCare" to s.eggCare,"eggCareAt" to s.eggCareAt,
            "starvingMs" to s.starvingMs,"lonelyMs" to s.lonelyMs,"sleepMs" to s.sleepMs,"diedAt" to s.diedAt,
            "rewardAt" to s.rewardAt,"fedAt" to s.fedAt,"playedAt" to s.playedAt,"washedAt" to s.washedAt,
            "medicineAt" to s.medicineAt,"curiosity" to s.curiosity,"vigor" to s.vigor,"dietMask" to s.dietMask,
            "sweets" to s.sweets,"meals" to s.meals,"form" to s.form.name).forEach{(k,v)->j.put(k,v)}
        prefs.edit().putString("life_v2",j.toString()).apply()
    }
    private fun archive(now:Long,reason:String) {
        check(context.getSharedPreferences("muse_pet_archive",Context.MODE_PRIVATE).edit()
            .putString("pet_${now}_$reason",JSONObject(prefs.all).toString()).commit())
    }
    fun startNewEgg(now:Long):PetState {
        if(prefs.all.isNotEmpty())archive(now,"new_egg")
        return PetState(updatedAt=now,seed=kotlin.random.Random.nextLong()).also(::save)
    }
}
