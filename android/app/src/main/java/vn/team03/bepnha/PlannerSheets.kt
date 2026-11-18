@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.team03.bepnha

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
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
import androidx.compose.ui.semantics.selected
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
        FilledTonalIconButton(onClick = vm::dismissSheet) { Icon(Icons.Outlined.Close, "Đóng") }
    }
}

@Composable fun RecipeSheet(vm: PlannerViewModel, id: String, actions: NativeActions) {
    val recipe = vm.engine.recipes.getValue(id)
    val people = vm.state.profile.people
    Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp)) {
        SheetTitle("Công thức", vm)
        Column(
            Modifier.fillMaxWidth().weight(1f, fill = false).verticalScroll(rememberScrollState()).padding(top = 8.dp, bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Surface(color = KitchenSoft, contentColor = KitchenGreen, shape = MaterialTheme.shapes.large) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        RecipePortrait(recipe, 82.dp)
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                            RecipeKindLabel(recipe.kind)
                            Text(recipe.name, style = MaterialTheme.typography.headlineSmall)
                            Text("${recipe.minutes} phút · $people người", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
                        }
                    }
                    Text(recipe.note, style = MaterialTheme.typography.bodyMedium)
                }
            }
            KitchenCard {
                KitchenSection("Chuẩn bị nguyên liệu")
                recipe.ingredients.forEachIndexed { index, item ->
                    if (index > 0) HorizontalDivider(color = KitchenLine)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(item.name, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        Text("${PlannerEngine.quantity(item.amount * people / 4.0)} ${item.unit}",
                            style = MaterialTheme.typography.labelLarge, color = KitchenGreen)
                    }
                }
            }
            KitchenSection("Cách nấu từng bước")
            recipe.steps.forEachIndexed { index, step ->
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(shape = MaterialTheme.shapes.small, color = KitchenPeach, contentColor = KitchenTerracotta) {
                        Text("${index + 1}", Modifier.padding(horizontal = 10.dp, vertical = 5.dp), style = MaterialTheme.typography.labelLarge)
                    }
                    Text(step, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge)
                }
            }
            Surface(color = KitchenSoft, shape = MaterialTheme.shapes.medium) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Mẹo nấu", style = MaterialTheme.typography.titleMedium, color = KitchenGreen)
                    Text(recipe.tip, style = MaterialTheme.typography.bodyMedium)
                }
            }
            Text("Thời gian là gợi ý. Bạn cần kiểm tra độ chín thực tế trước khi dùng.", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
            Button(onClick = {
                vm.startTimer(recipe.minutes, recipe.name)
                if (!vm.notifications) actions.notifications()
            }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) { Text("Hẹn giờ ${recipe.minutes} phút") }
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
    // Dishes that fit the family's diet profile come first; the others say why they do not fit.
    val choices = vm.engine.recipes.values.filter { it.kind == current.kind && it.id != current.id }
    LazyColumn(Modifier.fillMaxWidth().testTag("swap-list"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        item { SheetTitle("Đổi món", vm) }
        item { Text("Chọn món thay cho ${current.name.lowercase(PlannerEngine.locale)}. Danh sách đi chợ và lịch nấu sẽ tự cập nhật.", color = KitchenMuted) }
        items(choices, key = { it.id }) { recipe ->
            OutlinedCard(onClick = { vm.swap(index, recipe.id) }, modifier = Modifier.fillMaxWidth().testTag("choose-${recipe.id}"),
                shape = MaterialTheme.shapes.large, border = BorderStroke(1.dp, KitchenLine),
                colors = CardDefaults.outlinedCardColors(containerColor = KitchenSurface)) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    RecipePortrait(recipe, 60.dp)
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                        Text("${recipe.minutes} phút · ~${PlannerEngine.money((recipe.cost * vm.state.profile.people / 4.0).toLong())}", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
                    }
                    Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp), tint = KitchenGreen)
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
    var breakfastText by rememberSaveable(original.breakfast.toString()) { mutableStateOf(original.breakfast.toString()) }
    val breakfast = LocalTime.parse(breakfastText)
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
            TimeField("Giờ ăn sáng", breakfast, Modifier.fillMaxWidth()) { actions.pickTime(breakfast) { breakfastText = it.toString() } }
            KitchenActions(
                { TimeField("Giờ ăn trưa", lunch, it) { actions.pickTime(lunch) { lunchText = it.toString() } } },
                { TimeField("Giờ ăn tối", dinner, it) { actions.pickTime(dinner) { dinnerText = it.toString() } } },
            )
            KitchenButton(onClick = { focus.clearFocus(); vm.saveFamily(name, people, breakfast, lunch, dinner) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Lưu thông tin gia đình") }
        } }
        item { DietCard(vm) }
        item { VoiceSettingsCard(vm) }
        item { ShoppingReminderCard(vm, actions) }
        item { PermissionInfo(vm, actions) }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Dữ liệu của nhà mình", style = MaterialTheme.typography.titleMedium)
                Text("Thực đơn và công thức lưu trên thiết bị. Chưa có đồng bộ giữa các máy.", color = KitchenMuted)
                Text("Dùng AI sẽ gửi câu hỏi và ngữ cảnh bữa ăn đến máy chủ. Tìm trong công thức có sẵn dùng được khi mất mạng.",
                    style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                KitchenOutlinedButton(onClick = { vm.navigate(Screen.FAVORITES) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Xem món đã lưu") }
            }
        }
    }
}

@Composable private fun DietCard(vm: PlannerViewModel) {
    val saved = vm.state.diet
    var tags by remember(saved) { mutableStateOf(saved.tags) }
    var allergens by remember(saved) { mutableStateOf(saved.allergens) }
    var avoid by remember(saved) { mutableStateOf(saved.avoid) }
    var maxMinutes by remember(saved) { mutableStateOf(saved.maxMinutes) }
    var term by rememberSaveable { mutableStateOf("") }
    var termError by remember { mutableStateOf<String?>(null) }
    @Composable fun ToggleRow(label: String, on: Boolean, tag: String, onChange: (Boolean) -> Unit) {
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp).toggleable(value = on, role = Role.Checkbox, onValueChange = onChange).testTag(tag),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Checkbox(checked = on, onCheckedChange = null)
            Text(label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
        }
    }
    KitchenCard {
        Text("Ăn uống của gia đình", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Text("Gợi ý món, thực đơn tuần và trợ lý sẽ luôn theo những điều này. Không thay cho lời khuyên của bác sĩ.",
            style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        Text("Mọi món cần", style = MaterialTheme.typography.bodySmall)
        DietProfile.TAG_LABELS.forEach { (tag, label) ->
            ToggleRow(label.replaceFirstChar { it.titlecase() }, tag in tags, "diet-tag-$tag") { tags = if (it) tags + tag else tags - tag }
        }
        Text("Dị ứng hoặc không ăn", style = MaterialTheme.typography.bodySmall)
        DietProfile.ALLERGEN_LABELS.forEach { (code, label) ->
            ToggleRow(label.replaceFirstChar { it.titlecase() }, code in allergens, "diet-allergen-$code") { allergens = if (it) allergens + code else allergens - code }
        }
        avoid.sorted().forEach { name ->
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Tránh $name", modifier = Modifier.weight(1f))
                TextButton(onClick = { avoid = avoid - name }, modifier = Modifier.heightIn(min = 48.dp)) { Text("Bỏ") }
            }
        }
        OutlinedTextField(value = term, onValueChange = { term = it.take(40); termError = null }, label = { Text("Thêm nguyên liệu cần tránh") },
            placeholder = { Text("Ví dụ: hành, sả, thịt bò") }, singleLine = true, isError = termError != null,
            supportingText = termError?.let { { Text(it) } }, modifier = Modifier.fillMaxWidth().testTag("diet-avoid-input"))
        KitchenOutlinedButton(onClick = {
            val found = vm.resolveDietTerm(term)
            when {
                found == null -> termError = "Chưa có nguyên liệu này trong công thức."
                found.first != null -> { allergens = allergens + found.first!!; term = "" }
                else -> { avoid = avoid + found.second; term = "" }
            }
        }, enabled = term.isNotBlank(), modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Thêm vào danh sách tránh") }
        Text("Thời gian nấu mỗi bữa", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(null to "Không giới hạn", 30 to "30 phút", 45 to "45 phút").forEach { (value, label) ->
                val modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics { selected = maxMinutes == value }
                if (maxMinutes == value) KitchenButton(onClick = { maxMinutes = value }, modifier = modifier) { Text(label) }
                else KitchenOutlinedButton(onClick = { maxMinutes = value }, modifier = modifier) { Text(label) }
            }
        }
        val draft = saved.copy(tags = tags, allergens = allergens, avoid = avoid, maxMinutes = maxMinutes)
        val outside = vm.mealsOutsideDiet(draft)
        if (outside > 0) Text("$outside bữa trong tuần đang có món chưa hợp. Bạn có thể nhờ trợ lý “lên lại thực đơn tuần theo hồ sơ”.",
            style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        KitchenButton(onClick = { vm.saveDiet(draft) }, enabled = draft != saved,
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("diet-save")) { Text("Lưu hồ sơ ăn uống") }
    }
}

@Composable private fun VoiceSettingsCard(vm: PlannerViewModel) {
    val saved = vm.state.voice
    var address by rememberSaveable(saved) { mutableStateOf(saved.addressAs) }
    var readAloud by rememberSaveable(saved) { mutableStateOf(saved.readAloud) }
    var rate by rememberSaveable(saved) { mutableFloatStateOf(saved.rate) }
    KitchenCard {
        Text("Trợ lý nói chuyện", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        OutlinedTextField(value = address, onValueChange = { address = it.take(20) }, label = { Text("Trợ lý gọi bạn là") },
            placeholder = { Text("Ví dụ: bác, cô, bà, bạn") }, singleLine = true,
            modifier = Modifier.fillMaxWidth().testTag("voice-address"))
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = readAloud, role = Role.Switch, onValueChange = { readAloud = it }).testTag("voice-read-aloud"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Đọc to câu trả lời", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = readAloud, onCheckedChange = null)
        }
        Text("Tốc độ đọc", style = MaterialTheme.typography.bodySmall)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            VoiceSettings.RATES.forEach { (value, label) ->
                val selected = rate == value
                val modifier = Modifier.weight(1f).heightIn(min = 56.dp).semantics { this.selected = selected }
                if (selected) KitchenButton(onClick = { rate = value }, modifier = modifier) { Text(label) }
                else KitchenOutlinedButton(onClick = { rate = value }, modifier = modifier) { Text(label) }
            }
        }
        KitchenOutlinedButton(onClick = { vm.previewVoice(address, rate) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("voice-preview")) { Text("Nghe thử") }
        KitchenButton(onClick = { vm.saveVoiceSettings(address, readAloud, rate) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("voice-save")) { Text("Lưu cách nói chuyện") }
    }
}

@Composable private fun ShoppingReminderCard(vm: PlannerViewModel, actions: NativeActions) {
    val saved = vm.state.shopping
    var enabled by rememberSaveable(saved) { mutableStateOf(saved.enabled) }
    var timeText by rememberSaveable(saved) { mutableStateOf(saved.time.toString()) }
    val time = LocalTime.parse(timeText)
    KitchenCard {
        Text("Nhắc đi chợ", style = MaterialTheme.typography.titleMedium, modifier = Modifier.semantics { heading() })
        Row(Modifier.fillMaxWidth().heightIn(min = 56.dp)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = { enabled = it }).testTag("shopping-enabled"),
            verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Nhắc đi chợ mỗi sáng", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            Switch(checked = enabled, onCheckedChange = null)
        }
        Text("Thông báo liệt kê đồ còn thiếu cho các bữa hôm đó. Đã mua đủ thì không nhắc.",
            style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        TimeField("Giờ nhắc đi chợ", time, Modifier.fillMaxWidth().testTag("shopping-time")) {
            actions.pickTime(time) { timeText = it.toString() }
        }
        KitchenButton(onClick = {
            vm.saveShoppingReminder(enabled, time)
            if (enabled && !vm.notifications) actions.notifications()
        }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("shopping-save")) { Text("Lưu nhắc đi chợ") }
    }
}

@Composable private fun TimeField(label: String, time: LocalTime, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        KitchenOutlinedButton(onClick = onClick, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text(time.toString(), fontSize = 24.sp) }
    }
}
