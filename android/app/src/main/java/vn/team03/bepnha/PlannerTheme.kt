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

val KitchenGreen = Color(0xFF285343)
val KitchenInk = Color(0xFF253C32)
val KitchenPaper = Color(0xFFF8F7F2)
val KitchenSoft = Color(0xFFEDF2E9)
val KitchenMuted = Color(0xFF606E64)
val KitchenLine = Color(0xFFE4E8DF)
val KitchenPeach = Color(0xFFF6EBDD)
val KitchenTerracotta = Color(0xFF8B5635)

@Composable fun BepNhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = KitchenGreen, onPrimary = Color.White,
            primaryContainer = KitchenSoft, onPrimaryContainer = KitchenInk,
            secondary = KitchenTerracotta, secondaryContainer = KitchenPeach,
            onSecondaryContainer = KitchenInk, onSecondary = Color.White,
            background = KitchenPaper, onBackground = KitchenInk,
            surface = Color.White, onSurface = KitchenInk, surfaceVariant = KitchenSoft,
            onSurfaceVariant = KitchenMuted, outline = KitchenMuted, outlineVariant = KitchenLine),
        shapes = Shapes(extraSmall = RoundedCornerShape(10.dp), small = RoundedCornerShape(14.dp),
            medium = RoundedCornerShape(18.dp), large = RoundedCornerShape(24.dp), extraLarge = RoundedCornerShape(30.dp)),
        typography = Typography(
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 29.sp, lineHeight = 38.sp, fontWeight = FontWeight.Medium),
            headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 25.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium),
            titleLarge = TextStyle(fontSize = 21.sp, lineHeight = 28.sp, fontWeight = FontWeight.SemiBold),
            titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 25.sp, fontWeight = FontWeight.SemiBold),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 25.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 22.sp),
            bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 20.sp),
            labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.Medium),
            labelMedium = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, fontWeight = FontWeight.Medium),
        ), content = content,
    )
}
