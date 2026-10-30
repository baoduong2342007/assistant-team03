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

    val menu get() = engine.menu(state)
    val tasks get() = engine.timeline(state)
    val days get() = (0L..6L).map(today::plusDays)
    val reminderEnabled get() = state.key in state.reminders

    private fun update(next: PlannerState) { state = next; updates.trySend(next) }
    fun navigate(next: Screen) { screen = next }
    fun selectDate(date: LocalDate) { if (date in days) update(state.copy(date = date)) }
    fun selectMeal(meal: Meal) { update(state.copy(meal = meal)) }
    fun openRecipe(id: String) { sheet = PlannerSheet.RecipeDetail(id) }
    fun openSwap(index: Int) { sheet = PlannerSheet.Swap(index) }
    fun openReminders() { sheet = PlannerSheet.Reminder }
    fun openAssistant() { sheet = PlannerSheet.Assistant }
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
        if (screen != Screen.TODAY) { screen = Screen.TODAY; return true }
        return false
    }
    fun shoppingText(): String = "${state.meal.title} ${PlannerEngine.shortDate(state.date)} — ${state.profile.people} người\n" +
        engine.ingredients(menu, state.profile.people).joinToString("\n") { "□ ${it.name}: ${PlannerEngine.quantity(it.amount)} ${it.unit}" }
}

private fun Set<String>.toggle(value: String) = if (value in this) this - value else this + value
