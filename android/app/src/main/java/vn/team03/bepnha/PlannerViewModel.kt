package vn.team03.bepnha

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.AndroidViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class PlannerViewModel(application: Application, private val savedState: SavedStateHandle) : AndroidViewModel(application) {
    val engine = RecipeRepository(application).engine
    private val storage = PlannerStorage(application, engine)
    var today by mutableStateOf(PlannerEngine.today()); private set
    var state by mutableStateOf(storage.load(today)); private set
    var screen by mutableStateOf(Screen.TODAY); private set
    var sheet by mutableStateOf<PlannerSheet?>(null); private set
    var pendingTimer by mutableStateOf<Pair<Int, String>?>(null); private set
    var now by mutableLongStateOf(System.currentTimeMillis()); private set
    var message by mutableStateOf<String?>(null); private set
    var notifications by mutableStateOf(ReminderStore.canNotify(application)); private set
    var exactAlarms by mutableStateOf(ReminderStore.canScheduleExact(application)); private set
    var assistantVisible by mutableStateOf(false); private set
    var assistantHistory by mutableStateOf(emptyList<AssistantChatMessage>()); private set
    private var questionState by mutableStateOf(savedState.get<String>("assistant-draft").orEmpty())
    var assistantQuestion: String
        get() = questionState
        set(value) { questionState = value.take(1000); savedState["assistant-draft"] = questionState }
    var assistantBusy by mutableStateOf(false); private set
    var assistantError by mutableStateOf<String?>(null); private set
    var assistantProposal by mutableStateOf<AssistantProposal?>(null); private set
    var assistantRecipeId by mutableStateOf<String?>(null); private set
    private data class MealSuggestions(val base: PlannerState, val options: List<AssistantMealSuggestion>, val messageId: String, val requestId: String, val constraints: PantryConstraints?)
    private var offeredMeals by mutableStateOf<MealSuggestions?>(null)
    val assistantMealSuggestions get() = offeredMeals?.options.orEmpty()
    val assistantMealMessageId get() = offeredMeals?.messageId
    private var revisingProposal by mutableStateOf<AssistantProposal?>(null)
    val assistantRevisionTarget: String?
        get() = revisingProposal?.actions?.firstOrNull()?.let { action ->
            "${action.meal.title} · ${PlannerEngine.fullDate(action.date)}"
        }
    private var lastMenuChanges = emptyList<AssistantAction>()
    private var assistantOrigin = Screen.TODAY
    private var returnRecipeId: String? = null
    private data class Turn(val prompt: String, val history: List<AssistantChatMessage>, val base: PlannerState, val context: org.json.JSONObject, val messageId: String)
    private var generation = 0L
    private var activeJob: Job? = null
    private var activeTurn: Turn? = null
    private var failedTurn: Turn? = null
    private val unansweredMessageIds = mutableSetOf<String>()
    var failedMessageId by mutableStateOf<String?>(null); private set
    val assistantConfigured get() = BuildConfig.DEBUG && BuildConfig.FIREBASE_CONFIGURED
    val canRetryAssistant get() = failedMessageId != null && !assistantBusy && assistantProposal == null
    private val assistantService = AssistantService(engine, if (assistantConfigured) FirebaseAssistantProvider(application) else null)
    internal var assistantTransport: suspend (String, List<AssistantChatMessage>, org.json.JSONObject) -> AssistantReply = assistantService::ask
    private val commands = AssistantCommands(engine)
    private val updates = Channel<Pair<PlannerState, String?>>(Channel.UNLIMITED)
    private val undo = AssistantUndo()
    /** Reads [state], so it recomposes whenever the data the last change left behind is edited. */
    val canUndoAssistant get() = undo.canUndo(state)
    /** One pending Activity-side effect (share sheet, calendar, permission screen); see [consumeEffect]. */
    var effect by mutableStateOf<PlannerEffect?>(null); private set
    fun consumeEffect(shown: PlannerEffect) { if (effect == shown) effect = null }

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for ((snapshot, outcome) in updates) {
                try {
                    storage.save(snapshot)
                    ReminderStore.synchronize(application, PlannerStorage.alarmJson(engine.alarms(snapshot, System.currentTimeMillis())))
                    if (outcome != null) withContext(Dispatchers.Main) { assistantHistory = assistantHistory + AssistantChatMessage("assistant", outcome, source = AssistantSource.SYSTEM) }
                } catch (exception: Exception) {
                    withContext(Dispatchers.Main) {
                        message = "Chưa lưu được dữ liệu hoặc lịch nhắc. Bạn hãy mở lại ứng dụng để kiểm tra."
                        if (outcome != null) assistantError = message
                    }
                }
            }
        }
        updates.trySend(state to null)
    }

    val menu get() = engine.menu(state)
    val tasks get() = engine.timeline(state)
    val days get() = (0L..6L).map(today::plusDays)
    val reminderEnabled get() = state.key in state.reminders

    private fun update(next: PlannerState, outcome: String? = null) {
        if (next != state) {
            if (assistantBusy || assistantProposal != null || revisingProposal != null || failedTurn != null) {
                invalidateTurn("Bữa ăn đã thay đổi. Hãy gửi lại câu hỏi cho bữa đang chọn.")
            }
            assistantProposal = null
            revisingProposal = null
            offeredMeals = null
            lastMenuChanges = emptyList()
        }
        state = next
        updates.trySend(next to outcome)
    }
    fun navigate(next: Screen) {
        if (assistantVisible) finishAssistant(restoreRecipe = false)
        screen = next
    }
    fun selectDate(date: LocalDate) { if (date in days) update(state.copy(date = date)) }
    fun selectMeal(meal: Meal) { update(state.copy(meal = meal)) }
    fun openRecipe(id: String) { if (id in engine.recipes) sheet = PlannerSheet.RecipeDetail(id) }
    fun openSwap(index: Int) { sheet = PlannerSheet.Swap(index) }
    fun openReminders() { sheet = PlannerSheet.Reminder }
    fun openAssistant() {
        if (assistantVisible) {
            (sheet as? PlannerSheet.RecipeDetail)?.let { recipe ->
                if (assistantRecipeId != recipe.id) {
                    if (assistantBusy || assistantProposal != null || revisingProposal != null || failedTurn != null) {
                        invalidateTurn("Món đang hỏi đã thay đổi. Hãy gửi lại câu hỏi.")
                    }
                    assistantProposal = null
                    revisingProposal = null
                    offeredMeals = null
                }
                assistantRecipeId = recipe.id
                returnRecipeId = recipe.id
                sheet = null
            }
            return
        }
        returnRecipeId = (sheet as? PlannerSheet.RecipeDetail)?.id
        assistantRecipeId = returnRecipeId
        assistantOrigin = screen
        sheet = null
        assistantVisible = true
    }
    fun closeAssistant() = finishAssistant(restoreRecipe = true)
    private fun finishAssistant(restoreRecipe: Boolean) {
        if (assistantBusy || (revisingProposal != null && failedTurn != null)) {
            invalidateTurn("Yêu cầu trước đã dừng. Bạn có thể gửi lại câu hỏi.")
        }
        assistantProposal = null
        revisingProposal = null
        offeredMeals = null
        assistantVisible = false
        if (restoreRecipe) returnRecipeId?.let(::openRecipe)
        returnRecipeId = null
    }
    private fun invalidateTurn(reason: String) {
        val prompt = activeTurn?.prompt ?: failedTurn?.prompt ?: assistantProposal?.let { assistantHistory.lastOrNull { it.role == "user" }?.text }
        if (assistantQuestion.isBlank() && prompt != null) assistantQuestion = prompt
        generation++
        activeJob?.cancel()
        activeJob = null
        activeTurn = null
        failedTurn = null
        failedMessageId = null
        assistantBusy = false
        assistantError = reason
    }
    fun cancelAssistantProposal() {
        if (assistantProposal == null && revisingProposal == null) return
        if (assistantBusy || failedTurn != null) invalidateTurn("Đã hủy đề xuất.")
        assistantProposal = null
        revisingProposal = null
        assistantError = null
        assistantHistory = assistantHistory + AssistantChatMessage("assistant", "Đã hủy đề xuất. Dữ liệu được giữ nguyên.", source = AssistantSource.SYSTEM)
    }

    fun editAssistantProposal() {
        val proposal = assistantProposal?.takeIf { it.canReviseMenu } ?: return
        revisingProposal = proposal
        assistantProposal = null
    }

    fun chooseAssistantMeal(id: Int) = chooseAssistantMeal(id, "Chọn mâm $id")

    private fun chooseAssistantMeal(id: Int, prompt: String) {
        if (!assistantVisible || assistantBusy || assistantProposal != null) return
        val suggestion = offeredMeals?.takeIf { it.base == state }?.options?.firstOrNull { it.id == id }
        val reply = if (suggestion == null) {
            "Bạn hãy yêu cầu gợi ý mâm cho bữa muốn nấu trước khi chọn."
        } else {
            val proposal = commands.proposal(state, commands.actionsFor(state, suggestion, today), today, now)
            offeredMeals = null
            revisingProposal = null
            assistantProposal = proposal.takeIf { it.actions.isNotEmpty() }
            if (assistantProposal == null) "Mâm này đang có trong thực đơn." else "Bạn xem lại các món sẽ đổi trong mâm $id."
        }
        assistantHistory = assistantHistory + AssistantChatMessage("user", prompt) +
            AssistantChatMessage("assistant", reply, source = AssistantSource.SYSTEM)
        assistantQuestion = ""
        failedTurn = null
        failedMessageId = null
        assistantError = null
    }

    fun submitAssistant() {
        askAssistant()
    }
    fun askAssistant(question: String = assistantQuestion) {
        if (question.isBlank() || assistantBusy || assistantProposal != null || !assistantVisible) return
        val prompt = question.trim().take(1000)
        if (undoPhrase.matches(PlannerEngine.normalize(prompt).trimEnd('.', '?', '!'))) {
            assistantHistory = assistantHistory + AssistantChatMessage("user", prompt)
            assistantQuestion = ""
            undoAssistant()
            return
        }
        val choice = mealChoice.matchEntire(PlannerEngine.normalize(prompt).trimEnd('.', '?', '!'))
        if (choice != null) {
            val option = choice.groupValues[1]
            chooseAssistantMeal(if (option in setOf("1", "mot", "thu nhat")) 1 else 2, prompt)
            return
        }
        // A failed message is not supplied as an already answered history turn.
        val history = assistantHistory.filterNot { it.id in unansweredMessageIds }
        val user = AssistantChatMessage("user", prompt)
        unansweredMessageIds += user.id
        assistantHistory = assistantHistory + user
        assistantQuestion = ""
        val context = AssistantGateway.snapshot(
            engine, state, today, assistantOrigin, assistantRecipeId,
            pendingMenuChanges = revisingProposal?.actions.orEmpty(),
            lastMenuChanges = lastMenuChanges,
        )
        offeredMeals?.takeIf { it.base == state }?.options?.firstOrNull()?.let {
            context.put("suggested_meal", org.json.JSONObject().put("date", it.date.toString()).put("meal", it.meal.value))
        }
        offeredMeals?.takeIf { it.base == state }?.constraints?.let {
            context.put("suggestion_constraints", org.json.JSONObject()
                .put("pantry", org.json.JSONArray(it.pantry.toList())).put("excluded", org.json.JSONArray(it.excluded.toList())).put("only", it.only))
        }
        if (MealPlanning.isComparison(prompt)) {
            offeredMeals?.takeIf { it.base == state }?.let { batch ->
                context.put("suggestion_turn_id", batch.requestId)
                context.put("active_suggestions", org.json.JSONArray(batch.options.map { option ->
                    org.json.JSONObject().put("id", option.id).put("date", option.date.toString()).put("meal", option.meal.value)
                        .put("recipe_ids", org.json.JSONArray(option.recipeIds))
                }))
            }
        } else {
            // Any new instruction can change constraints, even if the next reply fails or asks for clarification.
            offeredMeals = null
        }
        startTurn(Turn(prompt, history, state, context, user.id))
    }
    fun retryAssistant() {
        val turn = failedTurn ?: return
        if (assistantBusy || assistantProposal != null || !assistantVisible) return
        if (turn.base != state) {
            invalidateTurn("Bữa ăn đã thay đổi. Hãy gửi lại câu hỏi cho bữa đang chọn.")
            return
        }
        startTurn(turn)
    }
    private fun startTurn(turn: Turn) {
        val token = ++generation
        activeTurn = turn
        failedTurn = null
        failedMessageId = null
        assistantBusy = true
        assistantError = null
        activeJob = viewModelScope.launch {
            try {
                val reply = withContext(Dispatchers.IO) { assistantTransport(turn.prompt, turn.history, turn.context) }
                if (token != generation || !assistantVisible) return@launch
                require(state == turn.base) { "Bữa ăn đã thay đổi. Hãy gửi lại câu hỏi cho bữa đang chọn." }
                val actions = revisingProposal?.let { commands.reviseMenu(it, reply.actions) } ?: reply.actions
                val proposal = commands.proposal(state, actions, today, System.currentTimeMillis())
                val responseId = java.util.UUID.randomUUID().toString()
                if (reply.suggestions.isNotEmpty()) {
                    val first = reply.suggestions.first()
                    require(reply.actions.isEmpty() && reply.suggestions.size <= 2)
                    reply.suggestions.forEachIndexed { index, suggestion ->
                        require(suggestion.id == index + 1 && suggestion.date == first.date && suggestion.meal == first.meal)
                        commands.actionsFor(state, suggestion, today)
                    }
                    offeredMeals = MealSuggestions(state, reply.suggestions, responseId, turn.messageId, reply.suggestionConstraints)
                    revisingProposal = null
                }
                val replyText = if (actions.isNotEmpty() && proposal.actions.isEmpty()) {
                    revisingProposal = null
                    "Các món đã khớp với thực đơn đang dùng."
                } else reply.text
                unansweredMessageIds -= turn.messageId
                assistantHistory = assistantHistory + AssistantChatMessage("assistant", replyText.take(8000),
                    (reply.recipeIds + proposal.actions.filter { it.type == AssistantActionType.open_recipe }.mapNotNull { it.recipeId }).distinct(),
                    id = responseId, source = reply.source, notice = reply.notice)
                if (proposal.needsConfirmation) {
                    offeredMeals = null
                    revisingProposal = null
                    assistantProposal = proposal
                } else {
                    // READ and LOW actions run now (LOW can be undone). The response is complete
                    // before navigation/actions can invalidate the turn.
                    activeTurn = null
                    assistantBusy = false
                    if (proposal.actions.any { it.mutates }) revisingProposal = null
                    executeAssistantActions(proposal, confirmed = false)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                if (token == generation) {
                    failedTurn = turn
                    failedMessageId = turn.messageId
                    assistantError = exception.message?.takeUnless {
                        it in setOf("Failed requirement.", "Required value was null.", "Check failed.")
                    } ?: "Phản hồi trợ lý chưa hợp lệ. Hãy thử lại hoặc tìm trong công thức có sẵn."
                }
            } finally {
                if (token == generation) {
                    assistantBusy = false
                    activeTurn = null
                    activeJob = null
                }
            }
        }
    }
    fun lookupOffline() = lookupOnDevice(assistantQuestion)
    fun lookupFailedOffline() {
        val turn = failedTurn ?: return
        if (assistantBusy || assistantProposal != null) return
        val answer = localAnswer(turn.prompt)
        unansweredMessageIds -= turn.messageId
        assistantHistory = assistantHistory + AssistantChatMessage("assistant", answer.text, answer.recipes, source = AssistantSource.LOCAL)
        failedTurn = null
        failedMessageId = null
        assistantError = null
    }
    private fun lookupOnDevice(question: String) {
        if (question.isBlank() || assistantBusy || assistantProposal != null) return
        val prompt = question.trim().take(1000)
        val answer = localAnswer(prompt)
        assistantHistory = assistantHistory + AssistantChatMessage("user", prompt) +
            AssistantChatMessage("assistant", answer.text, answer.recipes, source = AssistantSource.LOCAL)
        assistantQuestion = ""
        failedTurn = null
        failedMessageId = null
        assistantError = null
    }
    private fun localAnswer(prompt: String): AssistantAnswer {
        val recipe = assistantRecipeId?.let(engine.recipes::get)
        val query = PlannerEngine.normalize(prompt)
        if (recipe != null && query.contains("mon nay")) {
            val text = if (query.contains("nguyen lieu")) {
                recipe.ingredients.joinToString("\n") {
                    "${it.name}: ${PlannerEngine.quantity(it.amount * state.profile.people / 4.0)} ${it.unit}"
                }
            } else {
                recipe.steps.mapIndexed { index, step -> "${index + 1}. $step" }.joinToString("\n\n")
            }
            return AssistantAnswer("${recipe.name}\n\n$text", listOf(recipe.id))
        }
        return engine.answer(prompt, state)
    }
    fun confirmAssistantProposal() {
        val proposal = assistantProposal ?: return
        assistantProposal = null
        try {
            require(state == proposal.base) { "Dữ liệu đã thay đổi. Hãy gửi lại câu hỏi để xem đề xuất mới." }
            executeAssistantActions(proposal, confirmed = true)
            if (proposal.canReviseMenu) lastMenuChanges = proposal.actions
        } catch (exception: Exception) {
            if (assistantQuestion.isBlank()) assistantQuestion = assistantHistory.lastOrNull { it.role == "user" }?.text.orEmpty()
            assistantError = exception.message ?: "Không thể thực hiện thao tác."
        }
    }

    /** Remove one meal's dish swaps from the open proposal (the user keeps that meal as it is). */
    fun dropProposalMeal(date: LocalDate, meal: Meal) {
        val proposal = assistantProposal ?: return
        val remaining = proposal.actions.filterNot { it.type == AssistantActionType.swap_recipe && it.date == date && it.meal == meal }
        if (remaining.isEmpty()) { cancelAssistantProposal(); return }
        assistantProposal = commands.proposal(proposal.base, remaining, today, System.currentTimeMillis())
    }

    /** Dish swaps of the open proposal grouped by meal, in day order, for a short readable preview. */
    fun proposalMeals(): List<ProposalMealUi> {
        val proposal = assistantProposal ?: return emptyList()
        return proposal.actions.filter { it.type == AssistantActionType.swap_recipe }
            .groupBy { it.date to it.meal }.toSortedMap(compareBy({ it.first }, { it.second.ordinal }))
            .map { (target, swaps) ->
                ProposalMealUi(target.first, target.second, "${target.second.title} · ${PlannerEngine.fullDate(target.first)}",
                    swaps.sortedBy { it.index }.map { commands.swapLine(proposal.base, it) })
            }
    }
    /** Descriptions of the non-swap actions in the open proposal. */
    fun proposalOther(): List<String> {
        val proposal = assistantProposal ?: return emptyList()
        return proposal.actions.zip(proposal.descriptions).filter { it.first.type != AssistantActionType.swap_recipe }.map { it.second }
    }

    fun undoAssistant() {
        val change = undo.pop(state)
        if (change == null) {
            assistantHistory = assistantHistory + AssistantChatMessage("assistant",
                "Không còn thay đổi nào của trợ lý để hoàn tác, hoặc dữ liệu đã được chỉnh tay sau đó.", source = AssistantSource.SYSTEM)
            return
        }
        update(change.before, "Đã hoàn tác: ${change.label}")
    }

    private fun executeAssistantActions(proposal: AssistantProposal, confirmed: Boolean) {
        val actions = proposal.actions
        val base = proposal.base
        var next = base
        actions.forEach { next = commands.apply(next, it, today, System.currentTimeMillis()) }
        if (actions.any { it.mutates } && next != base) {
            val label = actions.zip(proposal.descriptions).filter { it.first.mutates }
                .joinToString("; ") { it.second.substringAfter('\n').trimEnd('.') }
            val permissions = if ((!notifications || !exactAlarms) && actions.any { it.type in setOf(AssistantActionType.set_reminder, AssistantActionType.start_timer) })
                " Hãy mở Nhắc nấu để kiểm tra quyền thông báo và nhắc đúng giờ." else ""
            val outcome = if (confirmed) "Đã lưu và áp dụng các thay đổi đã xác nhận.$permissions Bạn có thể bấm Hoàn tác nếu muốn trả lại."
                else "Đã làm: $label.$permissions Bấm Hoàn tác hoặc nói “hoàn tác” nếu muốn trả lại."
            undo.push(AssistantChange(base, next, label))
            update(next, outcome)
        }
        actions.forEach { action ->
            when (action.type) {
                AssistantActionType.navigate -> {
                    update(state.copy(date = action.date, meal = action.meal)); navigate(requireNotNull(action.screen))
                }
                AssistantActionType.open_recipe -> {
                    update(state.copy(date = action.date, meal = action.meal))
                    openRecipe(requireNotNull(action.recipeId))
                }
                AssistantActionType.select_meal -> if (action.date in days) update(state.copy(date = action.date, meal = action.meal))
                AssistantActionType.open_sheet -> {
                    update(state.copy(date = action.date, meal = action.meal))
                    if (action.option == "reminder") openReminders() else openSwap(requireNotNull(action.index))
                }
                AssistantActionType.close_assistant -> closeAssistant()
                AssistantActionType.share_shopping_list ->
                    effect = PlannerEffect.Share(shoppingText(action.date, if (action.option == "day") null else action.meal))
                AssistantActionType.add_to_calendar -> effect = PlannerEffect.Calendar(engine.timeline(state, action.date, action.meal))
                AssistantActionType.request_permission ->
                    effect = if (action.option == "exact_alarms") PlannerEffect.ExactAlarms else PlannerEffect.Notifications
                else -> Unit
            }
        }
    }
    fun assistantSuggestions(): List<String> = when {
        assistantRecipeId != null -> listOf("Giải thích cách nấu món này", "Nguyên liệu món này gồm gì?")
        assistantOrigin == Screen.SHOPPING -> listOf("Còn thiếu nguyên liệu gì?", "Mở thực đơn bữa này")
        assistantOrigin == Screen.COOK -> listOf("Nấu món nào trước?", "Nhắc mình chuẩn bị bữa này")
        assistantConfigured -> listOf("Lên thực đơn cả tuần, ăn nhẹ và nấu nhanh", "Tủ lạnh có thịt heo, trứng và rau muống. Tối mai nấu gì?", "Tối mai nấu canh chua cá nhé")
        else -> listOf("Đổi món mặn", "Món nấu nhanh", "Nấu món nào trước?")
    }
    fun dismissSheet() { sheet = null }
    fun dismissTimerReplacement() { pendingTimer = null }
    fun clearMessage(shown: String) { if (message == shown) message = null }
    fun showMessage(text: String) { message = text }

    fun swap(index: Int, id: String) {
        update(engine.swap(state, index, id)); sheet = null
        message = "Đã đổi món. Nguyên liệu và lịch nấu đã cập nhật."
    }
    fun toggleFavorite(id: String) {
        val favorites = if (id in state.favorites) state.favorites - id else state.favorites + id
        update(state.copy(favorites = favorites))
    }
    fun checkIngredient(key: String) {
        val checked = state.checked[state.key].orEmpty().toggle(key)
        update(state.copy(checked = state.checked + (state.key to checked)))
    }
    fun checkTask(key: String) {
        val done = state.done[state.key].orEmpty().toggle(key)
        update(state.copy(done = state.done + (state.key to done)))
    }
    fun saveFamily(name: String, people: Int, lunch: LocalTime, dinner: LocalTime) {
        val changedPeople = people != state.profile.people
        update(state.copy(profile = FamilyProfile(name.trim().take(35).ifBlank { "Nhà mình" }, people.coerceIn(1, 12), lunch, dinner),
            checked = if (changedPeople) emptyMap() else state.checked))
        message = "Đã lưu thông tin gia đình và cập nhật định lượng."
    }
    fun saveReminder(time: LocalTime, enabled: Boolean) {
        val profile = if (state.meal == Meal.LUNCH) state.profile.copy(lunch = time) else state.profile.copy(dinner = time)
        update(state.copy(profile = profile, reminders = if (enabled) state.reminders + state.key else state.reminders - state.key))
        sheet = null
        message = when {
            !enabled -> "Đã tắt nhắc nấu cho bữa này."
            tasks.first().at <= now -> "Giờ bắt đầu đã qua. Bạn hãy chỉnh giờ ăn nếu nấu muộn hơn."
            !notifications -> "Đã lưu lịch. Hãy cho phép thông báo để nhận nhắc nấu."
            else -> "Đã đặt nhắc vào bếp lúc ${PlannerEngine.time(tasks.first().at)}."
        }
    }
    fun startTimer(minutes: Int, label: String, replace: Boolean = false) {
        require(minutes in 1..180)
        if (!replace && state.timer?.end?.let { it > System.currentTimeMillis() } == true) {
            pendingTimer = minutes to label; return
        }
        update(state.copy(timer = CookingTimer(System.currentTimeMillis() + minutes * 60_000L, label, state.date, state.meal)))
        pendingTimer = null; sheet = null
        message = "Đã hẹn $minutes phút. Kiểm tra quyền thông báo và nhắc đúng giờ để được nhắc khi đóng app."
    }
    fun cancelTimer() { update(state.copy(timer = null)); message = "Đã dừng hẹn giờ." }
    fun visibleTick() {
        now = System.currentTimeMillis()
        if (PlannerEngine.today() != today) {
            today = PlannerEngine.today(); update(engine.ensureWeek(state, today))
        }
        state.timer?.takeIf { it.end <= now }?.let { timer ->
            update(state.copy(timer = null)); message = "Hết giờ: ${timer.label}. Hãy kiểm tra món ăn nhé!"
        }
    }
    fun refreshPermissions() {
        notifications = ReminderStore.canNotify(getApplication())
        exactAlarms = ReminderStore.canScheduleExact(getApplication())
        viewModelScope.launch(Dispatchers.IO) { ReminderStore.restore(getApplication()) }
    }
    fun openNotification(intent: Intent) {
        val date = runCatching { LocalDate.parse(intent.getStringExtra("date")) }.getOrNull() ?: return
        val meal = Meal.from(intent.getStringExtra("meal").orEmpty()) ?: return
        if (date in days) update(state.copy(date = date, meal = meal))
        if (assistantVisible) finishAssistant(restoreRecipe = false)
        sheet = null; screen = Screen.COOK
    }
    fun back(): Boolean {
        if (sheet != null) { sheet = null; return true }
        if (assistantVisible) { closeAssistant(); return true }
        if (screen != Screen.TODAY) { screen = Screen.TODAY; return true }
        return false
    }
    /** Shopping list for one meal, or for both meals of [date] when [meal] is null. */
    fun shoppingText(date: LocalDate = state.date, meal: Meal? = state.meal): String {
        val merged = linkedMapOf<String, Ingredient>()
        (meal?.let(::listOf) ?: Meal.entries).forEach { m ->
            engine.ingredients(engine.menu(state, date, m), state.profile.people).forEach { item ->
                merged[item.key] = merged[item.key]?.let { it.copy(amount = it.amount + item.amount) } ?: item
            }
        }
        return "${meal?.title ?: "Cả ngày"} ${PlannerEngine.shortDate(date)} — ${state.profile.people} người\n" +
            merged.values.joinToString("\n") { "□ ${it.name}: ${PlannerEngine.quantity(it.amount)} ${it.unit}" }
    }
}

private val mealChoice = Regex("""(?:minh )?(?:chon|lay|dung|ap dung)\s+mam\s+(?:so\s+)?(1|2|mot|hai|thu nhat|thu hai)(?:\s+(?:nhe|di))?""")
private val undoPhrase = Regex("""(?:hoan tac|tra lai nhu cu|quay lai nhu cu|undo)(?:\s+(?:di|nhe|giup minh|giup toi|giup chau))?""")

private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
