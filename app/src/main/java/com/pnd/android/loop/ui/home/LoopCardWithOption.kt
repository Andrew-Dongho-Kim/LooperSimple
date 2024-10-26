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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private enum class DragAnchors { Start, Center, End }

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LoopCardWithOption(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
    isSyncTime: Boolean,
    isHighlighted: Boolean
) {
    BoxWithConstraints(modifier = modifier) {
        val density = LocalDensity.current
        val state = remember {
            AnchoredDraggableState(
                initialValue = DragAnchors.Center,
                anchors = DraggableAnchors {
                    DragAnchors.Start at -constraints.maxWidth * 0.3f
                    DragAnchors.Center at 0f
                    DragAnchors.End at constraints.maxWidth * 0.3f
                },
                positionalThreshold = { distance -> distance * 0.7f },
                velocityThreshold = { with(density) { 125.dp.toPx() } },
                snapAnimationSpec = tween(
                    durationMillis = 100,
                    easing = FastOutSlowInEasing
                ),
                decayAnimationSpec = splineBasedDecay(
                    density = density
                )
            )
        }

        val coroutineScope = rememberCoroutineScope()
        if (!loop.isMock) {
            LoopOptions(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .graphicsLayer {
                        val fraction = state.absFraction()
                        alpha = 0.3f * (1 - fraction) + fraction * 1.0f
                    }
                    .padding(
                        vertical = 8.dp,
                        horizontal = 36.dp,
                    )
                    .fillMaxWidth()
                    .height(42.dp),
                blurState = blurState,
                title = loop.title,
                color = Color(loop.color),
                enabled = loop.enabled,
                onEnabled = { enabled ->
                    val updated = loop.copyAs(enabled = enabled).asLoopVo()
                    loopViewModel.addOrUpdateLoop(updated)
                    coroutineScope.launch { state.animateTo(DragAnchors.Center) }
                },
                onEdit = {
                    onEdit(loop)
                    coroutineScope.launch { state.animateTo(DragAnchors.Center) }
                },
                onDelete = { loopViewModel.removeLoop(loop) }
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
            loopViewModel = loopViewModel,
            loop = loop,
            onNavigateToDetailPage = onNavigateToDetailPage,
            isSyncTime = isSyncTime,
            isHighlighted = isHighlighted,
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
private fun AnchoredDraggableState<DragAnchors>.absFraction(): Float {
    val offset = requireOffset()
    return if (offset < 0) offset / anchors.minAnchor() else offset / anchors.maxAnchor()
}