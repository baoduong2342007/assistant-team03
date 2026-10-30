package vn.team03.bepnha

import android.view.View
import android.view.ViewGroup
import android.graphics.Bitmap
import java.io.File
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
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
            ReminderStore.synchronize(context, "[]")
        }
    }
    @get:Rule val chain: TestRule = RuleChain.outerRule(reset).around(compose)

    @Test fun screenUsesActualComposeViews() {
        compose.onNodeWithTag("native-root").assertExists()
        compose.onNodeWithText("Hôm nay nhà mình ăn gì?").assertIsDisplayed()
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

    @Test fun changingTheMainDishUpdatesTheNativeMealCard() {
        compose.onNodeWithTag("swap-pork").performScrollTo().performClick()
        compose.onNodeWithTag("choose-tofu").performScrollTo().performClick()
        compose.onNodeWithTag("recipe-tofu").assertExists()
        compose.onNodeWithTag("recipe-pork").assertDoesNotExist()
    }

    @Test fun opensTextRecipeAndShoppingAsNativeScreens() {
        compose.onNodeWithTag("open-pork").performScrollTo().performClick()
        compose.onNodeWithText("Cách nấu từng bước").performScrollTo().assertIsDisplayed()
        capture("native-recipe.png")
        compose.onNodeWithContentDescription("Đóng").performClick()
        compose.onNodeWithTag("tab-shopping").performClick()
        compose.onNodeWithTag("shopping-list").assertExists()
        compose.onNodeWithText("Đi chợ đủ bữa ngon").assertIsDisplayed()
        capture("native-shopping.png")
    }

    @Test fun sixPersonProfileUpdatesShoppingAndOpensCookingTimeline() {
        compose.onNodeWithContentDescription("Thông tin gia đình").performClick()
        compose.onNodeWithText("+").performClick().performClick()
        compose.onNodeWithText("Lưu thông tin gia đình").performScrollTo().performClick()
        compose.onNodeWithTag("family-people").assertTextEquals("6 người")
        compose.onNodeWithTag("tab-shopping").performClick()
        compose.onNodeWithTag("ingredient-Thịt ba chỉ").performScrollTo().assertTextContains("600 g")
        compose.onNodeWithTag("tab-cook").performClick()
        compose.onNodeWithText("Dọn cơm lúc 18:30").assertIsDisplayed()
        capture("native-cook.png")
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
