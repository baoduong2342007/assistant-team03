package vn.team03.bepnha

import androidx.activity.compose.LocalActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import vn.team03.bepnha.voice.LiveCookState
import vn.team03.bepnha.voice.VoiceState
import vn.team03.bepnha.voice.rememberMicPermission

/** "Nấu cùng tôi": one big step at a time, read aloud, with voice commands and timers. */
@Composable fun CookAlongScreen(vm: PlannerViewModel, active: Boolean = true) {
    var lastSession by remember { mutableStateOf<CookAlong?>(null) }
    val current = vm.cookAlong
    if (current != null) SideEffect { lastSession = current }
    val session = current ?: lastSession ?: return
    val voice = remember(vm) { vm.voice }
    val heard by voice.state.collectAsState()
    val mic = rememberMicPermission { granted -> if (granted) vm.startCookListening() }
    val listening = heard is VoiceState.Listening
    val live = vm.liveCookState
    val talking = vm.liveCookOpen
    // Android does not let a backgrounded app keep the microphone; end the conversation instead.
    // Rotating the phone also stops the activity briefly; the conversation lives on in the ViewModel.
    val activity = LocalActivity.current
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) { if (activity?.isChangingConfigurations != true) vm.stopCookLive() }
    // The phone should not lock while hands are busy cooking.
    val view = LocalView.current
    if (active) DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val step = session.step
    val progress = animateFloatAsState(
        (session.index + 1f) / session.steps.size,
        tween(ComponentMotionMillis),
        label = "cook step progress",
    )
    val back: @Composable () -> Unit = {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            KitchenOutlinedButton(onClick = vm::cookBack, enabled = session.index > 0,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("cook-back")) { Text("Quay lại") }
            KitchenOutlinedButton(onClick = vm::readStep,
                modifier = Modifier.weight(1f).heightIn(min = 64.dp).testTag("cook-repeat")) { Text("Đọc lại") }
        }
    }
    val exit: @Composable () -> Unit = {
        TextButton(onClick = vm::exitCookAlong, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("cook-exit")) {
            Text("Thoát chế độ nấu")
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        // Short screens or large text: only the next step and the microphone stay pinned; going back,
        // reading again and leaving scroll with the step, so the step text keeps its room.
        val compact = maxHeight < CompactBelow * LocalDensity.current.fontScale.coerceAtLeast(1f)
        val panelHeight = minOf(VoicePanelMaxHeight, maxHeight * if (compact) 0.16f else 0.2f)
        Column(Modifier.fillMaxSize().testTag("cook-along")) {
            Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).testTag("cook-step-area").padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("${session.meal.title} · ${PlannerEngine.fullDate(session.date)}", color = KitchenMuted)
                LinearProgressIndicator(progress = { progress.value }, modifier = Modifier.fillMaxWidth())
                Text("Bước ${session.index + 1} / ${session.steps.size}", style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.testTag("cook-step-count"))
                KitchenContentTransition(
                    session, "cook step", Modifier.fillMaxWidth(),
                    direction = { from, to -> to.index.compareTo(from.index) },
                ) { shown ->
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Text(shown.step.title, style = MaterialTheme.typography.headlineSmall, color = KitchenGreen,
                            modifier = Modifier.semantics { heading() })
                        Text(shown.step.text, style = MaterialTheme.typography.headlineMedium,
                            modifier = Modifier.testTag("cook-step-text").semantics { liveRegion = LiveRegionMode.Polite })
                    }
                }
                step.timerMinutes?.let { minutes ->
                    KitchenTonalButton(onClick = { vm.cookTimer(minutes) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("cook-timer")) { Text("Hẹn $minutes phút") }
                }
                vm.timerUi()?.let { timer -> KitchenTimer(timer) { vm.cancelTimer(timer.id) } }
                Text(
                    if (vm.cookUsesLive) "Bấm nút micro rồi nói tự nhiên: “xong rồi, tiếp đi”, “cá chín chưa thì nhìn sao?”, “hẹn giờ 10 phút”."
                    else "Có thể nói: “tiếp”, “đọc lại”, “quay lại”, “hẹn giờ 10 phút”.",
                    style = MaterialTheme.typography.bodySmall, color = KitchenMuted,
                )
                if (compact) { back(); exit() }
            }
            Surface(color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Above the buttons, so what was heard and answered stays in view however long the step is.
                    VoicePanel(heard, live, vm.voiceNotice, panelHeight)
                    KitchenButton(onClick = vm::cookNext, modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("cook-next")) {
                        Text(if (session.isLast) "Xong, dọn cơm" else "Bước tiếp theo", style = MaterialTheme.typography.titleMedium)
                    }
                    if (!compact) back()
                    KitchenOutlinedButton(onClick = {
                        when {
                            talking -> vm.stopCookLive()
                            listening -> vm.stopListening()
                            mic.granted -> vm.startCookListening()
                            else -> mic.request()
                        }
                    }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("cook-mic").semantics {
                        contentDescription = when {
                            live is LiveCookState.Connecting -> "Huỷ kết nối"
                            talking -> "Kết thúc trò chuyện"
                            listening -> "Dừng nghe"
                            vm.cookUsesLive -> "Trò chuyện với trợ lý nấu ăn"
                            else -> "Nói lệnh nấu ăn"
                        }
                    }) {
                        KitchenIcon(KitchenGlyph.MIC, Modifier.size(22.dp), KitchenGreen)
                        Spacer(Modifier.width(8.dp))
                        // Short labels: the panel above already says "connecting" or what is being heard.
                        Text(when {
                            live is LiveCookState.Connecting -> "Huỷ kết nối"
                            talking -> "Kết thúc trò chuyện"
                            listening -> "Dừng nghe"
                            vm.cookUsesLive -> "Bấm để trò chuyện"
                            else -> "Bấm để nói lệnh"
                        })
                    }
                    if (!compact) exit()
                }
            }
        }
    }
}

/** Below this height (per unit of font scale) the cook-along switches to its compact layout. */
private val CompactBelow = 520.dp

private val VoicePanelMaxHeight = 168.dp

/**
 * What the microphone heard, the last exchange with Gemini Live and any notice, in a short box
 * that always shows its newest line.
 */
@Composable private fun VoicePanel(heard: VoiceState, live: LiveCookState, notice: String?, maxHeight: Dp) {
    val partial = (heard as? VoiceState.Listening)?.partial?.ifBlank { "Đang nghe…" }
        ?: "Đang kết nối…".takeIf { live is LiveCookState.Connecting }
    val exchange = live as? LiveCookState.Active
    if (partial == null && exchange == null && notice == null) return
    val scroll = rememberScrollState()
    FollowNewest(scroll)
    Column(Modifier.fillMaxWidth().heightIn(max = maxHeight).verticalScroll(scroll).testTag("cook-voice-panel"),
        verticalArrangement = Arrangement.spacedBy(6.dp)) {
        partial?.let { Text(it, style = MaterialTheme.typography.titleMedium) }
        exchange?.let { LiveExchange(it) }
        notice?.let { Text(it, color = KitchenMuted, modifier = Modifier.testTag("cook-voice-notice").semantics { liveRegion = LiveRegionMode.Polite }) }
    }
}

/** Keeps [scroll] at the bottom as text grows. */
@Composable private fun FollowNewest(scroll: ScrollState) {
    LaunchedEffect(scroll) { snapshotFlow { scroll.maxValue }.collect { scroll.scrollTo(it) } }
}

/** The last exchange with Gemini Live, for people who cannot hear the phone well over the stove. */
@Composable private fun LiveExchange(live: LiveCookState.Active) {
    Column(Modifier.fillMaxWidth().testTag("cook-live-transcript").semantics { liveRegion = LiveRegionMode.Polite },
        verticalArrangement = Arrangement.spacedBy(4.dp)) {
        if (live.user.isNotBlank()) Text("Bạn: ${live.user}", style = MaterialTheme.typography.titleMedium)
        if (live.model.isNotBlank()) Text("Trợ lý: ${live.model}", style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
        if (live.user.isBlank() && live.model.isBlank()) Text("Đang nghe…", style = MaterialTheme.typography.titleMedium)
    }
}
