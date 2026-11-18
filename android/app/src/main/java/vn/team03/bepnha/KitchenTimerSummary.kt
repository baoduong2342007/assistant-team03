package vn.team03.bepnha

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

/** A long recipe name must never consume the entire phone's fixed footer. */
@Composable fun KitchenTimerSummary(timer: KitchenTimerUi, onCancel: () -> Unit) {
    var details by rememberSaveable { mutableStateOf(false) }
    KitchenOutlinedButton(onClick = { details = true }, modifier = Modifier.fillMaxWidth()
        .padding(horizontal = 16.dp).heightIn(min = 56.dp).testTag("timer-summary")) {
        Text("Hẹn giờ ${timer.remaining}", modifier = Modifier.testTag("timer-countdown"))
    }
    if (details) AlertDialog(
        onDismissRequest = { details = false },
        title = { Text("Hẹn giờ đang chạy") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(timer.label)
                Text(timer.remaining, style = MaterialTheme.typography.headlineSmall)
            }
        },
        confirmButton = { TextButton(onClick = { details = false; onCancel() }) { Text("Dừng hẹn giờ") } },
        dismissButton = { TextButton(onClick = { details = false }) { Text("Tiếp tục nấu") } },
    )
}
