package dev.cameronpak.muser1

import kotlinx.coroutines.*
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class ElevenLabsClientTest {
    @Test fun koreanTranscriptUsesScribeAndUnmodifiedText() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"text":"타이핑은 어떻게 해?"}"""))
            val api = ElevenLabsClient(ElevenLabsConfig("test-key", "voice123"), server.url("").toString().trimEnd('/'))
            assertEquals("타이핑은 어떻게 해?", api.transcribe(Wave.encode(ByteArray(320))))
            val request = server.takeRequest()
            assertEquals("/v1/speech-to-text", request.path)
            assertEquals("test-key", request.getHeader("xi-api-key"))
            val body = request.body.readUtf8()
            assertFalse(body.contains("name=\"language_code\""))
            assertTrue(body.contains("scribe_v2"))
            assertTrue(body.contains("audio/wav"))
        }
    }
    @Test fun japaneseTranscriptIsPreservedWithAutomaticDetection() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"text":"日本語で入力できますか？","language_code":"ja"}"""))
            val api = ElevenLabsClient(ElevenLabsConfig("test-key", "voice123"), server.url("").toString().trimEnd('/'))
            assertEquals("日本語で入力できますか？", api.transcribe(Wave.encode(ByteArray(320))))
            assertFalse(server.takeRequest().body.readUtf8().contains("name=\"language_code\""))
        }
    }
    @Test fun outputUsesV4DialogueAndKorean() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("fake-audio"))
            val api = ElevenLabsClient(ElevenLabsConfig("test-key", "voice123"), server.url("").toString().trimEnd('/'))
            assertEquals("fake-audio", api.synthesize("안녕하세요").toString(Charsets.UTF_8))
            val request = server.takeRequest()
            assertTrue(request.path!!.startsWith("/v1/text-to-dialogue?"))
            val json = JSONObject(request.body.readUtf8())
            assertEquals("eleven_v4", json.getString("model_id"))
            assertEquals("ko", json.getString("language_code"))
            assertEquals("voice123", json.getJSONArray("inputs").getJSONObject(0).getString("voice_id"))
        }
    }
    @Test fun explicitInputAndTranslationOutputLanguagesReachApi() = runBlocking {
        MockWebServer().use { server ->
            val api = ElevenLabsClient(ElevenLabsConfig("test-key", "voice123"), server.url("").toString().trimEnd('/'))
            for (language in listOf("ko", "ja", "en")) {
                server.enqueue(MockResponse().setBody("""{"text":"test"}"""))
                api.transcribe(Wave.encode(ByteArray(320)), language)
                val body = server.takeRequest().body.readUtf8()
                assertTrue(body.contains("name=\"language_code\""))
                assertTrue(body.contains("\r\n\r\n$language\r\n"))
                server.enqueue(MockResponse().setBody("audio"))
                api.synthesize("test", language)
                assertEquals(language, JSONObject(server.takeRequest().body.readUtf8()).getString("language_code"))
            }
        }
    }
    @Test fun authErrorsDoNotLeakBodyOrRetry() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(401).setBody("private server details"))
            val api = ElevenLabsClient(ElevenLabsConfig("secret", "voice123"), server.url("").toString().trimEnd('/'))
            try { api.transcribe(Wave.encode(ByteArray(320))); fail("Expected failure") }
            catch (e: ElevenLabsFailure) { assertEquals(401, e.status); assertFalse(e.message!!.contains("private")) }
            assertEquals(1, server.requestCount)
        }
    }
    @Test fun cancellationInterruptsPendingRequest() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val api = ElevenLabsClient(ElevenLabsConfig("test-key", "voice123"), server.url("").toString().trimEnd('/'))
            val pending = async { api.synthesize("안녕하세요") }
            withContext(Dispatchers.IO) { assertNotNull(server.takeRequest(5, TimeUnit.SECONDS)) }
            withTimeout(2000) { pending.cancelAndJoin() }
            assertTrue(pending.isCancelled)
        }
    }
    @Test fun emptyTranscriptNeverBecomesMuseMessage() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setBody("""{"text":" "}"""))
            val api = ElevenLabsClient(ElevenLabsConfig("test-key", "voice123"), server.url("").toString().trimEnd('/'))
            try { api.transcribe(Wave.encode(ByteArray(320))); fail("Expected failure") }
            catch (e: java.io.IOException) { assertEquals("Empty transcription", e.message) }
        }
    }
}
