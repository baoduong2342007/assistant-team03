@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
package vn.team03.bepnha

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable fun BepNhaApp(vm: PlannerViewModel, actions: NativeActions) {
    val snackbar = remember { SnackbarHostState() }
    val owner = LocalLifecycleOwner.current
    val fontScale = LocalDensity.current.fontScale
    val screenState = rememberSaveableStateHolder()
    LaunchedEffect(owner, vm, vm.state.timer?.end) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { vm.visibleTick(); delay(if (vm.state.timer != null) 1000L else 30_000L) }
        }
    }
    LaunchedEffect(vm.message) {
        vm.message?.let { message -> snackbar.showSnackbar(message); vm.clearMessage(message) }
    }
    BackHandler(enabled = vm.sheet == null && vm.screen != Screen.TODAY) { vm.back() }
    Scaffold(
        modifier = Modifier.testTag("native-root"), containerColor = KitchenPaper,
        topBar = {
            TopAppBar(title = {
                if (vm.screen in listOf(Screen.FAMILY, Screen.FAVORITES)) Text(vm.screen.title, style = MaterialTheme.typography.titleLarge)
                else KitchenBrand()
            }, navigationIcon = {
                if (vm.screen in listOf(Screen.FAMILY, Screen.FAVORITES)) IconButton(onClick = { vm.navigate(Screen.TODAY) }) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, "Trở về Hôm nay")
                }
            }, actions = {
                IconButton(onClick = vm::openReminders) {
                    BadgedBox(badge = { if (vm.reminderEnabled) Badge(containerColor = KitchenTerracotta) }) {
                        Icon(Icons.Outlined.Notifications, "Cài đặt nhắc nấu", tint = KitchenGreen)
                    }
                }
                IconButton(onClick = { vm.navigate(Screen.FAMILY) }) {
                    Box(Modifier.size(38.dp).background(KitchenPeach, CircleShape), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Person, "Thông tin gia đình", tint = KitchenTerracotta, modifier = Modifier.size(23.dp))
                    }
                }
            }, expandedHeight = (64 * fontScale.coerceIn(1f, 1.8f)).dp,
                colors = TopAppBarDefaults.topAppBarColors(containerColor = KitchenPaper))
        },
        bottomBar = {
            Column {
                vm.state.timer?.let { timer ->
                    val seconds = ((timer.end - vm.now + 999) / 1000).coerceAtLeast(0)
                    Surface(color = KitchenGreen, contentColor = Color.White) {
                        Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(Modifier.weight(1f)) {
                                Text(timer.label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
                                Text("%02d:%02d".format(seconds / 60, seconds % 60), fontSize = 24.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.testTag("timer-countdown"))
                            }
                            IconButton(onClick = vm::cancelTimer) { Icon(Icons.Outlined.Close, "Dừng hẹn giờ") }
                        }
                    }
                }
                HorizontalDivider(color = KitchenLine)
                NavigationBar(containerColor = KitchenSurface, tonalElevation = 0.dp) {
                    listOf(Screen.TODAY, Screen.PLAN, Screen.SHOPPING, Screen.COOK).forEach { screen ->
                        NavigationBarItem(selected = vm.screen == screen, onClick = { vm.navigate(screen) },
                            modifier = Modifier.testTag("tab-${screen.name.lowercase()}"),
                            icon = { when (screen) {
                                Screen.TODAY -> Icon(Icons.Outlined.Home, null)
                                Screen.PLAN -> Icon(Icons.Outlined.DateRange, null)
                                Screen.SHOPPING -> Icon(Icons.Outlined.ShoppingCart, null)
                                else -> KitchenIcon(KitchenGlyph.BOWL, tint = if (vm.screen == screen) KitchenTerracotta else KitchenMuted)
                            } }, label = { Text(screen.title, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = KitchenTerracotta, selectedTextColor = KitchenTerracotta,
                                unselectedIconColor = KitchenMuted, unselectedTextColor = KitchenMuted, indicatorColor = KitchenPeach))
                    }
                }
            }
        }, snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Box(Modifier.widthIn(max = 680.dp).fillMaxSize()) {
                screenState.SaveableStateProvider(vm.screen.name) {
                    when (vm.screen) {
                        Screen.TODAY -> TodayScreen(vm)
                        Screen.PLAN -> WeekScreen(vm)
                        Screen.SHOPPING -> ShoppingScreen(vm, actions)
                        Screen.COOK -> CookScreen(vm, actions)
                        Screen.FAMILY -> FamilyScreen(vm, actions)
                        Screen.FAVORITES -> FavoritesScreen(vm)
                    }
                }
            }
        }
    }
    vm.sheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = vm::dismissSheet, containerColor = KitchenSurface, sheetMaxWidth = 680.dp,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)) {
            when (sheet) {
                is PlannerSheet.RecipeDetail -> RecipeSheet(vm, sheet.id, actions)
                is PlannerSheet.Swap -> SwapSheet(vm, sheet.index)
                PlannerSheet.Reminder -> ReminderSheet(vm, actions)
                PlannerSheet.Assistant -> AssistantSheet(vm)
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
    Surface(modifier = modifier, color = KitchenSurface, shape = MaterialTheme.shapes.medium, border = BorderStroke(1.dp, KitchenLine)) {
        Row(Modifier.padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Meal.entries.forEach { meal ->
                val active = vm.state.meal == meal
                Box(Modifier.weight(1f).heightIn(min = 48.dp).clip(MaterialTheme.shapes.small)
                    .background(if (active) KitchenPeach else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { vm.selectMeal(meal) })
                    .padding(horizontal = 8.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(meal.title, style = MaterialTheme.typography.labelLarge,
                        color = if (active) KitchenTerracotta else KitchenMuted)
                }
            }
        }
    }
}

@Composable private fun PeopleCount(vm: PlannerViewModel) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        KitchenIcon(KitchenGlyph.PEOPLE, Modifier.size(17.dp), KitchenMuted)
        Text("${vm.state.profile.people} người", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
    }
}
@Composable fun DateStrip(vm: PlannerViewModel) {
    val scroll = rememberLazyListState()
    LaunchedEffect(vm.state.date, vm.days) {
        val index = vm.days.indexOf(vm.state.date)
        val layout = scroll.layoutInfo
        val fullyVisible = layout.visibleItemsInfo.any {
            it.index == index && it.offset >= layout.viewportStartOffset &&
                it.offset + it.size <= layout.viewportEndOffset
        }
        if (index >= 0 && !fullyVisible) scroll.animateScrollToItem(index)
    }
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val spacing = 2.dp
        val dayWidth = with(LocalDensity.current) {
            val available = constraints.maxWidth - 2 * 20.dp.roundToPx() - spacing.roundToPx() * (vm.days.size - 1)
            maxOf(48.dp, (available / vm.days.size).toDp())
        }
        LazyRow(
            modifier = Modifier.fillMaxWidth().selectableGroup(),
            state = scroll,
            horizontalArrangement = Arrangement.spacedBy(spacing),
            contentPadding = PaddingValues(horizontal = 20.dp),
        ) {
            items(vm.days, key = { it.toString() }) { day ->
                val selected = day == vm.state.date
                val weekday = when {
                    day == vm.today -> "Nay"
                    day.dayOfWeek.value == 7 -> "CN"
                    else -> "T${day.dayOfWeek.value + 1}"
                }
                Column(
                    modifier = Modifier.width(dayWidth).heightIn(min = 58.dp).clip(MaterialTheme.shapes.small)
                        .background(if (selected) KitchenTerracotta else KitchenSurface)
                        .selectable(selected = selected, role = Role.Tab, onClick = { vm.selectDate(day) })
                        .testTag("date-$day").semantics { contentDescription = PlannerEngine.fullDate(day) }
                        .padding(vertical = 7.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(weekday, style = MaterialTheme.typography.labelMedium,
                        color = if (selected) KitchenGold else KitchenMuted)
                    Text(day.dayOfMonth.toString(), style = MaterialTheme.typography.titleLarge,
                        color = if (selected) Color.White else KitchenInk)
                }
            }
        }
    }
}

@Composable fun TodayScreen(vm: PlannerViewModel) {
    LazyColumn(
        Modifier.fillMaxSize().testTag("today-list"),
        contentPadding = PaddingValues(top = 10.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item(key = "greeting") {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                KitchenEyebrow(PlannerEngine.fullDate(vm.state.date).uppercase(PlannerEngine.locale), KitchenTerracotta)
                SectionHeading("Nhà mình ăn gì?")
            }
        }
        item(key = "dates") { DateStrip(vm) }
        item(key = "meal") { Box(Modifier.padding(horizontal = 20.dp)) { MealSelector(vm) } }
        item(key = "overview") { MealOverview(vm, Modifier.padding(horizontal = 20.dp)) }
        item(key = "menu-heading") {
            Box(Modifier.padding(horizontal = 20.dp)) {
                KitchenSection("Các món trong bữa", "Xem cả tuần") { vm.navigate(Screen.PLAN) }
            }
        }
        items(vm.menu.withIndex().toList(), key = { it.value.id }) { (index, recipe) ->
            Box(Modifier.padding(horizontal = 20.dp)) { RecipeCard(vm, recipe, index) }
        }
        item(key = "assistant") { Box(Modifier.padding(horizontal = 20.dp)) { AssistantCard(vm) } }
    }
}

@Composable private fun MealOverview(vm: PlannerViewModel, modifier: Modifier = Modifier) {
    val cost = PlannerEngine.money(vm.engine.cost(vm.menu, vm.state.profile.people)).replace(' ', '\u00A0')
    val duration = "${vm.engine.minutes(vm.menu)} phút"
    Surface(modifier.fillMaxWidth(), color = KitchenGreen, contentColor = Color.White, shape = MaterialTheme.shapes.extraLarge) {
        Column {
            Image(
                LocalMealPhoto.current,
                "Ảnh minh họa mâm cơm Việt với thịt kho, canh và rau",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(144.dp),
            )
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
                Text("Chưa tính gạo và gia vị có sẵn.", style = MaterialTheme.typography.bodySmall, color = KitchenOnGreen)
            }
        }
    }
}

@Composable private fun MealMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = KitchenOnGreen)
        Text(value, style = MaterialTheme.typography.titleLarge)
    }
}

@Composable fun RecipeCard(vm: PlannerViewModel, recipe: Recipe, index: Int? = null) {
    KitchenCard(Modifier.testTag("recipe-${recipe.id}")) {
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
                colors = IconButtonDefaults.filledTonalIconButtonColors(
                    containerColor = if (saved) KitchenPeach else KitchenPaper,
                    contentColor = if (saved) KitchenTerracotta else KitchenMuted,
                ),
            ) {
                Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    if (saved) "Bỏ lưu ${recipe.name}" else "Lưu ${recipe.name}", modifier = Modifier.size(20.dp))
            }
        }
        HorizontalDivider(color = KitchenLine)
        val openRecipe: @Composable (Modifier) -> Unit = { modifier ->
            TextButton(onClick = { vm.openRecipe(recipe.id) },
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

@Composable fun AssistantCard(vm: PlannerViewModel) {
    Surface(
        onClick = vm::openAssistant,
        modifier = Modifier.fillMaxWidth(),
        color = KitchenPeach,
        contentColor = KitchenTerracotta,
        shape = MaterialTheme.shapes.large,
    ) {
        Row(Modifier.padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KitchenIcon(KitchenGlyph.SPARKLE, tint = KitchenTerracotta)
            Column(Modifier.weight(1f)) {
                Text("Hỏi Bếp Nhà", style = MaterialTheme.typography.titleMedium)
                Text("Tìm món nhanh, món tiết kiệm hoặc cách nấu", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
        }
    }
}

@Composable fun WeekScreen(vm: PlannerViewModel) {
    LazyColumn(Modifier.fillMaxSize().testTag("week-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item { SectionHeading("Thực đơn tuần này") }
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
                menu.forEach { recipe ->
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

@Composable fun ShoppingScreen(vm: PlannerViewModel, actions: NativeActions) {
    val ingredients = vm.engine.ingredients(vm.menu, vm.state.profile.people)
    val checked = vm.state.checked[vm.state.key].orEmpty()
    val count = ingredients.count { it.key in checked }
    LazyColumn(Modifier.fillMaxSize().testTag("shopping-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item {
            SectionHeading("Danh sách đi chợ", "${vm.state.meal.title} · ${PlannerEngine.fullDate(vm.state.date)} · ${vm.state.profile.people} người")
        }
        item {
            Surface(color = KitchenSoft, contentColor = KitchenGreen, shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(Modifier.size(44.dp).background(KitchenGreen, MaterialTheme.shapes.small), contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.ShoppingCart, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        }
                        Column(Modifier.weight(1f)) {
                            Text("Đã chuẩn bị $count/${ingredients.size}", style = MaterialTheme.typography.titleLarge)
                            Text("Tích cả nguyên liệu đã có ở nhà.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                        }
                    }
                    LinearProgressIndicator(
                        progress = { if (ingredients.isEmpty()) 0f else count.toFloat() / ingredients.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = KitchenGreen,
                        trackColor = KitchenGreen.copy(alpha = 0.14f),
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    OutlinedButton(
                        onClick = { actions.copy(vm.shoppingText()) },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp),
                        border = BorderStroke(1.dp, KitchenGreen.copy(alpha = 0.35f)),
                    ) { Text("Sao chép danh sách") }
                }
            }
        }
        ingredients.groupBy { it.group }.forEach { (group, groupItems) ->
            item(key = "group-$group") {
                OutlinedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    border = BorderStroke(1.dp, KitchenLine),
                    colors = CardDefaults.outlinedCardColors(containerColor = KitchenSurface),
                ) {
                    Text(group, Modifier.fillMaxWidth().background(KitchenPeach).padding(horizontal = 16.dp, vertical = 11.dp)
                        .semantics { heading() }, style = MaterialTheme.typography.titleMedium, color = KitchenTerracotta)
                    groupItems.forEachIndexed { index, item ->
                        val done = item.key in checked
                        Row(
                            Modifier.fillMaxWidth().background(if (done) KitchenSoft else KitchenSurface)
                                .toggleable(value = done, role = Role.Checkbox, onValueChange = { vm.checkIngredient(item.key) })
                                .testTag("ingredient-${item.name}").heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Checkbox(checked = done, onCheckedChange = null)
                            Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
                                textDecoration = if (done) TextDecoration.LineThrough else null,
                                color = if (done) KitchenMuted else KitchenInk)
                            Text("${PlannerEngine.quantity(item.amount)} ${item.unit}",
                                style = MaterialTheme.typography.labelMedium, color = KitchenGreen)
                        }
                        if (index < groupItems.lastIndex) HorizontalDivider(color = KitchenLine)
                    }
                }
            }
        }
        item {
            Button(onClick = { vm.navigate(Screen.COOK) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Vào bếp")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
}

@Composable fun CookScreen(vm: PlannerViewModel, actions: NativeActions) {
    val tasks = vm.tasks
    val done = vm.state.done[vm.state.key].orEmpty()
    val completed = tasks.count { it.key in done }
    LazyColumn(Modifier.fillMaxSize().testTag("cook-list"), contentPadding = PaddingValues(20.dp)) {
        item {
            SectionHeading("Lịch nấu", "${vm.state.meal.title} · ${PlannerEngine.fullDate(vm.state.date)}")
            Spacer(Modifier.height(14.dp))
            Surface(color = KitchenGreen, contentColor = Color.White, shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        KitchenIcon(KitchenGlyph.CLOCK, tint = KitchenGold)
                        Text("Bắt đầu lúc ${PlannerEngine.time(tasks.first().at)}", style = MaterialTheme.typography.labelLarge, color = KitchenGold)
                    }
                    Text("Dọn cơm lúc ${vm.state.profile.time(vm.state.meal)}", style = MaterialTheme.typography.headlineSmall)
                    Text("Khoảng ${vm.engine.minutes(vm.menu)} phút với 2 vùng nấu", style = MaterialTheme.typography.bodyMedium, color = KitchenOnGreen)
                    LinearProgressIndicator(
                        progress = { completed.toFloat() / tasks.size },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = KitchenGold,
                        trackColor = Color.White.copy(alpha = 0.2f),
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                    Text("Đã xong $completed/${tasks.size} việc", style = MaterialTheme.typography.bodySmall, color = KitchenOnGreen)
                    KitchenActions(
                        first = { modifier ->
                            OutlinedButton(
                                onClick = vm::openReminders,
                                border = BorderStroke(1.dp, KitchenOnGreen.copy(alpha = 0.55f)),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                modifier = modifier.heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            ) { Text("Chỉnh giờ") }
                        },
                        second = { modifier ->
                            Button(
                                onClick = { actions.calendar(tasks) },
                                colors = ButtonDefaults.buttonColors(containerColor = KitchenGold, contentColor = KitchenGreen),
                                modifier = modifier.heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            ) { Text("Thêm vào lịch") }
                        },
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
        itemsIndexed(tasks, key = { _, task -> task.key }) { index, task ->
            val finished = task.key in done
            val marker = if (finished) KitchenGreen else KitchenTerracotta
            Box(
                Modifier.fillMaxWidth().drawBehind {
                    val inset = 5.dp.toPx()
                    val x = if (layoutDirection == LayoutDirection.Ltr) inset else size.width - inset
                    val y = 40.dp.toPx()
                    drawLine(
                        KitchenLine,
                        start = Offset(x, if (index == 0) y else 0f),
                        end = Offset(x, if (index == tasks.lastIndex) y else size.height),
                        strokeWidth = 2.dp.toPx(),
                    )
                    drawCircle(marker, radius = 4.dp.toPx(), center = Offset(x, y))
                }.padding(start = 19.dp, bottom = 12.dp),
            ) {
                KitchenCard(Modifier.testTag("task-${task.key}")) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            color = if (finished) KitchenSoft else KitchenPeach,
                            contentColor = marker,
                            shape = MaterialTheme.shapes.extraSmall,
                        ) {
                            Text(PlannerEngine.time(task.at), Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelLarge)
                        }
                        Spacer(Modifier.weight(1f))
                        Checkbox(finished, onCheckedChange = { vm.checkTask(task.key) },
                            modifier = Modifier.semantics { contentDescription = "Đã xong ${task.title}" })
                    }
                    Text(task.title, style = MaterialTheme.typography.titleMedium,
                        textDecoration = if (finished) TextDecoration.LineThrough else null)
                    Text(task.detail, style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
                    val recipeId = task.recipeId
                    val timer: @Composable (Modifier) -> Unit = { modifier ->
                        OutlinedButton(
                            onClick = {
                                vm.startTimer(task.minutes, task.title)
                                if (!vm.notifications) actions.notifications()
                            },
                            modifier = modifier.heightIn(min = 48.dp),
                            border = BorderStroke(1.dp, KitchenLine),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                        ) { Text("Hẹn ${task.minutes} phút") }
                    }
                    if (recipeId != null) {
                        val recipe: @Composable (Modifier) -> Unit = { modifier ->
                            TextButton(onClick = { vm.openRecipe(recipeId) }, modifier = modifier.heightIn(min = 48.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)) { Text("Xem từng bước") }
                        }
                        if (task.minutes > 0) KitchenActions(first = recipe, second = timer)
                        else recipe(Modifier.fillMaxWidth())
                    } else if (task.minutes > 0) {
                        timer(Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable fun FavoritesScreen(vm: PlannerViewModel) {
    val favorites = vm.state.favorites.mapNotNull(vm.engine.recipes::get)
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (favorites.isEmpty()) item { KitchenCard {
            Icon(Icons.Outlined.FavoriteBorder, null, modifier = Modifier.size(40.dp), tint = KitchenGreen)
            Text("Chưa có món đã lưu", style = MaterialTheme.typography.titleMedium)
            Text("Chạm vào trái tim bên cạnh món bạn muốn tìm lại.", color = KitchenMuted)
            Button(onClick = { vm.navigate(Screen.TODAY) }) { Text("Xem thực đơn") }
        } }
        items(favorites, key = { it.id }) { recipe -> RecipeCard(vm, recipe) }
    }
}
