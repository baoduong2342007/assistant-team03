package vn.team03.bepnha;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;

public final class ReminderReceiver extends BroadcastReceiver {
    @Override public void onReceive(Context context, Intent intent) {
        ReminderSpec spec = ReminderStore.take(context, intent.getStringExtra("id"), intent.getLongExtra("at", -1));
        if (spec == null || !ReminderStore.canNotify(context)) return;
        ReminderStore.createChannel(context);
        Intent open = new Intent(context, MainActivity.class)
                .setData(new Uri.Builder().scheme("bepnha").authority("open").appendPath(spec.id).build())
                .putExtra("date", spec.date).putExtra("meal", spec.meal)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent tap = PendingIntent.getActivity(context, 0, open, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notification = new Notification.Builder(context, ReminderStore.CHANNEL)
                .setSmallIcon(R.drawable.ic_notification)
                .setColor(Color.rgb(73, 97, 60))
                .setContentTitle(spec.title)
                .setContentText(spec.body)
                .setStyle(new Notification.BigTextStyle().bigText(spec.body))
                .setContentIntent(tap)
                .setWhen(spec.at)
                .setShowWhen(true)
                .setAutoCancel(true)
                .setCategory(spec.timer ? Notification.CATEGORY_ALARM : Notification.CATEGORY_REMINDER)
                .build();
        try { context.getSystemService(NotificationManager.class).notify(spec.id, 0, notification); }
        catch (SecurityException ignored) { /* Respect a permission change while the receiver was running. */ }
    }
}
