package dev.cameronpak.muser1

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Handler
import android.os.Looper
import kotlinx.coroutines.*
import java.io.File
import java.io.FileInputStream
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Main-thread facade. Android speech remains available until ElevenLabs is provisioned. */
internal class SpeechOutput(private val context: Context, private val onState: (String) -> Unit) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val store get() = (context.applicationContext as MuseApp).store
    private val queue = ArrayDeque<String>()
    private var worker: Job? = null
    private var closed = false
    private val local = AndroidSpeechOutput(context) { if (worker?.isActive != true) onState(it) }
    private val manager = context.getSystemService(AudioManager::class.java)
    private val attributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()
    private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes).setOnAudioFocusChangeListener({ change ->
            if (change < 0 && worker?.isActive == true) { stop(); onState("AUDIO INTERRUPTED") }
        }, Handler(Looper.getMainLooper())).build()
    val hasPlayback get() = worker?.isActive == true || queue.isNotEmpty() || local.hasPlayback

    fun speak(text: String, language: String = "ko") {
        if (closed || text.isBlank()) return
        val config = store.elevenLabs()
        if (config == null) { local.speak(text); return }
        local.stop()
        queue.addAll(text.chunked(1800))
        if (worker?.isActive == true) return
        worker = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    queue.clear(); onState("AUDIO BUSY"); return@launch
                }
                val api = ElevenLabsClient(config)
                while (queue.isNotEmpty()) {
                    onState("ELEVEN V4 음성 생성 중")
                    val audio = api.synthesize(queue.removeFirst(), language)
                    ensureActive()
                    val file = File.createTempFile("eleven-playback-", ".mp3", context.cacheDir)
                    try {
                        withContext(Dispatchers.IO) { file.writeBytes(audio) }
                        play(file)
                    } finally { file.delete() }
                }
                onState("READY")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                queue.clear()
                onState(if (e is ElevenLabsFailure) "음성 생성 실패 (HTTP ${e.status})" else "음성 생성·재생 실패 — 연결을 확인하세요")
            } finally { manager.abandonAudioFocusRequest(focus) }
        }.also { it.start() }
    }

    private suspend fun play(file: File) {
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(attributes)
            FileInputStream(file).use { player.setDataSource(it.fd) }
            suspendCancellableCoroutine<Unit> { cont ->
                player.setOnPreparedListener {
                    if (cont.isActive) {
                        try { it.playbackParams = android.media.PlaybackParams().setSpeed(ReadingSettings.speed(context)).setPitch(1f); it.start(); onState("MUSE IS SPEAKING") }
                        catch (e: Exception) { cont.resumeWithException(e) }
                    }
                }
                player.setOnCompletionListener { if (cont.isActive) cont.resume(Unit) }
                player.setOnErrorListener { _, _, _ ->
                    if (cont.isActive) cont.resumeWithException(IllegalStateException("Playback failed"))
                    true
                }
                player.prepareAsync()
            }
        } finally { player.release() }
    }

    fun stop() {
        queue.clear(); worker?.cancel(); worker = null; local.stop()
        manager.abandonAudioFocusRequest(focus)
    }
    fun close() { closed = true; stop(); scope.cancel(); local.close() }
}
