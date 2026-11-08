package vn.team03.bepnha

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.time.LocalTime

@RunWith(AndroidJUnit4::class)
class NativeStorageTest {
    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test fun nativeStorageRoundTripsFamilyPlanAndShoppingProgress() {
        val engine = RecipeRepository(context).engine
        val storage = PlannerStorage(context, engine)
        val initial = engine.ensureWeek(PlannerState(date = PlannerEngine.today()), PlannerEngine.today())
        val saved = engine.swap(initial, 0, "tofu").copy(profile = FamilyProfile("Nhà thử nghiệm", 6, dinner = LocalTime.of(19, 0)),
            checked = mapOf(initial.key to setOf("Gạo|g")), favorites = setOf("tofu"))
        storage.save(saved)
        val loaded = storage.load(PlannerEngine.today())
        assertEquals(saved, loaded)
        context.getSharedPreferences("bep_nha_planner_v2", 0).edit().clear().commit()
    }

    @Test fun disablingAReminderRemovesTheAndroidPendingIntent() {
        val key = "meal:${PlannerEngine.today()}|dinner"
        val alarm = NativeAlarm(key, System.currentTimeMillis() + 3_600_000L, "Nhắc thử", "Dữ liệu kiểm thử", PlannerEngine.today(), Meal.DINNER)
        val intent = Intent(context, ReminderReceiver::class.java).setData(Uri.Builder().scheme("bepnha").authority("alarm").appendPath(key).build())
        try {
            ReminderStore.synchronize(context, PlannerStorage.alarmJson(listOf(alarm)))
            assertNotNull(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
            ReminderStore.synchronize(context, "[]")
            assertNull(PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE))
        } finally { ReminderStore.synchronize(context, "[]") }
    }
}
