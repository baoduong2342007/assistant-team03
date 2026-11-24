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
                    "main" -> RecipeKind.MAIN
                    "soup" -> RecipeKind.SOUP
                    "veg" -> RecipeKind.VEG
                    "breakfast" -> RecipeKind.BREAKFAST
                    else -> error("Unknown recipe kind for $id")
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
            return PlannerEngine(recipes, (0 until choices.length()).map { choices.getJSONArray(it).strings() },
                root.optJSONArray("breakfasts")?.strings().orEmpty())
        }
    }
}

/** Native persistence; the Android app never reads a web page or browser localStorage. */
class PlannerStorage(context: Context, private val engine: PlannerEngine) {
    private val preferences = context.getSharedPreferences("bep_nha_planner_v2", Context.MODE_PRIVATE)
    private val welcome = context.getSharedPreferences(WELCOME_PREFS, Context.MODE_PRIVATE)

    /**
     * The first-run welcome is shown until the family finishes it. Families who used the app before the welcome existed
     * skip it: that is decided once, on the first launch with this version, because the app saves state as soon as it opens.
     */
    fun welcomeSeen(): Boolean {
        if (!welcome.contains("seen")) welcome.edit().putBoolean("seen", preferences.contains("state")).commit()
        return welcome.getBoolean("seen", false)
    }
    fun markWelcomeSeen() { welcome.edit().putBoolean("seen", true).apply() }

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
        val shopping = root.optJSONObject("shopping")?.let { item -> ShoppingReminder(item.optBoolean("enabled", false),
            runCatching { LocalTime.parse(item.optString("time", "07:00")) }.getOrDefault(LocalTime.of(7, 0)))
        } ?: ShoppingReminder()
        val voice = root.optJSONObject("voice")?.let { item -> VoiceSettings(VoiceSettings.clean(item.optString("address_as", "bạn")),
            item.optBoolean("read_aloud", true), item.optDouble("rate", 0.85).toFloat().takeIf { it in 0.5f..1.5f } ?: 0.85f)
        } ?: VoiceSettings()
        val diet = root.optJSONObject("diet")?.let { runCatching { dietFrom(it, engine) }.getOrNull() } ?: DietProfile()
        // Hand-added shopping items; past days are dropped so the store does not grow forever.
        val extras = root.optJSONObject("extras")?.let { json ->
            json.keys().asSequence().mapNotNull { key ->
                val day = runCatching { LocalDate.parse(key.substringBefore('|')) }.getOrNull() ?: return@mapNotNull null
                if (day < today) return@mapNotNull null
                key to json.optJSONArray(key)?.objects().orEmpty().mapNotNull { item ->
                    runCatching { Ingredient(item.getString("name").take(60), item.getDouble("amount"),
                        item.getString("unit").take(20), item.getString("group").take(40)) }.getOrNull()
                }
            }.filter { it.second.isNotEmpty() }.toMap()
        }.orEmpty()
        engine.ensureWeek(PlannerState(family, selected, Meal.from(root.optString("meal")) ?: Meal.DINNER, plans,
            root.optJSONObject("checked").stringLists().mapValues { it.value.toSet() },
            root.optJSONObject("done").stringLists().mapValues { it.value.toSet() },
            root.optJSONArray("favorites")?.strings()?.filter { it in engine.recipes }?.toSet().orEmpty(),
            root.optJSONArray("reminders")?.strings()?.toSet().orEmpty(), timers, shopMeals, shopping, voice, diet, extras), today)
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
            .put("shopping", JSONObject().put("enabled", state.shopping.enabled).put("time", state.shopping.time.toString()))
            .put("voice", JSONObject().put("address_as", state.voice.addressAs).put("read_aloud", state.voice.readAloud)
                .put("rate", state.voice.rate.toDouble()))
            .put("diet", dietJson(state.diet))
            .put("extras", JSONObject().apply {
                state.extras.forEach { (key, items) -> put(key, JSONArray().apply {
                    items.forEach { put(JSONObject().put("name", it.name).put("amount", it.amount).put("unit", it.unit).put("group", it.group)) }
                }) }
            })
        // Called on the ViewModel's serial IO worker, so confirmed state survives process termination.
        check(preferences.edit().putString("state", root.toString()).commit()) { "Could not save planner state" }
    }

    companion object {
        /** Separate from the planner state so clearing a plan never replays the welcome. */
        const val WELCOME_PREFS = "bep_nha_welcome"
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
