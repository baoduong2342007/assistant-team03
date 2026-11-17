package vn.team03.bepnha

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

class RecipeRepository(context: Context) {
    val engine: PlannerEngine = parse(context.assets.open("recipes.json").bufferedReader().use { it.readText() })

    companion object {
        /** Pure parser so JVM tests read the shipped catalog exactly as the app does. */
        fun parse(json: String): PlannerEngine {
            val root = JSONObject(json)
            val recipes = root.getJSONArray("recipes").objects().associate { item ->
                val id = item.getString("id")
                id to Recipe(id, item.getString("name"), when (item.getString("kind")) {
                    "main" -> RecipeKind.MAIN; "soup" -> RecipeKind.SOUP; else -> RecipeKind.VEG
                }, item.getString("emoji"), item.getInt("minutes"), item.getLong("cost"), item.getString("note"),
                    item.getJSONArray("ingredients").objects().map { ingredient ->
                        Ingredient(ingredient.getString("name"), ingredient.getDouble("amount"),
                            ingredient.getString("unit"), ingredient.getString("group"))
                    }, item.getJSONArray("steps").strings(), item.getString("tip"),
                    item.optJSONArray("tags")?.strings()?.toSet().orEmpty(),
                    item.optJSONArray("allergens")?.strings()?.toSet().orEmpty(),
                    item.optString("sodium", ""))
            }
            val choices = root.getJSONArray("combinations")
            return PlannerEngine(recipes, (0 until choices.length()).map { choices.getJSONArray(it).strings() })
        }
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
            profile.optInt("people", 4).coerceIn(1, 12), safeTime("lunch", "11:30"), safeTime("dinner", "18:30"))
        val plans = root.optJSONObject("plans").stringLists().filterValues(engine::validPlan)
        val selected = runCatching { LocalDate.parse(root.optString("date")) }.getOrDefault(today)
        val timer = root.optJSONObject("timer")?.let { item -> runCatching {
            CookingTimer(item.getLong("end"), item.getString("label").take(200), LocalDate.parse(item.getString("date")),
                Meal.from(item.getString("meal")) ?: Meal.DINNER)
        }.getOrNull() }
        val shopping = root.optJSONObject("shopping")?.let { item -> ShoppingReminder(item.optBoolean("enabled", false),
            runCatching { LocalTime.parse(item.optString("time", "07:00")) }.getOrDefault(LocalTime.of(7, 0)))
        } ?: ShoppingReminder()
        val voice = root.optJSONObject("voice")?.let { item -> VoiceSettings(VoiceSettings.clean(item.optString("address_as", "bạn")),
            item.optBoolean("read_aloud", true), item.optDouble("rate", 0.85).toFloat().takeIf { it in 0.5f..1.5f } ?: 0.85f)
        } ?: VoiceSettings()
        val diet = root.optJSONObject("diet")?.let { runCatching { dietFrom(it, engine) }.getOrNull() } ?: DietProfile()
        engine.ensureWeek(PlannerState(family, selected, Meal.from(root.optString("meal")) ?: Meal.DINNER, plans,
            root.optJSONObject("checked").stringLists().mapValues { it.value.toSet() },
            root.optJSONObject("done").stringLists().mapValues { it.value.toSet() },
            root.optJSONArray("favorites")?.strings()?.filter { it in engine.recipes }?.toSet().orEmpty(),
            root.optJSONArray("reminders")?.strings()?.toSet().orEmpty(), timer, shopping, voice, diet), today)
    }.getOrElse { engine.ensureWeek(PlannerState(date = today), today) }

    fun save(state: PlannerState) {
        val profile = state.profile
        val root = JSONObject().put("profile", JSONObject().put("name", profile.name).put("people", profile.people)
            .put("lunch", profile.lunch.toString()).put("dinner", profile.dinner.toString()))
            .put("date", state.date.toString()).put("meal", state.meal.value)
            .put("plans", jsonLists(state.plans)).put("checked", jsonLists(state.checked)).put("done", jsonLists(state.done))
            .put("favorites", JSONArray(state.favorites.toList())).put("reminders", JSONArray(state.reminders.toList()))
            .put("shopping", JSONObject().put("enabled", state.shopping.enabled).put("time", state.shopping.time.toString()))
            .put("voice", JSONObject().put("address_as", state.voice.addressAs).put("read_aloud", state.voice.readAloud)
                .put("rate", state.voice.rate.toDouble()))
            .put("diet", dietJson(state.diet))
        state.timer?.let { timer -> root.put("timer", JSONObject().put("end", timer.end).put("label", timer.label)
            .put("date", timer.date.toString()).put("meal", timer.meal.value)) }
        // Called on the ViewModel's serial IO worker, so confirmed state survives process termination.
        check(preferences.edit().putString("state", root.toString()).commit()) { "Could not save planner state" }
    }

    companion object {
        fun alarmJson(alarms: List<NativeAlarm>): String = JSONArray().apply {
            alarms.forEach { alarm -> put(JSONObject().put("id", alarm.id).put("at", alarm.at)
                .put("title", alarm.title).put("body", alarm.body).put("date", alarm.date.toString())
                .put("meal", alarm.meal.value).put("timer", alarm.timer).put("kind", alarm.kind.value)) }
        }.toString()
    }
}

/** Shared by storage and the assistant snapshot. */
internal fun dietJson(diet: DietProfile): JSONObject = JSONObject()
    .put("tags", JSONArray(diet.tags.sorted())).put("allergens", JSONArray(diet.allergens.sorted()))
    .put("avoid", JSONArray(diet.avoid.sorted()))
    .put("max_minutes", diet.maxMinutes ?: JSONObject.NULL).put("max_cost", diet.maxCost ?: JSONObject.NULL)

/** Defensive parse: unknown tags, allergens and ingredients are dropped, limits clamped. */
internal fun dietFrom(json: JSONObject, engine: PlannerEngine): DietProfile {
    val known = engine.recipes.values.flatMap { it.ingredients }.map { it.name }.toSet()
    return DietProfile(
        tags = json.optJSONArray("tags")?.strings()?.filter { it in Recipe.TAGS }?.toSet().orEmpty(),
        allergens = json.optJSONArray("allergens")?.strings()?.filter { it in Recipe.ALLERGENS }?.toSet().orEmpty(),
        avoid = json.optJSONArray("avoid")?.strings()?.filter { it in known }?.toSet().orEmpty(),
        maxMinutes = if (json.isNull("max_minutes")) null else json.optInt("max_minutes").takeIf { it in 10..240 },
        maxCost = if (json.isNull("max_cost")) null else json.optLong("max_cost").takeIf { it in 1_000..10_000_000 },
    )
}

private fun JSONArray.objects() =(0 until length()).map { getJSONObject(it) }
private fun JSONArray.strings() = (0 until length()).map { getString(it) }
private fun JSONObject?.stringLists(): Map<String, List<String>> = this?.let { objectValue ->
    objectValue.keys().asSequence().mapNotNull { key ->
        objectValue.optJSONArray(key)?.let { key to it.strings() }
    }.toMap()
}.orEmpty()
private fun jsonLists(map: Map<String, Collection<String>>) = JSONObject().apply {
    map.forEach { (key, values) -> put(key, JSONArray(values.toList())) }
}
