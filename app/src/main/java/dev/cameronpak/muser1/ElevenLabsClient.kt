package dev.cameronpak.muser1

import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

internal class ElevenLabsConfig(val apiKey: String, val voiceId: String)
internal class ElevenLabsFailure(val status: Int) : IOException("ElevenLabs HTTP $status")

/** Never log request bodies, transcripts, credentials, or server error bodies. */
internal class ElevenLabsClient(
    private val config: ElevenLabsConfig,
    private val baseUrl: String = "https://api.elevenlabs.io",
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS).readTimeout(90, TimeUnit.SECONDS)
        .callTimeout(100, TimeUnit.SECONDS).retryOnConnectionFailure(false)
        .followRedirects(false).followSslRedirects(false).build()
) {
    suspend fun transcribe(wav: ByteArray, language: String? = null): String {
        require(wav.size in 45..640044)
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("model_id", "scribe_v2")
            .apply { if (language != null) addFormDataPart("language_code", language) }
            .addFormDataPart("tag_audio_events", "false")
            .addFormDataPart("diarize", "false")
            .addFormDataPart("file", "voice_note.wav", wav.toRequestBody("audio/wav".toMediaType()))
            .build()
        val bytes = execute(request("/v1/speech-to-text", body), 1_000_000)
        val text = JSONObject(bytes.toString(Charsets.UTF_8)).getString("text").trim()
        if (text.isEmpty()) throw IOException("Empty transcription")
        return text
    }

    suspend fun synthesize(text: String, language: String = "ko"): ByteArray {
        require(text.isNotBlank() && text.length <= 1800)
        val body = JSONObject().put("model_id", "eleven_v4").put("language_code", language)
            .put("inputs", JSONArray().put(JSONObject().put("text", text).put("voice_id", config.voiceId)))
            .toString().toRequestBody("application/json".toMediaType())
        return execute(request("/v1/text-to-dialogue?output_format=mp3_44100_128", body), 16_000_000)
    }

    private fun request(path: String, body: RequestBody) = Request.Builder()
        .url(baseUrl + path).header("xi-api-key", config.apiKey).post(body).build()

    private suspend fun execute(request: Request, limit: Int): ByteArray = suspendCancellableCoroutine { cont ->
        val call = client.newCall(request)
        cont.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (!cont.isCancelled) cont.resumeWithException(IOException("ElevenLabs connection failed"))
            }
            override fun onResponse(call: Call, response: Response) {
                try {
                    val data = response.use {
                        if (!it.isSuccessful) throw ElevenLabsFailure(it.code)
                        val body = it.body ?: throw IOException("Empty response")
                        if (body.contentLength() > limit) throw IOException("Response too large")
                        val output = java.io.ByteArrayOutputStream()
                        body.byteStream().use { input ->
                            val buffer = ByteArray(8192)
                            while (true) {
                                val n = input.read(buffer)
                                if (n < 0) break
                                if (output.size() + n > limit) throw IOException("Response too large")
                                output.write(buffer, 0, n)
                            }
                        }
                        output.toByteArray().also { if (it.isEmpty()) throw IOException("Empty response") }
                    }
                    if (!cont.isCancelled) cont.resume(data)
                } catch (e: Exception) {
                    if (!cont.isCancelled) cont.resumeWithException(e)
                }
            }
        })
    }
}
