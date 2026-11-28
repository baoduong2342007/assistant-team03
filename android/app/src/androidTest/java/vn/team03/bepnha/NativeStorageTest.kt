package vn.team03.bepnha

import android.app.PendingIntent
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONObject
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
            checked = mapOf(PlannerEngine.shopKey(initial.date) to setOf("Gạo|g")), favorites = setOf("tofu"),
            timers = listOf(CookingTimer(3, System.currentTimeMillis() + 600_000L, "Nấu canh", initial.date, Meal.DINNER)),
            shopMeals = setOf(Meal.LUNCH, Meal.DINNER))
        storage.save(saved)
        val loaded = storage.load(PlannerEngine.today())
        assertEquals(saved, loaded)
        context.getSharedPreferences("bep_nha_planner_v2", 0).edit().clear().commit()
    }

    @Test fun loadingTheLegacyTimerAlsoAddsBreakfastWithoutChangingDinner() {
        val engine = RecipeRepository(context).engine
        val today = PlannerEngine.today()
        val dinner = PlannerEngine.key(today, Meal.DINNER)
        val preferences = context.getSharedPreferences("bep_nha_planner_v2", 0)
        val now = System.currentTimeMillis()
        try {
            val legacy = JSONObject()
                .put("date", today.toString())
                .put("meal", "dinner")
                .put("plans", JSONObject().put(dinner, org.json.JSONArray(listOf("tofu", "soup", "greens"))))
                .put("timer", JSONObject().put("end", now + 300_000).put("label", "Canh")
                    .put("date", today.toString()).put("meal", "dinner"))
            assertTrue(preferences.edit().putString("state", legacy.toString()).commit())
            val state = PlannerStorage(context, engine).load(today)
            assertEquals("tofu", engine.menu(state, today, Meal.DINNER).first().id)
            assertEquals(RecipeKind.BREAKFAST, engine.menu(state, today, Meal.BREAKFAST).single().kind)
            assertEquals("Canh", state.timers.single().label)
            assertEquals(Meal.DINNER, state.timers.single().meal)
        } finally {
            preferences.edit().clear().commit()
        }
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

    @Test fun geminiVoiceIsSavedAndOlderDataGetsTheDefault() {
        val engine = RecipeRepository(context).engine
        val storage = PlannerStorage(context, engine)
        val preferences = context.getSharedPreferences("bep_nha_planner_v2", 0)
        try {
            val initial = engine.ensureWeek(PlannerState(date = PlannerEngine.today()), PlannerEngine.today())
            val saved = initial.copy(voice = VoiceSettings("bác", readAloud = false, rate = 0.7f, liveVoice = "Puck"))
            storage.save(saved)
            assertEquals(saved.voice, storage.load(PlannerEngine.today()).voice)

            // Data saved before the Gemini voice existed, and an unknown voice name, fall back to the default.
            val state = JSONObject(preferences.getString("state", null)!!)
            state.getJSONObject("voice").remove("live_voice")
            assertTrue(preferences.edit().putString("state", state.toString()).commit())
            assertEquals(VoiceSettings("bác", false, 0.7f), storage.load(PlannerEngine.today()).voice)
            state.getJSONObject("voice").put("live_voice", "Nobody")
            assertTrue(preferences.edit().putString("state", state.toString()).commit())
            assertEquals(VoiceSettings.DEFAULT_LIVE_VOICE, storage.load(PlannerEngine.today()).voice.liveVoice)
        } finally {
            preferences.edit().clear().commit()
        }
    }
}
