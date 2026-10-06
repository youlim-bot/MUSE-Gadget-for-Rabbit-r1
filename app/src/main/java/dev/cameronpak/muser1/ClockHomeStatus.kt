package dev.cameronpak.muser1

import android.content.Context
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Read-only summary: showing the home screen must never reschedule a clock. */
internal object ClockHomeStatus {
    fun text(c: Context): String {
        fun t(ko: String, ja: String, en: String) = UiText.text(c, ko, ja, en)
        val entries = LocalClock.entries(c).filter { it.state in setOf("active", "paused", "ringing") }
        return listOf("alarm", "timer").mapNotNull { kind ->
            val clocks = entries.filter { it.kind == kind }.sortedWith(
                compareBy<ClockEntry> { when (it.state) { "ringing" -> 0; "active" -> 1; else -> 2 } }
                    .thenBy { LocalClock.remaining(c, it) })
            val next = clocks.firstOrNull() ?: return@mapNotNull null
            val label = if (kind == "alarm") t("알람", "アラーム", "Alarm") else t("타이머", "タイマー", "Timer")
            val value = when {
                next.state == "ringing" -> t("울리는 중", "鳴動中", "Ringing")
                kind == "timer" -> {
                    val seconds = (LocalClock.remaining(c, next).coerceAtLeast(0) + 999) / 1000
                    val time = if (seconds >= 3600) String.format(Locale.ROOT, "%d:%02d:%02d", seconds / 3600, seconds / 60 % 60, seconds % 60)
                        else String.format(Locale.ROOT, "%02d:%02d", seconds / 60, seconds % 60)
                    if (next.state == "paused") "$time · ${t("일시정지", "一時停止", "Paused")}" else time
                }
                else -> {
                    val date = Instant.ofEpochMilli(next.due).atZone(ZoneId.systemDefault())
                    val day = when (date.toLocalDate()) {
                        LocalDate.now() -> t("오늘", "今日", "Today")
                        LocalDate.now().plusDays(1) -> t("내일", "明日", "Tomorrow")
                        else -> date.format(DateTimeFormatter.ofPattern("M/d"))
                    }
                    "$day ${date.format(DateTimeFormatter.ofPattern("HH:mm"))}"
                }
            }
            "$label  $value" + (if(next.title.isNotBlank()) " · " + next.title.take(16) else "") + if (clocks.size > 1) "  (+${clocks.size - 1})" else ""
        }.joinToString("\n")
    }
}
