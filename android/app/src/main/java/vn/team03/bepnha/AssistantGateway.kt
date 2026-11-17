package vn.team03.bepnha

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.LocalTime

enum class AssistantSource { AI, LOCAL, SYSTEM }

/** UI metadata is deliberately excluded from the model payload. */
data class AssistantChatMessage(
    val role: String,
    val text: String,
    val recipeIds: List<String> = emptyList(),
    val id: String = java.util.UUID.randomUUID().toString(),
    val source: AssistantSource = AssistantSource.AI,
    val notice: String? = null,
    val at: Long = System.currentTimeMillis(),
)
data class AssistantMealSuggestion(
    val id: Int,
    val date: LocalDate,
    val meal: Meal,
    val recipeIds: List<String>,
    val pantryKeys: Set<String>,
)

data class AssistantReply(
    val text: String,
    val actions: List<AssistantAction>,
    val suggestions: List<AssistantMealSuggestion> = emptyList(),
    val source: AssistantSource = AssistantSource.AI,
    val notice: String? = null,
    val recipeIds: List<String> = emptyList(),
    val suggestionConstraints: PantryConstraints? = null,
)

data class AssistantProposal(
    val base: PlannerState,
    val actions: List<AssistantAction>,
    val descriptions: List<String>,
    /** True when at least one action is HIGH risk and must be confirmed. */
    val needsConfirmation: Boolean = true,
) {
    val canReviseMenu: Boolean
        get() {
            val first = actions.firstOrNull() ?: return false
            return actions.all { it.type == AssistantActionType.swap_recipe && it.targetsSameMealAs(first) }
        }
}

object AssistantGateway {
    /**
     * Compact snapshot for the model. Only the selected meal carries ingredients and tasks;
     * the agent reads other details with tools (get_shopping_list, get_cooking_plan, get_recipe).
     */
    fun snapshot(
        engine: PlannerEngine,
        state: PlannerState,
        today: LocalDate,
        screen: Screen,
        recipeId: String?,
        pendingMenuChanges: List<AssistantAction> = emptyList(),
        lastMenuChanges: List<AssistantAction> = emptyList(),
    ): JSONObject {
        val meals = JSONArray()
        state.plans.keys.sorted().forEach { key ->
            val parts = key.split('|'); val date = LocalDate.parse(parts[0]); val meal = Meal.from(parts[1]) ?: return@forEach
            if (date !in today..today.plusDays(6) || !engine.validPlan(state.plans.getValue(key))) return@forEach
            val menu = engine.menu(state, date, meal)
            val entry = JSONObject().put("date", date.toString()).put("meal", meal.value).put("recipes", JSONArray(menu.map { it.id }))
                .put("cost", engine.cost(menu, state.profile.people)).put("minutes", engine.minutes(menu))
                .put("reminder", key in state.reminders)
            if (key == state.key) {
                entry.put("ingredients", JSONArray(engine.ingredients(menu, state.profile.people).map { i ->
                    JSONObject().put("key", i.key).put("amount", i.amount).put("checked", i.key in state.checked[key].orEmpty()) }))
                entry.put("tasks", JSONArray(engine.timeline(state, date, meal).map { t ->
                    JSONObject().put("key", t.key).put("title", t.title).put("time", PlannerEngine.time(t.at)).put("done", t.key in state.done[key].orEmpty()) }))
            }
            meals.put(entry)
        }
        return JSONObject().put("today", today.toString()).put("timezone", PlannerEngine.zone.id)
            .put("date", state.date.toString()).put("meal", state.meal.value).put("screen", screen.name).put("recipe_id", recipeId ?: JSONObject.NULL)
            .put("people", state.profile.people).put("lunch", state.profile.lunch.toString()).put("dinner", state.profile.dinner.toString())
            .put("address_as", state.voice.addressAs)
            .put("diet", dietJson(state.diet).put("summary", state.diet.describe()))
            .put("meals", meals).put("favorites", JSONArray(state.favorites.toList()))
            .put("timer", state.timer?.let { JSONObject().put("label", it.label).put("end", it.end).put("date", it.date.toString()).put("meal", it.meal.value) } ?: JSONObject.NULL)
            .apply {
                if (pendingMenuChanges.isNotEmpty()) put("pending_menu_changes", menuChangesJson(pendingMenuChanges))
                if (lastMenuChanges.isNotEmpty()) put("last_menu_changes", menuChangesJson(lastMenuChanges))
            }
    }

    /**
     * Rebuild the planner state the snapshot describes, so agent proposals can be validated
     * (and errors returned to the model) before they reach the ViewModel.
     */
    fun stateFrom(engine: PlannerEngine, context: JSONObject): PlannerState {
        val meals = context.getJSONArray("meals")
        val plans = mutableMapOf<String, List<String>>()
        val checked = mutableMapOf<String, Set<String>>()
        val done = mutableMapOf<String, Set<String>>()
        val reminders = mutableSetOf<String>()
        for (i in 0 until meals.length()) {
            val entry = meals.getJSONObject(i)
            val key = PlannerEngine.key(LocalDate.parse(entry.getString("date")), requireNotNull(Meal.from(entry.getString("meal"))))
            plans[key] = entry.getJSONArray("recipes").let { ids -> (0 until ids.length()).map(ids::getString) }
            if (entry.optBoolean("reminder")) reminders += key
            entry.optJSONArray("ingredients")?.let { items ->
                checked[key] = (0 until items.length()).map(items::getJSONObject).filter { it.optBoolean("checked") }.map { it.getString("key") }.toSet()
            }
            entry.optJSONArray("tasks")?.let { items ->
                done[key] = (0 until items.length()).map(items::getJSONObject).filter { it.optBoolean("done") }.map { it.getString("key") }.toSet()
            }
        }
        val timer = context.optJSONObject("timer")?.let {
            CookingTimer(it.getLong("end"), it.getString("label"), LocalDate.parse(it.getString("date")), requireNotNull(Meal.from(it.getString("meal"))))
        }
        val favorites = context.optJSONArray("favorites")?.let { ids -> (0 until ids.length()).map(ids::getString).filter { it in engine.recipes }.toSet() }.orEmpty()
        return PlannerState(
            profile = FamilyProfile(people = context.getInt("people"), lunch = LocalTime.parse(context.getString("lunch")), dinner = LocalTime.parse(context.getString("dinner"))),
            date = LocalDate.parse(context.getString("date")), meal = requireNotNull(Meal.from(context.getString("meal"))),
            plans = plans, checked = checked, done = done, favorites = favorites, reminders = reminders, timer = timer,
            diet = context.optJSONObject("diet")?.let { dietFrom(it, engine) } ?: DietProfile(),
        )
    }

    private fun menuChangesJson(changes: List<AssistantAction>) = JSONArray(changes.map { action ->
        JSONObject().put("date", action.date.toString()).put("meal", action.meal.value)
            .put("index", action.index).put("recipe_id", action.recipeId)
    })

    internal fun failureMessage(status: Int, retryAfterSeconds: Long? = null): String = when (status) {
        429 -> {
            val seconds = retryAfterSeconds?.takeIf { it in 1L..604800L }
            val wait = when {
                seconds == null -> "Hãy kiểm tra hạn mức hoặc thử lại sau."
                seconds >= 3600L -> "Dịch vụ yêu cầu chờ khoảng ${(seconds + 3599) / 3600} giờ trước khi thử lại."
                else -> "Dịch vụ yêu cầu chờ khoảng ${(seconds + 59) / 60} phút trước khi thử lại."
            }
            "AI đang đạt giới hạn yêu cầu hoặc hạn mức. $wait Bạn vẫn có thể tìm trong công thức có sẵn."
        }
        504 -> "AI phản hồi quá chậm. Hãy thử lại hoặc tìm trong công thức có sẵn."
        502 -> "Phản hồi chưa hợp lệ. Hãy thử diễn đạt rõ ngày, bữa và món."
        413 -> "Ngữ cảnh bữa ăn quá lớn. Hãy đóng trợ lý, mở lại rồi gửi câu hỏi."
        422 -> "Câu hỏi hoặc ngữ cảnh chưa hợp lệ. Hãy chọn lại bữa rồi gửi câu hỏi."
        else -> "AI chưa sẵn sàng. Hãy thử lại hoặc tìm trong công thức có sẵn."
    }

    /** Parse a serialized reply (`reply`, `actions`, optional `suggestions`); used by device tests. */
    internal fun parseReply(result: JSONObject): AssistantReply {
        val actions = result.getJSONArray("actions")
        val suggestions = if (result.has("suggestions")) result.getJSONArray("suggestions") else JSONArray()
        require(actions.length() <= AssistantAction.MAX_ACTIONS && suggestions.length() <= 2)
        require(actions.length() == 0 || suggestions.length() == 0)
        return AssistantReply(
            result.getString("reply"),
            (0 until actions.length()).map { parseAction(actions.getJSONObject(it)) },
            (0 until suggestions.length()).map { index ->
                val suggestion = suggestions.getJSONObject(index)
                require(wholeNumber(suggestion.get("id")) == index + 1)
                val recipes = suggestion.getJSONArray("recipe_ids")
                val pantry = suggestion.getJSONArray("pantry_keys")
                require(recipes.length() == 3 && pantry.length() in 1..60)
                AssistantMealSuggestion(
                    id = index + 1,
                    date = LocalDate.parse(suggestion.getString("date")),
                    meal = requireNotNull(Meal.from(suggestion.getString("meal"))),
                    recipeIds = (0 until recipes.length()).map { recipes.getString(it) },
                    pantryKeys = (0 until pantry.length()).map { pantry.getString(it) }.toSet(),
                )
            },
        )
    }

    /** Fields each action type accepts besides type/date/meal. Required unless listed in [OPTIONAL_FIELDS]. */
    val ACTION_FIELDS: Map<AssistantActionType, Set<String>> = mapOf(
        AssistantActionType.navigate to setOf("screen"),
        AssistantActionType.open_recipe to setOf("recipe_id"),
        AssistantActionType.swap_recipe to setOf("recipe_id", "index"),
        AssistantActionType.set_people to setOf("people"),
        AssistantActionType.set_meal_time to setOf("time"),
        AssistantActionType.set_reminder to setOf("enabled"),
        AssistantActionType.start_timer to setOf("minutes", "label"),
        AssistantActionType.cancel_timer to emptySet(),
        AssistantActionType.set_ingredient to setOf("key", "enabled"),
        AssistantActionType.set_task to setOf("key", "enabled"),
        AssistantActionType.set_favorite to setOf("recipe_id", "enabled"),
        AssistantActionType.select_meal to emptySet(),
        AssistantActionType.open_sheet to setOf("option", "index"),
        AssistantActionType.close_assistant to emptySet(),
        AssistantActionType.share_shopping_list to setOf("option"),
        AssistantActionType.add_to_calendar to emptySet(),
        AssistantActionType.request_permission to setOf("option"),
        AssistantActionType.set_family_name to setOf("name"),
        AssistantActionType.set_diet_tag to setOf("key", "enabled"),
        AssistantActionType.set_avoid to setOf("key", "enabled"),
        AssistantActionType.set_meal_limits to setOf("minutes", "cost"),
        AssistantActionType.start_cook_along to emptySet(),
    )
    private val OPTIONAL_FIELDS = mapOf(AssistantActionType.open_sheet to setOf("index"), AssistantActionType.share_shopping_list to setOf("option"),
        AssistantActionType.set_meal_limits to setOf("minutes", "cost"))

    /**
     * Parse one action. Global actions and actions without an explicit target use the
     * currently selected meal ([defaultDate], [defaultMeal]). Errors are in Vietnamese and
     * precise enough to send back to the model for self-correction.
     */
    fun parseAction(o: JSONObject, defaultDate: LocalDate? = null, defaultMeal: Meal? = null): AssistantAction {
        fun text(key: String) = if (!o.has(key) || o.isNull(key)) null else (o.get(key) as? String ?: throw IllegalArgumentException("$key phải là chuỗi."))
        fun number(key: String): Int? {
            if (!o.has(key) || o.isNull(key)) return null
            return wholeNumber(o.get(key)) ?: throw IllegalArgumentException("$key phải là số nguyên.")
        }
        val typeName = o.optString("type")
        val type = AssistantActionType.entries.firstOrNull { it.name == typeName }
            ?: throw IllegalArgumentException("Không có loại thao tác \"$typeName\".")
        val fields = ACTION_FIELDS.getValue(type)
        val unknown = o.keys().asSequence().filter { it !in fields + setOf("type", "date", "meal") && !o.isNull(it) }.toList()
        require(unknown.isEmpty()) { "Thao tác $typeName không nhận tham số ${unknown.joinToString()}." }
        val missing = (fields - OPTIONAL_FIELDS[type].orEmpty()).filter { !o.has(it) || o.isNull(it) }
        require(missing.isEmpty()) { "Thao tác $typeName thiếu tham số ${missing.joinToString()}." }
        val date = text("date")?.let(MealPlanning::parseDay) ?: requireNotNull(defaultDate) { "Thao tác $typeName thiếu date." }
        val meal = text("meal")?.let { requireNotNull(Meal.from(it)) { "meal phải là lunch hoặc dinner." } }
            ?: requireNotNull(defaultMeal) { "Thao tác $typeName thiếu meal." }
        return AssistantAction(type, date, meal,
            text("screen")?.let { name -> Screen.entries.firstOrNull { it.name == name } ?: throw IllegalArgumentException("Không có màn hình $name.") },
            text("recipe_id"), number("index"), number("people"),
            text("time")?.let { require(Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(it)) { "time phải có dạng HH:mm." }; LocalTime.parse(it) },
            if (o.has("enabled") && !o.isNull("enabled")) o.get("enabled") as? Boolean ?: throw IllegalArgumentException("enabled phải là true/false.") else null,
            number("minutes"), text("label"), text("key"),
            text("option") ?: if (type == AssistantActionType.share_shopping_list) "meal" else null,
            text("name"), number("cost"))
    }
}
