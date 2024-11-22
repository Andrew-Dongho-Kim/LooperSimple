package com.pnd.android.loop.ui.home.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.VerticalDashedDivider
import com.pnd.android.loop.ui.common.VerticalDivider
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.WineRed
import com.pnd.android.loop.ui.theme.onSurface
import java.time.LocalTime
import kotlin.math.max

@Composable
fun LoopTimeline(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
) {
    val horizontalScrollState = rememberScrollState()

    Column(modifier = modifier) {
        TimeGrid(
            modifier = Modifier.height(timelineHeight),
            blurState = blurState,
            horizontalScrollState = horizontalScrollState,
            loopViewModel = loopViewModel,
            loops = loops,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onEdit = onEdit,
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
    blurState: BlurState,
    horizontalScrollState: ScrollState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
) {
    BoxWithConstraints(modifier = modifier) {
        ScrollToLocalTime(
            scrollState = horizontalScrollState,
            timeGridWidth = constraints.maxWidth,
        )

        TimeGridContent(
            horizontalScrollState = horizontalScrollState,
            blurState = blurState,
            loopViewModel = loopViewModel,
            loops = loops,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onEdit = onEdit,
        )
    }
}

@Composable
private fun TimeGridContent(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    horizontalScrollState: ScrollState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
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
                blurState = blurState,
                loopViewModel = loopViewModel,
                loops = loops,
                onNavigateToDetailPage = onNavigateToDetailPage,
                onEdit = onEdit,
            )
            LocalTimeVerticalLineIndicator()
        }
    }
}


@Composable
private fun TimelineLoops(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
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
                        TimelineItem(
                            modifier = Modifier.offset {
                                IntOffset(x = loop.timelineOffsetStart().roundToPx(), y = 0)
                            },
                            blurState = blurState,
                            loop = loop,
                            onNavigateToDetailPage = onNavigateToDetailPage,
                            onEdit = onEdit,
                            onDelete = { loop -> loopViewModel.removeLoop(loop) }
                        )
                    }
                }
            }
        }
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
        if (loop.startInDay <= another.startInDay && another.startInDay < loop.endInDay) return true
        if (loop.startInDay < another.endInDay && another.endInDay <= loop.endInDay) return true
    }
    return false
}


private const val MAX_TIME_SLOTS = 5