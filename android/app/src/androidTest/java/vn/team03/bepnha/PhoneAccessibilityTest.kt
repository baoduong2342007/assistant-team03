package vn.team03.bepnha

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import java.io.File

/** Checks actual composed phone layouts; it doesn't change the device's global font setting. */
class PhoneAccessibilityTest {
    @get:Rule val compose = createComposeRule()

    @Composable private fun Phone(width: Dp = 320.dp, height: Dp = 640.dp, fontScale: Float = 2f, content: @Composable () -> Unit) {
        val density = LocalDensity.current.density
        CompositionLocalProvider(LocalDensity provides Density(density, fontScale)) {
            BepNhaTheme { Box(Modifier.requiredSize(width, height)) { content() } }
        }
    }

    @Test fun fourTabsAndAssistantStayReadableAndTappableAt200Percent() {
        compose.setContent { Phone { KitchenFrame("Bếp Nhà") { } } }
        compose.onNodeWithTag("ask-assistant").assertIsDisplayed().assertHeightIsAtLeast(56.dp)
        listOf("today", "plan", "shopping", "cook").forEach {
            compose.onNodeWithTag("tab-$it").assertIsDisplayed().assertHeightIsAtLeast(48.dp)
        }
        assertNoTextOverflow()
        capture("phone-navigation-320-text200.png")
    }

    @Test fun chatInputAndSendFitWithLargeTextAndTimer() {
        compose.setContent {
            Phone {
                KitchenFrame("Hỏi Bếp Nhà", assistant = true, showBack = true) {
                    AssistantContent(AssistantUiState("Bữa tối cho 4 người", "Thứ Sáu, 6 tháng 11",
                        configured = true, question = "Mình nên nấu món nào trước?",
                        timer = KitchenTimerUi("Thịt kho trứng", "12:34")))
                }
            }
        }
        compose.onNodeWithTag("assistant-input").assertIsDisplayed()
        compose.onNodeWithTag("assistant-send").assertIsDisplayed().assertIsEnabled().assertHeightIsAtLeast(56.dp)
        assertWithinRoot("assistant-send")
        capture("phone-chat-320-text200-timer.png")
    }

    @Test fun reducedHeightMovesContextAndTimerIntoTheScrollableConversation() {
        compose.setContent {
            Phone(width = 320.dp, height = 300.dp, fontScale = 1.5f) {
                KitchenFrame("Hỏi Bếp Nhà", assistant = true, showBack = true) {
                    AssistantContent(AssistantUiState("Bữa tối cho 4 người", "Thứ Sáu, 6 tháng 11",
                        configured = true, question = "Món nào nấu nhanh?",
                        timer = KitchenTimerUi("Thịt kho trứng", "12:34")))
                }
            }
        }
        compose.onNodeWithTag("assistant-send").assertIsDisplayed().assertIsEnabled()
        assertWithinRoot("assistant-input")
        assertWithinRoot("assistant-send")
        capture("phone-chat-reduced-height.png")
    }

    @Test fun suggestionOnlyFillsComposerAndOfflineActionHasAnExplicitLabel() {
        val draft = mutableStateOf("")
        var sends = 0
        compose.setContent {
            Phone(fontScale = 1f) {
                KitchenFrame("Hỏi Bếp Nhà", assistant = true) {
                    AssistantContent(AssistantUiState("Bữa tối cho 4 người", "Hôm nay",
                        question = draft.value, suggestions = listOf("Món nấu nhanh")),
                        onQuestion = { draft.value = it }, onSend = { sends++ })
                }
            }
        }
        compose.onNodeWithText("Món nấu nhanh").performScrollTo().performClick()
        compose.onNodeWithTag("assistant-input").assertTextContains("Món nấu nhanh")
        assertEquals(0, sends)
        compose.onNodeWithTag("assistant-send").assertTextEquals("Tìm công thức").performClick()
        assertEquals(1, sends)
        capture("phone-chat-offline.png")
    }

    @Test fun longProposalCanBeReadAndConfirmedAt200Percent() {
        var confirmed = false
        compose.setContent {
            Phone {
                KitchenFrame("Hỏi Bếp Nhà", assistant = true) {
                    AssistantContent(AssistantUiState("Bữa tối cho 4 người", "Thứ Sáu, 6 tháng 11",
                        configured = true,
                        proposal = listOf("Bữa tối ngày 6 tháng 11. Thay thịt kho trứng bằng đậu hũ sốt cà chua. Nguyên liệu và lịch nấu của bữa này sẽ được cập nhật.")),
                        onConfirm = { confirmed = true })
                }
            }
        }
        compose.onNodeWithTag("assistant-confirm").performScrollTo().assertIsDisplayed().performClick()
        assertTrue(confirmed)
        capture("phone-chat-proposal-text200.png")
    }

    @Test fun aReplyDoesNotPullTheReaderAwayFromOlderMessages() {
        val messages = mutableStateOf((1..20).map {
            AssistantChatMessage("assistant", "Câu trả lời $it. Chuẩn bị nguyên liệu trước khi bắt đầu nấu bữa cơm.", id = "message-$it")
        })
        compose.setContent {
            Phone(fontScale = 1f) {
                KitchenFrame("Hỏi Bếp Nhà", assistant = true) {
                    AssistantContent(AssistantUiState("Bữa tối cho 4 người", "Hôm nay", configured = true, messages = messages.value))
                }
            }
        }
        val history = compose.onNodeWithTag("assistant-history")
        history.performTouchInput { swipeUp(durationMillis = 500) }
        compose.waitForIdle()
        val before = history.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        compose.runOnIdle { messages.value = messages.value + AssistantChatMessage("assistant", "Đây là câu trả lời mới.", id = "new") }
        compose.waitForIdle()
        val after = history.fetchSemanticsNode().config[SemanticsProperties.VerticalScrollAxisRange].value()
        assertEquals(before, after, 0.01f)
        compose.onNodeWithText("Tin nhắn mới").assertIsDisplayed().performClick()
        compose.onNodeWithText("Đây là câu trả lời mới.").assertIsDisplayed()
    }

    @OptIn(ExperimentalTestApi::class)
    @Test fun landscapeComposerFitsTheViewportAboveALargeKeyboardAt200Percent() {
        compose.setContent {
            DeviceConfigurationOverride(
                DeviceConfigurationOverride.ForcedSize(androidx.compose.ui.unit.DpSize(640.dp, 140.dp)) then
                    DeviceConfigurationOverride.FontScale(2f)
            ) {
                BepNhaTheme {
                    AssistantContent(AssistantUiState("Bữa tối cho 4 người", "Hôm nay",
                        configured = true, question = "Món nấu nhanh"))
                }
            }
        }
        compose.onNodeWithTag("assistant-input").assertIsDisplayed()
        compose.onNodeWithTag("assistant-send").assertIsDisplayed().assertIsEnabled()
        assertWithinRoot("assistant-input", "assistant-screen")
        assertWithinRoot("assistant-send", "assistant-screen")
        val bitmap = compose.onNodeWithTag("assistant-screen").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "native-previews").apply { mkdirs() }
        File(directory, "phone-chat-landscape-text200-compact.png").outputStream().use {
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
        }
    }

    private fun assertWithinRoot(tag: String, rootTag: String = "native-root") {
        val root = compose.onNodeWithTag(rootTag).fetchSemanticsNode().boundsInRoot
        val bounds = compose.onNodeWithTag(tag).fetchSemanticsNode().boundsInRoot
        assertTrue("$tag outside root: $bounds / $root", bounds.left >= root.left && bounds.right <= root.right &&
            bounds.top >= root.top && bounds.bottom <= root.bottom)
    }

    private fun assertNoTextOverflow() {
        compose.onAllNodes(hasText("Hôm nay") or hasText("Thực đơn") or hasText("Đi chợ") or hasText("Vào bếp"), useUnmergedTree = true)
            .fetchSemanticsNodes().forEach { node ->
                val results = mutableListOf<TextLayoutResult>()
                node.config.getOrElseNullable(SemanticsActions.GetTextLayoutResult) { null }?.action?.invoke(results)
                assertFalse("Clipped label: ${node.config.getOrElse(SemanticsProperties.Text) { emptyList() }}",
                    results.any { it.hasVisualOverflow })
            }
    }

    private fun capture(name: String) {
        val bitmap = compose.onNodeWithTag("native-root").captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(context.getExternalFilesDir(null), "native-previews").apply { mkdirs() }
        File(directory, name).outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
}
