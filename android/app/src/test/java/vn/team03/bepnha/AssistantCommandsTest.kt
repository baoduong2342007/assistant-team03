package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class AssistantCommandsTest {
    private val day = LocalDate.of(2026, 11, 3)
    private fun recipe(id: String, kind: RecipeKind) = Recipe(id, id, kind, "", 10, 1000, "", listOf(Ingredient("Muối", 1.0, "g", "Gia vị")), listOf("Nấu"), "")
    private val engine = PlannerEngine(listOf(recipe("pork", RecipeKind.MAIN), recipe("tofu", RecipeKind.MAIN), recipe("soup", RecipeKind.SOUP), recipe("veg", RecipeKind.VEG)).associateBy { it.id }, listOf(listOf("pork", "soup", "veg")))
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
        commands.apply(state(), AssistantAction(AssistantActionType.cancel_timer, day.plusDays(7), Meal.DINNER), day, 0)
    }
    @Test fun timerRetainsTargetAndPreviewNamesExistingTimer() {
        val before = state().copy(timer = CookingTimer(60000, "Canh", day, Meal.DINNER))
        val action = AssistantAction(AssistantActionType.start_timer, day.plusDays(1), Meal.LUNCH, minutes = 15, label = "Đậu")
        assertTrue(commands.preview(before, action).contains("Canh"))
        val after = commands.apply(before, action, day, 1000)
        assertEquals(day.plusDays(1), after.timer?.date)
        assertEquals(901000L, after.timer?.end)
    }
}
