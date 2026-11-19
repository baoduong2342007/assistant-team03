package vn.team03.bepnha

import androidx.compose.animation.core.tween
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
import vn.team03.bepnha.voice.SpeechState
import vn.team03.bepnha.voice.VoiceState
import vn.team03.bepnha.voice.rememberMicPermission

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
    val mealSuggestions: List<AssistantMealCard> = emptyList(),
    val mealSuggestionMessageId: String? = null,
    val canEditProposal: Boolean = false,
    val revisionTarget: String? = null,
    /** Dish swaps grouped by meal; when not empty they replace the swap lines of [proposal]. */
    val proposalMeals: List<ProposalMealUi> = emptyList(),
    val proposalOther: List<String> = emptyList(),
    val canUndo: Boolean = false,
    /** Voice: hidden entirely when [voiceAvailable] is false (e.g. previews). */
    val voiceAvailable: Boolean = false,
    val listening: Boolean = false,
    val partial: String = "",
    val voiceNotice: String? = null,
    val micGranted: Boolean = true,
    val speaking: Boolean = false,
    val canClearHistory: Boolean = false,
)

data class AssistantMealCard(
    val id: Int,
    val target: String,
    val dishes: List<String>,
    val available: List<String>,
    val missing: List<String>,
)

@Composable fun AssistantScreen(vm: PlannerViewModel, active: Boolean = true) {
    val voice = remember(vm) { vm.voice }
    val heard by voice.state.collectAsState()
    val speech by voice.speech.collectAsState()
    val mic = rememberMicPermission { granted -> if (granted) vm.startListening() }
    val recipeNames = remember(vm) { vm.engine.recipes.mapValues { it.value.name } }
    val meals = vm.assistantMealSuggestions.map { suggestion ->
        val menu = suggestion.recipeIds.map(vm.engine.recipes::getValue)
        val (available, missing) = vm.engine.ingredients(menu, vm.state.profile.people)
            .partition { it.key in suggestion.pantryKeys }
        AssistantMealCard(
            id = suggestion.id,
            target = "${suggestion.meal.title} · ${PlannerEngine.fullDate(suggestion.date)}",
            dishes = menu.map { it.name },
            available = available.map { "${it.name} ${PlannerEngine.quantity(it.amount)} ${it.unit}" },
            missing = missing.map { "${it.name} ${PlannerEngine.quantity(it.amount)} ${it.unit}" },
        )
    }
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
            recipeNames = recipeNames, timer = vm.timerUi(),
            mealSuggestions = meals, canEditProposal = vm.assistantProposal?.canReviseMenu == true,
            mealSuggestionMessageId = vm.assistantMealMessageId,
            revisionTarget = vm.assistantRevisionTarget,
            proposalMeals = vm.proposalMeals(), proposalOther = vm.proposalOther(),
            canUndo = vm.canUndoAssistant && vm.assistantProposal == null && !vm.assistantBusy,
            voiceAvailable = true, listening = heard is VoiceState.Listening,
            partial = (heard as? VoiceState.Listening)?.partial.orEmpty(), voiceNotice = vm.voiceNotice,
            micGranted = mic.granted, speaking = speech == SpeechState.Speaking,
            canClearHistory = vm.assistantHistory.any { it.role == "user" } && !vm.assistantBusy && vm.assistantProposal == null,
        ),
        onClearHistory = vm::clearAssistantHistory,
        onMic = {
            when {
                heard is VoiceState.Listening -> vm.stopListening()
                mic.granted -> vm.startListening()
                else -> mic.request()
            }
        },
        onReadAgain = vm::readAgain, onStopSpeaking = vm::stopSpeaking,
        onQuestion = { vm.assistantQuestion = it }, onSend = vm::submitAssistant,
        onRetry = vm::retryAssistant, onOffline = vm::lookupFailedOffline,
        onRecipe = vm::openRecipe, onConfirm = vm::confirmAssistantProposal,
        onCancel = vm::cancelAssistantProposal, onCancelTimer = { vm.timerUi()?.let { vm.cancelTimer(it.id) } },
        onChooseMeal = vm::chooseAssistantMeal, onEdit = vm::editAssistantProposal,
        onDropMeal = { vm.dropProposalMeal(it.date, it.meal) }, onUndo = vm::undoAssistant,
        active = active,
    )
}

/** No ViewModel/network dependency: every device state can be previewed and tested. */
@Composable fun AssistantContent(
    ui: AssistantUiState,
    onQuestion: (String) -> Unit = {}, onSend: () -> Unit = {},
    onRetry: () -> Unit = {}, onOffline: () -> Unit = {}, onRecipe: (String) -> Unit = {},
    onConfirm: () -> Unit = {}, onCancel: () -> Unit = {}, onCancelTimer: () -> Unit = {},
    onChooseMeal: (Int) -> Unit = {}, onEdit: () -> Unit = {},
    onDropMeal: (ProposalMealUi) -> Unit = {}, onUndo: () -> Unit = {},
    onMic: () -> Unit = {}, onReadAgain: () -> Unit = {}, onStopSpeaking: () -> Unit = {},
    onClearHistory: () -> Unit = {},
    active: Boolean = true,
) {
    var confirmClear by rememberSaveable { mutableStateOf(false) }
    if (confirmClear && active) AlertDialog(onDismissRequest = { confirmClear = false },
        title = { Text("Xóa lịch sử trò chuyện?") },
        text = { Text("Các tin nhắn sẽ bị xóa khỏi máy. Thực đơn và dữ liệu nhà mình vẫn giữ nguyên.") },
        confirmButton = { TextButton(onClick = { confirmClear = false; onClearHistory() }, modifier = Modifier.testTag("assistant-clear-confirm")) { Text("Xóa") } },
        dismissButton = { TextButton(onClick = { confirmClear = false }) { Text("Giữ lại") } })
    val scroll = rememberLazyListState()
    var followingLatest by rememberSaveable { mutableStateOf(true) }
    var previousCount by rememberSaveable { mutableIntStateOf(ui.messages.size) }
    var newMessages by rememberSaveable { mutableStateOf(false) }
    var jumpToEnd by remember { mutableIntStateOf(0) }
    val canSend = !ui.busy && ui.proposal.isEmpty()
    LaunchedEffect(scroll, active) {
        if (!active) return@LaunchedEffect
        snapshotFlow { scroll.isScrollInProgress to scroll.canScrollForward }.collect { (moving, more) ->
            if (moving) followingLatest = !more
            if (!more) newMessages = false
        }
    }
    LaunchedEffect(ui.messages.size, ui.busy, ui.proposal, ui.error, ui.revisionTarget, active) {
        if (!active) return@LaunchedEffect
        val added = ui.messages.size > previousCount
        val follow = followingLatest || (added && ui.messages.lastOrNull()?.role == "user")
        previousCount = ui.messages.size
        if (!added && !ui.busy && ui.proposal.isEmpty() && ui.error == null && ui.revisionTarget == null) return@LaunchedEffect
        withFrameNanos { }
        if (follow && scroll.layoutInfo.totalItemsCount > 0) {
            scroll.animateScrollToItem(scroll.layoutInfo.totalItemsCount - 1)
            followingLatest = true
            newMessages = false
        } else if (added || ui.proposal.isNotEmpty() || ui.error != null || ui.revisionTarget != null) newMessages = true
    }
    LaunchedEffect(jumpToEnd) {
        if (!active) return@LaunchedEffect
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
                    item("connection") {
                        Text(
                            if (ui.configured) "Khi dùng AI, câu hỏi và ngữ cảnh bữa ăn được gửi đến máy chủ. Hãy xem lại các gợi ý."
                            else "Đang dùng trợ lý trên máy với ${ui.recipeNames.size} công thức có sẵn. Bạn có thể tra cứu, gợi ý mâm và đề xuất đổi món.",
                            style = MaterialTheme.typography.bodySmall, color = KitchenMuted,
                        )
                    }
                    items(ui.messages, key = { it.id }) { message ->
                        Column(
                            Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null, placementSpec = null),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AssistantMessage(message)
                            if (message.id == ui.mealSuggestionMessageId) {
                                ui.mealSuggestions.forEach { meal -> MealSuggestionCard(meal, canSend) { onChooseMeal(meal.id) } }
                            }
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
                    // The greeting is not a question, so suggestions stay until the user asks something.
                    if (ui.messages.none { it.role == "user" }) item("welcome") {
                        AssistantWelcome(ui.suggestions, canSend, onQuestion)
                    }
                    if (ui.mealSuggestions.isNotEmpty() && ui.mealSuggestionMessageId == null) item("meal-suggestions") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            ui.mealSuggestions.forEach { meal ->
                                MealSuggestionCard(meal, canSend) { onChooseMeal(meal.id) }
                            }
                        }
                    }
                    ui.revisionTarget?.let { target ->
                        item("revision") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Đang sửa đề xuất cho ${target.replaceFirstChar { it.lowercase() }}",
                                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                                KitchenOutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                    Text("Hủy đề xuất")
                                }
                            }
                        }
                    }
                    if (ui.canClearHistory) item("clear-history") {
                        TextButton(onClick = { confirmClear = true },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-clear")) { Text("Xóa lịch sử trò chuyện") }
                    }
                    if (ui.busy) item("busy") {
                        Row(
                            Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null, placementSpec = null),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Đang chuẩn bị câu trả lời…", style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                        }
                    }
                    if (ui.proposal.isNotEmpty()) item("proposal") {
                        OutlinedCard(
                            Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null, placementSpec = null)
                                .fillMaxWidth().testTag("assistant-preview"),
                            border = BorderStroke(1.dp, KitchenGreen),
                            colors = CardDefaults.outlinedCardColors(containerColor = KitchenSoft),
                        ) {
                            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Xem lại thay đổi", style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.semantics { heading() })
                                if (ui.proposalMeals.isEmpty()) ui.proposal.forEach { Text(it) } else {
                                    ui.proposalOther.forEach { Text(it) }
                                    ui.proposalMeals.forEach { group ->
                                        ProposalMealGroup(group, removable = ui.proposalMeals.size > 1) { onDropMeal(group) }
                                    }
                                    Text("Nguyên liệu và lịch nấu của các bữa này sẽ cập nhật theo món mới.",
                                        style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                                }
                                Text("Chỉ áp dụng sau khi bạn xác nhận.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                                KitchenButton(onClick = onConfirm,
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-confirm")) { Text("Xác nhận thay đổi") }
                                if (ui.canEditProposal) {
                                    KitchenOutlinedButton(onClick = onEdit,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-edit")) {
                                        Text("Sửa đề xuất")
                                    }
                                }
                                KitchenOutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Hủy") }
                            }
                        }
                    }
                    if (ui.error != null && ui.failedMessageId == null) item("error") {
                        AssistantError(ui.error, false, onRetry, onOffline)
                    }
                }
            }
            if (ui.canUndo) {
                KitchenOutlinedButton(onClick = onUndo,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).heightIn(min = 64.dp).testTag("assistant-undo")) {
                    Text("Hoàn tác thay đổi vừa rồi")
                }
            }
            // Keep this control outside the scroll viewport so it cannot cover proposal buttons.
            if (newMessages && scroll.canScrollForward) {
                KitchenTonalButton(onClick = { jumpToEnd++ },
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(8.dp).heightIn(min = 48.dp)) { Text("Tin nhắn mới") }
            }
            if (ui.voiceAvailable && !cramped) VoiceBar(ui, compact, onMic, onReadAgain, onStopSpeaking)
            // With the large microphone above, typing is secondary: keep the text box and Gửi on one row.
            AssistantComposer(ui, compact, cramped = cramped,
                inline = inlineComposer || (ui.voiceAvailable && !cramped),
                onQuestion = onQuestion, onSend = onSend, onProposal = { jumpToEnd++ })
        }
    }
}

/** The large microphone is the main way to talk; typing stays available below it. */
@Composable private fun VoiceBar(ui: AssistantUiState, compact: Boolean, onMic: () -> Unit, onReadAgain: () -> Unit, onStopSpeaking: () -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (ui.listening) {
                Text(ui.partial.ifBlank { "Đang nghe…" }, style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth().testTag("assistant-partial").semantics { liveRegion = LiveRegionMode.Polite })
            }
            ui.voiceNotice?.let {
                Text(it, color = KitchenMuted, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            val label = when {
                ui.listening -> "Đang nghe… Bấm để dừng"
                ui.proposal.isNotEmpty() -> "Bấm rồi nói “đồng ý” hoặc “không”"
                else -> "Bấm để nói"
            }
            val button = Modifier.fillMaxWidth().heightIn(min = 72.dp).testTag("assistant-mic").semantics {
                contentDescription = if (ui.listening) "Dừng nghe" else "Nói với trợ lý"
            }
            if (ui.listening) KitchenOutlinedButton(onClick = onMic, modifier = button) { Text(label, style = MaterialTheme.typography.titleMedium) }
            else KitchenButton(onClick = onMic, enabled = !ui.busy, modifier = button) { Text(label, style = MaterialTheme.typography.titleMedium) }
            if (!ui.micGranted && !compact) {
                Text("Lần đầu bấm, điện thoại sẽ hỏi quyền dùng micro để nghe bạn nói.",
                    style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
            if (ui.speaking) {
                TextButton(onClick = onStopSpeaking, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-stop-speaking")) { Text("Dừng đọc") }
            } else if (!compact && ui.messages.any { it.role == "assistant" }) {
                TextButton(onClick = onReadAgain, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-read-again")) { Text("Đọc lại câu trả lời") }
            }
        }
    }
}

@Composable private fun ProposalMealGroup(group: ProposalMealUi, removable: Boolean, onDrop: () -> Unit) {
    Surface(color = androidx.compose.ui.graphics.Color.White, shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth().testTag("assistant-proposal-${group.date}-${group.meal.value}")) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(group.title, style = MaterialTheme.typography.titleSmall, color = KitchenGreen,
                modifier = Modifier.semantics { heading() })
            group.lines.forEach { Text(it) }
            if (removable) {
                TextButton(onClick = onDrop, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Text("Giữ bữa này như cũ")
                }
            }
        }
    }
}

@Composable private fun MealSuggestionCard(meal: AssistantMealCard, enabled: Boolean, onChoose: () -> Unit) {
    OutlinedCard(
        Modifier.fillMaxWidth().testTag("assistant-meal-${meal.id}"),
        border = BorderStroke(1.dp, KitchenGreen),
        colors = CardDefaults.outlinedCardColors(containerColor = KitchenSoft),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Mâm ${meal.id}", style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.semantics { heading() })
            Text(meal.target, color = KitchenMuted)
            Text(meal.dishes.joinToString("\n"))
            Text("Nguyên liệu bạn đã nêu, lượng cần cho mâm: ${meal.available.joinToString(", ")}", style = MaterialTheme.typography.bodySmall)
            Text(
                if (meal.missing.isEmpty()) "Đã nhận ra đủ loại nguyên liệu."
                else "Cần thêm ${meal.missing.joinToString(", ")}",
                style = MaterialTheme.typography.bodySmall,
            )
            Text("Bạn kiểm tra lại lượng thực tế đang có trước khi nấu.", style = MaterialTheme.typography.bodySmall)
            KitchenOutlinedButton(
                onClick = onChoose,
                enabled = enabled,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-choose-meal-${meal.id}"),
            ) { Text("Dùng mâm ${meal.id}") }
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
            placeholder = {
                Text(when {
                    cramped -> "Câu hỏi của bạn"
                    ui.revisionTarget != null -> "Bạn muốn đổi món nào?"
                    else -> "Ví dụ: Món nào nấu nhanh?"
                })
            },
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
                contentDescription = "Gửi câu hỏi"
            }) {
            Text("Gửi")
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
                if (ui.proposal.isNotEmpty()) Text(if (ui.canEditProposal) "Xem, sửa hoặc xác nhận đề xuất ở trên."
                    else "Xác nhận hoặc hủy đề xuất để hỏi tiếp.",
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
            message.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = KitchenMuted) }
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
