package vn.team03.bepnha

import java.text.Normalizer
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

/** Native domain logic, independent of Android views, WebView and JavaScript. */
class PlannerEngine(val recipes: Map<String, Recipe>, val combinations: List<List<String>>) {
    companion object {
        val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
        val locale: Locale = Locale.forLanguageTag("vi-VN")
        fun key(date: LocalDate, meal: Meal) = "$date|${meal.value}"
        fun today() = LocalDate.now(zone)
        fun time(at: Long): String = DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(Instant.ofEpochMilli(at))
        fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d/M", locale))
        fun fullDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", locale))
        fun money(value: Long): String = NumberFormat.getCurrencyInstance(locale).apply { maximumFractionDigits = 0 }.format(value)
        fun quantity(value: Double): String = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(value)
        fun normalize(text: String): String = Normalizer.normalize(text.lowercase(locale), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "").replace('đ', 'd')
    }

    init {
        require(recipes.isNotEmpty() && combinations.isNotEmpty())
        require(combinations.all(::validPlan))
    }

    fun validPlan(ids: List<String>) = ids.size == 3 && ids.map { recipes[it]?.kind } ==
        listOf(RecipeKind.MAIN, RecipeKind.SOUP, RecipeKind.VEG)

    fun ensureWeek(state: PlannerState, today: LocalDate): PlannerState {
        val plans = state.plans.toMutableMap()
        repeat(7) { day -> Meal.entries.forEach { meal ->
            val key = key(today.plusDays(day.toLong()), meal)
            if (!validPlan(plans[key].orEmpty())) {
                plans[key] = combinations[(day + if (meal == Meal.LUNCH) 3 else 0) % combinations.size].toList()
            }
        } }
        val selected = state.date.takeIf { it >= today && it <= today.plusDays(6) } ?: today
        return state.copy(plans = plans, date = selected)
    }

    fun menu(state: PlannerState, date: LocalDate = state.date, meal: Meal = state.meal): List<Recipe> =
        state.plans.getValue(key(date, meal)).map { recipes.getValue(it) }

    fun minutes(menu: List<Recipe>) = maxOf(menu[0].minutes, menu[1].minutes + menu[2].minutes) + 15
    fun cost(menu: List<Recipe>, people: Int) = (menu.sumOf { it.cost } * people / 4.0).roundToLong()

    fun ingredients(menu: List<Recipe>, people: Int): List<Ingredient> {
        require(people in 1..12)
        val merged = linkedMapOf<String, Ingredient>()
        menu.flatMap { it.ingredients }.forEach { item ->
            val amount = (merged[item.key]?.amount ?: 0.0) + item.amount * people / 4.0
            merged[item.key] = item.copy(amount = amount)
        }
        val rice = Ingredient("Gạo", people * 90.0, "g", "Gia vị & đồ khô")
        merged[rice.key] = rice
        return merged.values.toList()
    }

    fun timeline(state: PlannerState, date: LocalDate = state.date, meal: Meal = state.meal): List<CookingTask> {
        val menu = menu(state, date, meal)
        val finish = date.atTime(state.profile.time(meal)).atZone(zone).toInstant().toEpochMilli()
        val (main, soup, veg) = menu
        return listOf(
            CookingTask("prep", "Sơ chế và nấu cơm", "Vo gạo, bật nồi cơm. Rửa rau, chuẩn bị thịt và gia vị.", finish - minutes(menu) * 60_000L, 15),
            CookingTask("main", "Nấu ${main.name.lowercase(locale)}", main.tip, finish - main.minutes * 60_000L, main.minutes, main.id),
            CookingTask("soup", "Nấu ${soup.name.lowercase(locale)}", "Dùng vùng bếp thứ hai để nấu canh, sau đó làm món rau.", finish - (soup.minutes + veg.minutes) * 60_000L, soup.minutes, soup.id),
            CookingTask("veg", "Làm ${veg.name.lowercase(locale)}", "Chuẩn bị sau cùng để rau còn tươi ngon khi dọn cơm.", finish - veg.minutes * 60_000L, veg.minutes, veg.id),
            CookingTask("serve", "Dọn cơm, cả nhà cùng ăn", "Kiểm tra độ chín, nếm lại rồi bày các món ra mâm.", finish),
        ).sortedBy { it.at }
    }

    fun swap(state: PlannerState, index: Int, recipeId: String): PlannerState {
        require(index in 0..2)
        val old = state.plans.getValue(state.key)
        require(recipes.getValue(old[index]).kind == recipes.getValue(recipeId).kind)
        val changed = old.toMutableList().apply { this[index] = recipeId }
        return state.copy(plans = state.plans + (state.key to changed), done = state.done - state.key)
    }

    fun alarms(state: PlannerState, now: Long): List<NativeAlarm> {
        fun inWindow(at: Long) = at > now && at - now <= 15 * 86_400_000L
        val alarms = state.reminders.mapNotNull { key ->
            val parts = key.split('|')
            val date = runCatching { LocalDate.parse(parts.first()) }.getOrNull() ?: return@mapNotNull null
            val meal = Meal.from(parts.getOrNull(1).orEmpty()) ?: return@mapNotNull null
            if (!validPlan(state.plans[key].orEmpty())) return@mapNotNull null
            val start = timeline(state, date, meal).first().at
            if (!inWindow(start)) return@mapNotNull null
            NativeAlarm("meal:$key", start, "Đến giờ chuẩn bị ${meal.title.lowercase(locale)}",
                "Cả nhà ăn lúc ${state.profile.time(meal)}. ${menu(state, date, meal).joinToString { it.name }}.", date, meal)
        }.toMutableList()
        state.timer?.let { timer ->
            if (inWindow(timer.end) && timer.end - now <= 3 * 60 * 60_000L) {
                alarms += NativeAlarm("cooking-timer", timer.end, "Đã hết giờ nấu",
                    "${timer.label.take(200)}: kiểm tra độ chín của món ăn nhé.", timer.date, timer.meal, true)
            }
        }
        return alarms.sortedBy { it.id }
    }

    fun answer(question: String, state: PlannerState): AssistantAnswer {
        val query = normalize(question)
        val current = menu(state)
        val named = recipes.values.firstOrNull { query.contains(normalize(it.name)) }
        return when {
            named != null -> AssistantAnswer("${named.name} nấu khoảng ${named.minutes} phút. ${named.tip}", listOf(named.id))
            listOf("truoc", "thu tu", "lich", "may gio").any(query::contains) -> AssistantAnswer(
                "Để ăn lúc ${state.profile.time(state.meal)}, bạn có thể nấu theo lịch:\n\n" +
                    timeline(state).joinToString("\n") { "${time(it.at)} — ${it.title}" })
            listOf("tiet kiem", "re hon", "ngan sach").any(query::contains) -> AssistantAnswer(
                "Bạn thử trứng chiên hành hoặc đậu hũ sốt cà chua. Giá là ước tính và thay đổi theo nơi mua.", listOf("eggs", "tofu"))
            listOf("nhanh", "ban ron").any(query::contains) -> AssistantAnswer(
                "Những món này nấu gọn trong khoảng 10–20 phút:", listOf("eggs", "tofu", "shrimp"))
            else -> {
                val words = query.split(" ").filter { it.length > 2 && it !in setOf("cach", "nau", "cong", "thuc", "minh", "mon") }
                val found = recipes.values.filter { recipe -> words.any { normalize(recipe.name).contains(it) } }.take(4)
                if (found.isNotEmpty()) AssistantAnswer("Mình tìm được những món này trong công thức có sẵn:", found.map { it.id })
                else AssistantAnswer("Bữa này có ${current.joinToString { it.name.lowercase(locale) }}. Bạn có thể hỏi tên món, “món nấu nhanh”, “tiết kiệm hơn” hoặc “nấu món nào trước”.")
            }
        }
    }
}
