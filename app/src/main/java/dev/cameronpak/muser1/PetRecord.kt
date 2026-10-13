package dev.cameronpak.muser1

import org.json.JSONObject

/** The existing life_v2 JSON format, independent of Android storage for migration tests. */
internal object PetRecord {
    const val MAX_TIME = 100_000_000_000_000L
    const val MAX_TRAIT = PetState.MAX_TRAIT

    fun decode(raw:String,now:Long,resets:List<Long> = emptyList()):PetState {
        // Parsing failure propagates without creating a replacement life or writing preferences.
        val j=JSONObject(raw)
        fun d(k:String,default:Double,max:Double=100.0):Double {
            val value=j.optDouble(k,default)
            return if(value.isFinite())value.coerceIn(0.0,max) else default
        }
        fun l(k:String,default:Long=0)=j.optLong(k,default).coerceIn(0L,MAX_TIME)
        fun count(k:String,max:Int=Int.MAX_VALUE)=j.optLong(k,0).coerceIn(0L,max.toLong()).toInt()
        val updated=l("updatedAt",now)
        val seed=j.optLong("seed") // All signed 64-bit seeds remain valid life identities.
        val duration=48*PetState.HOUR+Math.floorMod(seed,24*PetState.HOUR)
        val incubation=l("incubationMs").coerceAtMost(duration)
        val age=l("ageMs")
        val savedStart=j.optLong("startedAt",0).takeIf {it in 1..MAX_TIME}?:0L
        val start=PetElapsed.restoreStart(savedStart,updated,incubation,age,j.optBoolean("hatched"),seed,
            resets.filter {it in 1..MAX_TIME})
        return PetState(food=d("food",80.0),mood=d("mood",80.0),energy=d("energy",80.0),clean=d("clean",80.0),
            health=d("health",100.0),warmth=d("warmth",50.0),bond=count("bond",100),
            hatched=j.optBoolean("hatched"),sleeping=j.optBoolean("sleeping"),dead=j.optBoolean("dead"),
            updatedAt=updated,seed=seed,ageMs=age,startedAt=start.first,
            startEstimated=if(savedStart>0)j.optBoolean("startEstimated") else start.second,
            incubationMs=incubation,eggCare=count("eggCare",1000),eggCareAt=l("eggCareAt"),
            starvingMs=l("starvingMs"),lonelyMs=l("lonelyMs"),sleepMs=l("sleepMs"),diedAt=l("diedAt"),
            rewardAt=l("rewardAt"),fedAt=l("fedAt"),playedAt=l("playedAt"),washedAt=l("washedAt"),medicineAt=l("medicineAt"),
            curiosity=d("curiosity",0.0,MAX_TRAIT),vigor=d("vigor",0.0,MAX_TRAIT),dietMask=(j.optLong("dietMask").coerceAtLeast(0) and 63L).toInt(),
            sweets=count("sweets"),meals=count("meals"),
            form=PetForm.entries.firstOrNull {it.name==j.optString("form")}?:PetForm.UNDECIDED,
            wardrobe=decodeWardrobe(j.optJSONObject("wardrobe")))
    }

    private fun decodeWardrobe(j:JSONObject?):PetWardrobeState {
        val unlocked=j?.optJSONObject("unlocked")
        val equipped=j?.optJSONObject("equipped")
        val times=unlocked?.keys()?.asSequence()?.mapNotNull {id->
            val value=unlocked.opt(id)
            if(value is Number && value.toDouble().isFinite() && value.toDouble() in 0.0..MAX_TIME.toDouble())id to value.toLong() else null
        }?.toMap().orEmpty()
        // Keep future slots/items, but malformed or locked equipment never reaches the renderer.
        val slots=equipped?.keys()?.asSequence()?.mapNotNull {slot->
            val id=equipped.opt(slot) as? String
            if(id!=null && id in times)slot to id else null
        }?.toMap().orEmpty()
        return PetWardrobeState(times,slots)
    }

    fun encode(s:PetState):String {
        // Validate before encoding, and therefore before PetStore can replace the last saved record.
        require(listOf(s.food,s.mood,s.energy,s.clean,s.health,s.warmth).all {it.isFinite() && it in 0.0..100.0} &&
            listOf(s.curiosity,s.vigor).all {it.isFinite() && it in 0.0..MAX_TRAIT}) {"Invalid pet needs or traits"}
        require(listOf(s.updatedAt,s.rewardAt,s.incubationMs,s.eggCareAt,s.ageMs,s.starvingMs,s.lonelyMs,
            s.fedAt,s.playedAt,s.washedAt,s.medicineAt,s.sleepMs,s.diedAt,s.startedAt).all {it in 0..MAX_TIME} &&
            s.incubationMs<=s.hatchDuration) {"Invalid pet time"}
        require(s.bond in 0..100 && s.eggCare in 0..1000 && s.dietMask in 0..63 && s.sweets>=0 && s.meals>=0) {"Invalid pet counters"}
        val j=JSONObject()
        mapOf("food" to s.food,"mood" to s.mood,"energy" to s.energy,"clean" to s.clean,
            "health" to s.health,"warmth" to s.warmth,"bond" to s.bond,"hatched" to s.hatched,
            "sleeping" to s.sleeping,"dead" to s.dead,"updatedAt" to s.updatedAt,"seed" to s.seed,
            "ageMs" to s.ageMs,"incubationMs" to s.incubationMs,"eggCare" to s.eggCare,"eggCareAt" to s.eggCareAt,
            "starvingMs" to s.starvingMs,"lonelyMs" to s.lonelyMs,"sleepMs" to s.sleepMs,"diedAt" to s.diedAt,
            "rewardAt" to s.rewardAt,"fedAt" to s.fedAt,"playedAt" to s.playedAt,"washedAt" to s.washedAt,
            "medicineAt" to s.medicineAt,"curiosity" to s.curiosity,"vigor" to s.vigor,"dietMask" to s.dietMask,
            "sweets" to s.sweets,"meals" to s.meals,"form" to s.form.name,
            "startedAt" to s.startedAt,"startEstimated" to s.startEstimated).forEach{(k,v)->j.put(k,v)}
        require(s.wardrobe.unlocked.values.all {it in 0..MAX_TIME}) {"Invalid wardrobe time"}
        j.put("wardrobe",JSONObject().put("version",1).put("unlocked",JSONObject(s.wardrobe.unlocked)).put("equipped",JSONObject(s.wardrobe.equipped)))
        return j.toString()
    }
}
