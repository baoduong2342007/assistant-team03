package vn.team03.bepnha.voice

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import vn.team03.bepnha.AssistantFailure
import vn.team03.bepnha.AssistantFailureKind

/** A function the Live model asked the app to run. [args] holds each argument as plain text. */
data class LiveCall(val name: String, val args: Map<String, String>)

/** What a Live session is configured with when it opens. */
data class LiveCookSetup(val instruction: String, val voice: String)

/** Callbacks from a [LiveCookEngine]. */
interface LiveCookEvents {
    /** Runs on the main thread; the returned object is sent back to the model as the function result. */
    fun onCall(call: LiveCall): JSONObject
    /** Pieces of what the user said ([user]) and what the model said ([model]); may arrive on any thread. */
    fun onTranscript(user: String?, model: String?)
    /** The model finished a turn; may arrive on any thread. */
    fun onTurnComplete() = Unit
    /** The server will end this connection soon; may arrive on any thread. */
    fun onGoAway()
    /** The connection ended without [LiveCookEngine.close]; may arrive on any thread. */
    fun onClosed(error: Throwable?)
}

/** Thin seam over the Gemini Live SDK, faked in JVM tests. */
interface LiveCookEngine {
    /**
     * Connects and starts the microphone/speaker conversation. Throws [AssistantFailure] when it
     * cannot. Needs RECORD_AUDIO, which the caller checks first.
     */
    suspend fun open(setup: LiveCookSetup, events: LiveCookEvents)
    /** Sends a text turn (an app note) to the open session; failures arrive as [LiveCookEvents.onClosed]. */
    fun say(text: String)
    /** Ends the current session, if any. Never reports [LiveCookEvents.onClosed]. Safe to call twice. */
    fun close()
}

sealed interface LiveCookState {
    data object Off : LiveCookState
    data object Connecting : LiveCookState
    /** The last exchange: what the user said and what the model answered. */
    data class Active(val user: String = "", val model: String = "") : LiveCookState
    data class Failed(val kind: AssistantFailureKind) : LiveCookState
}

/**
 * One tap-to-talk Live conversation while cooking. Opens on [start], closes on [stop] or after
 * [idleMillis] without speech or function calls. When the server announces it will end the
 * connection, a fresh session is opened with the current setup (the cooking state lives in the
 * app, so nothing is lost). Any failure ends in [LiveCookState.Failed] without retrying.
 *
 * Call everything from the main thread; [scope] must run on it too.
 */
class LiveCookController(
    private val engine: LiveCookEngine,
    private val scope: CoroutineScope,
    private val idleMillis: Long = IDLE_MILLIS,
) {
    companion object {
        const val IDLE_MILLIS = 60_000L
    }

    private val _state = MutableStateFlow<LiveCookState>(LiveCookState.Off)
    val state: StateFlow<LiveCookState> = _state.asStateFlow()

    /** Handles the model's function calls on the main thread. */
    var onCall: (LiveCall) -> JSONObject = { JSONObject().put("ok", false) }

    val isOpen: Boolean get() = _state.value.let { it is LiveCookState.Connecting || it is LiveCookState.Active }

    private var generation = 0
    private var idle: Job? = null
    private var setup: () -> LiveCookSetup? = { null }
    /** Whether the last transcript piece came from the model, so the next user piece starts a new exchange. */
    private var modelSpokeLast = false
    /** The model ended its turn; its next words (an answer to an app note) start a new exchange too. */
    private var turnEnded = false

    /** Opens a session; [setup] is read again on every reconnect. [greeting] is sent once after the first connect. */
    fun start(setup: () -> LiveCookSetup?, greeting: String? = null) {
        if (isOpen) return
        this.setup = setup
        val id = ++generation
        modelSpokeLast = false
        turnEnded = false
        _state.value = LiveCookState.Connecting
        scope.launch { connect(id, greeting) }
    }

    fun stop() {
        generation++
        idle?.cancel()
        engine.close()
        _state.value = LiveCookState.Off
    }

    /** Sends an app note to the model (it then speaks); ignored unless a session is active. */
    fun say(text: String) {
        if (_state.value !is LiveCookState.Active || text.isBlank()) return
        touch(generation)
        engine.say(text)
    }

    private suspend fun connect(id: Int, greeting: String?) {
        val config = setup() ?: run { if (id == generation) stop(); return }
        try {
            engine.open(config, events(id))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Throwable) {
            if (id == generation) fail(e)
            return
        }
        // Stopped while connecting: drop the session that just opened.
        if (id != generation) { engine.close(); return }
        if (_state.value !is LiveCookState.Active) _state.value = LiveCookState.Active()
        touch(id)
        greeting?.let(engine::say)
    }

    private fun events(id: Int) = object : LiveCookEvents {
        override fun onCall(call: LiveCall): JSONObject {
            if (id != generation) return JSONObject().put("ok", false).put("error", "Phiên trò chuyện đã kết thúc.")
            touch(id)
            return this@LiveCookController.onCall(call)
        }
        override fun onTranscript(user: String?, model: String?) {
            scope.launch { if (id == generation) transcript(id, user, model) }
        }
        override fun onTurnComplete() {
            scope.launch { if (id == generation) turnEnded = true }
        }
        override fun onGoAway() {
            scope.launch {
                if (id != generation || !isOpen) return@launch
                engine.close()
                connect(id, greeting = null)
            }
        }
        override fun onClosed(error: Throwable?) {
            scope.launch { if (id == generation && isOpen) fail(error ?: AssistantFailure(AssistantFailureKind.NETWORK)) }
        }
    }

    private fun transcript(id: Int, user: String?, model: String?) {
        val current = _state.value as? LiveCookState.Active ?: return
        var heard = current.user
        var answer = current.model
        if (!user.isNullOrEmpty()) {
            if (modelSpokeLast) { heard = ""; answer = "" }
            heard += user
            modelSpokeLast = false
            turnEnded = false
        }
        if (!model.isNullOrEmpty()) {
            if (turnEnded) { heard = ""; answer = ""; turnEnded = false }
            answer += model
            modelSpokeLast = true
        }
        _state.value = LiveCookState.Active(tidy(heard), tidy(answer))
        touch(id)
    }

    private fun tidy(text: String) = text.replace(Regex("\\s+"), " ").trimStart()

    private fun touch(id: Int) {
        idle?.cancel()
        idle = scope.launch {
            delay(idleMillis)
            if (id == generation) stop()
        }
    }

    private fun fail(error: Throwable) {
        generation++
        idle?.cancel()
        engine.close()
        _state.value = LiveCookState.Failed((error as? AssistantFailure)?.kind ?: AssistantFailureKind.UNAVAILABLE)
    }
}
