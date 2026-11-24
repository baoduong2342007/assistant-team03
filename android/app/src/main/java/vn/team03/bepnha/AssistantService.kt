package vn.team03.bepnha

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate

data class AssistantToolCall(val name: String, val arguments: JSONObject, val id: String? = null)
data class AssistantModelReply(val text: String? = null, val calls: List<AssistantToolCall> = emptyList())
data class AssistantToolResult(val call: AssistantToolCall, val result: JSONObject)

/** One model conversation for one user turn. The first [send] carries no results. */
fun interface AssistantSession { suspend fun send(results: List<AssistantToolResult>): AssistantModelReply }
fun interface AssistantProvider {
    fun open(payload: JSONObject): AssistantSession
    companion object {
        /** A provider whose every model call is answered by [block] (handy for fakes and tests). */
        fun reply(block: suspend (List<AssistantToolResult>) -> AssistantModelReply) = AssistantProvider { AssistantSession { block(it) } }
    }
}

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

/**
 * One user turn as a small agent loop: the model may read app data with tools, then end with
 * one proposal. Proposals are validated against the snapshot; errors go back to the model so it
 * can correct itself. Never commits planner state — the ViewModel applies by risk level.
 */
class AssistantService(
    private val engine: PlannerEngine,
    private val provider: AssistantProvider?,
    private val clock: () -> Long = System::currentTimeMillis,
    private val turnTimeoutMillis: Long = BuildConfig.ASSISTANT_TIMEOUT_MS,
) {
    companion object {
        const val MAX_MODEL_CALLS = 4
        const val MAX_CALLS_PER_REPLY = 6
        const val GAVE_UP = "Mình chưa làm được yêu cầu này. Bạn nói rõ hơn ngày, bữa hoặc món giúp mình nhé."
        const val UNSURE = "Mình chưa chắc đã hiểu đúng món bạn muốn đổi hay giữ. Bạn nói lại giúp mình món và bữa nhé."
    }
    private val rules = MealPlanning(engine.recipes)
    private val commands = AssistantCommands(engine)
    private val reads = AgentReadTools(engine, rules)
    private val planner = MealPlanner(engine)
    private val catalogJson by lazy { AgentTools.catalogIndex(engine).toString() }

    suspend fun ask(question: String, history: List<AssistantChatMessage>, context: JSONObject): AssistantReply =
        ask(question, history, context, null)

    internal suspend fun ask(question: String, history: List<AssistantChatMessage>, context: JSONObject, snapshot: PlannerState?): AssistantReply {
        require(question.isNotBlank() && question.length <= 1000 && context.toString().length <= 100_000)
        val recent = history.takeLast(20)
        if (MealPlanning.isComparison(question)) compare(context)?.let { return it }
        val state by lazy { snapshot ?: AssistantGateway.stateFrom(engine, context) }
        if (provider == null) return localTurn(question, recent, context, AssistantFailureKind.UNCONFIGURED) { state }
        return try {
            withTimeoutOrNull(turnTimeoutMillis) { agent(provider, question, recent, context, state) }
                ?: localTurn(question, recent, context, AssistantFailureKind.TIMEOUT) { state }
        } catch (e: CancellationException) { throw e
        } catch (e: AssistantFailure) {
            if (!e.kind.localFallback) throw e
            localTurn(question, recent, context, e.kind) { state }
        } catch (_: IllegalArgumentException) { throw AssistantFailure(AssistantFailureKind.INVALID)
        } catch (_: IllegalStateException) { throw AssistantFailure(AssistantFailureKind.INVALID)
        } catch (_: org.json.JSONException) { throw AssistantFailure(AssistantFailureKind.INVALID) }
    }

    private fun localTurn(question: String, history: List<AssistantChatMessage>, context: JSONObject, kind: AssistantFailureKind, state: () -> PlannerState): AssistantReply {
        val scope = rules.scope(question, context, rules.previousQuestion(history))
        val reply = rules.clarification(question, history, context, scope)?.let { AssistantReply(it, emptyList()) }
            ?: local(question, history, context, scope, state)
        return reply.copy(source = AssistantSource.LOCAL, notice = kind.explanation)
    }

    private sealed interface Outcome {
        data class Done(val reply: AssistantReply) : Outcome
        data class Retry(val error: String, val advisory: Boolean = false) : Outcome
    }

    private suspend fun agent(provider: AssistantProvider, question: String, history: List<AssistantChatMessage>, context: JSONObject, state: PlannerState): AssistantReply {
        val today = MealPlanning.parseDay(context.getString("today"))
        val scope = runCatching { rules.scope(question, context, rules.previousQuestion(history)) }.getOrDefault(MenuScope())
        val payload = JSONObject().put("question", question).put("context", context)
            .put("history", JSONArray(history.map { JSONObject().put("role", it.role).put("text", it.text) }))
            .put("catalog", JSONArray(catalogJson))
            // A hint only: the agent may plan several meals; it asks when the target is unclear.
            .put("menu_scope", JSONObject().put("date", scope.day?.toString() ?: JSONObject.NULL)
                .put("meal", scope.meal?.value ?: JSONObject.NULL).put("clarification", scope.clarification ?: JSONObject.NULL))
        val session = provider.open(payload)
        var results = emptyList<AssistantToolResult>()
        var advised = false
        repeat(MAX_MODEL_CALLS) {
            val reply = session.send(results)
            if (reply.calls.isEmpty()) {
                val text = reply.text?.trim().orEmpty()
                if (text.isEmpty() || text.length > 8000) throw AssistantFailure(AssistantFailureKind.INVALID)
                return AssistantReply(text, emptyList())
            }
            if (reply.calls.size > MAX_CALLS_PER_REPLY) throw AssistantFailure(AssistantFailureKind.INVALID)
            val terminal = reply.calls.filter { it.name in AgentTools.terminal }
            val outcome = if (terminal.isEmpty()) null else finish(terminal, question, history, context, state, today, advised)
            if (outcome is Outcome.Done) return outcome.reply
            val retry = outcome as Outcome.Retry?
            advised = advised || retry?.advisory == true
            // Every call needs a response: reads return data, terminal calls return the error to fix.
            results = reply.calls.map { call ->
                AssistantToolResult(call, if (call.name in AgentTools.terminal) JSONObject().put("error", retry?.error) else read(call, state, today))
            }
        }
        return AssistantReply(GAVE_UP, emptyList())
    }

    private fun read(call: AssistantToolCall, state: PlannerState, today: LocalDate): JSONObject = try {
        reads.run(call.name, call.arguments, state, today)
    } catch (e: RuntimeException) {
        if (e is CancellationException) throw e
        JSONObject().put("error", e.message ?: "Tham số không hợp lệ.")
    }

    private fun finish(
        calls: List<AssistantToolCall>, question: String, history: List<AssistantChatMessage>, context: JSONObject,
        state: PlannerState, today: LocalDate, advised: Boolean,
    ): Outcome = try {
        if (calls.any { it.name == "suggest_meals" }) {
            if (calls.size != 1) Outcome.Retry("suggest_meals phải được gọi riêng, không kèm propose_actions hoặc plan_meals.")
            else Outcome.Done(suggestFromTool(calls.single().arguments, question, history, context, state.diet))
        } else {
            val texts = mutableListOf<String>()
            val actions = mutableListOf<AssistantAction>()
            for (call in calls) {
                val args = call.arguments
                args.optString("reply").takeIf { it.isNotBlank() }?.let { texts += it.trim().take(2000) }
                if (call.name == "propose_actions") {
                    val unknown = args.keys().asSequence().filter { it !in setOf("reply", "actions") }.toList()
                    require(unknown.isEmpty()) { "propose_actions không nhận tham số ${unknown.joinToString()}." }
                    val list = args.getJSONArray("actions")
                    actions += (0 until list.length()).map { i ->
                        try { AssistantGateway.parseAction(list.getJSONObject(i), state.date, state.meal) }
                        catch (e: IllegalArgumentException) { throw IllegalArgumentException("Thao tác thứ ${i + 1}: ${e.message}") }
                    }
                } else actions += planMeals(args, question, history, state, today)
            }
            val proposal = commands.proposal(state, actions, today, clock())
            val (dietAdvice, dietNotes) = dietCheck(actions, question, history, state.diet)
            val wording = advise(actions, question, history)
            val advice = wording ?: dietAdvice
            when {
                advice != null && advised -> Outcome.Done(AssistantReply(
                    if (wording != null) UNSURE else "Mình chưa tìm được món hợp hồ sơ ăn uống của gia đình (${state.diet.describe()}). Bạn muốn chọn món nào?",
                    emptyList()))
                advice != null -> Outcome.Retry(advice, advisory = true)
                actions.isNotEmpty() && proposal.actions.isEmpty() || actions.isEmpty() && calls.any { it.name == "plan_meals" } ->
                    Outcome.Done(AssistantReply("Các món đã khớp với thực đơn đang dùng.", emptyList()))
                actions.isEmpty() -> Outcome.Done(AssistantReply(texts.joinToString(" ").ifBlank { GAVE_UP }, emptyList()))
                else -> Outcome.Done(AssistantReply(
                    (texts.joinToString(" ").ifBlank { if (proposal.needsConfirmation) "Mình đề xuất các thay đổi sau." else "Mình làm ngay cho bạn nhé." } +
                        dietNotes.joinToString("") { " $it" }).trim(),
                    actions, recipeIds = actions.filter { it.type == AssistantActionType.open_recipe }.mapNotNull { it.recipeId }))
            }
        }
    } catch (e: MealPlanner.NoMealFits) {
        Outcome.Done(AssistantReply("${e.message} Bạn muốn nới điều kiện nào không?", emptyList()))
    } catch (e: IllegalArgumentException) {
        Outcome.Retry(e.message ?: "Tham số không hợp lệ.")
    } catch (e: org.json.JSONException) {
        Outcome.Retry("Tham số không đúng cấu trúc: ${e.message}")
    }

    private fun planMeals(args: JSONObject, question: String, history: List<AssistantChatMessage>, state: PlannerState, today: LocalDate): List<AssistantAction> {
        val allowed = setOf("from", "to", "meals", "avoid_ingredients", "prefer_ingredients", "max_minutes", "max_cost", "reply")
        val unknown = args.keys().asSequence().filter { it !in allowed }.toList()
        require(unknown.isEmpty()) { "plan_meals không nhận tham số ${unknown.joinToString()}." }
        val from = requireNotNull(args.optDate("from")) { "plan_meals thiếu from." }
        val to = requireNotNull(args.optDate("to")) { "plan_meals thiếu to." }
        val last = today.plusDays(6)
        require(from in today..last && to in from..last) { "from/to phải nằm trong $today..$last và from ≤ to." }
        val meals = args.optStrings("meals").map { requireNotNull(Meal.from(it)) { "meals chỉ gồm breakfast, lunch, dinner." } }.ifEmpty { engine.meals }
        val avoidTerms = args.optStrings("avoid_ingredients")
        val unknownAvoid = avoidTerms.filter { rules.ingredientNames(listOf(it)).isEmpty() }
        require(unknownAvoid.isEmpty()) { "Không có nguyên liệu ${unknownAvoid.joinToString()} trong kho; dùng tên nguyên liệu có trong công thức hoặc hỏi lại người dùng." }
        // The user's own words are authoritative over model omissions.
        val grounded = rules.constraints(userMessages(question, history, JSONObject()))
        val constraints = PlanConstraints(
            avoid = rules.ingredientNames(avoidTerms) + grounded.excluded,
            prefer = rules.ingredientNames(args.optStrings("prefer_ingredients")) + grounded.pantry,
            maxMinutes = args.optPositiveInt("max_minutes"), maxCost = args.optPositiveInt("max_cost")?.toLong(),
        )
        val targets = generateSequence(from) { it.plusDays(1) }.takeWhile { it <= to }.flatMap { day -> meals.map { day to it } }.toList()
        return planner.plan(state, targets, constraints).flatMap { planned ->
            commands.swapsTo(state.plans.getValue(PlannerEngine.key(planned.date, planned.meal)), planned.recipeIds, planned.date, planned.meal)
        }
    }

    /**
     * Dishes the model picked that break the family's diet profile are sent back once as advice.
     * Dishes the user named explicitly are allowed, with a spoken warning in the reply.
     */
    private fun dietCheck(actions: List<AssistantAction>, question: String, history: List<AssistantChatMessage>, diet: DietProfile): Pair<String?, List<String>> {
        if (diet.isEmpty) return null to emptyList()
        val named = rules.requestedRecipes("${rules.previousQuestion(history)} $question").values.toSet()
        val conflicts = actions.filter { it.type == AssistantActionType.swap_recipe }
            .mapNotNull { action -> engine.recipes[action.recipeId]?.let { recipe -> recipe to diet.reasons(recipe) } }
            .filter { it.second.isNotEmpty() }
        val chosen = conflicts.filter { it.first.id !in named }
        val advice = if (chosen.isEmpty()) null else "Hồ sơ ăn uống của gia đình: ${diet.describe()}. " +
            chosen.joinToString(" ") { (recipe, why) -> "${recipe.name} không hợp (${why.joinToString()})." } +
            " Hãy chọn món cùng nhóm phù hợp bằng search_recipes, hoặc hỏi lại người dùng."
        val notes = conflicts.filter { it.first.id in named }.map { (recipe, why) -> "Lưu ý: ${recipe.name} ${why.joinToString()}, chưa hợp hồ sơ ăn uống của gia đình." }
        return advice to notes
    }

    /** High-precision checks on the user's own words, sent back once as advice to the model. */
    private fun advise(actions: List<AssistantAction>, question: String, history: List<AssistantChatMessage>): String? {
        val swaps = actions.filter { it.type == AssistantActionType.swap_recipe }
        if (swaps.isEmpty() || swaps.map { it.date to it.meal }.distinct().size != 1) return null
        val request = "${rules.previousQuestion(history)} $question"
        val text = MealPlanning.normalize(request)
        fun slot(index: Int?) = if (swaps.first().meal.tray) RecipeKind.trayKinds[requireNotNull(index)].label.lowercase()
            else RecipeKind.BREAKFAST.label.lowercase()
        val kept = rules.namedSlots(request, "giu(?: nguyen)?|(?:dung|khong|khoi) (?:doi|thay)")
        swaps.firstOrNull { it.index in kept }?.let { return "Người dùng muốn giữ nguyên ${slot(it.index)}. Chỉ đổi vị trí họ yêu cầu." }
        if (kept.isEmpty() && Regex("""\b(?:dung|khong|khoi) (?:doi|thay)\b""").containsMatchIn(text))
            return "Người dùng nói không muốn đổi. Đừng đề xuất đổi món; hãy hỏi lại bằng văn bản."
        val named = rules.requestedRecipes(request)
        for ((index, id) in named) swaps.firstOrNull { it.index == index && it.recipeId != id }?.let {
            return "Người dùng nêu rõ món ${engine.recipes[id]?.name} (recipe_id=$id) cho ${slot(index)}."
        }
        val wanted = rules.namedSlots(request, "(?<!dung )(?<!khong )(?<!khoi )(?:doi|thay)")
        if (wanted.isNotEmpty()) swaps.firstOrNull { it.index !in wanted && named[it.index] == null }?.let {
            return "Người dùng chỉ yêu cầu đổi ${wanted.joinToString { index -> slot(index) }}; không đổi ${slot(it.index)}."
        }
        return null
    }

    private fun suggestFromTool(args: JSONObject, question: String, history: List<AssistantChatMessage>, context: JSONObject, diet: DietProfile): AssistantReply {
        val unknown = args.keys().asSequence().filter { it !in setOf("date", "meal", "pantry_items", "exclude_ingredients", "pantry_only") }.toList()
        require(unknown.isEmpty()) { "suggest_meals không nhận tham số ${unknown.joinToString()}." }
        val day = MealPlanning.parseDay(args.strictText("date"))
        val meal = requireNotNull(Meal.from(args.strictText("meal"))) { "meal phải là breakfast, lunch hoặc dinner." }
        checkTarget(day, meal, context)
        val terms = args.stringList("pantry_items", 1, 30)
        val excluded = if (args.has("exclude_ingredients")) args.stringList("exclude_ingredients", 0, 30) else emptyList()
        val only = if (args.has("pantry_only")) args.get("pantry_only") as? Boolean ?: throw IllegalArgumentException("pantry_only phải là true/false.") else false
        val messages = userMessages(question, history, context)
        val grounded = rules.constraints(messages)
        // Constraints from the user's words are authoritative over model omissions.
        val pantry = rules.pantryNames(terms, messages)
        if (excluded.any { rules.ingredientNames(listOf(it)).isEmpty() } || grounded.unknownExclusion)
            return AssistantReply("Mình chưa đối chiếu được nguyên liệu cần tránh với kho công thức. Bạn cho mình tên cụ thể nhé.", emptyList())
        return suggest(MenuScope(day, meal), PantryConstraints(pantry, rules.ingredientNames(excluded) + grounded.excluded, only || grounded.only), diet, context.getInt("people"))
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

    private fun suggest(scope: MenuScope, constraints: PantryConstraints, diet: DietProfile, people: Int = 4): AssistantReply {
        scope.clarification?.let { return AssistantReply(it, emptyList()) }
        if (constraints.unknownExclusion) return AssistantReply("Mình chưa nhận ra nguyên liệu cần tránh. Bạn cho mình tên cụ thể nhé.", emptyList())
        if (constraints.pantry.isEmpty()) return AssistantReply("Mình chưa nhận ra nguyên liệu nào trong kho công thức. Bạn cho mình tên nguyên liệu cụ thể nhé.", emptyList())
        val meal = requireNotNull(scope.meal)
        val ranked = rules.rankMeals(constraints.pantry, constraints.excluded + diet.avoid, diet::allows, meal) { dishes ->
            (diet.maxMinutes == null || engine.minutes(dishes) <= diet.maxMinutes) &&
                (diet.maxCost == null || engine.cost(dishes, people) <= diet.maxCost)
        }
        if (ranked.isEmpty()) return AssistantReply("Mình chưa tìm được mâm có thể tận dụng nguyên liệu đã nêu và đáp ứng yêu cầu này" +
            (if (diet.isEmpty) "." else " cùng hồ sơ ăn uống (${diet.describe()})."), emptyList())
        val options = ranked.filter { !constraints.only || it.missing.isEmpty() }.take(2)
        if (options.isEmpty()) return AssistantReply("Mâm phù hợp nhất vẫn cần thêm nguyên liệu như ${ranked.first().missing.take(3).joinToString()}. Bạn muốn xem lựa chọn có thể mua thêm không?", emptyList())
        return AssistantReply("Mình gợi ý mâm dưới đây theo nguyên liệu bạn có. Bạn có thể chọn mâm muốn áp dụng.", emptyList(),
            options.mapIndexed { index, item -> AssistantMealSuggestion(index + 1, requireNotNull(scope.day), requireNotNull(scope.meal), item.recipeIds, item.pantryKeys) },
            suggestionConstraints = constraints)
    }

    private fun local(question: String, history: List<AssistantChatMessage>, context: JSONObject, scope: MenuScope, state: () -> PlannerState): AssistantReply {
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
            if (localExtraIntents.any { Regex("\\b(?:$it)\\b").containsMatchIn(text) })
                return AssistantReply("Yêu cầu này có thêm thao tác ngoài đổi món mà trợ lý trên máy chưa xử lý chắc chắn. Bạn hãy tách yêu cầu hoặc dùng các màn hình của ứng dụng.", emptyList())
            val meal = requireNotNull(scope.meal)
            val diet = state().diet
            val dishes = named.values.map(engine.recipes::getValue)
            if (dishes.any { (it.kind == RecipeKind.BREAKFAST) == meal.tray })
                return AssistantReply("Món bạn nêu thuộc nhóm của bữa khác. Bạn muốn chọn bữa nào?", emptyList())
            val warnings = dishes.flatMap(diet::reasons).distinct()
            val reply = if (warnings.isEmpty()) "Mình đề xuất các thao tác sau."
                else "Món bạn chọn chưa hợp hồ sơ ăn uống: ${warnings.joinToString()}. Bạn hãy kiểm tra trước khi xác nhận."
            return AssistantReply(reply, named.map { (index, id) ->
                AssistantAction(AssistantActionType.swap_recipe, requireNotNull(scope.day), meal, recipeId = id, index = index)
            })
        }
        if (Regex("""\b(?:doi|thay)\b""").containsMatchIn(text))
            return AssistantReply("Bạn cho mình tên đầy đủ của món muốn thay vào nhé. Bạn cũng có thể chọn món trực tiếp trong Thực đơn.", emptyList())
        if (MealPlanning.menuRequest.containsMatchIn(text) || Regex("""\b(?:chi co|chi con|moi mua|tranh|khong dung|khong mua them)\b""").containsMatchIn(text))
            return suggest(scope, rules.constraints(userMessages(question, history, context)), state().diet, context.getInt("people"))
        val focused = engine.recipes[context.optString("recipe_id")]
        if (focused != null && "mon nay" in text) {
            val answer = if ("nguyen lieu" in text) focused.ingredients.joinToString("\n") {
                "${it.name}: ${PlannerEngine.quantity(it.amount * context.getInt("people") / 4.0)} ${it.unit}"
            } else focused.steps.mapIndexed { i, step -> "${i + 1}. $step" }.joinToString("\n\n")
            return AssistantReply("${focused.name}\n\n$answer", emptyList(), recipeIds = listOf(focused.id))
        }
        val current = state()
        val answer = engine.answer(question, current, MealPlanning.parseDay(context.getString("today")))
        if (Regex("""^(?:hay |giup minh )?(?:hen|dat gio|dem nguoc)\b""").containsMatchIn(text)) {
            answer.timerMinutes?.let { minutes ->
                if (current.timers.count { it.end > clock() } < PlannerEngine.MAX_TIMERS ||
                    current.timers.any { it.label == answer.timerLabel && it.date == current.date && it.meal == current.meal }) {
                    return AssistantReply("Mình đặt đồng hồ cho bạn nhé.", listOf(
                        AssistantAction(AssistantActionType.start_timer, current.date, current.meal, minutes = minutes, label = answer.timerLabel)))
                }
                return AssistantReply("Đang chạy ${PlannerEngine.MAX_TIMERS} đồng hồ. Bạn hãy dừng bớt một đồng hồ trước khi hẹn thêm.", emptyList())
            }
            return AssistantReply(answer.text, emptyList())
        }
        // Other mutations require the online agent or explicit app controls.
        if (Regex("""\b(?:nhac|hen gio|dong ho|khau phan|nguoi|danh dau|da mua|da xong|luu mon|mo)\b""").containsMatchIn(text))
            return AssistantReply("Trợ lý trên máy chưa xử lý chắc chắn thao tác này. Bạn có thể mở Thực đơn, Đi chợ, Vào bếp hoặc Gia đình để thao tác trực tiếp.", emptyList())
        return AssistantReply(answer.text, emptyList(), recipeIds = answer.recipes)
    }

    private fun checkTarget(day: LocalDate, meal: Meal, context: JSONObject) {
        val today = MealPlanning.parseDay(context.getString("today"))
        require(day in today..today.plusDays(6)) { "Ngày $day nằm ngoài thực đơn bảy ngày." }
        val meals = context.getJSONArray("meals")
        require((0 until meals.length()).any { meals.getJSONObject(it).let { entry -> entry.getString("date") == day.toString() && entry.getString("meal") == meal.value } }) { "Không tìm thấy bữa ăn." }
    }

    private fun compare(context: JSONObject): AssistantReply? {
        val batch = context.optJSONArray("active_suggestions") ?: return null
        if (batch.length() != 2) return AssistantReply("Bạn cần hai mâm trong cùng lượt gợi ý để so sánh.", emptyList(), source = AssistantSource.LOCAL)
        val text = (0..1).joinToString("\n\n") { i ->
            val ids = batch.getJSONObject(i).stringList("recipe_ids", 1, 3)
            val recipes = ids.map(engine.recipes::getValue)
            "Mâm ${i + 1}: ${recipes.joinToString { it.name }}. Ước tính ${engine.cost(recipes, context.getInt("people"))} đồng, ${engine.minutes(recipes)} phút."
        }
        return AssistantReply(text, emptyList(), source = AssistantSource.LOCAL)
    }

    /** Requests the local parser must not silently drop when it only understands the dish swap. */
    private val localExtraIntents = listOf(
        "khau phan|so nguoi|(?:\\d+|mot|hai|ba|bon|nam|sau|bay|tam|chin|muoi) nguoi",
        "gio an|an luc|an vao", "nhac|bao thuc|thong bao", "hen gio|dong ho|dem nguoc",
        "da mua|da co|danh dau|tich|nguyen lieu", "da xong|hoan thanh|buoc nau", "luu|yeu thich",
    )
}

internal fun JSONObject.strictText(key: String, limit: Int = 100, allowEmpty: Boolean = false): String =
    (get(key) as? String ?: throw IllegalArgumentException("$key phải là chuỗi.")).also { require((allowEmpty || it.isNotBlank()) && it.length <= limit) { "$key không hợp lệ." } }

internal fun JSONObject.stringList(key: String, min: Int, max: Int): List<String> {
    val list = getJSONArray(key); require(list.length() in min..max) { "$key cần từ $min đến $max mục." }
    return (0 until list.length()).map { (list.get(it) as? String ?: throw IllegalArgumentException("$key chỉ gồm chuỗi.")).also { item -> require(item.isNotBlank() && item.length <= 100) } }
}
