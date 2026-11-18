package vn.team03.bepnha

import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.time.LocalTime

/** Run seed with the baseline APK, install the new APK with -r, then run verify. */
class UpgradeCompatibilityTest {
    private val app get() = InstrumentationRegistry.getInstrumentation().targetContext
    private fun fixture(engine: PlannerEngine): PlannerState {
        val today = PlannerEngine.today()
        val initial = engine.ensureWeek(PlannerState(date = today), today)
        return engine.swap(initial, 0, "tofu").copy(
            profile = FamilyProfile("Kiểm tra nâng cấp", 6, LocalTime.of(11, 45), LocalTime.of(19, 0)),
            checked = mapOf(initial.key to setOf("Gạo|g")),
            favorites = setOf("tofu"), reminders = setOf(initial.key),
        )
    }

    @Test fun seedUpgradeFixture() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("upgradeStage") == "seed")
        val engine = RecipeRepository(app).engine
        val storage = PlannerStorage(app, engine)
        storage.save(fixture(engine))
        assertEquals(fixture(engine), storage.load(PlannerEngine.today()))
    }

    @Test fun verifyUpgradeFixture() {
        assumeTrue(InstrumentationRegistry.getArguments().getString("upgradeStage") == "verify")
        val engine = RecipeRepository(app).engine
        assertEquals(fixture(engine), PlannerStorage(app, engine).load(PlannerEngine.today()))
    }
}
