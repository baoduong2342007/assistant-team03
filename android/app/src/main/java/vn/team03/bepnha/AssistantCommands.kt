package vn.team03.bepnha

import java.time.LocalDate
import java.time.LocalTime

enum class AssistantActionType {
    navigate, open_recipe, swap_recipe, set_people, set_meal_time, set_reminder, start_timer, cancel_timer,
    set_ingredient, set_task, set_favorite,
    select_meal, open_sheet, close_assistant, share_shopping_list, add_to_calendar, request_permission, set_family_name,
    set_diet_tag, set_avoid, set_meal_limits, start_cook_along,
}

/**
 * READ runs at once and changes no saved data. LOW is applied at once and can be undone.
 * HIGH is shown as a preview and needs the user's confirmation.
 */
enum class AssistantRisk { READ, LOW, HIGH }

data class AssistantAction(
    val type: AssistantActionType, val date: LocalDate, val meal: Meal,
    val screen: Screen? = null, val recipeId: String? = null, val index: Int? = null,
    val people: Int? = null, val time: LocalTime? = null, val enabled: Boolean? = null,
    val minutes: Int? = null, val label: String? = null, val key: String? = null,
    val option: String? = null, val name: String? = null, val cost: Int? = null,
) {
    val risk: AssistantRisk get() = when (type) {
        AssistantActionType.swap_recipe, AssistantActionType.set_people, AssistantActionType.set_meal_time,
        AssistantActionType.set_family_name, AssistantActionType.set_diet_tag, AssistantActionType.set_avoid,
        AssistantActionType.set_meal_limits -> AssistantRisk.HIGH
        AssistantActionType.set_ingredient, AssistantActionType.set_task, AssistantActionType.set_favorite,
        AssistantActionType.start_timer, AssistantActionType.cancel_timer, AssistantActionType.set_reminder -> AssistantRisk.LOW
        else -> AssistantRisk.READ
    }
    val mutates get() = risk != AssistantRisk.READ
    /** Global actions apply to the whole family, not to one meal in the seven-day window. */
    val global get() = type in GLOBAL
    fun targetsSameMealAs(other: AssistantAction) = date == other.date && meal == other.meal

    companion object {
        val GLOBAL = setOf(
            AssistantActionType.set_people, AssistantActionType.set_family_name, AssistantActionType.set_favorite,
            AssistantActionType.cancel_timer, AssistantActionType.close_assistant, AssistantActionType.request_permission,
            AssistantActionType.set_diet_tag, AssistantActionType.set_avoid, AssistantActionType.set_meal_limits,
        )
        const val MAX_ACTIONS = 48
        val SHEETS = setOf("swap_options", "reminder")
        val PERMISSIONS = setOf("notifications", "exact_alarms")
        val SHOPPING_SCOPES = setOf("meal", "day")
    }
}

/** Pure validation/reducer shared by preview and execution. Never toggles state. */
class AssistantCommands(private val engine: PlannerEngine) {
    private val rules by lazy { MealPlanning(engine.recipes) }

    /** An allergen code ("tôm" → shrimp) or the canonical ingredient names a spoken term means. */
    fun resolveAvoid(term: String): Pair<String?, Set<String>> {
        val plain = MealPlanning.normalize(term)
        DietProfile.ALLERGEN_LABELS.entries.firstOrNull { it.key == term || MealPlanning.normalize(it.value) == plain }?.let { return it.key to emptySet() }
        // Exact names and aliases first; then a general word covers its kinds ("hành" → Hành lá, Hành tím).
        val names = rules.ingredientNames(listOf(term)).ifEmpty {
            engine.recipes.values.flatMap { it.ingredients }.map { it.name }.distinct()
                .filter { plain.isNotEmpty() && MealPlanning.normalize(it).startsWith("$plain ") }.toSet()
        }
        require(names.isNotEmpty()) { "Không có nguyên liệu “$term” trong kho công thức; dùng tên nguyên liệu có trong công thức." }
        return null to names
    }

    /** Replacing a running timer cannot be undone safely, so it needs confirmation. */
    fun risk(state: PlannerState, action: AssistantAction, now: Long): AssistantRisk =
        if (action.type == AssistantActionType.start_timer && state.timers.any {
            it.end > now && it.label == action.label && it.date == action.date && it.meal == action.meal
        }) AssistantRisk.HIGH
        else action.risk

    fun proposal(state: PlannerState, actions: List<AssistantAction>, today: LocalDate, now: Long): AssistantProposal {
        require(actions.size <= AssistantAction.MAX_ACTIONS) { "Đề xuất có quá nhiều thao tác." }
        val slots = actions.filter { it.type == AssistantActionType.swap_recipe }.map { Triple(it.date, it.meal, it.index) }
        require(slots.distinct().size == slots.size) { "Một vị trí món có nhiều thay đổi." }
        var simulated = state
        val effective = mutableListOf<AssistantAction>()
        val descriptions = mutableListOf<String>()
        for (action in actions) {
            val next = apply(simulated, action, today, now)
            if (action.type != AssistantActionType.swap_recipe || next != simulated) {
                effective.add(action)
                descriptions.add(preview(simulated, action))
            }
            simulated = next
        }
        return AssistantProposal(state, effective, descriptions,
            effective.any { risk(state, it, now) == AssistantRisk.HIGH })
    }

    fun reviseMenu(previous: AssistantProposal, changes: List<AssistantAction>): List<AssistantAction> {
        val first = previous.actions.first()
        if (changes.isEmpty() || !changes.all { it.type == AssistantActionType.swap_recipe && it.targetsSameMealAs(first) }) return changes
        val replaced = changes.map { it.index }.toSet()
        return (previous.actions.filterNot { it.index in replaced } + changes).sortedBy { it.index }
    }

    fun actionsFor(state: PlannerState, suggestion: AssistantMealSuggestion, today: LocalDate): List<AssistantAction> {
        val current = requireMeal(state, suggestion.date, suggestion.meal, today)
        require(engine.validPlan(suggestion.recipeIds, suggestion.meal)) { "Mâm gợi ý có món hoặc nhóm món chưa hợp lệ." }
        val recipes = suggestion.recipeIds.map(engine.recipes::getValue)
        val keys = engine.ingredients(recipes, state.profile.people).map { it.key }.toSet()
        require(suggestion.pantryKeys.isNotEmpty() && suggestion.pantryKeys.all { it in keys }) {
            "Nguyên liệu có sẵn chưa khớp với mâm gợi ý."
        }
        return swapsTo(current, suggestion.recipeIds, suggestion.date, suggestion.meal)
    }

    /** Swap actions that turn [current] into [target] for one meal; unchanged slots are skipped. */
    fun swapsTo(current: List<String>, target: List<String>, date: LocalDate, meal: Meal): List<AssistantAction> =
        target.mapIndexedNotNull { index, id ->
            if (id == current[index]) null
            else AssistantAction(AssistantActionType.swap_recipe, date, meal, recipeId = id, index = index)
        }

    private fun requireMeal(state: PlannerState, date: LocalDate, meal: Meal, today: LocalDate): List<String> {
        require(date in today..today.plusDays(6)) { "Ngày ${date} nằm ngoài thực đơn bảy ngày (từ $today đến ${today.plusDays(6)})." }
        val ids = state.plans[PlannerEngine.key(date, meal)].orEmpty()
        require(engine.validPlan(ids, meal)) { "Không tìm thấy bữa ăn." }
        return ids
    }

    fun apply(state: PlannerState, action: AssistantAction, today: LocalDate, now: Long): PlannerState {
        val current = if (action.global) emptyList() else requireMeal(state, action.date, action.meal, today)
        val key = PlannerEngine.key(action.date, action.meal)
        fun flag(set: Set<String>, item: String): Set<String> =
            if (requireNotNull(action.enabled) { "Thiếu trạng thái bật/tắt." }) set + item else set - item
        return when (action.type) {
            AssistantActionType.navigate -> { requireNotNull(action.screen) { "Thiếu màn hình cần mở." }; state }
            AssistantActionType.open_recipe -> { require(action.recipeId in engine.recipes) { "Không có công thức ${action.recipeId}." }; state }
            AssistantActionType.select_meal, AssistantActionType.close_assistant, AssistantActionType.add_to_calendar,
            AssistantActionType.start_cook_along -> state
            AssistantActionType.open_sheet -> {
                require(action.option in AssistantAction.SHEETS) { "Sheet phải là swap_options hoặc reminder." }
                if (action.option == "swap_options") require(action.index in current.indices) { "Vị trí món phải là 0, 1 hoặc 2." }
                state
            }
            AssistantActionType.share_shopping_list -> {
                require(action.option in AssistantAction.SHOPPING_SCOPES) { "Phạm vi danh sách phải là meal hoặc day." }
                state
            }
            AssistantActionType.request_permission -> {
                require(action.option in AssistantAction.PERMISSIONS) { "Quyền phải là notifications hoặc exact_alarms." }
                state
            }
            AssistantActionType.swap_recipe -> {
                val index = requireNotNull(action.index) { "Thiếu vị trí món." }
                require(index in current.indices) { "Vị trí món phải là 0, 1 hoặc 2." }
                val recipe = engine.recipes[action.recipeId]
                requireNotNull(recipe) { "Không có công thức ${action.recipeId} trong kho." }
                require(recipe.kind == engine.recipes.getValue(current[index]).kind) { "${recipe.name} là ${recipe.kind.label.lowercase()}, không đặt được vào vị trí $index." }
                if (current[index] == recipe.id) return state
                val changed = engine.swap(state.copy(date = action.date, meal = action.meal), index, recipe.id)
                changed.copy(date = state.date, meal = state.meal)
            }
            AssistantActionType.set_people -> {
                val people = requireNotNull(action.people) { "Thiếu số người." }; require(people in 1..12) { "Số người phải từ 1 đến 12." }
                state.copy(profile = state.profile.copy(people = people), checked = if (people == state.profile.people) state.checked else emptyMap())
            }
            AssistantActionType.set_diet_tag -> {
                val tag = requireNotNull(action.key) { "Thiếu key (tag)." }
                require(tag in Recipe.TAGS) { "Tag phải là một trong ${Recipe.TAGS.joinToString()}." }
                state.copy(diet = state.diet.copy(tags = flag(state.diet.tags, tag)))
            }
            AssistantActionType.set_avoid -> {
                val (allergen, names) = resolveAvoid(requireNotNull(action.key) { "Thiếu key (nguyên liệu cần tránh)." })
                state.copy(diet = if (allergen != null) state.diet.copy(allergens = flag(state.diet.allergens, allergen))
                    else state.diet.copy(avoid = names.fold(state.diet.avoid) { set, name -> flag(set, name) }))
            }
            AssistantActionType.set_meal_limits -> {
                require(action.minutes != null || action.cost != null) { "set_meal_limits cần minutes hoặc cost (0 để bỏ giới hạn)." }
                action.minutes?.let { require(it == 0 || it in 10..240) { "minutes phải là 0 hoặc từ 10 đến 240." } }
                action.cost?.let { require(it == 0 || it in 1_000..10_000_000) { "cost phải là 0 hoặc từ 1.000 đến 10.000.000." } }
                state.copy(diet = state.diet.copy(
                    maxMinutes = action.minutes?.let { it.takeIf { m -> m > 0 } } ?: state.diet.maxMinutes.takeIf { action.minutes == null },
                    maxCost = action.cost?.let { it.toLong().takeIf { c -> c > 0 } } ?: state.diet.maxCost.takeIf { action.cost == null },
                ))
            }
            AssistantActionType.set_family_name -> {
                val name = requireNotNull(action.name) { "Thiếu tên gia đình." }.trim()
                require(name.isNotEmpty() && name.length <= 35) { "Tên gia đình cần từ 1 đến 35 ký tự." }
                state.copy(profile = state.profile.copy(name = name))
            }
            AssistantActionType.set_meal_time -> {
                val time = requireNotNull(action.time) { "Thiếu giờ ăn." }
                state.copy(profile = state.profile.with(action.meal, time))
            }
            AssistantActionType.set_reminder -> {
                if (action.enabled == true) require(engine.timeline(state, action.date, action.meal).first().at > now) { "Giờ chuẩn bị đã qua. Hãy chỉnh giờ ăn trước." }
                state.copy(reminders = flag(state.reminders, key))
            }
            AssistantActionType.start_timer -> {
                val minutes = requireNotNull(action.minutes) { "Thiếu số phút." }; require(minutes in 1..180) { "Hẹn giờ từ 1 đến 180 phút." }
                val label = requireNotNull(action.label) { "Thiếu tên hẹn giờ." }; require(label.isNotBlank() && label.length <= 200)
                engine.startTimer(state, now, minutes, label, action.date, action.meal)
            }
            AssistantActionType.cancel_timer -> {
                val running = state.timers.filter { it.end > now }
                require(running.size <= 1) { "Đang chạy nhiều đồng hồ. Hãy mở Hẹn giờ và chọn đồng hồ muốn dừng." }
                state.copy(timers = state.timers - running.toSet())
            }
            AssistantActionType.set_ingredient -> {
                val ingredient = requireNotNull(action.key) { "Thiếu key nguyên liệu." }
                require(engine.ingredients(engine.menu(state, action.date, action.meal), state.profile.people).any { it.key == ingredient }) { "Nguyên liệu không thuộc bữa này." }
                val shop = PlannerEngine.shopKey(action.date)
                val checkedKey = if (shop in state.checked) shop else key
                state.copy(checked = state.checked + (checkedKey to flag(state.checked[checkedKey].orEmpty(), ingredient)))
            }
            AssistantActionType.set_task -> {
                val task = requireNotNull(action.key) { "Thiếu key bước nấu." }
                require(engine.timeline(state, action.date, action.meal).any { it.key == task }) { "Bước nấu không thuộc bữa này." }
                state.copy(done = state.done + (key to flag(state.done[key].orEmpty(), task)))
            }
            AssistantActionType.set_favorite -> {
                val id = requireNotNull(action.recipeId) { "Thiếu món." }; require(id in engine.recipes) { "Không có công thức $id." }
                state.copy(favorites = flag(state.favorites, id))
            }
        }
    }

    /** One short sentence per action, suitable for reading aloud. */
    fun preview(state: PlannerState, a: AssistantAction): String {
        val target = "${a.meal.title} · ${PlannerEngine.fullDate(a.date)}"
        val name = engine.recipes[a.recipeId]?.name.orEmpty()
        val flag = if (a.enabled == true) "Bật" else "Tắt"
        val text = when (a.type) {
            AssistantActionType.navigate -> "Mở ${a.screen?.title}"
            AssistantActionType.open_recipe -> "Xem công thức $name"
            AssistantActionType.select_meal -> "Chọn ${a.meal.title.lowercase()}"
            AssistantActionType.open_sheet -> if (a.option == "reminder") "Mở phần nhắc nấu" else "Mở danh sách món có thể thay"
            AssistantActionType.close_assistant -> "Đóng trợ lý"
            AssistantActionType.share_shopping_list -> if (a.option == "day") "Chia sẻ danh sách đi chợ cả ngày" else "Chia sẻ danh sách đi chợ bữa này"
            AssistantActionType.add_to_calendar -> "Thêm lịch nấu vào lịch điện thoại"
            AssistantActionType.start_cook_along -> "Bắt đầu nấu cùng trợ lý, đọc to từng bước"
            AssistantActionType.request_permission -> if (a.option == "exact_alarms") "Mở phần cho phép nhắc đúng giờ" else "Mở phần cho phép thông báo"
            AssistantActionType.swap_recipe -> "${swapLine(state, a)}. Nguyên liệu và lịch nấu sẽ cập nhật."
            AssistantActionType.set_people -> "Đổi khẩu phần từ ${state.profile.people} thành ${a.people} người cho mọi bữa. Dấu đã có trong danh sách đi chợ sẽ được xóa."
            AssistantActionType.set_family_name -> "Đổi tên gia đình thành “${a.name?.trim()}”"
            AssistantActionType.set_diet_tag -> "${if (a.enabled == true) "Luôn chọn món" else "Thôi yêu cầu món"} ${DietProfile.TAG_LABELS[a.key] ?: a.key} cho mọi bữa"
            AssistantActionType.set_avoid -> "${if (a.enabled == true) "Luôn tránh" else "Thôi tránh"} ${a.key?.trim()} trong mọi món"
            AssistantActionType.set_meal_limits -> listOfNotNull(
                a.minutes?.let { if (it == 0) "Bỏ giới hạn thời gian nấu" else "Mỗi bữa nấu tối đa $it phút" },
                a.cost?.let { if (it == 0) "Bỏ giới hạn tiền chợ" else "Mỗi bữa tối đa ${PlannerEngine.money(it.toLong())}" },
            ).joinToString("; ")
            AssistantActionType.set_meal_time -> "Đổi giờ ăn ${a.meal.title.lowercase()} từ ${state.profile.time(a.meal)} thành ${a.time} cho mọi ngày."
            AssistantActionType.set_reminder -> "$flag nhắc chuẩn bị bữa ăn."
            AssistantActionType.start_timer -> "Hẹn ${a.minutes} phút: ${a.label}." + state.timers.firstOrNull { it.label == a.label && it.date == a.date && it.meal == a.meal }
                ?.let { " Đặt lại đồng hồ đang có: ${it.label}." }.orEmpty()
            AssistantActionType.cancel_timer -> "Dừng đồng hồ: ${state.timers.singleOrNull()?.label ?: "không có"}."
            AssistantActionType.set_ingredient -> "${if (a.enabled == true) "Đánh dấu đã có" else "Bỏ đánh dấu"}: ${a.key?.substringBefore('|')}"
            AssistantActionType.set_task -> "${if (a.enabled == true) "Đánh dấu đã xong" else "Bỏ đánh dấu"}: ${engine.timeline(state, a.date, a.meal).first { it.key == a.key }.title}"
            AssistantActionType.set_favorite -> "${if (a.enabled == true) "Lưu" else "Bỏ lưu"} món $name"
        }
        return if (a.global) text else "$target\n$text"
    }

    /** "Món canh: Canh bí → Canh chua cá" */
    fun swapLine(state: PlannerState, a: AssistantAction): String {
        val index = requireNotNull(a.index)
        val old = engine.menu(state, a.date, a.meal)[index]
        return "${old.kind.label}: ${old.name} → ${engine.recipes[a.recipeId]?.name.orEmpty()}"
    }
}
