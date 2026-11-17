package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AssistantCommandsTest {
    private val day = LocalDate.of(2026, 11, 3)
    private fun recipe(id: String, kind: RecipeKind) = Recipe(id, id, kind, "", 10, 1000, "", listOf(Ingredient("Muối", 1.0, "g", "Gia vị")), listOf("Nấu"), "")
    private val engine = PlannerEngine(
        listOf(recipe("pork", RecipeKind.MAIN), recipe("tofu", RecipeKind.MAIN), recipe("soup", RecipeKind.SOUP),
            recipe("sour", RecipeKind.SOUP), recipe("veg", RecipeKind.VEG)).associateBy { it.id },
        listOf(listOf("pork", "soup", "veg")),
    )
    private val commands = AssistantCommands(engine)
    private fun state() = engine.ensureWeek(PlannerState(date = day), day)

    @Test fun swapTargetsExplicitMealAndLeavesSelectionAlone() {
        val before = state()
        val action = AssistantAction(AssistantActionType.swap_recipe, day.plusDays(1), Meal.LUNCH, recipeId = "tofu", index = 0)
        val after = commands.apply(before, action, day, 0)
        assertEquals(before.date, after.date)
        assertEquals("pork", engine.menu(after).first().id)
        assertEquals("tofu", engine.menu(after, day.plusDays(1), Meal.LUNCH).first().id)
        assertEquals("pork", engine.menu(before, day.plusDays(1), Meal.LUNCH).first().id)
    }
    @Test fun markingTwiceIsIdempotent() {
        val action = AssistantAction(AssistantActionType.set_ingredient, day, Meal.DINNER, key = "Muối|g", enabled = true)
        val once = commands.apply(state(), action, day, 0)
        assertEquals(once, commands.apply(once, action, day, 0))
    }
    @Test(expected = IllegalArgumentException::class) fun wrongKindCannotReplaceMainDish() {
        commands.apply(state(), AssistantAction(AssistantActionType.swap_recipe, day, Meal.DINNER, recipeId = "veg", index = 0), day, 0)
    }
    @Test(expected = IllegalArgumentException::class) fun cannotTouchMealsOutsideWindow() {
        commands.apply(state(), AssistantAction(AssistantActionType.swap_recipe, day.plusDays(7), Meal.DINNER, recipeId = "tofu", index = 0), day, 0)
    }

    @Test fun globalActionsIgnoreTheMealWindow() {
        val timer = state().copy(timer = CookingTimer(60000, "Canh", day, Meal.DINNER))
        assertNull(commands.apply(timer, AssistantAction(AssistantActionType.cancel_timer, day.plusDays(30), Meal.DINNER), day, 0).timer)
        assertEquals("Nhà bà Tư", commands.apply(state(), AssistantAction(AssistantActionType.set_family_name, day, Meal.DINNER, name = " Nhà bà Tư "), day, 0).profile.name)
    }

    @Test fun riskDecidesWhatNeedsConfirmation() {
        val favorite = AssistantAction(AssistantActionType.set_favorite, day, Meal.DINNER, recipeId = "tofu", enabled = true)
        val swap = AssistantAction(AssistantActionType.swap_recipe, day, Meal.DINNER, recipeId = "tofu", index = 0)
        val open = AssistantAction(AssistantActionType.navigate, day, Meal.DINNER, screen = Screen.SHOPPING)
        val timer = AssistantAction(AssistantActionType.start_timer, day, Meal.DINNER, minutes = 5, label = "Canh")
        assertEquals(AssistantRisk.READ, open.risk); assertEquals(AssistantRisk.LOW, favorite.risk); assertEquals(AssistantRisk.HIGH, swap.risk)
        assertFalse(commands.proposal(state(), listOf(favorite, open), day, 0).needsConfirmation)
        assertTrue(commands.proposal(state(), listOf(favorite, swap), day, 0).needsConfirmation)
        // Replacing a running timer cannot be undone safely.
        assertFalse(commands.proposal(state(), listOf(timer), day, 0).needsConfirmation)
        val running = state().copy(timer = CookingTimer(60_000, "Cơm", day, Meal.DINNER))
        assertTrue(commands.proposal(running, listOf(timer), day, 0).needsConfirmation)
    }

    @Test fun newReadActionsValidateTheirOptions() {
        fun fails(action: AssistantAction) = assertThrows(IllegalArgumentException::class.java) { commands.apply(state(), action, day, 0) }
        fails(AssistantAction(AssistantActionType.open_sheet, day, Meal.DINNER, option = "settings"))
        fails(AssistantAction(AssistantActionType.open_sheet, day, Meal.DINNER, option = "swap_options"))
        fails(AssistantAction(AssistantActionType.request_permission, day, Meal.DINNER, option = "camera"))
        fails(AssistantAction(AssistantActionType.share_shopping_list, day, Meal.DINNER, option = "week"))
        fails(AssistantAction(AssistantActionType.set_family_name, day, Meal.DINNER, name = " "))
        assertEquals(state(), commands.apply(state(), AssistantAction(AssistantActionType.open_sheet, day, Meal.DINNER, option = "swap_options", index = 1), day, 0))
    }

    @Test fun missingTargetDefaultsToSelectedMeal() {
        val action = AssistantGateway.parseAction(org.json.JSONObject().put("type", "start_timer").put("minutes", 5.0).put("label", "Luộc rau"), day, Meal.LUNCH)
        assertEquals(day, action.date); assertEquals(Meal.LUNCH, action.meal); assertEquals(5, action.minutes)
        val share = AssistantGateway.parseAction(org.json.JSONObject().put("type", "share_shopping_list"), day, Meal.LUNCH)
        assertEquals("meal", share.option)
    }

    @Test fun snapshotRoundTripsTheDataProposalsAreCheckedAgainst() {
        val base = state().copy(favorites = setOf("tofu"), reminders = setOf(PlannerEngine.key(day, Meal.DINNER)),
            checked = mapOf(PlannerEngine.key(day, Meal.DINNER) to setOf("Muối|g")))
        val rebuilt = AssistantGateway.stateFrom(engine, AssistantGateway.snapshot(engine, base, day, Screen.TODAY, null))
        assertEquals(base.plans.filterKeys { it.substringBefore('|') <= day.plusDays(6).toString() }, rebuilt.plans)
        assertEquals(base.favorites, rebuilt.favorites); assertEquals(base.reminders, rebuilt.reminders)
        assertEquals(base.checked, rebuilt.checked.filterValues { it.isNotEmpty() })
    }

    @Test fun editingOneDishKeepsOtherSuggestedChangesAndRemovesNoOp() {
        val base = state()
        val target = day.plusDays(2)
        val meal = AssistantMealSuggestion(1, target, Meal.LUNCH, listOf("tofu", "sour", "veg"), setOf("Muối|g"))
        val draft = commands.proposal(base, commands.actionsFor(base, meal, day), day, 0)
        assertTrue(draft.canReviseMenu)
        assertEquals(listOf("tofu", "sour"), draft.actions.map { it.recipeId })
        val restoreSoup = AssistantAction(AssistantActionType.swap_recipe, target, Meal.LUNCH, recipeId = "soup", index = 1)
        val updated = commands.proposal(base, commands.reviseMenu(draft, listOf(restoreSoup)), day, 0)
        assertEquals(listOf("tofu"), updated.actions.map { it.recipeId })
        assertEquals(base, draft.base)
        assertEquals(base, updated.base)
    }

    @Test(expected = IllegalArgumentException::class) fun batchCannotReplaceOneSlotTwice() {
        val first = AssistantAction(AssistantActionType.swap_recipe, day, Meal.DINNER, recipeId = "sour", index = 1)
        commands.proposal(state(), listOf(first, first.copy(recipeId = "soup")), day, 0)
    }

    @Test(expected = IllegalArgumentException::class) fun suggestionCannotIncludeUnknownPantryKey() {
        val suggestion = AssistantMealSuggestion(1, day, Meal.DINNER, listOf("tofu", "soup", "veg"), setOf("Xoài|quả"))
        commands.actionsFor(state(), suggestion, day)
    }

    @Test fun timerRetainsTargetAndPreviewNamesExistingTimer() {
        val before = state().copy(timer = CookingTimer(60000, "Canh", day, Meal.DINNER))
        val action = AssistantAction(AssistantActionType.start_timer, day.plusDays(1), Meal.LUNCH, minutes = 15, label = "Đậu")
        assertTrue(commands.preview(before, action).contains("Canh"))
        val after = commands.apply(before, action, day, 1000)
        assertEquals(day.plusDays(1), after.timer?.date)
        assertEquals(901000L, after.timer?.end)
    }

    @Test fun quotaMessageDistinguishesDailyWaitFromShortRateLimit() {
        assertTrue(AssistantGateway.failureMessage(429, 47007L).contains("14 giờ"))
        assertTrue(AssistantGateway.failureMessage(429, 40L).contains("1 phút"))
        assertFalse(AssistantGateway.failureMessage(429).contains("một lát"))
        assertTrue(AssistantGateway.failureMessage(504).contains("quá chậm"))
    }
}
