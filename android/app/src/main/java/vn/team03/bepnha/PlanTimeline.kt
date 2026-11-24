@file:OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)

package vn.team03.bepnha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import java.time.LocalTime

private val TimelineSection = Color(0xFFEEEBE3)
private val TimelineTrack = Color(0xFFD9D2C4)

/** Distance from the list's padded edge to the centre of the timeline track. */
private val TrackX = 12.dp
/** Left edge of each meal section, leaving room for the track and its markers. */
private val SectionStart = 32.dp
/** Half the meal header's height, so the marker and the track's start line up with the title. */
private val HeaderCenter = 28.dp

@Composable fun WeekScreen(vm: PlannerViewModel) {
    var week by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxSize()) {
        PlanModeTabs(week, { week = it }, Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp))
        if (week) WeekOverview(vm, Modifier.weight(1f)) else DayTimeline(vm, Modifier.weight(1f))
    }
}

@Composable private fun PlanModeTabs(week: Boolean, onWeek: (Boolean) -> Unit, modifier: Modifier) {
    Row(modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(TimelineSection).padding(4.dp).selectableGroup(),
        horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        listOf("Theo ngày" to false, "Cả tuần" to true).forEach { (label, value) ->
            val active = week == value
            Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(RoundedCornerShape(9.dp))
                .background(if (active) KitchenGreen else Color.Transparent)
                .selectable(active, role = Role.Tab) { onWeek(value) }
                .testTag(if (value) "plan-week" else "plan-day"), contentAlignment = Alignment.Center) {
                Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) Color.White else KitchenMuted)
            }
        }
    }
}

@Composable private fun DayTimeline(vm: PlannerViewModel, modifier: Modifier) {
    val date = vm.state.date
    val now = remember(date) { LocalTime.now() }
    LazyColumn(modifier.fillMaxSize().testTag("plan-timeline"), contentPadding = PaddingValues(top = 12.dp, bottom = 28.dp)) {
        item("month") {
            Text("Tháng ${date.monthValue}, ${date.year}", style = MaterialTheme.typography.titleLarge, color = KitchenInk,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, bottom = 12.dp).semantics { heading() })
        }
        item("dates") { KitchenDateStrip(vm) }
        item("gap") { Spacer(Modifier.height(20.dp)) }
        items(Meal.entries, key = { "timeline-${it.value}" }) { meal ->
            val done = date < vm.today || (date == vm.today && now > vm.state.profile.time(meal))
            TimelineMeal(vm, meal, done, first = meal == Meal.entries.first(), last = meal == Meal.entries.last())
        }
    }
}

@Composable private fun TimelineMeal(vm: PlannerViewModel, meal: Meal, done: Boolean, first: Boolean, last: Boolean) {
    val dishes = vm.engine.menu(vm.state, meal = meal)
    val people = vm.state.profile.people
    var menuOpen by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxWidth().padding(horizontal = 20.dp).drawBehind {
        // One continuous track from the first meal's marker down past the last card.
        val x = TrackX.toPx()
        drawLine(TimelineTrack, Offset(x, if (first) HeaderCenter.toPx() else 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
    }) {
        KitchenPanel(Modifier.padding(start = SectionStart, bottom = if (last) 0.dp else 14.dp).testTag("timeline-${meal.value}")) {
            Box(Modifier.fillMaxWidth().height(HeaderCenter * 2).padding(start = 16.dp, end = 4.dp)) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Text(meal.title, style = MaterialTheme.typography.titleMedium, color = KitchenInk,
                        modifier = Modifier.semantics { heading() })
                    Text("  ${vm.state.profile.time(meal)}", style = MaterialTheme.typography.bodySmall, color = KitchenMuted,
                        modifier = Modifier.weight(1f))
                    Text(PlannerEngine.money(vm.engine.cost(dishes, people)), style = MaterialTheme.typography.labelLarge, color = KitchenGreen)
                    Box {
                        IconButton(onClick = { menuOpen = true }, modifier = Modifier.testTag("timeline-menu-${meal.value}")) {
                            Icon(Icons.Outlined.MoreVert, "Tùy chọn ${meal.title.lowercase(PlannerEngine.locale)}", tint = KitchenMuted)
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            dishes.forEachIndexed { index, recipe ->
                                DropdownMenuItem(text = { Text("Đổi ${recipe.name.lowercase(PlannerEngine.locale)}") },
                                    onClick = { menuOpen = false; vm.selectMeal(meal); vm.openSwap(index) })
                            }
                        }
                    }
                }
            }
            dishes.forEach { recipe ->
                KitchenRowDivider()
                TimelineDish(vm, recipe, people)
            }
        }
        // Drawn outside the panel, which clips its own content.
        MealMarker(done, Modifier.offset(x = TrackX - 11.dp, y = HeaderCenter - 11.dp))
    }
}

@Composable private fun MealMarker(done: Boolean, modifier: Modifier) {
    Box(modifier.size(22.dp).background(if (done) KitchenGreen else KitchenPaper, CircleShape)
        .border(2.dp, KitchenGreen, CircleShape), contentAlignment = Alignment.Center) {
        if (done) Icon(Icons.Outlined.Check, "Đã qua giờ ăn", Modifier.size(14.dp), tint = Color.White)
    }
}

@Composable private fun TimelineDish(vm: PlannerViewModel, recipe: Recipe, people: Int) {
    Row(Modifier.fillMaxWidth().clickable(role = Role.Button) { vm.openRecipe(recipe.id) }
        .padding(horizontal = 16.dp, vertical = 10.dp).testTag("timeline-dish-${recipe.id}"),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        RecipePhoto(recipe, Modifier.size(52.dp).clip(RoundedCornerShape(12.dp)))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(recipe.name, style = MaterialTheme.typography.titleSmall, color = KitchenInk, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text("${recipe.kind.label} · ${recipe.minutes} phút · ${PlannerEngine.money(vm.engine.cost(listOf(recipe), people))}",
                style = MaterialTheme.typography.bodySmall, color = KitchenMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}
