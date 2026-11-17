package vn.team03.bepnha

import java.time.LocalDate
import java.time.LocalTime

enum class Meal(val value: String, val title: String, val short: String) {
    BREAKFAST("breakfast", "Bữa sáng", "Sáng"), LUNCH("lunch", "Bữa trưa", "Trưa"), DINNER("dinner", "Bữa tối", "Tối");
    /** Lunch and dinner are a Vietnamese rice tray (mặn + canh + rau); breakfast is a single dish. */
    val tray: Boolean get() = this != BREAKFAST
    companion object { fun from(value: String) = entries.firstOrNull { it.value == value } }
}
enum class RecipeKind(val label: String) { MAIN("Món mặn"), SOUP("Món canh"), VEG("Món rau"), BREAKFAST("Món sáng") }
enum class Screen(val title: String) {
    TODAY("Hôm nay"), PLAN("Thực đơn"), SHOPPING("Đi chợ"), COOK("Vào bếp"), ASK("Hỏi bếp"),
    FAMILY("Gia đình của bạn"), FAVORITES("Món đã lưu")
}
enum class AskTab(val title: String) { FRIDGE("Tủ lạnh"), CHAT("Trò chuyện"), TIMER("Hẹn giờ") }
data class Ingredient(val name: String, val amount: Double, val unit: String, val group: String) {
    val key: String get() = "$name|$unit"
}
data class Recipe(
    val id: String, val name: String, val kind: RecipeKind, val emoji: String,
    val minutes: Int, val cost: Long, val note: String,
    val ingredients: List<Ingredient>, val steps: List<String>, val tip: String,
)
data class FamilyProfile(
    val name: String = "Nhà mình", val people: Int = 4,
    val lunch: LocalTime = LocalTime.of(11, 30), val dinner: LocalTime = LocalTime.of(18, 30),
    val breakfast: LocalTime = LocalTime.of(6, 30),
) {
    fun time(meal: Meal) = when (meal) { Meal.BREAKFAST -> breakfast; Meal.LUNCH -> lunch; Meal.DINNER -> dinner }
    fun with(meal: Meal, time: LocalTime) = when (meal) {
        Meal.BREAKFAST -> copy(breakfast = time); Meal.LUNCH -> copy(lunch = time); Meal.DINNER -> copy(dinner = time)
    }
}
data class CookingTimer(val id: Long, val end: Long, val label: String, val date: LocalDate, val meal: Meal)
data class PlannerState(
    val profile: FamilyProfile = FamilyProfile(),
    val date: LocalDate, val meal: Meal = Meal.DINNER,
    val plans: Map<String, List<String>> = emptyMap(),
    val checked: Map<String, Set<String>> = emptyMap(),
    val done: Map<String, Set<String>> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val reminders: Set<String> = emptySet(),
    val timers: List<CookingTimer> = emptyList(),
    /** Meals bought together on the shopping screen; empty follows the selected meal. */
    val shopMeals: Set<Meal> = emptySet(),
) {
    val key: String get() = PlannerEngine.key(date, meal)
    val shoppingMeals: Set<Meal> get() = shopMeals.ifEmpty { setOf(meal) }
}
data class CookingTask(
    val key: String, val title: String, val detail: String, val at: Long,
    val minutes: Int = 0, val recipeId: String? = null,
)
data class NativeAlarm(
    val id: String, val at: Long, val title: String, val body: String,
    val date: LocalDate, val meal: Meal, val timer: Boolean = false,
)
sealed interface PlannerSheet {
    data class RecipeDetail(val id: String) : PlannerSheet
    data class Swap(val index: Int) : PlannerSheet
    data object Reminder : PlannerSheet
}

/** One suggested meal built around what is already in the fridge. */
data class TraySuggestion(val ids: List<String>, val matched: List<String>, val missing: List<String>)
data class MenuChange(val date: LocalDate, val meal: Meal, val recipeId: String)
data class AssistantAnswer(
    val text: String,
    val recipes: List<String> = emptyList(),
    val trays: List<TraySuggestion> = emptyList(),
    val date: LocalDate? = null,
    val meal: Meal? = null,
    val changes: List<MenuChange> = emptyList(),
    val timerMinutes: Int? = null,
    val timerLabel: String = "",
)
data class ChatEntry(val question: String, val answer: AssistantAnswer)
