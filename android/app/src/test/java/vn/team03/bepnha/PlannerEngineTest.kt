package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime

class PlannerEngineTest {
    private val day = LocalDate.of(2026, 10, 30)
    private val now = Instant.parse("2026-10-30T06:00:00Z").toEpochMilli()
    private fun recipe(id: String, kind: RecipeKind, minutes: Int, cost: Long, vararg fresh: String) = Recipe(id, id, kind, "", minutes, cost, "",
        listOf(Ingredient("Nước mắm", 1.0, "muỗng cà phê", PlannerEngine.PANTRY)) + fresh.map { Ingredient(it, 100.0, "g", "Tươi") },
        listOf("Nấu chín"), "")
    private val records = listOf(recipe("pork", RecipeKind.MAIN, 45, 65000, "Thịt ba chỉ"), recipe("eggs", RecipeKind.MAIN, 10, 22000, "Trứng gà"),
        recipe("tofu", RecipeKind.MAIN, 20, 25000, "Đậu hũ", "Cà chua"), recipe("soup", RecipeKind.SOUP, 15, 25000, "Cải xanh", "Thịt heo bằm"),
        recipe("sour", RecipeKind.SOUP, 25, 45000, "Cá basa", "Cà chua"), recipe("greens", RecipeKind.VEG, 10, 15000, "Rau muống"),
        recipe("broccoli", RecipeKind.VEG, 15, 22000, "Bông cải xanh"),
        recipe("bread", RecipeKind.BREAKFAST, 10, 40000, "Bánh mì", "Trứng gà"), recipe("porridge", RecipeKind.BREAKFAST, 30, 30000, "Thịt heo bằm"))
    private val engine = PlannerEngine(records.associateBy { it.id }, listOf(listOf("pork", "soup", "greens"), listOf("eggs", "sour", "broccoli")),
        listOf("bread", "porridge"))
    private fun state() = engine.ensureWeek(PlannerState(date = day), day)

    @Test fun nativeWeekRetainsSavedChoicesWhenTheDateChanges() {
        val initial = state()
        val futureKey = PlannerEngine.key(day.plusDays(1), Meal.DINNER)
        val changed = initial.copy(plans = initial.plans + (futureKey to listOf("tofu", "soup", "greens")))
        val tomorrow = engine.ensureWeek(changed, day.plusDays(1))
        assertEquals(day.plusDays(1), tomorrow.date)
        assertEquals(listOf("tofu", "soup", "greens"), tomorrow.plans[futureKey])
        assertTrue(tomorrow.plans.containsKey(PlannerEngine.key(day.plusDays(7), Meal.DINNER)))
        assertEquals(listOf("bread"), tomorrow.plans[PlannerEngine.key(day.plusDays(7), Meal.BREAKFAST)])
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
        val timer = CookingTimer(7, now + 60_000, "Nấu canh", initial.date, Meal.DINNER)
        val changed = initial.copy(date = day.plusDays(1), meal = Meal.LUNCH, timers = listOf(timer))
        val alarm = engine.alarms(changed, now).single()
        assertEquals(day, alarm.date)
        assertEquals(Meal.DINNER, alarm.meal)
        assertEquals("cooking-timer:7", alarm.id)
        assertTrue(engine.alarms(changed.copy(timers = emptyList()), now).isEmpty())
    }

    @Test fun cookingCanStartBeforeMidnightForAnEarlyMeal() {
        val initial = state().copy(profile = FamilyProfile(dinner = LocalTime.of(0, 30)))
        val start = engine.timeline(initial).first().at
        assertEquals(Instant.parse("2026-10-29T16:30:00Z").toEpochMilli(), start)
        assertEquals("23:30", PlannerEngine.time(start))
    }

    @Test fun twoDishesCanCountDownAtTheSameTime() {
        val timers = listOf(CookingTimer(1, now + 60_000, "Kho thịt", day, Meal.DINNER), CookingTimer(2, now + 120_000, "Nấu canh", day, Meal.DINNER))
        val alarms = engine.alarms(state().copy(timers = timers), now)
        assertEquals(listOf("cooking-timer:1", "cooking-timer:2"), alarms.map { it.id })
        assertTrue(alarms.all { it.timer })
    }

    @Test fun breakfastIsOneDishWithItsOwnTimelineAndNoRice() {
        val breakfast = state().copy(meal = Meal.BREAKFAST)
        val menu = engine.menu(breakfast)
        assertEquals(listOf("bread"), menu.map { it.id })
        assertEquals(15, engine.minutes(menu))
        val tasks = engine.timeline(breakfast)
        assertEquals(Instant.parse("2026-10-29T23:30:00Z").toEpochMilli(), tasks.last().at)
        assertEquals(tasks.last().at - 15 * 60_000L, tasks.first().at)
        assertTrue(engine.ingredients(menu, 4).none { it.name == "Gạo" })
        assertFalse(engine.validPlan(listOf("pork", "soup", "greens"), Meal.BREAKFAST))
        assertFalse(engine.validPlan(listOf("bread"), Meal.LUNCH))
    }

    @Test fun shoppingForLunchAndDinnerMergesBothMealsAndAddsRiceForEachTray() {
        val s = state().copy(shopMeals = setOf(Meal.LUNCH, Meal.DINNER))
        val menu = engine.shoppingMenu(s)
        assertEquals(6, menu.size)
        val ingredients = engine.ingredients(menu, 4)
        assertEquals(720.0, ingredients.first { it.name == "Gạo" }.amount, 0.0)
        assertEquals(6.0, ingredients.first { it.name == "Nước mắm" }.amount, 0.0)
        assertEquals(7, engine.shoppingMenu(s.copy(shopMeals = Meal.entries.toSet())).size)
        assertEquals(setOf(Meal.DINNER), s.copy(shopMeals = emptySet()).shoppingMeals)
    }

    @Test fun fridgeIngredientsBecomeCompleteTraysCentredOnThatIngredient() {
        val trays = engine.suggestTrays(listOf("thịt heo"), Meal.DINNER)
        assertEquals(3, trays.size)
        assertEquals("pork", trays.first().ids.first())
        assertTrue(trays.all { engine.validPlan(it.ids, Meal.DINNER) && it.matched == listOf("thịt heo") })
        assertEquals(3, trays.map { it.ids.first() }.distinct().size)
        val fish = engine.suggestTrays(listOf("Cá", "cà chua"), Meal.LUNCH).first()
        assertEquals(listOf("Cá", "cà chua"), fish.matched)
        assertTrue("sour" in fish.ids)
        assertFalse("Cà chua" in fish.missing)
        assertEquals(listOf("bread"), engine.suggestTrays(listOf("2 quả trứng"), Meal.BREAKFAST).map { it.ids.single() })
        assertTrue(engine.suggestTrays(listOf("sầu riêng"), Meal.DINNER).isEmpty())
    }

    @Test fun chatUnderstandsRelativeDaysAndTheEveningMeal() {
        assertEquals(day, engine.parseDate("Hôm nay ăn gì", day))
        assertEquals(day.plusDays(1), engine.parseDate("ngày mai tôi muốn ăn cá", day))
        assertEquals(day.plusDays(2), engine.parseDate("Ngày mai nữa nấu gì", day))
        assertEquals(day.plusDays(2), engine.parseDate("ngày kia", day))
        assertEquals(day.plusDays(1), engine.parseDate("thứ bảy", day))
        assertEquals(day.plusDays(2), engine.parseDate("chủ nhật", day))
        assertNull(engine.parseDate("món nào nhanh", day))
        assertEquals(Meal.DINNER, engine.parseMeal("tối mai"))
        assertEquals(Meal.BREAKFAST, engine.parseMeal("sáng mai ăn gì"))
        assertNull(engine.parseMeal("ngày mai tôi muốn ăn cá"))
    }

    @Test fun chatProposesAMenuChangeForTheRightDayAndMeal() {
        val answer = engine.answer("Ngày mai tôi muốn ăn đậu hũ", state(), day)
        val change = answer.changes.first()
        assertEquals(MenuChange(day.plusDays(1), Meal.DINNER, "tofu"), change)
        val changed = engine.setDish(state(), change.date, change.meal, change.recipeId)
        assertEquals("tofu", engine.menu(changed, change.date, change.meal).first().id)
        assertEquals(MenuChange(day.plusDays(2), Meal.BREAKFAST, "porridge"),
            engine.answer("sáng ngày mai nữa cho ăn porridge", state(), day).changes.single())
        val fridge = engine.answer("Tủ lạnh có trứng và rau muống", state(), day)
        assertTrue(fridge.trays.isNotEmpty())
        assertEquals(listOf("trung", "rau muong"), fridge.trays.first().matched)
        val timer = engine.answer("Hẹn 10 phút luộc rau", state(), day)
        assertEquals(10, timer.timerMinutes)
        assertEquals("Luộc rau", timer.timerLabel)
    }
}
