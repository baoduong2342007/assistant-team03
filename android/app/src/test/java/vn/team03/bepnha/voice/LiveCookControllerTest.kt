package vn.team03.bepnha.voice

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import vn.team03.bepnha.AssistantFailure
import vn.team03.bepnha.AssistantFailureKind

@OptIn(ExperimentalCoroutinesApi::class)
class LiveCookControllerTest {
    private class FakeEngine : LiveCookEngine {
        var failWith: Throwable? = null
        var gate: CompletableDeferred<Unit>? = null
        val opened = mutableListOf<LiveCookSetup>()
        val said = mutableListOf<String>()
        var closes = 0
        lateinit var events: LiveCookEvents
        override suspend fun open(setup: LiveCookSetup, events: LiveCookEvents) {
            gate?.await()
            failWith?.let { throw it }
            opened += setup
            this.events = events
        }
        override fun say(text: String) { said += text }
        override fun close() { closes++ }
    }

    private val engine = FakeEngine()
    private var step = 1
    private fun setup() = LiveCookSetup("bước $step", "Kore")

    private fun TestScope.controller(idle: Long = 60_000) =
        LiveCookController(engine, backgroundScope, idle).also { it.start(::setup, greeting = "chào"); runCurrent() }

    @Test fun startConnectsWithTheSetupAndGreetsOnce() = runTest {
        val live = controller()
        assertEquals(LiveCookState.Active(), live.state.value)
        assertTrue(live.isOpen)
        assertEquals(listOf(LiveCookSetup("bước 1", "Kore")), engine.opened)
        assertEquals(listOf("chào"), engine.said)
        live.start(::setup, "chào") // Already open: ignored.
        runCurrent()
        assertEquals(1, engine.opened.size)
    }

    @Test fun silenceClosesTheSessionAndSpeechKeepsItOpen() = runTest {
        val live = controller()
        advanceTimeBy(50_000)
        engine.events.onTranscript("tiếp", null)
        runCurrent()
        advanceTimeBy(50_000)
        assertTrue(live.isOpen)
        advanceTimeBy(10_001)
        assertEquals(LiveCookState.Off, live.state.value)
        assertEquals(1, engine.closes)
    }

    @Test fun functionCallsReachTheHandlerAndKeepTheSessionOpen() = runTest {
        val live = controller()
        live.onCall = { call -> JSONObject().put("ok", true).put("name", call.name) }
        advanceTimeBy(59_000)
        val result = engine.events.onCall(LiveCall("next_step", emptyMap()))
        assertEquals("next_step", result.getString("name"))
        advanceTimeBy(59_000)
        assertTrue(live.isOpen)
    }

    @Test fun transcriptShowsTheLastExchange() = runTest {
        val live = controller()
        engine.events.onTranscript("Cá", null)
        engine.events.onTranscript(" chín chưa?", null)
        engine.events.onTranscript(null, "Khi thịt cá")
        engine.events.onTranscript(null, " trắng đục là chín.")
        runCurrent()
        assertEquals(LiveCookState.Active("Cá chín chưa?", "Khi thịt cá trắng đục là chín."), live.state.value)
        engine.events.onTranscript(" Tiếp đi", null)
        runCurrent()
        assertEquals(LiveCookState.Active("Tiếp đi", ""), live.state.value)
    }

    @Test fun aNewModelTurnStartsAFreshExchange() = runTest {
        val live = controller()
        engine.events.onTranscript(null, "Chào bạn, mình đang nghe.")
        engine.events.onTurnComplete()
        engine.events.onTranscript(null, "Bước 2 trên 15.")
        runCurrent()
        assertEquals(LiveCookState.Active("", "Bước 2 trên 15."), live.state.value)
        // A user turn after the model finished keeps the model's next answer with it.
        engine.events.onTurnComplete()
        engine.events.onTranscript("Hẹn giờ 5 phút", null)
        engine.events.onTranscript(null, "Đã hẹn 5 phút.")
        runCurrent()
        assertEquals(LiveCookState.Active("Hẹn giờ 5 phút", "Đã hẹn 5 phút."), live.state.value)
    }

    @Test fun goAwayReopensWithTheCurrentStepWithoutGreetingAgain() = runTest {
        val live = controller()
        engine.events.onTranscript("hỏi", "đáp")
        runCurrent()
        step = 4
        engine.events.onGoAway()
        runCurrent()
        assertEquals(listOf("bước 1", "bước 4"), engine.opened.map { it.instruction })
        assertEquals(1, engine.closes)
        assertEquals(listOf("chào"), engine.said)
        assertTrue(live.isOpen)
    }

    @Test fun failuresEndTheSessionWithTheirKind() = runTest {
        engine.failWith = AssistantFailure(AssistantFailureKind.QUOTA)
        val live = controller()
        assertEquals(LiveCookState.Failed(AssistantFailureKind.QUOTA), live.state.value)
        assertFalse(live.isOpen)

        engine.failWith = IllegalStateException("socket")
        live.start(::setup)
        runCurrent()
        assertEquals(LiveCookState.Failed(AssistantFailureKind.UNAVAILABLE), live.state.value)
    }

    @Test fun aDroppedConnectionFailsAsANetworkProblem() = runTest {
        val live = controller()
        engine.events.onClosed(null)
        runCurrent()
        assertEquals(LiveCookState.Failed(AssistantFailureKind.NETWORK), live.state.value)
    }

    @Test fun stoppingWhileConnectingDropsTheSessionThatOpensLate() = runTest {
        engine.gate = CompletableDeferred()
        val live = controller()
        assertEquals(LiveCookState.Connecting, live.state.value)
        live.stop()
        engine.gate!!.complete(Unit)
        runCurrent()
        assertEquals(LiveCookState.Off, live.state.value)
        assertEquals(2, engine.closes)
        assertTrue(engine.said.isEmpty())
    }

    @Test fun eventsFromAnEndedSessionAreIgnored() = runTest {
        val live = controller()
        var calls = 0
        live.onCall = { calls++; JSONObject().put("ok", true) }
        val old = engine.events
        live.stop()
        assertFalse(old.onCall(LiveCall("next_step", emptyMap())).getBoolean("ok"))
        old.onClosed(null)
        old.onTranscript("x", null)
        runCurrent()
        assertEquals(0, calls)
        assertEquals(LiveCookState.Off, live.state.value)
    }

    @Test fun sayIsIgnoredUnlessActive() = runTest {
        engine.gate = CompletableDeferred()
        val live = controller()
        live.say("đọc bước")
        assertTrue(engine.said.isEmpty())
        engine.gate!!.complete(Unit)
        runCurrent()
        live.say("đọc bước")
        assertEquals(listOf("chào", "đọc bước"), engine.said)
    }

    @Test fun aMissingSetupStopsInsteadOfConnecting() = runTest {
        val live = LiveCookController(engine, backgroundScope)
        live.start({ null })
        runCurrent()
        assertEquals(LiveCookState.Off, live.state.value)
        assertTrue(engine.opened.isEmpty())
    }
}
