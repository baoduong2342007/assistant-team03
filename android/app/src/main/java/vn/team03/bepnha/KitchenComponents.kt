package vn.team03.bepnha

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Reflow actions instead of squeezing text into half-width buttons at large font sizes. */
@Composable fun KitchenActions(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 320.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.fillMaxWidth().heightIn(min = 56.dp))
                second(Modifier.fillMaxWidth().heightIn(min = 56.dp))
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                first(Modifier.weight(1f).heightIn(min = 56.dp))
                second(Modifier.weight(1f).heightIn(min = 56.dp))
            }
        }
    }
}

@Composable fun KitchenAmount(name: String, amount: String, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier.fillMaxWidth()) {
        if (maxWidth < 280.dp || LocalDensity.current.fontScale > 1.3f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(name)
                Text(amount, style = MaterialTheme.typography.bodySmall, color = KitchenGreen)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(name, Modifier.weight(1f))
                Text(amount, style = MaterialTheme.typography.bodySmall, color = KitchenGreen)
            }
        }
    }
}

/** Four stable, equally sized tabs. Labels determine height; no text scaling override. */
@Composable fun KitchenNavigation(selected: Screen, onSelect: (Screen) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = KitchenLine)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()) {
                listOf(Screen.TODAY, Screen.PLAN, Screen.SHOPPING, Screen.COOK).forEach { screen ->
                    Column(
                        Modifier.weight(1f).fillMaxHeight()
                            .selectable(selected == screen, role = Role.Tab, onClick = { onSelect(screen) })
                            .testTag("tab-${screen.name.lowercase()}")
                            .padding(horizontal = 4.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Surface(
                            color = if (selected == screen) KitchenSoft else MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(12.dp),
                        ) {
                            Box(Modifier.size(width = 56.dp, height = 32.dp), contentAlignment = Alignment.Center) {
                                when (screen) {
                                    Screen.TODAY -> Icon(Icons.Outlined.Home, null, tint = KitchenGreen)
                                    Screen.PLAN -> Icon(Icons.Outlined.DateRange, null, tint = KitchenGreen)
                                    Screen.SHOPPING -> Icon(Icons.Outlined.ShoppingCart, null, tint = KitchenGreen)
                                    else -> Icon(Icons.Outlined.PlayArrow, null, tint = KitchenGreen)
                                }
                            }
                        }
                        Text(screen.title, textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = if (selected == screen) FontWeight.Bold else FontWeight.Normal,
                            color = if (selected == screen) KitchenGreen else KitchenMuted)
                    }
                }
            }
        }
    }
}

data class KitchenTimerUi(val label: String, val remaining: String)

@Composable fun KitchenTimer(timer: KitchenTimerUi, onCancel: () -> Unit) {
    Surface(color = KitchenSoft, contentColor = KitchenInk) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Text(timer.label, style = MaterialTheme.typography.bodySmall)
            Text(timer.remaining, Modifier.testTag("timer-countdown"),
                style = MaterialTheme.typography.headlineSmall, color = KitchenGreen)
            TextButton(onClick = onCancel, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { Text("Dừng hẹn giờ") }
        }
    }
}

fun PlannerViewModel.timerUi(): KitchenTimerUi? = state.timer?.let {
    val seconds = ((it.end - now + 999) / 1000).coerceAtLeast(0)
    KitchenTimerUi(it.label, "%02d:%02d".format(seconds / 60, seconds % 60))
}
