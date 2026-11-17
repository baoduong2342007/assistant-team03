package vn.team03.bepnha

import android.util.Log
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Natural Vietnamese requests against the real model. Opt in explicitly — every case uses quota:
 *
 *   adb shell am instrument -w -e class vn.team03.bepnha.AgentEvalTest -e firebaseLive true \
 *     -e evalGroup swap vn.team03.bepnha.test/androidx.test.runner.AndroidJUnitRunner
 *
 * `evalGroup` (swap, plan, diet, pantry, app, clarify) or `evalCase` (one id) keeps a run small.
 * Stops at the first quota error. The report is written to the app's files dir as agent-eval.json.
 */
@RunWith(AndroidJUnit4::class)
class AgentEvalTest {
    private class Case(val id: String, val group: String, val question: String, val diet: DietProfile = DietProfile(),
                       val check: (AssistantReply, PlannerState, java.time.LocalDate) -> String?)

    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext.applicationContext
    private val engine by lazy { RecipeRepository(app).engine }

    private fun swaps(r: AssistantReply) = r.actions.filter { it.type == AssistantActionType.swap_recipe }
    private fun only(r: AssistantReply, type: AssistantActionType) = if (r.actions.map { it.type }.toSet() == setOf(type)) null else "expected only $type, got ${r.actions.map { it.type }}"
    private fun noWrites(r: AssistantReply) = if (r.actions.none { it.mutates }) null else "unexpected changes ${r.actions.map { it.type }}"
    private fun recipe(name: String) = engine.recipes.values.first { MealPlanning.normalize(it.name) == MealPlanning.normalize(name) }.id

    private val cases = listOf(
        // Changing one dish, phrased in many ways (the old keyword gate rejected several of these).
        Case("swap-casual", "swap", "Tối mai nấu canh chua cá nhé") { r, _, d -> swaps(r).singleOrNull()?.takeIf { it.recipeId == recipe("Canh chua cá") && it.date == d.plusDays(1) && it.meal == Meal.DINNER }?.let { null } ?: "want sour soup tomorrow dinner, got ${r.actions}" },
        Case("swap-no-accent", "swap", "toi mai doi mon canh thanh canh chua ca") { r, _, _ -> if (swaps(r).singleOrNull()?.recipeId == recipe("Canh chua cá")) null else "got ${r.actions}" },
        Case("swap-want-to-eat", "swap", "Mai trưa cho mình ăn đậu hũ sốt cà chua") { r, _, d -> if (swaps(r).singleOrNull()?.let { it.recipeId == recipe("Đậu hũ sốt cà chua") && it.meal == Meal.LUNCH && it.date == d.plusDays(1) } == true) null else "got ${r.actions}" },
        Case("swap-keep-others", "swap", "Đừng đổi món mặn với rau, chỉ đổi canh tối nay sang món khác") { r, _, _ -> if (swaps(r).singleOrNull()?.index == 1) null else "got ${r.actions}" },
        Case("swap-negated", "swap", "Tối nay đừng đổi gì cả nhé") { r, _, _ -> noWrites(r) },
        Case("swap-unknown-dish", "swap", "Đổi món mặn tối nay thành phở bò") { r, _, _ -> noWrites(r) },
        // Planning several meals.
        Case("plan-week", "plan", "Lên thực đơn cả tuần cho nhà mình") { r, _, _ -> if (swaps(r).map { it.date to it.meal }.distinct().size >= 7) null else "only ${swaps(r).size} swaps" },
        Case("plan-dinners", "plan", "Đổi hết các bữa tối tuần này, nấu nhanh dưới 30 phút") { r, _, _ ->
            if (swaps(r).isNotEmpty() && swaps(r).all { it.meal == Meal.DINNER }) null else "got ${r.actions}" },
        Case("plan-avoid", "plan", "Lên thực đơn 3 ngày tới, nhà không ăn được tôm") { r, s, d ->
            var after = s; r.actions.forEach { after = AssistantCommands(engine).apply(after, it, d, 0) }
            if (swaps(r).isNotEmpty() && (0L..2L).all { o -> Meal.entries.all { m -> engine.menu(after, d.plusDays(o), m).none { "shrimp" in it.allergens } } }) null else "shrimp kept or no plan" },
        // Lasting needs go to the diet profile.
        Case("diet-diabetes", "diet", "Bà nhà mình bị tiểu đường") { r, _, _ -> if (r.actions.any { it.type == AssistantActionType.set_diet_tag && it.key == "low_sugar" && it.enabled == true }) null else "got ${r.actions}" },
        Case("diet-allergy", "diet", "Cháu nó dị ứng tôm") { r, _, _ -> if (r.actions.any { it.type == AssistantActionType.set_avoid && it.enabled == true }) null else "got ${r.actions}" },
        Case("diet-respected", "diet", "Tối mai đổi món mặn khác", diet = DietProfile(allergens = setOf("shrimp", "fish"))) { r, _, _ ->
            val picked = swaps(r).mapNotNull { engine.recipes[it.recipeId] }
            if (picked.isNotEmpty() && picked.none { it.allergens.any { a -> a in setOf("shrimp", "fish") } }) null else "got ${r.actions}" },
        // Fridge.
        Case("pantry", "pantry", "Tủ lạnh có thịt heo, trứng và rau muống. Tối mai nấu gì?") { r, _, _ -> if (r.suggestions.isNotEmpty() && r.actions.isEmpty()) null else "no suggestions" },
        Case("pantry-only", "pantry", "Chỉ còn rau muống thôi, không đi chợ nữa, trưa mai nấu gì?") { r, _, _ -> noWrites(r) },
        // Other parts of the app.
        Case("app-shopping", "app", "Mở danh sách đi chợ") { r, _, _ -> if (r.actions.any { it.type == AssistantActionType.navigate && it.screen == Screen.SHOPPING }) null else "got ${r.actions}" },
        Case("app-share", "app", "Gửi danh sách đi chợ cả ngày mai cho con mình") { r, _, _ -> only(r, AssistantActionType.share_shopping_list) },
        Case("app-people", "app", "Cuối tuần nhà có khách, nấu cho 6 người") { r, _, _ -> if (r.actions.any { it.type == AssistantActionType.set_people && it.people == 6 }) null else "got ${r.actions}" },
        Case("app-timer", "app", "Hẹn giờ 10 phút luộc rau giúp mình") { r, _, _ -> if (r.actions.any { it.type == AssistantActionType.start_timer && it.minutes == 10 }) null else "got ${r.actions}" },
        Case("app-favorite", "app", "Lưu món canh chua cá vào món yêu thích") { r, _, _ -> if (r.actions.any { it.type == AssistantActionType.set_favorite && it.enabled == true }) null else "got ${r.actions}" },
        Case("app-cook-along", "app", "Đọc cho mình từng bước nấu bữa tối nay") { r, _, _ -> if (r.actions.any { it.type == AssistantActionType.start_cook_along }) null else "got ${r.actions}" },
        Case("app-family-name", "app", "Đổi tên gia đình thành Nhà bà Tư") { r, _, _ -> only(r, AssistantActionType.set_family_name) },
        // Asking back instead of guessing.
        Case("clarify-meal", "clarify", "Ngày mai mình muốn ăn canh chua cá") { r, _, _ -> if (r.actions.isEmpty() || swaps(r).size == 1) null else "got ${r.actions}" },
        Case("clarify-breakfast", "clarify", "Sáng mai ăn gì?") { r, _, _ -> noWrites(r) },
    )

    @Test fun naturalRequestsReachTheRightTools() = runBlocking {
        val args = InstrumentationRegistry.getArguments()
        assumeTrue(args.getString("firebaseLive") == "true")
        assertTrue("Supply team google-services.json before live evaluation", BuildConfig.FIREBASE_CONFIGURED)
        val selected = cases.filter { case ->
            (args.getString("evalCase") ?: case.id) == case.id && (args.getString("evalGroup") ?: case.group) == case.group
        }
        val today = PlannerEngine.today()
        val report = JSONArray()
        var failures = 0
        for (case in selected) {
            val state = engine.ensureWeek(PlannerState(date = today, diet = case.diet), today)
            val context = AssistantGateway.snapshot(engine, state, today, Screen.TODAY, null)
            val started = System.currentTimeMillis()
            val reply = AssistantService(engine, FirebaseAssistantProvider(app)).ask(case.question, emptyList(), context)
            val entry = JSONObject().put("id", case.id).put("question", case.question).put("ms", System.currentTimeMillis() - started)
                .put("reply", reply.text).put("actions", JSONArray(reply.actions.map { it.type.name }))
            if (reply.source != AssistantSource.AI) {
                // Quota, network or timeout: the local assistant answered, so this is not an AI result.
                report.put(entry.put("result", "not-ai").put("notice", reply.notice))
                if (reply.notice == AssistantFailureKind.QUOTA.explanation) break else continue
            }
            val problem = runCatching { case.check(reply, state, today) }.getOrElse { it.message ?: it.toString() }
            if (problem != null) failures++
            report.put(entry.put("result", if (problem == null) "pass" else "fail").put("problem", problem ?: JSONObject.NULL))
            Log.i("BepNhaEval", entry.toString())
        }
        File(app.filesDir, "agent-eval.json").writeText(report.toString(2))
        Log.i("BepNhaEval", "Report: ${report.length()} cases, $failures failed")
        assertTrue("$failures case(s) failed; see agent-eval.json", failures == 0)
    }
}
