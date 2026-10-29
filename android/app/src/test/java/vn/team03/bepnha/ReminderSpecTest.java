package vn.team03.bepnha;

import org.junit.Test;
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
}
