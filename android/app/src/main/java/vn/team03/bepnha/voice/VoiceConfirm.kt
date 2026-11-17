package vn.team03.bepnha.voice

import vn.team03.bepnha.PlannerEngine
import java.text.Normalizer

enum class ConfirmResult { Yes, No, Unclear }

/** A spoken word kept in two forms: as heard (lowercase, NFC) and stripped of diacritics. */
internal data class SpokenWord(val heard: String, val plain: String) {
    val accented: Boolean get() = heard.any { it.code > 0x7F }
}

internal object SpokenText {
    private val separators = "[^\\p{L}\\p{N}]+".toRegex()

    fun words(text: String): List<SpokenWord> =
        Normalizer.normalize(text.lowercase(PlannerEngine.locale), Normalizer.Form.NFC)
            .split(separators).filter { it.isNotEmpty() }
            .map { SpokenWord(it, PlannerEngine.normalize(it)) }
}

/**
 * Turns a short spoken reply into yes / no for a confirmation prompt.
 *
 * Deliberately strict: a reply must consist only of known phrases plus politeness
 * particles ("ạ", "nhé", ...). Anything longer, unknown, or mixing yes and no is
 * [ConfirmResult.Unclear] so the app asks again instead of guessing.
 */
object VoiceConfirm {
    const val MAX_WORDS = 6

    /** A phrase word: its plain form, and the accented spellings it may be heard as. */
    private class Word(val plain: String, vararg spellings: String) {
        val spellings = spellings.toSet()
    }

    private class Phrase(val result: ConfirmResult, val words: List<Word>)

    private fun w(plain: String, vararg spellings: String) = Word(plain, *spellings)

    // Plain forms that also spell an unrelated word ("dung": đừng / đúng) match only when accented.
    private val accentRequired = setOf("dung")

    private val phrases: List<Phrase> = listOf(
        // Negated "yes" phrases are a clear no, matched before their shorter parts.
        Phrase(ConfirmResult.No, listOf(w("khong", "không"), w("dong", "đồng"), w("y", "ý"))),
        Phrase(ConfirmResult.No, listOf(w("khong", "không"), w("duoc", "được"))),
        Phrase(ConfirmResult.No, listOf(w("khong", "không"), w("lam", "làm"))),
        Phrase(ConfirmResult.No, listOf(w("dung", "đừng"), w("lam", "làm"))),
        Phrase(ConfirmResult.Yes, listOf(w("dong", "đồng"), w("y", "ý"))),
        Phrase(ConfirmResult.Yes, listOf(w("u", "ừ"), w("lam", "làm"), w("di", "đi"))),
        Phrase(ConfirmResult.Yes, listOf(w("lam", "làm"), w("di", "đi"))),
        Phrase(ConfirmResult.Yes, listOf(w("duoc", "được"))),
        Phrase(ConfirmResult.Yes, listOf(w("ok"))),
        Phrase(ConfirmResult.Yes, listOf(w("oke"))),
        Phrase(ConfirmResult.Yes, listOf(w("okay"))),
        Phrase(ConfirmResult.Yes, listOf(w("o", "ô"), w("ke", "kê"))),
        Phrase(ConfirmResult.No, listOf(w("khong", "không"))),
        Phrase(ConfirmResult.No, listOf(w("thoi", "thôi"))),
        Phrase(ConfirmResult.No, listOf(w("huy", "hủy", "huỷ"))),
        Phrase(ConfirmResult.No, listOf(w("dung", "đừng"))),
    ).sortedByDescending { it.words.size }

    /** Particles that carry no yes/no meaning on their own. */
    private val fillers: Set<String> = setOf(
        "a", "à", "ạ", "u", "ừ", "o", "ờ", "da", "dạ", "vang", "vâng",
        "nhe", "nhé", "nha", "nhá", "roi", "rồi", "di", "đi", "thi", "thì",
        "con", "chau", "cháu", "ba", "bà", "ong", "ông", "me", "mẹ", "minh", "mình",
    )

    private fun Word.matches(word: SpokenWord): Boolean =
        if (word.accented) word.heard in spellings
        else word.plain == plain && plain !in accentRequired

    private fun isFiller(word: SpokenWord): Boolean =
        if (word.accented) word.heard in fillers else word.plain in fillers

    private val clauseBreaks = "[,.;:!?…]+".toRegex()

    fun parse(text: String): ConfirmResult {
        // A phrase never spans punctuation: "không, làm đi" is two answers, not "không làm".
        val clauses = text.split(clauseBreaks).map(SpokenText::words).filter { it.isNotEmpty() }
        val count = clauses.sumOf { it.size }
        if (count == 0 || count > MAX_WORDS) return ConfirmResult.Unclear
        val found = mutableSetOf<ConfirmResult>()
        for (words in clauses) {
            var i = 0
            while (i < words.size) {
                val phrase = phrases.firstOrNull { p ->
                    i + p.words.size <= words.size && p.words.indices.all { p.words[it].matches(words[i + it]) }
                }
                when {
                    phrase != null -> { found += phrase.result; i += phrase.words.size }
                    isFiller(words[i]) -> i++
                    else -> return ConfirmResult.Unclear
                }
            }
        }
        return found.singleOrNull() ?: ConfirmResult.Unclear
    }
}
