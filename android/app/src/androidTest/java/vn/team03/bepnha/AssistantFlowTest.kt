package vn.team03.bepnha

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

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
        }
    }
}
