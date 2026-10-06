package dev.cameronpak.muser1
import org.junit.Assert.*
import org.junit.Test
class PetLifecycleTest {
    private val now=1_800_000_000_000L
    @Test fun everyEggNeedsDaysButNoSpecificCareChecklist() {
        for(seed in listOf(0L,5L,-1L,Long.MIN_VALUE,Long.MAX_VALUE)) {
            val egg=PetState(updatedAt=now,seed=seed)
            assertTrue(egg.hatchDuration>=48*PetState.HOUR);assertTrue(egg.hatchDuration<72*PetState.HOUR)
            assertFalse(egg.advance(now+47*PetState.HOUR).hatchReady)
            assertTrue(egg.advance(now+72*PetState.HOUR).hatchReady)
        }
    }
    @Test fun readyEggWaitsForFirstMeetingBeforeNewbornNeedsStart() {
        val egg=PetState(updatedAt=now).advance(now+30*PetState.DAY)
        assertTrue(egg.hatchReady);assertFalse(egg.hatched);assertFalse(egg.dead)
        val baby=egg.hatch(now+30*PetState.DAY)
        assertEquals(1,baby.level);assertEquals(0L,baby.ageMs);assertEquals(80.0,baby.food,0.0)
        assertEquals(baby,baby.hatch(now+31*PetState.DAY))
    }
    @Test fun tappingAndConversationNeverAccelerateIncubation() {
        var egg=PetState(updatedAt=now,seed=12)
        repeat(1000){egg=egg.care(PetAction.entries[it%4],now+it).agentInteraction(now+it)}
        assertFalse(egg.hatchReady);assertEquals(1,egg.eggCare);assertEquals(0,egg.level)
        assertEquals(999L,egg.incubationMs);assertFalse(egg.hatch(now+1000).hatched)
    }
    @Test fun incubationPersistsFractionalOfflineTimeAndRejectsRollback() {
        val egg=PetState(updatedAt=now,seed=3).advance(now+1234)
        assertEquals(egg,egg.advance(now));assertEquals(2468L,egg.advance(now+2468).incubationMs)
    }
    @Test fun stagesSpanDaysAndWeeks() {
        val s=PetState(hatched=true,updatedAt=now)
        assertEquals(1,s.level);assertEquals(2,s.copy(ageMs=PetState.DAY).level)
        assertEquals(3,s.copy(ageMs=4*PetState.DAY).level);assertEquals(4,s.copy(ageMs=9*PetState.DAY).level)
        assertEquals(5,s.copy(ageMs=21*PetState.DAY).level)
    }
    @Test fun animationVariesAndRespectsReducedMotion() {
        assertNotEquals(PetMotion.pose(1000,2,false,"",0,true),PetMotion.pose(1800,2,false,"",0,true))
        assertEquals(PetMotion.Pose(),PetMotion.pose(1800,2,false,"heart",100,false))
        assertTrue(PetMotion.pose(1200,1,true,"",0,true).blink)
    }
}
