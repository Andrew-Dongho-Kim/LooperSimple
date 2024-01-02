package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.MaterialTheme
import androidx.compose.material3.DividerDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.ui.home.loop.LoopViewModel
import com.pnd.android.loop.ui.theme.RoundShapes
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import kotlin.math.max

@Composable
fun LoopTimeline(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loops: List<LoopWithDone>,
) {
    val horizontalScrollState = rememberScrollState()
    Column(modifier = modifier) {
        TimelineContent(
            horizontalScrollState = horizontalScrollState,
            loops = loops
        )
        HorizontalTimeBar(
            modifier = Modifier.padding(top = 4.dp),
            horizontalScrollState = horizontalScrollState,
        )
    }
}

@Composable
private fun TimelineContent(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
    loops: List<LoopWithDone>,
) {
    Box(modifier = modifier) {
        TimeGrid(
            modifier = Modifier.height(timelineHeight),
            horizontalScrollState = horizontalScrollState,
        )
        TimelineLoops(
            modifier = Modifier.align(Alignment.BottomStart),
            horizontalScrollState = horizontalScrollState,
            loops = loops
        )
    }
}

@Composable
private fun TimelineLoops(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
    loops: List<LoopWithDone>
) {
    Row(modifier = modifier.horizontalScroll(state = horizontalScrollState)) {
        Column(
            modifier = Modifier
                .background(color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f))
                .width(timelineWidth)
                .fillMaxHeight(),
            verticalArrangement = Arrangement.Bottom
        ) {
            loops.forEach { loop ->
                key(loop.id) {
                    TimelineLoop(loop = loop)
                }
            }
        }
    }
}

@Composable
private fun TimelineLoop(
    modifier: Modifier = Modifier,
    loop: LoopBase
) {
    val shape = RoundShapes.small
    Box(
        modifier = modifier
            .alpha(0.7f)
            .padding(start = loop.timelineOffsetStart())
            .background(
                color = Color(loop.color),
                shape = shape
            )
            .background(
                color = MaterialTheme.colors.surface.copy(alpha = 0.25f),
                shape = shape
            )
            .width(loop.timelineWidth())
            .height(timelineItemHeightDp)
    )
}

@Composable
private fun TimeGrid(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
) {
    BoxWithConstraints(modifier = modifier) {
        val timeGridWidth = constraints.maxWidth
        Row(
            modifier = Modifier.horizontalScroll(horizontalScrollState)
        ) {
            Box(modifier = Modifier.width(timelineWidth)) {
                ScrollToLocalTime(
                    scrollState = horizontalScrollState,
                    timeGridWidth = timeGridWidth,
                )
                repeat(24) { time ->
                    VerticalDivider(
                        modifier = Modifier.offset {
                            IntOffset(
                                x = timelineItemWidthDp.times(time + 1).roundToPx(),
                                y = 0
                            )
                        },
                        thickness = 0.5.dp,
                        color = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
                    )
                }
                LocalTimeVerticalLineIndicator()
            }
        }
    }

}

@Composable
private fun LocalTimeVerticalLineIndicator(
    modifier: Modifier = Modifier,
) {
    var localTime by remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(key1 = Unit) {
        while (isActive) {
            val next = LocalTime.of(
                localTime.hour + if (localTime.minute >= 59) 1 else 0,
                localTime.minute + if (localTime.minute < 59) 1 else 0
            )
            delay(localTime.until(next, ChronoUnit.MILLIS))
            localTime = LocalTime.now()
        }
    }
    VerticalDivider(
        modifier = modifier.offset {
            IntOffset(
                x = localTime.timelineOffsetStart().roundToPx(),
                y = 0
            )
        },
        thickness = 0.5.dp,
        color = Color.Red
    )
}

@Composable
private fun ScrollToLocalTime(
    scrollState: ScrollState,
    timeGridWidth: Int,
) {
    val start = timeGridWidth / 2
    val offset = LocalTime.now().hour * timeLineItemWidthPx

    LaunchedEffect(key1 = Unit) {
        scrollState.scrollTo(max(0, (offset - start).toInt()))
    }
}

@Composable
private fun VerticalDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = DividerDefaults.Thickness,
    color: Color = DividerDefaults.color,
) {
    val targetThickness = if (thickness == Dp.Hairline) {
        (1f / LocalDensity.current.density).dp
    } else {
        thickness
    }
    Box(
        modifier
            .fillMaxHeight()
            .width(targetThickness)
            .background(color = color)
    )
}