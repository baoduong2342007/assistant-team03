package vn.team03.bepnha

import java.time.LocalDate
import java.time.LocalTime

enum class Meal(val value: String, val title: String, val short: String) {
    BREAKFAST("breakfast", "Bữa sáng", "Sáng"), LUNCH("lunch", "Bữa trưa", "Trưa"), DINNER("dinner", "Bữa tối", "Tối");
    /** Lunch and dinner are a Vietnamese rice tray (mặn + canh + rau); breakfast is a single dish. */
    val tray: Boolean get() = this != BREAKFAST
    companion object { fun from(value: String) = entries.firstOrNull { it.value == value } }
}
enum class RecipeKind(val label: String) {
    MAIN("Món mặn"), SOUP("Món canh"), VEG("Món rau"), BREAKFAST("Món sáng");

    companion object {
        val trayKinds = listOf(MAIN, SOUP, VEG)
    }
}
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
    /** Diet tags from [TAGS]; empty when the catalog does not say. */
    val tags: Set<String> = emptySet(),
    /** Allergens from [ALLERGENS], derived from the listed ingredients (fish sauce is not counted as fish). */
    val allergens: Set<String> = emptySet(),
    /** Estimated sodium per serving: "low", "mid", "high", or "" when unknown. */
    val sodium: String = "",
) {
    companion object {
        val TAGS = setOf("low_salt", "low_sugar", "soft", "low_fat", "vegetarian")
        val ALLERGENS = setOf("shrimp", "shellfish", "fish", "egg", "peanut", "soy", "milk", "sesame", "wheat")
        val SODIUM = setOf("low", "mid", "high")
    }
}
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
/** Daily morning reminder listing what is still missing for that day. */
data class ShoppingReminder(val enabled: Boolean = false, val time: LocalTime = LocalTime.of(7, 0))
/**
 * The family's lasting eating needs, applied to every suggestion and plan.
 * [tags] must be carried by every dish; [allergens] and [avoid] (canonical ingredient names) must not appear.
 */
data class DietProfile(
    val tags: Set<String> = emptySet(),
    val allergens: Set<String> = emptySet(),
    val avoid: Set<String> = emptySet(),
    val maxMinutes: Int? = null,
    /** VND per meal for the family's current number of people. */
    val maxCost: Long? = null,
) {
    val isEmpty get() = tags.isEmpty() && allergens.isEmpty() && avoid.isEmpty() && maxMinutes == null && maxCost == null

    fun allows(recipe: Recipe) = reasons(recipe).isEmpty()

    /** Why [recipe] does not fit, in short Vietnamese phrases; empty when it fits. */
    fun reasons(recipe: Recipe): List<String> = buildList {
        (tags - recipe.tags).forEach { add("chưa phải món ${TAG_LABELS[it] ?: it}") }
        (recipe.allergens intersect allergens).forEach { add("có ${ALLERGEN_LABELS[it] ?: it}") }
        recipe.ingredients.map { it.name }.distinct().filter { it in avoid }.forEach { add("có ${it.lowercase(PlannerEngine.locale)}") }
    }

    /** "ít muối, mềm dễ nhai; tránh tôm, hành lá; tối đa 30 phút" */
    fun describe(): String = listOfNotNull(
        tags.mapNotNull(TAG_LABELS::get).joinToString().ifBlank { null },
        (allergens.mapNotNull(ALLERGEN_LABELS::get) + avoid.map { it.lowercase(PlannerEngine.locale) }).joinToString().ifBlank { null }?.let { "tránh $it" },
        maxMinutes?.let { "nấu tối đa $it phút" },
        maxCost?.let { "tối đa ${PlannerEngine.money(it)} mỗi bữa" },
    ).joinToString("; ")

    companion object {
        val TAG_LABELS = linkedMapOf("low_salt" to "ít muối", "low_sugar" to "ít đường", "soft" to "mềm, dễ nhai",
            "low_fat" to "ít dầu mỡ", "vegetarian" to "ăn chay")
        val ALLERGEN_LABELS = linkedMapOf("shrimp" to "tôm", "shellfish" to "hải sản có vỏ", "fish" to "cá", "egg" to "trứng",
            "peanut" to "đậu phộng", "soy" to "đậu nành", "milk" to "sữa", "sesame" to "mè", "wheat" to "lúa mì")
    }
}

/** How the assistant talks: what it calls the user, and whether/how fast it reads replies aloud. */
data class VoiceSettings(val addressAs: String = "bạn", val readAloud: Boolean = true, val rate: Float = 0.85f) {
    companion object {
        val RATES = listOf(0.7f to "Chậm", 0.85f to "Vừa", 1.0f to "Nhanh")
        fun clean(addressAs: String) = addressAs.trim().take(20).ifBlank { "bạn" }
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
    val shopping: ShoppingReminder = ShoppingReminder(),
    val voice: VoiceSettings = VoiceSettings(),
    val diet: DietProfile = DietProfile(),
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
    val kind: AlarmKind = if (timer) AlarmKind.TIMER else AlarmKind.MEAL,
)
/** Mirrors the kinds accepted by ReminderSpec.java. */
enum class AlarmKind(val value: String) { MEAL("meal"), TIMER("timer"), SHOPPING("shopping") }
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
