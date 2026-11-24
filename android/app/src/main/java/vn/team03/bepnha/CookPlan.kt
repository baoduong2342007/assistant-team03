package vn.team03.bepnha

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val CookChip = Color(0xFFEDF1EA)
private val CookAccent = Color(0xFF5E7F4F)

@Composable fun CookScreen(vm: PlannerViewModel, actions: NativeActions) {
    val tasks = vm.tasks
    val menu = vm.menu
    val main = menu.first()
    val done = vm.state.done[vm.state.key].orEmpty()
    val completed = tasks.count { it.key in done }
    val meal = vm.state.meal
    Box(Modifier.fillMaxSize()) {
        LazyColumn(Modifier.fillMaxSize().testTag("cook-list"),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 104.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item("hero") { CookHero(vm, main) }
            item("title") {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("${meal.title} · ${PlannerEngine.fullDate(vm.state.date)}", style = MaterialTheme.typography.bodyMedium, color = KitchenMuted)
                            Text("Dọn cơm lúc ${vm.state.profile.time(meal)}", style = KitchenHeroTitle, color = KitchenInk, modifier = Modifier.semantics { heading() })
                        }
                        CookIconButton(onClick = vm::openReminders) {
                            KitchenIcon(KitchenGlyph.CLOCK, Modifier.size(20.dp).semantics { contentDescription = "Chỉnh giờ" }, KitchenGreen)
                        }
                        Spacer(Modifier.width(8.dp))
                        CookIconButton(onClick = { actions.calendar(tasks) }) {
                            Icon(Icons.Outlined.DateRange, "Thêm vào lịch", Modifier.size(20.dp), tint = KitchenGreen)
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        CookPill("Bắt đầu ${PlannerEngine.time(tasks.first().at)}", KitchenGlyph.CLOCK)
                        CookPill("${vm.state.profile.people} người", KitchenGlyph.PEOPLE)
                        CookPill(if (meal.tray) "2 vùng nấu" else "1 vùng nấu", KitchenGlyph.POT)
                    }
                }
            }
            if (menu.size > 1) item("dishes") {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    menu.forEach { recipe ->
                        Column(Modifier.weight(1f).clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button) { vm.openRecipe(recipe.id) }
                            .testTag("cook-dish-${recipe.id}"), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            RecipePhoto(recipe, Modifier.fillMaxWidth().aspectRatio(1f).clip(RoundedCornerShape(16.dp)))
                            Text(recipe.name, style = MaterialTheme.typography.labelLarge, color = KitchenInk, maxLines = 2, minLines = 2,
                                overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            item("steps-heading") {
                val progress = animateFloatAsState(completed.toFloat() / tasks.size, tween(ComponentMotionMillis), label = "cooking task progress")
                Column(Modifier.padding(top = 4.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Các bước nấu", style = MaterialTheme.typography.titleLarge, color = KitchenInk,
                            modifier = Modifier.weight(1f).semantics { heading() })
                        Text("$completed/${tasks.size}", style = MaterialTheme.typography.labelLarge, color = KitchenMuted)
                    }
                    LinearProgressIndicator(progress = { progress.value }, modifier = Modifier.fillMaxWidth().height(6.dp).clip(CircleShape),
                        color = KitchenGreen, trackColor = KitchenGreen.copy(alpha = 0.1f), gapSize = 0.dp, drawStopIndicator = {})
                }
            }
            item("steps") {
                KitchenPanel {
                    tasks.forEachIndexed { index, task ->
                        if (index > 0) KitchenRowDivider(start = 64.dp)
                        CookStep(vm, actions, task, index, task.key in done)
                    }
                }
            }
        }
        // Floating like the reference's "Start Cooking" pill; it stays reachable however far the steps scroll.
        Surface(onClick = { vm.startCookAlong() }, color = KitchenGreen, contentColor = Color.White, shape = CircleShape, shadowElevation = 6.dp,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 16.dp).heightIn(min = 56.dp).testTag("start-cook-along")) {
            Row(Modifier.padding(start = 20.dp, end = 24.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Outlined.PlayArrow, null, Modifier.size(22.dp))
                Text("Nấu cùng tôi", style = MaterialTheme.typography.titleSmall)
            }
        }
    }
}

@Composable private fun CookHero(vm: PlannerViewModel, main: Recipe) {
    val saved = main.id in vm.state.favorites
    Box {
        RecipePhoto(main, Modifier.fillMaxWidth().height(210.dp).clip(RoundedCornerShape(24.dp)))
        Row(Modifier.align(Alignment.TopStart).padding(12.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 12.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            KitchenIcon(KitchenGlyph.CLOCK, Modifier.size(16.dp), KitchenGreen)
            Text("${vm.engine.minutes(vm.menu)} phút", style = MaterialTheme.typography.labelLarge, color = KitchenGreen)
        }
        Surface(onClick = { vm.toggleFavorite(main.id) }, color = Color.White.copy(alpha = 0.92f), contentColor = KitchenTerracotta, shape = CircleShape,
            modifier = Modifier.align(Alignment.TopEnd).padding(8.dp).size(44.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(if (saved) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                    if (saved) "Bỏ lưu ${main.name}" else "Lưu ${main.name}", Modifier.size(20.dp))
            }
        }
    }
}

@Composable private fun CookIconButton(onClick: () -> Unit, content: @Composable () -> Unit) {
    Surface(onClick = onClick, color = Color.White, shape = CircleShape, border = BorderStroke(1.dp, KitchenCardBorder), modifier = Modifier.size(44.dp)) {
        Box(contentAlignment = Alignment.Center) { content() }
    }
}

@Composable private fun CookPill(text: String, glyph: KitchenGlyph) {
    Row(Modifier.clip(CircleShape).background(Color.White).border(1.dp, KitchenCardBorder, CircleShape).padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
        KitchenIcon(glyph, Modifier.size(15.dp), KitchenMuted)
        Text(text, style = MaterialTheme.typography.labelMedium, color = KitchenInk, maxLines = 1)
    }
}

@Composable private fun CookStep(vm: PlannerViewModel, actions: NativeActions, task: CookingTask, index: Int, finished: Boolean) {
    val fill = animateColorAsState(if (finished) KitchenGreen else Color.White, tween(ComponentMotionMillis), label = "cooking task marker")
    Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 12.dp).testTag("task-${task.key}"),
        horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        Box(Modifier.size(34.dp).clip(CircleShape).background(fill.value).border(1.5.dp, if (finished) KitchenGreen else KitchenCardBorder, CircleShape)
            .toggleable(finished, role = Role.Checkbox) { vm.checkTask(task.key) }
            .semantics { contentDescription = "Đã xong ${task.title}" }, contentAlignment = Alignment.Center) {
            if (finished) Icon(Icons.Outlined.Check, null, Modifier.size(18.dp), tint = Color.White)
            else Text("${index + 1}", style = MaterialTheme.typography.labelLarge, color = KitchenGreen)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(task.title, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f),
                    color = if (finished) KitchenMuted else KitchenInk, textDecoration = if (finished) TextDecoration.LineThrough else null)
                Text(PlannerEngine.time(task.at), style = MaterialTheme.typography.labelLarge, color = if (finished) KitchenMuted else KitchenTerracotta)
            }
            Text(task.detail, style = MaterialTheme.typography.bodySmall, color = KitchenMuted)
            val recipeId = task.recipeId
            if (recipeId != null || task.minutes > 0) Row(Modifier.offset(x = (-10).dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (task.minutes > 0) TextButton(onClick = {
                    vm.startTimer(task.minutes, task.title)
                    if (!vm.notifications) actions.notifications()
                }, contentPadding = PaddingValues(horizontal = 10.dp), modifier = Modifier.heightIn(min = 40.dp)) {
                    KitchenIcon(KitchenGlyph.CLOCK, Modifier.size(15.dp), KitchenGreen)
                    Spacer(Modifier.width(6.dp))
                    Text("Hẹn ${task.minutes} phút", style = MaterialTheme.typography.labelLarge)
                }
                if (recipeId != null) TextButton(onClick = { vm.openRecipe(recipeId) }, contentPadding = PaddingValues(horizontal = 10.dp),
                    modifier = Modifier.heightIn(min = 40.dp)) { Text("Xem cách làm", style = MaterialTheme.typography.labelLarge) }
            }
        }
    }
}
