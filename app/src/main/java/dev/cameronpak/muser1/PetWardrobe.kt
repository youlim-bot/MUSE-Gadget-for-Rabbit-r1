package dev.cameronpak.muser1

/** Stable string IDs are shared with smaller devices; unknown IDs survive save/reload. */
data class PetWardrobeState(
    val unlocked: Map<String, Long> = emptyMap(),
    val equipped: Map<String, String> = emptyMap()
)

enum class PetWardrobeSlot { HEAD, NECK, BODY, FACE }

internal data class PetWardrobeItem(val id:String,val slot:PetWardrobeSlot,val growth:Int=0)

internal object PetWardrobe {
    val items=listOf(
        PetWardrobeItem("beanie",PetWardrobeSlot.HEAD,1),
        PetWardrobeItem("flowers",PetWardrobeSlot.HEAD,2),
        PetWardrobeItem("bow",PetWardrobeSlot.NECK),
        PetWardrobeItem("scarf",PetWardrobeSlot.NECK),
        PetWardrobeItem("raincoat",PetWardrobeSlot.BODY),
        PetWardrobeItem("sweater",PetWardrobeSlot.BODY,3),
        PetWardrobeItem("glasses",PetWardrobeSlot.FACE),
        PetWardrobeItem("sunglasses",PetWardrobeSlot.FACE,4)
    )
    fun refresh(s:PetState,now:Long):PetState {
        if(!s.hatched || s.dead)return s
        // Old saves earn only rewards evidenced by existing fields; no invented game wins.
        val ids=items.filter {it.growth in 1..s.level}.map {it.id}+
            (if(s.meals>0)listOf("bow")else emptyList())+
            (if(s.washedAt>0)listOf("scarf")else emptyList())
        return grant(s,ids,now)
    }
    fun reward(s:PetState,id:String,now:Long):PetState = refresh(grant(s,listOf(id),now),now)
    private fun grant(s:PetState,ids:List<String>,now:Long):PetState {
        if(!s.hatched || s.dead)return s
        val fresh=ids.filter {it !in s.wardrobe.unlocked}
        if(fresh.isEmpty())return s
        return s.copy(wardrobe=s.wardrobe.copy(unlocked=s.wardrobe.unlocked+fresh.associateWith {now.coerceIn(0,PetRecord.MAX_TIME)}))
    }
    fun equip(s:PetState,slot:PetWardrobeSlot,id:String?):PetState {
        if(!s.hatched || s.dead)return s
        if(id!=null && (id !in s.wardrobe.unlocked || items.none {it.id==id && it.slot==slot}))return s
        val next=if(id==null)s.wardrobe.equipped-slot.name else s.wardrobe.equipped+(slot.name to id)
        return s.copy(wardrobe=s.wardrobe.copy(equipped=next))
    }
    fun visible(s:PetState):List<PetWardrobeItem> = if(!s.hatched || s.dead)emptyList()else items.filter {
        it.id in s.wardrobe.unlocked && s.wardrobe.equipped[it.slot.name]==it.id
    }
}
