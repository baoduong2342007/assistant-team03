package vn.team03.bepnha

import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.ui.graphics.Color
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
    val motion = rememberMotionEnabled()
    var followingLatest by rememberSaveable { mutableStateOf(true) }
    var previousCount by rememberSaveable { mutableIntStateOf(ui.messages.size) }
    var newMessages by rememberSaveable { mutableStateOf(false) }
    var jumpToEnd by remember { mutableIntStateOf(0) }
    val welcome = ui.messages.all { it.id.startsWith("greeting-") } && !ui.busy &&
        ui.proposal.isEmpty() && ui.error == null && ui.revisionTarget == null && ui.mealSuggestions.isEmpty()
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
            scroll.scrollToItem(scroll.layoutInfo.totalItemsCount - 1)
            followingLatest = true
            newMessages = false
        } else if (added || ui.proposal.isNotEmpty() || ui.error != null || ui.revisionTarget != null) newMessages = true
    }
    LaunchedEffect(jumpToEnd) {
        if (!active) return@LaunchedEffect
        if (jumpToEnd > 0 && scroll.layoutInfo.totalItemsCount > 0) {
            val last = scroll.layoutInfo.totalItemsCount - 1
            if (motion) scroll.animateScrollToItem(last) else scroll.scrollToItem(last)
            followingLatest = true
            newMessages = false
        }
    }
    BoxWithConstraints(Modifier.fillMaxSize().background(KitchenSurface).imePadding().testTag("assistant-screen")) {
        val compact = maxHeight < 360.dp || androidx.compose.ui.platform.LocalDensity.current.fontScale >= 1.5f ||
            (ui.timer != null && maxHeight < 640.dp)
        val cramped = maxHeight < 240.dp
        val inlineComposer = (maxHeight < 360.dp && maxWidth >= 480.dp) || (!compact && maxWidth >= 340.dp)
        val heroSize = if (compact) 160.dp else (maxHeight * 0.43f).coerceIn(200.dp, 300.dp)
        Column(Modifier.fillMaxSize()) {
            if (!compact) {
                AssistantContext(ui, onClear = { confirmClear = true })
                ui.timer?.let { KitchenTimer(it, onCancelTimer) }
            }
            Box(Modifier.weight(1f).fillMaxWidth()) {
                LazyColumn(
                    Modifier.fillMaxSize().testTag("assistant-history"), state = scroll,
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                ) {
                    if (compact) {
                        item("context") { AssistantContext(ui, compact = true, onClear = { confirmClear = true }) }
                        ui.timer?.let { timer -> item("timer") { KitchenTimer(timer, onCancelTimer) } }
                    }
                    if (welcome) item("welcome") {
                        AssistantWelcome(ui.suggestions, canSend, onQuestion, heroSize)
                    }
                    items(if (welcome) emptyList() else ui.messages, key = { it.id }) { message ->
                        Column(
                            Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null, placementSpec = null),
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            AssistantMessage(message)
                            if (message.id == ui.mealSuggestionMessageId) {
                                ui.mealSuggestions.forEach { meal -> MealSuggestionCard(meal, canSend) { onChooseMeal(meal.id) } }
                            }
                            val recipes = message.recipeIds.filter { it in ui.recipeNames }
                            if (recipes.isNotEmpty()) FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                recipes.forEach { id ->
                                    OutlinedButton(onClick = { onRecipe(id) }, shape = RoundedCornerShape(14.dp),
                                        border = BorderStroke(1.dp, KitchenLine),
                                        modifier = Modifier.heightIn(min = 48.dp)) {
                                        Text("Xem ${ui.recipeNames.getValue(id)}", style = MaterialTheme.typography.labelLarge)
                                    }
                                }
                            }
                            if (ui.failedMessageId == message.id && ui.error != null) {
                                AssistantError(ui.error, ui.canRetry, onRetry, onOffline)
                            }
                        }
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
                    if (ui.busy) item("busy") {
                        Row(
                            Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null, placementSpec = null),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                            Text("Bếp Nhà đang chuẩn bị câu trả lời…", style = MaterialTheme.typography.bodyMedium,
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
                                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-confirm")) {
                                    Text(if (ui.proposalMeals.isNotEmpty() && ui.proposalOther.isEmpty()) "Đổi món" else "Xác nhận thay đổi")
                                }
                                if (ui.canEditProposal) {
                                    KitchenOutlinedButton(onClick = onEdit,
                                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("assistant-edit")) {
                                        Text("Sửa đề xuất")
                                    }
                                }
                                KitchenOutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                                    Text(if (ui.proposalMeals.isNotEmpty() && ui.proposalOther.isEmpty()) "Giữ món cũ" else "Hủy")
                                }
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
            if (ui.voiceAvailable && !cramped && (ui.listening || ui.voiceNotice != null || ui.speaking || !welcome))
                VoiceBar(ui, compact, onReadAgain, onStopSpeaking)
            // Use one composer row on regular phones; stack actions when large text needs more room.
            AssistantComposer(ui, compact, cramped = cramped,
                inline = inlineComposer,
                onQuestion = onQuestion, onSend = onSend, onMic = onMic, onProposal = { jumpToEnd++ })
        }
    }
}

/** Voice actions stay within reach without taking over the conversation. */
@Composable private fun VoiceBar(ui: AssistantUiState, compact: Boolean, onReadAgain: () -> Unit, onStopSpeaking: () -> Unit) {
    Surface(color = KitchenSurface) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (ui.listening) {
                Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
                    Text(ui.partial.ifBlank { "Đang nghe bạn nói…" },
                        Modifier.fillMaxWidth().padding(14.dp).testTag("assistant-partial")
                            .semantics { liveRegion = LiveRegionMode.Polite },
                        style = MaterialTheme.typography.bodyMedium)
                }
            }
            ui.voiceNotice?.let {
                Text(it, color = KitchenMuted, style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite })
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (ui.speaking) {
                    TextButton(onClick = onStopSpeaking,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("assistant-stop-speaking")) { Text("Dừng đọc") }
                } else if (!compact && ui.messages.any { it.role == "assistant" }) {
                    TextButton(onClick = onReadAgain,
                        modifier = Modifier.heightIn(min = 48.dp).testTag("assistant-read-again")) { Text("Đọc lại câu trả lời") }
                }
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
    onQuestion: (String) -> Unit, onSend: () -> Unit, onMic: () -> Unit, onProposal: () -> Unit,
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
            shape = RoundedCornerShape(28.dp),
            trailingIcon = if (ui.voiceAvailable) ({
                IconButton(onClick = onMic, enabled = !ui.busy,
                    modifier = Modifier.testTag("assistant-mic")) {
                    KitchenIcon(KitchenGlyph.MIC, tint = if (ui.listening) KitchenTerracotta else KitchenGreen,
                        modifier = Modifier.semantics { contentDescription = if (ui.listening) "Dừng nghe" else "Nói với trợ lý" })
                }
            }) else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = Color.White, unfocusedContainerColor = Color.White,
                unfocusedBorderColor = KitchenLine, focusedBorderColor = KitchenGreen,
            ),
            placeholder = {
                Text(when {
                    cramped -> "Câu hỏi của bạn"
                    ui.revisionTarget != null -> "Bạn muốn đổi món nào?"
                    else -> "Hỏi Bếp Nhà…"
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
            if (!compact) {
                Spacer(Modifier.width(6.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
    Surface(color = MaterialTheme.colorScheme.surface) {
        if (cramped && ui.proposal.isNotEmpty()) {
            KitchenButton(onClick = onProposal, modifier = Modifier.fillMaxWidth().padding(8.dp)) { Text("Xem thay đổi") }
        } else if (inline) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                input(Modifier.weight(1f))
                send(Modifier.widthIn(min = 80.dp, max = 128.dp))
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

@Composable private fun AssistantContext(ui: AssistantUiState, compact: Boolean = false, onClear: () -> Unit) {
    var expanded by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().padding(horizontal = if (compact) 0.dp else 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = { expanded = !expanded }, contentPadding = PaddingValues(horizontal = 4.dp),
                modifier = Modifier.weight(1f).heightIn(min = 48.dp).testTag("assistant-source-info")) {
                KitchenIcon(KitchenGlyph.BOWL, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(ui.context, style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                Spacer(Modifier.width(8.dp))
                Text(if (expanded) "−" else "+", color = KitchenMuted)
            }
            if (ui.canClearHistory) AssistantHistoryMenu(onClear)
            else IconButton(onClick = { expanded = !expanded }) {
                Icon(Icons.Outlined.MoreVert, "Thông tin trợ lý", tint = KitchenMuted)
            }
        }
        if (expanded) Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(ui.date, style = MaterialTheme.typography.labelLarge, color = KitchenGreen)
                ui.recipe?.let { Text("Đang hỏi về: $it", style = MaterialTheme.typography.bodySmall) }
                Text(if (ui.configured) "Khi dùng AI, câu hỏi và ngữ cảnh bữa ăn được gửi đến máy chủ. Hãy xem lại các gợi ý."
                    else "Đang dùng trợ lý trên máy với ${ui.recipeNames.size} công thức có sẵn. Bạn có thể tra cứu, gợi ý mâm và đề xuất đổi món.",
                    style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
        }
    }
}

@Composable private fun AssistantHistoryMenu(onClear: () -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { expanded = true }, modifier = Modifier.testTag("assistant-options")) {
            Icon(Icons.Outlined.MoreVert, contentDescription = "Tùy chọn trò chuyện", tint = KitchenMuted)
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            DropdownMenuItem(text = { Text("Xóa lịch sử trò chuyện") },
                onClick = { expanded = false; onClear() }, modifier = Modifier.testTag("assistant-clear"))
        }
    }
}

@Composable private fun AssistantWelcome(
    suggestions: List<String>, enabled: Boolean, onSelect: (String) -> Unit,
    heroSize: androidx.compose.ui.unit.Dp,
) {
    val prompts = (suggestions + "Còn thiếu nguyên liệu gì?").distinct().take(4)
    Column(Modifier.fillMaxWidth().padding(bottom = 24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Image(painterResource(R.drawable.assistant_hero), contentDescription = null,
            modifier = Modifier.size(heroSize).testTag("assistant-hero"), contentScale = ContentScale.Fit)
        Text("Hôm nay bạn muốn ăn gì?", style = MaterialTheme.typography.titleLarge,
            color = KitchenGreen, textAlign = TextAlign.Center, modifier = Modifier.semantics { heading() })
        Text("Để Bếp Nhà cùng bạn chuẩn bị.", style = MaterialTheme.typography.bodyMedium,
            color = KitchenMuted, textAlign = TextAlign.Center)
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalArrangement = Arrangement.spacedBy(4.dp), maxItemsInEachRow = 2) {
            prompts.forEachIndexed { index, prompt ->
                val label = when {
                    prompt.contains("tủ lạnh", ignoreCase = true) -> "Từ tủ lạnh"
                    prompt.contains("thiếu nguyên liệu", ignoreCase = true) -> "Đi chợ"
                    prompt.contains("nấu món nào trước", ignoreCase = true) -> "Thứ tự nấu"
                    prompt.contains("20 phút", ignoreCase = true) -> "Nấu nhanh"
                    prompt.length <= 18 -> prompt
                    prompt.contains("tuần") -> "Lên thực đơn"
                    prompt.contains("nguyên liệu", ignoreCase = true) -> "Nguyên liệu cần có"
                    prompt.contains("canh") -> "Gợi ý món canh"
                    else -> "Gợi ý nấu ăn"
                }
                val glyph = when (index) { 0 -> KitchenGlyph.BOWL; 1 -> KitchenGlyph.CLOCK; 2 -> KitchenGlyph.SPARKLE; else -> KitchenGlyph.FRIDGE }
                FilledTonalButton(onClick = { onSelect(prompt) }, enabled = enabled,
                    shape = CircleShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
                    modifier = Modifier.heightIn(min = 48.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = KitchenSoft.copy(alpha = 0.65f), contentColor = KitchenGreen)) {
                    KitchenIcon(glyph, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(label, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable private fun AssistantMessage(message: AssistantChatMessage) {
    val user = message.role == "user"
    Column(Modifier.fillMaxWidth().padding(start = if (user) 28.dp else 0.dp, end = if (user) 0.dp else 12.dp),
        horizontalAlignment = if (user) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            if (!user) KitchenIcon(KitchenGlyph.SPARKLE, Modifier.size(16.dp))
            Text(if (user) "Bạn" else when (message.source) {
                AssistantSource.LOCAL -> "Tra cứu trên máy"
                AssistantSource.SYSTEM -> "Bếp Nhà"
                AssistantSource.AI -> "Trợ lý Bếp Nhà"
            }, style = MaterialTheme.typography.labelMedium, color = KitchenMuted)
        }
        Surface(
            color = if (user) KitchenGreen else Color.White,
            contentColor = if (user) Color.White else KitchenInk,
            shape = if (user) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(6.dp, 20.dp, 20.dp, 20.dp),
            border = if (user) null else BorderStroke(1.dp, KitchenLine.copy(alpha = 0.7f)),
        ) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                message.notice?.let { Text(it, style = MaterialTheme.typography.bodySmall,
                    color = if (user) KitchenOnGreen else KitchenMuted) }
                SelectionContainer {
                    Text(message.text, style = MaterialTheme.typography.bodyLarge,
                        modifier = if (user) Modifier else Modifier.semantics { liveRegion = LiveRegionMode.Polite })
                }
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
