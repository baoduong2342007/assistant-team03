package vn.team03.bepnha.voice

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

/** [RecognizerEngine] backed by the platform [SpeechRecognizer]. Main thread only. */
internal class AndroidRecognizerEngine(private val context: Context) : RecognizerEngine {
    private var recognizer: SpeechRecognizer? = null

    override fun isAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    override fun hasPermission(): Boolean = VoicePermission.isGranted(context)

    override fun start(preferOffline: Boolean, listener: RecognizerListener) {
        val engine = recognizer ?: try {
            SpeechRecognizer.createSpeechRecognizer(context).also { recognizer = it }
        } catch (_: RuntimeException) {
            listener.onError(ERROR_LANGUAGE_NOT_SUPPORTED); return
        }
        engine.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) = Unit
            override fun onBeginningOfSpeech() = Unit
            override fun onRmsChanged(rmsdB: Float) = Unit
            override fun onBufferReceived(buffer: ByteArray?) = Unit
            override fun onEndOfSpeech() = Unit
            override fun onEvent(eventType: Int, params: Bundle?) = Unit
            override fun onError(error: Int) = listener.onError(error)
            override fun onPartialResults(partialResults: Bundle?) {
                best(partialResults)?.let(listener::onPartial)
            }
            override fun onResults(results: Bundle?) = listener.onFinal(best(results))
        })
        try {
            engine.startListening(intent(preferOffline))
        } catch (_: SecurityException) {
            listener.onError(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS)
        }
    }

    private fun best(bundle: Bundle?): String? =
        bundle?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull { it.isNotBlank() }

    private fun intent(preferOffline: Boolean) = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, VoiceController.LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, VoiceController.LANGUAGE)
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, preferOffline)
        putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        // Older users pause mid-sentence; give them time before the recognizer gives up.
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
        putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2_000L)
    }

    override fun stop() { recognizer?.stopListening() }

    override fun cancel() { recognizer?.cancel() }

    override fun destroy() {
        recognizer?.destroy()
        recognizer = null
    }
}

/** `SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED`, a constant added in API 31. */
private const val ERROR_LANGUAGE_NOT_SUPPORTED = 12

/** [SpeakerEngine] backed by the platform [TextToSpeech]. */
internal class AndroidSpeakerEngine(private val context: Context) : SpeakerEngine {
    @Volatile private var tts: TextToSpeech? = null
    @Volatile private var ready = false
    @Volatile private var listener: SpeakerListener? = null

    override val maxInputLength: Int = TextToSpeech.getMaxSpeechInputLength()

    override fun init(listener: SpeakerListener) {
        this.listener = listener
        tts = TextToSpeech(context) { status ->
            val engine = tts
            if (status != TextToSpeech.SUCCESS || engine == null) {
                listener.onInit(success = false, vietnamese = false)
                return@TextToSpeech
            }
            val result = try {
                engine.setLanguage(Locale.forLanguageTag(VoiceController.LANGUAGE))
            } catch (_: RuntimeException) {
                TextToSpeech.LANG_NOT_SUPPORTED
            }
            val vietnamese = result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED
            engine.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ASSISTANT)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) = Unit
                override fun onDone(utteranceId: String?) { utteranceId?.let(listener::onDone) }
                @Deprecated("Abstract in the platform class; the two-argument overload is preferred.")
                override fun onError(utteranceId: String?) { utteranceId?.let(listener::onError) }
                override fun onError(utteranceId: String?, errorCode: Int) { utteranceId?.let(listener::onError) }
            })
            ready = vietnamese
            listener.onInit(success = true, vietnamese = vietnamese)
        }
    }

    override fun speak(text: String, rate: Float, utteranceId: String, flush: Boolean) {
        val engine = tts?.takeIf { ready } ?: return
        engine.setSpeechRate(rate)
        val mode = if (flush) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
        if (engine.speak(text, mode, null, utteranceId) != TextToSpeech.SUCCESS) listener?.onError(utteranceId)
    }

    override fun stop() { tts?.stop() }

    override fun shutdown() {
        ready = false
        listener = null
        tts?.shutdown()
        tts = null
    }
}
