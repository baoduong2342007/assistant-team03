package vn.team03.bepnha

import vn.team03.bepnha.voice.LiveCall
import vn.team03.bepnha.voice.VoiceCommand

/** The functions Gemini Live may call while cooking. Only step control and timers: never menu changes. */
object CookLiveTools {
    data class Spec(val name: String, val description: String, val takesMinutes: Boolean = false)

    sealed interface Command {
        data object Next : Command
        data object Back : Command
        data object Repeat : Command
        data class Timer(val minutes: Int) : Command
        data object CancelTimer : Command
        data class Invalid(val reason: String) : Command
    }

    const val MINUTES = "minutes"

    val specs = listOf(
        Spec("next_step", "Chuyển sang bước nấu tiếp theo, khi người nấu nói đã xong bước này hoặc muốn làm tiếp."),
        Spec("previous_step", "Quay lại bước nấu trước đó."),
        Spec("repeat_step", "Lấy lại nội dung bước nấu hiện tại để đọc lại."),
        Spec("set_timer", "Đặt đồng hồ hẹn giờ cho bước đang nấu.", takesMinutes = true),
        Spec("cancel_timer", "Tắt đồng hồ hẹn giờ mới nhất của bữa đang nấu."),
    )

    fun parse(call: LiveCall): Command = when (call.name) {
        "next_step" -> Command.Next
        "previous_step" -> Command.Back
        "repeat_step" -> Command.Repeat
        "cancel_timer" -> Command.CancelTimer
        "set_timer" -> {
            val minutes = call.args[MINUTES]?.trim()?.toDoubleOrNull()
            when {
                minutes == null || minutes.isNaN() -> Command.Invalid("Thiếu số phút.")
                minutes % 1.0 != 0.0 || minutes.toInt() !in 1..VoiceCommand.MAX_TIMER_MINUTES ->
                    Command.Invalid("Chỉ hẹn được từ 1 đến ${VoiceCommand.MAX_TIMER_MINUTES} phút.")
                else -> Command.Timer(minutes.toInt())
            }
        }
        else -> Command.Invalid("Không có chức năng ${call.name.take(40)}.")
    }
}

/** Wording sent to Gemini Live: the system instruction, the opening note and app notes. */
object CookLivePrompt {
    /** Marks text that comes from the app rather than from the person cooking. */
    const val APP = "[Ứng dụng]"

    fun instruction(engine: PlannerEngine, state: PlannerState, session: CookAlong): String {
        val who = VoiceSettings.clean(state.voice.addressAs)
        val me = AssistantVoice.self(state.voice.addressAs)
        val people = state.profile.people
        val menu = engine.menu(state, session.date, session.meal)
        val ingredients = engine.ingredients(menu, people).joinToString("; ") { item ->
            "${item.name} ${PlannerEngine.quantity(item.amount)} ${item.unit}".trim()
        }
        val steps = session.steps.mapIndexed { i, step -> "${i + 1}. [${step.title}] ${step.text}" }.joinToString("\n")
        return """
            Bạn là trợ lý nấu ăn của ứng dụng Bếp Nhà, đang hướng dẫn nấu từng bước bằng giọng nói.
            Luôn nói tiếng Việt (vi-VN), giọng ấm áp, rõ ràng. Mỗi lượt chỉ 1-3 câu ngắn, trừ khi đọc một bước nấu.
            Gọi người nấu là "$who", xưng là "$me".

            Bữa đang nấu: ${session.meal.title}, ${PlannerEngine.fullDate(session.date)}, cho $people người.
            Món: ${menu.joinToString { it.name }}.
            Nguyên liệu: $ingredients.
            Các bước (${session.steps.size} bước):
            $steps
            Lúc bắt đầu, người nấu đang ở bước ${session.index + 1}.

            Quy tắc:
            - Muốn sang bước tiếp, nói đã xong bước này: gọi next_step. Quay lại: previous_step. Đọc lại: repeat_step.
              Hẹn giờ: set_timer với số phút. Tắt hẹn giờ: cancel_timer.
            - Chỉ đổi bước bằng cách gọi hàm. Sau khi gọi, đọc đúng nội dung trường "step" hoặc "say" trong kết quả, không thêm bớt.
            - Trả lời ngắn các câu hỏi về món đang nấu: nguyên liệu, cách làm, thay thế, cách nhận biết món đã chín, an toàn thực phẩm.
              Không bịa thêm bước hoặc nguyên liệu không có ở trên; nếu không chắc thì nói là không chắc.
            - Không đổi món, không sửa thực đơn, không thêm đồ đi chợ. Nếu được nhờ, nói $who dùng mục Trò chuyện của Bếp Nhà.
            - Tin nhắn bắt đầu bằng "$APP" là thông báo của ứng dụng, không phải lời người nấu. Làm theo yêu cầu trong đó.
            - Nếu nghe không rõ, hỏi lại thật ngắn.
        """.trimIndent()
    }

    fun greeting() = "$APP Người nấu vừa bật trò chuyện. Chào thật ngắn trong một câu và cho biết đang nghe."

    /** The user pressed a button; Gemini reads the new step instead of the phone's speech engine. */
    fun readStep(spoken: String) = "$APP Người nấu vừa bấm nút trên màn hình. Hãy đọc nguyên văn: $spoken"

    fun announce(text: String) = "$APP Hãy nói lại cho người nấu: $text"
}
