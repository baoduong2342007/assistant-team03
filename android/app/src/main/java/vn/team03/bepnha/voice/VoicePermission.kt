package vn.team03.bepnha.voice

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/**
 * Microphone permission helpers. Nothing here asks on app start: request only when
 * the user taps the microphone, so the system prompt has an obvious reason.
 */
object VoicePermission {
    const val NAME = Manifest.permission.RECORD_AUDIO

    fun isGranted(context: Context): Boolean =
        context.checkSelfPermission(NAME) == PackageManager.PERMISSION_GRANTED

    /** True when Android wants the app to explain why it needs the microphone before asking again. */
    fun shouldExplain(activity: Activity): Boolean = activity.shouldShowRequestPermissionRationale(NAME)

    /** Opens this app's system settings page, for when the user chose "don't ask again". */
    fun openAppSettings(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}

@Stable
class MicPermission internal constructor(private val state: State<Boolean>, private val launch: () -> Unit) {
    val granted: Boolean get() = state.value

    /** Shows the system prompt; the result arrives through `rememberMicPermission(onResult)`. */
    fun request() = launch()
}

/**
 * Tracks RECORD_AUDIO for a composable. Typical use on a mic tap:
 * `if (mic.granted) voice.start() else mic.request()`.
 * Re-checks on resume, so a permission granted in system settings is picked up.
 */
@Composable
fun rememberMicPermission(onResult: (granted: Boolean) -> Unit = {}): MicPermission {
    val context = LocalContext.current
    val granted = remember { mutableStateOf(VoicePermission.isGranted(context)) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { result ->
        granted.value = result
        onResult(result)
    }
    LifecycleResumeEffect(Unit) {
        granted.value = VoicePermission.isGranted(context)
        onPauseOrDispose { }
    }
    return remember(launcher) { MicPermission(granted) { launcher.launch(VoicePermission.NAME) } }
}
