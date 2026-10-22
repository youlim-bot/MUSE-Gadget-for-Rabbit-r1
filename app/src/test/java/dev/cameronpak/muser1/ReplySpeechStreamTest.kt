package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class ReplySpeechStreamTest {
    @Test fun firstSentenceEnqueuesBeforeDoneAndOnlyRemainderOnDone() {
        val stream = ReplySpeechStream()
        assertTrue(stream.update("안녕하세요", false).isEmpty())
        assertEquals(listOf(ReplySentence("안녕하세요. ".trimEnd(), 0)), stream.update("안녕하세요. 다음", false))
        assertTrue(stream.update("안녕하세요. 다음 문장", false).isEmpty())
        assertEquals(listOf(ReplySentence(" 다음 문장", 6)), stream.update("안녕하세요. 다음 문장", true))
        assertTrue(stream.update("안녕하세요. 다음 문장", true).isEmpty())
    }
    @Test fun everyPossibleTransportSplitQueuesExactlyOnceWithAbsoluteOffsets() {
        val full = "첫 문장。次です！ Hello world. Last remainder"
        for (split in 0..full.length) {
            val stream = ReplySpeechStream()
            val queue = mutableListOf<ReplySentence>()
            queue += stream.update(full.take(split), false)
            queue += stream.update(full, false)
            queue += stream.update(full, true)
            queue += stream.update(full, true)
            assertEquals("split=$split", full, queue.joinToString("") { it.text })
            var offset = 0
            queue.forEach { assertEquals(offset, it.offset); offset += it.text.length }
        }
    }
    @Test fun characterDeltasPreserveNumbersUrlsQuotesAndMarkdownOffsets() {
        val full = "**가격** 3.14, https://example.com 입니다. \"本当？\" Yes! 끝"
        val expected = full.replace("**", "")
        val stream = ReplySpeechStream(); val queue = mutableListOf<ReplySentence>()
        for (end in 1..full.length) queue += stream.update(full.take(end), false)
        queue += stream.update(full, true)
        assertEquals(expected, queue.joinToString("") { it.text })
        assertTrue(queue.first().text.contains("3.14"))
        assertTrue(queue.first().text.contains("https://example.com"))
        queue.forEach { assertEquals(it.text, expected.substring(it.offset, it.offset + it.text.length)) }
    }
    @Test fun finalSnapshotWithoutDeltasAndSeparateMessagesAreIndependent() {
        listOf("한국어입니다.", "日本語です。", "English.").forEach { text ->
            val stream = ReplySpeechStream()
            assertEquals(text, stream.update(text, true).joinToString("") { it.text })
            assertTrue(stream.update(text, true).isEmpty())
        }
    }
    @Test fun finalRewriteNeverReplaysAlreadySpokenPrefix() {
        val stream = ReplySpeechStream()
        assertFalse(stream.update("One! Rest", false).isEmpty())
        assertTrue(stream.update("Changed! Rest", true).isEmpty())
    }
    @Test fun emptyAndWhitespaceRepliesDoNotEnqueueAudio() {
        assertTrue(ReplySpeechStream().update("  \n", true).isEmpty())
        assertTrue(ReplySpeechStream().update("", true).isEmpty())
    }
}
