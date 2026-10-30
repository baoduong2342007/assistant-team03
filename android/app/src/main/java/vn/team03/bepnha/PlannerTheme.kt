package vn.team03.bepnha

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val KitchenGreen = Color(0xFF49613C)
val KitchenInk = Color(0xFF293225)
val KitchenPaper = Color(0xFFFAF9F5)
val KitchenSoft = Color(0xFFEDF1E7)
val KitchenMuted = Color(0xFF68725F)
val KitchenLine = Color(0xFFE1E5D8)

@Composable fun BepNhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(primary = KitchenGreen, onPrimary = Color.White,
            primaryContainer = KitchenSoft, onPrimaryContainer = KitchenInk,
            secondary = Color(0xFF88613C), secondaryContainer = Color(0xFFF5ECDC),
            background = KitchenPaper, onBackground = KitchenInk,
            surface = Color.White, onSurface = KitchenInk, surfaceVariant = KitchenSoft,
            onSurfaceVariant = KitchenMuted, outlineVariant = KitchenLine),
        typography = Typography(
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 28.sp, lineHeight = 35.sp, fontWeight = FontWeight.Medium),
            headlineSmall = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.SemiBold),
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
