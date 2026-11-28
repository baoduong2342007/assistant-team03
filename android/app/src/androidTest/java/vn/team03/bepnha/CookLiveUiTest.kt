package vn.team03.bepnha

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CompletableDeferred
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith
import vn.team03.bepnha.voice.LiveCall
import vn.team03.bepnha.voice.LiveCookEngine
import vn.team03.bepnha.voice.LiveCookEvents
import vn.team03.bepnha.voice.LiveCookSession
import vn.team03.bepnha.voice.LiveCookSetup
import vn.team03.bepnha.voice.LiveCookState
import vn.team03.bepnha.voice.VoiceState

/** "Nấu cùng tôi" with a fake Gemini Live engine: function calls, failures, notes and the transcript panel. */
@RunWith(AndroidJUnit4::class)
class CookLiveUiTest {
    private class FakeSession(val setup: LiveCookSetup, val events: LiveCookEvents) : LiveCookSession {
        val said = mutableListOf<String>()
        var closed = false
        override fun say(text: String) { said += text }
        override fun close() { closed = true }
    }

    private class FakeEngine : LiveCookEngine {
        var failWith: AssistantFailure? = null
        var gate: CompletableDeferred<Unit>? = null
        val sessions = mutableListOf<FakeSession>()
        override suspend fun open(setup: LiveCookSetup, events: LiveCookEvents): LiveCookSession {
            gate?.await()
            failWith?.let { throw it }
            return FakeSession(setup, events).also { sessions += it }
        }
    }

    private val compose = createAndroidComposeRule<MainActivity>()
    private val reset = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("bep_nha_planner_v2", 0).edit().clear().commit()
            context.getSharedPreferences(PlannerStorage.WELCOME_PREFS, 0).edit().putBoolean("seen", true).commit()
            context.getSharedPreferences("bep_nha_chat", 0).edit().clear().commit()
            ReminderStore.synchronize(context, "[]")
        }
    }
    @get:Rule val chain: TestRule = RuleChain.outerRule(reset).around(compose)
    private val engine = FakeEngine()
    private fun vm() = ViewModelProvider(compose.activity)[PlannerViewModel::class.java]

    /** Opens the cook-along and a (fake) Live session. */
    private fun startTalking() {
        compose.onNodeWithTag("tab-cook").performClick()
        compose.onNodeWithTag("start-cook-along").performClick()
        compose.runOnIdle {
            vm().cookLiveAllowed = true
            vm().liveCookFactory = { engine }
            vm().startCookListening()
        }
        compose.waitUntil(5_000) { compose.runOnIdle { vm().liveCookState is LiveCookState.Active } }
    }

    private fun call(name: String, vararg args: Pair<String, String>): JSONObject =
        compose.runOnIdle { engine.sessions.last().events.onCall(LiveCall(name, args.toMap())) }

    @Test fun functionCallsMoveStepsAndSetAndCancelTimers() {
        startTalking()
        assertEquals(listOf(CookLivePrompt.greeting()), engine.sessions.single().said)
        val first = compose.runOnIdle { vm().cookAlong!!.index }

        val next = call("next_step")
        assertTrue(next.getBoolean("ok"))
        assertTrue(next.getString("step").startsWith("Bước ${first + 2} trên"))
        compose.runOnIdle { assertEquals(first + 1, vm().cookAlong!!.index) }

        assertTrue(call("previous_step").getBoolean("ok"))
        compose.runOnIdle { assertEquals(first, vm().cookAlong!!.index) }
        assertTrue(call("repeat_step").getString("step").startsWith("Bước ${first + 1} trên"))

        val timer = call("set_timer", "minutes" to "5")
        assertTrue(timer.getBoolean("ok"))
        compose.runOnIdle { assertEquals(1, vm().state.timers.size) }
        assertTrue(call("cancel_timer").getBoolean("ok"))
        compose.runOnIdle { assertTrue(vm().state.timers.isEmpty()) }
        assertFalse(call("cancel_timer").getBoolean("ok"))
        assertFalse(call("set_timer", "minutes" to "0").getBoolean("ok"))
        assertFalse(call("swap_dish").getBoolean("ok"))
    }

    @Test fun aTimerThatCannotStartIsReportedAsNotOk() {
        startTalking()
        compose.runOnIdle { repeat(PlannerEngine.MAX_TIMERS) { vm().startTimer(10, "Đồng hồ $it") } }
        val result = call("set_timer", "minutes" to "5")
        assertFalse(result.getBoolean("ok"))
        assertTrue(result.getString("say").contains("Đang chạy ${PlannerEngine.MAX_TIMERS} đồng hồ"))
    }

    @Test fun goingBackAfterSayingFinishedKeepsCookingWithGemini() {
        startTalking()
        while (compose.runOnIdle { !vm().cookAlong!!.isLast }) call("next_step")
        assertTrue(call("next_step").getBoolean("done"))
        assertEquals("Bữa này đã nấu xong.", call("repeat_step").getString("error"))
        compose.onNodeWithTag("cook-back").performClick()
        assertTrue(call("repeat_step").getBoolean("ok"))
        // The finishing delay has passed; the cook-along must still be open.
        Thread.sleep(7_000)
        compose.runOnIdle { assertNotNull(vm().cookAlong) }
    }

    @Test fun aDroppedConnectionExplainsWhyAndRetriesOnTheNextTap() {
        startTalking()
        compose.runOnIdle { engine.sessions.single().events.onClosed(AssistantFailure(AssistantFailureKind.NETWORK)) }
        compose.waitUntil(5_000) { compose.runOnIdle { vm().liveCookState is LiveCookState.Failed } }
        compose.runOnIdle {
            val notice = vm().voiceNotice.orEmpty()
            assertTrue(notice, notice.contains("mất kết nối"))
            assertFalse(notice, notice.contains("gõ"))
            // No automatic listening: it would beep and fail again while offline.
            assertFalse(vm().voice.state.value is VoiceState.Listening)
            assertTrue(vm().cookUsesLive)
            vm().startCookListening()
        }
        compose.waitUntil(5_000) { compose.runOnIdle { vm().liveCookState is LiveCookState.Active } }
        assertEquals(2, engine.sessions.size)
        compose.onNodeWithTag("cook-voice-notice").assertDoesNotExist()
    }

    @Test fun aQuotaFailureSwitchesTheMicrophoneToShortCommands() {
        engine.failWith = AssistantFailure(AssistantFailureKind.QUOTA)
        compose.onNodeWithTag("tab-cook").performClick()
        compose.onNodeWithTag("start-cook-along").performClick()
        compose.runOnIdle {
            vm().cookLiveAllowed = true
            vm().liveCookFactory = { engine }
            vm().startCookListening()
        }
        compose.waitUntil(5_000) { compose.runOnIdle { vm().liveCookState is LiveCookState.Failed } }
        compose.runOnIdle {
            assertFalse(vm().cookUsesLive)
            assertTrue(vm().voiceNotice.orEmpty().contains("hết lượt"))
        }
        compose.onNodeWithTag("cook-voice-notice").assertIsDisplayed()
        compose.onNodeWithText("Bấm để nói lệnh", substring = true).assertExists()
    }

    @Test fun buttonsPressedWhileConnectingAreReadOnceConnected() {
        engine.gate = CompletableDeferred()
        compose.onNodeWithTag("tab-cook").performClick()
        compose.onNodeWithTag("start-cook-along").performClick()
        compose.runOnIdle {
            vm().cookLiveAllowed = true
            vm().liveCookFactory = { engine }
            vm().startCookListening()
        }
        compose.runOnIdle { assertEquals(LiveCookState.Connecting, vm().liveCookState) }
        compose.onNodeWithTag("cook-mic").assertContentDescriptionEquals("Huỷ kết nối")
        compose.onNodeWithTag("cook-next").performClick()
        val step = compose.runOnIdle { AssistantVoice.spoken(CookAlongPlan.spoken(vm().cookAlong!!)) }
        engine.gate!!.complete(Unit)
        compose.waitUntil(5_000) { compose.runOnIdle { vm().liveCookState is LiveCookState.Active } }
        assertEquals(listOf(CookLivePrompt.readStep(step)), engine.sessions.single().said)
    }

    @Test fun theLastExchangeStaysVisibleAboveTheButtons() {
        startTalking()
        val long = List(40) { "Khi thịt cá chuyển sang màu trắng đục và tách thớ là chín." }.joinToString(" ")
        compose.runOnIdle { engine.sessions.single().events.onTranscript("Cá chín chưa?", null) }
        compose.runOnIdle { engine.sessions.single().events.onTranscript(null, long) }
        compose.waitUntil(5_000) { compose.runOnIdle { (vm().liveCookState as? LiveCookState.Active)?.model?.isNotEmpty() == true } }
        compose.onNodeWithTag("cook-voice-panel").assertIsDisplayed()
        compose.onNodeWithTag("cook-next").assertIsDisplayed()
        compose.onNodeWithTag("cook-mic").assertIsDisplayed()
    }

    @Test fun exitingTheCookAlongClosesTheSession() {
        startTalking()
        compose.onNodeWithTag("cook-exit").performClick()
        compose.runOnIdle {
            assertTrue(engine.sessions.single().closed)
            assertEquals(LiveCookState.Off, vm().liveCookState)
        }
    }
}
