package com.pnd.android.loop.ui.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.splineBasedDecay
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.util.isActive
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class DragAnchors { Start, Center, End }

/**
 * Tuning values for the swipe-to-reveal options behind a loop card.
 * Centralized so the gesture feel and reveal animation stay consistent.
 */
private object LoopOptionsReveal {
    /** Fraction of the card width the card slides to fully reveal the options. */
    const val ANCHOR_FRACTION = 0.3f

    /** Drag distance fraction that must be passed to settle on the next anchor. */
    const val POSITIONAL_THRESHOLD = 0.7f

    /** Options stay this faint until the card is dragged, then fade up to fully opaque. */
    const val MIN_OPTIONS_ALPHA = 0.3f

    const val SNAP_DURATION_MILLIS = 100

    val VELOCITY_THRESHOLD = 125.dp
    val OPTIONS_HEIGHT = 42.dp
}

@Immutable
data class LoopCardValues(
    val syncWithTime: Boolean = true,
    val isActive: Boolean = false,
    val isHighlighted: Boolean = false,
    val showAddToGroup: Boolean = true,
)

@Composable
fun LoopCardWithOption(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    cardValues: LoopCardValues,
    onEdit: (LoopBase) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    var isActive by remember { mutableStateOf(false) }
    LaunchedEffect(loop, loopViewModel) {
        loopViewModel.localDateTime.collect { currTime ->
            isActive = loop.isActive(currTime)
        }
    }

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteLoopDialog(
            loopTitle = loop.title,
            onDismiss = {
                showDeleteDialog = false
                blurState.off()
            },
            onDelete = { loopViewModel.deleteLoop(loop) }
        )
    }

    LoopCardWithOption(
        modifier = modifier,
        loop = loop,
        cardValues = cardValues.copy(isActive = isActive),
        onEnabled = { enabled ->
            val updated = loop.copyAs(enabled = enabled).asLoopVo()
            loopViewModel.addOrUpdateLoop(updated)
        },
        onStateChanged = { newLoop, doneState ->
            loopViewModel.doneLoop(
                loop = newLoop,
                doneState = doneState
            )
        },
        onEdit = onEdit,
        onShowDeleteDialog = {
            showDeleteDialog = true
            blurState.on()
        },
        onNavigateToGroupPicker = onNavigateToGroupPicker,
        onNavigateToDetailPage = onNavigateToDetailPage
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoopCardWithOption(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    cardValues: LoopCardValues,
    onEnabled: (Boolean) -> Unit,
    onStateChanged: (LoopBase, @LoopDoneVo.DoneState Int) -> Unit,
    onEdit: (LoopBase) -> Unit,
    onShowDeleteDialog: (Boolean) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        val state = rememberDraggableState(constraints = constraints)

        val coroutineScope = rememberCoroutineScope()
        if (!loop.isMock) {
            val loopColor = remember(loop.color) { Color(loop.color) }
            LoopOptions(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        // Fade the options in as the card slides away from center.
                        alpha = lerp(
                            start = LoopOptionsReveal.MIN_OPTIONS_ALPHA,
                            stop = 1f,
                            fraction = state.absFraction(),
                        )
                    }
                    .padding(
                        vertical = 8.dp,
                        horizontal = 12.dp,
                    )
                    .fillMaxWidth()
                    .height(LoopOptionsReveal.OPTIONS_HEIGHT),

                color = loopColor,
                enabled = loop.enabled,
                onEnabledLoop = { enabled ->
                    onEnabled(enabled)
                    coroutineScope.launch { state.animateTo(DragAnchors.Center) }
                },
                onEditLoop = {
                    onEdit(loop)
                    coroutineScope.launch { state.animateTo(DragAnchors.Center) }
                },
                onShowDeleteDialog = onShowDeleteDialog,
            )
        }

        LoopCard(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = state
                            .requireOffset()
                            .roundToInt(),
                        y = 0
                    )
                }
                .anchoredDraggable(
                    state = state,
                    orientation = Orientation.Horizontal,
                    enabled = !loop.isMock
                ),
            loop = loop,
            cardValues = cardValues,
            onStateChanged = onStateChanged,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun rememberDraggableState(
    constraints: Constraints,
): AnchoredDraggableState<DragAnchors> {
    val density = LocalDensity.current
    return remember(density) {
        val revealOffset = constraints.maxWidth * LoopOptionsReveal.ANCHOR_FRACTION
        AnchoredDraggableState(
            initialValue = DragAnchors.Center,
            anchors = DraggableAnchors {
                DragAnchors.Start at -revealOffset
                DragAnchors.Center at 0f
                DragAnchors.End at revealOffset
            },
            positionalThreshold = { distance -> distance * LoopOptionsReveal.POSITIONAL_THRESHOLD },
            velocityThreshold = { with(density) { LoopOptionsReveal.VELOCITY_THRESHOLD.toPx() } },
            snapAnimationSpec = tween(
                durationMillis = LoopOptionsReveal.SNAP_DURATION_MILLIS,
                easing = FastOutSlowInEasing
            ),
            decayAnimationSpec = splineBasedDecay(
                density = density
            )
        )
    }
}


/**
 * How far the card has been dragged from center, as a 0f..1f fraction,
 * regardless of drag direction (left or right).
 */
@OptIn(ExperimentalFoundationApi::class)
private fun AnchoredDraggableState<DragAnchors>.absFraction(): Float {
    val offset = requireOffset()
    return if (offset < 0) offset / anchors.minAnchor() else offset / anchors.maxAnchor()
}