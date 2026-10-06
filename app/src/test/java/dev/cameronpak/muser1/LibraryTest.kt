package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class LibraryTest {
    @Test fun savedCopiesAndMemosRoundTripWithoutHistory() {
        val items=listOf(SavedItem("a","favorite","질문 日本語","answer\n답변",123),SavedItem("b","memo","할 일","우유 사기",456))
        assertEquals(items,decodeLibrary(encodeLibrary(items)))
        assertEquals(emptyList<SavedItem>(),decodeLibrary("[]"))
    }
    @Test fun searchesAcrossQuestionAndAnswerWithAllTerms() {
        assertTrue(matchesQuery("한국어 여행","Tokyo 日本語"," 여행 tokyo "))
        assertTrue(matchesQuery("질문","日本語 번역","日本語"))
        assertFalse(matchesQuery("한국어 여행","Tokyo","Tokyo 없는단어"))
        assertTrue(matchesQuery("a","b","  "))
    }
    @Test fun favoriteIdentityDeduplicatesSameContentButKeepsDifferentReplies() {
        assertEquals(favoriteId("hello","world"),favoriteId("hello","world"))
        assertNotEquals(favoriteId("hello","world"),favoriteId("hello","different"))
        assertNotEquals(favoriteId("ab","c"),favoriteId("a","bc"))
    }
}
