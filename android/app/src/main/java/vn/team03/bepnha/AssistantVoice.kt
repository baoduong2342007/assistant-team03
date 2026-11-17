package vn.team03.bepnha

import vn.team03.bepnha.voice.ConfirmResult
import vn.team03.bepnha.voice.VoiceConfirm
import vn.team03.bepnha.voice.VoiceErrorKind

/** What to do with a sentence the microphone heard. */
sealed interface HeardIntent {
    data object Confirm : HeardIntent
    data object Cancel : HeardIntent
    /** A proposal is waiting but the reply was not a clear yes or no. */
    data object AskAgain : HeardIntent
    data class Send(val text: String) : HeardIntent
}

/** Pure voice rules for the assistant screen: routing what was heard, and wording what is read aloud. */
object AssistantVoice {
    fun route(text: String, proposalPending: Boolean): HeardIntent {
        val heard = text.trim().take(1000)
        if (!proposalPending) return HeardIntent.Send(heard)
        return when (VoiceConfirm.parse(heard)) {
            ConfirmResult.Yes -> HeardIntent.Confirm
            ConfirmResult.No -> HeardIntent.Cancel
            ConfirmResult.Unclear -> HeardIntent.AskAgain
        }
    }

    /** "bác" → "Bác"; used at the start of spoken sentences. */
    fun title(addressAs: String) = VoiceSettings.clean(addressAs).replaceFirstChar { it.titlecase(PlannerEngine.locale) }

    /** Removes symbols a speech engine would read literally or stumble on. */
    fun spoken(text: String): String = text
        .replace("→", " thành ").replace("·", ",").replace("\n", ". ")
        .replace(Regex("[“”\"□*_#•]"), "").replace(Regex("\\.\\s*\\."), ".")
        .replace(Regex("\\s+"), " ").replace(Regex(" ([,.;:!?])"), "$1").trim()

    /** What to read when a change waits for confirmation; long multi-meal plans are summarized. */
    fun proposalPrompt(descriptions: List<String>, meals: Int, addressAs: String): String {
        val ask = "${title(addressAs)} nói đồng ý để áp dụng, hoặc không để hủy."
        return if (meals >= 2 || descriptions.size > 3) "Mình đề xuất đổi món cho $meals bữa. ${title(addressAs)} xem trên màn hình. $ask"
        else descriptions.joinToString(". ") + ". " + ask
    }

    fun askAgain(addressAs: String) = "Mình chưa nghe rõ. ${title(addressAs)} nói đồng ý để áp dụng, hoặc không để hủy nhé."

    fun error(kind: VoiceErrorKind, addressAs: String): String = when (kind) {
        VoiceErrorKind.NotHeard -> "Mình chưa nghe rõ. ${title(addressAs)} bấm nút và nói lại nhé."
        VoiceErrorKind.NoNetwork -> "Chưa kết nối được để nghe giọng nói. ${title(addressAs)} có thể gõ chữ thay thế."
        VoiceErrorKind.NoPermission -> "Cần cho phép dùng micro để nghe ${VoiceSettings.clean(addressAs)} nói. Bấm nút micro rồi chọn Cho phép."
        VoiceErrorKind.Unsupported -> "Điện thoại này chưa nhận được giọng nói tiếng Việt. ${title(addressAs)} có thể gõ chữ."
    }
}
