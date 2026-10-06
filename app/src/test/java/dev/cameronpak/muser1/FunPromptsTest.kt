package dev.cameronpak.muser1
import org.junit.Assert.*
import org.junit.Test
import org.json.JSONTokener
class FunPromptsTest {
    @Test fun practiceIsScopedAndPreservesQuotedUtterance() {
        val input="Hello \"Muse\"\nHow are you?"
        for(lang in listOf(InputLanguage.KOREAN,InputLanguage.JAPANESE,InputLanguage.ENGLISH)) {
            val p=FunPrompts.practice(lang,"a cafe",input)
            assertTrue(p.contains(lang.name));assertTrue(p.contains("For this turn only"))
            assertEquals(input,JSONTokener(p.substringAfterLast("utterance:\n")).nextValue())
            assertTrue(p.contains("pronunciation scores"))
        }
    }
    @Test fun automaticLanguageIsNotPracticeLanguage() {
        assertTrue(runCatching{FunPrompts.practice(InputLanguage.AUTO,"a cafe","Hi")}.isFailure)
    }
    @Test fun huntPromptsKeepTargetAndUncertainty() {
        assertTrue(FunPrompts.hunt("빨간 물건",InputLanguage.KOREAN).contains("불확실"))
        assertTrue(FunPrompts.hunt("赤いもの",InputLanguage.JAPANESE).contains("赤いもの"))
        assertTrue(FunPrompts.hunt("a red object",InputLanguage.ENGLISH).contains("If uncertain"))
    }
}
