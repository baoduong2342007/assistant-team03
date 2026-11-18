package vn.team03.bepnha.voice

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.team03.bepnha.voice.ConfirmResult.No
import vn.team03.bepnha.voice.ConfirmResult.Unclear
import vn.team03.bepnha.voice.ConfirmResult.Yes

class VoiceConfirmTest {
    private fun check(expected: ConfirmResult, vararg sentences: String) = sentences.forEach {
        assertEquals("\"$it\"", expected, VoiceConfirm.parse(it))
    }

    @Test fun acceptsEveryYesPhrase() = check(Yes,
        "đồng ý", "Được", "ừ làm đi", "ok", "làm đi", "OK!", "Oke", "okay", "ô kê")

    @Test fun acceptsYesWithPolitenessParticles() = check(Yes,
        "Dạ, đồng ý ạ.", "được rồi", "vâng, làm đi", "ừ được", "ok nhé", "được rồi con", "bà đồng ý")

    @Test fun acceptsEveryNoPhrase() = check(No,
        "không", "thôi", "hủy", "huỷ", "đừng", "Không!", "thôi đi")

    @Test fun acceptsNoWithParticlesAndNegatedYes() = check(No,
        "thôi con ạ", "dạ không", "không đồng ý", "không được", "đừng làm", "hủy đi nhé", "không không")

    @Test fun acceptsTextWithoutDiacritics() {
        check(Yes, "dong y", "duoc", "u lam di", "lam di", "duoc roi", "da dong y a")
        check(No, "khong", "thoi", "huy", "khong dong y", "khong duoc", "thoi di")
    }

    @Test fun unaccentedDungIsAmbiguousBetweenDungAndDung() {
        // "dung" could be "đừng" (don't) or "đúng" (right): never guess.
        check(Unclear, "dung", "dung lam")
        check(Unclear, "đúng", "đúng rồi")
        check(No, "đừng", "đừng nhé")
    }

    @Test fun mixedMeaningsAreUnclear() = check(Unclear,
        "được không", "ừ làm đi, à thôi", "ok hủy", "đồng ý hay không", "khong, lam di", "không, được")

    @Test fun longOrUnknownSentencesAreUnclear() = check(Unclear,
        "bà muốn đổi món canh chua thành canh rau được không",
        "được nhưng mà đổi món khác",
        "làm gì vậy",
        "có",
        "ừ",
        "dạ",
        "chờ chút",
        "thời gian",
        "khong biet nua",
        "",
        "   ...  ",
    )

    @Test fun capsAndPunctuationDoNotMatter() = check(Yes, "ĐỒNG Ý!!!", "Được.", "  làm   đi  ")
}
