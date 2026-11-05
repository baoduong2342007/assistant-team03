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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.Role
import androidx.compose.foundation.selection.toggleable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
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
            }, expandedHeight = (72 * fontScale.coerceIn(1f, 1.8f)).dp,
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
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    listOf(Screen.TODAY, Screen.PLAN, Screen.SHOPPING, Screen.COOK).forEach { screen ->
                        NavigationBarItem(selected = vm.screen == screen, onClick = { vm.navigate(screen) },
                            modifier = Modifier.testTag("tab-${screen.name.lowercase()}"),
                            icon = { when (screen) {
                                Screen.TODAY -> Icon(Icons.Outlined.Home, null)
                                Screen.PLAN -> Icon(Icons.Outlined.DateRange, null)
                                Screen.SHOPPING -> Icon(Icons.Outlined.ShoppingCart, null)
                                else -> KitchenIcon(KitchenGlyph.BOWL, tint = if (vm.screen == screen) KitchenGreen else KitchenMuted)
                            } }, label = { Text(screen.title, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = KitchenGreen, selectedTextColor = KitchenGreen,
                                unselectedIconColor = KitchenMuted, unselectedTextColor = KitchenMuted, indicatorColor = KitchenSoft))
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
        ModalBottomSheet(onDismissRequest = vm::dismissSheet, containerColor = KitchenPaper, sheetMaxWidth = 680.dp,
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
    OutlinedCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, KitchenLine),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.semantics { heading() })
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = KitchenMuted) }
    }
}
@Composable fun MealSelector(vm: PlannerViewModel) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 300.dp || LocalDensity.current.fontScale > 1.15f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                MealTabs(vm, Modifier.fillMaxWidth())
                Box(Modifier.align(Alignment.End)) { PeopleCount(vm) }
            }
        } else Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MealTabs(vm, Modifier.weight(1f))
            PeopleCount(vm)
        }
    }
}

@Composable private fun MealTabs(vm: PlannerViewModel, modifier: Modifier) {
    Surface(modifier = modifier, color = Color.White, shape = RoundedCornerShape(16.dp), border = BorderStroke(1.dp, KitchenLine)) {
        Row(Modifier.padding(4.dp), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Meal.entries.forEach { meal ->
                val active = vm.state.meal == meal
                Surface(onClick = { vm.selectMeal(meal) }, shape = RoundedCornerShape(12.dp),
                    color = if (active) KitchenGreen else Color.Transparent, contentColor = if (active) Color.White else KitchenMuted,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp).semantics { selected = active }) {
                    Box(Modifier.padding(horizontal = 16.dp, vertical = 12.dp), contentAlignment = Alignment.Center) {
                        Text(meal.title, style = MaterialTheme.typography.labelLarge)
                    }
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
    LaunchedEffect(vm.state.date) { scroll.animateScrollToItem(vm.days.indexOf(vm.state.date).coerceAtLeast(0)) }
    LazyRow(state = scroll, horizontalArrangement = Arrangement.spacedBy(7.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
        items(vm.days, key = { it.toString() }) { day ->
            val selected = day == vm.state.date
            Surface(onClick = { vm.selectDate(day) }, modifier = Modifier.width(49.dp).heightIn(min = 82.dp)
                .testTag("date-$day").semantics { this.selected = selected; contentDescription = PlannerEngine.fullDate(day) },
                color = if (selected) KitchenGreen else Color.Transparent, contentColor = if (selected) Color.White else KitchenInk,
                shape = RoundedCornerShape(17.dp)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (day == vm.today) "Nay" else if (day.dayOfWeek.value == 7) "CN" else "T${day.dayOfWeek.value + 1}", fontSize = 12.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(day.dayOfMonth.toString(), fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(6.dp))
                    Box(Modifier.size(4.dp).background(if (selected) Color(0xFFE4CBA0) else KitchenLine, CircleShape))
                }
            }
        }
    }
}

@Composable fun TodayScreen(vm: PlannerViewModel) {
    LazyColumn(Modifier.fillMaxSize().testTag("today-list"), contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item(key = "greeting") {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                KitchenEyebrow(PlannerEngine.fullDate(vm.today).uppercase(PlannerEngine.locale))
                SectionHeading("Hôm nay nhà mình ăn gì?", "Để Bếp lo thực đơn, bạn lo yêu thương.")
            }
        }
        item {
            val fontScale = LocalDensity.current.fontScale
            Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height((176 * fontScale.coerceIn(1f, 1.8f)).dp).clip(RoundedCornerShape(26.dp)).background(KitchenGreen)) {
                Image(LocalMealPhoto.current, "Mâm cơm Việt với thịt kho, canh và rau", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(), alignment = Alignment.Center)
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.05f), Color(0xFF18281B).copy(alpha = 0.78f)))))
                Surface(Modifier.align(Alignment.TopStart).padding(16.dp), color = KitchenPaper.copy(alpha = 0.96f), shape = CircleShape) {
                    Row(Modifier.padding(horizontal = 11.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        KitchenIcon(KitchenGlyph.LEAF, Modifier.size(13.dp))
                        Text("Vị quen, thương nhiều", fontSize = 10.sp, color = KitchenGreen, fontWeight = FontWeight.Medium)
                    }
                }
                Column(Modifier.padding(20.dp).align(Alignment.BottomStart), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("Bữa cơm nhỏ.\nHạnh phúc to.", style = MaterialTheme.typography.headlineMedium, color = Color.White)
                    Text("Một chút chăm chút, cả nhà ngon miệng.", fontSize = 12.sp, color = Color.White.copy(alpha = 0.9f))
                }
            }
        }
        item {
            Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                KitchenSection("Thực đơn của nhà", "Xem cả tuần →") { vm.navigate(Screen.PLAN) }
            }
        }
        item { DateStrip(vm) }
        item { Box(Modifier.padding(horizontal = 20.dp)) { MealSelector(vm) } }
        items(vm.menu.withIndex().toList(), key = { it.value.id }) { (index, recipe) ->
            Box(Modifier.padding(horizontal = 20.dp)) { RecipeCard(vm, recipe, index) }
        }
        item { Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            MealSummary(vm)
            CostCard(vm)
            Button(onClick = { vm.navigate(Screen.COOK) }, shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                KitchenIcon(KitchenGlyph.BOWL, tint = Color.White)
                Spacer(Modifier.width(10.dp))
                Text("Vào bếp cùng Bếp Nhà", modifier = Modifier.weight(1f))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(20.dp))
            }
            OutlinedButton(onClick = { vm.navigate(Screen.SHOPPING) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Icon(Icons.Outlined.ShoppingCart, null, Modifier.size(19.dp)); Spacer(Modifier.width(8.dp)); Text("Chuẩn bị danh sách đi chợ")
            }
            AssistantCard(vm)
        } }
    }
}

@Composable private fun MealSummary(vm: PlannerViewModel) {
    Surface(color = KitchenSoft, shape = RoundedCornerShape(18.dp)) {
        Row(Modifier.fillMaxWidth().padding(15.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KitchenIcon(KitchenGlyph.BOWL)
            Column(Modifier.weight(1f)) {
                Text("3 món ngon, một mâm cơm", style = MaterialTheme.typography.labelLarge, color = KitchenGreen)
                Text("Món mặn · Canh · Rau xanh", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("${vm.engine.minutes(vm.menu)} phút", style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                Text("ước tính", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
        }
    }
}

@Composable fun RecipeCard(vm: PlannerViewModel, recipe: Recipe, index: Int? = null) {
    KitchenCard(Modifier.testTag("recipe-${recipe.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RecipePortrait(recipe)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recipe.kind.label.uppercase(PlannerEngine.locale), fontSize = 10.sp, lineHeight = 16.sp, letterSpacing = 1.sp, color = KitchenTerracotta, fontWeight = FontWeight.SemiBold)
                Text(recipe.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.clickable { vm.openRecipe(recipe.id) }.testTag("open-${recipe.id}"))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    KitchenIcon(KitchenGlyph.CLOCK, Modifier.size(14.dp), KitchenMuted)
                    Text("${recipe.minutes} phút", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                }
            }
            IconButton(onClick = { vm.toggleFavorite(recipe.id) }) {
                val saved = recipe.id in vm.state.favorites
                Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    if (saved) "Bỏ lưu ${recipe.name}" else "Lưu ${recipe.name}", tint = if (saved) KitchenTerracotta else KitchenMuted)
            }
        }
        HorizontalDivider(color = KitchenLine)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.openRecipe(recipe.id) }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Xem công thức") }
            if (index != null) OutlinedButton(onClick = { vm.openSwap(index) }, modifier = Modifier.weight(1f).testTag("swap-${recipe.id}")
                .heightIn(min = 48.dp).semantics { contentDescription = "Đổi món ${recipe.name}" }) {
                Icon(Icons.Outlined.Refresh, null, Modifier.size(17.dp)); Spacer(Modifier.width(5.dp)); Text("Đổi món")
            }
        }
    }
}
@Composable fun CostCard(vm: PlannerViewModel) {
    Surface(color = KitchenPeach, shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chi phí ước tính", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(PlannerEngine.money(vm.engine.cost(vm.menu, vm.state.profile.people)), style = MaterialTheme.typography.titleLarge, color = KitchenTerracotta)
            }
            Text("3 món · ${vm.state.profile.people} người · Khoảng ${vm.engine.minutes(vm.menu)} phút", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            Text("Chưa tính gạo và gia vị có sẵn.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        }
    }
}
@Composable fun AssistantCard(vm: PlannerViewModel) {
    Surface(color = KitchenGreen, shape = RoundedCornerShape(24.dp)) {
        Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                KitchenIcon(KitchenGlyph.SPARKLE, tint = Color(0xFFE6CE9E))
                Text("Bếp luôn có một gợi ý hay", style = MaterialTheme.typography.titleMedium, color = Color.White)
            }
            Text("Món nhanh, món tiết kiệm hay cách nấu? Cứ hỏi Bếp nhé.", style = MaterialTheme.typography.bodyMedium, color = Color(0xFFDEE8DD))
            Button(onClick = vm::openAssistant, colors = ButtonDefaults.buttonColors(containerColor = KitchenPaper, contentColor = KitchenGreen),
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Hỏi Bếp Nhà"); Spacer(Modifier.width(8.dp)); Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp)) }
        }
    }
}

@Composable fun WeekScreen(vm: PlannerViewModel) {
    LazyColumn(Modifier.fillMaxSize().testTag("week-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeading("Tuần này ăn ngon mỗi ngày", "Một món mặn, một món canh, thêm rau xanh.") }
        item { MealSelector(vm) }
        items(vm.days, key = { it.toString() }) { day ->
            val menu = vm.engine.menu(vm.state, day)
            KitchenCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(color = if (day == vm.today) KitchenGreen else KitchenSoft, shape = RoundedCornerShape(14.dp)) {
                        Text(day.dayOfMonth.toString(), Modifier.padding(horizontal = 15.dp, vertical = 10.dp), fontSize = 22.sp,
                            color = if (day == vm.today) Color.White else KitchenGreen, fontWeight = FontWeight.SemiBold)
                    }
                    Column {
                        KitchenEyebrow(if (day == vm.today) "HÔM NAY" else "THỰC ĐƠN GIA ĐÌNH", KitchenTerracotta)
                        Text(PlannerEngine.fullDate(day), style = MaterialTheme.typography.titleMedium)
                    }
                }
                HorizontalDivider(color = KitchenLine)
                menu.forEach { recipe ->
                    Row(Modifier.fillMaxWidth().clickable { vm.openRecipe(recipe.id) }.heightIn(min = 56.dp).padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        RecipePortrait(recipe, 48.dp)
                        Column(Modifier.weight(1f)) { Text(recipe.name); Text(recipe.kind.label, style = MaterialTheme.typography.bodySmall, color = KitchenMuted) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Chi phí ước tính", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                        Text("~${PlannerEngine.money(vm.engine.cost(menu, vm.state.profile.people))}", color = KitchenGreen, fontWeight = FontWeight.SemiBold)
                    }
                    TextButton(onClick = { vm.selectDate(day); vm.navigate(Screen.TODAY) }) { Text("Xem và đổi món") }
                }
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
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.size(48.dp).background(KitchenSoft, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ShoppingCart, null, tint = KitchenGreen)
                }
                Column(Modifier.weight(1f)) {
                    KitchenEyebrow("GIỎ ĐỒ CỦA NHÀ MÌNH", KitchenTerracotta)
                    Text("Đã chuẩn bị $count/${ingredients.size} nguyên liệu", style = MaterialTheme.typography.titleMedium)
                }
            }
            LinearProgressIndicator(progress = { if (ingredients.isEmpty()) 0f else count.toFloat() / ingredients.size },
                modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape), color = KitchenGreen, trackColor = KitchenSoft)
            Text("Tích cả những nguyên liệu đã có ở nhà.", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
            OutlinedButton(onClick = { actions.copy(vm.shoppingText()) }, modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Sao chép danh sách") }
        } }
        ingredients.groupBy { it.group }.forEach { (group, items) ->
            item { KitchenCard {
                Text(group, style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                items.forEachIndexed { index, item ->
                    val done = item.key in checked
                    Row(Modifier.fillMaxWidth().toggleable(value = done, role = Role.Checkbox, onValueChange = { vm.checkIngredient(item.key) })
                        .testTag("ingredient-${item.name}").heightIn(min = 56.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = done, onCheckedChange = null, modifier = Modifier.padding(end = 12.dp))
                        Text(item.name, modifier = Modifier.weight(1f), textDecoration = if (done) TextDecoration.LineThrough else null,
                            color = if (done) KitchenMuted else KitchenInk)
                        Text("${PlannerEngine.quantity(item.amount)} ${item.unit}", style = MaterialTheme.typography.bodyMedium, color = KitchenGreen)
                    }
                    if (index < items.lastIndex) HorizontalDivider(color = KitchenLine.copy(alpha = 0.6f))
                }
            } }
        }
        item { Button(onClick = { vm.navigate(Screen.COOK) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Đủ đồ rồi, vào bếp thôi") } }
    }
}

@Composable fun CookScreen(vm: PlannerViewModel, actions: NativeActions) {
    val tasks = vm.tasks
    val done = vm.state.done[vm.state.key].orEmpty()
    LazyColumn(Modifier.fillMaxSize().testTag("cook-list"), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SectionHeading("Cùng nhau vào bếp", "${vm.state.meal.title} · ${PlannerEngine.fullDate(vm.state.date)}") }
        item { Surface(color = KitchenGreen, shape = RoundedCornerShape(24.dp)) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(13.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KitchenIcon(KitchenGlyph.CLOCK, tint = Color(0xFFE6CE9E))
                    KitchenEyebrow("THONG THẢ NẤU, ĐÚNG GIỜ ĂN", Color(0xFFE6CE9E))
                }
                Text("Dọn cơm lúc ${vm.state.profile.time(vm.state.meal)}", style = MaterialTheme.typography.headlineSmall, color = Color.White)
                Text("Bắt đầu ${PlannerEngine.time(tasks.first().at)} · Khoảng ${vm.engine.minutes(vm.menu)} phút", color = Color(0xFFDEE8DD))
                LinearProgressIndicator(progress = { tasks.count { it.key in done }.toFloat() / tasks.size }, modifier = Modifier.fillMaxWidth().height(5.dp).clip(CircleShape),
                    color = Color(0xFFE6CE9E), trackColor = Color.White.copy(alpha = 0.18f))
                Text("Đã xong ${tasks.count { it.key in done }}/${tasks.size} việc · Dự kiến cho bếp 2 vùng nấu", style = MaterialTheme.typography.bodySmall, color = Color(0xFFDEE8DD))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(onClick = vm::openReminders, border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White), modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Chỉnh giờ") }
                    Button(onClick = { actions.calendar(tasks) }, colors = ButtonDefaults.buttonColors(containerColor = KitchenPaper, contentColor = KitchenGreen),
                        modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Thêm vào lịch") }
                }
            }
        } }
        items(tasks, key = { it.key }) { task ->
            KitchenCard(Modifier.testTag("task-${task.key}")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(color = if (task.key in done) KitchenSoft else KitchenPeach, shape = RoundedCornerShape(10.dp)) {
                        Text(PlannerEngine.time(task.at), Modifier.padding(horizontal = 12.dp, vertical = 7.dp), fontWeight = FontWeight.SemiBold,
                            color = if (task.key in done) KitchenGreen else KitchenTerracotta)
                    }
                    Spacer(Modifier.weight(1f))
                    Checkbox(task.key in done, onCheckedChange = { vm.checkTask(task.key) }, modifier = Modifier.semantics { contentDescription = "Đã xong ${task.title}" })
                }
                Text(task.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (task.key in done) TextDecoration.LineThrough else null)
                Text(task.detail, style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
                if (task.recipeId != null || task.minutes > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    task.recipeId?.let { id -> TextButton(onClick = { vm.openRecipe(id) }, modifier = Modifier.weight(1f)) { Text("Xem từng bước") } }
                    if (task.minutes > 0) OutlinedButton(onClick = {
                        vm.startTimer(task.minutes, task.title)
                        if (!vm.notifications) actions.notifications()
                    }, modifier = Modifier.weight(1f).heightIn(min = 48.dp)) { Text("Hẹn ${task.minutes} phút") }
                }
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
            Text("Dành một chỗ cho món ruột", style = MaterialTheme.typography.titleMedium)
            Text("Bạn chưa lưu món nào. Chạm trái tim bên cạnh món yêu thích để dễ tìm lại lần sau.", color = KitchenMuted)
            Button(onClick = { vm.navigate(Screen.TODAY) }) { Text("Xem thực đơn") }
        } }
        items(favorites, key = { it.id }) { recipe -> RecipeCard(vm, recipe) }
    }
}
