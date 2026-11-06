package vn.team03.bepnha

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SheetTitle("Công thức nhà mình", vm)
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(recipe.emoji, fontSize = 32.sp, modifier = Modifier.clearAndSetSemantics { })
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(recipe.name, style = MaterialTheme.typography.headlineSmall)
                Text("${recipe.minutes} phút cho ${vm.state.profile.people} người", color = KitchenMuted, style = MaterialTheme.typography.bodySmall)
            }
        }
        KitchenOutlinedButton(onClick = vm::openAssistant, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Hỏi về món này") }
        Text(recipe.note, color = KitchenMuted)
        Text("Chuẩn bị nguyên liệu", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        recipe.ingredients.forEach { item ->
            KitchenAmount(item.name, "${PlannerEngine.quantity(item.amount * vm.state.profile.people / 4.0)} ${item.unit}")
            HorizontalDivider(color = KitchenLine)
        }
        Text("Cách nấu từng bước", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        recipe.steps.forEachIndexed { index, step ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Surface(shape = CircleShape, color = KitchenSoft, contentColor = KitchenGreen,
                    modifier = Modifier.sizeIn(minWidth = 40.dp, minHeight = 40.dp)) {
                    Box(Modifier.padding(4.dp), contentAlignment = Alignment.Center) { Text("${index + 1}", fontWeight = FontWeight.SemiBold) }
                }
                Text(step, modifier = Modifier.weight(1f))
            }
        }
        Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Mẹo nhỏ của Bếp Nhà", style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                Text(recipe.tip)
            }
        }
        Text("Thời gian là gợi ý. Bạn cần kiểm tra độ chín thực tế trước khi dùng.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        KitchenButton(onClick = {
            vm.startTimer(recipe.minutes, recipe.name)
            if (!vm.notifications) actions.notifications()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Hẹn giờ ${recipe.minutes} phút") }
    }
}

@Composable fun SwapSheet(vm: PlannerViewModel, index: Int) {
    val current = vm.menu[index]
    val choices = vm.engine.recipes.values.filter { it.kind == current.kind && it.id != current.id }
    LazyColumn(Modifier.fillMaxWidth().testTag("swap-list"),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item { SheetTitle("Đổi món", vm) }
        item { Text("Thay ${current.name.lowercase()} bằng món nào? Nguyên liệu và lịch nấu sẽ tự cập nhật.", color = KitchenMuted) }
        items(choices, key = { it.id }) { recipe ->
            OutlinedCard(onClick = { vm.swap(index, recipe.id) }, modifier = Modifier.fillMaxWidth().testTag("choose-${recipe.id}"),
                shape = RoundedCornerShape(16.dp), colors = CardDefaults.outlinedCardColors(containerColor = Color.White)) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                    Text("${recipe.minutes} phút", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                    Text("Ước tính ${PlannerEngine.money((recipe.cost * vm.state.profile.people / 4.0).toLong())}",
                        style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                }
            }
        }
    }
}

@Composable fun PermissionInfo(vm: PlannerViewModel, actions: NativeActions) {
    Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(if (vm.notifications) "Thông báo đã bật" else "Cho phép thông báo để nhận nhắc nấu",
                style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
            if (!vm.notifications) KitchenOutlinedButton(onClick = actions.notifications,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Cho phép thông báo") }
            Text(if (vm.exactAlarms) "Quyền nhắc đúng giờ đã bật." else "Android có thể trì hoãn nhắc. Bạn có thể cấp quyền nhắc đúng giờ để hạn chế việc này.",
                style = MaterialTheme.typography.bodySmall)
            if (!vm.exactAlarms) KitchenOutlinedButton(onClick = actions.exactAlarms,
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Bật quyền nhắc đúng giờ") }
        }
    }
}

@Composable fun ReminderSheet(vm: PlannerViewModel, actions: NativeActions) {
    var timeText by rememberSaveable(vm.state.key) { mutableStateOf(vm.state.profile.time(vm.state.meal).toString()) }
    var enabled by rememberSaveable(vm.state.key) { mutableStateOf(vm.reminderEnabled) }
    val time = LocalTime.parse(timeText)
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 16.dp).padding(bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        SheetTitle("Nhắc mình vào bếp", vm)
        Text("${vm.state.meal.title}\n${PlannerEngine.fullDate(vm.state.date)}", color = KitchenMuted)
        Text("Giờ ăn áp dụng cho mọi ${vm.state.meal.title.lowercase()}, không chỉ ngày này.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        Text("Cả nhà ăn lúc mấy giờ?", style = MaterialTheme.typography.titleMedium)
        KitchenOutlinedButton(onClick = { actions.pickTime(time) { timeText = it.toString() } },
            modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp).testTag("meal-time")) {
            Text(timeText, fontSize = 28.sp, fontWeight = FontWeight.SemiBold)
        }
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { enabled = it }).testTag("reminder-enabled"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nhắc khi đến giờ chuẩn bị", modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = null)
        }
        PermissionInfo(vm, actions)
        Text("Lịch nhắc dùng được khi đóng ứng dụng. Chế độ tiết kiệm pin hoặc dừng bắt buộc có thể ảnh hưởng thông báo.",
            style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        KitchenButton(onClick = {
            vm.saveReminder(time, enabled)
            if (enabled && !vm.notifications) actions.notifications()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Lưu lịch nhắc") }
        KitchenOutlinedButton(onClick = {
            val profile = if (vm.state.meal == Meal.LUNCH) vm.state.profile.copy(lunch = time) else vm.state.profile.copy(dinner = time)
            actions.calendar(vm.engine.timeline(vm.state.copy(profile = profile)))
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Thêm vào lịch điện thoại") }
    }
}

@Composable fun FamilyScreen(vm: PlannerViewModel, actions: NativeActions) {
    val original = vm.state.profile
    val focus = LocalFocusManager.current
    var name by rememberSaveable(original.name) { mutableStateOf(original.name) }
    var people by rememberSaveable(original.people) { mutableIntStateOf(original.people) }
    var lunchText by rememberSaveable(original.lunch.toString()) { mutableStateOf(original.lunch.toString()) }
    var dinnerText by rememberSaveable(original.dinner.toString()) { mutableStateOf(original.dinner.toString()) }
    val lunch = LocalTime.parse(lunchText)
    val dinner = LocalTime.parse(dinnerText)
    LazyColumn(Modifier.fillMaxSize().imePadding().testTag("family-list"), contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)) {
        item { SectionHeading("Gia đình của mình", "Điều chỉnh khẩu phần và giờ ăn.") }
        item { KitchenCard {
            OutlinedTextField(value = name, onValueChange = { name = it.take(35) }, label = { Text("Tên gia đình") },
                singleLine = true, modifier = Modifier.fillMaxWidth().testTag("family-name"))
            Text("Nhà mình có mấy người ăn?", style = MaterialTheme.typography.titleMedium)
            Text("$people người", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag("family-people"))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                KitchenOutlinedButton(onClick = { if (people > 1) people-- }, enabled = people > 1,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics { contentDescription = "Giảm số người ăn" }) { Text("−", fontSize = 24.sp) }
                KitchenOutlinedButton(onClick = { if (people < 12) people++ }, enabled = people < 12,
                    modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics { contentDescription = "Tăng số người ăn" }) { Text("+", fontSize = 24.sp) }
            }
            KitchenActions(
                { TimeField("Giờ ăn trưa", lunch, it) { actions.pickTime(lunch) { lunchText = it.toString() } } },
                { TimeField("Giờ ăn tối", dinner, it) { actions.pickTime(dinner) { dinnerText = it.toString() } } },
            )
            KitchenButton(onClick = { focus.clearFocus(); vm.saveFamily(name, people, lunch, dinner) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Lưu thông tin gia đình") }
        } }
        item { PermissionInfo(vm, actions) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dữ liệu của nhà mình", style = MaterialTheme.typography.titleMedium)
                Text("Thực đơn và công thức lưu trên thiết bị. Chưa có đồng bộ giữa các máy.", color = KitchenMuted)
                Text("Dùng AI sẽ gửi câu hỏi và ngữ cảnh bữa ăn đến máy chủ. Tìm trong 14 công thức có sẵn dùng được khi mất mạng.",
                    style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                KitchenOutlinedButton(onClick = { vm.navigate(Screen.FAVORITES) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Xem món đã lưu") }
            }
        }
    }
}

@Composable private fun TimeField(label: String, time: LocalTime, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        KitchenOutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(time.toString(), fontSize = 24.sp) }
    }
}
