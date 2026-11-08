package vn.team03.bepnha

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val KitchenGreen = Color(0xFF49613C)
val KitchenInk = Color(0xFF293225)
val KitchenPaper = Color(0xFFF7F8F5)
val KitchenSoft = Color(0xFFEDF1E7)
val KitchenMuted = Color(0xFF52604D)
val KitchenLine = Color(0xFFDCE2D7)

@Composable fun BepNhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = KitchenGreen, onPrimary = Color.White,
            primaryContainer = KitchenSoft, onPrimaryContainer = KitchenInk,
            secondary = KitchenGreen, onSecondary = Color.White,
            secondaryContainer = KitchenSoft, onSecondaryContainer = KitchenInk,
            background = KitchenPaper, onBackground = KitchenInk,
            surface = Color.White, onSurface = KitchenInk,
            surfaceVariant = KitchenSoft, onSurfaceVariant = KitchenMuted,
            outline = KitchenMuted, outlineVariant = KitchenLine,
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(12.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(12.dp),
            large = RoundedCornerShape(16.dp),
            extraLarge = RoundedCornerShape(12.dp),
        ),
        typography = Typography(
            headlineMedium = TextStyle(fontFamily = FontFamily.SansSerif, fontSize = 28.sp, lineHeight = 36.sp, fontWeight = FontWeight.SemiBold),
            headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
            titleLarge = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleSmall = TextStyle(fontSize = 18.sp, lineHeight = 26.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 28.sp),
            bodyMedium = TextStyle(fontSize = 18.sp, lineHeight = 28.sp),
            bodySmall = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            labelLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, fontWeight = FontWeight.Medium),
            labelSmall = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
        ),
        content = content,
    )
}
