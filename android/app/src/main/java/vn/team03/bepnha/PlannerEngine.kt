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
class PlannerEngine(
    val recipes: Map<String, Recipe>,
    val combinations: List<List<String>>,
    val breakfasts: List<String> = emptyList(),
) {
    companion object {
        val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")
        val locale: Locale = Locale.forLanguageTag("vi-VN")
        const val PANTRY = "Gia vị & đồ khô"
        const val MAX_TIMERS = 4
        const val SHOPPING_ITEMS = 6
        fun key(date: LocalDate, meal: Meal) = "$date|${meal.value}"
        fun shopKey(date: LocalDate) = "$date|shop"
        fun mealOf(key: String) = Meal.from(key.substringAfter('|', ""))
        fun today() = LocalDate.now(zone)
        fun time(at: Long): String = DateTimeFormatter.ofPattern("HH:mm").withZone(zone).format(Instant.ofEpochMilli(at))
        fun shortDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("d/M", locale))
        fun fullDate(date: LocalDate): String = date.format(DateTimeFormatter.ofPattern("EEEE, d 'tháng' M", locale))
        fun weekdayShort(date: LocalDate): String = if (date.dayOfWeek.value == 7) "CN" else "T${date.dayOfWeek.value + 1}"
        fun weekday(date: LocalDate): String = if (date.dayOfWeek.value == 7) "Chủ nhật" else
            "Thứ " + listOf("Hai", "Ba", "Tư", "Năm", "Sáu", "Bảy")[date.dayOfWeek.value - 1]
        fun dayLabel(date: LocalDate, today: LocalDate): String = when (date) {
            today -> "hôm nay"; today.plusDays(1) -> "ngày mai"; today.plusDays(2) -> "ngày kia"
            else -> "${weekday(date).lowercase(locale)} ${shortDate(date)}"
        }
        fun money(value: Long): String = NumberFormat.getCurrencyInstance(locale).apply { maximumFractionDigits = 0 }.format(value)
        fun quantity(value: Double): String = NumberFormat.getNumberInstance(locale).apply { maximumFractionDigits = 1 }.format(value)
        fun normalize(text: String): String = Normalizer.normalize(text.lowercase(locale), Normalizer.Form.NFD)
            .replace("\\p{M}+".toRegex(), "").replace('đ', 'd')

        /** Lowercase, accent-free words separated by single spaces; punctuation becomes a space. */
        fun words(text: String): String = normalize(text).replace("[^a-z0-9]+".toRegex(), " ").trim()
        private fun hasWord(text: String, phrase: String) = Regex("(^| )${Regex.escape(phrase)}( |$)").containsMatchIn(text)

        /** Everyday names people type for what is in the fridge, mapped to ingredient names in the recipes. */
        private val aliases: Map<String, List<String>> = buildMap {
            fun alias(vararg names: String, to: List<String>) = names.forEach { put(it, to) }
            alias("thit heo", "thit lon", "heo", "thit", "thit nac", to = listOf("thit heo", "thit ba chi"))
            alias("thit bam", "thit xay", to = listOf("thit heo bam"))
            alias("ga", "thit ga", "uc ga", "dui ga", to = listOf("thit ga"))
            alias("ca", "ca basa", "ca phi le", to = listOf("ca basa"))
            alias("trung", "trung ga", "hot ga", to = listOf("trung ga"))
            alias("dau hu", "dau phu", "tau hu", to = listOf("dau hu"))
            alias("rau", "rau xanh", to = listOf("rau muong", "cai xanh", "bap cai", "bong cai xanh"))
            alias("cai", "rau cai", to = listOf("cai xanh", "bap cai"))
            alias("bi", "bi ngo", to = listOf("bi do"))
            alias("bong cai", "sup lo", "sup lo xanh", to = listOf("bong cai xanh"))
            alias("pho", "banh pho", to = listOf("banh pho"))
            alias("nep", "gao nep", "xoi", to = listOf("gao nep"))
            alias("mi", "mi goi", "mi trung", to = listOf("mi trung"))
            alias("com", "com nguoi", "com thua", "gao", to = listOf("gao"))
            alias("dua chuot", to = listOf("dua leo"))
            alias("khom", "trai thom", to = listOf("thom"))
        }
        private val fillerWords = setOf("it", "chut", "mot", "con", "cai", "qua", "trai", "goi", "hop", "kg", "g", "gam",
            "lang", "nua", "thoi", "thi", "a", "nhe", "nha", "voi", "va", "them", "minh", "toi")
    }

    private val pantry = setOf(PANTRY)
    private val knownNames: Set<String> = recipes.values.flatMap { recipe -> recipe.ingredients.map { words(it.name) } + words(recipe.name) }.toSet()

    val meals: List<Meal> = if (breakfasts.isEmpty()) listOf(Meal.LUNCH, Meal.DINNER) else Meal.entries

    init {
        require(recipes.isNotEmpty() && combinations.isNotEmpty() && breakfasts.isNotEmpty())
        require(combinations.all { validPlan(it, Meal.DINNER) })
        require(breakfasts.all { validPlan(listOf(it), Meal.BREAKFAST) })
    }

    fun startTimer(state: PlannerState, now: Long, minutes: Int, label: String, date: LocalDate, meal: Meal): PlannerState {
        require(minutes in 1..180)
        val running = state.timers.filter { it.end > now }
        val replaced = running.filter { it.label == label && it.date == date && it.meal == meal }
        require(running.size - replaced.size < MAX_TIMERS) {
            "Đang chạy $MAX_TIMERS đồng hồ. Hãy dừng bớt một đồng hồ trước khi hẹn thêm."
        }
        val id = maxOf(now, (state.timers.maxOfOrNull { it.id } ?: 0L) + 1)
        return state.copy(timers = running - replaced.toSet() + CookingTimer(id, now + minutes * 60_000L, label, date, meal))
    }

    fun validPlan(ids: List<String>, meal: Meal = Meal.DINNER) = if (meal.tray) ids.size == 3 &&
        ids.map { recipes[it]?.kind } == RecipeKind.trayKinds
    else ids.size == 1 && recipes[ids[0]]?.kind == RecipeKind.BREAKFAST
    fun validEntry(key: String, ids: List<String>) = mealOf(key)?.let { validPlan(ids, it) } == true

    fun ensureWeek(state: PlannerState, today: LocalDate): PlannerState {
        val plans = state.plans.toMutableMap()
        repeat(7) { day -> meals.forEach { meal ->
            val key = key(today.plusDays(day.toLong()), meal)
            if (!validPlan(plans[key].orEmpty(), meal)) plans[key] = when (meal) {
                Meal.BREAKFAST -> listOf(breakfasts[day % breakfasts.size])
                Meal.LUNCH -> combinations[(day + 3) % combinations.size].toList()
                Meal.DINNER -> combinations[day % combinations.size].toList()
            }
        } }
        val selected = state.date.takeIf { it >= today && it <= today.plusDays(6) } ?: today
        return state.copy(plans = plans, date = selected)
    }

    fun menu(state: PlannerState, date: LocalDate = state.date, meal: Meal = state.meal): List<Recipe> =
        state.plans.getValue(key(date, meal)).map { recipes.getValue(it) }

    fun minutes(menu: List<Recipe>) = if (menu.size == 1) menu[0].minutes + 5
        else maxOf(menu[0].minutes, menu[1].minutes + menu[2].minutes) + 15
    fun cost(menu: List<Recipe>, people: Int) = (menu.sumOf { it.cost } * people / 4.0).roundToLong()

    /** Merged shopping list. Each rice tray (one per main dish) adds 90 g of rice per person. */
    fun ingredients(menu: List<Recipe>, people: Int): List<Ingredient> {
        require(people in 1..12)
        val merged = linkedMapOf<String, Ingredient>()
        fun add(item: Ingredient, amount: Double) {
            merged[item.key] = item.copy(amount = (merged[item.key]?.amount ?: 0.0) + amount)
        }
        menu.flatMap { it.ingredients }.forEach { add(it, it.amount * people / 4.0) }
        val trays = menu.count { it.kind == RecipeKind.MAIN }
        if (trays > 0) Ingredient("Gạo", 0.0, "g", PANTRY).let { add(it, people * 90.0 * trays) }
        return merged.values.toList()
    }

    fun shoppingMenu(state: PlannerState, date: LocalDate = state.date, meals: Set<Meal> = state.shoppingMeals): List<Recipe> =
        meals.sorted().flatMap { menu(state, date, it) }

    fun timeline(state: PlannerState, date: LocalDate = state.date, meal: Meal = state.meal): List<CookingTask> {
        val menu = menu(state, date, meal)
        val finish = date.atTime(state.profile.time(meal)).atZone(zone).toInstant().toEpochMilli()
        if (menu.size == 1) {
            val dish = menu[0]
            return listOf(
                CookingTask("prep", "Chuẩn bị nguyên liệu", "Lấy đủ nguyên liệu, rửa sạch và cắt sẵn.", finish - minutes(menu) * 60_000L, 5),
                CookingTask("main", "Nấu ${dish.name.lowercase(locale)}", dish.tip, finish - dish.minutes * 60_000L, dish.minutes, dish.id),
                CookingTask("serve", "Dọn bữa sáng", "Nếm lại cho vừa ăn rồi bày ra bàn.", finish),
            )
        }
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
        val old = state.plans.getValue(state.key)
        require(index in old.indices)
        require(recipes.getValue(old[index]).kind == recipes.getValue(recipeId).kind)
        if (old[index] == recipeId) return state
        val changed = old.toMutableList().apply { this[index] = recipeId }
        return setPlan(state, state.date, state.meal, changed)
    }

    /** Day-level checks supersede picks made by older per-meal versions. */
    fun checkedFor(state: PlannerState, date: LocalDate, meal: Meal): Set<String> =
        state.checked[shopKey(date)] ?: state.checked[key(date, meal)].orEmpty()

    fun checkedForShopping(state: PlannerState, date: LocalDate, selected: Set<Meal>): Set<String> {
        state.checked[shopKey(date)]?.let { return it }
        val checked = selected.map { meal ->
            val required = ingredients(menu(state, date, meal), state.profile.people).map { it.key }.toSet()
            required to checkedFor(state, date, meal)
        }
        return checked.flatMap { it.second }.filter { key -> checked.all { key !in it.first || key in it.second } }.toSet()
    }

    fun setPlan(state: PlannerState, date: LocalDate, meal: Meal, ids: List<String>): PlannerState {
        require(validPlan(ids, meal))
        val key = key(date, meal)
        if (state.plans[key] == ids) return state
        val result = state.copy(plans = state.plans + (key to ids.toList()), done = state.done - key)
        val shop = shopKey(date)
        val scope = if (shop in state.checked) meals.filter { key(date, it) in state.plans }.toSet() else setOf(meal)
        val checkKey = if (shop in state.checked) shop else key
        val checked = state.checked[checkKey] ?: return result
        val previous = ingredients(shoppingMenu(state, date, scope), state.profile.people).associate { it.key to it.amount }
        val kept = ingredients(shoppingMenu(result, date, scope), state.profile.people)
            .filter { it.key in checked && it.amount <= (previous[it.key] ?: 0.0) }.map { it.key }.toSet()
        return result.copy(checked = result.checked + (checkKey to kept))
    }

    fun alarms(state: PlannerState, now: Long): List<NativeAlarm> {
        fun inWindow(at: Long) = at > now && at - now <= 15 * 86_400_000L
        val alarms = state.reminders.mapNotNull { key ->
            val parts = key.split('|')
            val date = runCatching { LocalDate.parse(parts.first()) }.getOrNull() ?: return@mapNotNull null
            val meal = Meal.from(parts.getOrNull(1).orEmpty()) ?: return@mapNotNull null
            if (!validPlan(state.plans[key].orEmpty(), meal)) return@mapNotNull null
            val start = timeline(state, date, meal).first().at
            if (!inWindow(start)) return@mapNotNull null
            NativeAlarm("meal:$key", start, "Đến giờ chuẩn bị ${meal.title.lowercase(locale)}",
                "Cả nhà ăn lúc ${state.profile.time(meal)}. ${menu(state, date, meal).joinToString { it.name }}.", date, meal)
        }.toMutableList()
        state.timers.forEach { timer ->
            if (inWindow(timer.end) && timer.end - now <= 3 * 60 * 60_000L) {
                alarms += NativeAlarm("cooking-timer:${timer.id}", timer.end, "Đã hết giờ: ${timer.label.take(60)}",
                    "${timer.label.take(200)}: kiểm tra độ chín của món ăn nhé.", timer.date, timer.meal, true)
            }
        }
        if (state.shopping.enabled) {
            val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
            repeat(7) { day ->
                val date = today.plusDays(day.toLong())
                val at = date.atTime(state.shopping.time).atZone(zone).toInstant().toEpochMilli()
                val missing = missingForShopping(state, date)
                if (inWindow(at) && missing.isNotEmpty()) {
                    alarms += NativeAlarm("shopping:$date", at, "Nhắc đi chợ", shoppingBody(missing),
                        date, Meal.LUNCH, kind = AlarmKind.SHOPPING)
                }
            }
        }
        return alarms.sortedBy { it.id }
    }

    /** Unchecked fresh ingredients for the day's lunch and dinner; pantry staples are assumed to be at home. */
    fun missingForShopping(state: PlannerState, date: LocalDate): List<String> = meals.flatMap { meal ->
        val key = key(date, meal)
        if (!validPlan(state.plans[key].orEmpty(), meal)) return@flatMap emptyList()
        val checked = checkedFor(state, date, meal)
        ingredients(menu(state, date, meal), state.profile.people)
            .filter { it.group != PANTRY && it.key !in checked }.map { it.name }
    }.distinct()

    fun shoppingBody(missing: List<String>): String {
        val shown = missing.take(SHOPPING_ITEMS).joinToString()
        val more = missing.size - SHOPPING_ITEMS
        return "Hôm nay cần mua: $shown" + if (more > 0) " và $more thứ khác." else "."
    }

    // ---- Fridge suggestions -------------------------------------------------------------------

    /** Search phrases for one fridge item: a known alias, the item itself, or aliases of its words. */
    private fun tokens(item: String): List<String> {
        val text = words(item)
        aliases[text]?.let { return it }
        if (knownNames.any { hasWord(it, text) }) return listOf(text)
        val parts = text.split(' ').filter { it !in fillerWords }
        // "thịt" alone is too vague inside a longer phrase: "thịt bò" must not become pork.
        val found = (2 downTo 1).flatMap { size -> parts.windowed(size).map { it.joinToString(" ") }
            .filter { size > 1 || it != "thit" }.mapNotNull(aliases::get) }.flatten()
        return found.ifEmpty { listOf(parts.joinToString(" ")).filter { it.isNotBlank() } }.distinct()
    }

    private fun uses(recipe: Recipe, tokens: List<String>) = tokens.any { token ->
        hasWord(words(recipe.name), token) || recipe.ingredients.any { hasWord(words(it.name), token) }
    }

    /** Fridge items (as the user wrote them) that this dish uses. */
    fun covered(recipe: Recipe, items: Collection<String>): List<String> = items.filter { uses(recipe, tokens(it)) }

    /**
     * Builds up to [limit] complete meals that use as many fridge items as possible. A rice tray is
     * always mặn + canh + rau; the best trays put the fridge item in the main dish, need the fewest
     * extra purchases and prefer pairings already used in the weekly menu.
     */
    fun suggestTrays(items: Collection<String>, meal: Meal, limit: Int = 3, diet: DietProfile = DietProfile(), people: Int = 4): List<TraySuggestion> {
        val wanted = items.map { it.trim() }.filter { it.length >= 2 }.distinctBy(::words)
        if (wanted.isEmpty()) return emptyList()
        val wantedTokens = wanted.flatMap(::tokens)
        val byKind = recipes.values.filter(diet::allows).groupBy { it.kind }
        val coverage = byKind.values.flatten().associate { it.id to covered(it, wanted) }
        val candidates: List<List<Recipe>> = if (!meal.tray) byKind[RecipeKind.BREAKFAST].orEmpty().map { listOf(it) }
        else byKind[RecipeKind.MAIN].orEmpty().flatMap { main -> byKind[RecipeKind.SOUP].orEmpty().flatMap { soup ->
            byKind[RecipeKind.VEG].orEmpty().map { veg -> listOf(main, soup, veg) } } }

        val missingByRecipe = byKind.values.flatten().associate { recipe ->
            recipe.id to recipe.ingredients
                .filter { it.group !in pantry && wantedTokens.none { token -> hasWord(words(it.name), token) } }
                .map { it.name }
        }

        class Scored(val dishes: List<Recipe>, val cover: List<String>, val mainCover: Int, val missing: List<String>) {
            val ids = dishes.map { it.id }
        }
        val scored = candidates.asSequence()
            .filter { diet.maxMinutes == null || minutes(it) <= diet.maxMinutes }
            .filter { diet.maxCost == null || cost(it, people) <= diet.maxCost }
            .map { dishes ->
                val cover = dishes.flatMap { coverage.getValue(it.id) }.toSet()
                Scored(dishes, wanted.filter { it in cover }, coverage.getValue(dishes[0].id).size,
                    dishes.flatMap { missingByRecipe.getValue(it.id) }.distinct())
            }.filter { it.cover.isNotEmpty() }.sortedWith(
            compareByDescending<Scored> { it.cover.size }
                .thenByDescending { it.mainCover }
                .thenBy { it.missing.size }
                .thenByDescending { it.ids in combinations }
                .thenBy { minutes(it.dishes) }
                .thenBy { it.ids.joinToString() }).toList()

        val picked = mutableListOf<Scored>()
        // Different main dishes first so the three ideas really differ, then fill with the next best.
        scored.forEach { if (picked.size < limit && picked.none { p -> p.ids[0] == it.ids[0] }) picked += it }
        scored.forEach { if (picked.size < limit && it !in picked) picked += it }
        return picked.map { TraySuggestion(it.ids, it.cover, it.missing) }
    }

    // ---- Chat ---------------------------------------------------------------------------------

    /** "hôm nay", "ngày mai", "ngày mai nữa"/"ngày kia", or a weekday such as "thứ sáu". */
    fun parseDate(text: String, today: LocalDate): LocalDate? {
        val q = words(text)
        fun has(vararg phrases: String) = phrases.any { hasWord(q, it) }
        return when {
            has("ngay mai nua", "mai nua", "ngay kia", "ngay mot", "hom sau nua", "mot nua") -> today.plusDays(2)
            has("ngay mai", "mai", "hom sau", "sang mai", "trua mai", "toi mai") -> today.plusDays(1)
            has("hom nay", "nay") -> today
            else -> {
                val days = listOf(listOf("thu hai", "thu 2", "t2"), listOf("thu ba", "thu 3", "t3"), listOf("thu tu", "thu 4", "t4"),
                    listOf("thu nam", "thu 5", "t5"), listOf("thu sau", "thu 6", "t6"), listOf("thu bay", "thu 7", "t7"),
                    listOf("chu nhat", "cn"))
                val index = days.indexOfFirst { names -> names.any { hasWord(q, it) } }
                if (index < 0) null else today.plusDays(((index + 1 - today.dayOfWeek.value + 7) % 7).toLong())
            }
        }
    }

    /** Uses the accented text first because "tối" (evening) and "tôi" (I) are both "toi" without accents. */
    fun parseMeal(text: String): Meal? {
        val lower = text.lowercase(locale)
        val q = words(text)
        return when {
            listOf("sáng", "điểm tâm").any(lower::contains) || listOf("bua sang", "an sang", "diem tam").any { hasWord(q, it) } -> Meal.BREAKFAST
            "trưa" in lower || hasWord(q, "trua") -> Meal.LUNCH
            listOf("tối", "chiều").any(lower::contains) || listOf("bua toi", "an toi", "bua chieu").any { hasWord(q, it) } -> Meal.DINNER
            else -> null
        }
    }

    private fun namedRecipe(q: String) = recipes.values.sortedByDescending { it.name.length }
        .firstOrNull { hasWord(q, words(it.name)) }

    fun answer(question: String, state: PlannerState, today: LocalDate = today()): AssistantAnswer {
        val q = words(question)
        val days = (0L..6L).map(today::plusDays)
        val askedDate = parseDate(question, today)
        val askedMeal = parseMeal(question)

        // 1. Timer: "hẹn 10 phút luộc rau".
        Regex("(\\d{1,3}) ?(phut|p)( |$)").find(q)?.takeIf { listOf("hen", "dem nguoc", "bao", "dat gio").any { w -> hasWord(q, w) } }?.let { match ->
            val minutes = match.groupValues[1].toInt()
            if (minutes !in 1..180) return AssistantAnswer("Bếp Nhà hẹn được từ 1 đến 180 phút thôi nhé.")
            val label = Regex("\\d{1,3}\\s*(phút|phut|p)\\b\\s*(cho|để|de)?\\s*(.*)", RegexOption.IGNORE_CASE)
                .find(question)?.groupValues?.get(3)?.trim()?.trimEnd('.', '!', '?').orEmpty()
                .ifBlank { namedRecipe(q)?.name ?: "Hẹn giờ $minutes phút" }.replaceFirstChar { it.titlecase(locale) }
            return AssistantAnswer("Đã hẹn $minutes phút cho ${label.lowercase(locale)}.", timerMinutes = minutes, timerLabel = label.take(60))
        }

        // 2. Fridge: "tủ lạnh có thịt heo, cà chua".
        if (listOf("tu lanh", "trong nha co", "nha con", "con co", "dang co", "nha co", "con lai").any { hasWord(q, it) }) {
            val raw = normalize(question)
            val after = Regex("\\b(co|con)\\b(.*)").find(raw)?.groupValues?.get(2) ?: raw
            val items = after.split(Regex(",|;|\\.|\\?|\\b(va|voi|them|hoac)\\b"))
                .map { part -> words(part).split(' ').filterNot { it in fillerWords || it in setOf("co", "lai", "nau", "gi", "an", "hom", "nay", "ngay", "mai", "trua", "toi", "sang", "bua", "mon", "nao") }.joinToString(" ") }
                .filter { it.length >= 2 }
            val meal = askedMeal ?: state.meal
            val date = askedDate?.takeIf { it in days } ?: state.date
            if (items.isEmpty()) return AssistantAnswer("Tủ lạnh nhà bạn có gì? Ví dụ: “tủ lạnh có thịt heo và cà chua”.")
            val trays = suggestTrays(items, meal, diet = state.diet, people = state.profile.people)
            return if (trays.isEmpty()) AssistantAnswer("Mình chưa tìm thấy món hợp với ${items.joinToString()} trong ${recipes.size} công thức có sẵn. Bạn thử thịt heo, gà, cá, tôm, trứng, đậu hũ hoặc rau.")
            else AssistantAnswer("Với ${items.joinToString()}, Bếp Nhà gợi ý ${meal.title.lowercase(locale)} ${dayLabel(date, today)}:",
                trays = trays, date = date, meal = meal)
        }

        // 3. What is planned: "ngày mai ăn gì?", "thực đơn thứ bảy".
        if (listOf("an gi", "nau gi", "thuc don", "mon gi").any { hasWord(q, it) } && namedRecipe(q) == null) {
            val date = askedDate ?: state.date
            if (date !in days) return AssistantAnswer("Bếp Nhà mới lên thực đơn đến ${weekday(days.last()).lowercase(locale)} ${shortDate(days.last())}.")
            val meals = askedMeal?.let { listOf(it) } ?: meals
            return AssistantAnswer("Thực đơn ${dayLabel(date, today)}:\n" +
                meals.joinToString("\n") { meal -> "• ${meal.title}: ${menu(state, date, meal).joinToString { it.name.lowercase(locale) }}" } +
                "\n\nMuốn đổi, bạn nói “đổi sang …” hoặc cho mình biết tủ lạnh có gì.", date = date, meal = askedMeal)
        }

        // 4. Change the menu: "ngày mai tôi muốn ăn cá", "tối thứ sáu đổi sang canh chua".
        val wantsChange = listOf("muon an", "muon nau", "them mon", "doi mon", "doi sang", "doi thanh", "thay bang", "thay mon", "len thuc don", "cho an", "them vao")
            .any { hasWord(q, it) } || (askedDate != null && (hasWord(q, "an") || hasWord(q, "nau")))
        if (wantsChange) {
            val named = namedRecipe(q)
            val phrase = Regex("(^| )(muon an|muon nau|an mon|nau mon|doi sang|doi thanh|thay bang|an|nau|mon)( |$)(.*)").findAll(q).lastOrNull()?.groupValues?.get(4).orEmpty()
                .split(' ').filterNot { it in setOf("hom", "nay", "ngay", "mai", "nua", "kia", "sang", "trua", "toi", "chieu", "bua", "thu", "cn", "mon", "nhe", "nha", "di", "duoc", "khong", "a") || it.matches(Regex("\\d")) }
                .joinToString(" ")
            val matches = when {
                named != null -> listOf(named)
                phrase.isNotBlank() -> recipes.values.filter { uses(it, tokens(phrase)) }
                    .sortedWith(compareBy<Recipe> { if (askedMeal == Meal.BREAKFAST) it.kind != RecipeKind.BREAKFAST else it.kind != RecipeKind.MAIN }
                        .thenBy { it.kind.ordinal }.thenBy { it.minutes })
                else -> emptyList()
            }
            if (matches.isEmpty()) return AssistantAnswer(
                "Mình chưa tìm thấy “${phrase.ifBlank { question.trim() }}” trong ${recipes.size} công thức có sẵn. Bạn thử nói tên món như “ngày mai tôi muốn ăn canh chua cá”.")
            val first = matches.first()
            val meal = when {
                first.kind == RecipeKind.BREAKFAST -> Meal.BREAKFAST
                askedMeal != null && askedMeal.tray -> askedMeal
                state.meal.tray -> state.meal
                else -> Meal.DINNER
            }
            val date = askedDate ?: state.date
            if (date !in days) return AssistantAnswer("Bếp Nhà mới lên thực đơn đến ${weekday(days.last()).lowercase(locale)} ${shortDate(days.last())}. Bạn chọn một ngày trong tuần này nhé.")
            val options = matches.filter { (it.kind == RecipeKind.BREAKFAST) == !meal.tray }.take(3)
            if (options.isEmpty()) return AssistantAnswer("${first.name} là món cho ${if (meal.tray) "bữa sáng" else "bữa trưa hoặc tối"}. Bạn thử nói rõ bữa nhé.", listOf(first.id))
            val current = menu(state, date, meal)
            val label = "${meal.title.lowercase(locale)} ${dayLabel(date, today)}"
            if (current.any { it.id == options[0].id }) return AssistantAnswer("${options[0].name} đã có trong $label rồi.", listOf(options[0].id))
            val replaced = current.first { it.kind == options[0].kind }
            return AssistantAnswer(
                "Đổi ${replaced.name.lowercase(locale)} thành ${options[0].name.lowercase(locale)} cho $label nhé?" +
                    if (options.size > 1) " Hoặc chọn món khác bên dưới." else "",
                changes = options.map { MenuChange(date, meal, it.id) }, date = date, meal = meal)
        }

        // 5. Questions about the current meal and the bundled recipes.
        val current = menu(state)
        val named = namedRecipe(q)
        return when {
            named != null -> AssistantAnswer("${named.name} nấu khoảng ${named.minutes} phút. ${named.tip}", listOf(named.id))
            listOf("truoc", "thu tu", "lich", "may gio").any { hasWord(q, it) } -> AssistantAnswer(
                "Để ăn lúc ${state.profile.time(state.meal)}, bạn có thể nấu theo lịch:\n\n" +
                    timeline(state).joinToString("\n") { "${time(it.at)} — ${it.title}" })
            listOf("tiet kiem", "re hon", "ngan sach", "re").any { hasWord(q, it) } -> AssistantAnswer(
                "Bạn thử trứng chiên hành hoặc đậu hũ sốt cà chua. Giá là ước tính và thay đổi theo nơi mua.", listOf("eggs", "tofu"))
            listOf("nhanh", "ban ron", "gap").any { hasWord(q, it) } -> AssistantAnswer(
                "Những món này nấu gọn trong khoảng 10–20 phút:", listOf("eggs", "tofu", "shrimp"))
            else -> {
                val found = q.split(' ').filter { it.length > 2 && it !in setOf("cach", "nau", "cong", "thuc", "minh", "mon") }
                    .let { keys -> recipes.values.filter { recipe -> keys.any { hasWord(words(recipe.name), it) } }.take(4) }
                if (found.isNotEmpty()) AssistantAnswer("Mình tìm được những món này trong công thức có sẵn:", found.map { it.id })
                else AssistantAnswer("${state.meal.title} có ${current.joinToString { it.name.lowercase(locale) }}. " +
                    "Bạn có thể nói “tủ lạnh có thịt heo và cà chua”, “ngày mai tôi muốn ăn cá” hoặc “hẹn 10 phút luộc rau”.")
            }
        }
    }
}
