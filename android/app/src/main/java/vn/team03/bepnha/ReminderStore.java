package vn.team03.bepnha;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.LinkedHashMap;
import java.util.Map;

final class ReminderStore {
    static final String CHANNEL = "bep_nha_cooking";
    private static final String PREFS = "bep_nha_native";
    private static final String ALARMS = "alarms";

    private ReminderStore() {}

    static void createChannel(Context context) {
        NotificationManager manager = context.getSystemService(NotificationManager.class);
        NotificationChannel channel = new NotificationChannel(CHANNEL,
                context.getString(R.string.reminder_channel), NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription(context.getString(R.string.reminder_channel_description));
        manager.createNotificationChannel(channel);
    }

    static boolean canNotify(Context context) {
        return context.getSystemService(NotificationManager.class).areNotificationsEnabled();
    }

    static boolean canScheduleExact(Context context) {
        return Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager.class).canScheduleExactAlarms();
    }

    static SharedPreferences preferences(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    static synchronized void synchronize(Context context, String payload) throws JSONException {
        // Parse the entire payload before touching existing alarms, so a bad update cannot erase them.
        Map<String, ReminderSpec> incoming = parse(payload);
        Map<String, ReminderSpec> previous = read(context);
        long now = System.currentTimeMillis();
        incoming.values().removeIf(spec -> !spec.isFuture(now));
        // Persist desired state before scheduling; reboot/process recovery can safely retry it.
        write(context, incoming);
        for (ReminderSpec old : previous.values()) {
            if (!old.sameAs(incoming.get(old.id))) cancel(context, old);
        }
        for (ReminderSpec spec : incoming.values()) {
            if (!spec.sameAs(previous.get(spec.id))) schedule(context, spec);
        }
    }

    static synchronized void restore(Context context) {
        Map<String, ReminderSpec> specs = read(context);
        long now = System.currentTimeMillis();
        specs.values().removeIf(spec -> !spec.isFuture(now));
        write(context, specs);
        for (ReminderSpec spec : specs.values()) schedule(context, spec);
    }

    static synchronized ReminderSpec take(Context context, String id, long at) {
        Map<String, ReminderSpec> specs = read(context);
        ReminderSpec spec = specs.get(id);
        // Old intents are ignored after a dish/time change, and repeated broadcasts cannot notify twice.
        if (spec == null || spec.at != at || System.currentTimeMillis() < at) return null;
        specs.remove(id);
        write(context, specs);
        return spec;
    }

    private static Map<String, ReminderSpec> read(Context context) {
        try { return parse(preferences(context).getString(ALARMS, "[]")); }
        catch (JSONException | IllegalArgumentException exception) { return new LinkedHashMap<>(); }
    }

    private static Map<String, ReminderSpec> parse(String payload) throws JSONException {
        if (payload == null || payload.length() > 40_000) throw new JSONException("Alarm payload too large");
        JSONArray array = new JSONArray(payload);
        if (array.length() > 32) throw new JSONException("Too many alarms");
        Map<String, ReminderSpec> result = new LinkedHashMap<>();
        for (int i = 0; i < array.length(); i++) {
            JSONObject item = array.getJSONObject(i);
            if (!(item.get("at") instanceof Number)) throw new JSONException("Time must be numeric");
            ReminderSpec spec = new ReminderSpec(item.getString("id"), item.getLong("at"),
                    item.getString("title"), item.getString("body"), item.getString("date"),
                    item.getString("meal"), item.optBoolean("timer", false));
            if (result.put(spec.id, spec) != null) throw new JSONException("Duplicate alarm ID");
        }
        return result;
    }

    // Receivers may lose their process immediately after return; alarm state must already be durable.
    @SuppressLint("ApplySharedPref")
    private static void write(Context context, Map<String, ReminderSpec> specs) {
        JSONArray array = new JSONArray();
        for (ReminderSpec spec : specs.values()) {
            JSONObject item = new JSONObject();
            try {
                item.put("id", spec.id).put("at", spec.at).put("title", spec.title).put("body", spec.body)
                        .put("date", spec.date).put("meal", spec.meal).put("timer", spec.timer);
                array.put(item);
            } catch (JSONException exception) { throw new IllegalStateException(exception); }
        }
        preferences(context).edit().putString(ALARMS, array.toString()).commit();
    }

    private static PendingIntent alarmIntent(Context context, ReminderSpec spec) {
        Intent intent = new Intent(context, ReminderReceiver.class)
                .setData(new Uri.Builder().scheme("bepnha").authority("alarm").appendPath(spec.id).build())
                .putExtra("id", spec.id).putExtra("at", spec.at);
        return PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }

    private static void cancel(Context context, ReminderSpec spec) {
        PendingIntent intent = alarmIntent(context, spec);
        context.getSystemService(AlarmManager.class).cancel(intent);
        intent.cancel();
    }

    private static void schedule(Context context, ReminderSpec spec) {
        AlarmManager manager = context.getSystemService(AlarmManager.class);
        PendingIntent intent = alarmIntent(context, spec);
        try {
            if (canScheduleExact(context)) manager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, spec.at, intent);
            else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, spec.at, intent);
        } catch (SecurityException exception) {
            // Access may be revoked between the capability check and this call.
            manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, spec.at, intent);
        }
    }
}
