package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class LanguageModeTest {
    @Test fun conversationPreservesExactText() {
        assertEquals("hello 日本語 한국어", LanguageMode().message("hello 日本語 한국어"))
    }
    @Test fun translationSupportsEveryDirectedPair() {
        for (source in InputLanguage.entries) for (target in InputLanguage.entries.filter { it != InputLanguage.AUTO }) {
            val mode = LanguageMode(true, source, target)
            val prompt = mode.message("\"안녕\"\nHello")
            assertTrue(prompt.contains("into ${target.name}"))
            assertTrue(prompt.contains("Return only the translation"))
            assertTrue(prompt.endsWith(org.json.JSONObject.quote("\"안녕\"\nHello")))
        }
    }
    @Test fun swapPreservesAutoAndReversesExplicitPair() {
        val auto = LanguageMode(true)
        assertEquals(auto, auto.swapped())
        val pair = LanguageMode(true, InputLanguage.KOREAN, InputLanguage.ENGLISH)
        assertEquals(InputLanguage.ENGLISH, pair.swapped().input)
        assertEquals(pair, pair.swapped().swapped())
    }
    @Test fun translationHistoryRoundTripsAndOldHistoryStillLoads() {
        val row = ConversationTurn("안녕", linkedMapOf("reply" to "Hello"), "English")
        assertEquals(row, decodeHistory(encodeHistory(listOf(row))).single())
        assertNull(decodeHistory("""[{"user":"hello","replies":[]}]""").single().translationTarget)
    }
}
