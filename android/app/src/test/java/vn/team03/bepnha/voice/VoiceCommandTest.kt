package vn.team03.bepnha.voice

import org.junit.Assert.assertEquals
import org.junit.Test
import vn.team03.bepnha.voice.CookCommand.Back
import vn.team03.bepnha.voice.CookCommand.Next
import vn.team03.bepnha.voice.CookCommand.Repeat
import vn.team03.bepnha.voice.CookCommand.Timer
import vn.team03.bepnha.voice.CookCommand.Undo
import vn.team03.bepnha.voice.CookCommand.Unknown

class VoiceCommandTest {
    private fun check(expected: CookCommand, vararg sentences: String) = sentences.forEach {
        assertEquals("\"$it\"", expected, VoiceCommand.parse(it))
    }

    @Test fun next() = check(Next, "tiếp", "Tiếp theo", "tiếp tục", "bước tiếp theo", "tiếp đi con", "bước sau nhé")

    @Test fun repeat() = check(Repeat, "đọc lại", "Đọc lại đi", "nói lại giúp bà", "đọc lại nào")

    @Test fun back() = check(Back, "quay lại", "quay lại bước trước", "trở lại", "bước trước đi")

    @Test fun undo() = check(Undo, "hoàn tác", "Hoàn tác đi", "hoàn tác giúp mình")

    @Test fun timerWithDigits() {
        check(Timer(5), "hẹn giờ 5 phút", "Hẹn giờ 5 phút nhé.", "đặt hẹn giờ 5 phút", "hẹn giờ cho bà 5 phút")
        check(Timer(15), "hẹn 15 phút")
        check(Timer(120), "hẹn giờ 2 tiếng")
    }

    @Test fun timerWithNumberWords() {
        check(Timer(3), "hẹn giờ ba phút")
        check(Timer(5), "hẹn giờ năm phút")
        check(Timer(10), "hẹn giờ mười phút")
        check(Timer(15), "hẹn giờ mười lăm phút")
        check(Timer(20), "hẹn giờ hai mươi phút")
        check(Timer(21), "hẹn giờ hai mươi mốt phút")
        check(Timer(25), "hẹn giờ hai mươi lăm phút")
        check(Timer(30), "hẹn giờ nửa tiếng")
        check(Timer(60), "hẹn giờ một tiếng")
        check(Timer(90), "hẹn giờ một tiếng rưỡi")
    }

    @Test fun acceptsTextWithoutDiacritics() {
        check(Next, "tiep", "tiep theo", "buoc tiep")
        check(Repeat, "doc lai")
        check(Back, "quay lai")
        check(Undo, "hoan tac")
        check(Timer(10), "hen gio 10 phut", "hen gio muoi phut")
        check(Timer(45), "hen gio bon muoi lam phut nhe")
    }

    @Test fun invalidTimersAreUnknown() = check(Unknown,
        "hẹn giờ phút",
        "hẹn giờ 0 phút",
        "hẹn giờ 500 phút",
        "hẹn giờ 5",
        "hẹn giờ 5 phút rồi đọc lại",
        "5 phút",
        "hẹn giờ nửa phút",
        "hẹn giờ mười mười phút",
    )

    @Test fun mixedLongOrUnrelatedSentencesAreUnknown() = check(Unknown,
        "tiếp rồi quay lại",
        "đọc lại hay tiếp theo",
        "bà muốn tiếp tục nấu món khác vào ngày mai được không",
        "thêm hai bát nước",
        "lại",
        "bước",
        "đồng ý",
        "",
    )
}
