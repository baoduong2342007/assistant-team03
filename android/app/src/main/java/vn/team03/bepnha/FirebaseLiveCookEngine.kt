package vn.team03.bepnha

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.os.SystemClock
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
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
import vn.team03.bepnha.voice.LiveCookSession
import vn.team03.bepnha.voice.LiveCookSetup
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

/**
 * [LiveCookEngine] backed by the Gemini Live API through Firebase AI Logic (Gemini Developer API).
 *
 * Records and plays audio itself instead of `LiveSession.startAudioConversation`: in firebase-ai
 * 18.2.0 that helper's recording loop spins on `delay(0)` after the recorder is released, so an
 * audio thread keeps one core busy after every session. The microphone is muted while Gemini
 * speaks (and briefly after), like the SDK helper without interruptions.
 *
 * Only one session holds the microphone at a time: a new session waits until the previous one
 * has released its recorder, player and echo canceller.
 */
@OptIn(PublicPreviewAPI::class)
class FirebaseLiveCookEngine(context: Context) : LiveCookEngine {
    private val audioManager = context.applicationContext.getSystemService(AudioManager::class.java)
    // Every loop catches its own errors; this only keeps an unexpected one from crashing the app.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + CoroutineExceptionHandler { _, e ->
        log("Live loop failed: ${e.javaClass.simpleName}: ${e.message?.take(300)}")
    })
    /** Held by a session from the moment it creates its audio objects until its cleanup has finished. */
    private val audioLock = Mutex()
    private val sessions = AtomicInteger()

    /** True until every opened session has fully released its audio and network resources. */
    internal val active: Boolean get() = sessions.get() > 0

    @SuppressLint("MissingPermission") // The cook screen asks for RECORD_AUDIO before opening a session.
    override suspend fun open(setup: LiveCookSetup, events: LiveCookEvents): LiveCookSession {
        if (!BuildConfig.DEBUG || !BuildConfig.FIREBASE_CONFIGURED) throw AssistantFailure(AssistantFailureKind.UNCONFIGURED)
        var live: LiveSession? = null
        var locked = false
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
            // Wait for the previous session to give the microphone back.
            withTimeout(AUDIO_WAIT_MS) { audioLock.lock() }
            locked = true
            val conversation = Conversation(live, events, acquire())
            locked = false // The conversation now owns the lock and releases it in its cleanup.
            conversation.start()
            log("Live session opened with ${BuildConfig.COOK_LIVE_MODEL}")
            return conversation
        } catch (e: Throwable) {
            if (locked) audioLock.unlock()
            live?.let { opened -> scope.launch { runCatching { opened.close() } } }
            throw when (e) {
                is TimeoutCancellationException -> AssistantFailure(AssistantFailureKind.TIMEOUT)
                is CancellationException, is AssistantFailure -> e
                is Exception -> {
                    log("Live open failed: ${describe(e)}")
                    AssistantFailure(FirebaseCalls.classify(e))
                }
                else -> e
            }
        }
    }

    /** The microphone (with echo canceller) and the player of one session. */
    private class Audio(val recorder: AudioRecord, val echo: AcousticEchoCanceler?, val track: AudioTrack)

    private fun acquire(): Audio {
        val recorder = recorder()
        var echo: AcousticEchoCanceler? = null
        try {
            if (AcousticEchoCanceler.isAvailable()) echo = AcousticEchoCanceler.create(recorder.audioSessionId)?.apply { enabled = true }
            return Audio(recorder, echo, track())
        } catch (e: Throwable) {
            echo?.release()
            recorder.release()
            throw e
        }
    }

    /** One open Live session with its own microphone, player and loops. */
    private inner class Conversation(val live: LiveSession, val events: LiveCookEvents, audio: Audio) : LiveCookSession {
        private val job = Job(scope.coroutineContext[Job])
        private val closing = AtomicBoolean(false)
        /** Elapsed-realtime when Gemini's queued voice finishes playing; the microphone waits for it. */
        @Volatile private var playedUntil = 0L
        private val playback = Channel<ByteArray>(Channel.UNLIMITED)
        private val recorder = audio.recorder
        private val echo = audio.echo
        private val track = audio.track
        // Pause music or videos while Gemini listens and talks.
        private val focus = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).setAudioAttributes(ATTRIBUTES).build()

        init {
            sessions.incrementAndGet()
            audioManager.requestAudioFocus(focus)
        }

        fun start() {
            // Microphone → Gemini, in 100 ms chunks. read() blocks, so this loop does not spin.
            scope.launch(job) {
                guard {
                    val chunk = ByteArray(MIC_RATE * 2 / 10)
                    recorder.startRecording()
                    if (recorder.recordingState != AudioRecord.RECORDSTATE_RECORDING) throw AssistantFailure(AssistantFailureKind.UNAVAILABLE)
                    while (isActive) {
                        val read = recorder.read(chunk, 0, chunk.size)
                        if (read < 0) throw AssistantFailure(AssistantFailureKind.UNAVAILABLE)
                        if (read == 0) { delay(10); continue }
                        if (SystemClock.elapsedRealtime() < playedUntil + MIC_HANGOVER_MS) continue
                        live.sendAudioRealtime(InlineData(chunk.copyOf(read), "audio/pcm"))
                    }
                }
            }
            // Gemini → speaker.
            scope.launch(job) {
                guard {
                    track.play()
                    for (data in playback) {
                        val now = SystemClock.elapsedRealtime()
                        playedUntil = maxOf(playedUntil, now) + data.size * 1000L / (SPEAKER_RATE * 2)
                        val written = track.write(data, 0, data.size)
                        if (written < 0 && written != AudioTrack.ERROR_INVALID_OPERATION) throw AssistantFailure(AssistantFailureKind.UNAVAILABLE)
                    }
                }
            }
            // Server messages: audio, transcripts, function calls and the going-away notice.
            scope.launch(job) {
                guard {
                    live.receive().collect { message ->
                        when (message) {
                            is LiveServerContent -> {
                                if (message.inputTranscription != null || message.outputTranscription != null) {
                                    events.onTranscript(message.inputTranscription?.text, message.outputTranscription?.text)
                                }
                                if (message.turnComplete) events.onTurnComplete()
                                if (message.interrupted) silence()
                                else message.content?.parts?.filterIsInstance<InlineDataPart>()?.forEach { playback.trySend(it.inlineData) }
                            }
                            is LiveServerToolCall -> live.sendFunctionResponse(message.functionCalls.map { call ->
                                FunctionResponsePart(call.name, respond(events, call), call.id)
                            })
                            is LiveServerGoAway -> events.onGoAway()
                            else -> Unit
                        }
                    }
                    // receive() ends without an error when the server closes the socket.
                    if (!closing.get()) {
                        log("Live session ended by the server")
                        events.onClosed(null)
                        close()
                    }
                }
            }
        }

        /** Drops queued and buffered speech so the microphone opens again right away. */
        private fun silence() {
            while (playback.tryReceive().isSuccess) Unit
            runCatching { track.pause(); track.flush(); track.play() }
            playedUntil = 0L
        }

        override fun say(text: String) {
            if (closing.get()) return
            scope.launch(job) { guard { live.sendTextRealtime(text) } }
        }

        override fun close() {
            if (!closing.compareAndSet(false, true)) return
            scope.launch {
                log("Live session closing")
                job.cancel()
                // Unblock a pending read and write so the loops see the cancellation at once.
                runCatching { recorder.stop() }
                runCatching { track.pause(); track.flush() }
                runCatching { live.stopReceiving() }
                job.join()
                echo?.release()
                recorder.release()
                track.release()
                audioManager.abandonAudioFocusRequest(focus)
                runCatching { live.close() }
                sessions.decrementAndGet()
                audioLock.unlock()
                log("Live session closed")
            }
        }

        /** Runs [block]; any failure other than cancellation ends this session and is reported once. */
        private suspend fun guard(block: suspend CoroutineScope.() -> Unit) {
            try {
                coroutineScope(block)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (closing.get()) return
                log("Live session failed: ${describe(e)}")
                events.onClosed(e as? AssistantFailure ?: AssistantFailure(FirebaseCalls.classify(e)))
                close()
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
        return recorder
    }

    private fun track(): AudioTrack {
        val min = AudioTrack.getMinBufferSize(SPEAKER_RATE, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
        val track = AudioTrack.Builder()
            .setAudioAttributes(ATTRIBUTES)
            .setAudioFormat(AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(SPEAKER_RATE)
                .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setTransferMode(AudioTrack.MODE_STREAM)
            .setBufferSizeInBytes(maxOf(min, SPEAKER_RATE * 2 / 5))
            .build()
        if (track.state != AudioTrack.STATE_INITIALIZED) {
            track.release()
            throw AssistantFailure(AssistantFailureKind.UNAVAILABLE)
        }
        return track
    }

    private fun describe(e: Throwable) = FirebaseCalls.causes(e).joinToString(" <- ") { "${it.javaClass.simpleName}: ${it.message?.take(300)}" }

    private fun log(message: String) { if (BuildConfig.DEBUG) android.util.Log.d("BepNhaLive", message) }

    companion object {
        private const val CONNECT_TIMEOUT_MS = 15_000L
        /** How long a new session waits for the previous one to release the microphone. */
        private const val AUDIO_WAIT_MS = 3_000L
        private const val CALL_TIMEOUT_MS = 5_000L
        /** Live API audio formats: 16 kHz in, 24 kHz out, 16-bit mono PCM. */
        private const val MIC_RATE = 16_000
        private const val SPEAKER_RATE = 24_000
        /** Keeps the microphone muted a little after Gemini's voice ends, for room echo. */
        private const val MIC_HANGOVER_MS = 300L
        private val ATTRIBUTES: AudioAttributes = AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ASSISTANT)
            .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()

        internal fun declarations(): List<FunctionDeclaration> = CookLiveTools.specs.map { spec ->
            FunctionDeclaration(
                spec.name, spec.description,
                if (spec.takesMinutes) mapOf(CookLiveTools.MINUTES to Schema.integer("Số phút, từ 1 đến 180")) else emptyMap(),
            )
        }
    }
}
