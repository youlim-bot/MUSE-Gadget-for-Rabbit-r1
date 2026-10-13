package dev.cameronpak.muser1

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class PetRecordTest {
    private val now=1_800_000_000_000L
    @Test fun consecutiveMealsSurviveReloadWithoutReintroducingCooldown() {
        val first=PetState(updatedAt=now,hatched=true,food=40.0).feed(PetFood.CAKE,now)
        val loaded=PetRecord.decode(PetRecord.encode(first),now)
        val second=loaded.feed(PetFood.CAKE,now)
        assertEquals(2,second.meals);assertEquals(64.0,second.food,0.0)
        assertEquals(first.wardrobe,second.wardrobe)
        assertEquals(second,PetRecord.decode(PetRecord.encode(second),now))
    }
    @Test fun existingLifeV2RoundTripRetainsAllSavedFieldsAndSignedSeed() {
        for(seed in listOf(Long.MIN_VALUE,Long.MAX_VALUE,-1L,0L)) {
            val s=PetState(updatedAt=now,seed=seed,hatched=true,food=23.75,mood=42.5,energy=51.0,
                clean=65.0,health=35.0,warmth=77.0,bond=25,sleeping=true,rewardAt=now-5,
                incubationMs=48*PetState.HOUR,eggCare=4,eggCareAt=now-8,ageMs=22*PetState.DAY,
                starvingMs=PetState.HOUR,lonelyMs=2*PetState.HOUR,curiosity=12.5,vigor=6.5,
                dietMask=37,sweets=2,meals=20,fedAt=now-4,playedAt=now-3,washedAt=now-2,
                medicineAt=now-1,sleepMs=PetState.HOUR,form=PetForm.COMPANION,dead=true,diedAt=now,
                startedAt=now-30*PetState.DAY,startEstimated=true)
            val raw=PetRecord.encode(s)
            assertEquals(s,PetRecord.decode(raw,now))
            assertEquals(seed,JSONObject(raw).getLong("seed"))
            assertFalse(JSONObject(raw).has("version"))
        }
    }
    @Test fun missingLegacyStartUsesResetOrEstimateWithoutChangingIdentityAndCare() {
        val j=JSONObject(PetRecord.encode(PetState(updatedAt=now,seed=-5,hatched=true,
            ageMs=PetState.DAY,food=28.0,bond=7))).apply {remove("startedAt");remove("startEstimated")}
        val exact=PetRecord.decode(j.toString(),now,listOf(now-3*PetState.DAY))
        assertEquals(now-3*PetState.DAY,exact.startedAt);assertFalse(exact.startEstimated)
        val inferred=PetRecord.decode(j.toString(),now)
        assertTrue(inferred.startEstimated);assertEquals(now-PetState.DAY,inferred.startedAt)
        assertEquals(-5L,inferred.seed);assertEquals(28.0,inferred.food,0.0);assertEquals(7,inferred.bond)
        assertEquals(inferred,PetRecord.decode(PetRecord.encode(inferred),now))
    }
    @Test fun nonFiniteNeedsAndTraitsUseDefaultsAndFiniteValuesAreBounded() {
        val j=JSONObject().put("updatedAt",now).put("seed",42)
            .put("food","NaN").put("health","Infinity").put("curiosity","-Infinity")
            .put("vigor",1e30).put("mood",-3).put("clean",200)
        val s=PetRecord.decode(j.toString(),now)
        assertEquals(80.0,s.food,0.0);assertEquals(100.0,s.health,0.0)
        assertEquals(0.0,s.curiosity,0.0);assertEquals(PetRecord.MAX_TRAIT,s.vigor,0.0)
        assertEquals(0.0,s.mood,0.0);assertEquals(100.0,s.clean,0.0)
        assertEquals(s,PetRecord.decode(PetRecord.encode(s),now))
    }
    @Test fun oversizedCountersTimesAndUnknownFormLoadSafelyWithoutCreatingNewLife() {
        val j=JSONObject().put("updatedAt",now).put("seed",Long.MIN_VALUE).put("bond",Long.MAX_VALUE)
            .put("eggCare",Long.MAX_VALUE).put("meals",Long.MAX_VALUE).put("sweets",-1)
            .put("dietMask",Long.MAX_VALUE).put("ageMs",Long.MAX_VALUE).put("rewardAt",-1)
            .put("fedAt",Long.MAX_VALUE).put("incubationMs",Long.MAX_VALUE).put("form","UNKNOWN")
        val s=PetRecord.decode(j.toString(),now)
        assertEquals(Long.MIN_VALUE,s.seed);assertEquals(100,s.bond);assertEquals(1000,s.eggCare)
        assertEquals(Int.MAX_VALUE,s.meals);assertEquals(0,s.sweets);assertEquals(63,s.dietMask)
        assertEquals(PetRecord.MAX_TIME,s.ageMs);assertEquals(0L,s.rewardAt)
        assertEquals(PetRecord.MAX_TIME,s.fedAt);assertEquals(s.hatchDuration,s.incubationMs)
        assertEquals(PetForm.UNDECIDED,s.form);assertTrue(s.hatchReady)
        assertEquals(s,PetRecord.decode(PetRecord.encode(s),now))
        assertEquals(0,PetRecord.decode(j.put("dietMask",64).toString(),now).dietMask)
    }
    @Test fun oldReadyEggAboveDurationStillLoadsAndWaitsForExplicitHatch() {
        val raw=JSONObject().put("updatedAt",now).put("seed",0).put("incubationMs",72*PetState.HOUR).toString()
        val egg=PetRecord.decode(raw,now).advance(now+30*PetState.DAY)
        assertTrue(egg.hatchReady);assertFalse(egg.hatched);assertFalse(egg.dead)
        assertEquals(egg.seed,egg.hatch(now+30*PetState.DAY).seed)
        assertEquals(0L,egg.hatch(now+30*PetState.DAY).ageMs)
    }
    @Test fun invalidSaveIsRejectedBeforeReplacingPreviousRecord() {
        val good=PetRecord.encode(PetState(updatedAt=now,seed=9))
        for(bad in listOf(PetState(updatedAt=now,food=Double.NaN),PetState(updatedAt=now,health=101.0),
            PetState(updatedAt=-1),PetState(updatedAt=now,ageMs=Long.MAX_VALUE),
            PetState(updatedAt=now,bond=101),PetState(updatedAt=now,dietMask=64),
            PetState(updatedAt=now,meals=-1),PetState(updatedAt=now,curiosity=Double.POSITIVE_INFINITY))) {
            var stored=good
            assertThrows(IllegalArgumentException::class.java) {stored=PetRecord.encode(bad)}
            assertEquals(good,stored)
        }
    }
    @Test fun malformedJsonFailsWithoutInventingReplacementState() {
        assertThrows(org.json.JSONException::class.java) {PetRecord.decode("{broken",now)}
    }
    @Test fun actionAtMaximumCountersAndTraitsRemainsSerializable() {
        val s=PetState(updatedAt=now,hatched=true,food=20.0,meals=Int.MAX_VALUE,sweets=Int.MAX_VALUE,
            curiosity=PetRecord.MAX_TRAIT,vigor=PetRecord.MAX_TRAIT)
        val acted=s.feed(PetFood.CAKE,now).play(PetGame.MEMORY,true,now).agentInteraction(now)
        assertEquals(Int.MAX_VALUE,acted.meals);assertEquals(Int.MAX_VALUE,acted.sweets)
        assertEquals(PetRecord.MAX_TRAIT,acted.curiosity,0.0)
        val active=s.feed(PetFood.FISH,now).play(PetGame.RHYTHM,true,now)
        assertEquals(PetRecord.MAX_TRAIT,active.vigor,0.0)
        assertEquals(acted,PetRecord.decode(PetRecord.encode(acted),now))
        assertEquals(active,PetRecord.decode(PetRecord.encode(active),now))
    }
}
