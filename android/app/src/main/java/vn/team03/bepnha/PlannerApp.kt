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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalDate

@Composable fun BepNhaApp(vm: PlannerViewModel, actions: NativeActions) {
    val snackbar = remember { SnackbarHostState() }
    val owner = LocalLifecycleOwner.current
    LaunchedEffect(owner, vm) {
        owner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (isActive) { vm.visibleTick(); delay(1000) }
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
                else Text("Bếp Nhà", fontFamily = FontFamily.Serif, fontWeight = FontWeight.SemiBold, fontSize = 28.sp, color = KitchenGreen)
            }, navigationIcon = {
                if (vm.screen in listOf(Screen.FAMILY, Screen.FAVORITES)) IconButton(onClick = { vm.navigate(Screen.TODAY) }) {
                    Icon(Icons.Outlined.ArrowBack, "Trở về Hôm nay")
                }
            }, actions = {
                IconButton(onClick = vm::openReminders) { Icon(Icons.Outlined.Notifications, "Cài đặt nhắc nấu", tint = KitchenGreen) }
                IconButton(onClick = { vm.navigate(Screen.FAMILY) }) { Icon(Icons.Outlined.AccountCircle, "Thông tin gia đình", tint = KitchenGreen) }
            }, colors = TopAppBarDefaults.topAppBarColors(containerColor = KitchenPaper))
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
                NavigationBar(containerColor = Color.White, tonalElevation = 0.dp) {
                    listOf(Screen.TODAY, Screen.PLAN, Screen.SHOPPING, Screen.COOK).forEach { screen ->
                        NavigationBarItem(selected = vm.screen == screen, onClick = { vm.navigate(screen) },
                            modifier = Modifier.testTag("tab-${screen.name.lowercase()}"),
                            icon = { when (screen) {
                                Screen.TODAY -> Icon(Icons.Outlined.Home, null)
                                Screen.PLAN -> Icon(Icons.Outlined.DateRange, null)
                                Screen.SHOPPING -> Icon(Icons.Outlined.ShoppingCart, null)
                                else -> Text("♨", fontSize = 25.sp, color = KitchenGreen)
                            } }, label = { Text(screen.title, style = MaterialTheme.typography.labelMedium) },
                            colors = NavigationBarItemDefaults.colors(selectedIconColor = KitchenGreen, selectedTextColor = KitchenGreen, indicatorColor = KitchenSoft))
                    }
                }
            }
        }, snackbarHost = { SnackbarHost(snackbar) },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
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
    vm.sheet?.let { sheet ->
        ModalBottomSheet(onDismissRequest = vm::dismissSheet, containerColor = KitchenPaper,
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
    OutlinedCard(modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), border = BorderStroke(1.dp, KitchenLine),
        colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
        Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}
@Composable fun SectionHeading(title: String, subtitle: String? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall)
        subtitle?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = KitchenMuted) }
    }
}
@Composable fun MealSelector(vm: PlannerViewModel) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        Meal.entries.forEach { meal -> FilterChip(selected = vm.state.meal == meal, onClick = { vm.selectMeal(meal) },
            label = { Text(meal.title) }, modifier = Modifier.heightIn(min = 48.dp)) }
        Spacer(Modifier.weight(1f))
        Text("${vm.state.profile.people} người", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
    }
}
@Composable fun DateStrip(vm: PlannerViewModel) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 20.dp)) {
        items(vm.days, key = { it.toString() }) { day ->
            val selected = day == vm.state.date
            Surface(onClick = { vm.selectDate(day) }, modifier = Modifier.width(68.dp).height(80.dp),
                color = if (selected) KitchenGreen else Color.White, contentColor = if (selected) Color.White else KitchenInk,
                shape = RoundedCornerShape(16.dp), border = if (selected) null else BorderStroke(1.dp, KitchenLine)) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                    Text(if (day == vm.today) "Hôm nay" else if (day.dayOfWeek.value == 7) "CN" else "T${day.dayOfWeek.value + 1}", fontSize = 12.sp)
                    Spacer(Modifier.height(5.dp))
                    Text(day.dayOfMonth.toString(), fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
                    Text("tháng ${day.monthValue}", fontSize = 10.sp)
                }
            }
        }
    }
}

@Composable fun TodayScreen(vm: PlannerViewModel) {
    LazyColumn(Modifier.fillMaxSize().testTag("today-list"), contentPadding = PaddingValues(bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { Column(Modifier.padding(horizontal = 20.dp)) { SectionHeading("Hôm nay nhà mình ăn gì?", "Bữa cơm ngon, nhẹ chuyện bếp núc.") } }
        item {
            Box(Modifier.padding(horizontal = 20.dp).fillMaxWidth().height(168.dp).clip(RoundedCornerShape(22.dp)).background(KitchenSoft)) {
                Image(painterResource(R.drawable.family_meal), "Mâm cơm Việt với thịt kho, canh và rau", contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(), alignment = Alignment.CenterEnd)
                Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(KitchenSoft, KitchenSoft.copy(alpha = 0.87f), Color.Transparent))))
                Column(Modifier.padding(20.dp).widthIn(max = 235.dp).align(Alignment.CenterStart), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("VỊ QUEN, THƯƠNG NHIỀU", fontSize = 10.sp, color = KitchenGreen, fontWeight = FontWeight.SemiBold)
                    Text("Tròn vị cơm nhà,\nấm lòng cả nhà.", style = MaterialTheme.typography.headlineMedium, color = KitchenGreen)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Thực đơn của nhà", style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                TextButton(onClick = { vm.navigate(Screen.PLAN) }) { Text("Cả tuần") }
                IconButton(onClick = { vm.navigate(Screen.FAVORITES) }) { Icon(Icons.Outlined.FavoriteBorder, "Món đã lưu", tint = KitchenGreen) }
            }
        }
        item { DateStrip(vm) }
        item { Box(Modifier.padding(horizontal = 20.dp)) { MealSelector(vm) } }
        items(vm.menu.withIndex().toList(), key = { it.value.id }) { (index, recipe) ->
            Box(Modifier.padding(horizontal = 20.dp)) { RecipeCard(vm, recipe, index) }
        }
        item { Column(Modifier.padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            CostCard(vm)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = { vm.navigate(Screen.SHOPPING) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Chuẩn bị đi chợ") }
                Button(onClick = { vm.navigate(Screen.COOK) }, modifier = Modifier.weight(1f).heightIn(min = 52.dp)) { Text("Bắt đầu nấu") }
            }
            AssistantCard(vm)
        } }
    }
}

@Composable fun RecipeCard(vm: PlannerViewModel, recipe: Recipe, index: Int? = null) {
    KitchenCard(Modifier.testTag("recipe-${recipe.id}")) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(64.dp).clip(RoundedCornerShape(16.dp)).background(KitchenSoft), contentAlignment = Alignment.Center) { Text(recipe.emoji, fontSize = 34.sp) }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recipe.kind.label, style = MaterialTheme.typography.labelMedium, color = KitchenMuted)
                Text(recipe.name, style = MaterialTheme.typography.titleMedium, modifier = Modifier.clickable { vm.openRecipe(recipe.id) }.testTag("open-${recipe.id}"))
                Text("${recipe.minutes} phút · Dễ nấu", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
            IconButton(onClick = { vm.toggleFavorite(recipe.id) }) {
                val saved = recipe.id in vm.state.favorites
                Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    if (saved) "Bỏ lưu ${recipe.name}" else "Lưu ${recipe.name}", tint = KitchenGreen)
            }
        }
        HorizontalDivider(color = KitchenLine)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { vm.openRecipe(recipe.id) }, modifier = Modifier.weight(1f)) { Text("Xem công thức") }
            if (index != null) OutlinedButton(onClick = { vm.openSwap(index) }, modifier = Modifier.weight(1f).testTag("swap-${recipe.id}")
                .semantics { contentDescription = "Đổi món ${recipe.name}" }) { Text("Đổi món") }
        }
    }
}
@Composable fun CostCard(vm: PlannerViewModel) {
    Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Chi phí ước tính", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                Text(PlannerEngine.money(vm.engine.cost(vm.menu, vm.state.profile.people)), fontWeight = FontWeight.SemiBold, color = KitchenGreen)
            }
            Text("3 món · ${vm.state.profile.people} người · Khoảng ${vm.engine.minutes(vm.menu)} phút", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            Text("Chưa tính gạo và gia vị có sẵn.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        }
    }
}
@Composable fun AssistantCard(vm: PlannerViewModel) {
    KitchenCard {
        Text("Cần một gợi ý cho bữa cơm?", style = MaterialTheme.typography.titleMedium)
        Text("Tìm món nhanh, đổi vị hoặc xem cách nấu từ 14 công thức có sẵn.", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
        FilledTonalButton(onClick = vm::openAssistant, modifier = Modifier.fillMaxWidth()) { Text("Hỏi Bếp Nhà") }
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
                    Row(Modifier.fillMaxWidth().clickable { vm.openRecipe(recipe.id) }.padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(recipe.emoji, fontSize = 28.sp)
                        Column(Modifier.weight(1f)) { Text(recipe.name); Text(recipe.kind.label, style = MaterialTheme.typography.bodySmall, color = KitchenMuted) }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("~${PlannerEngine.money(vm.engine.cost(menu, vm.state.profile.people))}", color = KitchenGreen, modifier = Modifier.weight(1f))
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
            Text("Đã chuẩn bị $count/${ingredients.size} nguyên liệu", style = MaterialTheme.typography.titleMedium)
            LinearProgressIndicator(progress = { count.toFloat() / ingredients.size }, modifier = Modifier.fillMaxWidth(), color = KitchenGreen, trackColor = KitchenSoft)
            Text("Tích cả những nguyên liệu đã có ở nhà.", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
            OutlinedButton(onClick = { actions.copy(vm.shoppingText()) }, modifier = Modifier.fillMaxWidth()) { Text("Sao chép danh sách") }
        } }
        ingredients.groupBy { it.group }.forEach { (group, items) ->
            item { KitchenCard {
                Text(group, style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                items.forEach { item ->
                    val done = item.key in checked
                    Row(Modifier.fillMaxWidth().clickable { vm.checkIngredient(item.key) }.testTag("ingredient-${item.name}").padding(vertical = 5.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = done, onCheckedChange = { vm.checkIngredient(item.key) })
                        Text(item.name, modifier = Modifier.weight(1f), textDecoration = if (done) TextDecoration.LineThrough else null,
                            color = if (done) KitchenMuted else KitchenInk)
                        Text("${PlannerEngine.quantity(item.amount)} ${item.unit}", style = MaterialTheme.typography.bodyMedium, color = KitchenGreen)
                    }
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
        item { KitchenCard {
            Text("Dọn cơm lúc ${vm.state.profile.time(vm.state.meal)}", style = MaterialTheme.typography.headlineSmall, color = KitchenGreen)
            Text("Bắt đầu ${PlannerEngine.time(tasks.first().at)} · Khoảng ${vm.engine.minutes(vm.menu)} phút", color = KitchenMuted)
            Text("Lịch dự kiến cho bếp có 2 vùng nấu. Đã xong ${tasks.count { it.key in done }}/${tasks.size} việc.", style = MaterialTheme.typography.bodyMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedButton(onClick = vm::openReminders, modifier = Modifier.weight(1f)) { Text("Chỉnh giờ") }
                FilledTonalButton(onClick = { actions.calendar(tasks) }, modifier = Modifier.weight(1f)) { Text("Thêm vào lịch") }
            }
        } }
        items(tasks, key = { it.key }) { task ->
            KitchenCard(Modifier.testTag("task-${task.key}")) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(PlannerEngine.time(task.at), fontWeight = FontWeight.SemiBold, color = KitchenGreen, modifier = Modifier.weight(1f))
                    Checkbox(task.key in done, onCheckedChange = { vm.checkTask(task.key) }, modifier = Modifier.semantics { contentDescription = "Đã xong ${task.title}" })
                }
                Text(task.title, style = MaterialTheme.typography.titleMedium, textDecoration = if (task.key in done) TextDecoration.LineThrough else null)
                Text(task.detail, style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
                if (task.recipeId != null || task.minutes > 0) Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    task.recipeId?.let { id -> TextButton(onClick = { vm.openRecipe(id) }, modifier = Modifier.weight(1f)) { Text("Xem từng bước") } }
                    if (task.minutes > 0) OutlinedButton(onClick = {
                        vm.startTimer(task.minutes, task.title)
                        if (!vm.notifications) actions.notifications()
                    }, modifier = Modifier.weight(1f)) { Text("Hẹn ${task.minutes} phút") }
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
            Text("Bạn chưa lưu món nào.")
            Button(onClick = { vm.navigate(Screen.TODAY) }) { Text("Xem thực đơn") }
        } }
        items(favorites, key = { it.id }) { recipe -> RecipeCard(vm, recipe) }
    }
}
