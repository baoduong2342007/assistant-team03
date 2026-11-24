@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)

package vn.team03.bepnha

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp

private val GroupBackground = Color(0xFFF4F1EA)
private const val OTHER_GROUP = "Khác"

@Composable fun ShoppingScreen(vm: PlannerViewModel, actions: NativeActions) {
    val items = vm.shoppingItems()
    val checked = vm.checkedIngredients
    val meals = vm.state.shoppingMeals
    val count = items.count { it.key in checked }
    val extras = vm.state.extras[PlannerEngine.shopKey(vm.state.date)].orEmpty()
    var adding by rememberSaveable { mutableStateOf(false) }
    var clearing by rememberSaveable { mutableStateOf(false) }
    LazyColumn(Modifier.fillMaxSize().testTag("shopping-list"), contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)) {
        item("meals") { ShoppingMealPicker(vm, meals) }
        item("summary") {
            val progress = animateFloatAsState(if (items.isEmpty()) 0f else count.toFloat() / items.size,
                tween(ComponentMotionMillis), label = "shopping progress")
            KitchenPanel {
                Column(Modifier.padding(start = 16.dp, end = 8.dp, top = 12.dp, bottom = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Đã mua $count/${items.size}", style = MaterialTheme.typography.titleLarge, color = KitchenInk)
                            Text("${PlannerEngine.fullDate(vm.state.date)} · ước tính " +
                                PlannerEngine.money(vm.engine.cost(vm.shoppingMenu, vm.state.profile.people)).replace(' ', '\u00A0'),
                                style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                        }
                        IconButton(onClick = { clearing = true }, modifier = Modifier.testTag("shopping-clear")) {
                            Icon(Icons.Outlined.Delete, "Làm mới danh sách đi chợ", tint = KitchenMuted)
                        }
                    }
                    LinearProgressIndicator(progress = { progress.value }, modifier = Modifier.padding(end = 8.dp).fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = KitchenGreen, trackColor = KitchenGreen.copy(alpha = 0.1f), gapSize = 0.dp, drawStopIndicator = {})
                    Row(Modifier.padding(end = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { adding = true }, shape = RoundedCornerShape(12.dp), contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp).testTag("shopping-add")) {
                            Icon(Icons.Outlined.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Thêm món", maxLines = 1)
                        }
                        OutlinedButton(onClick = { actions.copy(vm.shoppingText()) }, shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, KitchenCardBorder), contentPadding = PaddingValues(horizontal = 8.dp),
                            modifier = Modifier.weight(1f).heightIn(min = 44.dp).testTag("shopping-copy")) { Text("Sao chép", maxLines = 1) }
                    }
                }
            }
        }
        items.groupBy { it.group }.forEach { (group, groupItems) ->
            item(key = "group-$group") {
                ShoppingGroup(vm, group, groupItems, checked, hasExtras = extras.any { it.group == group },
                    Modifier.animateItem(fadeInSpec = tween(ComponentMotionMillis), fadeOutSpec = null))
            }
        }
        item("cook") {
            Button(onClick = { vm.navigate(Screen.COOK) }, modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp)) {
                Text("Vào bếp")
                Spacer(Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(18.dp))
            }
        }
    }
    if (adding) AddShoppingItemSheet(vm, items.map { it.group }) { adding = false }
    if (clearing) ClearShoppingDialog(onDismiss = { clearing = false }, onConfirm = { clearing = false; vm.clearShopping() })
}

/** Buy for one meal, several meals together, or the whole day in one trip. */
@Composable private fun ShoppingMealPicker(vm: PlannerViewModel, meals: Set<Meal>) {
    val wholeDay = meals.size == Meal.entries.size
    FlowRow(Modifier.semantics { contentDescription = "Mua cho bữa nào" }, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val colors = FilterChipDefaults.filterChipColors(selectedContainerColor = KitchenGreen, selectedLabelColor = Color.White,
            selectedLeadingIconColor = Color.White, containerColor = Color.White)
        val border = @Composable { selected: Boolean -> FilterChipDefaults.filterChipBorder(true, selected, borderColor = KitchenCardBorder) }
        Meal.entries.forEach { meal ->
            val selected = meal in meals
            FilterChip(selected = selected, onClick = { vm.toggleShopMeal(meal) }, label = { Text(meal.title) },
                leadingIcon = if (selected) { { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) } } else null,
                colors = colors, border = border(selected), modifier = Modifier.testTag("shop-${meal.value}"))
        }
        FilterChip(selected = wholeDay, onClick = vm::shopWholeDay, label = { Text("Cả ngày") },
            leadingIcon = if (wholeDay) { { Icon(Icons.Outlined.Check, null, Modifier.size(16.dp)) } } else null,
            colors = colors, border = border(wholeDay), modifier = Modifier.testTag("shop-day"))
    }
}

@Composable private fun ShoppingGroup(vm: PlannerViewModel, group: String, items: List<Ingredient>, checked: Set<String>,
                                      hasExtras: Boolean, modifier: Modifier) {
    var menu by remember { mutableStateOf(false) }
    KitchenPanel(modifier) {
        Row(Modifier.fillMaxWidth().heightIn(min = 52.dp).padding(start = 16.dp, end = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(group, style = MaterialTheme.typography.titleMedium, color = KitchenInk, modifier = Modifier.semantics { heading() })
            Text("  ${items.count { it.key in checked }}/${items.size}", style = MaterialTheme.typography.bodySmall, color = KitchenMuted,
                modifier = Modifier.weight(1f))
            Box {
                IconButton(onClick = { menu = true }) { Icon(Icons.Outlined.MoreVert, "Tùy chọn nhóm $group", tint = KitchenMuted) }
                DropdownMenu(expanded = menu, onDismissRequest = { menu = false }) {
                    val keys = items.map { it.key }
                    DropdownMenuItem(text = { Text("Đánh dấu đã mua cả nhóm") }, onClick = { menu = false; vm.setIngredientsChecked(keys, true) })
                    DropdownMenuItem(text = { Text("Bỏ đánh dấu cả nhóm") }, onClick = { menu = false; vm.setIngredientsChecked(keys, false) })
                    if (hasExtras) DropdownMenuItem(text = { Text("Xóa món tự thêm") }, onClick = { menu = false; vm.removeShoppingExtras(group) })
                }
            }
        }
        items.forEach { item ->
            key(item.key) {
                KitchenRowDivider()
                ShoppingRow(item, item.key in checked) { vm.checkIngredient(item.key) }
            }
        }
    }
}

@Composable private fun ShoppingRow(item: Ingredient, done: Boolean, onToggle: () -> Unit) {
    val fill = animateColorAsState(if (done) KitchenGreen else Color.White, tween(ComponentMotionMillis), label = "ingredient check")
    Row(Modifier.fillMaxWidth().toggleable(value = done, role = Role.Checkbox, onValueChange = { onToggle() })
        .testTag("ingredient-${item.name}").heightIn(min = 52.dp).padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(22.dp).background(fill.value, CircleShape).border(1.5.dp, if (done) KitchenGreen else KitchenMuted.copy(alpha = 0.5f), CircleShape),
            contentAlignment = Alignment.Center) {
            if (done) Icon(Icons.Outlined.Check, null, Modifier.size(14.dp), tint = Color.White)
        }
        Text(item.name, Modifier.weight(1f), style = MaterialTheme.typography.bodyLarge,
            textDecoration = if (done) TextDecoration.LineThrough else null, color = if (done) KitchenMuted else KitchenInk)
        Text("${PlannerEngine.quantity(item.amount)} ${item.unit}".trim(), style = MaterialTheme.typography.bodyMedium,
            color = KitchenMuted)
    }
}

@Composable private fun AddShoppingItemSheet(vm: PlannerViewModel, listed: List<String>, onClose: () -> Unit) {
    val groups = remember(vm.engine, listed) {
        (listed + vm.engine.recipes.values.flatMap { recipe -> recipe.ingredients.map { it.group } } + PlannerEngine.PANTRY + OTHER_GROUP).distinct()
    }
    var name by rememberSaveable { mutableStateOf("") }
    var group by rememberSaveable { mutableStateOf(OTHER_GROUP) }
    var amount by rememberSaveable { mutableStateOf("1") }
    var unit by rememberSaveable { mutableStateOf("") }
    var groupMenu by remember { mutableStateOf(false) }
    val quantity = amount.replace(',', '.').toDoubleOrNull()
    val valid = name.isNotBlank() && quantity != null && quantity > 0
    ModalBottomSheet(onDismissRequest = onClose, sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White) {
        Column(Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 28.dp).testTag("shopping-add-sheet"),
            verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(Modifier.size(22.dp).background(Color(0xFF689F38), CircleShape), contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Add, null, Modifier.size(16.dp), tint = Color.White)
                }
                Text("Thêm vào danh sách đi chợ", style = MaterialTheme.typography.titleMedium, color = KitchenGreen,
                    modifier = Modifier.semantics { heading() })
            }
            val fieldColors = TextFieldDefaults.colors(unfocusedContainerColor = GroupBackground, focusedContainerColor = GroupBackground,
                errorContainerColor = GroupBackground, unfocusedIndicatorColor = Color.Transparent, focusedIndicatorColor = Color.Transparent,
                errorIndicatorColor = Color.Transparent)
            TextField(name, { name = it.take(60) }, Modifier.fillMaxWidth().testTag("shopping-add-name"), singleLine = true,
                label = { Text("Tên món") }, shape = RoundedCornerShape(10.dp), colors = fieldColors)
            ExposedDropdownMenuBox(expanded = groupMenu, onExpandedChange = { groupMenu = it }) {
                TextField(group, {}, readOnly = true, singleLine = true, label = { Text("Nhóm") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(groupMenu) }, shape = RoundedCornerShape(10.dp), colors = fieldColors,
                    modifier = Modifier.fillMaxWidth().menuAnchor(MenuAnchorType.PrimaryNotEditable))
                ExposedDropdownMenu(expanded = groupMenu, onDismissRequest = { groupMenu = false }) {
                    groups.forEach { option -> DropdownMenuItem(text = { Text(option) }, onClick = { group = option; groupMenu = false }) }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                TextField(amount, { amount = it.filter { c -> c.isDigit() || c == '.' || c == ',' }.take(8) },
                    Modifier.weight(1f).testTag("shopping-add-amount"), singleLine = true, label = { Text("Số lượng") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), shape = RoundedCornerShape(10.dp), colors = fieldColors,
                    isError = amount.isNotEmpty() && (quantity == null || quantity <= 0))
                TextField(unit, { unit = it.take(20) }, Modifier.weight(1f).testTag("shopping-add-unit"), singleLine = true,
                    label = { Text("Đơn vị") }, placeholder = { Text("g, bó, quả…") }, shape = RoundedCornerShape(10.dp), colors = fieldColors)
            }
            Button(onClick = { vm.addShoppingItem(name, quantity ?: 0.0, unit, group); onClose() }, enabled = valid,
                shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp).testTag("shopping-add-confirm")) {
                Text("Thêm món")
            }
        }
    }
}

@Composable private fun ClearShoppingDialog(onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss, containerColor = Color.White, shape = RoundedCornerShape(16.dp),
        icon = {
            Box(Modifier.size(44.dp).background(Color(0xFF689F38), CircleShape), contentAlignment = Alignment.Center) {
                Text("!", style = MaterialTheme.typography.titleLarge, color = Color.White)
            }
        },
        title = { Text("Làm mới danh sách đi chợ?", style = MaterialTheme.typography.titleMedium, color = KitchenGreen) },
        text = { Text("Mọi dấu tích sẽ bị bỏ và các món bạn tự thêm cho ngày này sẽ bị xóa. Không thể hoàn tác.",
            style = MaterialTheme.typography.bodyMedium, color = KitchenMuted) },
        confirmButton = {
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = onDismiss, shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)) { Text("Hủy") }
                OutlinedButton(onClick = onConfirm, shape = RoundedCornerShape(10.dp), border = BorderStroke(1.5.dp, KitchenGreen),
                    modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp).testTag("shopping-clear-confirm")) { Text("Xóa hết") }
            }
        })
}
