package vn.team03.bepnha

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

/** SDK-neutral schema so tool definitions can be unit-tested and converted for any provider. */
data class ToolParam(
    val type: String,
    val description: String? = null,
    val values: List<String>? = null,
    val items: ToolParam? = null,
    val properties: Map<String, ToolParam> = emptyMap(),
    val optional: List<String> = emptyList(),
    val maxItems: Int? = null,
)

/** [terminal] tools end the turn with a proposal; the others return data to the model. */
data class ToolSpec(
    val name: String,
    val description: String,
    val parameters: Map<String, ToolParam>,
    val optional: List<String> = emptyList(),
    val terminal: Boolean = false,
)

object AgentTools {
    private val date = ToolParam("string", "Ngày YYYY-MM-DD trong thực đơn bảy ngày")
    private val meal = ToolParam("enum", values = Meal.entries.map { it.value })
    private val nameList = ToolParam("array", items = ToolParam("string"), maxItems = 30)

    private val actionFields = linkedMapOf(
        "type" to ToolParam("enum", values = AssistantActionType.entries.map { it.name }),
        "date" to ToolParam("string", "Ngày YYYY-MM-DD; bỏ trống để dùng bữa đang chọn"),
        "meal" to ToolParam("enum", values = Meal.entries.map { it.value }),
        "screen" to ToolParam("enum", values = Screen.entries.map { it.name }),
        "recipe_id" to ToolParam("string", "ID công thức lấy từ catalog hoặc search_recipes"),
        "index" to ToolParam("integer", "Bữa sáng: 0. Bữa trưa và tối: 0=món mặn, 1=món canh, 2=món rau"),
        "people" to ToolParam("integer", "Số người từ 1 đến 12"),
        "time" to ToolParam("string", "Giờ ăn HH:mm"),
        "enabled" to ToolParam("boolean", "Trạng thái đích"),
        "minutes" to ToolParam("integer", "Số phút từ 1 đến 180"),
        "label" to ToolParam("string", "Tên ngắn cho hẹn giờ"),
        "key" to ToolParam("string", "Key chính xác của nguyên liệu hoặc bước nấu; set_diet_tag: tag; set_avoid: tên nguyên liệu hoặc chất dị ứng (tôm, cá, trứng...)"),
        "option" to ToolParam("string", "open_sheet: swap_options|reminder; share_shopping_list: meal|day; request_permission: notifications|exact_alarms"),
        "name" to ToolParam("string", "Tên gia đình, tối đa 35 ký tự"),
        "cost" to ToolParam("integer", "set_meal_limits: tiền chợ tối đa mỗi bữa (VND), 0 để bỏ giới hạn"),
    )

    val specs: List<ToolSpec> = listOf(
        ToolSpec("get_meals", "Xem thực đơn các bữa trong khoảng ngày: tên món, giá ước tính, thời gian nấu, nhắc nấu.",
            mapOf("from" to date, "to" to date), optional = listOf("from", "to")),
        ToolSpec("search_recipes", "Tìm công thức trong kho theo tên, nhóm món, nguyên liệu, thời gian và giá. Chỉ dùng món có trong kết quả.",
            mapOf(
                "query" to ToolParam("string", "Tên món hoặc từ khóa"),
                "kind" to ToolParam("enum", values = RecipeKind.entries.map { it.name.lowercase() }),
                "include_ingredients" to nameList, "exclude_ingredients" to nameList,
                "max_minutes" to ToolParam("integer"), "max_cost" to ToolParam("integer", "VND cho 4 người"),
                "tags" to ToolParam("array", "Yêu cầu thêm: low_salt, low_sugar, soft, low_fat, vegetarian", items = ToolParam("enum", values = Recipe.TAGS.toList()), maxItems = 5),
                "ignore_profile" to ToolParam("boolean", "true để xem cả món không hợp hồ sơ ăn uống (chỉ khi người dùng hỏi đích danh)"),
            ), optional = listOf("query", "kind", "include_ingredients", "exclude_ingredients", "max_minutes", "max_cost", "tags", "ignore_profile")),
        ToolSpec("get_recipe", "Xem đầy đủ một công thức: nguyên liệu theo khẩu phần gia đình, các bước và mẹo.",
            mapOf("recipe_id" to ToolParam("string"))),
        ToolSpec("get_shopping_list", "Xem danh sách đi chợ của một bữa hoặc cả ngày, gồm key và trạng thái đã có.",
            mapOf("date" to date, "meal" to meal), optional = listOf("meal")),
        ToolSpec("get_cooking_plan", "Xem lịch nấu của một bữa: các bước, giờ bắt đầu, key và trạng thái đã xong.",
            mapOf("date" to date, "meal" to meal)),
        ToolSpec("propose_actions", "Đề xuất thao tác trong app. Việc nhỏ được làm ngay và có Hoàn tác; đổi món, khẩu phần, giờ ăn, tên gia đình cần người dùng xác nhận.",
            mapOf("reply" to ToolParam("string", "Một câu ngắn cho người dùng"),
                "actions" to ToolParam("array", items = ToolParam("object", properties = actionFields, optional = actionFields.keys.drop(1)), maxItems = AssistantAction.MAX_ACTIONS)),
            terminal = true),
        ToolSpec("plan_meals", "Lên thực đơn cho nhiều bữa/ngày. App tự chọn mâm mặn-canh-rau hợp lệ theo ràng buộc; người dùng xem trước rồi xác nhận.",
            mapOf("from" to date, "to" to date,
                "meals" to ToolParam("array", items = meal, maxItems = 3),
                "avoid_ingredients" to nameList, "prefer_ingredients" to nameList,
                "max_minutes" to ToolParam("integer", "Thời gian nấu tối đa mỗi bữa"),
                "max_cost" to ToolParam("integer", "Ngân sách tối đa mỗi bữa (VND) cho số người hiện tại"),
                "reply" to ToolParam("string")),
            optional = listOf("meals", "avoid_ingredients", "prefer_ingredients", "max_minutes", "max_cost", "reply"), terminal = true),
        ToolSpec("suggest_meals", "Gợi ý tối đa hai mâm cho một bữa từ nguyên liệu người dùng đang có; người dùng chọn mâm.",
            mapOf("date" to date, "meal" to meal, "pantry_items" to nameList,
                "exclude_ingredients" to nameList, "pantry_only" to ToolParam("boolean")),
            optional = listOf("exclude_ingredients", "pantry_only"), terminal = true),
    )
    val terminal: Set<String> = specs.filter { it.terminal }.map { it.name }.toSet()
    val toolNames: Set<String> = specs.map { it.name }.toSet()

    /** Short catalog index sent each turn; details come from get_recipe. */
    fun catalogIndex(engine: PlannerEngine) = JSONArray(engine.recipes.values.map { recipe ->
        JSONObject().put("id", recipe.id).put("name", recipe.name).put("kind", kindName(recipe.kind))
            .put("minutes", recipe.minutes).put("cost", recipe.cost).put("tags", JSONArray(recipe.tags.sorted()))
    })

    fun kindName(kind: RecipeKind) = kind.name.lowercase()
}

/** Runs read-only tools against a planner state. Never changes state. */
class AgentReadTools(private val engine: PlannerEngine, private val rules: MealPlanning) {
    fun run(name: String, args: JSONObject, state: PlannerState, today: LocalDate): JSONObject = when (name) {
        "get_meals" -> {
            val from = args.optDate("from") ?: today; val to = args.optDate("to") ?: today.plusDays(6)
            JSONObject().put("meals", JSONArray(state.plans.keys.sorted().mapNotNull { key ->
                val day = LocalDate.parse(key.substringBefore('|')); val meal = Meal.from(key.substringAfter('|')) ?: return@mapNotNull null
                if (day < from || day > to || day !in today..today.plusDays(6)) return@mapNotNull null
                val menu = engine.menu(state, day, meal)
                JSONObject().put("date", day.toString()).put("meal", meal.value)
                    .put("dishes", JSONArray(menu.map { JSONObject().put("id", it.id).put("name", it.name).put("kind", AgentTools.kindName(it.kind)) }))
                    .put("cost", engine.cost(menu, state.profile.people)).put("minutes", engine.minutes(menu))
                    .put("reminder", key in state.reminders)
            }))
        }
        "search_recipes" -> {
            val query = args.optString("query").takeIf { it.isNotBlank() }?.let(MealPlanning::normalize)
            val kind = args.optString("kind").takeIf { it.isNotBlank() }
            val include = rules.ingredientNames(args.optStrings("include_ingredients"))
            val exclude = rules.ingredientNames(args.optStrings("exclude_ingredients"))
            val maxMinutes = args.optPositiveInt("max_minutes"); val maxCost = args.optPositiveInt("max_cost")
            val tags = args.optStrings("tags").filter { it in Recipe.TAGS }.toSet()
            val diet = if (args.optBoolean("ignore_profile")) DietProfile() else state.diet
            val found = engine.recipes.values.filter { recipe ->
                val ingredients = recipe.ingredients.map { it.name }
                (kind == null || AgentTools.kindName(recipe.kind) == kind) &&
                    (query == null || MealPlanning.normalize(recipe.name).contains(query) || ingredients.any { MealPlanning.normalize(it).contains(query) }) &&
                    include.all { it in ingredients } && exclude.none { it in ingredients } &&
                    (maxMinutes == null || recipe.minutes <= maxMinutes) && (maxCost == null || recipe.cost <= maxCost) &&
                    recipe.tags.containsAll(tags) && diet.allows(recipe)
            }.sortedBy { it.minutes }.take(12)
            JSONObject().put("recipes", JSONArray(found.map { recipe ->
                JSONObject().put("id", recipe.id).put("name", recipe.name).put("kind", AgentTools.kindName(recipe.kind))
                    .put("minutes", recipe.minutes).put("cost", recipe.cost).put("ingredients", JSONArray(recipe.ingredients.map { it.name }))
                    .put("tags", JSONArray(recipe.tags.sorted())).put("fits_profile", state.diet.allows(recipe))
            })).put("note", if (found.isEmpty()) "Không có món nào khớp" + (if (!diet.isEmpty) " với hồ sơ ăn uống (${diet.describe()})" else "") + ". Không được bịa món mới." else JSONObject.NULL)
        }
        "get_recipe" -> {
            val recipe = engine.recipes[args.getString("recipe_id")] ?: throw IllegalArgumentException("Không có công thức ${args.getString("recipe_id")}.")
            JSONObject().put("id", recipe.id).put("name", recipe.name).put("kind", AgentTools.kindName(recipe.kind))
                .put("minutes", recipe.minutes).put("cost_for_family", (recipe.cost * state.profile.people / 4.0).toLong())
                .put("tags", JSONArray(recipe.tags.sorted())).put("allergens", JSONArray(recipe.allergens.sorted()))
                .put("fits_profile", state.diet.allows(recipe)).put("profile_conflicts", JSONArray(state.diet.reasons(recipe)))
                .put("ingredients", JSONArray(recipe.ingredients.map {
                    JSONObject().put("name", it.name).put("amount", it.amount * state.profile.people / 4.0).put("unit", it.unit)
                })).put("steps", JSONArray(recipe.steps)).put("tip", recipe.tip)
        }
        "get_shopping_list" -> {
            val day = requireWindow(args.getString("date"), today)
            val meals = args.optString("meal").takeIf { it.isNotBlank() }?.let { listOf(requireNotNull(Meal.from(it)) { "meal phải là breakfast, lunch hoặc dinner." }) } ?: engine.meals
            JSONObject().put("date", day.toString()).put("items", JSONArray(meals.flatMap { meal ->
                engine.ingredients(engine.menu(state, day, meal), state.profile.people).map {
                    JSONObject().put("meal", meal.value).put("key", it.key).put("name", it.name)
                        .put("amount", it.amount).put("unit", it.unit).put("checked", it.key in engine.checkedFor(state, day, meal))
                }
            }))
        }
        "get_cooking_plan" -> {
            val day = requireWindow(args.getString("date"), today)
            val meal = requireNotNull(Meal.from(args.getString("meal"))) { "meal phải là breakfast, lunch hoặc dinner." }
            val key = PlannerEngine.key(day, meal)
            JSONObject().put("serve_at", state.profile.time(meal).toString()).put("tasks", JSONArray(engine.timeline(state, day, meal).map {
                JSONObject().put("key", it.key).put("title", it.title).put("time", PlannerEngine.time(it.at))
                    .put("minutes", it.minutes).put("done", it.key in state.done[key].orEmpty())
            }))
        }
        else -> throw IllegalArgumentException("Không có công cụ \"$name\". Các công cụ hợp lệ: ${AgentTools.toolNames.joinToString()}.")
    }

    private fun requireWindow(value: String, today: LocalDate): LocalDate = MealPlanning.parseDay(value).also {
        require(it in today..today.plusDays(6)) { "Ngày $it nằm ngoài thực đơn bảy ngày (từ $today đến ${today.plusDays(6)})." }
    }
}

internal fun JSONObject.optDate(key: String): LocalDate? = optString(key).takeIf { it.isNotBlank() }?.let(MealPlanning::parseDay)
internal fun JSONObject.optStrings(key: String): List<String> = optJSONArray(key)?.let { list ->
    (0 until list.length()).mapNotNull { list.opt(it) as? String }.filter { it.isNotBlank() }.take(30)
}.orEmpty()
internal fun JSONObject.optPositiveInt(key: String): Int? = if (has(key) && !isNull(key)) wholeNumber(get(key))?.takeIf { it > 0 } ?: throw IllegalArgumentException("$key phải là số nguyên dương.") else null

/** Integers may arrive as 5 or 5.0 from the model; strings and fractions are rejected. */
internal fun wholeNumber(value: Any?): Int? = when (value) {
    is Int -> value
    is Long -> value.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
    is Double -> value.takeIf { it % 1.0 == 0.0 && it in Int.MIN_VALUE.toDouble()..Int.MAX_VALUE.toDouble() }?.toInt()
    else -> null
}
