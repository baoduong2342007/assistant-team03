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

    @Test fun swappingRechecksIngredientsWhenTheRequiredAmountIncreases() {
        fun dish(id: String, amount: Double) = Recipe(id, id, RecipeKind.MAIN, "", 10, 1000, "",
            listOf(Ingredient("Thịt", amount, "g", "Thịt")), listOf("Nấu"), "")
        val local = PlannerEngine((records.filter { it.kind != RecipeKind.MAIN } + listOf(dish("small", 100.0), dish("large", 300.0)))
            .associateBy { it.id }, listOf(listOf("small", "soup", "greens")))
        val base = local.ensureWeek(PlannerState(date = day), day).let {
            it.copy(checked = mapOf(it.key to setOf("Thịt|g", "Gạo|g", "Đã bỏ|g"), "other" to setOf("Gạo|g")))
        }
        val changed = local.swap(base, 0, "large")
        assertEquals(setOf("Gạo|g"), changed.checked[base.key])
        assertEquals(base.checked["other"], changed.checked["other"])
        val stocked = changed.copy(checked = changed.checked + (changed.key to setOf("Thịt|g")))
        assertEquals(setOf("Thịt|g"), local.swap(stocked, 0, "small").checked[stocked.key])
    }

    @Test fun selectingTheSameDishKeepsCookingProgress() {
        val initial = state().let { it.copy(done = mapOf(it.key to setOf("main"))) }
        assertEquals(initial, engine.swap(initial, 0, engine.menu(initial)[0].id))
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

    @Test fun fridgeRecognizesRegionalAliasesFromTheChatCatalog() {
        val catalog = AssistantTestCatalog.engine
        val recipe = catalog.recipes.values.first { dish -> dish.ingredients.any { it.name == "Khổ qua" } }
        assertEquals(listOf("mướp đắng"), catalog.covered(recipe, listOf("mướp đắng")))
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
        val current = engine.menu(state(), change.date, change.meal).map { it.id }
        val changed = engine.setPlan(state(), change.date, change.meal, listOf(change.recipeId) + current.drop(1))
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

    private val shoppingRecords = listOf(
        Recipe("fish", "Cá kho", RecipeKind.MAIN, "", 30, 50000, "", listOf(Ingredient("Cá basa", 400.0, "g", "Thịt & cá"),
            Ingredient("Nước mắm", 2.0, "muỗng canh", "Gia vị & đồ khô")), listOf("Kho"), ""),
        Recipe("pork", "Thịt rim", RecipeKind.MAIN, "", 30, 50000, "", listOf(Ingredient("Thịt nạc", 300.0, "g", "Thịt & cá"),
            Ingredient("Hành tím", 2.0, "củ", "Rau củ")), listOf("Rim"), ""),
        Recipe("soup", "Canh chua", RecipeKind.SOUP, "", 20, 30000, "", listOf(Ingredient("Cà chua", 2.0, "quả", "Rau củ"),
            Ingredient("Dứa", 0.25, "quả", "Rau củ"), Ingredient("Hành tím", 1.0, "củ", "Rau củ")), listOf("Nấu"), ""),
        Recipe("veg", "Rau muống xào", RecipeKind.VEG, "", 10, 15000, "", listOf(Ingredient("Rau muống", 1.0, "bó", "Rau củ"),
            Ingredient("Tỏi", 3.0, "tép", "Rau củ")), listOf("Xào"), ""),
    )
    private val shoppingEngine = PlannerEngine(shoppingRecords.associateBy { it.id },
        listOf(listOf("fish", "soup", "veg"), listOf("pork", "soup", "veg")))
    private val morning = Instant.parse("2026-10-29T20:00:00Z").toEpochMilli() // 03:00 on 30/10 in Vietnam
    private fun shoppingState() = shoppingEngine.ensureWeek(PlannerState(date = day), day)

    @Test fun shoppingReminderListsMissingFreshIngredientsEveryMorning() {
        val initial = shoppingState()
        assertTrue(shoppingEngine.alarms(initial, morning).none { it.kind == AlarmKind.SHOPPING })
        val enabled = initial.copy(shopping = ShoppingReminder(enabled = true))
        val shopping = shoppingEngine.alarms(enabled, morning).filter { it.kind == AlarmKind.SHOPPING }
        assertEquals((0L..6L).map { "shopping:${day.plusDays(it)}" }, shopping.map { it.id })
        val first = shopping.first()
        assertEquals(Instant.parse("2026-10-30T00:00:00Z").toEpochMilli(), first.at)
        assertEquals("Nhắc đi chợ", first.title)
        assertEquals(day, first.date)
        val names = shoppingEngine.missingForShopping(enabled, day)
        assertFalse("Pantry staples are not on the morning list", "Gạo" in names || "Nước mắm" in names)
        assertEquals(1, names.count { it == "Hành tím" })
        assertTrue(names.size > 6)
        assertEquals("Hôm nay cần mua: ${names.take(6).joinToString()} và ${names.size - 6} thứ khác.", first.body)
        assertTrue(first.body.startsWith("Hôm nay cần mua: Thịt nạc, Hành tím, Cà chua"))
    }

    @Test fun shoppingReminderSkipsADayWhenEverythingIsChecked() {
        val initial = shoppingState().copy(shopping = ShoppingReminder(true, LocalTime.of(6, 30)))
        val stocked = shoppingEngine.meals.associate { meal ->
            val key = PlannerEngine.key(day, meal)
            key to shoppingEngine.ingredients(shoppingEngine.menu(initial, day, meal), initial.profile.people).map { it.key }.toSet()
        }
        val ids = shoppingEngine.alarms(initial.copy(checked = stocked), morning).map { it.id }
        assertFalse("shopping:$day" in ids)
        assertTrue("shopping:${day.plusDays(1)}" in ids)
        // Only lunch bought: dinner ingredients are still listed, lunch-only ones are gone.
        val lunchKey = PlannerEngine.key(day, Meal.LUNCH)
        val lunchOnly = initial.copy(checked = mapOf(lunchKey to stocked.getValue(lunchKey)))
        val body = shoppingEngine.alarms(lunchOnly, morning).first { it.id == "shopping:$day" }.body
        assertEquals("Hôm nay cần mua: ${shoppingEngine.missingForShopping(lunchOnly, day).joinToString()}.", body)
        assertFalse(body, "Thịt nạc" in body)
        assertTrue(body, "Cá basa" in body)
    }

    @Test fun shoppingReminderAfterItsTimeStartsTomorrow() {
        val initial = shoppingState().copy(shopping = ShoppingReminder(true))
        val afterSeven = Instant.parse("2026-10-30T01:00:00Z").toEpochMilli()
        val ids = shoppingEngine.alarms(initial, afterSeven).filter { it.kind == AlarmKind.SHOPPING }.map { it.id }
        assertEquals("shopping:${day.plusDays(1)}", ids.first())
    }

    @Test fun shoppingAlarmSurvivesTheRebootStore() {
        val initial = shoppingState().copy(shopping = ShoppingReminder(true), reminders = setOf(PlannerEngine.key(day, Meal.DINNER)))
        val alarms = shoppingEngine.alarms(initial, morning)
        val parsed = ReminderStore.parse(PlannerStorage.alarmJson(alarms))
        val restored = ReminderStore.parse(ReminderStore.serialize(parsed))
        assertEquals(alarms.size, restored.size)
        val spec = restored.getValue("shopping:$day")
        assertTrue(spec.isShopping)
        assertFalse(spec.timer)
        assertTrue(spec.sameAs(parsed.getValue("shopping:$day")))
        assertEquals(ReminderSpec.KIND_MEAL, restored.getValue("meal:${PlannerEngine.key(day, Meal.DINNER)}").kind)
    }
}
