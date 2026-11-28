package vn.team03.bepnha.voice

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import vn.team03.bepnha.AssistantFailure
import vn.team03.bepnha.AssistantFailureKind

@OptIn(ExperimentalCoroutinesApi::class)
class LiveCookControllerTest {
    private class FakeSession(val setup: LiveCookSetup, val events: LiveCookEvents) : LiveCookSession {
        val said = mutableListOf<String>()
        var closes = 0
        override fun say(text: String) { said += text }
        override fun close() { closes++ }
    }

    private class FakeEngine : LiveCookEngine {
        var failWith: Throwable? = null
        /** Each open waits for the next gate, if any. */
        val gates = ArrayDeque<CompletableDeferred<Unit>>()
        /** Opens that keep going after being cancelled, like a network call that cannot be interrupted. */
        var ignoreCancellation = false
        val sessions = mutableListOf<FakeSession>()
        var cancelled = 0
        val events get() = sessions.last().events
        val said get() = sessions.flatMap { it.said }
        val opened get() = sessions.map { it.setup }

        override suspend fun open(setup: LiveCookSetup, events: LiveCookEvents): LiveCookSession {
            val gate = gates.removeFirstOrNull()
            try {
                if (gate != null) { if (ignoreCancellation) suspendCoroutine<Unit> { c -> gate.invokeOnCompletion { c.resume(Unit) } } else gate.await() }
            } catch (e: CancellationException) {
                cancelled++
                throw e
            }
            failWith?.let { throw it }
            return FakeSession(setup, events).also { sessions += it }
        }
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
        assertEquals(1, engine.sessions.size)
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
        assertEquals(1, engine.sessions.single().closes)
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
        step = 4
        engine.events.onGoAway()
        runCurrent()
        assertEquals(listOf("bước 1", "bước 4"), engine.opened.map { it.instruction })
        assertEquals(listOf(1, 0), engine.sessions.map { it.closes })
        assertEquals(listOf("chào"), engine.said)
        assertTrue(live.isOpen)
    }

    @Test fun aLateCloseOfTheOldConnectionDoesNotEndTheReconnectedSession() = runTest {
        val live = controller()
        val old = engine.events
        engine.gates += CompletableDeferred()
        old.onGoAway()
        runCurrent()
        assertEquals(LiveCookState.Connecting, live.state.value)
        old.onClosed(null) // The server closes the old socket while the new one is still connecting.
        runCurrent()
        assertEquals(LiveCookState.Connecting, live.state.value)
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

    @Test fun aDroppedConnectionFailsWithItsReasonAndClosesTheSession() = runTest {
        val live = controller()
        engine.events.onClosed(null)
        runCurrent()
        assertEquals(LiveCookState.Failed(AssistantFailureKind.NETWORK), live.state.value)
        assertEquals(1, engine.sessions.single().closes)

        live.start(::setup)
        runCurrent()
        engine.events.onClosed(AssistantFailure(AssistantFailureKind.QUOTA))
        runCurrent()
        assertEquals(LiveCookState.Failed(AssistantFailureKind.QUOTA), live.state.value)
    }

    @Test fun stoppingWhileConnectingCancelsTheOpen() = runTest {
        engine.gates += CompletableDeferred()
        val live = controller()
        assertEquals(LiveCookState.Connecting, live.state.value)
        live.stop()
        runCurrent()
        assertEquals(1, engine.cancelled)
        assertEquals(LiveCookState.Off, live.state.value)
        assertTrue(engine.sessions.isEmpty())
    }

    @Test fun aCancelledOpenThatFinishesLateIsClosedAndTheNewSessionStays() = runTest {
        engine.ignoreCancellation = true
        val first = CompletableDeferred<Unit>()
        val second = CompletableDeferred<Unit>()
        engine.gates += first
        engine.gates += second
        val live = controller()
        live.stop()
        live.start(::setup, "chào")
        runCurrent()
        second.complete(Unit) // The newer open finishes first...
        runCurrent()
        first.complete(Unit) // ...then the cancelled one.
        runCurrent()
        val (newer, older) = engine.sessions
        assertEquals(0, newer.closes)
        assertEquals(1, older.closes)
        assertTrue(live.state.value is LiveCookState.Active)
        live.stop()
        assertEquals(1, newer.closes)
    }

    @Test fun notesSentWhileConnectingAreSentOnceConnectedInsteadOfTheGreeting() = runTest {
        val gate = CompletableDeferred<Unit>()
        engine.gates += gate
        val live = controller()
        live.say("đọc bước 2")
        live.say("đã hẹn 5 phút")
        gate.complete(Unit)
        runCurrent()
        assertEquals(listOf("đọc bước 2\nđã hẹn 5 phút"), engine.said)
        live.say("đọc bước 3")
        assertEquals("đọc bước 3", engine.said.last())
    }

    @Test fun sayIsDroppedWhenNoSessionIsOpen() = runTest {
        val live = LiveCookController(engine, backgroundScope)
        live.say("đọc bước")
        live.start(::setup)
        runCurrent()
        live.stop()
        live.say("đọc bước")
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

    @Test fun aMissingSetupStopsInsteadOfConnecting() = runTest {
        val live = LiveCookController(engine, backgroundScope)
        live.start({ null })
        runCurrent()
        assertEquals(LiveCookState.Off, live.state.value)
        assertTrue(engine.sessions.isEmpty())
    }
}
