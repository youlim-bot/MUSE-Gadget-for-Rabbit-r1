package dev.cameronpak.muser1

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class SpeechTimingTest {
    @Test fun unicodeUsesDisplayOffsetsAndMediaMilliseconds() {
        val points = SpeechTiming.parse("한😀日", JSONObject("""{"characters":["한","😀","日"],"character_start_times_seconds":[0,0.25,1.5]}"""))
        assertEquals(listOf(SpeechPoint(0,0), SpeechPoint(250,1), SpeechPoint(1500,3)), points)
    }
    @Test fun invalidOrNormalizedAlignmentCannotScrollToWrongText() {
        for (json in listOf(
            """{"characters":["a"],"character_start_times_seconds":[]}""",
            """{"characters":["b"],"character_start_times_seconds":[0]}""",
            """{"characters":["a","b"],"character_start_times_seconds":[1,0]}"""
        )) assertTrue(SpeechTiming.parse("ab", JSONObject(json)).isEmpty())
        assertTrue(SpeechTiming.parse("ab", null).isEmpty())
    }
}
