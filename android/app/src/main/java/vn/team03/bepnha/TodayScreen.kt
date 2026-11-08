@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
package vn.team03.bepnha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.time.LocalDate

data class TodayUi(
    val today: LocalDate, val date: LocalDate, val days: List<LocalDate>,
    val meal: Meal, val people: Int, val menu: List<Recipe>,
    val favorites: Set<String>, val cost: String, val minutes: Int,
)

@Composable fun TodayScreen(vm: PlannerViewModel) {
    TodayContent(
        TodayUi(vm.today, vm.state.date, vm.days, vm.state.meal, vm.state.profile.people, vm.menu,
            vm.state.favorites, PlannerEngine.money(vm.engine.cost(vm.menu, vm.state.profile.people)), vm.engine.minutes(vm.menu)),
        vm::selectDate, vm::selectMeal, vm::openRecipe, vm::openSwap, vm::toggleFavorite,
        { vm.navigate(Screen.PLAN) }, { vm.navigate(Screen.SHOPPING) }, { vm.navigate(Screen.COOK) },
    )
}

@Composable fun TodayContent(
    ui: TodayUi,
    onDate: (LocalDate) -> Unit, onMeal: (Meal) -> Unit,
    onRecipe: (String) -> Unit, onSwap: (Int) -> Unit, onFavorite: (String) -> Unit,
    onWeek: () -> Unit, onShopping: () -> Unit, onCook: () -> Unit,
) {
    LazyColumn(Modifier.fillMaxSize().testTag("today-list"),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SectionHeading("Hôm nay nhà mình ăn gì?")
                Text(PlannerEngine.fullDate(ui.date), style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                KitchenMealSelector(ui.meal, ui.people, onMeal)
            }
        }
        item { KitchenDateStrip(ui.today, ui.date, ui.days, onDate) }
        item {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text("Thực đơn của nhà", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                TextButton(onClick = onWeek, modifier = Modifier.heightIn(min = 48.dp)) { Text("Cả tuần") }
            }
        }
        items(ui.menu.withIndex().toList(), key = { it.value.id }) { (index, recipe) ->
            Box(Modifier.padding(horizontal = 16.dp)) {
                RecipeCardContent(recipe, recipe.id in ui.favorites, { onRecipe(recipe.id) },
                    { onFavorite(recipe.id) }, { onSwap(index) })
            }
        }
        item {
            Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Surface(color = KitchenSoft, shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        KitchenAmount("Chi phí ước tính", ui.cost)
                        Text("${ui.menu.size} món cho ${ui.people} người", style = MaterialTheme.typography.bodySmall)
                        Text("Khoảng ${ui.minutes} phút. Chưa tính gạo và gia vị có sẵn.",
                            style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                    }
                }
                KitchenActions(
                    { KitchenButton(onClick = onShopping, modifier = it) { Text("Chuẩn bị đi chợ") } },
                    { KitchenOutlinedButton(onClick = onCook, modifier = it) { Text("Bắt đầu nấu") } },
                )
            }
        }
    }
}

@Composable fun KitchenMealSelector(meal: Meal, people: Int, onSelect: (Meal) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Meal.entries.forEach { item ->
                FilterChip(selected = meal == item, onClick = { onSelect(item) },
                    label = { Text(item.title) }, modifier = Modifier.heightIn(min = 48.dp))
            }
        }
        Text("$people người ăn", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
    }
}

@Composable fun MealSelector(vm: PlannerViewModel) =
    KitchenMealSelector(vm.state.meal, vm.state.profile.people, vm::selectMeal)

@Composable fun KitchenDateStrip(today: LocalDate, date: LocalDate, days: List<LocalDate>, onSelect: (LocalDate) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(horizontal = 16.dp)) {
        items(days, key = { it.toString() }) { day ->
            val active = date == day
            Surface(onClick = { onSelect(day) }, modifier = Modifier.width(96.dp).heightIn(min = 96.dp)
                .semantics { selected = active },
                color = if (active) KitchenGreen else Color.White,
                contentColor = if (active) Color.White else KitchenInk,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(if (active) 3.dp else 1.dp, if (active) KitchenGreen else KitchenMuted)) {
                Column(Modifier.padding(8.dp), horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(if (day == today) "Hôm nay" else if (day.dayOfWeek.value == 7) "CN" else "T${day.dayOfWeek.value + 1}",
                        style = MaterialTheme.typography.labelMedium)
                    Text(day.dayOfMonth.toString(), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text("tháng ${day.monthValue}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable fun RecipeCard(vm: PlannerViewModel, recipe: Recipe, index: Int? = null) {
    RecipeCardContent(recipe, recipe.id in vm.state.favorites, { vm.openRecipe(recipe.id) },
        { vm.toggleFavorite(recipe.id) }, index?.let { { vm.openSwap(it) } })
}

@Composable fun RecipeCardContent(recipe: Recipe, saved: Boolean, onOpen: () -> Unit, onSave: () -> Unit, onSwap: (() -> Unit)? = null) {
    KitchenCard(Modifier.testTag("recipe-${recipe.id}")) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            if (androidx.compose.ui.platform.LocalDensity.current.fontScale <= 1.3f) {
                Box(Modifier.size(48.dp).clip(RoundedCornerShape(12.dp)).background(KitchenSoft), contentAlignment = Alignment.Center) {
                    Text(recipe.emoji, fontSize = 28.sp, modifier = Modifier.clearAndSetSemantics { })
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(recipe.kind.label, style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
                Text(recipe.name, style = MaterialTheme.typography.titleMedium)
                Text("${recipe.minutes} phút", style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            }
            IconButton(onClick = onSave, modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)) {
                Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    if (saved) "Bỏ lưu ${recipe.name}" else "Lưu ${recipe.name}", tint = KitchenGreen)
            }
        }
        if (onSwap == null) {
            KitchenOutlinedButton(onClick = onOpen, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp).testTag("open-${recipe.id}")) { Text("Xem công thức") }
        } else {
            KitchenActions(
                { KitchenOutlinedButton(onClick = onOpen, modifier = it.testTag("open-${recipe.id}")) { Text("Xem công thức") } },
                { TextButton(onClick = onSwap, modifier = it.testTag("swap-${recipe.id}")) { Text("Đổi món") } },
            )
        }
    }
}
