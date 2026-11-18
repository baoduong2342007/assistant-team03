package vn.team03.bepnha.voice

/** A spoken command while cooking a recipe step by step. */
sealed interface CookCommand {
    data object Next : CookCommand
    data object Repeat : CookCommand
    data object Back : CookCommand
    data object Undo : CookCommand
    data class Timer(val minutes: Int) : CookCommand
    data object Unknown : CookCommand
}

/**
 * Parses short cooking-mode commands: "tiếp", "đọc lại", "quay lại",
 * "hẹn giờ N phút", "hoàn tác". Matching ignores diacritics, so typed
 * text without accents works too. Long or mixed sentences are [CookCommand.Unknown].
 */
object VoiceCommand {
    const val MAX_WORDS = 10
    const val MAX_TIMER_MINUTES = 180

    private val phrases: List<Pair<List<String>, CookCommand>> = listOf(
        "buoc tiep theo" to CookCommand.Next,
        "tiep theo" to CookCommand.Next,
        "tiep tuc" to CookCommand.Next,
        "buoc tiep" to CookCommand.Next,
        "buoc sau" to CookCommand.Next,
        "tiep" to CookCommand.Next,
        "doc lai" to CookCommand.Repeat,
        "noi lai" to CookCommand.Repeat,
        "quay lai" to CookCommand.Back,
        "tro lai" to CookCommand.Back,
        "buoc truoc" to CookCommand.Back,
        "hoan tac" to CookCommand.Undo,
    ).map { (text, command) -> text.split(' ') to command }.sortedByDescending { it.first.size }

    private val fillers = setOf(
        "a", "u", "o", "da", "vang", "nhe", "nha", "roi", "di", "thi", "cho", "giup", "voi",
        "con", "chau", "ba", "ong", "me", "minh", "nao",
    )

    private val timerPrefix = setOf("dat", "bat", "hen", "gio", "cho", "giup", "minh", "con", "chau", "ba", "ong", "me")
    private val digits = mapOf(
        "khong" to 0, "mot" to 1, "hai" to 2, "ba" to 3, "bon" to 4, "tu" to 4,
        "nam" to 5, "lam" to 5, "nham" to 5, "sau" to 6, "bay" to 7, "tam" to 8, "chin" to 9,
    )
    private val numberWords = digits.keys + "muoi" + "nua"

    fun parse(text: String): CookCommand {
        val words = SpokenText.words(text).map { it.plain }
        if (words.isEmpty() || words.size > MAX_WORDS) return CookCommand.Unknown
        if ("hen" in words) return parseTimer(words)
        val found = mutableSetOf<CookCommand>()
        var i = 0
        while (i < words.size) {
            val match = phrases.firstOrNull { (p, _) -> words.subList(i, words.size).take(p.size) == p }
            when {
                match != null -> { found += match.second; i += match.first.size }
                words[i] in fillers -> i++
                else -> return CookCommand.Unknown
            }
        }
        return found.singleOrNull() ?: CookCommand.Unknown
    }

    /** "hẹn giờ 10 phút", "đặt hẹn giờ mười lăm phút nhé", "hẹn giờ nửa tiếng", "hẹn giờ 1 tiếng rưỡi". */
    private fun parseTimer(words: List<String>): CookCommand {
        val unitAt = words.indexOfFirst { it == "phut" || it == "tieng" }
        if (unitAt < 0) return CookCommand.Unknown
        val hour = words[unitAt] == "tieng"
        val half = hour && words.getOrNull(unitAt + 1) == "ruoi"
        if (words.drop(unitAt + if (half) 2 else 1).any { it !in fillers }) return CookCommand.Unknown
        var start = unitAt
        if (unitAt > 0 && words[unitAt - 1].all(Char::isDigit)) start = unitAt - 1
        else while (start > 0 && words[start - 1] in numberWords && unitAt - start < 3) start--
        val amount = words.subList(start, unitAt)
        val prefix = words.subList(0, start)
        if (amount.isEmpty() || "hen" !in prefix || prefix.any { it !in timerPrefix }) return CookCommand.Unknown
        val minutes = when {
            amount == listOf("nua") -> if (hour && !half) 30 else return CookCommand.Unknown
            hour -> (number(amount) ?: return CookCommand.Unknown) * 60 + if (half) 30 else 0
            else -> number(amount) ?: return CookCommand.Unknown
        }
        return if (minutes in 1..MAX_TIMER_MINUTES) CookCommand.Timer(minutes) else CookCommand.Unknown
    }

    /** Digits ("15") or Vietnamese number words up to 99 ("mười lăm", "hai mươi mốt"). */
    internal fun number(words: List<String>): Int? {
        if (words.size == 1) words[0].toIntOrNull()?.let { return it }
        return when (words.size) {
            1 -> if (words[0] == "muoi") 10 else digits[words[0]]
            2 -> when {
                words[0] == "muoi" -> digits[words[1]]?.takeIf { it > 0 }?.let { 10 + it }
                words[1] == "muoi" -> digits[words[0]]?.takeIf { it > 1 }?.let { it * 10 }
                else -> null
            }
            3 -> if (words[1] != "muoi") null else {
                val tens = digits[words[0]]?.takeIf { it > 1 }
                val ones = digits[words[2]]?.takeIf { it > 0 }
                if (tens == null || ones == null) null else tens * 10 + ones
            }
            else -> null
        }
    }
}
