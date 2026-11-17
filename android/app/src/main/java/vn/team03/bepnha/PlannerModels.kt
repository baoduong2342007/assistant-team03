package vn.team03.bepnha

import java.time.LocalDate
import java.time.LocalTime

enum class Meal(val value: String, val title: String) {
    LUNCH("lunch", "Bữa trưa"), DINNER("dinner", "Bữa tối");
    companion object { fun from(value: String) = entries.firstOrNull { it.value == value } }
}
enum class RecipeKind(val label: String) { MAIN("Món mặn"), SOUP("Món canh"), VEG("Món rau") }
enum class Screen(val title: String) {
    TODAY("Hôm nay"), PLAN("Thực đơn"), SHOPPING("Đi chợ"), COOK("Vào bếp"),
    FAMILY("Gia đình của bạn"), FAVORITES("Món đã lưu")
}
data class Ingredient(val name: String, val amount: Double, val unit: String, val group: String) {
    val key: String get() = "$name|$unit"
}
data class Recipe(
    val id: String, val name: String, val kind: RecipeKind, val emoji: String,
    val minutes: Int, val cost: Long, val note: String,
    val ingredients: List<Ingredient>, val steps: List<String>, val tip: String,
    /** Diet tags from [TAGS]; empty when the catalog does not say. */
    val tags: Set<String> = emptySet(),
    /** Allergens from [ALLERGENS], derived from the listed ingredients (fish sauce is not counted as fish). */
    val allergens: Set<String> = emptySet(),
    /** Estimated sodium per serving: "low", "mid", "high", or "" when unknown. */
    val sodium: String = "",
) {
    companion object {
        val TAGS = setOf("low_salt", "low_sugar", "soft", "low_fat", "vegetarian")
        val ALLERGENS = setOf("shrimp", "shellfish", "fish", "egg", "peanut", "soy", "milk", "sesame")
        val SODIUM = setOf("low", "mid", "high")
    }
}
data class FamilyProfile(
    val name: String = "Nhà mình", val people: Int = 4,
    val lunch: LocalTime = LocalTime.of(11, 30), val dinner: LocalTime = LocalTime.of(18, 30),
) { fun time(meal: Meal) = if (meal == Meal.LUNCH) lunch else dinner }
/** Daily morning reminder listing what is still missing for that day's lunch and dinner. */
data class ShoppingReminder(val enabled: Boolean = false, val time: LocalTime = LocalTime.of(7, 0))
/** How the assistant talks: what it calls the user, and whether/how fast it reads replies aloud. */
data class VoiceSettings(val addressAs: String = "bạn", val readAloud: Boolean = true, val rate: Float = 0.85f) {
    companion object {
        val RATES = listOf(0.7f to "Chậm", 0.85f to "Vừa", 1.0f to "Nhanh")
        fun clean(addressAs: String) = addressAs.trim().take(20).ifBlank { "bạn" }
    }
}
data class CookingTimer(val end: Long, val label: String, val date: LocalDate, val meal: Meal)
data class PlannerState(
    val profile: FamilyProfile = FamilyProfile(),
    val date: LocalDate, val meal: Meal = Meal.DINNER,
    val plans: Map<String, List<String>> = emptyMap(),
    val checked: Map<String, Set<String>> = emptyMap(),
    val done: Map<String, Set<String>> = emptyMap(),
    val favorites: Set<String> = emptySet(),
    val reminders: Set<String> = emptySet(),
    val timer: CookingTimer? = null,
    val shopping: ShoppingReminder = ShoppingReminder(),
    val voice: VoiceSettings = VoiceSettings(),
) { val key: String get() = PlannerEngine.key(date, meal) }
data class CookingTask(
    val key: String, val title: String, val detail: String, val at: Long,
    val minutes: Int = 0, val recipeId: String? = null,
)
data class NativeAlarm(
    val id: String, val at: Long, val title: String, val body: String,
    val date: LocalDate, val meal: Meal, val timer: Boolean = false,
    val kind: AlarmKind = if (timer) AlarmKind.TIMER else AlarmKind.MEAL,
)
/** Mirrors the kinds accepted by ReminderSpec.java. */
enum class AlarmKind(val value: String) { MEAL("meal"), TIMER("timer"), SHOPPING("shopping") }
sealed interface PlannerSheet {
    data class RecipeDetail(val id: String) : PlannerSheet
    data class Swap(val index: Int) : PlannerSheet
    data object Reminder : PlannerSheet
}
data class AssistantAnswer(val text: String, val recipes: List<String> = emptyList())
