package dev.cameronpak.muser1
import java.time.ZonedDateTime
import org.junit.Assert.*
import org.junit.Test

class ReminderCommandTest {
    private val now = ZonedDateTime.parse("2026-10-06T12:00:00+09:00[Asia/Tokyo]")
    @Test fun reminderKeepsDescriptionAndFutureTime() {
        val c = ReminderCommand.parse("내일 오후 3시 거래처에 전화하라고 알려줘", now = now)!!
        assertEquals("alarm", c.kind)
        assertEquals(15, c.hour)
        assertEquals("tomorrow", c.day)
        assertTrue(c.title.contains("거래처"))
        assertEquals(now.plusDays(1).withHour(15).toInstant().toEpochMilli(), LocalClockCommand.due(c, now))
    }
    @Test fun relativeAndOtherLanguages() {
        for (text in listOf("20분 후 약 먹으라고 알려줘", "remind me in 20 minutes to take medicine", "20分後に薬を飲むように知らせて")) {
            val c = ReminderCommand.parse(text, now = now)!!
            assertEquals(now.toInstant().toEpochMilli() + 1200000, LocalClockCommand.due(c,now))
        }
        assertEquals(15, ReminderCommand.parse("明日午後3時に電話するように知らせて", now=now)!!.hour)
    }
    @Test fun ordinaryQuestionsAndTranslationAreNotScheduled() {
        assertNull(ReminderCommand.parse("추천 기능 3가지 알려줘"))
        assertNull(ReminderCommand.parse("내일 날씨 알려줘"))
        assertNull(ReminderCommand.parse("내일 3시 전화하라고 알려줘", true))
    }
    @Test fun unsupportedCalendarDateRequiresManualConfirmationInsteadOfWrongDay() {
        assertEquals("help", ReminderCommand.parse("10월 10일 오후 3시 약속 알려줘")!!.kind)
        assertEquals("help", ReminderCommand.parse("Remind me on October 10 at 3pm")!!.kind)
    }
    @Test(expected = IllegalArgumentException::class) fun pastAppointmentIsRejected() {
        LocalClockCommand.due(ClockCommand("alarm", title="Call", at=now.minusHours(1).toInstant().toEpochMilli()), now)
    }
}
