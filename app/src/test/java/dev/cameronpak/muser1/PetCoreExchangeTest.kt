package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

/** Regression scenarios shared with the portable C core, plus Android save migration. */
class PetCoreExchangeTest {
    private val now=1_800_000_000_000L
    @Test fun fractionalStepSurvivesSaveLoadAndRollbackWithoutReplay() {
        val baby=PetState(updatedAt=now,hatched=true)
        val partial=baby.advance(now+PetState.STEP+1234)
        assertEquals(now+PetState.STEP,partial.updatedAt)
        val loaded=PetRecord.decode(PetRecord.encode(partial),now+PetState.STEP+1234)
        assertEquals(loaded,loaded.advance(now))
        assertEquals(baby.advance(now+2*PetState.STEP),loaded.advance(now+2*PetState.STEP))
    }
    @Test fun incubationAddsOnlyRemainingTimeBeforeCapping() {
        val egg=PetState(updatedAt=1,incubationMs=PetState.HOUR,seed=Long.MIN_VALUE)
        val ready=egg.advance(Long.MAX_VALUE)
        assertEquals(egg.hatchDuration,ready.incubationMs)
        assertTrue(ready.hatchReady);assertFalse(ready.hatched);assertFalse(ready.dead)
        assertEquals(egg.seed,ready.seed)
        assertEquals(ready,ready.advance(1))
    }
    @Test fun guardsUseAdvancedNeedsRatherThanStaleSnapshot() {
        val baby=PetState(updatedAt=now,hatched=true)
        val t=now+PetState.STEP
        val tired=baby.copy(energy=10.0)
        assertEquals(tired.advance(t),tired.play(PetGame.STARS,true,t))
        val full=baby.copy(food=95.1)
        assertEquals(1,full.feed(PetFood.FISH,t).meals)
        val becomingIll=baby.copy(food=0.0,health=45.01,starvingMs=8*PetState.HOUR)
        assertTrue(becomingIll.medicine(t).health>becomingIll.advance(t).health)
        val sleeping=baby.copy(sleeping=true,food=20.0)
        assertEquals(1,sleeping.feed(PetFood.FISH,now+8*PetState.HOUR).meals)
    }
    @Test fun feedingVectorsUseFullnessRatherThanLastMealTime() {
        val baby=PetState(updatedAt=now,hatched=true,food=70.0,fedAt=now)
        val first=baby.feedResult(PetFood.CAKE,now)
        assertTrue(first.accepted);assertEquals(82.0,first.state.food,0.0)
        val second=first.state.feedResult(PetFood.CAKE,now)
        assertTrue(second.accepted);assertEquals(94.0,second.state.food,0.0)
        val third=second.state.feedResult(PetFood.CAKE,now)
        assertTrue(third.accepted);assertEquals(100.0,third.state.food,0.0)
        assertEquals(PetFeedRefusal.FULL,third.state.feedResult(PetFood.CAKE,now).refusal)
        assertEquals(PetFeedRefusal.SLEEPING,baby.copy(sleeping=true).feedResult(PetFood.FISH,now).refusal)
        assertEquals(PetFeedRefusal.EGG,baby.copy(hatched=false).feedResult(PetFood.FISH,now).refusal)
        assertEquals(PetFeedRefusal.DEAD,baby.copy(dead=true,diedAt=now).feedResult(PetFood.FISH,now).refusal)
        val wakes=baby.copy(sleeping=true,sleepMs=8*PetState.HOUR-PetState.STEP).feedResult(PetFood.FISH,now+PetState.STEP)
        assertTrue(wakes.accepted)
        val refused=third.state.feedResult(PetFood.FISH,now+PetState.STEP)
        assertEquals(PetFeedRefusal.FULL,refused.refusal)
        assertEquals(now+PetState.STEP,refused.state.updatedAt)
    }
    @Test fun failedCareStillReturnsElapsedChangesAndCannotResurrectDeath() {
        val start=PetState(updatedAt=now,hatched=true,food=0.0,health=0.1,starvingMs=8*PetState.HOUR)
        val t=now+PetState.STEP
        val dead=start.advance(t)
        assertTrue(dead.dead);assertEquals(t,dead.diedAt)
        assertEquals(dead,start.feed(PetFood.FISH,t))
        assertEquals(dead,start.play(PetGame.STARS,true,t))
        assertEquals(dead,start.medicine(t))
        assertEquals(dead,start.agentInteraction(t))
        for(action in PetAction.entries)assertEquals(dead,start.care(action,t))
        assertEquals(dead,PetRecord.decode(PetRecord.encode(dead),t))
    }
    @Test fun legacyStartInferenceCannotOverflowToPlausiblePositiveStart() {
        val start=PetElapsed.restoreStart(0,5000,Long.MAX_VALUE,Long.MAX_VALUE,false,0,emptyList())
        assertEquals(1L to true,start)
        val invalidNegative=PetElapsed.restoreStart(0,-5,0,0,false,0,emptyList())
        assertEquals(1L to true,invalidNegative)
    }
}
