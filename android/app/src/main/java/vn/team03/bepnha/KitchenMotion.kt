package vn.team03.bepnha

import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.SeekableTransitionState
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.SaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

internal sealed interface PlannerPage {
    data class ScreenPage(val screen: Screen) : PlannerPage
    data object Chat : PlannerPage
    data object Cooking : PlannerPage
}

private val PlannerPage.isDetail: Boolean
    get() = this !is PlannerPage.ScreenPage || screen == Screen.FAMILY || screen == Screen.FAVORITES

private val routeSpring = spring<Float>(dampingRatio = 0.9f, stiffness = 430f, visibilityThreshold = 0.002f)
internal val selectionSpring = spring<Float>(dampingRatio = 0.84f, stiffness = 560f)
internal const val ComponentMotionMillis = 160

@Composable
internal fun rememberMotionEnabled(): Boolean =
    rememberCoroutineScope().coroutineContext[MotionDurationScale]?.scaleFactor != 0f

@Composable
internal fun Modifier.pressCompression(vararg interactions: MutableInteractionSource): Modifier {
    val pressed = interactions.map { it.collectIsPressedAsState() }.any { it.value }
    val scale = animateFloatAsState(
        if (pressed) 0.98f else 1f,
        spring(dampingRatio = 0.85f, stiffness = 900f, visibilityThreshold = 0.001f),
        label = "press scale",
    )
    return drawWithContent { scale(scale.value) { this@drawWithContent.drawContent() } }
}

internal fun Modifier.inactiveContent(inactive: Boolean): Modifier {
    if (!inactive) return this
    return clearAndSetSemantics { hideFromAccessibility() }
        .focusProperties { canFocus = false }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                }
            }
        }
}

@Composable
internal fun <T> KitchenContentTransition(
    target: T,
    label: String,
    modifier: Modifier = Modifier,
    direction: ((T, T) -> Int)? = null,
    content: @Composable (T) -> Unit,
) {
    val motion = rememberMotionEnabled()
    val shift = with(LocalDensity.current) { 12.dp.roundToPx() }
    AnimatedContent(
        targetState = target,
        modifier = modifier.clipToBounds(),
        transitionSpec = {
            if (!motion) EnterTransition.None togetherWith ExitTransition.None
            else {
                val fadeIn = fadeIn(tween(ComponentMotionMillis))
                val fadeOut = fadeOut(tween(ComponentMotionMillis))
                val offset = direction?.invoke(initialState, targetState)?.times(shift) ?: 0
                if (offset == 0) fadeIn togetherWith fadeOut
                else (fadeIn + slideInVertically(tween(ComponentMotionMillis)) { offset }) togetherWith
                    (fadeOut + slideOutVertically(tween(ComponentMotionMillis)) { -offset })
            }.using(null)
        },
        label = label,
    ) { value ->
        Box(Modifier.inactiveContent(value != target)) { content(value) }
    }
}

@Composable
internal fun PlannerMotionHost(
    page: PlannerPage,
    backDestination: PlannerPage?,
    backEnabled: Boolean,
    onBack: () -> Unit,
    screenStates: SaveableStateHolder,
    contentPadding: PaddingValues,
    content: @Composable (PlannerPage, Boolean) -> Unit,
) {
    val state = remember { SeekableTransitionState(page) }
    val transition = rememberTransition(state, label = "planner page")
    val motion = rememberMotionEnabled()
    val latestPage by rememberUpdatedState(page)
    val latestDestination by rememberUpdatedState(backDestination)
    val latestBackEnabled by rememberUpdatedState(backEnabled)
    val latestBack by rememberUpdatedState(onBack)
    var seekingFrom by remember { mutableStateOf<PlannerPage?>(null) }
    val previousPadding = remember { mutableStateMapOf<PlannerPage, PaddingValues>() }
    val direction = LocalLayoutDirection.current
    val shift = with(LocalDensity.current) { 28.dp.roundToPx() } *
        if (direction == LayoutDirection.Ltr) 1 else -1

    PredictiveBackHandler(enabled = backEnabled && backDestination != null) { events ->
        val from = latestPage
        val target = latestDestination
        try {
            events.collect { event ->
                if (target != null && motion && latestBackEnabled && latestPage == from && latestDestination == target) {
                    if (seekingFrom == null) {
                        seekingFrom = from
                        state.snapTo(from)
                    }
                    state.seekTo(event.progress, target)
                }
            }
            if (target != null && latestBackEnabled && latestPage == from && latestDestination == target) {
                latestBack()
            }
        } finally {
            seekingFrom = null
        }
    }

    LaunchedEffect(page, seekingFrom, motion) {
        when {
            !motion -> state.snapTo(page)
            seekingFrom == page -> Unit
            state.currentState != page -> {
                // Tab switches keep the linear 160 ms duration. A detail route springs along
                // the same path that the system Back gesture can seek.
                val detail = state.currentState.isDetail || page.isDetail
                state.animateTo(page, if (detail) routeSpring else null)
            }
            state.targetState != page -> {
                // A cancelled Back gesture leaves the logical page untouched.
                coroutineScope {
                    animate(state.fraction, 0f, animationSpec = routeSpring) { fraction, _ ->
                        launch(start = CoroutineStart.UNDISPATCHED) { state.seekTo(fraction.coerceIn(0f, 1f)) }
                    }
                }
                state.snapTo(page)
            }
        }
    }

    transition.AnimatedContent(
        modifier = Modifier.fillMaxSize().clipToBounds(),
        transitionSpec = {
            if (!motion) EnterTransition.None togetherWith ExitTransition.None
            else {
                val detail = initialState.isDetail || targetState.isDetail
                val duration = if (detail) 250 else 160
                val returning = initialState.isDetail && !targetState.isDetail
                val enter = fadeIn(tween(duration, easing = LinearEasing))
                val exit = fadeOut(tween(duration, easing = LinearEasing))
                if (detail) {
                    (enter + slideInHorizontally(tween(duration, easing = LinearEasing)) {
                        if (returning) -shift else shift
                    }) togetherWith (exit + slideOutHorizontally(tween(duration, easing = LinearEasing)) {
                        if (returning) shift else -shift
                    })
                } else enter togetherWith exit
            }.using(null)
        },
    ) { destination ->
        val active = destination == page
        val key = when (destination) {
            PlannerPage.Chat -> "assistant"
            PlannerPage.Cooking -> "cook-along"
            is PlannerPage.ScreenPage -> destination.screen.name
        }
        // Preserve the measured viewport of an outgoing list when the next page hides the footer.
        val padding = if (active) contentPadding else previousPadding[destination] ?: contentPadding
        Box(Modifier.fillMaxSize().inactiveContent(!active).padding(padding).consumeWindowInsets(padding)) {
            screenStates.SaveableStateProvider(key) { content(destination, active) }
        }
    }

    LaunchedEffect(page, contentPadding, direction) {
        snapshotFlow {
            PaddingValues(
                start = contentPadding.calculateStartPadding(direction),
                top = contentPadding.calculateTopPadding(),
                end = contentPadding.calculateEndPadding(direction),
                bottom = contentPadding.calculateBottomPadding(),
            )
        }.collect { previousPadding[page] = it }
    }
}
