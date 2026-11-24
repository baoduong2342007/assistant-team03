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

/** The one card style for list content: white, a hairline border, no shadow. */
val KitchenCardShape = RoundedCornerShape(20.dp)
val KitchenCardBorder = Color(0xFFEAE3D7)

/** The one big sans title per screen: the home greeting and the cook hero. */
val KitchenHeroTitle = TextStyle(fontSize = 24.sp, lineHeight = 32.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp)

@Composable fun BepNhaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = lightColorScheme(
            primary = KitchenGreen, onPrimary = Color.White,
            primaryContainer = KitchenSoft, onPrimaryContainer = KitchenInk,
            secondary = KitchenTerracotta, secondaryContainer = KitchenPeach,
            onSecondaryContainer = KitchenTerracotta, onSecondary = Color.White,
            background = KitchenPaper, onBackground = KitchenInk,
            surface = KitchenSurface, onSurface = KitchenInk, surfaceVariant = KitchenSoft,
            onSurfaceVariant = KitchenMuted, outline = KitchenMuted, outlineVariant = KitchenLine),
        shapes = Shapes(extraSmall = RoundedCornerShape(8.dp), small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp), large = RoundedCornerShape(20.dp), extraLarge = RoundedCornerShape(28.dp)),
        // One scale for every screen. Line heights are ~1.35–1.45× so Vietnamese stacked diacritics never clip.
        //   headline*  – serif brand/cook-along voice      titleLarge – screen and section headings
        //   titleMedium – group headers (a meal, a shelf)  titleSmall – one item: a dish, an ingredient, a step
        //   body*      – reading text and meta             label*     – buttons, chips and tags
        typography = Typography(
            headlineMedium = TextStyle(fontFamily = FontFamily.Serif, fontSize = 32.sp, lineHeight = 40.sp, fontWeight = FontWeight.Medium),
            headlineSmall = TextStyle(fontFamily = FontFamily.Serif, fontSize = 26.sp, lineHeight = 34.sp, fontWeight = FontWeight.Medium),
            titleLarge = TextStyle(fontSize = 20.sp, lineHeight = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.1).sp),
            titleMedium = TextStyle(fontSize = 17.sp, lineHeight = 24.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
            titleSmall = TextStyle(fontSize = 15.sp, lineHeight = 21.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
            bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.sp),
            bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.sp),
            bodySmall = TextStyle(fontSize = 13.sp, lineHeight = 18.sp, letterSpacing = 0.sp),
            labelLarge = TextStyle(fontSize = 14.sp, lineHeight = 20.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
            labelMedium = TextStyle(fontSize = 12.sp, lineHeight = 16.sp, fontWeight = FontWeight.SemiBold, letterSpacing = 0.sp),
            labelSmall = TextStyle(fontSize = 11.sp, lineHeight = 15.sp, fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
        ), content = content,
    )
}
