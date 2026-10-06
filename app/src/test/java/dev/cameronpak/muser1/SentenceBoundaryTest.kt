package dev.cameronpak.muser1

import org.junit.Assert.*
import org.junit.Test

class SentenceBoundaryTest {
    private fun pcm(ms: Int, amplitude: Int): ByteArray = java.nio.ByteBuffer.allocate(Wave.RATE * ms / 1000 * 2)
        .order(java.nio.ByteOrder.LITTLE_ENDIAN).apply { repeat(capacity() / 2) { putShort(amplitude.toShort()) } }.array()
    private fun feed(boundary: SentenceBoundary, ms: Int, amplitude: Int): Boolean = pcm(ms, amplitude).let { boundary.accept(it, it.size) }
    @Test fun silenceDoesNotSubmitAudio() {
        val b = SentenceBoundary()
        repeat(200) { assertFalse(feed(b, 100, 0)) }
        assertFalse(b.hasSpeech)
    }
    @Test fun endsOnlyAfterSufficientSpeechAndNineHundredMsPause() {
        val b = SentenceBoundary()
        assertFalse(feed(b, 300, 1800))
        assertTrue(b.hasSpeech)
        assertFalse(feed(b, 899, 0))
        assertTrue(feed(b, 2, 0))
    }
    @Test fun briefNoiseIsNotSpeech() {
        val b = SentenceBoundary()
        assertFalse(feed(b, 50, 2000))
        assertFalse(feed(b, 1500, 0))
        assertFalse(b.hasSpeech)
    }
    @Test fun resumedSpeechResetsPause() {
        val b = SentenceBoundary()
        feed(b, 500, 1500)
        assertFalse(feed(b, 700, 0))
        assertFalse(feed(b, 200, 1500))
        assertFalse(feed(b, 700, 0))
        assertTrue(feed(b, 200, 0))
    }
    @Test fun lowLevelBackgroundDoesNotCountAsVoice() {
        val b = SentenceBoundary()
        repeat(100) { assertFalse(feed(b, 100, 100)) }
        assertFalse(b.hasSpeech)
    }
    @Test fun negativeSamplesAndSplitBuffersAreHandled() {
        val b = SentenceBoundary()
        repeat(20) { assertFalse(feed(b, 10, -2000)) }
        assertTrue(b.hasSpeech)
        repeat(89) { assertFalse(feed(b, 10, 0)) }
        assertTrue(feed(b, 10, 0))
    }
}
