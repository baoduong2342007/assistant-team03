@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.team03.bepnha

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable fun BepNhaApp(vm: PlannerViewModel, actions: NativeActions) {
    if (vm.showWelcome) { WelcomeFlow(vm); return }
    val snackbar = remember { SnackbarHostState() }
    val owner = LocalLifecycleOwner.current
    val screenStates = rememberSaveableStateHolder()
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(owner, vm, vm.state.timers.size) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { vm.visibleTick(); delay(if (vm.state.timers.isNotEmpty()) 1000L else 30_000L) }
        }
    }
    LaunchedEffect(vm.message) {
        vm.message?.let { message -> snackbar.showSnackbar(message); vm.clearMessage(message) }
    }
    LaunchedEffect(vm.effect) {
        val effect = vm.effect ?: return@LaunchedEffect
        vm.consumeEffect(effect)
        when (effect) {
            is PlannerEffect.Share -> actions.share(effect.text)
            is PlannerEffect.Calendar -> actions.calendar(effect.tasks)
            PlannerEffect.Notifications -> actions.notifications()
            PlannerEffect.ExactAlarms -> actions.exactAlarms()
        }
    }
    val cooking = vm.cookAlong != null
    val page = when {
        cooking -> PlannerPage.Cooking
        vm.assistantVisible -> PlannerPage.Chat
        else -> PlannerPage.ScreenPage(vm.screen)
    }
    val activeSheet = vm.sheet?.let { SheetPresentation(vm.sheetVersion, it) }
    var closingSheet by remember(page, vm.state.key, vm.notificationVersion) {
        mutableStateOf<SheetPresentation?>(null)
    }
    if (activeSheet != null) SideEffect { closingSheet = activeSheet }
    val shownSheet = activeSheet ?: closingSheet
    val menu = rememberSaveable(vm.notificationVersion) { mutableStateOf(false) }
    val timer = if (vm.assistantVisible || cooking) null else vm.timerUi()
    KitchenFrame(
        title = if (cooking) "Nấu cùng tôi" else if (vm.assistantVisible) "Hỏi Bếp Nhà" else if (vm.screen == Screen.TODAY) "Bếp Nhà" else vm.screen.title,
        selected = vm.screen,
        homeName = if (!cooking && !vm.assistantVisible && vm.screen == Screen.TODAY) vm.state.profile.name else null,
        homeMeal = vm.state.meal.title,
        assistant = vm.assistantVisible || cooking,
        showBack = cooking || vm.assistantVisible || vm.screen in listOf(Screen.FAMILY, Screen.FAVORITES),
        hideNavigation = imeOpen,
        menu = menu,
        padContent = false,
        timer = timer,
        extraTimers = if (!vm.assistantVisible && !cooking && vm.state.timers.size > 1) ({ TimerBar(vm) }) else null,
        onBack = { vm.back() }, onTab = vm::navigate, onAssistant = vm::openAssistant,
        onPantry = { vm.navigate(Screen.ASK) }, onFamily = { vm.navigate(Screen.FAMILY) }, onReminder = vm::openReminders,
        onFavorites = { vm.navigate(Screen.FAVORITES) }, onCancelTimer = { timer?.let { vm.cancelTimer(it.id) } },
        snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        PlannerMotionHost(page, vm.backDestination(), shownSheet == null && !imeOpen && !menu.value,
            onBack = { vm.back() }, screenStates = screenStates, contentPadding = padding) { destination, active ->
            when (destination) {
                PlannerPage.Cooking -> CookAlongScreen(vm, active)
                PlannerPage.Chat -> AssistantScreen(vm, active)
                is PlannerPage.ScreenPage -> when (destination.screen) {
                    Screen.TODAY -> TodayScreen(vm)
                    Screen.PLAN -> WeekScreen(vm)
                    Screen.SHOPPING -> ShoppingScreen(vm, actions)
                    Screen.COOK -> CookScreen(vm, actions)
                    Screen.ASK -> AskScreen(vm, actions)
                    Screen.FAMILY -> FamilyScreen(vm, actions)
                    Screen.FAVORITES -> FavoritesScreen(vm)
                }
            }
        }
    }
    if (shownSheet != null) key(shownSheet.version) {
        val open = activeSheet == shownSheet
        val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        LaunchedEffect(open) {
            if (!open) {
                sheetState.hide()
                if (closingSheet == shownSheet) closingSheet = null
            }
        }
        ModalBottomSheet(onDismissRequest = {
            if (vm.sheetVersion == shownSheet.version && vm.sheet != null) vm.dismissSheet()
        }, containerColor = KitchenSurface, sheetMaxWidth = 680.dp, sheetState = sheetState) {
            Column(Modifier.inactiveContent(!open)) {
                when (val sheet = shownSheet.content) {
                    is PlannerSheet.RecipeDetail -> RecipeSheet(vm, sheet.id, actions)
                    is PlannerSheet.Swap -> SwapSheet(vm, sheet.index)
                    PlannerSheet.Reminder -> ReminderSheet(vm, actions)
                }
            }
        }
    }
}

private data class SheetPresentation(val version: Int, val content: PlannerSheet)

/** The same scaffold is used by device screens and full-screen design previews. */
@Composable fun KitchenFrame(
    title: String, selected: Screen = Screen.TODAY, assistant: Boolean = false, homeName: String? = null, homeMeal: String = "bữa ăn",
    showBack: Boolean = false, hideNavigation: Boolean = false, timer: KitchenTimerUi? = null,
    onBack: () -> Unit = {}, onTab: (Screen) -> Unit = {}, onAssistant: () -> Unit = {},
    onPantry: () -> Unit = {}, onFamily: () -> Unit = {}, onReminder: () -> Unit = {}, onFavorites: () -> Unit = {},
    onCancelTimer: () -> Unit = {}, extraTimers: (@Composable () -> Unit)? = null, snackbarHost: @Composable () -> Unit = {},
    menu: MutableState<Boolean> = rememberSaveable { mutableStateOf(false) },
    padContent: Boolean = true,
    content: @Composable (PaddingValues) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compactKeyboard = assistant && maxHeight < 480.dp && WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(modifier = Modifier.testTag("native-root"), containerColor = if (homeName != null) Color.White else KitchenPaper,
        topBar = {
            if (!compactKeyboard) Surface(color = if (homeName != null) KitchenGreen else if (assistant) KitchenSurface else KitchenPaper,
                // Home: the dashboard draws the header's rounded bottom behind its search card.
                shape = RoundedCornerShape(0.dp)) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (showBack) IconButton(onClick = onBack,
                        colors = IconButtonDefaults.iconButtonColors(containerColor = if (assistant) KitchenSoft.copy(alpha = 0.6f) else Color.Transparent)) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại", Modifier.size(20.dp))
                    }
                    if (homeName != null) {
                        Column(Modifier.weight(1f).padding(horizontal = 12.dp, vertical = 12.dp).testTag("home-greeting")) {
                            val hour = java.time.LocalTime.now().hour
                            Text((when (hour) { in 5..10 -> "Chào buổi sáng"; in 11..13 -> "Chào buổi trưa"; in 14..17 -> "Chào buổi chiều"; else -> "Chào buổi tối" }) + ", $homeName",
                                style = MaterialTheme.typography.bodyMedium, color = KitchenOnGreen)
                            Spacer(Modifier.height(4.dp))
                            Text("Cùng chuẩn bị ${homeMeal.lowercase(PlannerEngine.locale)}", style = KitchenHeroTitle, color = Color.White)
                        }
                        IconButton(onClick = onReminder, colors = IconButtonDefaults.iconButtonColors(containerColor = KitchenSurface)) {
                            Icon(Icons.Outlined.Notifications, "Nhắc nấu", tint = KitchenGreen)
                        }
                    } else {
                    Column(Modifier.weight(1f).padding(horizontal = 8.dp)) {
                    // Screen titles sit one step above the section headings (titleLarge) inside the screen.
                    Text(title, style = if (assistant) MaterialTheme.typography.titleLarge else KitchenHeroTitle,
                        fontFamily = if (title == "Bếp Nhà") androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.SansSerif,
                        modifier = Modifier.semantics { heading() }, color = KitchenGreen)
                    if (title == "Hỏi Bếp Nhà") Text("Trợ lý cho bữa cơm nhà", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                    }
                    }
                    if (!assistant) Box {
                        if (homeName != null) IconButton(onClick = { menu.value = true }, modifier = Modifier.testTag("more-menu")) {
                            Icon(Icons.Outlined.MoreVert, "Thêm", tint = Color.White)
                        } else IconButton(onClick = { menu.value = true }, modifier = Modifier.testTag("more-menu")) {
                            Icon(Icons.Outlined.MoreVert, "Thêm", tint = KitchenInk)
                        }
                        DropdownMenu(expanded = menu.value, onDismissRequest = { menu.value = false }) {
                            DropdownMenuItem(text = { Text("Tủ lạnh & gợi ý món") }, onClick = { menu.value = false; onPantry() },
                                modifier = Modifier.heightIn(min = 56.dp).testTag("tab-ask"))
                            DropdownMenuItem(text = { Text("Gia đình") }, onClick = { menu.value = false; onFamily() },
                                modifier = Modifier.heightIn(min = 56.dp).semantics { contentDescription = "Thông tin gia đình" })
                            DropdownMenuItem(text = { Text("Nhắc nấu") }, onClick = { menu.value = false; onReminder() },
                                modifier = Modifier.heightIn(min = 56.dp))
                            DropdownMenuItem(text = { Text("Món đã lưu") }, onClick = { menu.value = false; onFavorites() },
                                modifier = Modifier.heightIn(min = 56.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!assistant && !hideNavigation) {
                Surface(color = KitchenPaper) {
                    Column(Modifier.navigationBarsPadding()) {
                        if (extraTimers != null) extraTimers() else timer?.let { KitchenTimerSummary(it, onCancelTimer) }
                        KitchenNavigation(selected, onTab, onAssistant)
                    }
                }
            }
        },
        snackbarHost = snackbarHost,
    ) { padding ->
        if (padContent) {
            Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { content(padding) }
        } else {
            content(padding)
        }
    }
    }
}

@Composable fun KitchenCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier.fillMaxWidth(), shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, KitchenLine),
        colors = CardDefaults.outlinedCardColors(containerColor = KitchenSurface)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineMedium, modifier = Modifier.semantics { heading() })
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = KitchenMuted) }
    }
}
@Composable fun MealSelector(vm: PlannerViewModel) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.25f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                MealTabs(vm, Modifier.fillMaxWidth())
                Box(Modifier.align(Alignment.End)) { PeopleCount(vm) }
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
            MealTabs(vm, Modifier.weight(1f))
            PeopleCount(vm)
        }
    }
}

@Composable private fun MealTabs(vm: PlannerViewModel, modifier: Modifier) {
    KitchenSegmented(Meal.entries.map { it.title }, vm.state.meal.ordinal, { vm.selectMeal(Meal.entries[it]) }, modifier,
        tag = { "meal-${Meal.entries[it].value}" })
}

@Composable private fun PeopleCount(vm: PlannerViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        KitchenIcon(KitchenGlyph.PEOPLE, Modifier.size(17.dp), KitchenMuted)
        Text("${vm.state.profile.people} người", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
    }
}
/** Picks the day and doubles as the week's menu at a glance for the selected meal. */
@Composable fun WeekStrip(vm: PlannerViewModel) {
    val scroll = rememberLazyListState()
    val fontScale = LocalDensity.current.fontScale.coerceIn(1f, 1.5f)
    LaunchedEffect(vm.state.date, vm.days) {
        val index = vm.days.indexOf(vm.state.date)
        val layout = scroll.layoutInfo
        val fullyVisible = layout.visibleItemsInfo.any {
            it.index == index && it.offset >= layout.viewportStartOffset &&
                it.offset + it.size <= layout.viewportEndOffset
        }
        if (index >= 0 && !fullyVisible) scroll.animateScrollToItem(index)
    }
    LazyRow(
        modifier = Modifier.fillMaxWidth().selectableGroup(),
        state = scroll,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 20.dp),
    ) {
        items(vm.days, key = { it.toString() }) { day ->
            val selected = day == vm.state.date
            val dishes = vm.engine.menu(vm.state, day)
            val label = if (day == vm.today) "Hôm nay" else "${PlannerEngine.weekdayShort(day)} · ${PlannerEngine.shortDate(day)}"
            Column(
                modifier = Modifier.width((116 * fontScale).dp).clip(MaterialTheme.shapes.medium)
                    .background(if (selected) KitchenGreen else KitchenSurface)
                    .border(1.dp, if (selected) KitchenGreen else KitchenLine, MaterialTheme.shapes.medium)
                    .selectable(selected = selected, role = Role.Tab, onClick = { vm.selectDate(day) })
                    .testTag("date-$day")
                    .semantics { contentDescription = "${PlannerEngine.fullDate(day)}: ${dishes.joinToString { it.name }}" }
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1,
                    color = if (selected) KitchenGold else KitchenMuted)
                Text(dishes.joinToString(" ") { it.emoji }, fontSize = 18.sp, lineHeight = 26.sp, maxLines = 1, softWrap = false)
                Text(dishes.first().name, style = MaterialTheme.typography.labelLarge, minLines = 2, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, color = if (selected) Color.White else KitchenInk)
            }
        }
    }
}

@Composable fun TodayScreen(vm: PlannerViewModel) { TodayDashboard(vm) }

@Composable fun MealOverview(vm: PlannerViewModel, modifier: Modifier = Modifier) {
    val menu = vm.menu
    val cost = PlannerEngine.money(vm.engine.cost(menu, vm.state.profile.people)).replace(' ', '\u00A0')
    val duration = "${vm.engine.minutes(menu)} phút"
    Surface(modifier.fillMaxWidth(), color = KitchenGreen, contentColor = Color.White, shape = MaterialTheme.shapes.extraLarge) {
        Column {
            KitchenContentTransition(menu, "meal photo", Modifier.fillMaxWidth().height(144.dp)) { recipes ->
                MealPhoto(recipes, Modifier.fillMaxSize())
            }
            Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                BoxWithConstraints(Modifier.fillMaxWidth()) {
                    val dividerColor = KitchenOnGreen.copy(alpha = 0.27f)
                    if (maxWidth < 235.dp || LocalDensity.current.fontScale > 1.4f) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            MealMetric("Chi phí ước tính", cost)
                            HorizontalDivider(color = dividerColor)
                            MealMetric("Thời gian nấu", duration)
                        }
                    } else {
                        Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                            MealMetric("Chi phí ước tính", cost, Modifier.weight(1.2f))
                            VerticalDivider(Modifier.fillMaxHeight(), color = dividerColor)
                            MealMetric("Thời gian nấu", duration, Modifier.weight(1f))
                        }
                    }
                }
                Text(if (vm.state.meal.tray) "Chưa tính gạo và gia vị có sẵn." else "Chưa tính gia vị có sẵn.", style = MaterialTheme.typography.bodySmall, color = KitchenOnGreen)
            }
        }
    }
}

@Composable private fun MealMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KitchenOnGreen)
        KitchenContentTransition(value, "meal metric") { metric ->
            Text(metric, style = MaterialTheme.typography.titleLarge)
        }
    }
}

@Composable fun RecipeCard(vm: PlannerViewModel, recipe: Recipe, index: Int? = null) {
    val openInteraction = remember { MutableInteractionSource() }
    val swapInteraction = remember { MutableInteractionSource() }
    val saveInteraction = remember { MutableInteractionSource() }
    KitchenCard(Modifier.testTag("recipe-${recipe.id}").pressCompression(openInteraction, swapInteraction, saveInteraction)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecipePortrait(recipe, 76.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                RecipeKindLabel(recipe.kind)
                Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    KitchenIcon(KitchenGlyph.CLOCK, Modifier.size(14.dp), KitchenMuted)
                    Text("${recipe.minutes} phút", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                }
            }
            val saved = recipe.id in vm.state.favorites
            FilledTonalIconButton(
                onClick = { vm.toggleFavorite(recipe.id) },
                interactionSource = saveInteraction,
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (saved) KitchenPeach else KitchenPaper,
                    contentColor = if (saved) KitchenTerracotta else KitchenMuted,
                ),
            ) {
                KitchenContentTransition(saved, "favorite icon") { selected ->
                    Icon(if (selected) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                        if (selected) "Bỏ lưu ${recipe.name}" else "Lưu ${recipe.name}", modifier = Modifier.size(20.dp))
                }
            }
        }
        HorizontalDivider(color = KitchenLine)
        val openRecipe: @Composable (Modifier) -> Unit = { modifier ->
            TextButton(onClick = { vm.openRecipe(recipe.id) },
                interactionSource = openInteraction,
                modifier = modifier.heightIn(min = 48.dp).testTag("open-${recipe.id}")) {
                Text("Xem công thức")
            }
        }
        if (index == null) {
            openRecipe(Modifier.fillMaxWidth())
        } else {
            KitchenActions(first = openRecipe, second = { modifier ->
                OutlinedButton(
                    onClick = { vm.openSwap(index) },
                    interactionSource = swapInteraction,
                    modifier = modifier.heightIn(min = 48.dp).testTag("swap-${recipe.id}")
                        .semantics { contentDescription = "Đổi món ${recipe.name}" },
                    border = BorderStroke(1.dp, KitchenLine),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Refresh, null, Modifier.size(17.dp))
                    Spacer(Modifier.width(5.dp))
                    Text("Đổi món")
                }
            })
        }
    }
}

/** Entry to the key feature: tell Bếp Nhà what is in the fridge and get a full meal. */
@Composable fun FridgePromo(vm: PlannerViewModel) {
    Surface(
        onClick = { vm.openAsk(AskTab.FRIDGE) },
        modifier = Modifier.fillMaxWidth().testTag("fridge-promo"),
        color = KitchenPeach,
        contentColor = KitchenInk,
        shape = MaterialTheme.shapes.large,
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(44.dp).background(KitchenTerracotta, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                    KitchenIcon(KitchenGlyph.FRIDGE, tint = Color.White)
                }
                Column(Modifier.weight(1f)) {
                    Text("Tủ lạnh còn gì?", style = MaterialTheme.typography.titleMedium)
                    Text("Chọn nguyên liệu có sẵn, Bếp Nhà gợi ý cả mâm cơm trưa hoặc tối.",
                        style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                }
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp), tint = KitchenTerracotta)
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("Thịt heo", "Trứng", "Cá", "Đậu hũ", "Rau").forEach { item ->
                    SuggestionChip(onClick = { vm.openFridgeWith(item) }, label = { Text(item) },
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = KitchenSurface, labelColor = KitchenTerracotta),
                        border = BorderStroke(1.dp, KitchenTerracotta.copy(alpha = 0.25f)))
                }
            }
        }
    }
}

/** The whole week for one meal; the "Cả tuần" tab of [WeekScreen]. */
@Composable fun WeekOverview(vm: PlannerViewModel, modifier: Modifier = Modifier) {
    LazyColumn(modifier.fillMaxSize().testTag("week-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { MealSelector(vm) }
        items(vm.days, key = { it.toString() }) { day ->
            val menu = vm.engine.menu(vm.state, day)
            val isToday = day == vm.today
            KitchenCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        color = if (isToday) KitchenTerracotta else KitchenSoft,
                        contentColor = if (isToday) Color.White else KitchenGreen,
                        shape = MaterialTheme.shapes.small,
                    ) {
                        Icon(Icons.Outlined.DateRange, null, Modifier.padding(11.dp).size(20.dp))
                    }
                    Column(Modifier.weight(1f)) {
                        Text(PlannerEngine.fullDate(day), style = MaterialTheme.typography.titleMedium)
                        if (isToday) Text("Hôm nay", style = MaterialTheme.typography.labelMedium, color = KitchenTerracotta)
                    }
                }
                HorizontalDivider(color = KitchenLine)
                KitchenContentTransition(menu, "weekly dishes", Modifier.fillMaxWidth()) { dishes ->
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        dishes.forEach { recipe ->
                            Row(
                                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small)
                                    .clickable(role = Role.Button) { vm.openRecipe(recipe.id) }.heightIn(min = 52.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                RecipePortrait(recipe, 46.dp)
                                Column(Modifier.weight(1f)) {
                                    Text(recipe.name, style = MaterialTheme.typography.labelLarge)
                                    Text(recipe.kind.label, style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                                }
                                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp), tint = KitchenMuted)
                            }
                        }
                    }
                }
                HorizontalDivider(color = KitchenLine)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Column(Modifier.weight(1f)) {
                        Text("Chi phí ước tính", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                        Text(PlannerEngine.money(vm.engine.cost(menu, vm.state.profile.people)), style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                    }
                    TextButton(
                        onClick = { vm.selectDate(day); vm.navigate(Screen.TODAY) },
                        contentPadding = PaddingValues(horizontal = 6.dp),
                    ) { Text("Xem và đổi món") }
                }
            }
        }
    }
}

@Composable fun FavoritesScreen(vm: PlannerViewModel) {
    val favorites = vm.state.favorites.mapNotNull(vm.engine.recipes::get)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (favorites.isEmpty()) item(key = "empty-favorites") {
            KitchenCard(Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null)) {
                Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(40.dp), tint = KitchenGreen)
                Text("Chưa có món đã lưu", style = MaterialTheme.typography.titleMedium)
                Text("Chạm vào trái tim bên cạnh món bạn muốn tìm lại.", color = KitchenMuted)
                Button(onClick = { vm.navigate(Screen.TODAY) }) { Text("Xem thực đơn") }
            }
        }
        items(favorites, key = { it.id }) { recipe ->
            Box(Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null)) {
                RecipeCard(vm, recipe)
            }
        }
    }
}

/** Every running timer stays visible, so two dishes can be watched while doing something else. */
@Composable fun TimerBar(vm: PlannerViewModel) {
    val timers = vm.state.timers.sortedBy { it.end }
    if (timers.isEmpty()) return
    Surface(color = KitchenGreen, contentColor = Color.White) {
        if (timers.size == 1) TimerChip(vm, timers[0], Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), first = true)
        else LazyRow(Modifier.fillMaxWidth().testTag("timer-bar"), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(timers, key = { _, timer -> timer.id }) { index, timer ->
                TimerChip(vm, timer,
                    Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null)
                        .width(184.dp).clip(MaterialTheme.shapes.small).background(Color.White.copy(alpha = 0.1f)),
                    first = index == 0,
                )
            }
        }
    }
}

@Composable private fun TimerChip(vm: PlannerViewModel, timer: CookingTimer, modifier: Modifier, first: Boolean) {
    val seconds = ((timer.end - vm.now + 999) / 1000).coerceAtLeast(0)
    Row(modifier.padding(start = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(timer.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, color = KitchenOnGreen)
            Text("%02d:%02d".format(seconds / 60, seconds % 60), fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                modifier = if (first) Modifier.testTag("timer-countdown") else Modifier)
        }
        IconButton(onClick = { vm.cancelTimer(timer.id) }) { Icon(Icons.Outlined.Close, "Dừng hẹn giờ ${timer.label}") }
    }
}
