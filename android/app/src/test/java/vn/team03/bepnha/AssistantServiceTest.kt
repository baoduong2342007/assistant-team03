package vn.team03.bepnha

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AssistantServiceTest {
    private val engine = AssistantTestCatalog.engine
    private fun proposal(vararg actions: JSONObject) = AssistantModelReply(calls = listOf(AssistantToolCall("propose_actions",
        JSONObject().put("reply", "Đề xuất").put("actions", JSONArray(actions.toList())))))
    private fun suggestions(vararg terms: String, only: Boolean = false, excluded: List<String> = emptyList()) = AssistantModelReply(calls = listOf(AssistantToolCall("suggest_meals",
        JSONObject().put("date", AssistantTestCatalog.day.plusDays(1).toString()).put("meal", "dinner").put("pantry_items", JSONArray(terms.toList()))
            .put("pantry_only", only).put("exclude_ingredients", JSONArray(excluded)))))
    private fun ask(response: AssistantModelReply, question: String, context: JSONObject = AssistantTestCatalog.context(), history: List<AssistantChatMessage> = emptyList()): AssistantReply = runBlocking {
        AssistantService(engine, AssistantProvider { response }).ask(question, history, context)
    }
    private fun invalid(response: AssistantModelReply, question: String, context: JSONObject = AssistantTestCatalog.context()) {
        try { ask(response, question, context); fail("Expected invalid response") }
        catch (e: AssistantFailure) { assertEquals(AssistantFailureKind.INVALID, e.kind) }
    }

    @Test fun cloudSuggestionUsesLocalRankingAndDoesNotMutateState() {
        val before = AssistantTestCatalog.state()
        val reply = ask(suggestions("thịt heo", "rau muống"), "Tủ lạnh có thịt heo, rau muống. Tối mai nấu gì?")
        assertEquals(2, reply.suggestions.size)
        assertTrue(reply.actions.isEmpty())
        val commands = AssistantCommands(engine)
        reply.suggestions.forEach { commands.proposal(before, commands.actionsFor(before, it, AssistantTestCatalog.day), AssistantTestCatalog.day, 0) }
        assertEquals(AssistantTestCatalog.state(), before)
    }

    @Test fun noProviderSupportsLocalPantryAndExactReplacement() = runBlocking {
        val service = AssistantService(engine, null)
        val reply = service.ask("Tủ lạnh có thịt heo, rau muống. Tối mai nấu gì?", emptyList(), AssistantTestCatalog.context())
        assertEquals(AssistantSource.LOCAL, reply.source); assertNotNull(reply.notice); assertEquals(2, reply.suggestions.size)
        val swap = service.ask("Tối mai đổi món canh thành canh chua cá", emptyList(), AssistantTestCatalog.context())
        assertEquals(listOf(AssistantTestCatalog.swap()), swap.actions)
    }

    @Test fun transientFailuresFallBackOnceWithCorrectIntent() = runBlocking {
        listOf(AssistantFailureKind.NETWORK, AssistantFailureKind.TIMEOUT, AssistantFailureKind.QUOTA, AssistantFailureKind.UNAVAILABLE).forEach { kind ->
            var calls = 0
            val service = AssistantService(engine, AssistantProvider { calls++; throw AssistantFailure(kind) })
            val reply = service.ask("Tối mai đổi món canh thành canh chua cá", emptyList(), AssistantTestCatalog.context())
            assertEquals(1, calls); assertEquals(AssistantSource.LOCAL, reply.source)
            assertEquals(listOf(AssistantTestCatalog.swap()), reply.actions); assertTrue(reply.suggestions.isEmpty())
            assertEquals(kind.explanation, reply.notice)
        }
    }

    @Test fun authAndInvalidResponsesDoNotBecomeSuggestions() = runBlocking {
        listOf(AssistantFailureKind.CONFIGURATION, AssistantFailureKind.INVALID, AssistantFailureKind.BLOCKED).forEach { kind ->
            val service = AssistantService(engine, AssistantProvider { throw AssistantFailure(kind) })
            try { service.ask("Tủ lạnh có trứng, tối mai nấu gì?", emptyList(), AssistantTestCatalog.context()); fail() }
            catch (e: AssistantFailure) { assertEquals(kind, e.kind) }
        }
    }

    @Test fun cancelledRequestDoesNotFallBack() = runBlocking {
        try {
            AssistantService(engine, AssistantProvider { throw CancellationException("Cancelled") })
                .ask("Tủ lạnh có trứng, tối mai nấu gì?", emptyList(), AssistantTestCatalog.context())
            fail()
        } catch (_: CancellationException) { }
    }

    @Test fun clarificationRunsBeforeProviderAndPreservesContext() = runBlocking {
        val service = AssistantService(engine, AssistantProvider { fail("Should clarify without a network call"); AssistantModelReply() })
        val first = service.ask("Ngày mai mình muốn ăn canh chua cá", emptyList(), AssistantTestCatalog.context())
        assertEquals(MealPlanning.MEAL_QUESTION, first.text)
        val history = listOf(AssistantChatMessage("user", "Ngày mai mình muốn ăn canh chua cá"), AssistantChatMessage("assistant", first.text))
        val second = AssistantService(engine, null).ask("Bữa tối nhé, giữ nguyên món mặn và rau", history, AssistantTestCatalog.context())
        assertEquals(listOf(AssistantTestCatalog.swap()), second.actions)
    }

    @Test fun negatedAndWrongSlotAndWrongRecipeChangesAreRejected() {
        invalid(proposal(AssistantTestCatalog.swapJson()), "Tối mai đừng thay canh thành canh chua cá")
        invalid(proposal(AssistantTestCatalog.swapJson(0, "tofu")), "toi mai doi mon canh thanh canh chua ca, giu man va rau")
        invalid(proposal(AssistantTestCatalog.swapJson(1, "cabbage")), "toi mai doi mon canh thanh canh chua ca")
        invalid(proposal(AssistantTestCatalog.swapJson(1, "tofu")), "Tối mai đổi món canh")
        invalid(proposal(AssistantTestCatalog.swapJson(1, "invented")), "Tối mai đổi món canh")
    }

    @Test fun unrelatedMutationsAndDuplicateSlotsAreRejected() {
        val people = JSONObject().put("type", "set_people").put("date", AssistantTestCatalog.day.plusDays(1).toString()).put("meal", "dinner").put("people", 6)
        invalid(proposal(AssistantTestCatalog.swapJson(), people), "Tối mai đổi món canh thành canh chua cá")
        val reply = ask(proposal(AssistantTestCatalog.swapJson(), people), "Tối mai đổi món canh thành canh chua cá cho 6 người")
        assertEquals(2, reply.actions.size)
        invalid(proposal(AssistantTestCatalog.swapJson(), AssistantTestCatalog.swapJson()), "Tối mai đổi món canh")
        invalid(proposal(people), "Tối mai món nào nấu nhanh?")
        invalid(proposal(AssistantTestCatalog.swapJson()), "Tối mai món nào nấu nhanh?")
        invalid(proposal(AssistantTestCatalog.swapJson()), "Tối mai đừng đổi thực đơn")
    }

    @Test fun malformedToolsCannotReachProposal() {
        invalid(AssistantModelReply(calls = listOf(AssistantToolCall("delete_everything", JSONObject()))), "Tối mai đổi canh")
        val call = proposal(AssistantTestCatalog.swapJson()).calls.single()
        invalid(AssistantModelReply(calls = listOf(call, call)), "Tối mai đổi canh")
        invalid(AssistantModelReply(), "Tối mai đổi canh")
        invalid(proposal(AssistantTestCatalog.swapJson().put("index", "1")), "Tối mai đổi canh")
        invalid(proposal(AssistantTestCatalog.swapJson().put("date", "2026-12-20")), "Tối mai đổi canh")
        invalid(proposal(AssistantTestCatalog.swapJson().put("enabled", true)), "Tối mai đổi canh")
    }

    @Test fun userExclusionSurvivesModelOmissionAndUnavailableDoesNotMeanExcluded() {
        val reply = ask(suggestions("thịt heo", "rau muống"), "Có thịt heo và rau muống. Tối mai gợi ý mâm, tránh trứng.")
        assertTrue(reply.suggestions.isNotEmpty())
        reply.suggestions.flatMap { it.recipeIds }.forEach { assertFalse(engine.recipes.getValue(it).ingredients.any { ingredient -> ingredient.name == "Trứng gà" }) }
        val constraints = MealPlanning(engine.recipes).constraints(listOf("Hết trứng, chỉ còn rau muống"))
        assertTrue(constraints.excluded.isEmpty()); assertFalse(constraints.only)
    }

    @Test fun strictPantryCannotInventEnoughIngredients() {
        val reply = ask(suggestions("rau muống"), "Tối mai chỉ dùng rau muống, không mua thêm")
        assertTrue(reply.suggestions.isEmpty()); assertTrue(reply.text.contains("cần thêm"))
        val invented = ask(suggestions("thịt heo"), "Tủ lạnh chỉ còn rau muống, tối mai nấu gì?")
        assertTrue(invented.suggestions.isEmpty())
    }

    @Test fun batchKeepsConditionsAfterComparisonTurnsAndNewInventoryReplacesThem() = runBlocking {
        val context = AssistantTestCatalog.context().put("suggestion_constraints", JSONObject()
            .put("pantry", JSONArray(listOf("Rau muống", "Trứng gà"))).put("excluded", JSONArray(listOf("Cá basa"))).put("only", false))
        val service = AssistantService(engine, null)
        val reply = service.ask("Tối mai gợi ý mâm khác", emptyList(), context)
        assertEquals(2, reply.suggestions.size)
        assertEquals(setOf("Cá basa"), reply.suggestionConstraints?.excluded)
        assertEquals(setOf("Rau muống", "Trứng gà"), reply.suggestionConstraints?.pantry)
        val updated = service.ask("Tối mai gợi ý mâm, chỉ có thịt gà", emptyList(), context)
        assertEquals(setOf("Thịt gà"), updated.suggestionConstraints?.pantry)
    }

    @Test fun localLookupKeepsRecipeLinksAndAmbiguousEditsAsk() = runBlocking {
        val service = AssistantService(engine, null)
        assertTrue(service.ask("Món nấu nhanh", emptyList(), AssistantTestCatalog.context()).recipeIds.isNotEmpty())
        val reply = service.ask("Tối mai đổi món canh", emptyList(), AssistantTestCatalog.context())
        assertTrue(reply.actions.isEmpty()); assertTrue(reply.suggestions.isEmpty()); assertTrue(reply.text.contains("tên đầy đủ"))
    }

    @Test fun allElevenActionTypesRemainSupported() {
        val context = AssistantTestCatalog.context(); val state = AssistantTestCatalog.state()
        val meal = engine.menu(state); val key = engine.ingredients(meal, 4).first().key
        val task = engine.timeline(state).first().key
        val additions = mapOf(
            "navigate" to ("screen" to "SHOPPING"), "open_recipe" to ("recipe_id" to "tofu"),
            "set_people" to ("people" to 6), "set_meal_time" to ("time" to "19:00"),
            "set_reminder" to ("enabled" to false), "cancel_timer" to ("type" to "cancel_timer"),
            "set_ingredient" to ("key" to key), "set_task" to ("key" to task), "set_favorite" to ("recipe_id" to "tofu"),
            "start_timer" to ("minutes" to 5), "swap_recipe" to ("recipe_id" to "tofu"),
        )
        additions.forEach { (type, field) ->
            val action = JSONObject().put("type", type).put("date", state.date.toString()).put("meal", state.meal.value).put(field.first, field.second)
            if (type in listOf("set_ingredient", "set_task", "set_favorite")) action.put("enabled", true)
            if (type == "start_timer") action.put("label", "Nấu canh")
            if (type == "swap_recipe") action.put("index", 0)
            val request = mapOf(
                "navigate" to "Mở đi chợ", "open_recipe" to "Xem công thức đậu hũ",
                "set_people" to "Đổi khẩu phần thành 6 người", "set_meal_time" to "Đổi giờ ăn thành 19:00",
                "set_reminder" to "Tắt nhắc nấu", "cancel_timer" to "Hủy đồng hồ",
                "set_ingredient" to "Đánh dấu nguyên liệu đã mua", "set_task" to "Đánh dấu bước nấu đã xong",
                "set_favorite" to "Lưu món đậu hũ", "start_timer" to "Hẹn giờ nấu canh 5 phút", "swap_recipe" to "Đổi món mặn",
            ).getValue(type)
            val reply = ask(proposal(action), request, context)
            assertEquals(type, reply.actions.single().type.name)
            AssistantCommands(engine).proposal(state, reply.actions, AssistantTestCatalog.day, 0)
        }
    }
}
