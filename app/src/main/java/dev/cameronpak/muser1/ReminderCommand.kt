package dev.cameronpak.muser1

import java.time.ZonedDateTime

internal object ReminderCommand {
    fun parse(text: String, interpreting: Boolean = false, now: ZonedDateTime = ZonedDateTime.now()): ClockCommand? {
        if (interpreting || text.length > 500) return null
        if (!Regex("리마인더|알려\\s*줘|알려주세요|상기|remind me|reminder|リマインダー|リマインド|教えて|知らせて", RegexOption.IGNORE_CASE).containsMatchIn(text)) return null
        val explicit = Regex("리마인더|remind|リマイン", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val time = Regex("(?:[0-9]+|한|두|세)\\s*(?:시|시간|분|초|時|分|秒|hours?|minutes?|seconds?|am|pm|:)", RegexOption.IGNORE_CASE).containsMatchIn(text)
        if (!explicit && !time) return null
        // Calendar dates outside the supported today/tomorrow grammar require the date picker.
        // Never silently schedule a named date for the next occurrence of its clock time.
        if (Regex("[0-9]+\\s*(?:월|月|년|年)|[0-9]+[-/][0-9]+|다음|来週|来月|next week|next month|january|february|march|april|may|june|july|august|september|october|november|december", RegexOption.IGNORE_CASE).containsMatchIn(text)) return ClockCommand("help")
        val relative = Regex("(?:시간|분|초)\\s*(?:후|뒤)|(?:時間|分|秒)後|in\\s+[0-9]+\\s*(?:hours?|minutes?|seconds?)", RegexOption.IGNORE_CASE).containsMatchIn(text)
        val parsed = LocalClockCommand.parse((if(relative) "타이머 " else "알람 ") + text) ?: return ClockCommand("help")
        if (parsed.kind !in setOf("alarm", "timer")) return parsed
        return if (parsed.kind == "timer") ClockCommand("alarm", title = text.take(160),
            at = now.toInstant().toEpochMilli() + parsed.seconds * 1000)
        else parsed.copy(title = text.take(160))
    }
}
