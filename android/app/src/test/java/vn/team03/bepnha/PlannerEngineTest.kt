package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class PlannerEngineTest {
    private val day = LocalDate.of(2026, 10, 30)
    private val now = Instant.parse("2026-10-30T06:00:00Z").toEpochMilli()
    private fun recipe(id: String, kind: RecipeKind, minutes: Int, cost: Long) = Recipe(id, id, kind, "", minutes, cost, "",
        listOf(Ingredient("Nước mắm", 1.0, "muỗng cà phê", "Gia vị")), listOf("Nấu chín"), "")
    private val records = listOf(recipe("pork", RecipeKind.MAIN, 45, 65000), recipe("eggs", RecipeKind.MAIN, 10, 22000),
        recipe("tofu", RecipeKind.MAIN, 20, 25000), recipe("soup", RecipeKind.SOUP, 15, 25000),
        recipe("sour", RecipeKind.SOUP, 25, 45000), recipe("greens", RecipeKind.VEG, 10, 15000), recipe("broccoli", RecipeKind.VEG, 15, 22000))
    private val engine = PlannerEngine(records.associateBy { it.id }, listOf(listOf("pork", "soup", "greens"), listOf("eggs", "sour", "broccoli")))
    private fun state() = engine.ensureWeek(PlannerState(date = day), day)

    @Test fun nativeWeekRetainsSavedChoicesWhenTheDateChanges() {
        val initial = state()
        val futureKey = PlannerEngine.key(day.plusDays(1), Meal.DINNER)
        val changed = initial.copy(plans = initial.plans + (futureKey to listOf("tofu", "soup", "greens")))
        val tomorrow = engine.ensureWeek(changed, day.plusDays(1))
        assertEquals(day.plusDays(1), tomorrow.date)
        assertEquals(listOf("tofu", "soup", "greens"), tomorrow.plans[futureKey])
        assertTrue(tomorrow.plans.containsKey(PlannerEngine.key(day.plusDays(7), Meal.DINNER)))
    }

    @Test fun quantitiesAndCostsScaleAndSharedIngredientsAreMerged() {
        val menu = engine.menu(state())
        val ingredients = engine.ingredients(menu, 2)
        assertEquals(1, ingredients.count { it.name == "Nước mắm" })
        assertEquals(1.5, ingredients.first { it.name == "Nước mắm" }.amount, 0.0)
        assertEquals(180.0, ingredients.first { it.name == "Gạo" }.amount, 0.0)
        assertEquals(52500L, engine.cost(menu, 2))
    }

    @Test fun nativeTimelineUsesVietnamTimeAndAtMostTwoBurners() {
        for (main in records.filter { it.kind == RecipeKind.MAIN }) for (soup in records.filter { it.kind == RecipeKind.SOUP }) for (veg in records.filter { it.kind == RecipeKind.VEG }) {
            val initial = state()
            val s = initial.copy(plans = initial.plans + (initial.key to listOf(main.id, soup.id, veg.id)))
            val tasks = engine.timeline(s)
            assertEquals(Instant.parse("2026-10-30T11:30:00Z").toEpochMilli(), tasks.last().at)
            val cooking = tasks.filter { it.recipeId != null }
            for (task in cooking) {
                assertTrue(task.at >= tasks.first().at + 15 * 60_000L)
                assertTrue(task.at + task.minutes * 60_000L <= tasks.last().at)
            }
            for (at in tasks.first().at until tasks.last().at step 60_000L) {
                assertTrue(cooking.count { at >= it.at && at < it.at + it.minutes * 60_000L } <= 2)
            }
        }
    }

    @Test fun swappingARecipeRebuildsTimelineAndClearsOnlyThatMealsProgress() {
        val initial = state().let { it.copy(done = mapOf(it.key to setOf("main"), "other" to setOf("prep"))) }
        val changed = engine.swap(initial, 0, "eggs")
        assertEquals("eggs", engine.menu(changed).first().id)
        assertFalse(changed.done.containsKey(initial.key))
        assertEquals(setOf("prep"), changed.done["other"])
        assertTrue(engine.timeline(changed).first().at > engine.timeline(initial).first().at)
    }

    @Test(expected = IllegalArgumentException::class) fun cannotReplaceVegetablesWithAMainDish() {
        engine.swap(state(), 2, "pork")
    }

    @Test fun disabledAndExpiredMealsProduceNoAndroidAlarm() {
        val initial = state()
        assertTrue(engine.alarms(initial, now).isEmpty())
        val enabled = initial.copy(reminders = setOf(initial.key))
        val alarm = engine.alarms(enabled, now).single()
        assertEquals(Instant.parse("2026-10-30T10:30:00Z").toEpochMilli(), alarm.at)
        assertTrue(engine.alarms(enabled, alarm.at).isEmpty())
    }

    @Test fun timerRetainsItsOriginalMealWhenTheUserNavigatesElsewhere() {
        val initial = state()
        val timer = CookingTimer(now + 60_000, "Nấu canh", initial.date, Meal.DINNER)
        val changed = initial.copy(date = day.plusDays(1), meal = Meal.LUNCH, timer = timer)
        val alarm = engine.alarms(changed, now).single()
        assertEquals(day, alarm.date)
        assertEquals(Meal.DINNER, alarm.meal)
        assertEquals("cooking-timer", alarm.id)
        assertTrue(engine.alarms(changed.copy(timer = null), now).isEmpty())
    }

    @Test fun cookingCanStartBeforeMidnightForAnEarlyMeal() {
        val initial = state().copy(profile = FamilyProfile(dinner = LocalTime.of(0, 30)))
        val start = engine.timeline(initial).first().at
        assertEquals(Instant.parse("2026-10-29T16:30:00Z").toEpochMilli(), start)
        assertEquals("23:30", PlannerEngine.time(start))
    }
}
