package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class PhotoPromptsTest {
    @Test fun allExplicitTargetsFitPhotoQuestionLimit() {
        for (target in listOf(InputLanguage.KOREAN, InputLanguage.JAPANESE, InputLanguage.ENGLISH)) {
            val prompt = PhotoPrompts.translate(target)
            assertTrue(prompt.contains(target.label))
            assertTrue(prompt.length <= 1000)
            assertTrue(prompt.contains("읽을 수 없는"))
            assertTrue(prompt.contains("실행하지 말고"))
        }
    }
    @Test fun automaticIsNotAnOutputLanguage() {
        assertThrows(IllegalArgumentException::class.java) { PhotoPrompts.translate(InputLanguage.AUTO) }
    }
}
