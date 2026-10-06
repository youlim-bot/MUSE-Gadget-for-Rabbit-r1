package dev.cameronpak.muser1

import org.json.JSONObject

internal data class SpeechPoint(val millis: Int, val offset: Int)
internal data class TimedSpeech(val audio: ByteArray, val points: List<SpeechPoint>)

internal object SpeechTiming {
    fun parse(text: String, alignment: JSONObject?): List<SpeechPoint> {
        val chars = alignment?.optJSONArray("characters") ?: return emptyList()
        val times = alignment.optJSONArray("character_start_times_seconds") ?: return emptyList()
        if (chars.length() != times.length()) return emptyList()
        val points = mutableListOf<SpeechPoint>()
        var offset = 0
        var previous = 0.0
        for (i in 0 until chars.length()) {
            val char = chars.optString(i)
            val time = times.optDouble(i, Double.NaN)
            if (char.isEmpty() || !text.startsWith(char, offset) || !time.isFinite() || time < previous) return emptyList()
            points += SpeechPoint((time * 1000).toInt(), offset)
            offset += char.length // UTF-16 offsets, including supplementary characters.
            previous = time
        }
        return if (offset == text.length) points else emptyList()
    }
}
