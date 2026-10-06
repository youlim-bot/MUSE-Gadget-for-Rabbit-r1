package dev.cameronpak.muser1

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.*
import org.junit.Test

class TimedSpeechClientTest {
    @Test fun timedDialogueDecodesAudioAndOriginalCharacterPositions() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"audio_base64":"YXVkaW8=","alignment":{"characters":["안","녕"],"character_start_times_seconds":[0,0.3]}}"""))
            val api = ElevenLabsClient(ElevenLabsConfig("test-key", "test-voice"), server.url("").toString().trimEnd('/'))
            val audio = api.synthesizeTimed("안녕", "ko")
            assertEquals("audio", audio.audio.toString(Charsets.UTF_8))
            assertEquals(listOf(SpeechPoint(0,0), SpeechPoint(300,1)), audio.points)
            assertTrue(server.takeRequest().path!!.startsWith("/v1/text-to-dialogue/with-timestamps?"))
        }
    }
    @Test fun unsupportedTimingPreservesVoiceButAuthFailureIsNotRetried() = runBlocking {
        MockWebServer().use { server ->
            val api = ElevenLabsClient(ElevenLabsConfig("test-key", "test-voice"), server.url("").toString().trimEnd('/'))
            server.enqueue(MockResponse().setResponseCode(422))
            server.enqueue(MockResponse().setBody("audio"))
            assertEquals("audio", api.synthesizeTimed("hello").audio.toString(Charsets.UTF_8))
            assertEquals(2, server.requestCount)
            server.enqueue(MockResponse().setResponseCode(401))
            try { api.synthesizeTimed("hello"); fail("Expected auth failure") }
            catch (e: ElevenLabsFailure) { assertEquals(401, e.status) }
            assertEquals(3, server.requestCount)
        }
    }
}
