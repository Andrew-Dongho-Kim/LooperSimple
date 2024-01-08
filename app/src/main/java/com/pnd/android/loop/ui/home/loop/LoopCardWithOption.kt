package com.pnd.android.loop.ui.home.loop

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.rememberSwipeableState
import androidx.compose.material.swipeable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.isMock
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun LoopCardWithOption(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    showActiveDays: Boolean
) {
    BoxWithConstraints(modifier = modifier) {
        val swipeState = rememberSwipeableState(
            initialValue = 0,
            animationSpec = tween(
                durationMillis = 100,
                easing = FastOutSlowInEasing,
            )
        )

        val coroutineScope = rememberCoroutineScope()
        if (!loop.isMock()) {
            LoopOptions(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(
                        vertical = 8.dp,
                        horizontal = 36.dp,
                    )
                    .fillMaxWidth()
                    .height(46.dp),
                color = Color(loop.color),
                onEdit = {
                    coroutineScope.launch { swipeState.animateTo(0) }
                },
                onDelete = { loopViewModel.removeLoop(loop) }
            )
        }

        LoopCard(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = swipeState.offset.value.roundToInt(),
                        y = 0
                    )
                }
                .swipeable(
                    state = swipeState,
                    anchors = mapOf(0f to 0, (constraints.maxWidth * 0.4f) to 1),
                    orientation = Orientation.Horizontal
                )
                .graphicsLayer {

                },
            loopViewModel = loopViewModel,
            loop = loop,
            showActiveDays = showActiveDays
        )
    }
}