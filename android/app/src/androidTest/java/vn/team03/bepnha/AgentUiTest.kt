package vn.team03.bepnha

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExternalResource
import org.junit.rules.RuleChain
import org.junit.rules.TestRule
import org.junit.runner.RunWith

/** Device checks for the agent features: cook-along, diet profile, greeting, undo and clearing chat. */
@RunWith(AndroidJUnit4::class)
class AgentUiTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val reset = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("bep_nha_planner_v2", 0).edit().clear().commit()
            context.getSharedPreferences("bep_nha_chat", 0).edit().clear().commit()
            ReminderStore.synchronize(context, "[]")
        }
    }
    @get:Rule val chain: TestRule = RuleChain.outerRule(reset).around(compose)
    private fun vm() = ViewModelProvider(compose.activity)[PlannerViewModel::class.java]

    @Test fun cookAlongStepsForwardAndBackAndMarksTasksDone() {
        compose.onNodeWithTag("tab-cook").performClick()
        compose.onNodeWithTag("start-cook-along").performScrollTo().performClick()
        compose.onNodeWithTag("cook-along").assertIsDisplayed()
        compose.onNodeWithTag("cook-step-count").assertTextContains("Bước 1 /", substring = true)
        val firstTask = compose.runOnIdle { vm().cookAlong!!.step.taskKey }
        val stepsInFirstTask = compose.runOnIdle { vm().cookAlong!!.steps.count { it.taskKey == firstTask } }
        repeat(stepsInFirstTask) { compose.onNodeWithTag("cook-next").performClick() }
        compose.runOnIdle {
            assertTrue(firstTask in vm().state.done[vm().state.key].orEmpty())
            assertEquals(stepsInFirstTask, vm().cookAlong!!.index)
        }
        compose.onNodeWithTag("cook-back").performClick()
        compose.runOnIdle { assertEquals(stepsInFirstTask - 1, vm().cookAlong!!.index) }
        compose.onNodeWithTag("cook-exit").performClick()
        compose.runOnIdle { assertNull(vm().cookAlong) }
        compose.onNodeWithTag("cook-list").assertIsDisplayed()
    }

    @Test fun dietProfileIsSavedFromTheFamilyScreen() {
        compose.runOnIdle { vm().navigate(Screen.FAMILY) }
        compose.onNodeWithTag("family-list").performScrollToNode(hasTestTag("diet-tag-low_salt"))
        compose.onNodeWithTag("diet-tag-low_salt").performClick()
        compose.onNodeWithTag("family-list").performScrollToNode(hasTestTag("diet-allergen-shrimp"))
        compose.onNodeWithTag("diet-allergen-shrimp").performClick()
        compose.onNodeWithTag("family-list").performScrollToNode(hasTestTag("diet-save"))
        compose.onNodeWithTag("diet-save").performClick()
        compose.runOnIdle {
            assertEquals(setOf("low_salt"), vm().state.diet.tags)
            assertEquals(setOf("shrimp"), vm().state.diet.allergens)
        }
    }

    @Test fun openingTheAssistantGreetsAndShowsTheMicrophone() {
        compose.onNodeWithTag("ask-assistant").performClick()
        compose.onNodeWithTag("assistant-mic").assertIsDisplayed()
        compose.runOnIdle {
            val greeting = vm().assistantHistory.single { it.id.startsWith("greeting-") }
            assertTrue(greeting.text, greeting.text.startsWith("Chào"))
            vm().closeAssistant(); vm().openAssistant()
            assertEquals("greets once per meal", 1, vm().assistantHistory.count { it.id.startsWith("greeting-") })
        }
    }

    @Test fun lowRiskChangeRunsAtOnceAndCanBeUndone() {
        val recipe = compose.runOnIdle { vm().menu.first().id }
        compose.runOnIdle {
            vm().assistantTransport = { _, _, _ ->
                AssistantReply("Mình lưu món này nhé.", listOf(AssistantAction(AssistantActionType.set_favorite, vm().state.date, vm().state.meal, recipeId = recipe, enabled = true)))
            }
            vm().openAssistant()
            vm().askAssistant("Lưu món này vào yêu thích")
        }
        compose.waitUntil(5000) { !vm().assistantBusy && recipe in vm().state.favorites }
        compose.runOnIdle { assertNull("low-risk needs no confirmation", vm().assistantProposal) }
        compose.onNodeWithTag("assistant-undo").performClick()
        compose.runOnIdle { assertFalse(recipe in vm().state.favorites) }
    }

    @Test fun clearingHistoryAsksFirstAndKeepsMealData() {
        compose.runOnIdle {
            vm().assistantTransport = { _, _, _ -> AssistantReply("Món nấu nhanh có trứng chiên.", emptyList()) }
            vm().openAssistant(); vm().askAssistant("Món nấu nhanh")
        }
        compose.waitUntil(5000) { !vm().assistantBusy }
        val before = compose.runOnIdle { vm().state }
        compose.onNodeWithTag("assistant-options").performClick()
        compose.onNodeWithTag("assistant-clear").performClick()
        compose.onNodeWithTag("assistant-clear-confirm").performClick()
        compose.runOnIdle {
            assertTrue(vm().assistantHistory.isEmpty())
            assertEquals(before, vm().state)
        }
    }
}
