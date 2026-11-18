package vn.team03.bepnha

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.style.TextAlign
enum class KitchenGlyph { BOWL, CLOCK, PEOPLE, SPARKLE, MIC, FRIDGE }
val LocalMealPhoto = staticCompositionLocalOf<ImageBitmap> { error("Meal photo must be provided by MainActivity") }

/** Small native line illustrations; no icon-font or network dependency. */
@Composable fun KitchenIcon(glyph: KitchenGlyph, modifier: Modifier = Modifier, tint: Color = KitchenGreen) {
    Canvas(modifier.size(22.dp)) {
        val stroke = Stroke(width = 1.65f, cap = StrokeCap.Round)
        scale(size.width / 24f, size.height / 24f, pivot = androidx.compose.ui.geometry.Offset.Zero) {
            fun path(build: Path.() -> Unit) = drawPath(Path().apply(build), tint, style = stroke)
            when (glyph) {
                KitchenGlyph.BOWL -> {
                    path { moveTo(3f, 11f); lineTo(21f, 11f); cubicTo(20f, 21f, 4f, 21f, 3f, 11f); moveTo(8f, 22f); lineTo(16f, 22f) }
                    listOf(7f, 12f, 17f).forEach { x -> path { moveTo(x, 7f); cubicTo(x - 3, 4f, x + 3, 3f, x, 1f) } }
                }
                KitchenGlyph.CLOCK -> {
                    drawCircle(tint, 9f, androidx.compose.ui.geometry.Offset(12f, 12f), style = stroke)
                    path { moveTo(12f, 6f); lineTo(12f, 12f); lineTo(16f, 14f) }
                }
                KitchenGlyph.PEOPLE -> {
                    drawCircle(tint, 3.2f, androidx.compose.ui.geometry.Offset(9f, 7f), style = stroke)
                    path { moveTo(2f, 21f); lineTo(2f, 18f); cubicTo(2f, 11f, 16f, 11f, 16f, 18f); lineTo(16f, 21f); moveTo(17f, 4f); cubicTo(22f, 5f, 22f, 10f, 17f, 11f); moveTo(19f, 14f); cubicTo(22f, 15f, 22f, 18f, 22f, 21f) }
                }
                KitchenGlyph.SPARKLE -> {
                    path { moveTo(10f, 3f); lineTo(12f, 9f); lineTo(18f, 11f); lineTo(12f, 13f); lineTo(10f, 19f); lineTo(8f, 13f); lineTo(2f, 11f); lineTo(8f, 9f); close(); moveTo(20f, 1f); lineTo(20f, 6f); moveTo(17.5f, 3.5f); lineTo(22.5f, 3.5f) }
                }
                KitchenGlyph.MIC -> {
                    path { moveTo(9f, 7f); cubicTo(9f, 3f, 15f, 3f, 15f, 7f); lineTo(15f, 11f); cubicTo(15f, 15f, 9f, 15f, 9f, 11f); close() }
                    path { moveTo(5.5f, 11f); cubicTo(5.5f, 19.5f, 18.5f, 19.5f, 18.5f, 11f); moveTo(12f, 17.5f); lineTo(12f, 21f); moveTo(8.5f, 21f); lineTo(15.5f, 21f) }
                }
                KitchenGlyph.FRIDGE -> {
                    path { moveTo(8f, 2f); lineTo(16f, 2f); cubicTo(17.2f, 2f, 18f, 2.8f, 18f, 4f); lineTo(18f, 20f); cubicTo(18f, 21.2f, 17.2f, 22f, 16f, 22f)
                        lineTo(8f, 22f); cubicTo(6.8f, 22f, 6f, 21.2f, 6f, 20f); lineTo(6f, 4f); cubicTo(6f, 2.8f, 6.8f, 2f, 8f, 2f); close() }
                    path { moveTo(6f, 10f); lineTo(18f, 10f); moveTo(9f, 5f); lineTo(9f, 7.5f); moveTo(9f, 13f); lineTo(9f, 16.5f) }
                }
            }
        }
    }
}
@Composable fun KitchenBrand() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(Modifier.size(38.dp).clip(MaterialTheme.shapes.small).background(KitchenGreen), contentAlignment = Alignment.Center) {
            Image(painterResource(R.drawable.ic_bowl), null, Modifier.size(30.dp))
        }
        Text("Bếp Nhà", style = MaterialTheme.typography.headlineSmall, color = KitchenGreen)
    }
}

@Composable fun KitchenEyebrow(text: String, color: Color = KitchenMuted) {
    Text(text, fontSize = 11.sp, lineHeight = 17.sp, letterSpacing = 1.3.sp, fontWeight = FontWeight.SemiBold, color = color)
}

@Composable fun KitchenSection(title: String, action: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).semantics { heading() })
        if (action != null) TextButton(onClick = onAction, contentPadding = PaddingValues(horizontal = 4.dp)) {
            Text(action)
            Spacer(Modifier.width(6.dp))
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, Modifier.size(16.dp))
        }
    }
}


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

/** Segmented control: one row of equally wide, accessible tabs. */
@Composable fun KitchenSegmented(
    options: List<String>,
    selected: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    tag: (Int) -> String? = { null },
) {
    Surface(modifier = modifier, color = KitchenSurface, shape = MaterialTheme.shapes.medium,
        border = androidx.compose.foundation.BorderStroke(1.dp, KitchenLine)) {
        Row(Modifier.padding(4.dp).selectableGroup(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            options.forEachIndexed { index, label ->
                val active = index == selected
                Box(Modifier.weight(1f).heightIn(min = 44.dp).clip(MaterialTheme.shapes.small)
                    .background(if (active) KitchenPeach else Color.Transparent)
                    .selectable(selected = active, role = Role.Tab, onClick = { onSelect(index) })
                    .let { base -> tag(index)?.let { base.testTag(it) } ?: base }
                    .padding(horizontal = 6.dp, vertical = 8.dp), contentAlignment = Alignment.Center) {
                    Text(label, style = MaterialTheme.typography.labelLarge, maxLines = 1,
                        color = if (active) KitchenTerracotta else KitchenMuted)
                }
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

/** Stable, equally sized tabs. Labels determine height; no text scaling override. */
@Composable fun KitchenNavigation(selected: Screen, onSelect: (Screen) -> Unit) {
    Surface(color = MaterialTheme.colorScheme.surface) {
        Column {
            HorizontalDivider(color = KitchenLine)
            Row(Modifier.fillMaxWidth().height(IntrinsicSize.Min).selectableGroup()) {
                listOf(Screen.TODAY, Screen.PLAN, Screen.SHOPPING, Screen.COOK, Screen.ASK).forEach { screen ->
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
                                    Screen.ASK -> KitchenIcon(KitchenGlyph.SPARKLE)
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

@Composable fun RecipeKindLabel(kind: RecipeKind) {
    val (background, color) = when (kind) {
        RecipeKind.MAIN -> KitchenPeach to KitchenTerracotta
        RecipeKind.SOUP -> Color(0xFFF7EBD0) to Color(0xFF77521F)
        RecipeKind.VEG -> KitchenSoft to KitchenGreen
        RecipeKind.BREAKFAST -> Color(0xFFFFEFCC) to Color(0xFF7A4B0E)
    }
    Surface(color = background, contentColor = color, shape = MaterialTheme.shapes.extraSmall) {
        Text(kind.label, Modifier.padding(horizontal = 8.dp, vertical = 3.dp), style = MaterialTheme.typography.labelMedium)
    }
}

@Composable fun RecipePortrait(recipe: Recipe, size: androidx.compose.ui.unit.Dp = 80.dp) {
    val background = when (recipe.kind) {
        RecipeKind.MAIN -> KitchenPeach
        RecipeKind.SOUP -> Color(0xFFF3EFD9)
        RecipeKind.VEG -> KitchenSoft
        RecipeKind.BREAKFAST -> Color(0xFFFFF3DC)
    }
    Box(Modifier.size(size).clip(MaterialTheme.shapes.medium).background(background), contentAlignment = Alignment.Center) {
        if (hasRecipePhoto(recipe.id)) {
            RecipePhoto(recipe, Modifier.fillMaxSize())
        } else {
            Text(recipe.emoji, Modifier.clearAndSetSemantics { }, fontSize = (size.value * 0.46f).sp)
        }
    }
}

data class KitchenTimerUi(val label: String, val remaining: String, val id: Long = 0L)

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

fun PlannerViewModel.timerUi(): KitchenTimerUi? = state.timers.minByOrNull { it.end }?.let {
    val seconds = ((it.end - now + 999) / 1000).coerceAtLeast(0)
    KitchenTimerUi(it.label, "%02d:%02d".format(seconds / 60, seconds % 60), it.id)
}
