package vn.team03.bepnha

import java.time.LocalDate

/** One screen of "Nấu cùng tôi": a short instruction read aloud, belonging to one cooking task. */
data class CookStep(val title: String, val text: String, val taskKey: String, val timerMinutes: Int? = null)

/** An open cook-along session for one meal. */
data class CookAlong(val date: LocalDate, val meal: Meal, val steps: List<CookStep>, val index: Int = 0) {
    val step get() = steps[index]
    val isLast get() = index == steps.lastIndex
    /** The cooking task is finished when the user moves past its last step. */
    val finishesTask get() = isLast || steps[index + 1].taskKey != step.taskKey
}

object CookAlongPlan {
    private val minutes = Regex("""(\d{1,3})(?:\s*[-–]\s*(\d{1,3}))?\s*phút""")

    /** Steps in timeline order: prepare, then each dish's recipe steps, then serving. */
    fun steps(engine: PlannerEngine, state: PlannerState, date: LocalDate, meal: Meal): List<CookStep> =
        engine.timeline(state, date, meal).flatMap { task ->
            val recipe = task.recipeId?.let(engine.recipes::get)
            if (recipe == null) listOf(CookStep(task.title, task.detail, task.key, task.minutes.takeIf { it > 0 && task.key != "prep" }))
            else recipe.steps.map { text -> CookStep(recipe.name, text, task.key, timerIn(text)) } +
                listOfNotNull(recipe.tip.takeIf { it.isNotBlank() }?.let { CookStep(recipe.name, "Mẹo: $it", task.key) })
        }

    /** "Luộc 5-7 phút" → 7; null when the step names no duration. */
    fun timerIn(text: String): Int? = minutes.find(text)?.let { match ->
        (match.groupValues[2].ifEmpty { match.groupValues[1] }).toIntOrNull()?.takeIf { it in 1..180 }
    }

    /** What is read for a step: "Bước 3 trên 12. Canh chua cá. Cho cá vào nồi…" */
    fun spoken(session: CookAlong): String =
        "Bước ${session.index + 1} trên ${session.steps.size}. ${session.step.title}. ${session.step.text}" +
            (session.step.timerMinutes?.let { " Nói hẹn giờ $it phút nếu cần." } ?: "")
}

/** A short local summary read when the assistant opens; no network call. */
object AssistantGreeting {
    data class Upcoming(val date: LocalDate, val meal: Meal)

    /** The next meal still to be served (today or later in the week). */
    fun upcoming(state: PlannerState, today: LocalDate, now: Long): Upcoming? =
        (0L..6L).map(today::plusDays).flatMap { day -> Meal.entries.map { Upcoming(day, it) } }.firstOrNull { (day, meal) ->
            state.plans[PlannerEngine.key(day, meal)] != null &&
                day.atTime(state.profile.time(meal)).atZone(PlannerEngine.zone).toInstant().toEpochMilli() > now
        }

    fun text(engine: PlannerEngine, state: PlannerState, today: LocalDate, now: Long): String {
        val who = AssistantVoice.title(state.voice.addressAs)
        val me = AssistantVoice.self(state.voice.addressAs)
        val next = upcoming(state, today, now)
            ?: return "Chào ${state.voice.addressAs}. ${me.replaceFirstChar { it.titlecase() }} có thể giúp lên thực đơn, đi chợ hoặc nấu ăn."
        val menu = engine.menu(state, next.date, next.meal)
        val key = PlannerEngine.key(next.date, next.meal)
        val missing = engine.ingredients(menu, state.profile.people)
            .filter { it.group != "Gia vị & đồ khô" && it.key !in state.checked[key].orEmpty() }.map { it.name.lowercase(PlannerEngine.locale) }
        val start = engine.timeline(state, next.date, next.meal).first().at
        val day = when (next.date) { today -> "hôm nay"; today.plusDays(1) -> "ngày mai"; else -> PlannerEngine.fullDate(next.date) }
        return buildString {
            append("Chào ${state.voice.addressAs}. ${next.meal.title} $day có ${menu.joinToString { it.name }}.")
            if (missing.isEmpty()) append(" Đã có đủ nguyên liệu.")
            else append(" Còn cần ${missing.take(4).joinToString()}${if (missing.size > 4) " và ${missing.size - 4} thứ khác" else ""}.")
            if (start > now) append(" Nên vào bếp lúc ${PlannerEngine.time(start)}.")
            append(" $who cần $me giúp gì?")
        }
    }
}
