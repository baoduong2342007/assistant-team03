package vn.team03.bepnha

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

class RecipeRepository(context: Context) {
    val engine: PlannerEngine
    init {
        val root = JSONObject(context.assets.open("recipes.json").bufferedReader().use { it.readText() })
        val recipes = root.getJSONArray("recipes").objects().associate { item ->
            val id = item.getString("id")
            id to Recipe(id, item.getString("name"), when (item.getString("kind")) {
                "main" -> RecipeKind.MAIN; "soup" -> RecipeKind.SOUP; "breakfast" -> RecipeKind.BREAKFAST; else -> RecipeKind.VEG
            }, item.getString("emoji"), item.getInt("minutes"), item.getLong("cost"), item.getString("note"),
                item.getJSONArray("ingredients").objects().map { ingredient ->
                    Ingredient(ingredient.getString("name"), ingredient.getDouble("amount"),
                        ingredient.getString("unit"), ingredient.getString("group"))
                }, item.getJSONArray("steps").strings(), item.getString("tip"))
        }
        val choices = root.getJSONArray("combinations")
        engine = PlannerEngine(recipes, (0 until choices.length()).map { choices.getJSONArray(it).strings() },
            root.getJSONArray("breakfasts").strings())
    }
}

/** Native persistence; the Android app never reads a web page or browser localStorage. */
class PlannerStorage(context: Context, private val engine: PlannerEngine) {
    private val preferences = context.getSharedPreferences("bep_nha_planner_v2", Context.MODE_PRIVATE)

    fun load(today: LocalDate): PlannerState = runCatching {
        val root = JSONObject(preferences.getString("state", "{}") ?: "{}")
        val profile = root.optJSONObject("profile") ?: JSONObject()
        fun safeTime(key: String, default: String) = runCatching { LocalTime.parse(profile.optString(key, default)) }
            .getOrDefault(LocalTime.parse(default))
        val family = FamilyProfile(profile.optString("name", "Nhà mình").take(35).ifBlank { "Nhà mình" },
            profile.optInt("people", 4).coerceIn(1, 12), safeTime("lunch", "11:30"), safeTime("dinner", "18:30"),
            safeTime("breakfast", "06:30"))
        val plans = root.optJSONObject("plans").stringLists().filter { (key, ids) -> engine.validEntry(key, ids) }
        val selected = runCatching { LocalDate.parse(root.optString("date")) }.getOrDefault(today)
        fun timer(item: JSONObject, fallbackId: Long) = runCatching {
            CookingTimer(item.optLong("id", fallbackId), item.getLong("end"), item.getString("label").take(200),
                LocalDate.parse(item.getString("date")), Meal.from(item.getString("meal")) ?: Meal.DINNER)
        }.getOrNull()
        // Version 0.3 stored a single "timer" object; later versions store a "timers" array.
        val timers = (root.optJSONArray("timers")?.objects()?.mapIndexedNotNull { index, item -> timer(item, index.toLong()) }
            ?: listOfNotNull(root.optJSONObject("timer")?.let { timer(it, 0L) }))
            .distinctBy { it.id }.take(PlannerEngine.MAX_TIMERS)
        val shopMeals = root.optJSONArray("shopMeals")?.strings()?.mapNotNull(Meal::from)?.toSet().orEmpty()
        engine.ensureWeek(PlannerState(family, selected, Meal.from(root.optString("meal")) ?: Meal.DINNER, plans,
            root.optJSONObject("checked").stringLists().mapValues { it.value.toSet() },
            root.optJSONObject("done").stringLists().mapValues { it.value.toSet() },
            root.optJSONArray("favorites")?.strings()?.filter { it in engine.recipes }?.toSet().orEmpty(),
            root.optJSONArray("reminders")?.strings()?.toSet().orEmpty(), timers, shopMeals), today)
    }.getOrElse { engine.ensureWeek(PlannerState(date = today), today) }

    fun save(state: PlannerState) {
        val profile = state.profile
        val root = JSONObject().put("profile", JSONObject().put("name", profile.name).put("people", profile.people)
            .put("lunch", profile.lunch.toString()).put("dinner", profile.dinner.toString())
            .put("breakfast", profile.breakfast.toString()))
            .put("date", state.date.toString()).put("meal", state.meal.value)
            .put("plans", jsonLists(state.plans)).put("checked", jsonLists(state.checked)).put("done", jsonLists(state.done))
            .put("favorites", JSONArray(state.favorites.toList())).put("reminders", JSONArray(state.reminders.toList()))
            .put("shopMeals", JSONArray(state.shopMeals.map { it.value }))
            .put("timers", JSONArray().apply {
                state.timers.forEach { timer -> put(JSONObject().put("id", timer.id).put("end", timer.end).put("label", timer.label)
                    .put("date", timer.date.toString()).put("meal", timer.meal.value)) }
            })
        // Called on the ViewModel's serial IO worker, so confirmed state survives process termination.
        check(preferences.edit().putString("state", root.toString()).commit()) { "Could not save planner state" }
    }

    companion object {
        fun alarmJson(alarms: List<NativeAlarm>): String = JSONArray().apply {
            alarms.forEach { alarm -> put(JSONObject().put("id", alarm.id).put("at", alarm.at)
                .put("title", alarm.title).put("body", alarm.body).put("date", alarm.date.toString())
                .put("meal", alarm.meal.value).put("timer", alarm.timer)) }
        }.toString()
    }
}

private fun JSONArray.objects() = (0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings() = (0 until length()).map { getString(it) }
private fun JSONObject?.stringLists(): Map<String, List<String>> = this?.let { objectValue ->
    objectValue.keys().asSequence().mapNotNull { key ->
        objectValue.optJSONArray(key)?.let { key to it.strings() }
    }.toMap()
}.orEmpty()
private fun jsonLists(map: Map<String, Collection<String>>) = JSONObject().apply {
    map.forEach { (key, values) -> put(key, JSONArray(values.toList())) }
}
