package vn.team03.bepnha

import org.json.JSONObject
import java.text.Normalizer
import java.time.LocalDate
import java.util.Locale

data class MenuScope(val day: LocalDate? = null, val meal: Meal? = null, val clarification: String? = null)
data class MealOption(val recipeIds: List<String>, val pantryKeys: Set<String>, val missing: List<String>)
data class PantryConstraints(val pantry: Set<String>, val excluded: Set<String>, val only: Boolean, val unknownExclusion: Boolean = false)

/** Deterministic rules shared by cloud tool handling and the local assistant. No network or UI. */
class MealPlanning(private val recipes: Map<String, Recipe>) {
    companion object {
        const val MEAL_QUESTION = "Bạn muốn áp dụng cho bữa trưa hay bữa tối?"
        const val DISH_QUESTION = "Bạn muốn đổi món mặn, món canh hay món rau?"
        private const val STAPLES = "Gia vị & đồ khô"
        fun normalize(value: String): String = Normalizer.normalize(value.lowercase(Locale.ROOT), Normalizer.Form.NFD)
            .replace("đ", "d").replace(Regex("\\p{M}+"), "").trim().replace(Regex("\\s+"), " ")
        fun parseDay(value: String): LocalDate = LocalDate.parse(value).also { require(it.toString() == value) }
        private val relative = mapOf("hom nay" to 0L, "ngay mai" to 1L, "mai" to 1L,
            "ngay mai nua" to 2L, "mai nua" to 2L, "ngay kia" to 2L, "ngay mot" to 2L,
            "hom qua" to -1L, "hom kia" to -2L)
        private val datePattern = Regex("""\b(?<relative>${relative.keys.sortedByDescending { it.length }.joinToString("|")})\b|\b(?<iso>\d{4}-\d{2}-\d{2})\b|\b(?<day>\d{1,2})/(?<month>\d{1,2})(?:/(?<year>\d{4}))?(?![\d/])|\b(?<weekday>thu (?:hai|ba|tu|bon|nam|sau|bay|[2-7])|chu nhat)(?<nextweek> tuan sau)?\b""")
        private val mealPattern = Regex("""\b(trưa|trua|tối(?! (?:đa|thiểu)\b)|bua toi|toi(?= (?:nay|mai|ngay|thu)\b)|bữa sáng|bua sang|sáng(?= (?:nay|mai|ngày|thứ)\b)|sang(?= (?:nay|mai|ngay|thu)\b))\b""")
        val menuRequest = Regex("""\b(?:doi (?:mon|mam|canh|rau|sang|lai)|thay (?:mon|canh|rau)|tu lanh|goi y mam|len thuc don|nau gi|muon an|muon nau)\b""")
        val dishReference = Regex("""\b(?:doi|thay) (?:mon|mon an) (?:do|nay)\b""")
        private val followUp = Regex("""^(?:a|y minh la|y toi la|thoi)\b|\b(?:doi sang|doi lai|thay bang|mon do)\b""")
        private val aliases = mapOf(
            "Thịt ba chỉ" to listOf("thịt heo", "thịt lợn", "ba chỉ", "ba rọi", "thịt ba rọi"),
            "Thịt heo bằm" to listOf("thịt heo", "thịt lợn", "thịt bằm", "thịt băm", "thịt heo băm", "thịt heo xay", "thịt lợn xay"),
            "Trứng gà" to listOf("trứng"), "Thịt gà" to listOf("gà"), "Cá basa" to listOf("cá", "cá ba sa"),
            "Đậu hũ" to listOf("đậu phụ", "tàu hũ"), "Thơm" to listOf("dứa", "khóm"),
            "Dưa leo" to listOf("dưa chuột"), "Bí đỏ" to listOf("bí ngô"),
            "Cải xanh" to listOf("cải bẹ xanh"), "Bông cải xanh" to listOf("súp lơ xanh"),
        )
        fun isComparison(question: String): Boolean {
            val text = normalize(question)
            return Regex("""\bmam\b""").containsMatchIn(text) && Regex("""\b(?:so sanh|khac|giong)\b""").containsMatchIn(text) &&
                !Regex("""\b(?:doi|thay|tranh|het|khong|chi|ngay|mai|nay|trua|toi)\b""").containsMatchIn(text)
        }
    }

    private val ingredientGroups = recipes.values.flatMap { it.ingredients }.associate { it.name to it.group } + ("Gạo" to STAPLES)
    private val namesByAlias = buildMap<String, Set<String>> {
        ingredientGroups.keys.forEach { name ->
            (listOf(name) + aliases[name].orEmpty()).forEach { alias ->
                val key = normalize(alias)
                put(key, get(key).orEmpty() + name)
            }
        }
    }
    private val ingredientPattern = Regex("\\b(?:${namesByAlias.keys.sortedByDescending { it.length }.joinToString("|", transform = Regex::escape)})\\b")

    fun mentionedDays(question: String, today: LocalDate): Set<LocalDate> = buildSet {
        val original = Normalizer.normalize(question.lowercase(Locale.ROOT), Normalizer.Form.NFC)
        if (Regex("""\bmốt\b""").containsMatchIn(original)) add(today.plusDays(2))
        if (Regex("""\b(?:tối|toi|trưa|trua|sáng|sang) nay\b""").containsMatchIn(original)) add(today)
        datePattern.findAll(normalize(question)).forEach { match ->
            fun part(name: String) = match.groups[name]?.value
            add(when {
                part("relative") != null -> today.plusDays(relative.getValue(part("relative")!!))
                part("iso") != null -> parseDay(part("iso")!!)
                part("weekday") != null -> {
                    val word = part("weekday")!!.removePrefix("thu ")
                    val weekday = word.toIntOrNull()?.minus(2) ?: mapOf("hai" to 0, "ba" to 1, "tu" to 2, "bon" to 2, "nam" to 3, "sau" to 4, "bay" to 5).getOrDefault(word, 6)
                    val offset = weekday - (today.dayOfWeek.value - 1)
                    today.plusDays((if (part("nextweek") != null) offset + 7 else Math.floorMod(offset, 7)).toLong())
                }
                else -> {
                    val day = part("day")!!.toInt(); val month = part("month")!!.toInt()
                    val inWeek = (0L..6L).map(today::plusDays).firstOrNull { it.dayOfMonth == day && it.monthValue == month }
                    if (part("year") == null && inWeek != null) inWeek else LocalDate.of(part("year")?.toInt() ?: today.year, month, day)
                }
            })
        }
    }

    fun mentionedMeals(question: String): Set<String> = buildSet {
        val text = Normalizer.normalize(question.lowercase(Locale.ROOT), Normalizer.Form.NFC)
        mealPattern.findAll(text).forEach { match ->
            val name = normalize(match.value)
            val isVerb = name == "sang" && Regex("""\b(?:doi|chuyen|thay)(?: \w+){0,3}$""").containsMatchIn(normalize(text.substring(0, match.range.first)))
            if (!isVerb) add(when (name) { "trua" -> "lunch"; "toi", "bua toi" -> "dinner"; else -> "breakfast" })
        }
    }

    fun previousQuestion(history: List<AssistantChatMessage>): String {
        val fragments = mutableListOf<String>()
        var i = history.lastIndex
        while (i >= 1) {
            val reply = history[i]; val question = history[i - 1]
            if (question.role != "user" || reply.role != "assistant" || reply.text !in listOf(MEAL_QUESTION, DISH_QUESTION)) break
            fragments.add(0, question.text); i -= 2
        }
        return fragments.joinToString(" ")
    }

    fun scope(question: String, context: JSONObject, previous: String = ""): MenuScope {
        val today = parseDay(context.getString("today"))
        val days: Set<LocalDate>; val meals: Set<String>
        try {
            days = mentionedDays(question, today).ifEmpty { mentionedDays(previous, today) }
            meals = mentionedMeals(question).ifEmpty { mentionedMeals(previous) }
        } catch (_: RuntimeException) { return MenuScope(clarification = "Bạn cho mình ngày hợp lệ trong thực đơn bảy ngày nhé.") }
        if ("breakfast" in meals) return MenuScope(clarification = "Thực đơn hiện hỗ trợ bữa trưa và bữa tối. Bạn muốn chọn bữa nào?")
        if (days.size > 1 || meals.size > 1) return MenuScope(clarification = "Bạn muốn chỉnh hoặc gợi ý cho ngày và bữa nào trước?")
        val current = target(context)
        var focus = changeTarget(context, "pending_menu_changes")
        if (focus == null) focus = context.optJSONObject("suggested_meal")?.let(::target)
        if (focus == null && followUp.containsMatchIn(normalize(question))) focus = changeTarget(context, "last_menu_changes")
        val currentRequested = "bua nay" in normalize(question)
        if (currentRequested) focus = current
        val day = days.firstOrNull() ?: focus?.first ?: current.first
        if (day !in today..today.plusDays(6)) return MenuScope(clarification = "Ngày đó nằm ngoài thực đơn bảy ngày. Bạn muốn chọn ngày nào trong tuần đang xem?")
        if (meals.isNotEmpty()) return MenuScope(day, Meal.from(meals.first()))
        if (focus?.first == day) return MenuScope(day, focus.second)
        if (days.isNotEmpty() && day != current.first && !currentRequested) return MenuScope(day, clarification = MEAL_QUESTION)
        return MenuScope(day, current.second)
    }

    private fun target(o: JSONObject) = parseDay(o.getString("date")) to requireNotNull(Meal.from(o.getString("meal")))
    private fun changeTarget(context: JSONObject, key: String): Pair<LocalDate, Meal>? {
        val changes = context.optJSONArray(key) ?: return null
        return (0 until changes.length()).map { target(changes.getJSONObject(it)) }.distinct().singleOrNull()
    }

    fun namedSlots(question: String, command: String): Set<Int> {
        val slot = "(?:mon )?(?:man|canh|rau)"
        val pattern = Regex("""\b(?:$command)\s+($slot(?:(?:, *| va | cung )$slot)*)\b""")
        return pattern.findAll(normalize(question)).flatMap { Regex("""\b(man|canh|rau)\b""").findAll(it.groupValues[1]) }
            .map { mapOf("man" to 0, "canh" to 1, "rau" to 2).getValue(it.value) }.toSet()
    }

    fun requestedRecipes(question: String): Map<Int, String> {
        val text = normalize(question)
        if (Regex("""\b(?:hay|hoac)\b""").containsMatchIn(text)) return emptyMap()
        val result = mutableMapOf<Int, String>()
        Regex("""\b(?:sang|thanh|bang|muon an|muon nau)\s+(?:mon\s+)?""").findAll(text).forEach { match ->
            recipes.values.forEach { recipe ->
                if (Regex("^${Regex.escape(normalize(recipe.name))}\\b").containsMatchIn(text.substring(match.range.last + 1))) result[recipe.kind.ordinal] = recipe.id
            }
        }
        return result
    }

    fun referencedSlot(context: JSONObject, scope: MenuScope): Int? {
        val changes = context.optJSONArray("pending_menu_changes")?.takeIf { it.length() > 0 } ?: context.optJSONArray("last_menu_changes")
        if (changes?.length() == 1) {
            val item = changes.getJSONObject(0)
            if (item.optString("date") == scope.day.toString() && item.optString("meal") == scope.meal?.value && item.optInt("index", -1) in 0..2) return item.getInt("index")
        }
        val recipeId = context.optString("recipe_id")
        val meals = context.optJSONArray("meals") ?: return null
        for (i in 0 until meals.length()) {
            val entry = meals.getJSONObject(i)
            if (entry.optString("date") == scope.day.toString() && entry.optString("meal") == scope.meal?.value) {
                val ids = entry.getJSONArray("recipes")
                return (0 until ids.length()).firstOrNull { ids.getString(it) == recipeId }
            }
        }
        return null
    }

    fun clarification(question: String, history: List<AssistantChatMessage>, context: JSONObject, scope: MenuScope): String? {
        val request = "${previousQuestion(history)} $question"
        if (!menuRequest.containsMatchIn(normalize(request))) return null
        scope.clarification?.let { return it }
        if (!dishReference.containsMatchIn(normalize(request)) || requestedRecipes(request).isNotEmpty()) return null
        if (history.lastOrNull()?.text == DISH_QUESTION && namedSlots("doi $question", "doi").isNotEmpty()) return null
        return if (referencedSlot(context, scope) == null) DISH_QUESTION else null
    }

    fun ingredientNames(terms: List<String>): Set<String> = terms.flatMap { namesByAlias[normalize(it)].orEmpty() }.toSet()
    private fun ingredientSource(message: String): String {
        val original = Normalizer.normalize(message.lowercase(Locale.ROOT), Normalizer.Form.NFC)
            .replace(Regex("""\b(?:tối|tôi)\b"""), "___")
        return normalize(original).replace(Regex("""\b(?:bua toi|toi (?=mai|nay|ngay|thu|mot\b|co\b|con\b|muon\b|vua\b|moi\b))"""), "___ ")
    }
    fun ingredientTerms(messages: List<String>): List<String> = messages.flatMap { ingredientPattern.findAll(ingredientSource(it)).map { match -> match.value }.toList() }.distinct()

    /** Messages are newest first; an exhaustive inventory replaces older contents. */
    fun pantryNames(terms: List<String>, messages: List<String>): Set<String> {
        val remaining = ingredientNames(terms).toMutableSet(); val grounded = mutableSetOf<String>()
        for (message in messages) {
            val text = ingredientSource(message); val availability = mutableMapOf<String, Boolean>()
            ingredientPattern.findAll(text).forEach { mention ->
                val clause = text.substring(0, mention.range.first).split(Regex("""[.;!?]|\bnhung\b""")).last()
                val marker = Regex("""\b(?:khong (?:co|con|dung|an)|tranh|bo qua|het|thieu|co|con|moi mua|mua them)\b""").findAll(clause).lastOrNull()?.value
                val available = marker !in listOf("khong co", "khong con", "khong dung", "khong an", "tranh", "bo qua", "het", "thieu") &&
                    !Regex("""^\s+(?:da )?het\b""").containsMatchIn(text.substring(mention.range.last + 1))
                namesByAlias.getValue(mention.value).forEach { availability[it] = available }
            }
            grounded += remaining.filter { availability[it] == true }
            remaining -= availability.keys
            if (Regex("""\bchi (?:co|con)\b""").containsMatchIn(text)) break
        }
        return grounded
    }

    fun constraints(messages: List<String>): PantryConstraints {
        val pantry = pantryNames(ingredientTerms(messages), messages)
        var only: Boolean? = null; var excluded: Set<String>? = null; var unknown = false
        for (message in messages) {
            val text = normalize(message)
            if (only == null) {
                if (Regex("""\b(?:co the|duoc|chap nhan) mua them\b""").containsMatchIn(text)) only = false
                else if (Regex("""\b(?:chi (?:dung|su dung|nau)|khong mua them)\b""").containsMatchIn(text)) only = true
            }
            if (excluded == null) {
                val clauses = Regex("""\b(?:tranh|khong (?:dung|an)|bo qua|khong co mon chua)\s+([^.;!?]+)""").findAll(text).toList()
                if (clauses.isNotEmpty()) {
                    excluded = clauses.flatMap { ingredientNames(ingredientTerms(listOf(it.groupValues[1]))) }.toSet()
                    unknown = clauses.any { clause ->
                        // Every requested exclusion must be understood, including mixed known/unknown lists.
                        val list = clause.groupValues[1].split(Regex("""\b(?:cho bua|vao bua|toi mai|trua mai|nhe|nha|khong mua them)\b"""), limit = 2).first()
                        list.split(Regex("""\s*(?:,|\bva\b|\bcung\b)\s*""")).filter { it.isNotBlank() }.any { term ->
                            ingredientTerms(listOf(term)).isEmpty() || ingredientPattern.replace(term, "")
                                .replace(Regex("""\b(?:mon|chua|cac|loai|nguyen lieu)\b"""), "").trim().isNotEmpty()
                        }
                    }
                }
            }
        }
        return PantryConstraints(pantry, excluded.orEmpty(), only ?: false, unknown)
    }

    fun rankMeals(pantry: Set<String>, excluded: Set<String>): List<MealOption> {
        data class Ranked(val score: List<Int>, val option: MealOption)
        val groups = RecipeKind.entries.map { kind -> recipes.values.filter { it.kind == kind } }
        val ranked = mutableListOf<Ranked>()
        for (main in groups[0]) for (soup in groups[1]) for (veg in groups[2]) {
            val menu = listOf(main, soup, veg)
            val ingredients = menu.flatMap { it.ingredients } + Ingredient("Gạo", 0.0, "g", STAPLES)
            val names = ingredients.map { it.name }.toSet()
            val available = pantry intersect names
            if (available.isEmpty() || (excluded intersect names).isNotEmpty()) continue
            val missing = names - pantry
            val option = MealOption(menu.map { it.id }, ingredients.filter { it.name in pantry }.map { it.key }.toSortedSet(),
                missing.sortedWith(compareBy<String> { ingredientGroups[it] == STAPLES }.thenBy { it }))
            ranked += Ranked(listOf(-available.count { ingredientGroups[it] != STAPLES }, -available.size,
                missing.count { ingredientGroups[it] != STAPLES }, missing.size), option)
        }
        return ranked.sortedWith { a, b ->
            a.score.zip(b.score).map { (x, y) -> x.compareTo(y) }.firstOrNull { it != 0 }
                ?: a.option.recipeIds.joinToString("|").compareTo(b.option.recipeIds.joinToString("|"))
        }.map { it.option }
    }
}
