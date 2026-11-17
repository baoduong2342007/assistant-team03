package vn.team03.bepnha

import org.junit.Assert.*
import org.junit.Test

class CookAlongTest {
    private val engine = AssistantTestCatalog.engine
    private val day = AssistantTestCatalog.day
    private val state = AssistantTestCatalog.state()

    @Test fun stepsFollowTheTimelineAndCoverEveryRecipeStep() {
        val steps = CookAlongPlan.steps(engine, state, day, Meal.DINNER)
        val timeline = engine.timeline(state, day, Meal.DINNER)
        assertEquals(timeline.map { it.key }, steps.map { it.taskKey }.distinct())
        engine.menu(state, day, Meal.DINNER).forEach { recipe ->
            assertEquals(recipe.steps, steps.filter { it.title == recipe.name && !it.text.startsWith("Mẹo") }.map { it.text })
        }
        assertEquals("serve", steps.last().taskKey)
    }

    @Test fun durationsInAStepBecomeATimerSuggestion() {
        assertEquals(10, CookAlongPlan.timerIn("Luộc trứng 10 phút cho chín."))
        assertEquals(7, CookAlongPlan.timerIn("Rim lửa nhỏ 5-7 phút"))
        assertNull(CookAlongPlan.timerIn("Nêm lại cho vừa ăn."))
        assertNull(CookAlongPlan.timerIn("Ngâm 600 phút"))
    }

    @Test fun movingPastATaskFinishesIt() {
        val steps = CookAlongPlan.steps(engine, state, day, Meal.DINNER)
        val lastOfFirstTask = steps.indexOfLast { it.taskKey == steps.first().taskKey }
        assertTrue(CookAlong(day, Meal.DINNER, steps, lastOfFirstTask).finishesTask)
        if (lastOfFirstTask > 0) assertFalse(CookAlong(day, Meal.DINNER, steps, 0).finishesTask)
        val end = CookAlong(day, Meal.DINNER, steps, steps.lastIndex)
        assertTrue(end.isLast && end.finishesTask)
        assertTrue(CookAlongPlan.spoken(CookAlong(day, Meal.DINNER, steps, 0)).startsWith("Bước 1 trên ${steps.size}."))
    }

    @Test fun greetingSummarizesTheNextMealWhatIsMissingAndWhenToStart() {
        val morning = day.atTime(8, 0).atZone(PlannerEngine.zone).toInstant().toEpochMilli()
        val elder = state.copy(voice = VoiceSettings(addressAs = "bác"))
        val text = AssistantGreeting.text(engine, elder, day, morning)
        val lunch = engine.menu(elder, day, Meal.LUNCH)
        assertTrue(text, text.startsWith("Chào bác. Bữa trưa hôm nay có ${lunch.joinToString { it.name }}."))
        assertTrue(text.contains("Còn cần")); assertTrue(text.contains("Nên vào bếp lúc"))
        assertTrue(text.endsWith("Bác cần cháu giúp gì?"))
        val evening = day.atTime(20, 0).atZone(PlannerEngine.zone).toInstant().toEpochMilli()
        assertEquals(AssistantGreeting.Upcoming(day.plusDays(1), Meal.LUNCH), AssistantGreeting.upcoming(state, day, evening))
        assertTrue(AssistantGreeting.text(engine, state, day, evening).contains("ngày mai"))
    }

    @Test fun greetingSaysWhenEverythingIsAlreadyBought() {
        val morning = day.atTime(8, 0).atZone(PlannerEngine.zone).toInstant().toEpochMilli()
        val key = PlannerEngine.key(day, Meal.LUNCH)
        val all = engine.ingredients(engine.menu(state, day, Meal.LUNCH), state.profile.people).map { it.key }.toSet()
        val stocked = state.copy(checked = mapOf(key to all))
        assertTrue(AssistantGreeting.text(engine, stocked, day, morning).contains("Đã có đủ nguyên liệu."))
    }
}
