package dev.cameronpak.muser1

import java.security.MessageDigest
import java.util.Locale

internal enum class WaitingTopic { MAIL, CALENDAR, WEATHER, NEWS, SEARCH, THINKING }
internal object ReplyWaiting {
    const val VOICE_MS = 2500L
    const val SECOND_MS = 8000L
    const val THIRD_MS = 15000L
    const val MODEL = "eleven_v4"
    fun stage(elapsed: Long) = when { elapsed >= THIRD_MS -> 2; elapsed >= SECOND_MS -> 1; else -> 0 }
    fun voiceDue(elapsed: Long, attempted: Boolean) = elapsed >= VOICE_MS && !attempted
    fun topic(text: String): WaitingTopic {
        val value = text.lowercase(Locale.ROOT)
        val words = listOf(
            WaitingTopic.MAIL to listOf("메일", "이메일", "メール", "mail", "inbox"),
            WaitingTopic.CALENDAR to listOf("일정", "달력", "予定", "スケジュール", "カレンダー", "calendar", "schedule", "appointment"),
            WaitingTopic.WEATHER to listOf("날씨", "기온", "天気", "気温", "weather", "forecast"),
            WaitingTopic.NEWS to listOf("뉴스", "ニュース", "news", "headlines"),
            WaitingTopic.SEARCH to listOf("검색", "찾아", "検索", "調べ", "探して", "search", "look up", "find"))
        return words.firstOrNull { (_, keys) -> keys.any { it in value } }?.first ?: WaitingTopic.THINKING
    }
    fun phrase(topic: WaitingTopic, language: String, stage: Int): String {
        val row = when {
            stage >= 2 -> listOf("아직 확인 중이에요. 조금만 더 기다려 주세요…", "まだ確認中です。もう少々お待ちください…", "Still checking. Thank you for waiting…")
            stage == 1 -> listOf("조금만 더 기다려 주세요…", "もう少々お待ちください…", "Please wait a little longer…")
            else -> when (topic) {
                WaitingTopic.MAIL -> listOf("메일을 확인하고 있어요…", "メールを確認しています…", "Checking your mail…")
                WaitingTopic.CALENDAR -> listOf("일정을 확인하고 있어요…", "予定を確認しています…", "Checking your calendar…")
                WaitingTopic.WEATHER -> listOf("날씨를 확인하고 있어요…", "天気を確認しています…", "Checking the weather…")
                WaitingTopic.NEWS -> listOf("뉴스를 찾아보고 있어요…", "ニュースを探しています…", "Looking for news…")
                WaitingTopic.SEARCH -> listOf("검색하고 있어요…", "検索しています…", "Searching…")
                WaitingTopic.THINKING -> listOf("생각하고 있어요…", "考えています…", "Thinking…")
            }
        }
        return row[when (language) { "ja" -> 1; "en" -> 2; else -> 0 }]
    }
    fun voice(language: String) = when (language) {
        "ja" -> "少々お待ちください、確認しますね。"
        "en" -> "One moment, let me check."
        else -> "잠시만요, 확인해 볼게요."
    }
    fun cacheKey(voiceId: String, model: String, language: String): String =
        MessageDigest.getInstance("SHA-256").digest("waiting-v1\u0000$voiceId\u0000$model\u0000$language".toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it.toInt() and 255) }
}
