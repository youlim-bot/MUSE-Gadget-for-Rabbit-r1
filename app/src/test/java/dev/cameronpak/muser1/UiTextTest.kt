package dev.cameronpak.muser1
import org.junit.Assert.*
import org.junit.Test

class UiTextTest {
    @Test fun coreControlsTranslateInAllThreeDirections() {
        assertEquals("대화", UiText.translate(DisplayLanguage.KO, "Chat"))
        assertEquals("会話", UiText.translate(DisplayLanguage.JA, "대화"))
        assertEquals("Translate", UiText.translate(DisplayLanguage.EN, "通訳"))
        assertEquals("表示言語", UiText.translate(DisplayLanguage.JA, "표시 언어"))
        assertEquals("■ Stop", UiText.translate(DisplayLanguage.EN, "■ 중지"))
    }
    @Test fun untranslatedContentIsPreserved() {
        val message = "가장 가까운 역은 어디인가요?"
        for (language in DisplayLanguage.entries) assertEquals(message, UiText.translate(language, message))
    }
}
