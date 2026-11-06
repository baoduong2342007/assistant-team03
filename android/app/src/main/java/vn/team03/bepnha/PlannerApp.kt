@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package vn.team03.bepnha

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable fun BepNhaApp(vm: PlannerViewModel, actions: NativeActions) {
    val snackbar = remember { SnackbarHostState() }
    val owner = LocalLifecycleOwner.current
    val screenStates = rememberSaveableStateHolder()
    val imeOpen = WindowInsets.ime.getBottom(LocalDensity.current) > 0
    LaunchedEffect(owner, vm) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { vm.visibleTick(); delay(1000) }
        }
    }
    LaunchedEffect(vm.message) {
        vm.message?.let { message -> snackbar.showSnackbar(message); vm.clearMessage(message) }
    }
    BackHandler(enabled = vm.sheet == null && (vm.assistantVisible || vm.screen != Screen.TODAY)) { vm.back() }
    KitchenFrame(
        title = if (vm.assistantVisible) "Hỏi Bếp Nhà" else if (vm.screen == Screen.TODAY) "Bếp Nhà" else vm.screen.title,
        selected = vm.screen,
        assistant = vm.assistantVisible,
        showBack = vm.assistantVisible || vm.screen in listOf(Screen.FAMILY, Screen.FAVORITES),
        hideNavigation = imeOpen,
        timer = if (vm.assistantVisible) null else vm.timerUi(),
        onBack = { vm.back() }, onTab = vm::navigate, onAssistant = vm::openAssistant,
        onFamily = { vm.navigate(Screen.FAMILY) }, onReminder = vm::openReminders,
        onFavorites = { vm.navigate(Screen.FAVORITES) }, onCancelTimer = vm::cancelTimer,
        snackbarHost = { SnackbarHost(snackbar) },
    ) {
        screenStates.SaveableStateProvider(if (vm.assistantVisible) "assistant" else vm.screen.name) {
            if (vm.assistantVisible) AssistantScreen(vm) else when (vm.screen) {
                Screen.TODAY -> TodayScreen(vm)
                Screen.PLAN -> WeekScreen(vm)
                Screen.SHOPPING -> ShoppingScreen(vm, actions)
                Screen.COOK -> CookScreen(vm, actions)
                Screen.FAMILY -> FamilyScreen(vm, actions)
                Screen.FAVORITES -> FavoritesScreen(vm)
            }
        }
    }
    vm.sheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = vm::dismissSheet, containerColor = KitchenPaper,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            when (sheet) {
                is PlannerSheet.RecipeDetail -> RecipeSheet(vm, sheet.id, actions)
                is PlannerSheet.Swap -> SwapSheet(vm, sheet.index)
                PlannerSheet.Reminder -> ReminderSheet(vm, actions)
            }
        }
    }
    vm.pendingTimer?.let { pending ->
        AlertDialog(onDismissRequest = vm::dismissTimerReplacement,
            title = { Text("Đang có một hẹn giờ") },
            text = { Text("Bạn muốn thay đồng hồ đang chạy bằng hẹn ${pending.first} phút cho ${pending.second.lowercase()}?") },
            confirmButton = { TextButton(onClick = {
                vm.startTimer(pending.first, pending.second, replace = true)
                if (!vm.notifications) actions.notifications()
            }) { Text("Thay hẹn giờ") } },
            dismissButton = { TextButton(onClick = vm::dismissTimerReplacement) { Text("Giữ đồng hồ cũ") } })
    }
}

/** The same scaffold is used by device screens and full-screen design previews. */
@Composable fun KitchenFrame(
    title: String, selected: Screen = Screen.TODAY, assistant: Boolean = false,
    showBack: Boolean = false, hideNavigation: Boolean = false, timer: KitchenTimerUi? = null,
    onBack: () -> Unit = {}, onTab: (Screen) -> Unit = {}, onAssistant: () -> Unit = {},
    onFamily: () -> Unit = {}, onReminder: () -> Unit = {}, onFavorites: () -> Unit = {},
    onCancelTimer: () -> Unit = {}, snackbarHost: @Composable () -> Unit = {},
    content: @Composable () -> Unit,
) {
    var menu by rememberSaveable { mutableStateOf(false) }
    BoxWithConstraints(Modifier.fillMaxSize()) {
    val compactKeyboard = assistant && maxHeight < 480.dp && WindowInsets.ime.getBottom(LocalDensity.current) > 0
    Scaffold(modifier = Modifier.testTag("native-root"), containerColor = KitchenPaper,
        topBar = {
            if (!compactKeyboard) Surface(color = KitchenPaper) {
                Row(Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically) {
                    if (showBack) IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Quay lại") }
                    Text(title, style = MaterialTheme.typography.titleLarge,
                        fontFamily = if (title == "Bếp Nhà") androidx.compose.ui.text.font.FontFamily.Serif else androidx.compose.ui.text.font.FontFamily.SansSerif,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp).semantics { heading() }, color = KitchenGreen)
                    if (!assistant) Box {
                        TextButton(onClick = { menu = true }, modifier = Modifier.heightIn(min = 48.dp).testTag("more-menu")) { Text("Thêm") }
                        DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                            DropdownMenuItem(text = { Text("Gia đình") }, onClick = { menu = false; onFamily() },
                                modifier = Modifier.heightIn(min = 56.dp).semantics { contentDescription = "Thông tin gia đình" })
                            DropdownMenuItem(text = { Text("Nhắc nấu") }, onClick = { menu = false; onReminder() },
                                modifier = Modifier.heightIn(min = 56.dp))
                            DropdownMenuItem(text = { Text("Món đã lưu") }, onClick = { menu = false; onFavorites() },
                                modifier = Modifier.heightIn(min = 56.dp))
                        }
                    }
                }
            }
        },
        bottomBar = {
            if (!assistant && !hideNavigation) {
                Surface(color = Color.White) {
                    Column(Modifier.navigationBarsPadding()) {
                        timer?.let { KitchenTimerSummary(it, onCancelTimer) }
                        KitchenTonalButton(onClick = onAssistant,
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
                                .heightIn(min = 56.dp).testTag("ask-assistant")) { Text("Hỏi Bếp Nhà") }
                        KitchenNavigation(selected, onTab)
                    }
                }
            }
        },
        snackbarHost = snackbarHost,
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding).consumeWindowInsets(padding)) { content() }
    }
    }
}

@Composable fun KitchenCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    OutlinedCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, KitchenLine),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = KitchenMuted) }
    }
}

@Composable fun WeekScreen(vm: PlannerViewModel) {
    LazyColumn(Modifier.fillMaxSize().testTag("week-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeading("Tuần này ăn ngon mỗi ngày", "Một món mặn, một món canh, thêm rau xanh.") }
        item { MealSelector(vm) }
        items(vm.days, key = { it.toString() }) { day ->
            val menu = vm.engine.menu(vm.state, day)
            KitchenCard {
                Text(if (day == vm.today) "Hôm nay · ${PlannerEngine.shortDate(day)}" else PlannerEngine.fullDate(day), style = MaterialTheme.typography.titleMedium)
                menu.forEach { recipe ->
                    Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).clickable(onClickLabel = "Xem công thức") { vm.openRecipe(recipe.id) }.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(recipe.emoji, fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics { })
                        Column(Modifier.weight(1f)) { Text(recipe.name); Text(recipe.kind.label, style = MaterialTheme.typography.bodySmall, color = KitchenMuted) }
                    }
                }
                Text("Ước tính ${PlannerEngine.money(vm.engine.cost(menu, vm.state.profile.people))}", color = KitchenGreen)
                KitchenOutlinedButton(onClick = { vm.selectDate(day); vm.navigate(Screen.TODAY) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Xem và đổi món") }
            }
        }
    }
}

@Composable fun ShoppingScreen(vm: PlannerViewModel, actions: NativeActions) {
    val ingredients = vm.engine.ingredients(vm.menu, vm.state.profile.people)
    val checked = vm.state.checked[vm.state.key].orEmpty()
    val count = ingredients.count { it.key in checked }
    LazyColumn(Modifier.fillMaxSize().testTag("shopping-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeading("Đi chợ đủ bữa ngon", "${vm.state.meal.title} · ${PlannerEngine.fullDate(vm.state.date)} · ${vm.state.profile.people} người") }
        item { KitchenCard {
            Text("Đã chuẩn bị $count/${ingredients.size} nguyên liệu", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { count.toFloat() / ingredients.size }, modifier = Modifier.fillMaxWidth(), color = KitchenGreen, trackColor = KitchenSoft)
            Text("Tích cả những nguyên liệu đã có ở nhà.", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
            KitchenOutlinedButton(onClick = { actions.copy(vm.shoppingText()) }, modifier = Modifier.fillMaxWidth()) { Text("Sao chép danh sách") }
        } }
        ingredients.groupBy { it.group }.forEach { (group, items) ->
            item {
                Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(group, style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                        items.forEach { item ->
                            val done = item.key in checked
                            Row(Modifier.fillMaxWidth().heightIn(min = 64.dp)
                                .toggleable(value = done, role = Role.Checkbox, onValueChange = { vm.checkIngredient(item.key) })
                                .testTag("ingredient-${item.name}").padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = done, onCheckedChange = null)
                                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(item.name, textDecoration = if (done) TextDecoration.LineThrough else null,
                                        color = if (done) KitchenMuted else KitchenInk)
                                    Text("${PlannerEngine.quantity(item.amount)} ${item.unit}", style = MaterialTheme.typography.bodySmall, color = KitchenGreen)
                                }
                            }
                            HorizontalDivider(color = KitchenLine)
                        }
                    }
                }
            }
        }
        item { KitchenButton(onClick = { vm.navigate(Screen.COOK) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Đủ đồ rồi, vào bếp thôi") } }
    }
}

@Composable fun CookScreen(vm: PlannerViewModel, actions: NativeActions) {
    val tasks = vm.tasks
    val done = vm.state.done[vm.state.key].orEmpty()
    LazyColumn(Modifier.fillMaxSize().testTag("cook-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeading("Cùng nhau vào bếp", "${vm.state.meal.title} · ${PlannerEngine.fullDate(vm.state.date)}") }
        item { KitchenCard {
            Text("Dọn cơm lúc ${vm.state.profile.time(vm.state.meal)}", style = MaterialTheme.typography.headlineSmall, color = KitchenGreen)
            Text("Bắt đầu ${PlannerEngine.time(tasks.first().at)} · Khoảng ${vm.engine.minutes(vm.menu)} phút", color = KitchenMuted)
            Text("Lịch dự kiến cho bếp có 2 vùng nấu. Đã xong ${tasks.count { it.key in done }}/${tasks.size} việc.", style = MaterialTheme.typography.bodyMedium)
            KitchenActions(
                { KitchenOutlinedButton(onClick = vm::openReminders, modifier = it) { Text("Chỉnh giờ") } },
                { KitchenOutlinedButton(onClick = { actions.calendar(tasks) }, modifier = it) { Text("Thêm vào lịch") } },
            )
        } }
        items(tasks, key = { it.key }) { task ->
            KitchenCard(Modifier.testTag("task-${task.key}")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(PlannerEngine.time(task.at), fontWeight = FontWeight.SemiBold, color = KitchenGreen, modifier = Modifier.weight(1f))
                    Checkbox(task.key in done, onCheckedChange = { vm.checkTask(task.key) }, modifier = Modifier.semantics { contentDescription = "Hoàn thành công việc ${task.title}" })
                }
                Text(task.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (task.key in done) TextDecoration.LineThrough else null)
                Text(task.detail, style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
                val recipeButton: @Composable (Modifier) -> Unit = { modifier ->
                    task.recipeId?.let { id -> KitchenOutlinedButton(onClick = { vm.openRecipe(id) }, modifier = modifier) { Text("Xem từng bước") } }
                }
                val timerButton: @Composable (Modifier) -> Unit = { modifier ->
                    KitchenOutlinedButton(onClick = {
                        vm.startTimer(task.minutes, task.title)
                        if (!vm.notifications) actions.notifications()
                    }, modifier = modifier) { Text("Hẹn ${task.minutes} phút") }
                }
                if (task.recipeId != null && task.minutes > 0) KitchenActions(recipeButton, timerButton)
                else if (task.recipeId != null) recipeButton(Modifier.fillMaxWidth().heightIn(min = 56.dp))
                else if (task.minutes > 0) timerButton(Modifier.fillMaxWidth().heightIn(min = 56.dp))
            }
        }
    }
}

@Composable fun FavoritesScreen(vm: PlannerViewModel) {
    val favorites = vm.state.favorites.mapNotNull(vm.engine.recipes::get)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeading("Những món nhà mình thích", "Chạm trái tim trên mỗi món để lưu lại.") }
        if (favorites.isEmpty()) item { KitchenCard {
            Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(40.dp), tint = KitchenGreen)
            Text("Bạn chưa lưu món nào.")
            KitchenButton(onClick = { vm.navigate(Screen.TODAY) }) { Text("Xem thực đơn") }
        } }
        items(favorites, key = { it.id }) { recipe -> RecipeCard(vm, recipe) }
    }
}
