package vn.team03.bepnha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

data class AssistantUiState(
    val context: String,
    val date: String,
    val recipe: String? = null,
    val messages: List<AssistantChatMessage> = emptyList(),
    val question: String = "",
    val busy: Boolean = false,
    val error: String? = null,
    val failedMessageId: String? = null,
    val canRetry: Boolean = false,
    val proposal: List<String> = emptyList(),
    val configured: Boolean = false,
    val suggestions: List<String> = emptyList(),
    val recipeNames: Map<String, String> = emptyMap(),
    val timer: KitchenTimerUi? = null,
)

@Composable fun AssistantScreen(vm: PlannerViewModel) {
    AssistantContent(
        ui = AssistantUiState(
            context = "${vm.state.meal.title} cho ${vm.state.profile.people} người",
            date = PlannerEngine.fullDate(vm.state.date),
            recipe = vm.assistantRecipeId?.let { vm.engine.recipes[it]?.name },
            messages = vm.assistantHistory, question = vm.assistantQuestion,
            busy = vm.assistantBusy, error = vm.assistantError,
            failedMessageId = vm.failedMessageId, canRetry = vm.canRetryAssistant,
            proposal = vm.assistantProposal?.descriptions.orEmpty(),
            configured = vm.assistantConfigured, suggestions = vm.assistantSuggestions(),
            recipeNames = vm.engine.recipes.mapValues { it.value.name }, timer = vm.timerUi(),
        ),
        onQuestion = { vm.assistantQuestion = it }, onSend = vm::submitAssistant,
        onRetry = vm::retryAssistant, onOffline = vm::lookupFailedOffline,
        onRecipe = vm::openRecipe, onConfirm = vm::confirmAssistantProposal,
        onCancel = vm::cancelAssistantProposal, onCancelTimer = vm::cancelTimer,
    )
}

/** No ViewModel/network dependency: every device state can be previewed and tested. */
@Composable fun AssistantContent(
    ui: AssistantUiState,
    onQuestion: (String) -> Unit = {}, onSend: () -> Unit = {},
    onRetry: () -> Unit = {}, onOffline: () -> Unit = {}, onRecipe: (String) -> Unit = {},
    onConfirm: () -> Unit = {}, onCancel: () -> Unit = {}, onCancelTimer: () -> Unit = {},
) {
    val scroll = rememberLazyListState()
    var followingLatest by rememberSaveable { mutableStateOf(true) }
    var previousCount by rememberSaveable { mutableIntStateOf(ui.messages.size) }
    var newMessages by rememberSaveable { mutableStateOf(false) }
    var jumpToEnd by remember { mutableIntStateOf(0) }
    val canSend = !ui.busy && ui.proposal.isEmpty()
    LaunchedEffect(scroll) {
        snapshotFlow { scroll.isScrollInProgress to scroll.canScrollForward }.collect { (moving, more) ->
            if (moving) followingLatest = !more
            if (!more) newMessages = false
        }
    }
    LaunchedEffect(ui.messages.size, ui.busy, ui.proposal, ui.error) {
        val added = ui.messages.size > previousCount
        val follow = followingLatest || (added && ui.messages.lastOrNull()?.role == "user")
        previousCount = ui.messages.size
        if (!added && !ui.busy && ui.proposal.isEmpty() && ui.error == null) return@LaunchedEffect
        withFrameNanos { }
        if (follow && scroll.layoutInfo.totalItemsCount > 0) {
            scroll.animateScrollToItem(scroll.layoutInfo.totalItemsCount - 1)
            followingLatest = true
            newMessages = false
        } else if (added || ui.proposal.isNotEmpty() || ui.error != null) newMessages = true
    }
    LaunchedEffect(jumpToEnd) {
        if (jumpToEnd > 0 && scroll.layoutInfo.totalItemsCount > 0) {
            scroll.animateScrollToItem(scroll.layoutInfo.totalItemsCount - 1)
            followingLatest = true
            newMessages = false
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().imePadding().testTag("assistant-screen")) {
        val compact = maxHeight < 360.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f ||
            (ui.timer != null && maxHeight < 640.dp)
        val cramped = maxHeight < 240.dp
        val inlineComposer = maxHeight < 360.dp && maxWidth >= 480.dp
        Column(Modifier.fillMaxSize()) {
            if (!compact) {
                AssistantContext(ui)
                ui.timer?.let { KitchenTimer(it, onCancelTimer) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    Modifier.fillMaxSize().testTag("assistant-history"), state = scroll,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    if (compact) {
                        item("context") { AssistantContext(ui) }
                        ui.timer?.let { timer -> item("timer") { KitchenTimer(timer, onCancelTimer) } }
                    }
                    if (ui.messages.isEmpty()) item("welcome") {
                        AssistantWelcome(ui.suggestions, canSend, onQuestion)
                    }
                    item("connection") {
                        Text(
                            if (ui.configured) "Khi dùng AI, câu hỏi và ngữ cảnh bữa ăn được gửi đến máy chủ. Hãy xem lại các gợi ý."
                            else "AI chưa được kết nối. Bạn có thể tìm trong 14 công thức có sẵn trên máy.",
                            style = MaterialTheme.typography.bodySmall, color = KitchenMuted,
                        )
                    }
                    items(ui.messages, key = { it.id }) { message ->
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            AssistantMessage(message)
                            message.recipeIds.filter { it in ui.recipeNames }.forEach { id ->
                                KitchenOutlinedButton(onClick = { onRecipe(id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                    Text("Xem ${ui.recipeNames.getValue(id)}")
                                }
                            }
                            if (ui.failedMessageId == message.id && ui.error != null) {
                                AssistantError(ui.error, ui.canRetry, onRetry, onOffline)
                            }
                        }
                    }
                    if (ui.busy) item("busy") {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Đang chuẩn bị câu trả lời…", style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                    }
                    if (ui.proposal.isNotEmpty()) item("proposal") {
                        OutlinedCard(
                            Modifier.fillMaxWidth().testTag("assistant-preview"),
                            border = BorderStroke(1.dp, KitchenGreen),
                            colors = CardDefaults.outlinedCardColors(containerColor = KitchenSoft),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Xem lại thay đổi", style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.semantics { heading() })
                                ui.proposal.forEach { Text(it) }
                                Text("Chỉ áp dụng sau khi bạn xác nhận.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                                KitchenButton(onClick = onConfirm,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-confirm")) { Text("Xác nhận thay đổi") }
                                KitchenOutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Hủy") }
                            }
                        }
                    }
                    if (ui.error != null && ui.failedMessageId == null) item("error") {
                        AssistantError(ui.error, false, onRetry, onOffline)
                    }
                }
                if (newMessages && scroll.canScrollForward) {
                    KitchenTonalButton(onClick = { jumpToEnd++ },
                        modifier = Modifier.align(Alignment.BottomCenter).padding(8.dp).heightIn(min = 48.dp)) { Text("Tin nhắn mới") }
                }
            }
            AssistantComposer(ui, compact, cramped = cramped,
                inline = inlineComposer,
                onQuestion = onQuestion, onSend = onSend, onProposal = { jumpToEnd++ })
        }
    }
}

@Composable private fun AssistantComposer(
    ui: AssistantUiState, compact: Boolean, cramped: Boolean, inline: Boolean,
    onQuestion: (String) -> Unit, onSend: () -> Unit, onProposal: () -> Unit,
) {
    val canSend = !ui.busy && ui.proposal.isEmpty() && ui.question.isNotBlank()
    val keyboard = androidx.compose.ui.platform.LocalSoftwareKeyboardController.current
    val submit = {
        if (canSend) {
            if (cramped) keyboard?.hide()
            onSend()
        }
    }
    val input: @Composable (Modifier) -> Unit = { modifier ->
        OutlinedTextField(
            value = ui.question, onValueChange = { onQuestion(it.take(1000)) },
            label = if (cramped) null else { { Text("Câu hỏi của bạn") } },
            placeholder = { Text(if (cramped) "Câu hỏi của bạn" else "Ví dụ: Món nào nấu nhanh?") },
            modifier = modifier.testTag("assistant-input").semantics { contentDescription = "Câu hỏi của bạn" },
            singleLine = cramped, minLines = 1, maxLines = if (compact) 1 else 3,
            enabled = ui.proposal.isEmpty(),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
            keyboardActions = KeyboardActions(onSend = { submit() }),
        )
    }
    val send: @Composable (Modifier) -> Unit = { modifier ->
        KitchenButton(onClick = submit, enabled = canSend,
            modifier = modifier.testTag("assistant-send").semantics {
                contentDescription = if (ui.configured) "Gửi câu hỏi" else "Tìm công thức"
            }) {
            Text(if (ui.configured) "Gửi" else if (inline) "Tìm" else "Tìm công thức")
        }
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        if (cramped && ui.proposal.isNotEmpty()) {
            KitchenButton(onClick = onProposal, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Xem thay đổi") }
        } else if (inline) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                input(Modifier.weight(1f))
                send(Modifier.widthIn(min = 96.dp, max = 160.dp))
            }
        } else {
            Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (ui.proposal.isNotEmpty()) Text("Xác nhận hoặc hủy đề xuất để hỏi tiếp.",
                    style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                input(Modifier.fillMaxWidth())
                send(Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable private fun AssistantContext(ui: AssistantUiState) {
    Surface(color = KitchenSoft) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(ui.context, style = MaterialTheme.typography.titleSmall, color = KitchenGreen)
            Text(ui.date, style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            ui.recipe?.let { Text("Đang hỏi về: $it", style = MaterialTheme.typography.bodySmall) }
        }
    }
}

@Composable private fun AssistantWelcome(suggestions: List<String>, enabled: Boolean, onSelect: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Bạn cần chuẩn bị gì cho bữa này?", style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() })
        Text("Hỏi cách nấu, tìm món thay thế hoặc xem nguyên liệu.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        Text("Chọn gợi ý để sửa trước khi gửi", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        suggestions.take(3).forEach { suggestion ->
            KitchenOutlinedButton(onClick = { onSelect(suggestion) }, enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(suggestion, Modifier.weight(1f)) }
        }
    }
}

@Composable private fun AssistantMessage(message: AssistantChatMessage) {
    val user = message.role == "user"
    Column(Modifier.fillMaxWidth(), horizontalAlignment = if (user) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(if (user) "Bạn" else when (message.source) {
            AssistantSource.LOCAL -> "Tra cứu trên máy"
            AssistantSource.SYSTEM -> "Bếp Nhà"
            AssistantSource.AI -> "Trợ lý Bếp Nhà"
        }, style = MaterialTheme.typography.labelMedium, color = KitchenMuted)
        if (user) {
            Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)) {
                SelectionContainer { Text(message.text, Modifier.padding(16.dp)) }
            }
        } else {
            SelectionContainer {
                Text(message.text, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
        }
    }
}

@Composable private fun AssistantError(error: String, retry: Boolean, onRetry: () -> Unit, onOffline: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.errorContainer, shape = RoundedCornerShape(12.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(error, color = MaterialTheme.colorScheme.onErrorContainer,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            if (retry) {
                KitchenOutlinedButton(onClick = onRetry, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-retry")) { Text("Thử lại") }
                TextButton(onClick = onOffline, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Tìm trong công thức có sẵn") }
            }
        }
    }
}

/** Keep preview default callbacks beside their implementation for Studio incremental rendering. */
@Composable fun AssistantPreviewContent(ui: AssistantUiState) { AssistantContent(ui) }

@androidx.compose.ui.tooling.preview.Preview(name = "Chat phone", widthDp = 360, heightDp = 800, showBackground = true)
@androidx.compose.ui.tooling.preview.Preview(name = "Chat large text", widthDp = 320, heightDp = 640, fontScale = 2f, showBackground = true)
@Composable fun AssistantScreenPreview() {
    BepNhaTheme {
        KitchenFrame(title = "Hỏi Bếp Nhà", assistant = true, showBack = true) {
            AssistantContent(AssistantUiState(
                context = "Bữa tối cho 4 người", date = "Thứ Sáu, 6 tháng 11",
                question = "Món nào nấu nhanh?", suggestions = listOf("Món nấu nhanh", "Nấu món nào trước?"),
            ))
        }
    }
}
