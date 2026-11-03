package vn.team03.bepnha

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class PlannerViewModel(application: Application) : AndroidViewModel(application) {
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
    var assistantQuestion by mutableStateOf("")
    var assistantBusy by mutableStateOf(false); private set
    var assistantError by mutableStateOf<String?>(null); private set
    var assistantProposal by mutableStateOf<AssistantProposal?>(null); private set
    var assistantRecipeId by mutableStateOf<String?>(null); private set
    private var assistantOrigin = Screen.TODAY
    internal var assistantTransport: (String, List<AssistantChatMessage>, org.json.JSONObject) -> AssistantReply = AssistantGateway::ask
    private val commands = AssistantCommands(engine)
    private val updates = Channel<Pair<PlannerState, String?>>(Channel.UNLIMITED)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for ((snapshot, outcome) in updates) {
                try {
                    storage.save(snapshot)
                    ReminderStore.synchronize(application, PlannerStorage.alarmJson(engine.alarms(snapshot, System.currentTimeMillis())))
                    if (outcome != null) withContext(Dispatchers.Main) { assistantHistory = assistantHistory + AssistantChatMessage("assistant", outcome) }
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

    private fun update(next: PlannerState, outcome: String? = null) { state = next; updates.trySend(next to outcome) }
    fun navigate(next: Screen) { screen = next; assistantVisible = false }
    fun selectDate(date: LocalDate) { if (date in days) update(state.copy(date = date)) }
    fun selectMeal(meal: Meal) { update(state.copy(meal = meal)) }
    fun openRecipe(id: String) { sheet = PlannerSheet.RecipeDetail(id) }
    fun openSwap(index: Int) { sheet = PlannerSheet.Swap(index) }
    fun openReminders() { sheet = PlannerSheet.Reminder }
    fun openAssistant() {
        assistantRecipeId = (sheet as? PlannerSheet.RecipeDetail)?.id
        assistantOrigin = screen; sheet = null; assistantVisible = true
    }
    fun closeAssistant() { assistantVisible = false }
    fun cancelAssistantProposal() {
        assistantProposal = null
        assistantHistory = assistantHistory + AssistantChatMessage("assistant", "Đã hủy đề xuất. Dữ liệu được giữ nguyên.")
    }
    fun askAssistant(question: String = assistantQuestion) {
        if (question.isBlank() || assistantBusy || assistantProposal != null) return
        val prompt = question.trim().take(1000)
        val base = state
        val context = AssistantGateway.snapshot(engine, base, today, assistantOrigin, assistantRecipeId)
        val history = assistantHistory
        assistantHistory = history + AssistantChatMessage("user", prompt)
        assistantQuestion = prompt; assistantBusy = true; assistantError = null
        viewModelScope.launch {
            try {
                val reply = withContext(Dispatchers.IO) { assistantTransport(prompt, history, context) }
                require(state == base && assistantVisible) { "Ngữ cảnh đã thay đổi. Hãy gửi lại câu hỏi cho bữa đang chọn." }
                var simulated = state
                reply.actions.forEach { simulated = commands.apply(simulated, it, today, System.currentTimeMillis()) }
                assistantHistory = assistantHistory + AssistantChatMessage("assistant", reply.text.take(8000))
                assistantQuestion = ""
                if (reply.actions.any { it.mutates }) {
                    var previewState = base
                    val descriptions = reply.actions.map { action ->
                        val description = commands.preview(previewState, action)
                        previewState = commands.apply(previewState, action, today, System.currentTimeMillis())
                        description
                    }
                    assistantProposal = AssistantProposal(base, reply.actions, descriptions)
                } else executeAssistantActions(reply.actions, base)
            } catch (exception: Exception) {
                assistantError = exception.message ?: "Chưa kết nối được trợ lý. Hãy thử lại hoặc tra cứu offline."
            } finally { assistantBusy = false }
        }
    }
    fun lookupOffline() {
        if (assistantQuestion.isBlank() || assistantBusy || assistantProposal != null) return
        val prompt = assistantQuestion
        val answer = engine.answer(prompt, state)
        assistantHistory = assistantHistory + AssistantChatMessage("user", prompt) + AssistantChatMessage("assistant",
            "Tra cứu offline\n${answer.text}\n" + answer.recipes.mapNotNull { engine.recipes[it]?.name }.joinToString("\n"), answer.recipes)
        assistantQuestion = ""; assistantError = null
    }
    fun confirmAssistantProposal() {
        val proposal = assistantProposal ?: return
        assistantProposal = null
        try {
            require(state == proposal.base) { "Dữ liệu đã thay đổi. Hãy gửi lại câu hỏi để xem đề xuất mới." }
            executeAssistantActions(proposal.actions, proposal.base)
        } catch (exception: Exception) {
            assistantQuestion = assistantHistory.lastOrNull { it.role == "user" }?.text.orEmpty()
            assistantError = exception.message ?: "Không thể thực hiện thao tác."
        }
    }
    private fun executeAssistantActions(actions: List<AssistantAction>, base: PlannerState) {
        var next = base
        actions.forEach { next = commands.apply(next, it, today, System.currentTimeMillis()) }
        if (actions.any { it.mutates }) {
            update(next, "Đã lưu và áp dụng các thay đổi đã xác nhận." +
                if ((!notifications || !exactAlarms) && actions.any { it.type in setOf(AssistantActionType.set_reminder, AssistantActionType.start_timer) }) " Hãy mở Nhắc nấu để kiểm tra quyền thông báo và nhắc đúng giờ." else "")
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
                else -> Unit
            }
        }
    }
    fun assistantSuggestions(): List<String> = when {
        assistantRecipeId != null -> listOf("Giải thích cách nấu món này", "Nguyên liệu món này gồm gì?")
        assistantOrigin == Screen.SHOPPING -> listOf("Còn thiếu nguyên liệu gì?", "Mở thực đơn bữa này")
        assistantOrigin == Screen.COOK -> listOf("Nấu món nào trước?", "Nhắc mình chuẩn bị bữa này")
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
        sheet = null; screen = Screen.COOK
    }
    fun back(): Boolean {
        if (sheet != null) { sheet = null; return true }
        if (assistantVisible) { closeAssistant(); return true }
        if (screen != Screen.TODAY) { screen = Screen.TODAY; return true }
        return false
    }
    fun shoppingText(): String = "${state.meal.title} ${PlannerEngine.shortDate(state.date)} — ${state.profile.people} người\n" +
        engine.ingredients(menu, state.profile.people).joinToString("\n") { "□ ${it.name}: ${PlannerEngine.quantity(it.amount)} ${it.unit}" }
}

private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
