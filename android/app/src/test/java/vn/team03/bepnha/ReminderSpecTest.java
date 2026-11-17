package vn.team03.bepnha;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;
import static org.junit.Assert.*;

public class ReminderSpecTest {
    private final long now = 1_798_000_000_000L;
    private ReminderSpec spec(long at) {
        return new ReminderSpec("meal:2026-10-29|dinner", at, "Đến giờ vào bếp", "Cả nhà ăn lúc 18:30", "2026-10-29", "dinner", false);
    }

    @Test public void onlyFutureAlarmsWithinThePlanningWindowAreAccepted() {
        assertFalse(spec(now - 1).isFuture(now));
        assertFalse(spec(now).isFuture(now));
        assertTrue(spec(now + 1).isFuture(now));
        assertTrue(spec(now + ReminderSpec.MAX_FUTURE_MILLIS).isFuture(now));
        assertFalse(spec(now + ReminderSpec.MAX_FUTURE_MILLIS + 1).isFuture(now));
    }

    @Test public void changingTimeInvalidatesThePreviouslyScheduledAlarm() {
        ReminderSpec original = spec(now + 1000);
        assertTrue(original.sameAs(spec(now + 1000)));
        assertFalse(original.sameAs(spec(now + 2000)));
        assertFalse(original.sameAs(null));
    }

    @Test(expected = IllegalArgumentException.class) public void refusesUnknownMealNames() {
        new ReminderSpec("id", now, "Title", "Body", "2026-10-29", "unknown", false);
    }

    @Test(expected = IllegalArgumentException.class) public void refusesInvalidIdentifiers() {
        new ReminderSpec("../../alarm", now, "Title", "Body", "2026-10-29", "lunch", false);
    }

    private ReminderSpec shopping(String id, String date) {
        return new ReminderSpec(id, now + 1000, "Nhắc đi chợ", "Hôm nay cần mua: Cà chua.", date, "lunch", ReminderSpec.KIND_SHOPPING);
    }

    @Test public void shoppingKindIsAcceptedAndKeptApartFromMealReminders() {
        ReminderSpec spec = shopping("shopping:2026-10-29", "2026-10-29");
        assertTrue(spec.isShopping());
        assertFalse(spec.timer);
        ReminderSpec asMeal = new ReminderSpec(spec.id, spec.at, spec.title, spec.body, spec.date, spec.meal, ReminderSpec.KIND_MEAL);
        assertFalse(spec.sameAs(asMeal));
        assertTrue(spec.sameAs(shopping("shopping:2026-10-29", "2026-10-29")));
    }

    @Test public void legacyBooleanConstructorMapsToMealOrTimerKind() {
        assertEquals(ReminderSpec.KIND_MEAL, spec(now + 1).kind);
        ReminderSpec timer = new ReminderSpec("cooking-timer", now + 1, "Hết giờ", "", "2026-10-29", "dinner", true);
        assertEquals(ReminderSpec.KIND_TIMER, timer.kind);
        assertTrue(timer.timer);
    }

    @Test(expected = IllegalArgumentException.class) public void refusesUnknownKinds() {
        new ReminderSpec("id", now, "Title", "Body", "2026-10-29", "lunch", "breakfast");
    }

    @Test(expected = IllegalArgumentException.class) public void shoppingIdMustNameItsOwnDay() {
        shopping("shopping:2026-10-30", "2026-10-29");
    }

    @Test public void storedAlarmsWithoutKindStillRestoreAfterReboot() throws Exception {
        String legacy = "[{\"id\":\"meal:2026-10-29|dinner\",\"at\":" + (now + 1000) + ",\"title\":\"Vào bếp\",\"body\":\"\","
                + "\"date\":\"2026-10-29\",\"meal\":\"dinner\",\"timer\":false},"
                + "{\"id\":\"cooking-timer\",\"at\":" + (now + 2000) + ",\"title\":\"Hết giờ\",\"body\":\"\","
                + "\"date\":\"2026-10-29\",\"meal\":\"dinner\",\"timer\":true}]";
        Map<String, ReminderSpec> specs = ReminderStore.parse(legacy);
        assertEquals(ReminderSpec.KIND_MEAL, specs.get("meal:2026-10-29|dinner").kind);
        assertEquals(ReminderSpec.KIND_TIMER, specs.get("cooking-timer").kind);
        Map<String, ReminderSpec> stored = new LinkedHashMap<>(specs);
        stored.put("shopping:2026-10-29", shopping("shopping:2026-10-29", "2026-10-29"));
        Map<String, ReminderSpec> restored = ReminderStore.parse(ReminderStore.serialize(stored));
        assertEquals(3, restored.size());
        for (ReminderSpec spec : stored.values()) assertTrue(spec.sameAs(restored.get(spec.id)));
    }
}
