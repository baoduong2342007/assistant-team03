package vn.team03.bepnha

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import java.time.LocalDate

@Preview(name = "Phone 360", widthDp = 360, heightDp = 800, showBackground = true)
@Preview(name = "Small phone, text 200%", widthDp = 320, heightDp = 640, fontScale = 2f, showBackground = true)
@Preview(name = "Phone 412, text 130%", widthDp = 412, heightDp = 915, fontScale = 1.3f, showBackground = true)
@Preview(name = "Landscape, text 150%", widthDp = 640, heightDp = 360, fontScale = 1.5f, showBackground = true)
annotation class PhonePreviews

private val previewDate = LocalDate.of(2026, 11, 6)
private val previewRecipes = listOf(
    Recipe("pork", "Thịt kho trứng", RecipeKind.MAIN, "🍲", 35, 65000, "", emptyList(), emptyList(), ""),
    Recipe("soup", "Canh rau ngót thịt bằm", RecipeKind.SOUP, "🥣", 15, 25000, "", emptyList(), emptyList(), ""),
    Recipe("greens", "Rau muống xào tỏi", RecipeKind.VEG, "🥬", 10, 15000, "", emptyList(), emptyList(), ""),
)
private val previewContext = AssistantUiState(
    context = "Bữa tối cho 4 người", date = "Thứ Sáu, 6 tháng 11",
    question = "Món nào nấu nhanh?",
    suggestions = listOf("Đổi món mặn", "Món nấu nhanh", "Nấu món nào trước?"),
    recipeNames = mapOf("pork" to "Thịt kho trứng"),
)

@PhonePreviews
@Composable fun TodayPhonePreview() {
    BepNhaTheme {
        KitchenFrame(title = "Bếp Nhà") {
            TodayContent(
                TodayUi(previewDate, previewDate, (0L..6L).map(previewDate::plusDays), Meal.DINNER, 4,
                    previewRecipes, emptySet(), "105.000 đ", 40),
                {}, {}, {}, {}, {}, {}, {}, {},
            )
        }
    }
}

@PhonePreviews
@Composable fun AssistantPhonePreview() {
    BepNhaTheme {
        KitchenFrame(title = "Hỏi Bếp Nhà", assistant = true, showBack = true) {
            AssistantPreviewContent(previewContext)
        }
    }
}

@PhonePreviews
@Composable fun AssistantAnswerPreview() {
    BepNhaTheme {
        KitchenFrame(title = "Hỏi Bếp Nhà", assistant = true, showBack = true) {
            AssistantPreviewContent(previewContext.copy(configured = true, question = "", messages = listOf(
                AssistantChatMessage("user", "Mình nên nấu món nào trước?", id = "question"),
                AssistantChatMessage("assistant", "Bạn nên bắt đầu với thịt kho trứng vì món này cần khoảng 35 phút.\n\nTrong lúc chờ thịt mềm, nhặt rau và chuẩn bị canh. Rau xào nên làm gần giờ ăn để giữ độ giòn.",
                    listOf("pork"), id = "answer"),
            )))
        }
    }
}

@PhonePreviews
@Composable fun AssistantFailurePreview() {
    BepNhaTheme {
        KitchenFrame(title = "Hỏi Bếp Nhà", assistant = true, showBack = true) {
            AssistantPreviewContent(previewContext.copy(configured = true, question = "",
                messages = listOf(AssistantChatMessage("user", "Đổi món mặn cho bữa này", id = "failed")),
                error = "Chưa kết nối được trợ lý. Hãy thử lại hoặc tìm trong công thức có sẵn.",
                failedMessageId = "failed", canRetry = true))
        }
    }
}

@PhonePreviews
@Composable fun AssistantProposalPreview() {
    BepNhaTheme {
        KitchenFrame(title = "Hỏi Bếp Nhà", assistant = true, showBack = true) {
            AssistantPreviewContent(previewContext.copy(configured = true, question = "",
                proposal = listOf("Bữa tối, 6 tháng 11\nThay thịt kho trứng bằng đậu hũ sốt cà chua. Nguyên liệu và lịch nấu sẽ cập nhật."),
                messages = listOf(AssistantChatMessage("assistant", "Bạn có thể đổi sang đậu hũ sốt cà chua.", id = "proposal"))))
        }
    }
}

/** Models the reduced viewport above an IME; an actual IME is verified on a device. */
@Preview(name = "Reduced height with timer", widthDp = 640, heightDp = 320, fontScale = 1.5f)
@Composable fun AssistantCompactPreview() {
    BepNhaTheme {
        KitchenFrame(title = "Hỏi Bếp Nhà", assistant = true, showBack = true) {
            AssistantPreviewContent(previewContext.copy(configured = true, timer = KitchenTimerUi("Thịt kho trứng", "12:34")))
        }
    }
}
