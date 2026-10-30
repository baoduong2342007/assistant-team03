package vn.team03.bepnha

import android.Manifest
import android.app.TimePickerDialog
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import java.time.LocalTime

data class NativeActions(
    val notifications: () -> Unit,
    val exactAlarms: () -> Unit,
    val calendar: (List<CookingTask>) -> Unit,
    val copy: (String) -> Unit,
    val pickTime: (LocalTime, (LocalTime) -> Unit) -> Unit,
)

/** Entirely native Android entry point. UI is rendered by Jetpack Compose. */
class MainActivity : ComponentActivity() {
    private val planner by lazy { ViewModelProvider(this)[PlannerViewModel::class.java] }
    private val notificationPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) {
        planner.refreshPermissions()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paper = android.graphics.Color.rgb(250, 249, 245)
        enableEdgeToEdge(SystemBarStyle.light(paper, paper), SystemBarStyle.light(paper, paper))
        ReminderStore.createChannel(this)
        planner.openNotification(intent)
        val actions = NativeActions(::requestNotifications, ::requestExactAlarms, ::openCalendar, { text ->
            getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText("Bếp Nhà", text))
            planner.showMessage("Đã sao chép danh sách đi chợ.")
        }, { initial, selected ->
            TimePickerDialog(this, { _, hour, minute -> selected(LocalTime.of(hour, minute)) },
                initial.hour, initial.minute, true).show()
        })
        setContent { BepNhaTheme { BepNhaApp(planner, actions) } }
    }

    override fun onResume() { super.onResume(); planner.refreshPermissions() }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent); setIntent(intent); planner.openNotification(intent)
    }

    private fun requestNotifications() {
        if (ReminderStore.canNotify(this)) return
        val preferences = ReminderStore.preferences(this)
        val asked = preferences.getBoolean("asked_notifications", false)
        if (Build.VERSION.SDK_INT >= 33 && (!asked || shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS))) {
            preferences.edit().putBoolean("asked_notifications", true).apply()
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, packageName))
    }

    private fun requestExactAlarms() {
        if (Build.VERSION.SDK_INT >= 31 && !ReminderStore.canScheduleExact(this)) {
            try { startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:$packageName"))) }
            catch (exception: ActivityNotFoundException) { toast("Thiết bị không có mục cấp quyền nhắc đúng giờ.") }
        }
    }

    private fun openCalendar(tasks: List<CookingTask>) {
        val intent = Intent(Intent.ACTION_INSERT).setData(CalendarContract.Events.CONTENT_URI)
            .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, tasks.first().at)
            .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, tasks.last().at)
            .putExtra(CalendarContract.Events.TITLE, "Vào bếp cùng Bếp Nhà")
            .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Ho_Chi_Minh")
            .putExtra(CalendarContract.Events.DESCRIPTION, tasks.joinToString("\n") { "${PlannerEngine.time(it.at)} — ${it.title}" })
        try { startActivity(intent) }
        catch (exception: ActivityNotFoundException) { toast("Bạn cần cài ứng dụng lịch để thêm sự kiện.") }
    }

    private fun toast(text: String) = Toast.makeText(this, text, Toast.LENGTH_LONG).show()
}
