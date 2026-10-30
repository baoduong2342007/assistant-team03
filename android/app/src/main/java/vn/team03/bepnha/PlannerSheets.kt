package vn.team03.bepnha

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalTime

@Composable private fun SheetTitle(title: String, vm: PlannerViewModel) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        IconButton(onClick = vm::dismissSheet) { Icon(Icons.Outlined.Close, "Đóng") }
    }
}

@Composable fun RecipeSheet(vm: PlannerViewModel, id: String, actions: NativeActions) {
    val recipe = vm.engine.recipes.getValue(id)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SheetTitle("Công thức nhà mình", vm)
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Text(recipe.emoji, fontSize = 46.sp)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(recipe.name, style = MaterialTheme.typography.headlineSmall)
                Text("${recipe.minutes} phút · ${vm.state.profile.people} người · Dễ nấu", color = KitchenMuted, style = MaterialTheme.typography.bodyMedium)
            }
        }
        Text(recipe.note, color = KitchenMuted)
        Text("Chuẩn bị nguyên liệu", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        recipe.ingredients.forEach { item ->
            Row(Modifier.fillMaxWidth().padding(vertical = 3.dp), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(item.name, modifier = Modifier.weight(1f))
                Text("${PlannerEngine.quantity(item.amount * vm.state.profile.people / 4.0)} ${item.unit}", color = KitchenGreen)
            }
            HorizontalDivider(color = KitchenLine)
        }
        Text("Cách nấu từng bước", style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
        recipe.steps.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Surface(shape = CircleShape, color = KitchenSoft, contentColor = KitchenGreen, modifier = Modifier.size(32.dp)) {
                    Box(contentAlignment = Alignment.Center) { Text("${index + 1}", fontWeight = FontWeight.SemiBold) }
                }
                Text(step, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
            }
        }
        Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mẹo nhỏ của Bếp Nhà", style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                Text(recipe.tip)
            }
        }
        Text("Thời gian là gợi ý. Bạn cần kiểm tra độ chín thực tế trước khi dùng.", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
        Button(onClick = {
            vm.startTimer(recipe.minutes, recipe.name)
            if (!vm.notifications) actions.notifications()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Hẹn giờ ${recipe.minutes} phút") }
    }
}

@Composable fun SwapSheet(vm: PlannerViewModel, index: Int) {
    val current = vm.menu[index]
    val choices = vm.engine.recipes.values.filter { it.kind == current.kind && it.id != current.id }
    LazyColumn(Modifier.fillMaxWidth().testTag("swap-list"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SheetTitle("Đổi một món, thêm một vị", vm) }
        item { Text("Thay ${current.name.lowercase()} bằng món nào? Nguyên liệu và lịch nấu sẽ tự cập nhật.", color = KitchenMuted) }
        items(choices, key = { it.id }) { recipe ->
            OutlinedCard(onClick = { vm.swap(index, recipe.id) }, modifier = Modifier.fillMaxWidth().testTag("choose-${recipe.id}"),
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text(recipe.emoji, fontSize = 34.sp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                        Text("${recipe.minutes} phút · ~${PlannerEngine.money((recipe.cost * vm.state.profile.people / 4.0).toLong())}", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
                    }
                }
            }
        }
    }
}

@Composable fun PermissionInfo(vm: PlannerViewModel, actions: NativeActions) {
    Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (vm.notifications) "Thông báo Android đã bật" else "Cho phép thông báo để nhận nhắc nấu", style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
            if (!vm.notifications) OutlinedButton(onClick = actions.notifications, modifier = Modifier.fillMaxWidth()) { Text("Cho phép thông báo") }
            Text(if (vm.exactAlarms) "Quyền nhắc đúng giờ đã bật." else "Android có thể trì hoãn nhắc. Bạn có thể cấp quyền nhắc đúng giờ để hạn chế việc này.", style = MaterialTheme.typography.bodyMedium)
            if (!vm.exactAlarms) OutlinedButton(onClick = actions.exactAlarms, modifier = Modifier.fillMaxWidth()) { Text("Bật quyền nhắc đúng giờ") }
        }
    }
}

@Composable fun ReminderSheet(vm: PlannerViewModel, actions: NativeActions) {
    var time by remember(vm.state.key) { mutableStateOf(vm.state.profile.time(vm.state.meal)) }
    var enabled by remember(vm.state.key) { mutableStateOf(vm.reminderEnabled) }
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(bottom = 30.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        SheetTitle("Nhắc mình vào bếp", vm)
        Text("${vm.state.meal.title} · ${PlannerEngine.fullDate(vm.state.date)}", color = KitchenMuted)
        Text("Cả nhà ăn lúc mấy giờ?", style = MaterialTheme.typography.titleMedium)
        OutlinedButton(onClick = { actions.pickTime(time) { time = it } }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("meal-time")) {
            Text(time.toString(), fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nhắc khi đến giờ chuẩn bị bữa ăn", modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = { enabled = it }, modifier = Modifier.testTag("reminder-enabled"))
        }
        PermissionInfo(vm, actions)
        Text("Lịch nhắc được đặt trên Android để dùng khi đóng ứng dụng. Chế độ tiết kiệm pin và dừng bắt buộc ứng dụng có thể ảnh hưởng thông báo.", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
        Button(onClick = {
            vm.saveReminder(time, enabled)
            if (enabled && !vm.notifications) actions.notifications()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Lưu lịch nhắc") }
        OutlinedButton(onClick = {
            val profile = if (vm.state.meal == Meal.LUNCH) vm.state.profile.copy(lunch = time) else vm.state.profile.copy(dinner = time)
            actions.calendar(vm.engine.timeline(vm.state.copy(profile = profile)))
        }, modifier = Modifier.fillMaxWidth()) { Text("Thêm vào lịch điện thoại") }
    }
}

@Composable fun FamilyScreen(vm: PlannerViewModel, actions: NativeActions) {
    val original = vm.state.profile
    var name by remember(original) { mutableStateOf(original.name) }
    var people by remember(original) { mutableIntStateOf(original.people) }
    var lunch by remember(original) { mutableStateOf(original.lunch) }
    var dinner by remember(original) { mutableStateOf(original.dinner) }
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
        item { SectionHeading("Hiểu nhà mình, nấu vừa ý", "Điều chỉnh khẩu phần và giờ ăn cho gia đình.") }
        item { KitchenCard {
            OutlinedTextField(value = name, onValueChange = { name = it.take(35) }, label = { Text("Tên gia đình") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Text("Nhà mình có mấy người ăn?", style = MaterialTheme.typography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                OutlinedButton(onClick = { if (people > 1) people-- }, enabled = people > 1) { Text("−", fontSize = 24.sp) }
                Text("$people người", style = MaterialTheme.typography.titleLarge, modifier = Modifier.testTag("family-people"))
                OutlinedButton(onClick = { if (people < 12) people++ }, enabled = people < 12) { Text("+", fontSize = 24.sp) }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                TimeField("Giờ ăn trưa", lunch, Modifier.weight(1f)) { actions.pickTime(lunch) { lunch = it } }
                TimeField("Giờ ăn tối", dinner, Modifier.weight(1f)) { actions.pickTime(dinner) { dinner = it } }
            }
            Button(onClick = { vm.saveFamily(name, people, lunch, dinner) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Lưu thông tin gia đình") }
        } }
        item { PermissionInfo(vm, actions) }
        item { KitchenCard {
            Text("Dữ liệu của nhà mình", style = MaterialTheme.typography.titleMedium)
            Text("Thực đơn và công thức được lưu trên thiết bị, dùng được offline. Chưa có đồng bộ giữa các máy.", color = KitchenMuted)
            Text("Trợ lý hiện tra cứu 14 công thức có sẵn, chưa kết nối AI tạo sinh. Chi phí và thời gian nấu là ước tính.", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
            OutlinedButton(onClick = { vm.navigate(Screen.FAVORITES) }, modifier = Modifier.fillMaxWidth()) { Text("Xem món đã lưu") }
        } }
    }
}
@Composable private fun TimeField(label: String, time: LocalTime, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        OutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text(time.toString(), fontSize = 20.sp) }
    }
}

@Composable fun AssistantSheet(vm: PlannerViewModel) {
    var question by remember { mutableStateOf("") }
    var history by remember { mutableStateOf(emptyList<Pair<String, AssistantAnswer>>()) }
    val scroll = rememberLazyListState()
    fun ask(text: String) {
        if (text.isBlank()) return
        history = history + (text to vm.engine.answer(text, vm.state)); question = ""
    }
    LaunchedEffect(history.size) { if (history.isNotEmpty()) scroll.animateScrollToItem(history.size) }
    Column(Modifier.fillMaxWidth().fillMaxHeight(0.92f).imePadding().padding(horizontal = 20.dp).padding(bottom = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SheetTitle("Hỏi Bếp Nhà", vm)
        Text("Gợi ý từ 14 công thức có sẵn.", color = KitchenMuted, style = MaterialTheme.typography.bodyMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            AssistChip(onClick = { ask("Món nấu nhanh") }, label = { Text("Món nhanh") })
            AssistChip(onClick = { ask("Tiết kiệm hơn") }, label = { Text("Tiết kiệm") })
            AssistChip(onClick = { ask("Nấu món nào trước?") }, label = { Text("Lịch nấu") })
        }
        LazyColumn(Modifier.weight(1f), state = scroll, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            item { Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
                Text("Bạn có thể hỏi tên món, cách nấu, món nhanh hoặc thứ tự vào bếp theo thực đơn đã chọn.", Modifier.padding(16.dp))
            } }
            items(history) { (prompt, answer) ->
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(color = Color.White, shape = RoundedCornerShape(16.dp)) { Text(prompt, Modifier.padding(16.dp), fontWeight = FontWeight.Medium) }
                    Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
                        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(answer.text)
                            answer.recipes.forEach { id ->
                                TextButton(onClick = { vm.openRecipe(id) }) { Text("Xem ${vm.engine.recipes.getValue(id).name.lowercase()}") }
                            }
                        }
                    }
                }
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedTextField(value = question, onValueChange = { question = it.take(300) }, modifier = Modifier.weight(1f),
                placeholder = { Text("Bạn muốn nấu gì?") }, maxLines = 3,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send), keyboardActions = KeyboardActions(onSend = { ask(question) }))
            FilledIconButton(onClick = { ask(question) }, enabled = question.isNotBlank(), modifier = Modifier.size(52.dp)) { Icon(Icons.Outlined.Send, "Gửi câu hỏi") }
        }
    }
}
