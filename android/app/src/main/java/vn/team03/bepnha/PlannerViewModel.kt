package vn.team03.bepnha

import android.app.Application
import android.content.Intent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.derivedStateOf
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
    var now by mutableLongStateOf(System.currentTimeMillis()); private set
    var message by mutableStateOf<String?>(null); private set
    var notifications by mutableStateOf(ReminderStore.canNotify(application)); private set
    var exactAlarms by mutableStateOf(ReminderStore.canScheduleExact(application)); private set

    // "Hỏi Bếp Nhà" keeps its drafts and conversation while the user moves between tabs.
    var askTab by mutableStateOf(AskTab.FRIDGE); private set
    var fridgeItems by mutableStateOf(listOf<String>()); private set
    var fridgeDraft by mutableStateOf("")
    var fridgeMeal by mutableStateOf(if (state.meal.tray) state.meal else Meal.DINNER); private set
    var chat by mutableStateOf(listOf<ChatEntry>()); private set
    var chatDraft by mutableStateOf("")
    private var speechTarget = AskTab.CHAT
    private val updates = Channel<PlannerState>(Channel.CONFLATED)

    init {
        viewModelScope.launch(Dispatchers.IO) {
            for (snapshot in updates) {
                try {
                    storage.save(snapshot)
                    ReminderStore.synchronize(application, PlannerStorage.alarmJson(engine.alarms(snapshot, System.currentTimeMillis())))
                } catch (exception: Exception) {
                    withContext(Dispatchers.Main) { message = "Chưa lưu được dữ liệu hoặc lịch nhắc. Bạn hãy mở lại ứng dụng để kiểm tra." }
                }
            }
        }
        updates.trySend(state)
    }

    val menu by derivedStateOf { engine.menu(state) }
    val tasks by derivedStateOf { engine.timeline(state) }
    val days by derivedStateOf { (0L..6L).map(today::plusDays) }
    val shoppingMenu by derivedStateOf { engine.shoppingMenu(state) }
    val fridgeTrays by derivedStateOf { engine.suggestTrays(fridgeItems, fridgeMeal) }
    val reminderEnabled get() = state.key in state.reminders

    private fun update(next: PlannerState) { state = next; updates.trySend(next) }
    fun navigate(next: Screen) { screen = next }
    fun selectDate(date: LocalDate) { if (date in days) update(state.copy(date = date)) }
    fun selectMeal(meal: Meal) { update(state.copy(meal = meal)) }
    fun openRecipe(id: String) { sheet = PlannerSheet.RecipeDetail(id) }
    fun openSwap(index: Int) { sheet = PlannerSheet.Swap(index) }
    fun openReminders() { sheet = PlannerSheet.Reminder }
    fun openAsk(tab: AskTab) { askTab = tab; screen = Screen.ASK }
    fun selectAskTab(tab: AskTab) { askTab = tab }
    fun dismissSheet() { sheet = null }
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

    // ---- Shopping ----
    fun toggleShopMeal(meal: Meal) {
        val current = state.shoppingMeals
        val next = if (meal in current) current - meal else current + meal
        if (next.isEmpty()) { message = "Chọn ít nhất một bữa để đi chợ."; return }
        update(state.copy(shopMeals = next))
    }
    fun shopWholeDay() { update(state.copy(shopMeals = Meal.entries.toSet())) }
    fun checkIngredient(key: String) {
        val shop = PlannerEngine.shopKey(state.date)
        update(state.copy(checked = state.checked + (shop to state.checked[shop].orEmpty().toggle(key))))
    }
    val checkedIngredients: Set<String> get() = state.checked[PlannerEngine.shopKey(state.date)].orEmpty()

    fun checkTask(key: String) {
        val done = state.done[state.key].orEmpty().toggle(key)
        update(state.copy(done = state.done + (state.key to done)))
    }
    fun saveFamily(name: String, people: Int, breakfast: LocalTime, lunch: LocalTime, dinner: LocalTime) {
        val changedPeople = people != state.profile.people
        update(state.copy(profile = FamilyProfile(name.trim().take(35).ifBlank { "Nhà mình" }, people.coerceIn(1, 12), lunch, dinner, breakfast),
            checked = if (changedPeople) emptyMap() else state.checked))
        message = "Đã lưu thông tin gia đình và cập nhật định lượng."
    }
    fun saveReminder(time: LocalTime, enabled: Boolean) {
        update(state.copy(profile = state.profile.with(state.meal, time),
            reminders = if (enabled) state.reminders + state.key else state.reminders - state.key))
        sheet = null
        message = when {
            !enabled -> "Đã tắt nhắc nấu cho bữa này."
            tasks.first().at <= now -> "Giờ bắt đầu đã qua. Bạn hãy chỉnh giờ ăn nếu nấu muộn hơn."
            !notifications -> "Đã lưu lịch. Hãy cho phép thông báo để nhận nhắc nấu."
            else -> "Đã đặt nhắc vào bếp lúc ${PlannerEngine.time(tasks.first().at)}."
        }
    }

    // ---- Timers: several dishes can count down at the same time ----
    private fun addTimer(minutes: Int, label: String): String {
        require(minutes in 1..180)
        val clock = System.currentTimeMillis()
        val running = state.timers.filter { it.end > clock }
        val same = running.firstOrNull { it.label == label }
        if (same == null && running.size >= PlannerEngine.MAX_TIMERS) {
            return "Đang chạy ${PlannerEngine.MAX_TIMERS} đồng hồ. Hãy dừng bớt một đồng hồ trước khi hẹn thêm."
        }
        val id = maxOf(clock, (state.timers.maxOfOrNull { it.id } ?: 0L) + 1)
        val timer = CookingTimer(id, clock + minutes * 60_000L, label, state.date, state.meal)
        update(state.copy(timers = running.filter { it !== same } + timer))
        val count = running.size + if (same == null) 1 else 0
        return when {
            same != null -> "Đã đặt lại $minutes phút cho ${label.lowercase(PlannerEngine.locale)}."
            count > 1 -> "Đã hẹn $minutes phút. Đang chạy $count đồng hồ cùng lúc."
            else -> "Đã hẹn $minutes phút. Bạn cứ làm việc khác, hết giờ Bếp Nhà sẽ báo."
        }
    }
    fun startTimer(minutes: Int, label: String) { message = addTimer(minutes, label); sheet = null }
    fun cancelTimer(id: Long) {
        val timer = state.timers.firstOrNull { it.id == id } ?: return
        update(state.copy(timers = state.timers - timer)); message = "Đã dừng hẹn giờ ${timer.label.lowercase(PlannerEngine.locale)}."
    }
    fun visibleTick() {
        now = System.currentTimeMillis()
        if (PlannerEngine.today() != today) {
            today = PlannerEngine.today(); update(engine.ensureWeek(state, today))
        }
        val finished = state.timers.filter { it.end <= now }
        if (finished.isNotEmpty()) {
            update(state.copy(timers = state.timers - finished.toSet()))
            message = "Hết giờ: ${finished.joinToString { it.label }}. Hãy kiểm tra món ăn nhé!"
        }
    }

    // ---- Hỏi Bếp Nhà ----
    fun toggleFridgeItem(item: String) {
        fridgeItems = if (fridgeItems.any { it.equals(item, ignoreCase = true) })
            fridgeItems.filterNot { it.equals(item, ignoreCase = true) } else fridgeItems + item
    }
    fun addFridgeText(text: String) {
        val items = text.split(Regex(",|;|\\.|\\s+và\\s+|\\s+với\\s+|\\n", RegexOption.IGNORE_CASE))
            .map { it.trim().replaceFirstChar { c -> c.titlecase(PlannerEngine.locale) } }.filter { it.length >= 2 }
        fridgeItems = (fridgeItems + items).distinctBy { PlannerEngine.words(it) }.take(16)
        fridgeDraft = ""
    }
    fun clearFridge() { fridgeItems = emptyList() }
    fun selectFridgeMeal(meal: Meal) { fridgeMeal = meal }
    fun openFridgeWith(item: String) {
        if (fridgeItems.none { it.equals(item, ignoreCase = true) }) fridgeItems = fridgeItems + item
        openAsk(AskTab.FRIDGE)
    }
    fun applyTray(date: LocalDate, meal: Meal, ids: List<String>) {
        if (date !in days) return
        update(engine.setPlan(state, date, meal, ids).copy(date = date, meal = meal))
        message = "Đã chọn ${engine.recipes.getValue(ids[0]).name.lowercase(PlannerEngine.locale)} cho ${meal.title.lowercase(PlannerEngine.locale)} " +
            "${PlannerEngine.dayLabel(date, today)}. Danh sách đi chợ đã cập nhật."
    }
    fun applyChange(change: MenuChange) {
        if (change.date !in days) return
        val recipe = engine.recipes.getValue(change.recipeId)
        update(engine.setDish(state, change.date, change.meal, change.recipeId).copy(date = change.date, meal = change.meal))
        message = "Đã thêm ${recipe.name.lowercase(PlannerEngine.locale)} vào ${change.meal.title.lowercase(PlannerEngine.locale)} " +
            "${PlannerEngine.dayLabel(change.date, today)}."
    }
    fun isPlanned(date: LocalDate, meal: Meal, ids: List<String>) = state.plans[PlannerEngine.key(date, meal)]?.containsAll(ids) == true

    fun ask(text: String) {
        val question = text.trim().take(300)
        if (question.isBlank()) return
        var answer = engine.answer(question, state, today)
        answer.timerMinutes?.let { minutes -> answer = answer.copy(text = addTimer(minutes, answer.timerLabel)) }
        chat = (chat + ChatEntry(question, answer)).takeLast(30)
        chatDraft = ""
    }
    fun clearChat() { chat = emptyList() }
    fun listenFor(target: AskTab) { speechTarget = target }
    fun receiveSpeech(text: String) {
        if (text.isBlank()) return
        if (speechTarget == AskTab.FRIDGE) addFridgeText(text) else ask(text)
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
        if (screen != Screen.TODAY) { screen = Screen.TODAY; return true }
        return false
    }
    fun shoppingText(): String {
        val meals = state.shoppingMeals.sorted()
        val title = if (meals.size == Meal.entries.size) "Cả ngày" else meals.joinToString(" + ") { it.title }
        return "$title ${PlannerEngine.shortDate(state.date)} — ${state.profile.people} người\n" +
            engine.ingredients(shoppingMenu, state.profile.people).joinToString("\n") { "□ ${it.name}: ${PlannerEngine.quantity(it.amount)} ${it.unit}" }
    }
}

private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
