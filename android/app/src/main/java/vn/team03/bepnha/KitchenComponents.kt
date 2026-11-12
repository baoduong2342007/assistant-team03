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
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class KitchenGlyph { BOWL, CLOCK, PEOPLE, SPARKLE }
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

@Composable fun KitchenActions(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        if (maxWidth < 248.dp || LocalDensity.current.fontScale > 1.25f) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                first(Modifier.fillMaxWidth())
                second(Modifier.fillMaxWidth())
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                first(Modifier.weight(1f))
                second(Modifier.weight(1f))
            }
        }
    }
}

@Composable fun RecipeKindLabel(kind: RecipeKind) {
    val (background, color) = when (kind) {
        RecipeKind.MAIN -> KitchenPeach to KitchenTerracotta
        RecipeKind.SOUP -> Color(0xFFF7EBD0) to Color(0xFF77521F)
        RecipeKind.VEG -> KitchenSoft to KitchenGreen
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
    }
    Box(Modifier.size(size).clip(MaterialTheme.shapes.medium).background(background), contentAlignment = Alignment.Center) {
        // The three dishes actually pictured in the bundled photograph share one decoded bitmap.
        if (recipe.id in setOf("pork", "soup", "greens")) {
            val bitmap = LocalMealPhoto.current
            Canvas(Modifier.fillMaxSize()) {
                val (x, y, width, height) = when (recipe.id) {
                    "soup" -> listOf(0.0f, 0.0f, 0.48f, 0.49f)
                    "greens" -> listOf(0.48f, 0.0f, 0.48f, 0.49f)
                    else -> listOf(0.20f, 0.36f, 0.58f, 0.62f)
                }
                drawImage(bitmap, srcOffset = IntOffset((bitmap.width * x).toInt(), (bitmap.height * y).toInt()),
                    srcSize = IntSize((bitmap.width * width).toInt(), (bitmap.height * height).toInt()),
                    dstSize = IntSize(this.size.width.toInt(), this.size.height.toInt()))
            }
        } else Text(recipe.emoji, fontSize = (size.value * 0.46f).sp)
    }
}
