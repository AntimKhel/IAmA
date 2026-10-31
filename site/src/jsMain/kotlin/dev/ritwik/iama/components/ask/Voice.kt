package dev.ritwik.iama.components.ask

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.ritwik.iama.data.Backend
import dev.ritwik.iama.util.Logcat
import dev.ritwik.iama.util.jsObject
import dev.ritwik.iama.util.transcribeAudio
import kotlinx.browser.window
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.await
import kotlinx.coroutines.launch
import kotlin.math.sqrt

/**
 * Voice questions: records from the microphone, then Whisper on the site's own Worker turns it into text
 * (so it works the same in every browser, Firefox included, and nothing goes to a third party).
 */
object Voice {
    enum class State { IDLE, RECORDING, TRANSCRIBING }

    private const val MAX_SECONDS = 30

    var state by mutableStateOf(State.IDLE); private set
    /** Microphone level, 0..1, for the meter. */
    var level by mutableStateOf(0.0); private set
    var seconds by mutableStateOf(0); private set

    val supported: Boolean
        get() = Backend.enabled && window.navigator.asDynamic().mediaDevices?.getUserMedia != null &&
            window.asDynamic().MediaRecorder != null

    private val scope = MainScope()
    private var recorder: dynamic = null
    private var stream: dynamic = null
    private var audioCtx: dynamic = null
    private var raf = 0
    private var timer = 0
    private var startedAt = 0.0
    private var send = true

    fun start(onText: (String) -> Unit, onError: (String) -> Unit) {
        if (state != State.IDLE || !supported) return
        val constraints = jsObject("audio" to jsObject("echoCancellation" to true, "noiseSuppression" to true))
        scope.launch {
            val s: dynamic = try {
                (window.navigator.asDynamic().mediaDevices.getUserMedia(constraints) as kotlin.js.Promise<dynamic>).await()
            } catch (t: Throwable) {
                val name = t.asDynamic().name as? String
                Logcat.w("AudioRecord", "microphone unavailable: ${name ?: t.message}")
                onError(if (name == "NotAllowedError") "Microphone access was blocked. You can still type." else "Couldn't use the microphone. You can still type.")
                return@launch
            }
            stream = s
            val type = listOf("audio/webm;codecs=opus", "audio/mp4", "audio/ogg;codecs=opus", "audio/webm")
                .firstOrNull { window.asDynamic().MediaRecorder.isTypeSupported(it) as Boolean } ?: ""
            val options = if (type.isNotEmpty()) jsObject("mimeType" to type, "audioBitsPerSecond" to 32000) else jsObject()
            val rec = js("Reflect").construct(window.asDynamic().MediaRecorder, arrayOf(s, options))
            recorder = rec
            val chunks = mutableListOf<dynamic>()
            rec.ondataavailable = { e: dynamic -> if ((e.data.size as Number).toInt() > 0) chunks.add(e.data) }
            rec.onstop = {
                val mime = (rec.mimeType as? String)?.ifEmpty { null } ?: type.ifEmpty { "audio/webm" }
                val blob = js("Reflect").construct(window.asDynamic().Blob, arrayOf(chunks.toTypedArray(), jsObject("type" to mime)))
                val duration = (window.performance.now() - startedAt) / 1000
                cleanup()
                if (send) transcribe(blob, mime, duration, onText, onError) else state = State.IDLE
            }
            rec.start()
            startedAt = window.performance.now()
            send = true
            seconds = 0
            state = State.RECORDING
            Logcat.d("AudioRecord", "start: ${type.ifEmpty { "default" }}, 32 kbps")
            meter(s)
            timer = window.setInterval({
                seconds = ((window.performance.now() - startedAt) / 1000).toInt()
                if (seconds >= MAX_SECONDS) stop()
            }, 250)
        }
    }

    /** Stops recording; [submit] false throws the recording away. */
    fun stop(submit: Boolean = true) {
        if (state != State.RECORDING) return
        send = submit
        runCatching { recorder?.stop() }
    }

    private fun transcribe(blob: dynamic, mime: String, duration: Double, onText: (String) -> Unit, onError: (String) -> Unit) {
        state = State.TRANSCRIBING
        scope.launch {
            val started = window.performance.now()
            val (text, error) = transcribeAudio(blob, mime)
            state = State.IDLE
            when {
                error != null -> onError(error)
                text.isNullOrBlank() -> onError("Didn't catch that. Try again?")
                else -> {
                    Logcat.i("Whisper", "${(duration * 10).toInt() / 10.0} s of audio → \"$text\" (${(window.performance.now() - started).toInt()} ms)")
                    onText(text)
                }
            }
        }
    }

    /** Live input level from an analyser on the same microphone stream (no second permission prompt). */
    private fun meter(s: dynamic) {
        val ctor = window.asDynamic().AudioContext ?: window.asDynamic().webkitAudioContext ?: return
        runCatching {
            val ctx = js("Reflect").construct(ctor, arrayOf<dynamic>())
            audioCtx = ctx
            val analyser = ctx.createAnalyser()
            analyser.fftSize = 512
            ctx.createMediaStreamSource(s).connect(analyser)
            val data = js("new Uint8Array(512)")
            var tick: (Double) -> Unit = {}
            tick = {
                analyser.getByteTimeDomainData(data)
                var sum = 0.0
                for (i in 0 until 512) {
                    val v = ((data[i] as Int) - 128) / 128.0
                    sum += v * v
                }
                level = (sqrt(sum / 512) * 4).coerceIn(0.0, 1.0)
                raf = window.requestAnimationFrame(tick)
            }
            raf = window.requestAnimationFrame(tick)
        }
    }

    private fun cleanup() {
        if (raf != 0) window.cancelAnimationFrame(raf)
        raf = 0
        if (timer != 0) window.clearInterval(timer)
        timer = 0
        runCatching { (stream.getTracks() as Array<dynamic>).forEach { it.stop() } }
        runCatching { audioCtx?.close() }
        stream = null
        audioCtx = null
        recorder = null
        level = 0.0
    }
}

/** Read-aloud for answers, with the browser's speech synthesis (an on-device voice where there is one). */
object Speech {
    var speakingId by mutableStateOf<Int?>(null); private set

    val supported: Boolean get() = window.asDynamic().speechSynthesis != null

    fun speak(id: Int, text: String) {
        val synth = window.asDynamic().speechSynthesis ?: return
        synth.cancel()
        val clean = text.replace("**", "").replace(Regex("^\\s*[-*•]\\s+", RegexOption.MULTILINE), "")
        val utterance = js("Reflect").construct(window.asDynamic().SpeechSynthesisUtterance, arrayOf(clean))
        val voices = synth.getVoices() as Array<dynamic>
        val english = voices.filter { (it.lang as String).startsWith("en") }
        val voice = english.firstOrNull { it.localService == true && (it.lang as String) == "en-GB" }
            ?: english.firstOrNull { it.localService == true }
            ?: english.firstOrNull()
        if (voice != null) utterance.voice = voice
        utterance.rate = 1.03
        utterance.onend = { if (speakingId == id) speakingId = null }
        utterance.onerror = { if (speakingId == id) speakingId = null }
        speakingId = id
        synth.speak(utterance)
        Logcat.d("TextToSpeech", "speak: ${clean.length} chars${if (voice != null) " · ${voice.name}" else ""}")
    }

    fun stop() {
        window.asDynamic().speechSynthesis?.cancel()
        speakingId = null
    }
}
