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
    var onReading: (String, Int) -> Unit = { _, _ -> }
    var onReadingEnd: () -> Unit = {}
    private data class Part(val text: String, val full: String, val offset: Int, val language: String)
    private val queue = ArrayDeque<Part>()
    private var worker: Job? = null
    private var closed = false
    private val local: AndroidSpeechOutput = AndroidSpeechOutput(context) {
        if (worker?.isActive != true) { onState(it); if (!localPlayback()) onReadingEnd() }
    }.also { output -> output.onReading = { text, offset -> onReading(text, offset) } }
    private fun localPlayback(): Boolean = local.hasPlayback
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
        val clean = text.replace("**", "")
        if (clean.isBlank()) return
        val config = store.elevenLabs()
        if (config == null) { local.speak(clean); return }
        local.stop()
        var offset = 0
        clean.chunked(1800).forEach { part -> queue.add(Part(part, clean, offset, language)); offset += part.length }
        if (worker?.isActive == true) return
        worker = scope.launch(start = CoroutineStart.LAZY) {
            try {
                if (manager.requestAudioFocus(focus) != AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
                    queue.clear(); onState("AUDIO BUSY"); return@launch
                }
                val api = ElevenLabsClient(config)
                while (queue.isNotEmpty()) {
                    onState("ELEVEN V4 음성 생성 중")
                    val part = queue.removeFirst()
                    val audio = api.synthesizeTimed(part.text, part.language)
                    ensureActive()
                    val file = File.createTempFile("eleven-playback-", ".mp3", context.cacheDir)
                    try {
                        withContext(Dispatchers.IO) { file.writeBytes(audio.audio) }
                        play(file, audio, part)
                    } finally { file.delete() }
                }
                onState("READY")
            } catch (e: CancellationException) { throw e }
            catch (e: Exception) {
                queue.clear()
                onState(if (e is ElevenLabsFailure) "음성 생성 실패 (HTTP ${e.status})" else "음성 생성·재생 실패 — 연결을 확인하세요")
            } finally { onReadingEnd(); manager.abandonAudioFocusRequest(focus) }
        }.also { it.start() }
    }

    private suspend fun play(file: File, audio: TimedSpeech, part: Part) {
        val handler = Handler(Looper.getMainLooper())
        var tick: Runnable? = null
        val player = MediaPlayer()
        try {
            player.setAudioAttributes(attributes)
            FileInputStream(file).use { player.setDataSource(it.fd) }
            suspendCancellableCoroutine<Unit> { cont ->
                player.setOnPreparedListener {
                    if (cont.isActive) {
                        try { it.playbackParams = android.media.PlaybackParams().setSpeed(ReadingSettings.speed(context)).setPitch(1f); it.start(); onState("MUSE IS SPEAKING")
                            tick = object : Runnable {
                                override fun run() {
                                    if (!cont.isActive) return
                                    val offset = audio.points.lastOrNull { point -> point.millis <= player.currentPosition }?.offset ?: 0
                                    onReading(part.full, part.offset + offset)
                                    handler.postDelayed(this, 100)
                                }
                            }.also { task -> handler.post(task) } }
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
        } finally { tick?.let { handler.removeCallbacks(it) }; player.release() }
    }

    fun stop() {
        queue.clear(); worker?.cancel(); worker = null; local.stop(); onReadingEnd()
        manager.abandonAudioFocusRequest(focus)
    }
    fun close() { closed = true; stop(); scope.cancel(); local.close() }
}
