package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class PetElapsedTest {
    private val zone=ZoneId.of("Asia/Tokyo")
    private fun time(s:String)=ZonedDateTime.parse(s).toInstant().toEpochMilli()
    @Test fun elapsedUsesCalendarYearsMonthsDaysAndHours() {
        assertEquals(PetElapsed(1,2,3,4),PetElapsed.between(time("2024-01-15T10:00:00+09:00"),time("2025-03-18T14:59:00+09:00"),zone))
    }
    @Test fun leapDayAndMonthEndsDoNotUseThirtyDayMonths() {
        assertEquals(PetElapsed(0,1,0,0),PetElapsed.between(time("2024-01-31T10:00:00+09:00"),time("2024-02-29T10:00:00+09:00"),zone))
        assertEquals(PetElapsed(1,0,0,0),PetElapsed.between(time("2024-02-29T10:00:00+09:00"),time("2025-02-28T10:00:00+09:00"),zone))
    }
    @Test fun localDaySurvivesDaylightSaving() {
        val start=ZonedDateTime.of(2026,3,7,12,0,0,0,ZoneId.of("America/New_York"))
        assertEquals(PetElapsed(0,0,1,3),PetElapsed.between(start.toInstant().toEpochMilli(),start.plusDays(1).plusHours(3).toInstant().toEpochMilli(),start.zone))
    }
    @Test fun rollbackAndUnknownStartNeverShowNegativeTime() {
        assertEquals(PetElapsed(0,0,0,0),PetElapsed.between(1000,900,zone))
        assertEquals(PetElapsed(0,0,0,0),PetElapsed.between(0,900,zone))
    }
    @Test fun storedStartAndLatestValidResetHavePriority() {
        assertEquals(1000L to false,PetElapsed.restoreStart(1000,5000,200,0,false,1,listOf(3000)))
        assertEquals(3000L to false,PetElapsed.restoreStart(0,5000,200,0,false,1,listOf(1000,3000,6000)))
    }
    @Test fun uncappedEggTimeCanRecoverAnExactStart() {
        assertEquals(1000L to false,PetElapsed.restoreStart(0,5000,4000,0,false,1,emptyList()))
    }
    @Test fun CappedOrHatchedLegacyRecordsAreMarkedEstimated() {
        val h=48*PetState.HOUR
        assertTrue(PetElapsed.restoreStart(0,h*2,h,0,false,0,emptyList()).second)
        assertTrue(PetElapsed.restoreStart(0,h*3,h,h,true,0,emptyList()).second)
    }
    @Test fun startSurvivesHatchingCareAndTimeAdvance() {
        val now=1_800_000_000_000L
        val egg=PetState(updatedAt=now,startedAt=now-3*PetState.DAY,incubationMs=72*PetState.HOUR,seed=7)
        val baby=egg.hatch(now)
        assertTrue(baby.hatched);assertEquals(egg.startedAt,baby.agentInteraction(now).advance(now+PetState.HOUR).startedAt)
    }
    @Test fun allThreeLanguagesIncludeAllFourUnitsAndEstimatedMarker() {
        val elapsed=PetElapsed(0,0,2,3)
        assertEquals("0년 0개월 2일 3시간",elapsed.text(DisplayLanguage.KO))
        assertEquals("0年 0か月 2日 3時間",elapsed.text(DisplayLanguage.JA))
        assertEquals("~0y 0mo 2d 3h",elapsed.text(DisplayLanguage.EN,true))
    }
}
