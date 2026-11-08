package vn.team03.bepnha

import java.time.LocalDate
import java.time.LocalTime

enum class AssistantActionType { navigate, open_recipe, swap_recipe, set_people, set_meal_time, set_reminder, start_timer, cancel_timer, set_ingredient, set_task, set_favorite }

data class AssistantAction(
    val type: AssistantActionType, val date: LocalDate, val meal: Meal,
    val screen: Screen? = null, val recipeId: String? = null, val index: Int? = null,
    val people: Int? = null, val time: LocalTime? = null, val enabled: Boolean? = null,
    val minutes: Int? = null, val label: String? = null, val key: String? = null,
) {
    val mutates get() = type !in setOf(AssistantActionType.navigate, AssistantActionType.open_recipe)
}

/** Pure validation/reducer shared by preview and execution. Never toggles state. */
class AssistantCommands(private val engine: PlannerEngine) {
    fun apply(state: PlannerState, action: AssistantAction, today: LocalDate, now: Long): PlannerState {
        require(action.date in today..today.plusDays(6)) { "Ngày nằm ngoài thực đơn bảy ngày." }
        val key = PlannerEngine.key(action.date, action.meal)
        require(engine.validPlan(state.plans[key].orEmpty())) { "Không tìm thấy bữa ăn." }
        fun flag(set: Set<String>, item: String): Set<String> = if (requireNotNull(action.enabled)) set + item else set - item
        return when (action.type) {
            AssistantActionType.navigate -> { requireNotNull(action.screen); state }
            AssistantActionType.open_recipe -> { require(action.recipeId in engine.recipes); state }
            AssistantActionType.swap_recipe -> {
                val index = requireNotNull(action.index); require(index in 0..2)
                val target = state.copy(date = action.date, meal = action.meal)
                if (engine.menu(target)[index].id == action.recipeId) return state
                val changed = engine.swap(target, index, requireNotNull(action.recipeId))
                changed.copy(date = state.date, meal = state.meal)
            }
            AssistantActionType.set_people -> {
                val people = requireNotNull(action.people); require(people in 1..12)
                state.copy(profile = state.profile.copy(people = people), checked = if (people == state.profile.people) state.checked else emptyMap())
            }
            AssistantActionType.set_meal_time -> {
                val time = requireNotNull(action.time)
                state.copy(profile = if (action.meal == Meal.LUNCH) state.profile.copy(lunch = time) else state.profile.copy(dinner = time))
            }
            AssistantActionType.set_reminder -> {
                if (action.enabled == true) require(engine.timeline(state, action.date, action.meal).first().at > now) { "Giờ chuẩn bị đã qua. Hãy chỉnh giờ ăn trước." }
                state.copy(reminders = flag(state.reminders, key))
            }
            AssistantActionType.start_timer -> {
                val minutes = requireNotNull(action.minutes); require(minutes in 1..180)
                val label = requireNotNull(action.label); require(label.isNotBlank() && label.length <= 200)
                state.copy(timer = CookingTimer(now + minutes * 60_000L, label, action.date, action.meal))
            }
            AssistantActionType.cancel_timer -> state.copy(timer = null)
            AssistantActionType.set_ingredient -> {
                val ingredient = requireNotNull(action.key)
                require(engine.ingredients(engine.menu(state, action.date, action.meal), state.profile.people).any { it.key == ingredient }) { "Nguyên liệu không thuộc bữa này." }
                state.copy(checked = state.checked + (key to flag(state.checked[key].orEmpty(), ingredient)))
            }
            AssistantActionType.set_task -> {
                val task = requireNotNull(action.key)
                require(engine.timeline(state, action.date, action.meal).any { it.key == task })
                state.copy(done = state.done + (key to flag(state.done[key].orEmpty(), task)))
            }
            AssistantActionType.set_favorite -> {
                val id = requireNotNull(action.recipeId); require(id in engine.recipes)
                state.copy(favorites = flag(state.favorites, id))
            }
        }
    }

    fun preview(state: PlannerState, a: AssistantAction): String {
        val target = "${a.meal.title} · ${PlannerEngine.fullDate(a.date)}"
        val name = engine.recipes[a.recipeId]?.name.orEmpty()
        val flag = if (a.enabled == true) "Bật" else "Tắt"
        val text = when (a.type) {
            AssistantActionType.navigate -> "Mở ${a.screen?.title}"
            AssistantActionType.open_recipe -> "Xem công thức $name"
            AssistantActionType.swap_recipe -> "Thay ${engine.menu(state, a.date, a.meal)[requireNotNull(a.index)].name} bằng $name. Nguyên liệu và lịch nấu sẽ cập nhật; tiến độ nấu bữa này được đặt lại."
            AssistantActionType.set_people -> "Đổi khẩu phần toàn gia đình từ ${state.profile.people} thành ${a.people} người. Định lượng mọi bữa cập nhật, danh sách nguyên liệu đã tích sẽ được xóa."
            AssistantActionType.set_meal_time -> "Đổi giờ ăn ${a.meal.title.lowercase()} từ ${state.profile.time(a.meal)} thành ${a.time} cho mọi ngày. Các lịch nhắc liên quan sẽ cập nhật."
            AssistantActionType.set_reminder -> "$flag nhắc chuẩn bị bữa ăn theo lịch nấu. Cần quyền thông báo và nhắc đúng giờ trên Android."
            AssistantActionType.start_timer -> "Hẹn ${a.minutes} phút: ${a.label}." + if (state.timer != null) " Thay đồng hồ đang có: ${state.timer.label}." else ""
            AssistantActionType.cancel_timer -> "Dừng đồng hồ đang có: ${state.timer?.label ?: "không có"}."
            AssistantActionType.set_ingredient -> "${if (a.enabled == true) "Đánh dấu đã có" else "Bỏ đánh dấu"}: ${a.key?.substringBefore('|')}"
            AssistantActionType.set_task -> "${if (a.enabled == true) "Đánh dấu đã xong" else "Bỏ đánh dấu"}: ${engine.timeline(state, a.date, a.meal).first { it.key == a.key }.title}"
            AssistantActionType.set_favorite -> "${if (a.enabled == true) "Lưu" else "Bỏ lưu"} món $name"
        }
        return "$target\n$text"
    }
}
