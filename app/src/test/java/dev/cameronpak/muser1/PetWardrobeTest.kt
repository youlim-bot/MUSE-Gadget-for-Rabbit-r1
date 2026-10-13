package dev.cameronpak.muser1

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.security.MessageDigest

class PetWardrobeTest {
    private val now=1_800_000_000_000L
    private fun baby()=PetState(updatedAt=now,hatched=true,seed=35,food=50.0)
    @Test fun eightItemsHaveStableDistinctIdsAndTwoPerSlot(){
        assertEquals(8,PetWardrobe.items.map {it.id}.distinct().size)
        PetWardrobeSlot.entries.forEach {assertEquals(2,PetWardrobe.items.count {item->item.slot==it})}
    }
    @Test fun growthRewardsAreDeterministicAndNeverAutomaticallyEquip(){
        val stages=listOf(0L,PetState.DAY,4*PetState.DAY,9*PetState.DAY,21*PetState.DAY)
        val expected=listOf(setOf("beanie"),setOf("beanie","flowers"),setOf("beanie","flowers","sweater"),setOf("beanie","flowers","sweater","sunglasses"))
        stages.forEachIndexed {i,age->val s=PetWardrobe.refresh(baby().copy(ageMs=age),now);assertEquals(expected[minOf(i,3)],s.wardrobe.unlocked.keys);assertTrue(s.wardrobe.equipped.isEmpty());assertEquals(s,PetWardrobe.refresh(s,now+100));assertTrue(s.wardrobe.unlocked.values.all {it==now})}
    }
    @Test fun eggAndMemorialCannotUnlockEquipOrRenderItems(){
        for(s in listOf(baby().copy(hatched=false),baby().copy(dead=true))){assertEquals(s,PetWardrobe.refresh(s,now));assertEquals(s,PetWardrobe.reward(s,"bow",now));assertEquals(s,PetWardrobe.equip(s,PetWardrobeSlot.HEAD,"beanie"));assertTrue(PetWardrobe.visible(s).isEmpty())}
    }
    @Test fun firstAcceptedMealAndBathGrantOnceAndRetainFirstTime(){
        val fed=baby().feed(PetFood.PORRIDGE,now);assertEquals(now,fed.wardrobe.unlocked["bow"])
        val again=fed.feed(PetFood.MILK,now+1000)
        assertEquals(2,again.meals);assertEquals(fed.wardrobe,again.wardrobe)
        val washed=fed.care(PetAction.CLEAN,now+2000);assertEquals(now+2000,washed.wardrobe.unlocked["scarf"])
        assertEquals(washed,washed.care(PetAction.CLEAN,now+3000))
        assertEquals(now+2000,washed.care(PetAction.CLEAN,now+31*60_000).wardrobe.unlocked["scarf"])
    }
    @Test fun rejectedCareDoesNotGrantItems(){
        for(s in listOf(baby().copy(sleeping=true),baby().copy(food=100.0),baby().copy(hatched=false),baby().copy(dead=true))){assertFalse("bow" in s.feed(PetFood.PORRIDGE,now+1000).wardrobe.unlocked)}
        assertFalse("scarf" in baby().copy(sleeping=true).care(PetAction.CLEAN,now).wardrobe.unlocked)
    }
    @Test fun memoryVictoryGrantsGlassesWhileOtherWinsGrantRaincoat(){
        assertEquals(setOf("beanie","glasses"),baby().play(PetGame.MEMORY,true,now).wardrobe.unlocked.keys)
        for(game in listOf(PetGame.STARS,PetGame.RHYTHM))assertEquals(setOf("beanie","raincoat"),baby().play(game,true,now).wardrobe.unlocked.keys)
    }
    @Test fun lossesCooldownSleepAndExhaustionNeverGrantGameRewards(){
        for(game in PetGame.entries){assertTrue(baby().play(game,false,now).wardrobe.unlocked.isEmpty());for(s in listOf(baby().copy(energy=0.0),baby().copy(sleeping=true),baby().copy(playedAt=now))){assertTrue(s.play(game,true,now+1000).wardrobe.unlocked.isEmpty())}}
    }
    @Test fun slotsReplaceOnlyTheirOwnItemAndRemoveIndependently(){
        var s=PetWardrobe.refresh(baby().copy(ageMs=9*PetState.DAY,meals=1,washedAt=now),now)
        s=PetWardrobe.equip(s,PetWardrobeSlot.HEAD,"beanie");s=PetWardrobe.equip(s,PetWardrobeSlot.NECK,"bow");s=PetWardrobe.equip(s,PetWardrobeSlot.HEAD,"flowers")
        assertEquals(mapOf("HEAD" to "flowers","NECK" to "bow"),s.wardrobe.equipped)
        assertEquals(s,PetWardrobe.equip(s,PetWardrobeSlot.BODY,"flowers"));assertEquals(s,PetWardrobe.equip(s,PetWardrobeSlot.FACE,"glasses"))
        s=PetWardrobe.equip(s,PetWardrobeSlot.HEAD,null);assertEquals(mapOf("NECK" to "bow"),s.wardrobe.equipped)
    }
    @Test fun oldRecordLoadsUnchangedWithEmptyWardrobe(){
        val s=baby().copy(meals=4,ageMs=PetState.DAY,washedAt=now-500)
        val raw=JSONObject(PetRecord.encode(s)).apply {remove("wardrobe")}.toString()
        assertEquals(s,PetRecord.decode(raw,now));assertTrue(PetRecord.decode(raw,now).wardrobe.unlocked.isEmpty())
        val migrated=PetWardrobe.refresh(PetRecord.decode(raw,now),now)
        assertEquals(setOf("beanie","flowers","bow","scarf"),migrated.wardrobe.unlocked.keys)
        assertEquals(s,migrated.copy(wardrobe=PetWardrobeState()))
    }
    @Test fun wardrobeRoundTripPreservesUnknownFutureItemsAndSlots(){
        val s=baby().copy(wardrobe=PetWardrobeState(mapOf("beanie" to now,"future_cape" to now-10),mapOf("HEAD" to "beanie","FUTURE" to "future_cape")))
        assertEquals(s,PetRecord.decode(PetRecord.encode(s),now));assertEquals(listOf("beanie"),PetWardrobe.visible(s).map {it.id})
    }
    @Test fun malformedWardrobeTimesAndLockedEquipmentAreDiscarded(){
        val raw=JSONObject(PetRecord.encode(baby())).put("wardrobe",JSONObject().put("version",99).put("unlocked",JSONObject().put("beanie",now).put("scarf",-1).put("bow","NaN")).put("equipped",JSONObject().put("HEAD","beanie").put("NECK","scarf"))).toString()
        val w=PetRecord.decode(raw,now).wardrobe;assertEquals(mapOf("beanie" to now),w.unlocked);assertEquals(mapOf("HEAD" to "beanie"),w.equipped)
    }
    @Test fun wrongSlotInImportedRecordCannotRender(){
        val s=baby().copy(wardrobe=PetWardrobeState(mapOf("beanie" to now),mapOf("FACE" to "beanie")));assertTrue(PetWardrobe.visible(s).isEmpty())
    }
    @Test fun invalidUnlockTimeFailsBeforeReplacingSavedRecord(){
        assertThrows(IllegalArgumentException::class.java){PetRecord.encode(baby().copy(wardrobe=PetWardrobeState(mapOf("beanie" to Long.MAX_VALUE))))}
    }
    @Test fun adultOutfitAndRemovalPreserveIdentityNeedsAndOriginalPng(){
        val original=File("src/main/res/drawable-nodpi/pet_adult.png").readBytes()
        val before=baby().copy(ageMs=9*PetState.DAY);var s=PetWardrobe.refresh(before,now)
        s=s.play(PetGame.STARS,true,now);s=PetWardrobe.equip(s,PetWardrobeSlot.BODY,"raincoat");s=PetWardrobe.equip(s,PetWardrobeSlot.HEAD,"beanie")
        val dressed=s;PetWardrobeSlot.entries.forEach {s=PetWardrobe.equip(s,it,null)}
        assertEquals(dressed.copy(wardrobe=s.wardrobe),s);assertTrue(PetWardrobe.visible(s).isEmpty());assertArrayEquals(original,File("src/main/res/drawable-nodpi/pet_adult.png").readBytes())
        assertEquals("4beb9bc2ebe6d2664e5571f850d677222e0f92ae0989e65ec3f77c0b656ccfaf",MessageDigest.getInstance("SHA-256").digest(original).joinToString(""){"%02x".format(it)})
    }
}
