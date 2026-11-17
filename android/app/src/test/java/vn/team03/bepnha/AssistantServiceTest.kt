package vn.team03.bepnha

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class AssistantServiceTest {
    private val engine = AssistantTestCatalog.engine
    private val day = AssistantTestCatalog.day

    /** Answers model calls in order (the last reply repeats) and records the tool results sent back. */
    private class Script(vararg val replies: AssistantModelReply) {
        val sent = mutableListOf<List<AssistantToolResult>>()
        val provider = AssistantProvider.reply { results -> sent += results; replies[minOf(sent.size - 1, replies.lastIndex)] }
    }
    private fun call(name: String, args: JSONObject) = AssistantModelReply(calls = listOf(AssistantToolCall(name, args)))
    private fun proposal(vararg actions: JSONObject) = call("propose_actions", JSONObject().put("reply", "Đề xuất").put("actions", JSONArray(actions.toList())))
    private fun suggestions(vararg terms: String, only: Boolean = false, excluded: List<String> = emptyList()) = call("suggest_meals",
        JSONObject().put("date", day.plusDays(1).toString()).put("meal", "dinner").put("pantry_items", JSONArray(terms.toList()))
            .put("pantry_only", only).put("exclude_ingredients", JSONArray(excluded)))
    private fun plan(vararg extra: Pair<String, Any>) = call("plan_meals", JSONObject().put("from", day.toString()).put("to", day.plusDays(6).toString())
        .apply { extra.forEach { (key, value) -> put(key, value) } })
    private fun ask(script: Script, question: String, context: JSONObject = AssistantTestCatalog.context(), history: List<AssistantChatMessage> = emptyList()): AssistantReply = runBlocking {
        AssistantService(engine, script.provider, clock = { 0L }).ask(question, history, context)
    }
    private fun ask(response: AssistantModelReply, question: String, context: JSONObject = AssistantTestCatalog.context()) = ask(Script(response), question, context)
    private fun applied(reply: AssistantReply): PlannerState {
        val commands = AssistantCommands(engine)
        var state = AssistantTestCatalog.state()
        reply.actions.forEach { state = commands.apply(state, it, day, 0) }
        return state
    }

    @Test fun cloudSuggestionUsesLocalRankingAndDoesNotMutateState() {
        val before = AssistantTestCatalog.state()
        val reply = ask(suggestions("thịt heo", "rau muống"), "Tủ lạnh có thịt heo, rau muống. Tối mai nấu gì?")
        assertEquals(2, reply.suggestions.size)
        assertTrue(reply.actions.isEmpty())
        val commands = AssistantCommands(engine)
        reply.suggestions.forEach { commands.proposal(before, commands.actionsFor(before, it, day), day, 0) }
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
            val service = AssistantService(engine, AssistantProvider.reply { calls++; throw AssistantFailure(kind) })
            val reply = service.ask("Tối mai đổi món canh thành canh chua cá", emptyList(), AssistantTestCatalog.context())
            assertEquals(1, calls); assertEquals(AssistantSource.LOCAL, reply.source)
            assertEquals(listOf(AssistantTestCatalog.swap()), reply.actions); assertTrue(reply.suggestions.isEmpty())
            assertEquals(kind.explanation, reply.notice)
        }
    }

    @Test fun authAndInvalidResponsesDoNotBecomeSuggestions() = runBlocking {
        listOf(AssistantFailureKind.CONFIGURATION, AssistantFailureKind.INVALID, AssistantFailureKind.BLOCKED).forEach { kind ->
            val service = AssistantService(engine, AssistantProvider.reply { throw AssistantFailure(kind) })
            try { service.ask("Tủ lạnh có trứng, tối mai nấu gì?", emptyList(), AssistantTestCatalog.context()); fail() }
            catch (e: AssistantFailure) { assertEquals(kind, e.kind) }
        }
    }

    @Test fun cancelledRequestDoesNotFallBack() = runBlocking {
        try {
            AssistantService(engine, AssistantProvider.reply { throw CancellationException("Cancelled") })
                .ask("Tủ lạnh có trứng, tối mai nấu gì?", emptyList(), AssistantTestCatalog.context())
            fail()
        } catch (_: CancellationException) { }
    }

    @Test fun localClarificationPreservesContext() = runBlocking {
        val service = AssistantService(engine, null)
        val first = service.ask("Ngày mai mình muốn ăn canh chua cá", emptyList(), AssistantTestCatalog.context())
        assertEquals(MealPlanning.MEAL_QUESTION, first.text)
        val history = listOf(AssistantChatMessage("user", "Ngày mai mình muốn ăn canh chua cá"), AssistantChatMessage("assistant", first.text))
        val second = service.ask("Bữa tối nhé, giữ nguyên món mặn và rau", history, AssistantTestCatalog.context())
        assertEquals(listOf(AssistantTestCatalog.swap()), second.actions)
    }

    /** The old keyword gate rejected these; the agent now accepts any phrasing with a valid proposal. */
    @Test fun casualPhrasingReachesTheProposal() {
        listOf("tối mai nấu canh chua cá nhé", "toi mai cho minh an canh chua ca", "Mai tối làm canh chua cá được không").forEach { question ->
            assertEquals(question, listOf(AssistantTestCatalog.swap()), ask(proposal(AssistantTestCatalog.swapJson()), question).actions)
        }
    }

    @Test fun readToolResultsGoBackToTheModelBeforeItProposes() {
        val script = Script(call("get_recipe", JSONObject().put("recipe_id", "sour")), proposal(AssistantTestCatalog.swapJson()))
        val reply = ask(script, "Tối mai nấu canh chua cá, món đó cần gì?")
        assertEquals(listOf(AssistantTestCatalog.swap()), reply.actions)
        val recipe = script.sent[1].single()
        assertEquals("get_recipe", recipe.call.name)
        assertTrue(recipe.result.getJSONArray("steps").length() > 0)
    }

    @Test fun validationErrorsAreReturnedSoTheModelCanFixThem() {
        val script = Script(proposal(AssistantTestCatalog.swapJson(1, "tofu")), proposal(AssistantTestCatalog.swapJson()))
        val reply = ask(script, "Tối mai đổi món canh thành canh chua cá")
        assertEquals(listOf(AssistantTestCatalog.swap()), reply.actions)
        assertTrue(script.sent[1].single().result.getString("error").contains("món mặn"))
    }

    @Test fun repeatedInvalidProposalsStopAtTheStepBudget() {
        val script = Script(proposal(AssistantTestCatalog.swapJson(1, "invented")))
        val reply = ask(script, "Tối mai đổi món canh")
        assertTrue(reply.actions.isEmpty()); assertEquals(AssistantService.GAVE_UP, reply.text)
        assertEquals(AssistantService.MAX_MODEL_CALLS, script.sent.size)
    }

    @Test fun userWordsCorrectTheModelOnceThenItAsks() {
        val fixed = Script(proposal(AssistantTestCatalog.swapJson(0, "tofu")), proposal(AssistantTestCatalog.swapJson()))
        assertEquals(listOf(AssistantTestCatalog.swap()), ask(fixed, "toi mai doi mon canh thanh canh chua ca, giu man va rau").actions)
        assertTrue(fixed.sent[1].single().result.getString("error").contains("giữ nguyên"))
        listOf(
            AssistantTestCatalog.swapJson() to "Tối mai đừng thay canh thành canh chua cá",
            AssistantTestCatalog.swapJson(1, "cabbage") to "toi mai doi mon canh thanh canh chua ca",
            AssistantTestCatalog.swapJson() to "Tối mai đừng đổi thực đơn",
        ).forEach { (action, question) ->
            val reply = ask(proposal(action), question)
            assertTrue(question, reply.actions.isEmpty()); assertEquals(question, AssistantService.UNSURE, reply.text)
        }
    }

    @Test fun extraActionsPassAndNeedConfirmation() {
        val people = JSONObject().put("type", "set_people").put("people", 6)
        val reply = ask(proposal(AssistantTestCatalog.swapJson(), people), "Tối mai đổi canh thành canh chua cá, nhà có 6 người")
        assertEquals(2, reply.actions.size)
        assertEquals(6, reply.actions[1].people)
        assertTrue(AssistantCommands(engine).proposal(AssistantTestCatalog.state(), reply.actions, day, 0).needsConfirmation)
        val duplicate = ask(proposal(AssistantTestCatalog.swapJson(), AssistantTestCatalog.swapJson()), "Tối mai đổi món canh")
        assertTrue(duplicate.actions.isEmpty())
    }

    @Test fun malformedToolsCannotReachProposal() {
        fun none(response: AssistantModelReply) = assertTrue(ask(response, "Tối mai đổi canh").actions.isEmpty())
        none(call("delete_everything", JSONObject()))
        val swap = proposal(AssistantTestCatalog.swapJson()).calls.single()
        none(AssistantModelReply(calls = listOf(swap, swap)))
        none(proposal(AssistantTestCatalog.swapJson().put("index", "1")))
        none(proposal(AssistantTestCatalog.swapJson().put("date", "2026-12-20")))
        none(proposal(AssistantTestCatalog.swapJson().put("enabled", true)))
        listOf(AssistantModelReply(), AssistantModelReply(calls = List(AssistantService.MAX_CALLS_PER_REPLY + 1) { swap })).forEach { response ->
            try { ask(response, "Tối mai đổi canh"); fail() } catch (e: AssistantFailure) { assertEquals(AssistantFailureKind.INVALID, e.kind) }
        }
    }

    @Test fun planMealsFillsTheWeekWithValidVariedTraysAndAvoidsIngredients() {
        val reply = ask(plan("avoid_ingredients" to JSONArray(listOf("trứng"))), "Lên thực đơn cả tuần, nhà mình không ăn trứng")
        assertTrue(reply.actions.isNotEmpty() && reply.actions.all { it.type == AssistantActionType.swap_recipe })
        assertTrue(AssistantCommands(engine).proposal(AssistantTestCatalog.state(), reply.actions, day, 0).needsConfirmation)
        val after = applied(reply)
        val week = (0L..6L).flatMap { offset -> Meal.entries.map { engine.menu(after, day.plusDays(offset), it) } }
        assertEquals(14, week.size)
        week.flatten().forEach { recipe -> assertFalse(recipe.name, recipe.ingredients.any { it.name == "Trứng gà" }) }
        assertTrue(week.map { it[0].id }.distinct().size >= 4)
    }

    @Test fun planMealsKeepsUserExclusionsTheModelForgot() {
        val reply = ask(plan("meals" to JSONArray(listOf("dinner"))), "Lên thực đơn bữa tối cả tuần, tránh trứng")
        val after = applied(reply)
        (0L..6L).forEach { offset ->
            engine.menu(after, day.plusDays(offset), Meal.DINNER).forEach { recipe -> assertFalse(recipe.ingredients.any { it.name == "Trứng gà" }) }
        }
        assertEquals(AssistantTestCatalog.state().plans.filterKeys { it.endsWith("lunch") }, after.plans.filterKeys { it.endsWith("lunch") })
    }

    @Test fun planMealsExplainsWhenNothingFits() {
        val reply = ask(plan("max_minutes" to 1), "Lên thực đơn tuần này, mỗi bữa nấu dưới 1 phút")
        assertTrue(reply.actions.isEmpty()); assertTrue(reply.text.contains("Không có mâm nào"))
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

    @Test fun everyActionTypeIsSupportedAndGlobalActionsNeedNoMeal() {
        val state = AssistantTestCatalog.state()
        val meal = engine.menu(state); val key = engine.ingredients(meal, 4).first().key
        val task = engine.timeline(state).first().key
        val otherMain = engine.recipes.values.first { it.kind == RecipeKind.MAIN && it.id != meal[0].id }.id
        val fields: Map<String, Map<String, Any>> = mapOf(
            "navigate" to mapOf("screen" to "SHOPPING"), "open_recipe" to mapOf("recipe_id" to "tofu"),
            "set_people" to mapOf("people" to 6), "set_meal_time" to mapOf("time" to "19:00"),
            "set_reminder" to mapOf("enabled" to false), "cancel_timer" to emptyMap(),
            "set_ingredient" to mapOf("key" to key, "enabled" to true), "set_task" to mapOf("key" to task, "enabled" to true),
            "set_favorite" to mapOf("recipe_id" to "tofu", "enabled" to true),
            "start_timer" to mapOf("minutes" to 5, "label" to "Nấu canh"), "swap_recipe" to mapOf("recipe_id" to otherMain, "index" to 0),
            "select_meal" to emptyMap(), "open_sheet" to mapOf("option" to "reminder"), "close_assistant" to emptyMap(),
            "share_shopping_list" to mapOf("option" to "day"), "add_to_calendar" to emptyMap(),
            "request_permission" to mapOf("option" to "notifications"), "set_family_name" to mapOf("name" to "Nhà bà Tư"),
            "set_diet_tag" to mapOf("key" to "low_salt", "enabled" to true), "set_avoid" to mapOf("key" to "tôm", "enabled" to true),
            "set_meal_limits" to mapOf("minutes" to 30), "start_cook_along" to emptyMap(),
        )
        assertEquals(AssistantActionType.entries.map { it.name }.toSet(), fields.keys)
        fields.forEach { (type, extra) ->
            val action = JSONObject().put("type", type)
            if (type !in AssistantAction.GLOBAL.map { it.name }) action.put("date", state.date.toString()).put("meal", state.meal.value)
            extra.forEach { (name, value) -> action.put(name, value) }
            val reply = ask(proposal(action), "Giúp mình việc này")
            assertEquals(type, type, reply.actions.singleOrNull()?.type?.name)
            AssistantCommands(engine).proposal(state, reply.actions, day, 0)
        }
    }
}
