package vn.team03.bepnha.voice

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Why listening failed, worded for what the user can do about it. */
enum class VoiceErrorKind {
    /** Nothing or nothing clear was heard: ask the user to speak again. */
    NotHeard,
    /** Recognition needs a network that is not reachable right now. */
    NoNetwork,
    /** RECORD_AUDIO has not been granted. */
    NoPermission,
    /** The device has no recognizer, or none that understands Vietnamese. */
    Unsupported,
}

sealed interface VoiceState {
    data object Idle : VoiceState
    data class Listening(val partial: String = "") : VoiceState
    data class Result(val text: String) : VoiceState
    data class Error(val kind: VoiceErrorKind) : VoiceState
}

enum class SpeechState {
    /** The text-to-speech engine is still starting; [VoiceController.speak] is queued. */
    Initializing,
    Ready,
    Speaking,
    /** No Vietnamese voice (or no engine at all). Speaking is a silent no-op. */
    Unsupported,
}

/** Callbacks from a speech recognizer, delivered on the main thread. */
interface RecognizerListener {
    fun onPartial(text: String)
    fun onFinal(text: String?)
    /** [code] is one of the `SpeechRecognizer.ERROR_*` values. */
    fun onError(code: Int)
}

/** Thin seam over `android.speech.SpeechRecognizer`, faked in JVM tests. */
interface RecognizerEngine {
    fun isAvailable(): Boolean
    fun hasPermission(): Boolean
    fun start(preferOffline: Boolean, listener: RecognizerListener)
    fun stop()
    fun cancel()
    fun destroy()
}

/** Callbacks from a text-to-speech engine; may arrive on any thread. */
interface SpeakerListener {
    /** [vietnamese] is false when the engine started but has no usable vi-VN voice. */
    fun onInit(success: Boolean, vietnamese: Boolean)
    fun onDone(utteranceId: String)
    fun onError(utteranceId: String)
}

/** Thin seam over `android.speech.tts.TextToSpeech`, faked in JVM tests. */
interface SpeakerEngine {
    /** Starts the engine; [SpeakerListener.onInit] reports the outcome exactly once. */
    fun init(listener: SpeakerListener)
    val maxInputLength: Int
    fun speak(text: String, rate: Float, utteranceId: String, flush: Boolean)
    fun stop()
    fun shutdown()
}

/**
 * Voice in and out for the assistant: listen in Vietnamese, then read replies aloud.
 *
 * Listening stops any reply being read so the microphone does not hear the phone itself.
 * Call [start], [stop], [release] and [speak] from the main thread.
 */
class VoiceController(
    private val recognizer: RecognizerEngine,
    private val speaker: SpeakerEngine,
    rate: Float = DEFAULT_RATE,
) {
    companion object {
        const val LANGUAGE = "vi-VN"
        const val DEFAULT_RATE = 0.85f
        const val MIN_RATE = 0.5f
        const val MAX_RATE = 1.5f

        fun create(context: Context): VoiceController =
            VoiceController(AndroidRecognizerEngine(context.applicationContext), AndroidSpeakerEngine(context.applicationContext))

        /** Maps `SpeechRecognizer.ERROR_*` codes (literal values: some constants need API 31+). */
        fun classify(code: Int): VoiceErrorKind = when (code) {
            9 -> VoiceErrorKind.NoPermission // ERROR_INSUFFICIENT_PERMISSIONS
            1, 2, 4, 10, 11 -> VoiceErrorKind.NoNetwork // NETWORK_TIMEOUT, NETWORK, SERVER, TOO_MANY_REQUESTS, SERVER_DISCONNECTED
            12, 13, 14, 15 -> VoiceErrorKind.Unsupported // LANGUAGE_NOT_SUPPORTED, LANGUAGE_UNAVAILABLE, CANNOT_CHECK_SUPPORT, ...DOWNLOAD_EVENTS
            else -> VoiceErrorKind.NotHeard // AUDIO, CLIENT, SPEECH_TIMEOUT, NO_MATCH, RECOGNIZER_BUSY
        }

        private fun isLanguageError(code: Int) = code == 12 || code == 13

        /** Splits text at sentence ends so each piece fits the engine's input limit. */
        fun chunk(text: String, max: Int): List<String> {
            val clean = text.trim().replace("\\s+".toRegex(), " ")
            if (clean.isEmpty()) return emptyList()
            if (clean.length <= max) return listOf(clean)
            val pieces = mutableListOf<String>()
            var rest = clean
            while (rest.length > max) {
                val window = rest.substring(0, max)
                val cut = listOf(". ", "! ", "? ", "; ", ", ", " ")
                    .map { window.lastIndexOf(it).let { at -> if (at > 0) at + 1 else -1 } }
                    .firstOrNull { it > 0 } ?: max
                pieces += rest.substring(0, cut).trim()
                rest = rest.substring(cut).trim()
            }
            if (rest.isNotEmpty()) pieces += rest
            return pieces
        }
    }

    private val _state = MutableStateFlow<VoiceState>(VoiceState.Idle)
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val _speech = MutableStateFlow(SpeechState.Initializing)
    val speech: StateFlow<SpeechState> = _speech.asStateFlow()

    @Volatile var rate: Float = rate.coerceIn(MIN_RATE, MAX_RATE)
        private set

    private val lock = Any()
    private var session = 0
    private var utterance = 0
    private var lastUtteranceId: String? = null
    private var pendingText: String? = null
    private var released = false

    init {
        speaker.init(object : SpeakerListener {
            override fun onInit(success: Boolean, vietnamese: Boolean) {
                val pending = synchronized(lock) {
                    if (released) return
                    _speech.value = if (success && vietnamese) SpeechState.Ready else SpeechState.Unsupported
                    pendingText.also { pendingText = null }
                }
                if (pending != null) speak(pending)
            }
            override fun onDone(utteranceId: String) = finished(utteranceId, error = false)
            override fun onError(utteranceId: String) = finished(utteranceId, error = true)
        })
    }

    // ---- Listening ----

    fun start() {
        if (released) return
        if (!recognizer.isAvailable()) { _state.value = VoiceState.Error(VoiceErrorKind.Unsupported); return }
        if (!recognizer.hasPermission()) { _state.value = VoiceState.Error(VoiceErrorKind.NoPermission); return }
        stopSpeaking()
        recognizer.cancel()
        listen(++session, preferOffline = true)
    }

    private fun listen(id: Int, preferOffline: Boolean) {
        _state.value = VoiceState.Listening()
        recognizer.start(preferOffline, object : RecognizerListener {
            override fun onPartial(text: String) {
                if (id == session && _state.value is VoiceState.Listening) _state.value = VoiceState.Listening(text.trim())
            }
            override fun onFinal(text: String?) {
                if (id != session) return
                val heard = text?.trim().orEmpty()
                _state.value = if (heard.isEmpty()) VoiceState.Error(VoiceErrorKind.NotHeard) else VoiceState.Result(heard)
            }
            override fun onError(code: Int) {
                if (id != session) return
                // An offline-only request fails when the Vietnamese pack is not downloaded: try online once.
                if (preferOffline && isLanguageError(code)) { listen(id, preferOffline = false); return }
                _state.value = VoiceState.Error(classify(code))
            }
        })
    }

    /** Stops listening and lets the recognizer deliver what it heard so far. */
    fun stop() {
        if (_state.value is VoiceState.Listening) recognizer.stop()
    }

    /** Clears a shown result or error back to [VoiceState.Idle]; cancels listening. */
    fun reset() {
        session++
        recognizer.cancel()
        _state.value = VoiceState.Idle
    }

    // ---- Speaking ----

    fun setRate(rate: Float) { this.rate = rate.coerceIn(MIN_RATE, MAX_RATE) }

    /** Reads [text] aloud, interrupting anything being read. Queued until the engine is ready. */
    fun speak(text: String) {
        val pieces = chunk(text, speaker.maxInputLength.coerceAtLeast(1))
        synchronized(lock) {
            if (released) return
            when (_speech.value) {
                SpeechState.Unsupported -> return
                SpeechState.Initializing -> { pendingText = text; return }
                else -> Unit
            }
            if (pieces.isEmpty()) { stopSpeakingLocked(); return }
            val prefix = "bepnha-${++utterance}"
            lastUtteranceId = "$prefix-${pieces.lastIndex}"
            _speech.value = SpeechState.Speaking
            pieces.forEachIndexed { i, piece -> speaker.speak(piece, rate, "$prefix-$i", flush = i == 0) }
        }
    }

    fun stopSpeaking() = synchronized(lock) { stopSpeakingLocked() }

    private fun stopSpeakingLocked() {
        pendingText = null
        if (lastUtteranceId == null) return
        lastUtteranceId = null
        speaker.stop()
        if (_speech.value == SpeechState.Speaking) _speech.value = SpeechState.Ready
    }

    private fun finished(utteranceId: String, error: Boolean) = synchronized(lock) {
        val current = lastUtteranceId ?: return
        // A failed piece ends the whole reply; otherwise only the last piece does.
        val sameReply = utteranceId.substringBeforeLast('-') == current.substringBeforeLast('-')
        if (utteranceId == current || (error && sameReply)) {
            if (error) speaker.stop()
            lastUtteranceId = null
            if (_speech.value == SpeechState.Speaking) _speech.value = SpeechState.Ready
        }
    }

    fun release() {
        synchronized(lock) {
            if (released) return
            released = true
            pendingText = null
            lastUtteranceId = null
        }
        session++
        recognizer.destroy()
        speaker.shutdown()
        _state.value = VoiceState.Idle
        _speech.value = SpeechState.Unsupported
    }
}
