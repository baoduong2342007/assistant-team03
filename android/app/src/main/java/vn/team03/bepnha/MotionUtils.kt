package vn.team03.bepnha

import android.os.Build
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Standard iOS-style spring specifications.
 * Uses physical spring curves instead of artificial duration/bezier curves.
 */
fun <T> iosSpring(
    dampingRatio: Float = Spring.DampingRatioLowBouncy, // ~0.75-0.8: snappy with subtle overshoot
    stiffness: Float = Spring.StiffnessMediumLow       // responsive and smooth
): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

fun <T> iosBouncySpring(
    dampingRatio: Float = Spring.DampingRatioMediumBouncy, // bouncier for tab icons, bubbles
    stiffness: Float = Spring.StiffnessMediumLow
): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

fun <T> iosGentleSpring(
    dampingRatio: Float = Spring.DampingRatioNoBouncy,
    stiffness: Float = Spring.StiffnessLow
): SpringSpec<T> = spring(dampingRatio = dampingRatio, stiffness = stiffness)

fun iosOffsetSpring(): SpringSpec<IntOffset> = iosSpring()

fun iosSizeSpring(): SpringSpec<IntSize> = iosSpring()

/**
 * iOS-style tactile press-down bounce:
 * Slightly scales down when finger presses down, springs back on release,
 * and emits a subtle haptic tap on click.
 */
fun Modifier.bounceClick(
    scaleDown: Float = 0.96f,
    enabled: Boolean = true,
    onClick: () -> Unit,
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed && enabled) scaleDown else 1f,
        animationSpec = iosSpring(),
        label = "bounceScale"
    )
    val haptic = LocalHapticFeedback.current

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .clickable(
            interactionSource = interactionSource,
            indication = null, // Suppress standard Android circle ripple
            enabled = enabled,
            onClick = {
                try {
                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                } catch (_: Exception) { }
                onClick()
            }
        )
}

/**
 * iOS-style clean bar surface:
 * Provides a clean background tint and hairline border without blurring foreground content.
 */
fun Modifier.iosBarSurface(
    color: Color = KitchenPaper,
    borderColor: Color = KitchenLine,
    borderAtBottom: Boolean = true
): Modifier = this
    .background(color)
    .drawBehind {
        val y = if (borderAtBottom) size.height else 0f
        drawLine(
            color = borderColor,
            start = androidx.compose.ui.geometry.Offset(0f, y),
            end = androidx.compose.ui.geometry.Offset(size.width, y),
            strokeWidth = 1f
        )
    }

/**
 * Staggered cascade entrance modifier for Lazy lists:
 * Each item cascades in with a progressive delay and upward spring slide.
 */
fun Modifier.staggeredEntrance(
    index: Int,
    baseDelayMs: Long = 35L,
): Modifier = composed {
    val alphaAnim = remember { Animatable(0f) }
    val offsetYAnim = remember { Animatable(32f) }

    LaunchedEffect(Unit) {
        delay(index * baseDelayMs)
        alphaAnim.animateTo(1f, spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow))
    }
    LaunchedEffect(Unit) {
        delay(index * baseDelayMs)
        offsetYAnim.animateTo(0f, iosSpring())
    }

    this.graphicsLayer {
        alpha = alphaAnim.value
        translationY = offsetYAnim.value
    }
}

/**
 * iOS Clock-style Rolling Ticker Text for countdown timers:
 * Digits roll vertically into place when numbers change.
 */
@Composable
fun RollingTickerText(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    color: Color = Color.Unspecified,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) { },
        verticalAlignment = Alignment.CenterVertically
    ) {
        text.forEachIndexed { charIndex, char ->
            AnimatedContent(
                targetState = char,
                transitionSpec = {
                    val isForward = targetState > initialState
                    val enter = slideInVertically(animationSpec = iosOffsetSpring()) { height -> if (isForward) height else -height } + fadeIn()
                    val exit = slideOutVertically(animationSpec = iosOffsetSpring()) { height -> if (isForward) -height else height } + fadeOut()
                    (enter togetherWith exit) as ContentTransform
                },
                label = "ticker-$charIndex"
            ) { targetChar ->
                androidx.compose.material3.Text(
                    text = targetChar.toString(),
                    style = style,
                    color = color
                )
            }
        }
    }
}
