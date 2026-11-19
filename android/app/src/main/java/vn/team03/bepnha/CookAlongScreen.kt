package vn.team03.bepnha

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import vn.team03.bepnha.voice.VoiceState
import vn.team03.bepnha.voice.rememberMicPermission

/** "Nấu cùng tôi": one big step at a time, read aloud, with voice commands and timers. */
@Composable fun CookAlongScreen(vm: PlannerViewModel) {
    val session = vm.cookAlong ?: return
    val voice = remember(vm) { vm.voice }
    val heard by voice.state.collectAsState()
    val mic = rememberMicPermission { granted -> if (granted) vm.startCookListening() }
    val listening = heard is VoiceState.Listening
    // The phone should not lock while hands are busy cooking.
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val step = session.step
    Column(Modifier.fillMaxSize().testTag("cook-along")) {
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("${session.meal.title} · ${PlannerEngine.fullDate(session.date)}", color = KitchenMuted)
            val animatedProgress by androidx.compose.animation.core.animateFloatAsState(
                targetValue = (session.index + 1f) / session.steps.size,
                animationSpec = iosSpring(),
                label = "cookProgress"
            )
            LinearProgressIndicator(progress = { animatedProgress }, modifier = Modifier.fillMaxWidth())
            Text("Bước ${session.index + 1} / ${session.steps.size}", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.testTag("cook-step-count"))
            Text(step.title, style = MaterialTheme.typography.headlineSmall, color = KitchenGreen,
                modifier = Modifier.semantics { heading() })
            Text(step.text, style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.testTag("cook-step-text").semantics { liveRegion = LiveRegionMode.Polite })
            step.timerMinutes?.let { minutes ->
                KitchenTonalButton(onClick = { vm.cookTimer(minutes) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("cook-timer")) { Text("Hẹn $minutes phút") }
            }
            vm.timerUi()?.let { timer -> KitchenTimer(timer) { vm.cancelTimer(timer.id) } }
            if (listening) Text((heard as VoiceState.Listening).partial.ifBlank { "Đang nghe…" }, style = MaterialTheme.typography.titleMedium)
            vm.voiceNotice?.let { Text(it, color = KitchenMuted, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) }
            Text("Có thể nói: “tiếp”, “đọc lại”, “quay lại”, “hẹn giờ 10 phút”.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        }
        Surface(color = MaterialTheme.colorScheme.surface) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                KitchenButton(onClick = vm::cookNext, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("cook-next")) {
                    Text(if (session.isLast) "Xong, dọn cơm" else "Bước tiếp theo", style = MaterialTheme.typography.titleMedium)
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    KitchenOutlinedButton(onClick = vm::cookBack, enabled = session.index > 0,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("cook-back")) { Text("Quay lại") }
                    KitchenOutlinedButton(onClick = vm::readStep,
                        modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("cook-repeat")) { Text("Đọc lại") }
                }
                KitchenOutlinedButton(onClick = {
                    when {
                        listening -> vm.stopListening()
                        mic.granted -> vm.startCookListening()
                        else -> mic.request()
                    }
                }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("cook-mic").semantics {
                    contentDescription = if (listening) "Dừng nghe" else "Nói lệnh nấu ăn"
                }) { Text(if (listening) "Đang nghe… Bấm để dừng" else "Bấm để nói lệnh") }
                TextButton(onClick = vm::exitCookAlong, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("cook-exit")) {
                    Text("Thoát chế độ nấu")
                }
            }
        }
    }
}
