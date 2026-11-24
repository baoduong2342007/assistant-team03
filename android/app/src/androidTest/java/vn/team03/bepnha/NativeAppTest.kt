package vn.team03.bepnha

import android.content.Intent
import android.view.View
import android.view.ViewGroup
import android.graphics.Bitmap
import java.io.File
import androidx.activity.BackEventCompat
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

@RunWith(AndroidJUnit4::class)
class NativeAppTest {
    private val compose = createAndroidComposeRule<MainActivity>()
    private val reset = object : ExternalResource() {
        override fun before() {
            val context = InstrumentationRegistry.getInstrumentation().targetContext
            context.getSharedPreferences("bep_nha_planner_v2", 0).edit().clear().commit()
            context.getSharedPreferences(PlannerStorage.WELCOME_PREFS, 0).edit().putBoolean("seen", true).commit()
            ReminderStore.synchronize(context, "[]")
        }
    }
    @get:Rule val chain: TestRule = RuleChain.outerRule(reset).around(compose)

    @Test fun screenUsesActualComposeViews() {
        compose.onNodeWithTag("native-root").assertExists()
        compose.onNodeWithTag("home-greeting").assertIsDisplayed()
        compose.activityRule.scenario.onActivity { activity ->
            val views = mutableListOf<View>()
            fun collect(view: View) {
                views += view
                if (view is ViewGroup) for (i in 0 until view.childCount) collect(view.getChildAt(i))
            }
            collect(activity.window.decorView)
            assertTrue(views.any { it.javaClass.name == "androidx.compose.ui.platform.AndroidComposeView" })
            assertFalse(views.any { it.javaClass.name.contains("WebView") })
        }
        capture("native-home.png")
    }

    @Test fun todaySearchFindsAccentFreeNamesAndOpensTheRecipe() {
        compose.onNodeWithTag("today-search").performTextInput("dau hu sot ca chua")
        compose.onNodeWithTag("today-list").performScrollToNode(hasTestTag("open-tofu"))
        compose.onNodeWithTag("open-tofu").performClick()
        compose.onNodeWithText("Cách nấu từng bước").performScrollTo().assertIsDisplayed()
    }

    @Test fun todayCategoryAndEmptySearchCanReturnToTheMeal() {
        onToday("today-category-SOUP").performScrollTo().performClick().assertIsSelected()
        compose.onNodeWithTag("today-search").performTextInput("zzzzkhongcomon")
        compose.onNodeWithText("0 công thức").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Về thực đơn hôm nay").performClick()
        onToday("open-pork").performClick()
        compose.onNodeWithText("Cách nấu từng bước").performScrollTo().assertIsDisplayed()
    }

    @Test fun changingTheMainDishUpdatesTheNativeMealCardAndPhoto() {
        onToday("meal-photo-family").assertIsDisplayed()
        onToday("swap-pork").performClick()
        compose.onNodeWithTag("swap-list").performScrollToNode(hasTestTag("choose-tofu"))
        compose.onNodeWithTag("choose-tofu").performClick()
        compose.onNodeWithTag("recipe-tofu").assertExists()
        compose.onNodeWithTag("recipe-pork").assertDoesNotExist()
        onToday("meal-photo-tofu").assertContentDescriptionEquals("Ảnh minh họa Đậu hũ sốt cà chua")
        compose.onNodeWithTag("meal-photo-family").assertDoesNotExist()
        capture("native-demo-swapped.png")
    }

    @Test fun everyBundledRecipeHasAnIllustration() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val missing = RecipeRepository(context).engine.recipes.keys.filterNot(::hasRecipePhoto)
        assertTrue("Missing recipe photos: $missing", missing.isEmpty())
    }

    @Test fun breakfastShowsItsOwnPhoto() {
        onToday("today-meal-picker").performClick()
        compose.onNodeWithTag("meal-breakfast").performClick()
        onToday("meal-photo-bread").assertContentDescriptionEquals("Ảnh minh họa Bánh mì ốp la")
        compose.onNodeWithTag("meal-photo-family").assertDoesNotExist()
        capture("native-demo-breakfast.png")
    }

    @Test fun opensTextRecipeAndShoppingAsNativeScreens() {
        onToday("open-pork").performClick()
        compose.onNodeWithText("Cách nấu từng bước").performScrollTo().assertIsDisplayed()
        capture("native-recipe.png")
        compose.onNodeWithContentDescription("Đóng").performClick()
        compose.onNodeWithTag("tab-shopping").performClick()
        compose.onNodeWithTag("shopping-list").assertExists()
        compose.onNodeWithText("Danh sách đi chợ").assertIsDisplayed()
        capture("native-shopping.png")
    }

    @Test fun sixPersonProfileUpdatesShoppingAndOpensCookingTimeline() {
        compose.onNodeWithTag("more-menu").performClick()
        compose.onNodeWithContentDescription("Thông tin gia đình").performClick()
        compose.onNodeWithText("+").performClick().performClick()
        compose.onNodeWithText("Lưu thông tin gia đình").performScrollTo().performClick()
        compose.onNodeWithTag("family-people").assertTextEquals("6 người")
        compose.onNodeWithTag("tab-shopping").performClick()
        compose.onNodeWithTag("shopping-list").performScrollToNode(hasTestTag("ingredient-Thịt ba chỉ"))
        compose.onNodeWithTag("ingredient-Thịt ba chỉ").assertTextContains("600 g")
        compose.onNodeWithTag("tab-cook").performClick()
        compose.onNodeWithText("Dọn cơm lúc 18:30").assertIsDisplayed()
        capture("native-cook.png")
    }

    @Test fun switchingTabsRestoresTheHomeScrollPosition() {
        onToday("swap-pork").assertIsDisplayed()
        compose.onNodeWithTag("tab-shopping").performClick()
        compose.onNodeWithTag("shopping-list").assertExists()
        compose.onNodeWithTag("tab-today").performClick()
        compose.onNodeWithTag("swap-pork").assertIsDisplayed()
    }

    @Test fun rapidTabChangesRetargetAndRestoreHomeScroll() {
        val vm = ViewModelProvider(compose.activity)[PlannerViewModel::class.java]
        val timerId = compose.runOnIdle {
            vm.startTimer(5, "Nấu cơm")
            vm.state.timers.single().id
        }
        onToday("swap-pork").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        try {
            for (tab in listOf("shopping", "cook", "plan", "today")) {
                compose.onNodeWithTag("tab-$tab").performClick()
                compose.mainClock.advanceTimeBy(48)
            }
            compose.mainClock.advanceTimeBy(500)
            compose.onNodeWithTag("tab-today").assertIsSelected()
            compose.onNodeWithTag("shopping-list").assertDoesNotExist()
            compose.onNodeWithTag("swap-pork").assertIsDisplayed()
            compose.runOnIdle { assertEquals(timerId, vm.state.timers.single().id) }
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test fun predictiveBackCancellationKeepsChatAndCommitRestoresRecipe() {
        val vm = ViewModelProvider(compose.activity)[PlannerViewModel::class.java]
        compose.runOnIdle {
            vm.openRecipe("pork")
            vm.openAssistant()
            vm.assistantQuestion = "Giữ bản nháp"
        }
        val chat = compose.onNodeWithTag("assistant-screen").assertIsDisplayed()
        val startingLeft = chat.getUnclippedBoundsInRoot().left

        fun startBack() {
            compose.runOnIdle {
                val dispatcher = compose.activity.onBackPressedDispatcher
                dispatcher.dispatchOnBackStarted(BackEventCompat(0f, 200f, 0f, BackEventCompat.EDGE_LEFT))
                dispatcher.dispatchOnBackProgressed(BackEventCompat(150f, 200f, 0.6f, BackEventCompat.EDGE_LEFT))
            }
            chat.assertIsDisplayed()
            assertTrue(chat.getUnclippedBoundsInRoot().left > startingLeft)
        }

        startBack()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.dispatchOnBackCancelled() }
        compose.onNodeWithTag("assistant-input").assertTextContains("Giữ bản nháp")
        assertEquals(startingLeft, chat.getUnclippedBoundsInRoot().left)
        compose.runOnIdle { assertNull(vm.sheet) }

        startBack()
        compose.runOnIdle { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.onNodeWithText("Công thức").assertIsDisplayed()
        compose.runOnIdle {
            assertEquals(PlannerSheet.RecipeDetail("pork"), vm.sheet)
            assertEquals("Giữ bản nháp", vm.assistantQuestion)
        }
    }

    @Test fun notificationSupersedesClosingSwapSheet() {
        val vm = ViewModelProvider(compose.activity)[PlannerViewModel::class.java]
        onToday("swap-pork").performClick()
        compose.onNodeWithTag("swap-list").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        try {
            compose.onNodeWithContentDescription("Đóng").performClick()
            compose.mainClock.advanceTimeBy(48)
            compose.runOnIdle {
                vm.openNotification(Intent()
                    .putExtra("date", PlannerEngine.today().toString())
                    .putExtra("meal", Meal.BREAKFAST.value))
            }
            compose.mainClock.autoAdvance = true
            compose.waitUntil(5_000) {
                compose.onAllNodesWithTag("cook-list").fetchSemanticsNodes().isNotEmpty()
            }
            compose.onNodeWithTag("swap-list").assertDoesNotExist()
            compose.runOnIdle {
                assertEquals(Meal.BREAKFAST, vm.state.meal)
                assertNull(vm.sheet)
                vm.openReminders()
            }
            compose.onNodeWithTag("meal-time").assertIsDisplayed()
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test fun ingredientRowIsOneAccessibleCheckboxAndKeepsItsState() {
        compose.onNodeWithTag("tab-shopping").performClick()
        compose.onNodeWithTag("shopping-list").performScrollToNode(hasTestTag("ingredient-Thịt ba chỉ"))
        compose.onNodeWithTag("ingredient-Thịt ba chỉ").assertIsOff().performClick().assertIsOn()
        compose.onNodeWithTag("tab-cook").performClick()
        compose.onNodeWithTag("tab-shopping").performClick()
        compose.onNodeWithTag("ingredient-Thịt ba chỉ").performScrollTo().assertIsOn().performClick().assertIsOff()
    }

    @Test fun rapidAskPanelSwitchesKeepFridgeScrollAndExposeOnlyTheSelectedPanel() {
        compose.onNodeWithTag("more-menu").performClick()
        compose.onNodeWithTag("tab-ask").performClick()
        compose.onNodeWithTag("fridge-Trứng").performClick()
        compose.onNodeWithTag("fridge-list").performScrollToNode(hasTestTag("tray-0"))
        compose.onNodeWithTag("tray-0").assertIsDisplayed()
        compose.mainClock.autoAdvance = false
        try {
            for (panel in listOf("ask-timer", "ask-fridge", "ask-timer", "ask-fridge")) {
                compose.onNodeWithTag(panel).performClick()
                compose.mainClock.advanceTimeBy(48)
            }
            compose.mainClock.advanceTimeBy(250)
            compose.onNodeWithTag("ask-fridge").assertIsSelected()
            compose.onNodeWithTag("timer-list").assertDoesNotExist()
            compose.onNodeWithTag("tray-0").assertIsDisplayed()
        } finally {
            compose.mainClock.autoAdvance = true
        }
    }

    @Test fun fridgeSuggestsAMealAndChatChangesTomorrowsDinner() {
        compose.onNodeWithTag("more-menu").performClick()
        compose.onNodeWithTag("tab-ask").performClick()
        compose.onNodeWithTag("fridge-Trứng").performClick()
        compose.onNodeWithTag("fridge-list").performScrollToNode(hasTestTag("tray-0"))
        compose.onNodeWithTag("tray-0").assertIsDisplayed()
        capture("native-fridge.png")
        compose.onNodeWithTag("ask-chat").performClick()
        compose.onNodeWithTag("assistant-input").performTextInput("Bua toi ngay mai toi muon an dau hu sot ca chua")
        compose.onNodeWithTag("assistant-send").performClick()
        compose.waitUntil(5_000) {
            compose.onAllNodes(hasTestTag("assistant-confirm")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithTag("assistant-confirm").performScrollTo().performClick()
        compose.onNodeWithContentDescription("Quay lại").performClick()
        compose.onNodeWithTag("tab-today").performClick()
        onToday("date-${PlannerEngine.today().plusDays(1)}").performClick()
        onToday("recipe-tofu").assertIsDisplayed()
    }

    @Test fun shoppingCanCoverLunchAndDinnerTogether() {
        compose.onNodeWithTag("tab-shopping").performClick()
        val list = compose.onNodeWithTag("shopping-list")
        list.performScrollToNode(hasTestTag("ingredient-Gạo"))
        compose.onNodeWithTag("ingredient-Gạo").assertTextContains("360 g", substring = true)
        list.performScrollToNode(hasTestTag("shop-lunch"))
        compose.onNodeWithTag("shop-lunch").performClick()
        list.performScrollToNode(hasTestTag("ingredient-Gạo"))
        compose.onNodeWithTag("ingredient-Gạo").assertTextContains("720 g", substring = true)
    }

    /** The Today list is lazy: cards below the week strip and fridge card are composed only once scrolled to. */
    private fun onToday(tag: String): SemanticsNodeInteraction {
        compose.onNodeWithTag("today-list").performScrollToNode(hasTestTag(tag))
        return compose.onNodeWithTag(tag)
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val directory = File(instrumentation.targetContext.getExternalFilesDir(null), "native-previews").apply { mkdirs() }
        val image = instrumentation.uiAutomation.takeScreenshot()
        checkNotNull(image) { "Android could not capture the rendered screen" }
        File(directory, name).outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
    }
}
