package vn.team03.bepnha

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp

val KitchenGreen = Color(0xFF253F34)
val KitchenInk = Color(0xFF252E27)
val KitchenPaper = Color(0xFFF8F2E8)
val KitchenSurface = Color(0xFFFFFCF7)
val KitchenSoft = Color(0xFFE7EEE0)
val KitchenMuted = Color(0xFF5E655B)
val KitchenLine = Color(0xFFE6DDCE)
val KitchenPeach = Color(0xFFF6E2D4)
val KitchenTerracotta = Color(0xFFA2422B)
val KitchenGold = Color(0xFFFFDFB0)
val KitchenOnGreen = Color(0xFFDFE9D9)

@Composable fun BepNhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = KitchenGreen, onPrimary = Color.White,
            primaryContainer = KitchenSoft, onPrimaryContainer = KitchenInk,
            secondary = KitchenTerracotta, secondaryContainer = KitchenPeach,
            onSecondaryContainer = KitchenTerracotta, onSecondary = Color.White,
            background = KitchenPaper, onBackground = KitchenInk,
            surface = KitchenSurface, onSurface = KitchenInk, surfaceVariant = KitchenSoft,
            onSurfaceVariant = KitchenMuted, outline = KitchenMuted, outlineVariant = KitchenLine),
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)),
        typography = Typography(
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 34.sp, lineHeight = 41.sp, fontWeight = FontWeight.Medium),
            headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 27.sp, lineHeight = 35.sp, fontWeight = FontWeight.Medium),
            titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold),
            titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 23.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 21.sp),
            bodySmall = TextStyle(fontSize = 12.sp, lineHeight = 18.sp),
            labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 17.sp, fontWeight = FontWeight.SemiBold),
        ), content = content,
    )
}
