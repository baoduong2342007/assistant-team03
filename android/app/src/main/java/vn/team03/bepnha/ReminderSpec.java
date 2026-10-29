package vn.team03.bepnha;

/** Small immutable record shared by alarm scheduling and its JVM validation tests. */
final class ReminderSpec {
    static final long MAX_FUTURE_MILLIS = 15L * 24 * 60 * 60 * 1000;
    final String id;
    final long at;
    final String title;
    final String body;
    final String date;
    final String meal;
    final boolean timer;

    ReminderSpec(String id, long at, String title, String body, String date, String meal, boolean timer) {
        if (id == null || !id.matches("[a-z0-9|:_-]{1,90}")) throw new IllegalArgumentException("Invalid reminder ID");
        if (title == null || title.trim().isEmpty() || title.length() > 100) throw new IllegalArgumentException("Invalid title");
        if (body == null || body.length() > 600) throw new IllegalArgumentException("Invalid body");
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException("Invalid date");
        if (!"lunch".equals(meal) && !"dinner".equals(meal)) throw new IllegalArgumentException("Invalid meal");
        if (at <= 0 || at > 8_640_000_000_000_000L) throw new IllegalArgumentException("Invalid time");
        this.id = id;
        this.at = at;
        this.title = title;
        this.body = body;
        this.date = date;
        this.meal = meal;
        this.timer = timer;
    }

    boolean isFuture(long now) {
        return at > now && at - now <= MAX_FUTURE_MILLIS;
    }

    boolean sameAs(ReminderSpec other) {
        return other != null && id.equals(other.id) && at == other.at && title.equals(other.title)
                && body.equals(other.body) && date.equals(other.date) && meal.equals(other.meal) && timer == other.timer;
    }
}
