package vn.team03.bepnha

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

@RunWith(AndroidJUnit4::class)
class AssistantFlowTest {
    @get:Rule val compose = createAndroidComposeRule<MainActivity>()
    private fun vm() = ViewModelProvider(compose.activity)[PlannerViewModel::class.java]

    @Test fun assistantIsReachableFromEveryMainTabAndKeepsConversationAfterRecipeAndRotation() {
        listOf("today", "plan", "shopping", "cook").forEach { tab ->
            compose.onNodeWithTag("tab-$tab").performClick()
            compose.onNodeWithTag("ask-assistant").performClick()
            compose.onNodeWithTag("assistant-screen").assertIsDisplayed()
            compose.runOnIdle { vm().closeAssistant() }
        }
        compose.runOnIdle {
            val base = vm().state
            vm().assistantTransport = { _, _, _ -> AssistantReply("Công thức bạn cần", listOf(AssistantAction(AssistantActionType.open_recipe, base.date, base.meal, recipeId = "tofu"))) }
            vm().openAssistant(); vm().askAssistant("Xem đậu hũ")
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        compose.onNodeWithContentDescription("Đóng").performClick()
        compose.onNodeWithText("Công thức bạn cần").assertExists()
        compose.activityRule.scenario.recreate()
        compose.onNodeWithText("Công thức bạn cần").assertExists()
    }

    @Test fun proposalRequiresConfirmationAndDoubleConfirmationCannotReplay() {
        lateinit var original: PlannerState
        compose.runOnIdle {
            original = vm().state
            vm().assistantTransport = { _, _, _ -> AssistantReply("Đề xuất", listOf(AssistantAction(AssistantActionType.set_people, original.date, original.meal, people = if (original.profile.people == 6) 5 else 6))) }
            vm().openAssistant(); vm().askAssistant("Đổi khẩu phần")
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        compose.runOnIdle { assertEquals(original, vm().state) }
        compose.onNodeWithTag("assistant-confirm").performScrollTo().performClick()
        compose.runOnIdle {
            assertNotEquals(original.profile.people, vm().state.profile.people)
            val applied = vm().state
            vm().confirmAssistantProposal()
            assertEquals(applied, vm().state)
        }
    }

    @Test fun changedMealInvalidatesProposalAndCancellationLeavesDataAlone() {
        compose.runOnIdle {
            val base = vm().state
            vm().assistantTransport = { _, _, _ -> AssistantReply("Đề xuất", listOf(AssistantAction(AssistantActionType.set_people, base.date, base.meal, people = 8))) }
            vm().openAssistant(); vm().askAssistant("Tám người")
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        compose.runOnIdle {
            val before = vm().state
            vm().cancelAssistantProposal()
            assertEquals(before, vm().state)
            vm().askAssistant("Tám người")
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        compose.runOnIdle {
            vm().selectMeal(if (vm().state.meal == Meal.LUNCH) Meal.DINNER else Meal.LUNCH)
            val before = vm().state
            vm().confirmAssistantProposal()
            assertEquals(before, vm().state)
            assertNotNull(vm().assistantError)
        }
    }

    @Test fun responseForPreviousMealIsDiscarded() {
        lateinit var selected: PlannerState
        compose.runOnIdle {
            val base = vm().state
            vm().assistantTransport = { _, _, _ -> AssistantReply("Đề xuất cũ", listOf(AssistantAction(AssistantActionType.set_people, base.date, base.meal, people = 9))) }
            vm().openAssistant(); vm().askAssistant("Chín người")
            vm().selectMeal(if (base.meal == Meal.LUNCH) Meal.DINNER else Meal.LUNCH)
            selected = vm().state
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        compose.runOnIdle {
            assertEquals(selected, vm().state)
            assertNull(vm().assistantProposal)
            assertNotNull(vm().assistantError)
            assertEquals("Chín người", vm().assistantQuestion)
        }
    }

    @Test fun offlineLookupKeepsRecipeLinks() {
        compose.runOnIdle {
            vm().openAssistant(); vm().assistantQuestion = "Món nấu nhanh"; vm().lookupOffline()
            assertTrue(vm().assistantHistory.last().recipeIds.isNotEmpty())
            assertEquals(AssistantSource.LOCAL, vm().assistantHistory.last().source)
        }
    }

    @Test fun retryKeepsOneUserMessageAndDoesNotOverwriteTheNextDraft() {
        val calls = AtomicInteger()
        compose.runOnIdle {
            vm().assistantTransport = { _, history, _ ->
                assertTrue(history.isEmpty())
                if (calls.incrementAndGet() == 1) error("Dịch vụ tạm ngừng")
                AssistantReply("Đã kết nối lại", emptyList())
            }
            vm().openAssistant()
            vm().askAssistant("Món nấu nhanh")
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        compose.runOnIdle {
            assertNotNull(vm().failedMessageId)
            vm().assistantQuestion = "Câu hỏi tiếp theo"
            vm().retryAssistant()
            vm().retryAssistant()
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        compose.runOnIdle {
            assertEquals(2, calls.get())
            assertEquals(1, vm().assistantHistory.count { it.role == "user" })
            assertEquals("Đã kết nối lại", vm().assistantHistory.last().text)
            assertEquals("Câu hỏi tiếp theo", vm().assistantQuestion)
            assertNull(vm().assistantError)
        }
    }

    @Test fun doubleSendIsIgnoredAndClosingDiscardsLateActions() {
        val entered = CountDownLatch(1)
        val release = CountDownLatch(1)
        val calls = AtomicInteger()
        lateinit var base: PlannerState
        try {
            compose.runOnIdle {
                base = vm().state
                vm().assistantTransport = { _, _, _ ->
                    calls.incrementAndGet()
                    entered.countDown()
                    check(release.await(10, TimeUnit.SECONDS))
                    AssistantReply("Phản hồi đã hết hiệu lực", listOf(
                        AssistantAction(AssistantActionType.set_people, base.date, base.meal, people = 11)))
                }
                vm().openAssistant()
                vm().askAssistant("Đổi số người")
                vm().askAssistant("Không được gửi thêm")
                vm().assistantQuestion = "Bản nháp mới"
            }
            assertTrue(entered.await(5, TimeUnit.SECONDS))
            compose.runOnIdle {
                assertEquals(1, calls.get())
                assertEquals(1, vm().assistantHistory.count { it.role == "user" })
                vm().closeAssistant()
                vm().openAssistant()
            }
            release.countDown()
            compose.waitForIdle()
            compose.runOnIdle {
                assertFalse(vm().assistantBusy)
                assertNull(vm().assistantProposal)
                assertEquals(base, vm().state)
                assertEquals("Bản nháp mới", vm().assistantQuestion)
                assertFalse(vm().assistantHistory.any { it.text == "Phản hồi đã hết hiệu lực" })
            }
        } finally { release.countDown() }
    }

    @Test fun askingFromRecipeReturnsToThatRecipeAndKeepsDraftAfterRotation() {
        compose.runOnIdle {
            vm().openRecipe("tofu")
            vm().openAssistant()
            vm().assistantQuestion = "Giải thích cách nấu"
        }
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("assistant-input").assertTextContains("Giải thích cách nấu")
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.runOnIdle {
            assertFalse(vm().assistantVisible)
            assertEquals(PlannerSheet.RecipeDetail("tofu"), vm().sheet)
        }
    }

    @Test fun familyAndReminderDraftsSurviveActivityRecreation() {
        compose.runOnIdle { vm().navigate(Screen.FAMILY) }
        compose.onNodeWithTag("family-name").performTextReplacement("Nhà thử nghiệm")
        compose.onNodeWithContentDescription("Tăng số người ăn").performScrollTo().performClick()
        val people = compose.onNodeWithTag("family-people").fetchSemanticsNode().config[androidx.compose.ui.semantics.SemanticsProperties.Text].first().text
        compose.activityRule.scenario.recreate()
        compose.onNodeWithTag("family-name").assertTextContains("Nhà thử nghiệm")
        compose.onNodeWithTag("family-people").assertTextEquals(people)
        compose.runOnIdle { vm().openReminders() }
        val before = vm().reminderEnabled
        compose.onNodeWithTag("reminder-enabled").performScrollTo().performClick()
        compose.activityRule.scenario.recreate()
        if (before) compose.onNodeWithTag("reminder-enabled").assertIsOff()
        else compose.onNodeWithTag("reminder-enabled").assertIsOn()
    }

    @Test fun recipeContextWorksForOnDeviceLookupAndCanChangeWithinChat() {
        compose.runOnIdle {
            vm().openRecipe("tofu")
            vm().openAssistant()
            vm().assistantQuestion = "Nguyên liệu món này gồm gì?"
            vm().lookupOffline()
            assertEquals(listOf("tofu"), vm().assistantHistory.last().recipeIds)
            assertTrue(vm().assistantHistory.last().text.contains(vm().engine.recipes.getValue("tofu").ingredients.first().name))
            vm().openRecipe("eggs")
            vm().openAssistant()
            assertNull(vm().sheet)
            assertEquals("eggs", vm().assistantRecipeId)
            vm().closeAssistant()
            assertEquals(PlannerSheet.RecipeDetail("eggs"), vm().sheet)
        }
    }

    @Test fun invalidBackendActionHasReadableErrorAndCannotChangeTheMeal() {
        lateinit var base: PlannerState
        compose.runOnIdle {
            base = vm().state
            vm().assistantTransport = { _, _, _ ->
                AssistantReply("Không được hiển thị như một đề xuất hợp lệ", listOf(
                    AssistantAction(AssistantActionType.set_people, base.date, base.meal, people = 999)))
            }
            vm().openAssistant()
            vm().askAssistant("Đổi khẩu phần")
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        compose.runOnIdle {
            assertEquals(base, vm().state)
            assertNull(vm().assistantProposal)
            assertTrue(vm().assistantError.orEmpty().contains("Phản hồi trợ lý chưa hợp lệ"))
            assertFalse(vm().assistantError.orEmpty().contains("Failed requirement"))
            assertFalse(vm().assistantHistory.any { it.role == "assistant" })
        }
    }

    @Test fun restoredDraftDoesNotRestoreAConversationOrExecutableProposal() {
        val application = compose.activity.application
        val handle = androidx.lifecycle.SavedStateHandle(mapOf("assistant-draft" to "Câu hỏi chưa gửi"))
        val restored = PlannerViewModel(application, handle)
        val store = androidx.lifecycle.ViewModelStore()
        store.put("restored", restored)
        try {
            assertEquals("Câu hỏi chưa gửi", restored.assistantQuestion)
            assertTrue(restored.assistantHistory.isEmpty())
            assertNull(restored.assistantProposal)
            assertFalse(restored.assistantBusy)
            restored.assistantQuestion = "Bản nháp mới"
            assertEquals("Bản nháp mới", handle.get<String>("assistant-draft"))
        } finally { store.clear() }
    }

    @Test fun openingChatDoesNotResetTodayScrollPosition() {
        val lastId = vm().menu.last().id
        compose.onNodeWithTag("today-list").performScrollToNode(hasTestTag("open-$lastId"))
        compose.onNodeWithTag("open-$lastId").assertIsDisplayed()
        compose.onNodeWithTag("ask-assistant").performClick()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithTag("open-$lastId").assertIsDisplayed()
    }
}
