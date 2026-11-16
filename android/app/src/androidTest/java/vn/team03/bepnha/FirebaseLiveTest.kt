package vn.team03.bepnha

import androidx.test.ext.junit.runners.AndroidJUnit4
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

    @Test fun unregisteredDeviceCannotCallAI() = runBlocking {
        enabled("firebaseUnregistered")
        try {
            FirebaseAssistantProvider(app).generate(org.json.JSONObject().put("question", "Chào bạn"))
            fail("An unregistered test device must not access AI")
        } catch (e: AssistantFailure) { assertEquals(AssistantFailureKind.CONFIGURATION, e.kind) }
    }
}
