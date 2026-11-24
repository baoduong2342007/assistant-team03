@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.team03.bepnha

import androidx.compose.foundation.BorderStroke
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

private val fridgeStaples = listOf("Thịt heo", "Thịt gà", "Cá", "Tôm", "Trứng", "Đậu hũ", "Cà chua",
    "Rau muống", "Cải xanh", "Bắp cải", "Cà rốt", "Bí đỏ", "Bông cải", "Dưa leo")

/** "Hỏi Bếp Nhà": the fridge-to-meal feature, a chat that can change the menu, and several timers. */
@Composable fun AskScreen(vm: PlannerViewModel, actions: NativeActions) {
    val panels = rememberSaveableStateHolder()
    Column(Modifier.fillMaxSize().testTag("ask-screen")) {
        Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 10.dp, bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeading("Hỏi Bếp Nhà", "Gợi ý từ ${vm.engine.recipes.size} công thức có sẵn, dùng được khi không có mạng.")
            KitchenSegmented(AskTab.entries.map { it.title }, vm.askTab.ordinal, { vm.selectAskTab(AskTab.entries[it]) },
                Modifier.fillMaxWidth(), tag = { "ask-${AskTab.entries[it].name.lowercase()}" })
        }
        KitchenContentTransition(vm.askTab, "kitchen panels", Modifier.weight(1f).fillMaxWidth()) { tab ->
            panels.SaveableStateProvider(tab.name) {
                when (tab) {
                    AskTab.FRIDGE -> FridgePanel(vm, actions)
                    AskTab.CHAT -> Unit
                    AskTab.TIMER -> TimerPanel(vm, actions)
                }
            }
        }
    }
}

@Composable private fun MicButton(description: String, onClick: () -> Unit) {
    FilledTonalIconButton(onClick = onClick, modifier = Modifier.size(52.dp).semantics { contentDescription = description },
        colors = IconButtonDefaults.filledTonalIconButtonColors(containerColor = KitchenPeach, contentColor = KitchenTerracotta)) {
        KitchenIcon(KitchenGlyph.MIC, tint = KitchenTerracotta)
    }
}

// ---- Tủ lạnh ---------------------------------------------------------------------------------

@Composable private fun FridgePanel(vm: PlannerViewModel, actions: NativeActions) {
    val trays = vm.fridgeTrays
    LazyColumn(Modifier.fillMaxSize().testTag("fridge-list"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "input") {
            Surface(color = KitchenGreen, contentColor = Color.White, shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        KitchenIcon(KitchenGlyph.FRIDGE, tint = KitchenGold)
                        Text("Tủ lạnh hôm nay có gì?", style = MaterialTheme.typography.titleLarge)
                    }
                    Text("Chạm để chọn, gõ hoặc nói thêm nguyên liệu. Gia vị cơ bản coi như đã có.",
                        style = MaterialTheme.typography.bodyMedium, color = KitchenOnGreen)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = vm.fridgeDraft, onValueChange = { vm.fridgeDraft = it.take(120) },
                            modifier = Modifier.weight(1f).testTag("fridge-input"), singleLine = true,
                            placeholder = { Text("Ví dụ: sườn, cà tím") }, shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(focusedContainerColor = KitchenSurface, unfocusedContainerColor = KitchenSurface,
                                focusedTextColor = KitchenInk, unfocusedTextColor = KitchenInk, focusedBorderColor = KitchenGold,
                                unfocusedBorderColor = Color.Transparent, cursorColor = KitchenGreen),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { vm.addFridgeText(vm.fridgeDraft) }),
                        )
                        if (vm.fridgeDraft.isNotBlank()) FilledIconButton(onClick = { vm.addFridgeText(vm.fridgeDraft) }, modifier = Modifier.size(52.dp),
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = KitchenGold, contentColor = KitchenGreen)) {
                            Icon(Icons.Outlined.Add, "Thêm nguyên liệu")
                        } else MicButton("Nói nguyên liệu trong tủ lạnh") { actions.listen() }
                    }
                    val custom = vm.fridgeItems.filter { item -> fridgeStaples.none { it.equals(item, ignoreCase = true) } }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (fridgeStaples + custom).forEach { item ->
                            val selected = vm.fridgeItems.any { it.equals(item, ignoreCase = true) }
                            FilterChip(
                                selected = selected, onClick = { vm.toggleFridgeItem(item) }, label = { Text(item) },
                                leadingIcon = if (selected) { { Icon(Icons.Outlined.Check, null, Modifier.size(18.dp)) } } else null,
                                modifier = Modifier.testTag("fridge-$item"),
                                colors = FilterChipDefaults.filterChipColors(containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White, selectedContainerColor = KitchenGold, selectedLabelColor = KitchenGreen,
                                    selectedLeadingIconColor = KitchenGreen),
                                border = FilterChipDefaults.filterChipBorder(enabled = true, selected = selected,
                                    borderColor = Color.White.copy(alpha = 0.3f), selectedBorderColor = KitchenGold),
                            )
                        }
                    }
                    if (vm.fridgeItems.isNotEmpty()) TextButton(onClick = vm::clearFridge,
                        colors = ButtonDefaults.textButtonColors(contentColor = KitchenGold)) { Text("Bỏ chọn tất cả") }
                }
            }
        }
        item(key = "target") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Gợi ý cho ${PlannerEngine.dayLabel(vm.state.date, vm.today)}", style = MaterialTheme.typography.titleMedium)
                KitchenSegmented(Meal.entries.map { it.title }, vm.fridgeMeal.ordinal, { vm.selectFridgeMeal(Meal.entries[it]) },
                    Modifier.fillMaxWidth(), tag = { "fridge-meal-${Meal.entries[it].value}" })
            }
        }
        when {
            vm.fridgeItems.isEmpty() -> item(key = "empty") {
                HintCard("Chọn ít nhất một nguyên liệu, Bếp Nhà sẽ ghép thành mâm cơm đủ món mặn, canh và rau.")
            }
            trays.isEmpty() -> item(key = "none") {
                HintCard("Chưa có món hợp với ${vm.fridgeItems.joinToString { it.lowercase(PlannerEngine.locale) }} trong công thức có sẵn. " +
                    "Bạn thử chọn thêm thịt, cá, trứng hoặc rau.")
            }
            else -> itemsIndexed(trays, key = { _, tray -> tray.ids.joinToString() }) { index, tray ->
                Box(Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null)) {
                    TrayCard(vm, tray, index, vm.state.date, vm.fridgeMeal, vm.fridgeItems)
                }
            }
        }
    }
}

@Composable private fun HintCard(text: String) {
    Surface(color = KitchenSoft, shape = MaterialTheme.shapes.large) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            KitchenIcon(KitchenGlyph.SPARKLE)
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

/** A complete suggested meal: which dishes use the fridge items, what still has to be bought, one button to use it. */
@Composable fun TrayCard(vm: PlannerViewModel, tray: TraySuggestion, index: Int, date: LocalDate, meal: Meal, items: List<String>) {
    val dishes = tray.ids.map(vm.engine.recipes::getValue)
    val planned = vm.isPlanned(date, meal, tray.ids)
    KitchenCard(Modifier.testTag("tray-$index")) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            KitchenEyebrow(if (meal.tray) "MÂM CƠM ${index + 1}" else "GỢI Ý ${index + 1}", KitchenTerracotta)
            Spacer(Modifier.weight(1f))
            Text("${vm.engine.minutes(dishes)} phút · ~${PlannerEngine.money(vm.engine.cost(dishes, vm.state.profile.people)).replace(' ', ' ')}",
                style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
        }
        dishes.forEach { recipe ->
            val uses = vm.engine.covered(recipe, items)
            Row(
                Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).clickable(role = Role.Button) { vm.openRecipe(recipe.id) }
                    .heightIn(min = 52.dp),
                verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                RecipePortrait(recipe, 46.dp)
                Column(Modifier.weight(1f)) {
                    Text(recipe.name, style = MaterialTheme.typography.labelLarge)
                    Text(recipe.kind.label, style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                }
                if (uses.isNotEmpty()) Surface(color = KitchenSoft, contentColor = KitchenGreen, shape = MaterialTheme.shapes.extraSmall) {
                    Text("Dùng ${uses.joinToString { it.lowercase(PlannerEngine.locale) }}", Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        style = MaterialTheme.typography.labelMedium, maxLines = 2)
                }
            }
        }
        HorizontalDivider(color = KitchenLine)
        Text(if (tray.missing.isEmpty()) "Đủ nguyên liệu, không cần đi chợ thêm."
            else "Cần mua thêm: ${tray.missing.joinToString { it.lowercase(PlannerEngine.locale) }}",
            style = MaterialTheme.typography.bodyMedium, color = if (tray.missing.isEmpty()) KitchenGreen else KitchenMuted)
        Button(onClick = { vm.applyTray(date, meal, tray.ids) }, enabled = !planned,
            modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("apply-tray-$index")) {
            Text(if (planned) "Đã có trong ${meal.title.lowercase(PlannerEngine.locale)}" else
                "Nấu ${if (meal.tray) "mâm này" else "món này"} cho ${meal.title.lowercase(PlannerEngine.locale)}")
        }
    }
}

// ---- Trò chuyện ------------------------------------------------------------------------------

private val chatExamples = listOf("Ngày mai tôi muốn ăn cá", "Tủ lạnh có trứng và cà chua", "Hẹn 10 phút luộc rau",
    "Sáng mai ăn phở gà", "Tối nay nấu món nào trước?", "Món tiết kiệm")

@Composable private fun TimerPanel(vm: PlannerViewModel, actions: NativeActions) {
    val running = vm.state.timers.sortedBy { it.end }
    LazyColumn(Modifier.fillMaxSize().testTag("timer-list"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item(key = "running") {
            Surface(color = KitchenGreen, contentColor = Color.White, shape = MaterialTheme.shapes.extraLarge) {
                Column(Modifier.fillMaxWidth().animateContentSize(tween(ComponentMotionMillis)).padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(9.dp), verticalAlignment = Alignment.CenterVertically) {
                        KitchenIcon(KitchenGlyph.CLOCK, tint = KitchenGold)
                        Text(if (running.isEmpty()) "Chưa có đồng hồ nào" else "Đang hẹn ${running.size}/${PlannerEngine.MAX_TIMERS} đồng hồ",
                            style = MaterialTheme.typography.titleLarge)
                    }
                    Text("Hẹn giờ cho từng món rồi cứ làm việc khác, hết giờ điện thoại sẽ báo. Có thể nấu 2 món trở lên cùng lúc.",
                        style = MaterialTheme.typography.bodyMedium, color = KitchenOnGreen)
                    running.forEach { timer ->
                        val seconds = ((timer.end - vm.now + 999) / 1000).coerceAtLeast(0)
                        Row(Modifier.fillMaxWidth().clip(MaterialTheme.shapes.small).background(Color.White.copy(alpha = 0.1f)).padding(start = 12.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Text(timer.label, Modifier.weight(1f), style = MaterialTheme.typography.labelLarge, maxLines = 2)
                            Text("%02d:%02d".format(seconds / 60, seconds % 60), fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                            IconButton(onClick = { vm.cancelTimer(timer.id) }) { Icon(Icons.Outlined.Close, "Dừng hẹn giờ ${timer.label}") }
                        }
                    }
                    if (!vm.notifications) OutlinedButton(onClick = actions.notifications, modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, KitchenOnGreen.copy(alpha = 0.55f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)) { Text("Cho phép thông báo khi hết giờ") }
                }
            }
        }
        item(key = "dishes-title") {
            KitchenSection("Món của ${vm.state.meal.title.lowercase(PlannerEngine.locale)} ${PlannerEngine.dayLabel(vm.state.date, vm.today)}")
        }
        items(vm.menu, key = { it.id }) { recipe ->
            val active = running.any { it.label == recipe.name }
            KitchenCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    RecipePortrait(recipe, 52.dp)
                    Column(Modifier.weight(1f)) {
                        Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                        Text("${recipe.minutes} phút", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                    }
                    OutlinedButton(onClick = {
                        vm.startTimer(recipe.minutes, recipe.name)
                        if (!vm.notifications) actions.notifications()
                    }, modifier = Modifier.heightIn(min = 48.dp).testTag("timer-${recipe.id}"), border = BorderStroke(1.dp, KitchenLine),
                        contentPadding = PaddingValues(horizontal = 12.dp)) { Text(if (active) "Đặt lại" else "Hẹn giờ") }
                }
            }
        }
        item(key = "quick") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                KitchenSection("Hẹn giờ nhanh")
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(3, 5, 10, 15, 20, 30).forEach { minutes ->
                        AssistChip(onClick = {
                            vm.startTimer(minutes, "Hẹn giờ $minutes phút")
                            if (!vm.notifications) actions.notifications()
                        }, label = { Text("$minutes phút") }, leadingIcon = { KitchenIcon(KitchenGlyph.CLOCK, Modifier.size(18.dp)) })
                    }
                }
                Text("Hoặc nói ở tab Trò chuyện: “hẹn 10 phút luộc rau”.", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
        }
    }
}
