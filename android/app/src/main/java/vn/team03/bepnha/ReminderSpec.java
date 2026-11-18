package vn.team03.bepnha;

/** Small immutable record shared by alarm scheduling and its JVM validation tests. */
final class ReminderSpec {
    static final long MAX_FUTURE_MILLIS = 15L * 24 * 60 * 60 * 1000;
    static final String KIND_MEAL = "meal";
    static final String KIND_TIMER = "timer";
    static final String KIND_SHOPPING = "shopping";
    final String id;
    final long at;
    final String title;
    final String body;
    final String date;
    final String meal;
    final boolean timer;
    final String kind;

    ReminderSpec(String id, long at, String title, String body, String date, String meal, boolean timer) {
        this(id, at, title, body, date, meal, timer ? KIND_TIMER : KIND_MEAL);
    }

    ReminderSpec(String id, long at, String title, String body, String date, String meal, String kind) {
        if (id == null || !id.matches("[a-z0-9|:_-]{1,90}")) throw new IllegalArgumentException("Invalid reminder ID");
        if (title == null || title.trim().isEmpty() || title.length() > 100) throw new IllegalArgumentException("Invalid title");
        if (body == null || body.length() > 600) throw new IllegalArgumentException("Invalid body");
        if (date == null || !date.matches("\\d{4}-\\d{2}-\\d{2}")) throw new IllegalArgumentException("Invalid date");
        if (!"breakfast".equals(meal) && !"lunch".equals(meal) && !"dinner".equals(meal)) throw new IllegalArgumentException("Invalid meal");
        if (at <= 0 || at > 8_640_000_000_000_000L) throw new IllegalArgumentException("Invalid time");
        if (!KIND_MEAL.equals(kind) && !KIND_TIMER.equals(kind) && !KIND_SHOPPING.equals(kind)) {
            throw new IllegalArgumentException("Invalid kind");
        }
        // A shopping reminder covers a whole day, so its ID must name exactly that day.
        if (KIND_SHOPPING.equals(kind) && !id.equals("shopping:" + date)) throw new IllegalArgumentException("Invalid shopping ID");
        this.id = id;
        this.at = at;
        this.title = title;
        this.body = body;
        this.date = date;
        this.meal = meal;
        this.kind = kind;
        this.timer = KIND_TIMER.equals(kind);
    }

    boolean isFuture(long now) {
        return at > now && at - now <= MAX_FUTURE_MILLIS;
    }

    boolean isShopping() {
        return KIND_SHOPPING.equals(kind);
    }

    boolean sameAs(ReminderSpec other) {
        return other != null && id.equals(other.id) && at == other.at && title.equals(other.title)
                && body.equals(other.body) && date.equals(other.date) && meal.equals(other.meal) && kind.equals(other.kind);
    }
}
