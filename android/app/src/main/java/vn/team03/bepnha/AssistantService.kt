package vn.team03.bepnha

import kotlinx.coroutines.CancellationException
import org.json.JSONArray
import org.json.JSONObject

data class AssistantToolCall(val name: String, val arguments: JSONObject)
data class AssistantModelReply(val text: String? = null, val calls: List<AssistantToolCall> = emptyList())
fun interface AssistantProvider { suspend fun generate(payload: JSONObject): AssistantModelReply }

enum class AssistantFailureKind(val localFallback: Boolean, val explanation: String) {
    UNCONFIGURED(true, "AI chưa được kết nối. Đang dùng trợ lý trên máy."),
    NETWORK(true, "Chưa kết nối được AI. Đang dùng trợ lý trên máy."),
    TIMEOUT(true, "AI phản hồi quá chậm. Đang dùng trợ lý trên máy."),
    QUOTA(true, "AI đang hết hạn mức. Đang dùng trợ lý trên máy; bạn có thể thử AI lại sau."),
    UNAVAILABLE(true, "AI tạm thời chưa sẵn sàng. Đang dùng trợ lý trên máy."),
    CONFIGURATION(false, "Chưa xác thực được dịch vụ AI. Nhóm cần kiểm tra cấu hình Firebase, model và đăng ký thiết bị App Check."),
    INVALID(false, "Phản hồi trợ lý chưa hợp lệ. Hãy thử diễn đạt rõ ngày, bữa và món."),
    BLOCKED(false, "AI chưa thể trả lời yêu cầu này. Bạn có thể diễn đạt lại hoặc tra cứu công thức trên máy."),
}
class AssistantFailure(val kind: AssistantFailureKind) : IllegalStateException(kind.explanation)

/** One turn: scope -> provider/local -> semantic validation. Never commits planner state. */
class AssistantService(private val engine: PlannerEngine, private val provider: AssistantProvider?) {
    private val rules = MealPlanning(engine.recipes)

    suspend fun ask(question: String, history: List<AssistantChatMessage>, context: JSONObject): AssistantReply {
        require(question.isNotBlank() && question.length <= 1000 && context.toString().length <= 100_000)
        val recent = history.takeLast(20)
        val scope = rules.scope(question, context, rules.previousQuestion(recent))
        rules.clarification(question, recent, context, scope)?.let { return AssistantReply(it, emptyList(), source = AssistantSource.LOCAL) }
        if (MealPlanning.isComparison(question)) compare(context)?.let { return it }
        val payload = JSONObject().put("question", question).put("context", context)
            .put("history", JSONArray(recent.map { JSONObject().put("role", it.role).put("text", it.text) }))
            .put("catalog", catalog())
            .put("menu_scope", JSONObject().put("date", scope.day?.toString() ?: JSONObject.NULL)
                .put("meal", scope.meal?.value ?: JSONObject.NULL).put("clarification", scope.clarification ?: JSONObject.NULL))
        val answer = try {
            val response = (provider ?: throw AssistantFailure(AssistantFailureKind.UNCONFIGURED)).generate(payload)
            decode(response, question, recent, context, scope)
        } catch (e: CancellationException) { throw e
        } catch (e: AssistantFailure) {
            if (!e.kind.localFallback) throw e
            local(question, recent, context, scope).copy(source = AssistantSource.LOCAL, notice = e.kind.explanation)
        } catch (_: IllegalArgumentException) { throw AssistantFailure(AssistantFailureKind.INVALID)
        } catch (_: IllegalStateException) { throw AssistantFailure(AssistantFailureKind.INVALID)
        } catch (_: org.json.JSONException) { throw AssistantFailure(AssistantFailureKind.INVALID) }
        try { validate(answer, question, recent, context, scope) }
        catch (_: RuntimeException) { throw AssistantFailure(AssistantFailureKind.INVALID) }
        return if (answer.actions.any { it.mutates }) answer.copy(text = "Mình đề xuất các thao tác sau.") else answer
    }

    internal fun decode(response: AssistantModelReply, question: String, history: List<AssistantChatMessage>, context: JSONObject, scope: MenuScope): AssistantReply {
        require(response.calls.size <= 1)
        if (response.calls.isEmpty()) return AssistantReply(requireNotNull(response.text).also { require(it.isNotBlank() && it.length <= 8000) }, emptyList())
        val call = response.calls.single(); val args = call.arguments
        return when (call.name) {
            "propose_actions" -> {
                require(args.keys().asSequence().all { it in setOf("reply", "actions") })
                val text = args.strictText("reply", 8000, allowEmpty = true)
                val actions = args.getJSONArray("actions"); require(actions.length() <= 8)
                AssistantReply(text, (0 until actions.length()).map { AssistantGateway.parseAction(actions.getJSONObject(it)) })
            }
            "suggest_meals" -> {
                require(args.keys().asSequence().all { it in setOf("date", "meal", "pantry_items", "exclude_ingredients", "pantry_only") })
                checkTarget(MealPlanning.parseDay(args.strictText("date")), requireNotNull(Meal.from(args.strictText("meal"))), context, scope)
                val terms = args.stringList("pantry_items", 1, 30)
                val excluded = if (args.has("exclude_ingredients")) args.stringList("exclude_ingredients", 0, 30) else emptyList()
                val only = if (args.has("pantry_only")) args.get("pantry_only") as? Boolean ?: error("Expected boolean") else false
                val messages = userMessages(question, history, context)
                val grounded = rules.constraints(messages)
                // Constraints from the user's words are authoritative over model omissions.
                val pantry = rules.pantryNames(terms, messages)
                if (excluded.any { rules.ingredientNames(listOf(it)).isEmpty() } || grounded.unknownExclusion)
                    return AssistantReply("Mình chưa đối chiếu được nguyên liệu cần tránh với kho công thức. Bạn cho mình tên cụ thể nhé.", emptyList())
                suggest(scope, PantryConstraints(pantry, rules.ingredientNames(excluded) + grounded.excluded, only || grounded.only))
            }
            else -> throw AssistantFailure(AssistantFailureKind.INVALID)
        }
    }

    private fun userMessages(question: String, history: List<AssistantChatMessage>, context: JSONObject): List<String> {
        val prior = context.optJSONObject("suggestion_constraints")?.let {
            buildString {
                append("Tủ lạnh chỉ có ${it.stringList("pantry", 1, 60).joinToString(", ")}. ")
                val excluded = it.stringList("excluded", 0, 60)
                if (excluded.isNotEmpty()) append("Tránh ${excluded.joinToString(", ")}. ")
                if (it.getBoolean("only")) append("Không mua thêm.")
            }
        }
        return listOf(question) + listOfNotNull(prior) + history.filter { it.role == "user" }.takeLast(2).reversed().map { it.text }
    }

    private fun suggest(scope: MenuScope, constraints: PantryConstraints): AssistantReply {
        scope.clarification?.let { return AssistantReply(it, emptyList()) }
        if (constraints.unknownExclusion) return AssistantReply("Mình chưa nhận ra nguyên liệu cần tránh. Bạn cho mình tên cụ thể nhé.", emptyList())
        if (constraints.pantry.isEmpty()) return AssistantReply("Mình chưa nhận ra nguyên liệu nào trong kho công thức. Bạn cho mình tên nguyên liệu cụ thể nhé.", emptyList())
        val ranked = rules.rankMeals(constraints.pantry, constraints.excluded)
        if (ranked.isEmpty()) return AssistantReply("Mình chưa tìm được mâm có thể tận dụng nguyên liệu đã nêu và đáp ứng yêu cầu này.", emptyList())
        val options = ranked.filter { !constraints.only || it.missing.isEmpty() }.take(2)
        if (options.isEmpty()) return AssistantReply("Mâm phù hợp nhất vẫn cần thêm nguyên liệu như ${ranked.first().missing.take(3).joinToString()}. Bạn muốn xem lựa chọn có thể mua thêm không?", emptyList())
        return AssistantReply("Mình gợi ý mâm dưới đây theo nguyên liệu bạn có. Bạn có thể chọn mâm muốn áp dụng.", emptyList(),
            options.mapIndexed { index, item -> AssistantMealSuggestion(index + 1, requireNotNull(scope.day), requireNotNull(scope.meal), item.recipeIds, item.pantryKeys) },
            suggestionConstraints = constraints)
    }

    private fun local(question: String, history: List<AssistantChatMessage>, context: JSONObject, scope: MenuScope): AssistantReply {
        val request = "${rules.previousQuestion(history)} $question".trim()
        val text = MealPlanning.normalize(request)
        if (Regex("""\b(?:ngan sach|calo|dinh duong|di ung|an kieng|duoi \d+ phut)\b""").containsMatchIn(text))
            return AssistantReply("Mình chưa kiểm tra được ràng buộc ngân sách, dinh dưỡng, dị ứng hay thời gian tối đa. Bạn có muốn xem công thức để tự đối chiếu không?", emptyList())
        if (MealPlanning.menuRequest.containsMatchIn(text)) scope.clarification?.let { return AssistantReply(it, emptyList()) }
        val named = rules.requestedRecipes(request)
        if (named.isNotEmpty()) {
            // A local parser only proposes unambiguous positive requests.
            if (Regex("""\b(?:dung|khong|khoi) (?:doi|thay|muon)\b""").containsMatchIn(text))
                return AssistantReply("Mình giữ nguyên thực đơn. Bạn muốn chỉnh món nào khác?", emptyList())
            if (extraIntents.values.any { Regex("\\b(?:$it)\\b").containsMatchIn(text) })
                return AssistantReply("Yêu cầu này có thêm thao tác ngoài đổi món mà trợ lý trên máy chưa xử lý chắc chắn. Bạn hãy tách yêu cầu hoặc dùng các màn hình của ứng dụng.", emptyList())
            return AssistantReply("Mình đề xuất các thao tác sau.", named.map { (index, id) ->
                AssistantAction(AssistantActionType.swap_recipe, requireNotNull(scope.day), requireNotNull(scope.meal), recipeId = id, index = index)
            })
        }
        if (Regex("""\b(?:doi|thay)\b""").containsMatchIn(text))
            return AssistantReply("Bạn cho mình tên đầy đủ của món muốn thay vào nhé. Bạn cũng có thể chọn món trực tiếp trong Thực đơn.", emptyList())
        if (MealPlanning.menuRequest.containsMatchIn(text) || Regex("""\b(?:chi co|chi con|moi mua|tranh|khong dung|khong mua them)\b""").containsMatchIn(text))
            return suggest(scope, rules.constraints(userMessages(question, history, context)))
        val focused = engine.recipes[context.optString("recipe_id")]
        if (focused != null && "mon nay" in text) {
            val answer = if ("nguyen lieu" in text) focused.ingredients.joinToString("\n") {
                "${it.name}: ${PlannerEngine.quantity(it.amount * context.getInt("people") / 4.0)} ${it.unit}"
            } else focused.steps.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n\n")
            return AssistantReply("${focused.name}\n\n$answer", emptyList(), recipeIds = listOf(focused.id))
        }
        // Other mutations require the online interpreter or explicit app controls.
        if (Regex("""\b(?:nhac|hen gio|dong ho|khau phan|nguoi|danh dau|da mua|da xong|luu mon|mo)\b""").containsMatchIn(text))
            return AssistantReply("Trợ lý trên máy chưa xử lý chắc chắn thao tác này. Bạn có thể mở Thực đơn, Đi chợ, Vào bếp hoặc Gia đình để thao tác trực tiếp.", emptyList())
        val plans = context.getJSONArray("meals")
        val planMap = (0 until plans.length()).associate { i ->
            val meal = plans.getJSONObject(i)
            PlannerEngine.key(MealPlanning.parseDay(meal.getString("date")), requireNotNull(Meal.from(meal.getString("meal")))) to meal.stringList("recipes", 3, 3)
        }
        val state = PlannerState(date = MealPlanning.parseDay(context.getString("date")), meal = requireNotNull(Meal.from(context.getString("meal"))),
            profile = FamilyProfile(people = context.getInt("people"), lunch = java.time.LocalTime.parse(context.getString("lunch")), dinner = java.time.LocalTime.parse(context.getString("dinner"))), plans = planMap)
        val answer = engine.answer(question, state)
        return AssistantReply(answer.text, emptyList(), recipeIds = answer.recipes)
    }

    internal fun validate(answer: AssistantReply, question: String, history: List<AssistantChatMessage>, context: JSONObject, scope: MenuScope) {
        require(answer.actions.size <= 8 && answer.suggestions.size <= 2 && (answer.actions.isEmpty() || answer.suggestions.isEmpty()))
        val swaps = answer.actions.filter { it.type == AssistantActionType.swap_recipe }
        require(swaps.map { Triple(it.date, it.meal, it.index) }.distinct().size == swaps.size)
        val request = "${rules.previousQuestion(history)} $question"
        var wanted = rules.namedSlots(request, "(?<!dung )(?<!khong )(?<!khoi )(?:doi|thay)")
        if (history.lastOrNull()?.text == MealPlanning.DISH_QUESTION) wanted = wanted.ifEmpty { rules.namedSlots("doi $question", "doi") }
        val kept = rules.namedSlots(request, "giu(?: nguyen)?|(?:dung|khong|khoi) (?:doi|thay)")
        val named = rules.requestedRecipes(request)
        wanted = wanted.ifEmpty { named.keys }
        if (wanted.isEmpty() && MealPlanning.dishReference.containsMatchIn(MealPlanning.normalize(request))) wanted = setOfNotNull(rules.referencedSlot(context, scope))
        for (action in answer.actions) {
            checkTarget(action.date, action.meal, context, scope)
            if (action.recipeId != null) require(action.recipeId in engine.recipes)
            if (action.type == AssistantActionType.swap_recipe) {
                require(Regex("""\b(?:doi|thay|muon an|muon nau)\b""").containsMatchIn(MealPlanning.normalize(request)))
                if (wanted.isEmpty()) require(!Regex("""\b(?:dung|khong|khoi) (?:doi|thay)\b""").containsMatchIn(MealPlanning.normalize(request)))
                val index = requireNotNull(action.index); require(index in 0..2)
                require(engine.recipes.getValue(requireNotNull(action.recipeId)).kind.ordinal == index)
                require(index !in kept && (wanted.isEmpty() || index in wanted))
                require(named[index] == null || named[index] == action.recipeId)
            }
            if (action.type == AssistantActionType.set_people) require(action.people in 1..12)
            if (action.type == AssistantActionType.start_timer) require(action.minutes in 1..180 && !action.label.isNullOrBlank() && action.label.length <= 200)
            extraIntents[action.type]?.let { require(Regex("\\b(?:$it)\\b").containsMatchIn(MealPlanning.normalize(request))) }
        }
        if (swaps.isNotEmpty()) require((named.keys - swaps.mapNotNull { it.index }.toSet()).isEmpty())
        answer.suggestions.forEachIndexed { index, item ->
            require(item.id == index + 1 && engine.validPlan(item.recipeIds))
            checkTarget(item.date, item.meal, context, scope)
            val keys = engine.ingredients(item.recipeIds.map(engine.recipes::getValue), context.getInt("people")).map { it.key }.toSet()
            require(item.pantryKeys.isNotEmpty() && keys.containsAll(item.pantryKeys))
        }
    }

    private fun checkTarget(day: java.time.LocalDate, meal: Meal, context: JSONObject, scope: MenuScope?) {
        val today = MealPlanning.parseDay(context.getString("today"))
        require(day in today..today.plusDays(6))
        val meals = context.getJSONArray("meals")
        require((0 until meals.length()).any { meals.getJSONObject(it).let { entry -> entry.getString("date") == day.toString() && entry.getString("meal") == meal.value } })
        if (scope != null) {
            require(scope.clarification == null)
            require(scope.day == null || scope.day == day); require(scope.meal == null || scope.meal == meal)
        }
    }

    private fun compare(context: JSONObject): AssistantReply? {
        val batch = context.optJSONArray("active_suggestions") ?: return null
        if (batch.length() != 2) return AssistantReply("Bạn cần hai mâm trong cùng lượt gợi ý để so sánh.", emptyList(), source = AssistantSource.LOCAL)
        val text = (0..1).joinToString("\n\n") { i ->
            val ids = batch.getJSONObject(i).stringList("recipe_ids", 3, 3)
            val recipes = ids.map(engine.recipes::getValue)
            "Mâm ${i + 1}: ${recipes.joinToString { it.name }}. Ước tính ${engine.cost(recipes, context.getInt("people"))} đồng, ${engine.minutes(recipes)} phút."
        }
        return AssistantReply(text, emptyList(), source = AssistantSource.LOCAL)
    }

    private fun catalog() = JSONArray(engine.recipes.values.map { recipe ->
        JSONObject().put("id", recipe.id).put("name", recipe.name).put("kind", listOf("main", "soup", "veg")[recipe.kind.ordinal])
            .put("minutes", recipe.minutes).put("cost", recipe.cost).put("steps", JSONArray(recipe.steps)).put("tip", recipe.tip)
            .put("ingredients", JSONArray(recipe.ingredients.map { JSONObject().put("name", it.name).put("amount", it.amount).put("unit", it.unit).put("group", it.group) }))
    })

    private val extraIntents = mapOf(
        AssistantActionType.set_people to "khau phan|so nguoi|(?:\\d+|mot|hai|ba|bon|nam|sau|bay|tam|chin|muoi) nguoi",
        AssistantActionType.set_meal_time to "gio an|an luc|an vao", AssistantActionType.set_reminder to "nhac|bao thuc|thong bao",
        AssistantActionType.start_timer to "hen gio|dong ho|dem nguoc", AssistantActionType.cancel_timer to "hen gio|dong ho|dem nguoc",
        AssistantActionType.set_ingredient to "da mua|da co|danh dau|tich|nguyen lieu", AssistantActionType.set_task to "da xong|hoan thanh|danh dau|tich|buoc nau",
        AssistantActionType.set_favorite to "luu|yeu thich",
    )
}

internal fun JSONObject.strictText(key: String, limit: Int = 100, allowEmpty: Boolean = false): String =
    (get(key) as? String ?: throw AssistantFailure(AssistantFailureKind.INVALID)).also { require((allowEmpty || it.isNotBlank()) && it.length <= limit) }

internal fun JSONObject.stringList(key: String, min: Int, max: Int): List<String> {
    val list = getJSONArray(key); require(list.length() in min..max)
    return (0 until list.length()).map { (list.get(it) as? String ?: throw AssistantFailure(AssistantFailureKind.INVALID)).also { item -> require(item.isNotBlank() && item.length <= 100) } }
}
