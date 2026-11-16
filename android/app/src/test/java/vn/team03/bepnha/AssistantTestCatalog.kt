package vn.team03.bepnha

import org.json.JSONObject
import java.time.LocalDate

internal object AssistantTestCatalog {
    val day: LocalDate = LocalDate.of(2026, 11, 16)
    val engine: PlannerEngine by lazy {
        val root = JSONObject(checkNotNull(javaClass.getResourceAsStream("/recipes.json")).bufferedReader().use { it.readText() })
        val items = root.getJSONArray("recipes")
        val recipes = (0 until items.length()).map { i ->
            val item = items.getJSONObject(i); val ingredients = item.getJSONArray("ingredients")
            Recipe(item.getString("id"), item.getString("name"), when(item.getString("kind")) {
                "main" -> RecipeKind.MAIN; "soup" -> RecipeKind.SOUP; else -> RecipeKind.VEG
            }, item.getString("emoji"), item.getInt("minutes"), item.getLong("cost"), item.getString("note"),
                (0 until ingredients.length()).map { j -> ingredients.getJSONObject(j).let { Ingredient(it.getString("name"), it.getDouble("amount"), it.getString("unit"), it.getString("group")) } },
                item.getJSONArray("steps").let { steps -> (0 until steps.length()).map(steps::getString) }, item.getString("tip"))
        }.associateBy { it.id }
        val choices = root.getJSONArray("combinations")
        PlannerEngine(recipes, (0 until choices.length()).map { i -> val ids = choices.getJSONArray(i); (0 until ids.length()).map(ids::getString) })
    }
    fun state(): PlannerState = engine.ensureWeek(PlannerState(date = day, meal = Meal.LUNCH), day)
    fun context(): JSONObject = AssistantGateway.snapshot(engine, state(), day, Screen.TODAY, null)
    fun swap(index: Int = 1, id: String = "sour") = AssistantAction(AssistantActionType.swap_recipe, day.plusDays(1), Meal.DINNER, recipeId = id, index = index)
    fun swapJson(index: Int = 1, id: String = "sour") = JSONObject().put("type", "swap_recipe").put("date", day.plusDays(1).toString()).put("meal", "dinner").put("index", index).put("recipe_id", id)
}
