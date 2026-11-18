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
        // The family's diet profile always applies; a request may only tighten its limits.
        val diet = state.diet
        val maxMinutes = listOfNotNull(constraints.maxMinutes, diet.maxMinutes).minOrNull()
        val maxCost = listOfNotNull(constraints.maxCost, diet.maxCost).minOrNull()
        val byKind = RecipeKind.trayKinds.map { kind -> engine.recipes.values.filter { it.kind == kind }.sortedBy { it.id } }
        val allowed = byKind.map { group -> group.filter { recipe -> diet.allows(recipe) && recipe.ingredients.none { it.name in constraints.avoid } } }
        if (targets.any { it.second.tray }) RecipeKind.trayKinds.forEachIndexed { index, kind ->
            if (allowed[index].isEmpty()) throw NoMealFits(buildString {
                append("Không còn ${kind.label.lowercase()} nào phù hợp")
                if (!diet.isEmpty) append(" hồ sơ ăn uống (${diet.describe()})")
                if (constraints.avoid.isNotEmpty()) append(" khi tránh ${constraints.avoid.joinToString { it.lowercase(PlannerEngine.locale) }}")
                append(".")
            })
        }
        val trays = buildList {
            for (main in allowed[0]) for (soup in allowed[1]) for (veg in allowed[2]) {
                val menu = listOf(main, soup, veg)
                if (maxMinutes != null && engine.minutes(menu) > maxMinutes) continue
                if (maxCost != null && engine.cost(menu, people) > maxCost) continue
                add(menu)
            }
        }
        if (targets.any { it.second.tray } && trays.isEmpty()) throw NoMealFits(buildString {
            append("Không có mâm nào thỏa")
            maxMinutes?.let { append(" thời gian nấu tối đa $it phút") }
            maxCost?.let { append(" ngân sách ${PlannerEngine.money(it)} mỗi bữa") }
            if (!diet.isEmpty) append(" cùng hồ sơ ăn uống (${diet.describe()})")
            append(".")
        })
        val breakfasts = engine.breakfasts.map(engine.recipes::getValue)
            .filter { diet.allows(it) && it.ingredients.none { ingredient -> ingredient.name in constraints.avoid } }
            .map(::listOf)
            .filter { (maxMinutes == null || engine.minutes(it) <= maxMinutes) &&
                (maxCost == null || engine.cost(it, people) <= maxCost) }
        // No breakfast fits (e.g. a diet tag no breakfast dish carries yet): still plan lunch and
        // dinner and leave breakfasts as they are; fail only when breakfast was all that was asked.
        if (targets.any { !it.second.tray } && breakfasts.isEmpty() && targets.none { it.second.tray }) {
            throw NoMealFits("Không có món sáng nào đáp ứng các điều kiện của gia đình.")
        }
        val planned = if (breakfasts.isEmpty()) targets.filter { it.second.tray } else targets
        // Already-planned meals outside the targets still count for variety.
        val chosen = state.plans.filterKeys { key -> planned.none { PlannerEngine.key(it.first, it.second) == key } }
            .mapNotNull { (key, ids) -> runCatching { LocalDate.parse(key.substringBefore('|')) to ids }.getOrNull() }
            .toMutableList()
        val ordered = planned.distinct().sortedWith(compareBy({ it.first }, { it.second.ordinal }))
        return ordered.mapIndexed { position, (date, meal) ->
            fun usedNear(id: String, days: Long) = chosen.any { (day, ids) -> id in ids && kotlin.math.abs(day.toEpochDay() - date.toEpochDay()) <= days }
            val candidates = if (meal.tray) trays else breakfasts
            val best = candidates.minWith(compareBy<List<Recipe>>(
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
