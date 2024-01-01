package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.util.toLocalTime


val timelineItemWidth = 40.dp
val timelineItemHeight = 32.dp

val timelineHeight = timelineItemHeight * 5

val timeBarFontStyle @Composable get() = MaterialTheme.typography.caption
val timeBarFontSize @Composable get() = with(LocalDensity.current) { timeBarFontStyle.fontSize.toDp() }


fun LoopBase.timelineOffsetStart(): Dp {
    val startTime = loopStart.toLocalTime()

    return timelineItemWidth.times(startTime.hour + (startTime.minute / 60f))
}

fun LoopBase.timelineWidth(): Dp {
    val startTime = loopStart.toLocalTime()
    val endTime = loopEnd.toLocalTime()

    return timelineItemWidth.times(
        (endTime.hour - startTime.hour) + ((endTime.minute - startTime.minute) / 60f)
    )
}

fun timelineLoopWidth(times: Float) = timelineItemWidth * times