package dev.cameronpak.muser1
import org.junit.Assert.*
import org.junit.Test
import java.time.*
class LocalClockCommandTest {
    private val friday=ZonedDateTime.of(2026,10,9,18,30,0,0,ZoneId.of("Asia/Tokyo"))
    @Test fun durationLanguages(){for(s in listOf("3분 타이머","타이머 3분","3分タイマー","set a timer for three minutes","삼 분 타이머"))assertEquals(s,180,LocalClockCommand.parse(s)!!.seconds)}
    @Test fun combinedUnits(){assertEquals(3630,LocalClockCommand.parse("1시간 30초 타이머")!!.seconds)}
    @Test fun boundedDurations(){for(s in listOf("0초 타이머","25시간 타이머","9999999999999999999초 타이머","1.5분 타이머"))assertEquals(s,"help",LocalClockCommand.parse(s)!!.kind)}
    @Test fun morningTomorrow(){val c=LocalClockCommand.parse("내일 아침 7시에 깨워줘")!!;assertEquals(7,c.hour);assertEquals("tomorrow",c.day);assertEquals(friday.toLocalDate().plusDays(1),Instant.ofEpochMilli(LocalClockCommand.due(c,friday)).atZone(friday.zone).toLocalDate())}
    @Test fun afternoonAndMidnight(){assertEquals(19,LocalClockCommand.parse("오후 7시 알람")!!.hour);assertEquals(0,LocalClockCommand.parse("오전 12시 알람")!!.hour);assertEquals(12,LocalClockCommand.parse("오후 12시 알람")!!.hour)}
    @Test fun weekdaySkipsWeekend(){val c=LocalClockCommand.parse("평일 오전 8시 알람")!!;assertEquals(DayOfWeek.MONDAY,Instant.ofEpochMilli(LocalClockCommand.due(c,friday)).atZone(friday.zone).dayOfWeek)}
    @Test fun englishJapanese(){assertEquals(19,LocalClockCommand.parse("set alarm at 7 pm")!!.hour);assertEquals(30,LocalClockCommand.parse("明日午前7時半に起こして")!!.minute)}
    @Test fun pastTodayNotSilentlyTomorrow(){assertTrue(runCatching{LocalClockCommand.due(ClockCommand("alarm",hour=7,day="today"),friday)}.isFailure)}
    @Test fun invalidClock(){for(s in listOf("25시 알람","7시 99분 알람","오후 19시 알람"))assertEquals("help",LocalClockCommand.parse(s)!!.kind)}
    @Test fun translationAndOrdinaryConversation(){assertNull(LocalClockCommand.parse("3분 타이머",true));assertNull(LocalClockCommand.parse("오늘 날씨 어때"))}
    @Test fun cancellationOpensLocalManager(){assertEquals("manage",LocalClockCommand.parse("타이머 취소해줘")!!.kind)}
}
