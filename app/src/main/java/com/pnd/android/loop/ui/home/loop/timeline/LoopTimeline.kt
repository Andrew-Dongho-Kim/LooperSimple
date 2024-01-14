package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.VerticalDashedDivider
import com.pnd.android.loop.ui.common.VerticalDivider
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.WineRed
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import java.time.LocalTime
import kotlin.math.max

@Composable
fun LoopTimeline(
    modifier: Modifier = Modifier,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val horizontalScrollState = rememberScrollState()
    Column(modifier = modifier) {
        TimeGrid(
            modifier = Modifier.height(timelineHeight),
            horizontalScrollState = horizontalScrollState,
            loops = loops,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
        HorizontalTimeBar(
            modifier = Modifier.padding(top = 4.dp),
            horizontalScrollState = horizontalScrollState,
        )
    }
}


@Composable
private fun TimeGrid(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        ScrollToLocalTime(
            scrollState = horizontalScrollState,
            timeGridWidth = constraints.maxWidth,
        )

        TimeGridContent(
            horizontalScrollState = horizontalScrollState,
            loops = loops,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

@Composable
private fun TimeGridContent(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    Row(
        modifier = modifier.horizontalScroll(horizontalScrollState)
    ) {
        Box(modifier = Modifier.width(timelineWidth)) {
            repeat(24) { time ->
                VerticalDivider(
                    modifier = Modifier.offset {
                        IntOffset(
                            x = timelineItemWidthDp.times(time + 1).roundToPx(),
                            y = 0
                        )
                    },
                    thickness = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.3f)
                )
            }
            TimelineLoops(
                modifier = Modifier.align(Alignment.BottomStart),
                loops = loops,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
            LocalTimeVerticalLineIndicator()
        }
    }
}


@Composable
private fun TimelineLoops(
    modifier: Modifier = Modifier,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val slots = rememberTimelineSlots(loops = loops)
    Column(
        modifier = modifier
            .background(color = AppColor.onSurface.copy(alpha = 0.1f))
            .width(timelineWidth),
        verticalArrangement = Arrangement.Bottom
    ) {
        slots.forEach { slot ->
            Box(modifier = Modifier.height(timelineItemHeightDp)) {
                slot.forEach { loop ->
                    key(loop.id) {
                        TimelineLoop(
                            loop = loop,
                            onNavigateToDetailPage = onNavigateToDetailPage,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TimelineLoop(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val alpha = animateCardAlphaWithMock(loopBase = loop)
    val shape = RoundShapes.small
    Box(
        modifier = modifier
            .alpha(0.7f)
            .padding(start = loop.timelineOffsetStart())
            .padding(1.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(
                color = Color(loop.color),
                shape = shape
            )
            .background(
                color = AppColor.surface.copy(alpha = 0.25f),
                shape = shape
            )
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.2f),
                shape = shape
            )
            .width(loop.timelineWidth())
            .height(timelineItemHeightDp)
            .clickable { onNavigateToDetailPage(loop) }
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(2.dp),
            text = loop.title,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.caption.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Normal
            )
        )
    }
}


@Composable
private fun LocalTimeVerticalLineIndicator(
    modifier: Modifier = Modifier,
) {
    val localTime by rememberLocalTime()
    VerticalDashedDivider(
        modifier = modifier.offset {
            IntOffset(
                x = localTime.timelineOffsetStart().roundToPx(),
                y = 0
            )
        },
        color = WineRed
    )
}

@Composable
private fun ScrollToLocalTime(
    scrollState: ScrollState,
    timeGridWidth: Int,
) {
    val start = timeGridWidth / 2
    val localTime = LocalTime.now()
    val offset = (localTime.hour + (localTime.minute / 60f)) * timeLineItemWidthPx

    LaunchedEffect(key1 = Unit) {
        scrollState.scrollTo(max(0, (offset - start).toInt()))
    }
}


@Composable
private fun rememberTimelineSlots(loops: List<LoopBase>) = remember(loops) {
    val slots = List(MAX_TIME_SLOTS) { mutableListOf<LoopBase>() }
    loops.forEach { loop ->
        val properSlot = slots.findLast { slot -> !isIntersect(slot, loop) }
        properSlot?.add(loop)
    }
    slots.filter { it.isNotEmpty() }
}


private fun isIntersect(slot: List<LoopBase>, another: LoopBase): Boolean {
    slot.forEach { loop ->
        if (loop.loopStart <= another.loopStart && another.loopStart < loop.loopEnd) return true
        if (loop.loopStart < another.loopEnd && another.loopEnd <= loop.loopEnd) return true
    }
    return false
}

@Composable
private fun animateCardAlphaWithMock(loopBase: LoopBase): Float {
    if (!loopBase.isMock) {
        return 1f
    }
    val transition = rememberInfiniteTransition("CreateLoopTransitions")
    val alpha by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = 1_500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaAnimForCreateLoop",
    )

    return alpha
}


private const val MAX_TIME_SLOTS = 5