package vn.team03.bepnha

import java.time.LocalDate

/** Hard filters and soft preferences for planning several meals at once. */
data class PlanConstraints(
    /** Canonical ingredient names (see [MealPlanning.ingredientNames]) that must not appear. */
    val avoid: Set<String> = emptySet(),
    /** Canonical ingredient names to use where possible (e.g. what is in the fridge). */
    val prefer: Set<String> = emptySet(),
    val maxMinutes: Int? = null,
    /** VND per meal for the family's current number of people. */
    val maxCost: Long? = null,
)

data class PlannedMeal(val date: LocalDate, val meal: Meal, val recipeIds: List<String>)

/**
 * Deterministic multi-meal planner. The model only states constraints; the app chooses dishes,
 * so every result is a valid main/soup/veg tray from the reviewed catalog.
 */
class MealPlanner(private val engine: PlannerEngine) {
    class NoMealFits(message: String) : IllegalArgumentException(message)

    fun plan(state: PlannerState, targets: List<Pair<LocalDate, Meal>>, constraints: PlanConstraints): List<PlannedMeal> {
        require(targets.isNotEmpty()) { "Chưa có bữa nào để lên thực đơn." }
        val people = state.profile.people
        val byKind = RecipeKind.entries.map { kind -> engine.recipes.values.filter { it.kind == kind }.sortedBy { it.id } }
        val allowed = byKind.map { group -> group.filter { recipe -> recipe.ingredients.none { it.name in constraints.avoid } } }
        RecipeKind.entries.forEachIndexed { index, kind ->
            if (allowed[index].isEmpty()) throw NoMealFits("Không còn ${kind.label.lowercase()} nào sau khi tránh ${constraints.avoid.joinToString()}.")
        }
        val trays = buildList {
            for (main in allowed[0]) for (soup in allowed[1]) for (veg in allowed[2]) {
                val menu = listOf(main, soup, veg)
                if (constraints.maxMinutes != null && engine.minutes(menu) > constraints.maxMinutes) continue
                if (constraints.maxCost != null && engine.cost(menu, people) > constraints.maxCost) continue
                add(menu)
            }
        }
        if (trays.isEmpty()) throw NoMealFits(buildString {
            append("Không có mâm nào thỏa")
            constraints.maxMinutes?.let { append(" thời gian nấu tối đa $it phút") }
            constraints.maxCost?.let { append(" ngân sách ${PlannerEngine.money(it)} mỗi bữa") }
            append(".")
        })
        // Already-planned meals outside the targets still count for variety.
        val chosen = state.plans.filterKeys { key -> targets.none { PlannerEngine.key(it.first, it.second) == key } }
            .mapNotNull { (key, ids) -> runCatching { LocalDate.parse(key.substringBefore('|')) to ids }.getOrNull() }
            .toMutableList()
        val ordered = targets.distinct().sortedWith(compareBy({ it.first }, { it.second.ordinal }))
        return ordered.mapIndexed { position, (date, meal) ->
            fun usedNear(id: String, days: Long) = chosen.any { (day, ids) -> id in ids && kotlin.math.abs(day.toEpochDay() - date.toEpochDay()) <= days }
            val best = trays.minWith(compareBy<List<Recipe>>(
                // Same main within three days, or same soup/veg on the same or next day, feels repetitive.
                { menu -> (if (usedNear(menu[0].id, 2)) 4 else 0) + menu.drop(1).count { usedNear(it.id, 1) } },
                { menu -> -menu.flatMap { it.ingredients }.map { it.name }.distinct().count { it in constraints.prefer } },
                // Rotate ties by position so a week does not repeat one cheapest tray.
                { menu -> Math.floorMod(menu.joinToString("|") { it.id }.hashCode() + position * 7919, 1009) },
            ))
            val ids = best.map { it.id }
            chosen += date to ids
            PlannedMeal(date, meal, ids)
        }
    }
}
