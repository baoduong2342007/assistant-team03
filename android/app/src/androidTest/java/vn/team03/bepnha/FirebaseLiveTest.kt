package vn.team03.bepnha

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Opt in explicitly: these tests consume the team's quota and require App Check registration. */
@RunWith(AndroidJUnit4::class)
class FirebaseLiveTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private fun enabled(flag: String = "firebaseLive") {
        assumeTrue(InstrumentationRegistry.getArguments().getString(flag) == "true")
        assertTrue("Supply team google-services.json before live verification", BuildConfig.FIREBASE_CONFIGURED)
    }

    @Test fun firebaseCanAnswerWithoutLocalFallback() = runBlocking {
        enabled()
        // Verify the project/model/App Check path with a tiny request before testing full app context.
        // This provider has no local fallback, so a successful reply must come from Firebase.
        val response = FirebaseAssistantProvider(app).generate(org.json.JSONObject().put("question", "Xin chào, trả lời chào mình bằng một câu ngắn, không dùng công cụ."))
        assertTrue(response.calls.isEmpty())
        assertFalse(response.text.isNullOrBlank())
    }

    @Test fun realPantryExclusionProducesGroundedSuggestions() = runBlocking {
        enabled()
        val engine = RecipeRepository(app).engine
        val today = PlannerEngine.today()
        val state = engine.ensureWeek(PlannerState(date = today), today)
        val response = AssistantService(engine, FirebaseAssistantProvider(app)).ask(
            "Tủ lạnh có thịt heo và rau muống. Tối mai gợi ý mâm, tránh trứng.", emptyList(),
            AssistantGateway.snapshot(engine, state, today, Screen.TODAY, null))
        assertEquals(response.notice, AssistantSource.AI, response.source)
        assertTrue(response.suggestions.isNotEmpty())
        response.suggestions.forEach {
            assertEquals(today.plusDays(1), it.date); assertEquals(Meal.DINNER, it.meal)
            assertFalse(it.recipeIds.flatMap { id -> engine.recipes.getValue(id).ingredients }.any { ingredient -> ingredient.name == "Trứng gà" })
            AssistantCommands(engine).proposal(state, AssistantCommands(engine).actionsFor(state, it, today), today, System.currentTimeMillis())
        }
    }

    @Test fun realPendingRevisionOnlyChangesSoup() = runBlocking {
        enabled()
        val engine = RecipeRepository(app).engine
        val today = PlannerEngine.today()
        val state = engine.ensureWeek(PlannerState(date = today), today)
        val pending = listOf("tofu", "soup", "greens").mapIndexed { i, id ->
            AssistantAction(AssistantActionType.swap_recipe, today.plusDays(1), Meal.DINNER, recipeId = id, index = i)
        }
        val response = AssistantService(engine, FirebaseAssistantProvider(app)).ask("À đổi món canh thành canh chua cá, giữ nguyên món mặn và món rau", emptyList(),
            AssistantGateway.snapshot(engine, state, today, Screen.TODAY, null, pendingMenuChanges = pending))
        assertEquals(response.notice, AssistantSource.AI, response.source)
        assertEquals(listOf(AssistantAction(AssistantActionType.swap_recipe, today.plusDays(1), Meal.DINNER, recipeId = "sour", index = 1)), response.actions)
    }

    @Test fun realNavigationOpensShopping() = runBlocking {
        enabled()
        val engine = RecipeRepository(app).engine
        val today = PlannerEngine.today()
        val state = engine.ensureWeek(PlannerState(date = today), today)
        val response = AssistantService(engine, FirebaseAssistantProvider(app)).ask("Mở danh sách đi chợ cho bữa này", emptyList(),
            AssistantGateway.snapshot(engine, state, today, Screen.TODAY, null))
        assertEquals(response.notice, AssistantSource.AI, response.source)
        assertEquals(AssistantActionType.navigate, response.actions.single().type)
        assertEquals(Screen.SHOPPING, response.actions.single().screen)
    }

    /**
     * Separates where a hang happens: token counting (no generation), then a bare one-line
     * generation without tools, thinking or system prompt. `-e diagModel <name>` tries another model.
     */
    @Test fun diagnoseLatency() = runBlocking {
        enabled("firebaseDiag")
        val name = InstrumentationRegistry.getArguments().getString("diagModel") ?: BuildConfig.ASSISTANT_MODEL
        val model = Firebase.ai(backend = com.google.firebase.ai.type.GenerativeBackend.googleAI(), useLimitedUseAppCheckTokens = true)
            .generativeModel(modelName = name, requestOptions = com.google.firebase.ai.type.RequestOptions(timeoutInMillis = 60_000))
        fun log(step: String, started: Long, outcome: String) =
            android.util.Log.i("BepNhaDiag", "$name $step ${System.currentTimeMillis() - started} ms: $outcome")
        var started = System.currentTimeMillis()
        runCatching { kotlinx.coroutines.withTimeout(60_000) { model.countTokens("Xin chào").totalTokens } }
            .onSuccess { log("countTokens", started, "ok $it tokens") }.onFailure { log("countTokens", started, "${it.javaClass.simpleName}: ${it.message?.take(300)}") }
        started = System.currentTimeMillis()
        val reply = runCatching { kotlinx.coroutines.withTimeout(60_000) { model.generateContent("Chào bạn, trả lời một câu ngắn.").text } }
            .onSuccess { log("generateContent", started, "ok ${it?.take(80)}") }.onFailure { log("generateContent", started, "${it.javaClass.simpleName}: ${it.message?.take(300)}") }
        assertTrue(reply.exceptionOrNull()?.toString() ?: "", reply.isSuccess)
    }

    /** "Nấu cùng tôi": connects to the Live model, hears its reply as a transcript and receives a function call. */
    @Test fun liveCookSessionTalksAndCallsCookingFunctions() = runBlocking {
        enabled()
        InstrumentationRegistry.getInstrumentation().uiAutomation
            .grantRuntimePermission(app.packageName, android.Manifest.permission.RECORD_AUDIO)
        val calls = java.util.concurrent.CopyOnWriteArrayList<String>()
        val spoken = StringBuffer()
        val engine = FirebaseLiveCookEngine(app)
        val session = engine.open(vn.team03.bepnha.voice.LiveCookSetup(
                "Bạn là trợ lý nấu ăn. Luôn nói tiếng Việt, thật ngắn. Khi được yêu cầu đọc lại bước, gọi hàm repeat_step rồi đọc trường step.",
                VoiceSettings.DEFAULT_LIVE_VOICE,
            ), object : vn.team03.bepnha.voice.LiveCookEvents {
                override fun onCall(call: vn.team03.bepnha.voice.LiveCall): org.json.JSONObject {
                    calls += call.name
                    return org.json.JSONObject().put("ok", true).put("step", "Bước 1 trên 3. Rửa rau muống.")
                }
                override fun onTranscript(user: String?, model: String?) { model?.let(spoken::append) }
                override fun onGoAway() = Unit
                override fun onClosed(error: Throwable?) = Unit
            })
        assertTrue(engine.active)
        try {
            session.say("${CookLivePrompt.APP} Người nấu muốn nghe lại bước hiện tại. Hãy gọi repeat_step rồi đọc kết quả.")
            val deadline = System.currentTimeMillis() + 30_000
            while (System.currentTimeMillis() < deadline && (calls.isEmpty() || spoken.isBlank())) kotlinx.coroutines.delay(250)
            android.util.Log.i("BepNhaLive", "calls=$calls spoken=$spoken")
            assertEquals(listOf("repeat_step"), calls.distinct())
            assertTrue("Gemini Live should answer aloud", spoken.isNotBlank())
        } finally {
            session.close()
        }
        // Nothing may keep running (or spinning) once the session has released its microphone and socket.
        val closedBy = System.currentTimeMillis() + 5_000
        while (engine.active && System.currentTimeMillis() < closedBy) kotlinx.coroutines.delay(100)
        assertFalse("Live session loops should stop after close", engine.active)
        val audio = Thread.getAllStackTraces().keys.filter { it.isAlive && it.name.startsWith("Firebase Audio") }
        assertTrue("SDK audio threads left running: $audio", audio.isEmpty())
    }

    @Test fun unregisteredDeviceCannotCallAI() = runBlocking {
        enabled("firebaseUnregistered")
        try {
            FirebaseAssistantProvider(app).generate(org.json.JSONObject().put("question", "Chào bạn"))
            fail("An unregistered test device must not access AI")
        } catch (e: AssistantFailure) { assertEquals(AssistantFailureKind.CONFIGURATION, e.kind) }
    }
}
