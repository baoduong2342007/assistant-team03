package vn.team03.bepnha

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.SystemClock
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.json.JSONObject
import vn.team03.bepnha.voice.LiveCall
import vn.team03.bepnha.voice.LiveCookEngine
import vn.team03.bepnha.voice.LiveCookEvents
import vn.team03.bepnha.voice.LiveCookSetup

/**
 * [LiveCookEngine] backed by the Gemini Live API through Firebase AI Logic (Gemini Developer API).
 *
 * Records and plays audio itself instead of `LiveSession.startAudioConversation`: in firebase-ai
 * 18.2.0 that helper's recording loop spins on `delay(0)` after the recorder is released, so an
 * audio thread keeps one core busy after every session. The microphone is muted while Gemini
 * speaks (and briefly after), like the SDK helper without interruptions.
 */
@OptIn(PublicPreviewAPI::class)
class FirebaseLiveCookEngine : LiveCookEngine {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    @Volatile private var current: Conversation? = null

    /** True while a session's audio and network loops are running. */
    internal val active: Boolean get() = current?.job?.isActive == true

    private class Conversation(val live: LiveSession, val recorder: AudioRecord, val track: AudioTrack, val job: Job) {
        @Volatile var closing = false
        /** Elapsed-realtime when Gemini's queued voice finishes playing; the microphone waits for it. */
        @Volatile var playedUntil = 0L
        val playback = Channel<ByteArray>(Channel.UNLIMITED)
    }

    @SuppressLint("MissingPermission") // The cook screen asks for RECORD_AUDIO before opening a session.
    override suspend fun open(setup: LiveCookSetup, events: LiveCookEvents) {
        if (!BuildConfig.DEBUG || !BuildConfig.FIREBASE_CONFIGURED) throw AssistantFailure(AssistantFailureKind.UNCONFIGURED)
        close()
        var live: LiveSession? = null
        try {
            FirebaseCalls.checkAppToken()
            val model = Firebase.ai(backend = GenerativeBackend.googleAI(), useLimitedUseAppCheckTokens = true).liveModel(
                modelName = BuildConfig.COOK_LIVE_MODEL,
                generationConfig = liveGenerationConfig {
                    responseModality = ResponseModality.AUDIO
                    speechConfig = SpeechConfig(voice = Voice(setup.voice))
                    inputAudioTranscription = AudioTranscriptionConfig()
                    outputAudioTranscription = AudioTranscriptionConfig()
                },
                tools = listOf(Tool.functionDeclarations(declarations())),
                systemInstruction = content { text(setup.instruction) },
            )
            live = withTimeout(CONNECT_TIMEOUT_MS) { model.connect() }
            val recorder = recorder()
            val track = try { track() } catch (e: Exception) { recorder.release(); throw e }
            // A job of the engine's own scope, never of the caller's coroutine.
            val conversation = Conversation(live, recorder, track, Job(scope.coroutineContext[Job]))
            current = conversation
            start(conversation, events)
            log("Live session opened with ${BuildConfig.COOK_LIVE_MODEL}")
        } catch (e: Throwable) {
            val failure = when (e) {
                is TimeoutCancellationException -> AssistantFailure(AssistantFailureKind.TIMEOUT)
                is CancellationException, is AssistantFailure -> e
                is Exception -> {
                    log("Live open failed: ${FirebaseCalls.causes(e).joinToString(" <- ") { "${it.javaClass.simpleName}: ${it.message?.take(500)}" }}")
                    AssistantFailure(FirebaseCalls.classify(e))
                }
                else -> e
            }
            close()
            live?.let { opened -> scope.launch { runCatching { opened.close() } } }
            throw failure
        }
    }

    override fun say(text: String) {
        val conversation = current ?: return
        scope.launch(conversation.job) {
            try {
                conversation.live.sendTextRealtime(text)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                log("Live text failed: ${e.javaClass.simpleName}: ${e.message?.take(500)}")
            }
        }
    }

    override fun close() {
        val conversation = current ?: return
        current = null
        conversation.closing = true
        scope.launch {
            log("Live session closing")
            conversation.job.cancel()
            // Unblock a pending write so the playback loop can see the cancellation.
            runCatching { conversation.track.pause(); conversation.track.flush() }
            runCatching { conversation.live.stopReceiving() }
            conversation.job.join()
            runCatching { conversation.recorder.stop() }
            conversation.recorder.release()
            conversation.track.release()
            runCatching { conversation.live.close() }
            log("Live session closed")
        }
    }

    private fun start(conversation: Conversation, events: LiveCookEvents) {
        val live = conversation.live
        // Microphone → Gemini, in 100 ms chunks. read() blocks, so this loop does not spin.
        scope.launch(conversation.job) {
            val chunk = ByteArray(MIC_RATE * 2 / 10)
            conversation.recorder.startRecording()
            while (isActive) {
                val read = conversation.recorder.read(chunk, 0, chunk.size)
                if (read <= 0) { if (read < 0) break else continue }
                if (SystemClock.elapsedRealtime() < conversation.playedUntil + MIC_HANGOVER_MS) continue
                live.sendAudioRealtime(InlineData(chunk.copyOf(read), "audio/pcm"))
            }
        }
        // Gemini → speaker.
        scope.launch(conversation.job) {
            conversation.track.play()
            for (data in conversation.playback) {
                val now = SystemClock.elapsedRealtime()
                conversation.playedUntil = maxOf(conversation.playedUntil, now) + data.size * 1000L / (SPEAKER_RATE * 2)
                conversation.track.write(data, 0, data.size)
            }
        }
        // Server messages: audio, transcripts, function calls and the going-away notice.
        scope.launch(conversation.job) {
            var error: Throwable? = null
            try {
                live.receive().collect { message ->
                    when (message) {
                        is LiveServerContent -> {
                            if (message.inputTranscription != null || message.outputTranscription != null) {
                                events.onTranscript(message.inputTranscription?.text, message.outputTranscription?.text)
                            }
                            if (message.turnComplete) events.onTurnComplete()
                            if (message.interrupted) {
                                while (conversation.playback.tryReceive().isSuccess) Unit
                            } else {
                                message.content?.parts?.filterIsInstance<InlineDataPart>()?.forEach { conversation.playback.trySend(it.inlineData) }
                            }
                        }
                        is LiveServerToolCall -> live.sendFunctionResponse(message.functionCalls.map { call ->
                            FunctionResponsePart(call.name, respond(events, call), call.id)
                        })
                        is LiveServerGoAway -> events.onGoAway()
                        else -> Unit
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                error = e
            }
            if (!conversation.closing) {
                log("Live session ended by the server${error?.let { ": ${it.javaClass.simpleName}: ${it.message?.take(300)}" } ?: ""}")
                events.onClosed(error)
            }
        }
    }

    /** The app's state belongs to the main thread. */
    private suspend fun respond(events: LiveCookEvents, call: FunctionCallPart): JsonObject {
        val args = call.args.mapValues { (_, value) -> (value as? JsonPrimitive)?.content ?: value.toString() }
        val result = try {
            withContext(Dispatchers.Main.immediate) { withTimeout(CALL_TIMEOUT_MS) { events.onCall(LiveCall(call.name, args)) } }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            JSONObject().put("ok", false).put("error", "Ứng dụng chưa làm được việc này.")
        }
        return Json.parseToJsonElement(result.toString()).jsonObject
    }

    @SuppressLint("MissingPermission")
    private fun recorder(): AudioRecord {
        val format = AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(MIC_RATE)
            .setChannelMask(AudioFormat.CHANNEL_IN_MONO).build()
        val min = AudioRecord.getMinBufferSize(MIC_RATE, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val recorder = AudioRecord.Builder().setAudioSource(MediaRecorder.AudioSource.VOICE_COMMUNICATION)
            .setAudioFormat(format).setBufferSizeInBytes(maxOf(min, MIC_RATE * 2 / 2)).build()
        if (recorder.state != AudioRecord.STATE_INITIALIZED) {
            recorder.release()
            throw AssistantFailure(AssistantFailureKind.UNAVAILABLE)
        }
        if (AcousticEchoCanceler.isAvailable()) AcousticEchoCanceler.create(recorder.audioSessionId)?.enabled = true
        return recorder
    }

    private fun track(): AudioTrack {
        val min = AudioTrack.getMinBufferSize(SPEAKER_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        return AudioTrack.Builder()
            .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SPEAKER_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(min, SPEAKER_RATE * 2 / 5))
            .build()
    }

    private fun log(message: String) { if (BuildConfig.DEBUG) android.util.Log.d("BepNhaLive", message) }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000L
        private const val CALL_TIMEOUT_MS = 5_000L
        /** Live API audio formats: 16 kHz in, 24 kHz out, 16-bit mono PCM. */
        private const val MIC_RATE = 16_000
        private const val SPEAKER_RATE = 24_000
        /** Keeps the microphone muted a little after Gemini's voice ends, for room echo. */
        private const val MIC_HANGOVER_MS = 300L

        internal fun declarations(): List<FunctionDeclaration> = CookLiveTools.specs.map { spec ->
            FunctionDeclaration(
                spec.name, spec.description,
                if (spec.takesMinutes) mapOf(CookLiveTools.MINUTES to Schema.integer("Số phút, từ 1 đến 180")) else emptyMap(),
            )
        }
    }
}
