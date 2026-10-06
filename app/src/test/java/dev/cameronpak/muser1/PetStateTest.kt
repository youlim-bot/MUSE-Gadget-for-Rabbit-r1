package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class PetStateTest {
    private val now=1_800_000_000_000L
    private val h=PetState.HOUR
    private fun baby()=PetState(hatched=true,updatedAt=now,seed=7)
    @Test fun shortAbsenceIsSafeButLongStarvationIsFatal() {
        val rested=baby().advance(now+12*h)
        assertFalse(rested.dead);assertTrue(rested.food>0)
        val abandoned=baby().advance(now+7*PetState.DAY)
        assertTrue(abandoned.dead);assertTrue(abandoned.diedAt>now+2*PetState.DAY)
        assertEquals(abandoned,abandoned.feed(PetFood.FISH,now+8*PetState.DAY))
        assertEquals(abandoned,abandoned.agentInteraction(now+8*PetState.DAY))
    }
    @Test fun foregroundTicksAndOfflineCatchupAgreeExactly() {
        val start=baby().copy(food=3.0,clean=3.0)
        var frequent=start
        for(minute in 1..720) frequent=frequent.advance(now+minute*60_000L)
        assertEquals(start.advance(now+12*h),frequent)
        assertEquals(frequent,frequent.advance(now+12*h))
    }
    @Test fun sleepAutomaticallyEndsAndCannotPreventStarvationForever() {
        val s=baby().copy(energy=5.0).care(PetAction.SLEEP,now)
        assertEquals(s,s.feed(PetFood.MILK,now))
        val rested=s.advance(now+8*h)
        assertFalse(rested.sleeping);assertTrue(rested.energy>=99)
        assertTrue(s.advance(now+10*PetState.DAY).dead)
    }
    @Test fun foodVarietyAndTreatsHaveDifferentEffects() {
        val hungry=baby().copy(food=20.0,health=80.0)
        val fish=hungry.feed(PetFood.FISH,now)
        val cake=hungry.feed(PetFood.CAKE,now)
        assertTrue(fish.food>cake.food);assertTrue(fish.health>cake.health)
        assertTrue(cake.mood>fish.mood);assertEquals(1,cake.sweets)
        assertEquals(2,Integer.bitCount(fish.feed(PetFood.VEGETABLES,now+h).dietMask))
        assertEquals(1,fish.feed(PetFood.FISH,now+1000).meals)
    }
    @Test fun neglectChangesPersonalityAndPatientCareCanRecoverIt() {
        val lonely=baby().advance(now+49*h)
        assertEquals(PetTemperament.GRUMPY,lonely.temperament)
        var s=lonely
        repeat(6){i->val t=now+49*h+i*h;s=s.copy(energy=80.0).play(PetGame.MEMORY,true,t).feed(PetFood.FISH,t)}
        assertTrue(s.warmth>lonely.warmth);assertNotEquals(PetTemperament.GRUMPY,s.temperament)
    }
    @Test fun ageNotButtonTapsDeterminesStageAndCareDeterminesAdultForm() {
        var young=baby()
        repeat(1000){young=young.agentInteraction(now+it)}
        assertEquals(1,young.level)
        val verge=baby().copy(ageMs=9*PetState.DAY-PetState.STEP)
        assertEquals(PetForm.EXPLORER,verge.copy(curiosity=50.0).advance(now+PetState.STEP).form)
        assertEquals(PetForm.SPRINTER,verge.copy(vigor=50.0).advance(now+PetState.STEP).form)
        assertEquals(PetForm.WILD,verge.copy(warmth=10.0).advance(now+PetState.STEP).form)
        val adult=verge.copy(curiosity=50.0).advance(now+PetState.STEP)
        assertEquals(PetForm.EXPLORER,adult.copy(vigor=100.0).advance(now+2*PetState.STEP).form)
    }
    @Test fun feedingAndMedicineCanSaveAnIllLivingPet() {
        val ill=baby().copy(health=15.0,food=0.0,starvingMs=8*h)
        val helped=ill.feed(PetFood.PORRIDGE,now).medicine(now).care(PetAction.CLEAN,now).advance(now+h)
        assertFalse(helped.dead);assertTrue(helped.health>ill.health);assertEquals(0L,helped.starvingMs)
        assertEquals(helped.health,helped.medicine(now+h).health,0.0)
    }
    @Test fun rollbackCannotReplayTimeAndHugeAbsenceTerminates() {
        val s=baby().advance(now+h)
        assertEquals(s,s.advance(now));assertEquals(s,s.advance(now+h))
        assertTrue(baby().advance(now+100*365*PetState.DAY).dead)
    }
    @Test fun conversationCountsAsCompanionshipAndLosingStillCountsAsPlay() {
        val s=baby().copy(lonelyMs=20*h)
        val talked=s.agentInteraction(now)
        assertEquals(0L,talked.lonelyMs);assertTrue(talked.curiosity>s.curiosity)
        assertEquals(talked,talked.agentInteraction(now+1000))
        val loss=s.play(PetGame.RHYTHM,false,now)
        assertTrue(loss.mood>s.mood);assertTrue(loss.vigor>s.vigor)
        assertEquals(loss,loss.play(PetGame.RHYTHM,true,now+1000))
    }
}
