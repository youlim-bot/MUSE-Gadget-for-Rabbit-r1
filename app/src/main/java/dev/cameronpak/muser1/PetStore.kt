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
                ageMs=age,bond=prefs.getInt("bond",0).coerceIn(0,100)).also(::save)
        }
        val resets=context.getSharedPreferences("muse_pet_archive",Context.MODE_PRIVATE).all.keys.mapNotNull {key->
            Regex("pet_([0-9]+)_new_egg").matchEntire(key)?.groupValues?.get(1)?.toLongOrNull()
        }
        val restored=PetRecord.decode(raw,now,resets).advance(now)
        val awarded=PetWardrobe.refresh(restored,now)
        if(awarded.wardrobe!=restored.wardrobe)save(awarded)
        return awarded
    }
    fun save(s:PetState) {
        val raw=PetRecord.encode(s)
        prefs.edit().putString("life_v2",raw).apply()
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
