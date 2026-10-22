package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class ReplyWaitingTest {
    @Test fun classifiesAllTopicsInAllThreeLanguages() {
        val examples = mapOf(
            WaitingTopic.MAIL to listOf("메일 확인해줘", "メールを確認", "Check my inbox"),
            WaitingTopic.CALENDAR to listOf("내일 일정", "明日の予定", "My calendar"),
            WaitingTopic.WEATHER to listOf("오늘 날씨", "東京の天気", "Weather forecast"),
            WaitingTopic.NEWS to listOf("오늘 뉴스", "ニュースを教えて", "Latest NEWS"),
            WaitingTopic.SEARCH to listOf("맛집 검색", "お店を検索", "Look up this word"),
            WaitingTopic.THINKING to listOf("안녕", "こんにちは", "Hello there"))
        examples.forEach { (topic, inputs) -> inputs.forEach { assertEquals(it, topic, ReplyWaiting.topic(it)) } }
    }
    @Test fun scheduleHasExactThresholdsAndVoiceIsOncePerTurn() {
        assertFalse(ReplyWaiting.voiceDue(2499, false))
        assertTrue(ReplyWaiting.voiceDue(2500, false))
        assertFalse(ReplyWaiting.voiceDue(15000, true))
        assertEquals(0, ReplyWaiting.stage(7999))
        assertEquals(1, ReplyWaiting.stage(8000))
        assertEquals(1, ReplyWaiting.stage(14999))
        assertEquals(2, ReplyWaiting.stage(15000))
    }
    @Test fun statusAndVoiceAreLocalizedAndChangeAtEachStage() {
        for (language in listOf("ko", "ja", "en")) {
            val phrases = (0..2).map { ReplyWaiting.phrase(WaitingTopic.MAIL, language, it) }
            assertEquals(3, phrases.toSet().size)
            assertTrue(ReplyWaiting.voice(language).isNotBlank())
        }
        assertEquals("메일을 확인하고 있어요…", ReplyWaiting.phrase(WaitingTopic.MAIL, "ko", 0))
        assertEquals("Searching…", ReplyWaiting.phrase(WaitingTopic.SEARCH, "en", 0))
    }
    @Test fun cacheKeyIsStablePrivateAndIncludesVoiceModelAndLanguage() {
        val key = ReplyWaiting.cacheKey("voice-a", "eleven_v4", "ko")
        assertEquals(key, ReplyWaiting.cacheKey("voice-a", "eleven_v4", "ko"))
        assertTrue(key.matches(Regex("[0-9a-f]{64}")))
        assertEquals(4, setOf(key, ReplyWaiting.cacheKey("voice-b", "eleven_v4", "ko"),
            ReplyWaiting.cacheKey("voice-a", "other", "ko"), ReplyWaiting.cacheKey("voice-a", "eleven_v4", "ja")).size)
        assertNotEquals(ReplyWaiting.cacheKey("ab", "c", "ko"), ReplyWaiting.cacheKey("a", "bc", "ko"))
    }
}
