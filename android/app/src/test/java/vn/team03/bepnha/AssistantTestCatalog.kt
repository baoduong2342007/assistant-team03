package vn.team03.bepnha

import org.json.JSONObject
import java.time.LocalDate

internal object AssistantTestCatalog {
    val day: LocalDate = LocalDate.of(2026, 11, 16)
    val engine: PlannerEngine by lazy {
        val catalog = RecipeRepository.parse(checkNotNull(javaClass.getResourceAsStream("/recipes.json")).bufferedReader().use { it.readText() })
        PlannerEngine(catalog.recipes.filterValues { it.kind != RecipeKind.BREAKFAST }, catalog.combinations)
    }
    fun state(): PlannerState = engine.ensureWeek(PlannerState(date = day, meal = Meal.LUNCH), day)
    fun context(): JSONObject = AssistantGateway.snapshot(engine, state(), day, Screen.TODAY, null)
    fun swap(index: Int = 1, id: String = "sour") = AssistantAction(AssistantActionType.swap_recipe, day.plusDays(1), Meal.DINNER, recipeId = id, index = index)
    fun swapJson(index: Int = 1, id: String = "sour") = JSONObject().put("type", "swap_recipe").put("date", day.plusDays(1).toString()).put("meal", "dinner").put("index", index).put("recipe_id", id)
}
