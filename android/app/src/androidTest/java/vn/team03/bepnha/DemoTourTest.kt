package vn.team03.bepnha

import android.graphics.Bitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Scripted demo tour for screen recording, one scene per test, driven like a real user and
 * answered by the real AI. Opt in with `-e demo true`; add `-e demoReset true` to start from fresh
 * app data (the App Check token is kept). Paced for the free tier (15 requests per minute). Not part of the normal suite.
 */
@RunWith(AndroidJUnit4::class)
class DemoTourTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var scenario: ActivityScenario<MainActivity>
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    private lateinit var vm: PlannerViewModel
    private var shot = 0
    private var scene = ""

    @Before fun launch() {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("demo") == "true")
        if (args.getString("demoReset") == "true") {
            // Fresh start for the recording; App Check's own preferences stay untouched.
            context.getSharedPreferences("bep_nha_planner_v2", 0).edit().clear().commit()
            context.getSharedPreferences("bep_nha_chat", 0).edit().clear().commit()
            ReminderStore.synchronize(context, "[]")
        }
        scenario = ActivityScenario.launch(MainActivity::class.java)
        scenario.onActivity { vm = ViewModelProvider(it)[PlannerViewModel::class.java] }
        compose.waitForIdle()
    }

    @After fun close() { if (::scenario.isInitialized) scenario.close() }

    private fun pause(ms: Long) { compose.waitForIdle(); Thread.sleep(ms) }
    private fun tap(tag: String) { compose.onNodeWithTag(tag).performScrollTo().performClick(); pause(1200) }
    private fun tapInPlace(tag: String) { compose.onNodeWithTag(tag).performClick(); pause(1200) }
    private fun swipe(tag: String, times: Int = 2) = repeat(times) {
        compose.onNodeWithTag(tag).performTouchInput { swipeUp(durationMillis = 900) }; pause(1500)
    }
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    /** Screenshot for checking the recording without watching it. */
    private fun snap(label: String) {
        val bitmap = InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot() ?: return
        val dir = File(context.filesDir, "demo").apply { mkdirs() }
        File(dir, "$scene-${shot++}-$label.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 80, it) }
    }

    private fun ask(question: String) {
        val input = compose.onNodeWithTag("assistant-input")
        input.performClick()
        question.chunked(2).forEach { input.performTextInput(it); Thread.sleep(70) }
        pause(800)
        tapInPlace("assistant-send")
        compose.waitUntil(90_000) { !vm.assistantBusy }
        pause(5000)
        snap(question.take(20).replace(Regex("""[^\p{L}0-9]+"""), "_"))
    }

    private fun confirm() {
        if (vm.assistantProposal == null) return
        compose.onNodeWithTag("assistant-history").performTouchInput { swipeUp(durationMillis = 1200) }
        pause(2500)
        tap("assistant-confirm")
        pause(3500)
        snap("confirmed")
    }

    /** Free tier: up to 4 model calls per question, 15 calls per minute. */
    private fun breathe() = pause(14_000)

    @Test fun scene1_tour() {
        scene = "1"
        pause(3000); snap("today")
        swipe("today-list")
        tapInPlace("tab-plan"); pause(2000); swipe("week-list"); snap("week")
        tapInPlace("tab-shopping"); pause(2000); swipe("shopping-list"); snap("shopping")
        tapInPlace("tab-cook"); pause(2000); swipe("cook-list"); snap("cook")
        main { vm.navigate(Screen.FAMILY) }; pause(2500); swipe("family-list", 4); snap("family")
        tapInPlace("tab-today"); pause(2000)
    }

    @Test fun scene2_greetAndSwap() {
        scene = "2"
        tapInPlace("ask-assistant"); pause(7000); snap("greeting")
        ask("Tối mai nấu canh chua cá nhé"); confirm()
        breathe()
        ask("Đừng đổi món mặn với rau, chỉ đổi canh tối nay sang canh bí đỏ"); confirm()
        main { vm.navigate(Screen.PLAN) }; pause(2500); swipe("week-list", 1); snap("week-after")
    }

    @Test fun scene3_dietAndWeekPlan() {
        scene = "3"
        tapInPlace("ask-assistant"); pause(2500)
        ask("Bà nhà mình bị tiểu đường, nhà cũng không ăn được tôm"); confirm()
        breathe()
        ask("Lên thực đơn 3 ngày tới cho nhà mình nhé")
        compose.onNodeWithTag("assistant-history").performTouchInput { swipeUp(durationMillis = 1500) }; pause(2500)
        compose.onNodeWithTag("assistant-history").performTouchInput { swipeUp(durationMillis = 1500) }; pause(2500)
        snap("preview")
        confirm()
        main { vm.navigate(Screen.PLAN) }; pause(2500); swipe("week-list", 2); snap("week-after")
    }

    @Test fun scene4_quickActionsAndUndo() {
        scene = "4"
        tapInPlace("ask-assistant"); pause(2500)
        ask("Lưu món canh chua cá vào món yêu thích")
        pause(1500); tapInPlace("assistant-undo"); pause(2500); snap("undone")
        breathe()
        ask("Hẹn giờ 10 phút luộc rau giúp mình"); confirm()
        breathe()
        ask("Cuối tuần nhà có khách, nấu cho 6 người"); confirm()
        main { vm.closeAssistant() }; pause(3000); snap("timer")
    }

    @Test fun scene5_smartAnswers() {
        scene = "5"
        tapInPlace("ask-assistant"); pause(2500)
        ask("Đổi món mặn tối nay thành phở bò")
        breathe()
        ask("Tủ lạnh có thịt heo, trứng và rau muống. Tối mai nấu gì?")
        compose.onNodeWithTag("assistant-history").performTouchInput { swipeUp(durationMillis = 1500) }; pause(3000)
        breathe()
        ask("Sáng mai ăn gì?")
    }

    @Test fun scene6_appControlAndCookAlong() {
        scene = "6"
        tapInPlace("ask-assistant"); pause(2500)
        ask("Mở danh sách đi chợ")
        pause(3000); snap("shopping")
        breathe()
        tapInPlace("ask-assistant"); pause(2000)
        ask("Đọc cho mình từng bước nấu bữa tối nay")
        compose.waitUntil(10_000) { vm.cookAlong != null }
        pause(6000); snap("cook-along")
        repeat(3) { tapInPlace("cook-next"); pause(5000) }
        snap("cook-step")
        tapInPlace("cook-exit"); pause(2500)
    }
}
