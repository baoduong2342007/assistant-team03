package vn.team03.bepnha

import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class DietProfileTest {
    private val engine = AssistantTestCatalog.engine
    private val day = AssistantTestCatalog.day
    private val commands = AssistantCommands(engine)
    private val strict = DietProfile(tags = setOf("low_salt"), allergens = setOf("shrimp", "egg"))

    private fun state(diet: DietProfile = strict) = AssistantTestCatalog.state().copy(diet = diet)
    private fun context(diet: DietProfile = strict) = AssistantGateway.snapshot(engine, state(diet), day, Screen.TODAY, null)
    private fun ask(question: String, vararg replies: AssistantModelReply, diet: DietProfile = strict): AssistantReply = runBlocking {
        var calls = 0
        AssistantService(engine, AssistantProvider.reply { replies[minOf(calls++, replies.lastIndex)] }, clock = { 0L })
            .ask(question, emptyList(), context(diet))
    }
    private fun call(name: String, args: JSONObject) = AssistantModelReply(calls = listOf(AssistantToolCall(name, args)))
    private fun swap(id: String, index: Int) = call("propose_actions", JSONObject().put("actions", JSONArray().put(
        JSONObject().put("type", "swap_recipe").put("date", day.plusDays(1).toString()).put("meal", "dinner").put("index", index).put("recipe_id", id))))

    @Test fun reasonsExplainWhyADishDoesNotFit() {
        val eggs = engine.recipes.values.first { "egg" in it.allergens && "low_salt" !in it.tags }
        val why = strict.reasons(eggs)
        assertTrue(why.contains("chưa phải món ít muối")); assertTrue(why.contains("có trứng"))
        assertTrue(engine.recipes.values.filter { strict.allows(it) }.all { "low_salt" in it.tags && it.allergens.none { a -> a in strict.allergens } })
        assertEquals("ít muối; tránh tôm, trứng", strict.describe())
    }

    @Test fun weeklyPlanFollowsTheProfileWithoutTheModelRepeatingIt() {
        val reply = ask("Lên thực đơn cả tuần", call("plan_meals", JSONObject().put("from", day.toString()).put("to", day.plusDays(6).toString())))
        var after = state()
        reply.actions.forEach { after = commands.apply(after, it, day, 0) }
        (0L..6L).forEach { offset -> engine.meals.forEach { meal ->
            engine.menu(after, day.plusDays(offset), meal).forEach { assertTrue(it.name, strict.allows(it)) }
        } }
    }

    @Test fun impossibleProfileIsExplained() {
        val everything = DietProfile(avoid = engine.recipes.values.flatMap { it.ingredients }.map { it.name }.toSet())
        val reply = ask("Lên thực đơn tuần này", call("plan_meals", JSONObject().put("from", day.toString()).put("to", day.toString())), diet = everything)
        assertTrue(reply.actions.isEmpty()); assertTrue(reply.text.contains("hồ sơ ăn uống"))
    }

    @Test fun fridgeSuggestionsSkipDishesOutsideTheProfile() {
        val reply = ask("Tủ lạnh có thịt heo, rau muống. Tối mai nấu gì?", call("suggest_meals", JSONObject()
            .put("date", day.plusDays(1).toString()).put("meal", "dinner").put("pantry_items", JSONArray(listOf("thịt heo", "rau muống")))))
        reply.suggestions.flatMap { it.recipeIds }.forEach { assertTrue(it, strict.allows(engine.recipes.getValue(it))) }
    }

    @Test fun modelPickOutsideTheProfileIsCorrectedButANamedDishIsAllowedWithANote() {
        val bad = engine.recipes.values.first { it.kind == RecipeKind.SOUP && !strict.allows(it) }
        val good = engine.recipes.values.first { it.kind == RecipeKind.SOUP && strict.allows(it) && it.id != engine.menu(state(), day.plusDays(1), Meal.DINNER)[1].id }
        val fixed = ask("Tối mai đổi món canh khác", swap(bad.id, 1), swap(good.id, 1))
        assertEquals(listOf(good.id), fixed.actions.map { it.recipeId })
        val named = ask("Tối mai đổi món canh thành ${bad.name}", swap(bad.id, 1))
        assertEquals(listOf(bad.id), named.actions.map { it.recipeId })
        assertTrue(named.text.contains("chưa hợp hồ sơ ăn uống"))
    }

    @Test fun searchHidesDishesOutsideTheProfileUnlessAskedByName() {
        val reads = AgentReadTools(engine, MealPlanning(engine.recipes))
        val all = reads.run("search_recipes", JSONObject().put("kind", "main"), state(), day).getJSONArray("recipes")
        (0 until all.length()).forEach { assertTrue(all.getJSONObject(it).getBoolean("fits_profile")) }
        val everything = reads.run("search_recipes", JSONObject().put("kind", "main").put("ignore_profile", true), state(), day).getJSONArray("recipes")
        assertTrue(everything.length() > all.length())
        val recipe = engine.recipes.values.first { !strict.allows(it) }
        val detail = reads.run("get_recipe", JSONObject().put("recipe_id", recipe.id), state(), day)
        assertFalse(detail.getBoolean("fits_profile")); assertTrue(detail.getJSONArray("profile_conflicts").length() > 0)
    }

    @Test fun profileActionsUpdateTagsAllergensIngredientsAndLimits() {
        var s = state(DietProfile())
        fun act(type: AssistantActionType, key: String? = null, enabled: Boolean? = null, minutes: Int? = null, cost: Int? = null) {
            s = commands.apply(s, AssistantAction(type, day, Meal.DINNER, key = key, enabled = enabled, minutes = minutes, cost = cost), day, 0)
        }
        act(AssistantActionType.set_diet_tag, "low_sugar", true)
        act(AssistantActionType.set_avoid, "tôm", true)
        act(AssistantActionType.set_avoid, "hành", true)
        act(AssistantActionType.set_meal_limits, minutes = 30, cost = 150_000)
        assertEquals(setOf("low_sugar"), s.diet.tags); assertEquals(setOf("shrimp"), s.diet.allergens)
        assertTrue(s.diet.avoid.isNotEmpty() && s.diet.avoid.all { it.startsWith("Hành") })
        assertEquals(30, s.diet.maxMinutes); assertEquals(150_000L, s.diet.maxCost)
        act(AssistantActionType.set_meal_limits, minutes = 0)
        assertNull(s.diet.maxMinutes); assertEquals(150_000L, s.diet.maxCost)
        act(AssistantActionType.set_avoid, "tôm", false)
        assertTrue(s.diet.allergens.isEmpty())
        assertThrows(IllegalArgumentException::class.java) { act(AssistantActionType.set_avoid, "kim cương", true) }
        assertThrows(IllegalArgumentException::class.java) { act(AssistantActionType.set_diet_tag, "keto", true) }
        assertTrue(AssistantAction(AssistantActionType.set_avoid, day, Meal.DINNER).risk == AssistantRisk.HIGH)
    }

    @Test fun profileRoundTripsThroughTheSnapshot() {
        val diet = strict.copy(avoid = setOf("Hành lá").filter { name -> engine.recipes.values.any { r -> r.ingredients.any { it.name == name } } }.toSet(), maxMinutes = 40)
        val rebuilt = AssistantGateway.stateFrom(engine, context(diet)).diet
        assertEquals(diet, rebuilt)
        val json = dietJson(diet).put("tags", JSONArray(listOf("low_salt", "keto"))).put("max_minutes", 5)
        val cleaned = dietFrom(json, engine)
        assertEquals(setOf("low_salt"), cleaned.tags); assertNull(cleaned.maxMinutes)
    }
}
