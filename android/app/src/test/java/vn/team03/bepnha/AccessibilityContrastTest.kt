package vn.team03.bepnha

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.pow

class AccessibilityContrastTest {
    @Test fun textPaletteMeetsFourPointFiveToOneOnItsActualSurfaces() {
        listOf(
            "Body on page" to (KitchenInk to KitchenPaper),
            "Secondary text on page" to (KitchenMuted to KitchenPaper),
            "Secondary text on tinted panels" to (KitchenMuted to KitchenSoft),
            "Primary buttons" to (Color.White to KitchenGreen),
            "Outlined controls" to (KitchenGreen to Color.White),
        ).forEach { (name, pair) ->
            val first = luminance(pair.first)
            val second = luminance(pair.second)
            val ratio = (maxOf(first, second) + 0.05) / (minOf(first, second) + 0.05)
            assertTrue("$name: $ratio", ratio >= 4.5)
        }
    }

    private fun luminance(color: Color): Double {
        fun linear(value: Float): Double = if (value <= 0.04045) value / 12.92 else ((value + 0.055) / 1.055).pow(2.4)
        return linear(color.red) * 0.2126 + linear(color.green) * 0.7152 + linear(color.blue) * 0.0722
    }
}
