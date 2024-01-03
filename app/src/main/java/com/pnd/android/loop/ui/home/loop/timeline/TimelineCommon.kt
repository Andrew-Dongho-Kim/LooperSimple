package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.util.toLocalTime
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import java.time.LocalTime
import java.time.temporal.ChronoUnit


val timelineItemWidthDp = 60.dp
val timelineItemHeightDp = 36.dp
val timeLineItemWidthPx
    @Composable get() = with(LocalDensity.current) { timelineItemWidthDp.toPx() }
val timelineItemHeightPx
    @Composable get() = with(LocalDensity.current) { timelineItemHeightDp.toPx() }

val timelineHeight = timelineItemHeightDp.times(5)
val timelineWidth
    @Composable get() = timelineItemWidthDp.times(24) + timelineWidthExtra
private val timelineWidthExtra
    @Composable get() = with(LocalDensity.current) { timeBarFontStyle.fontSize.toDp() }


@Composable
fun measureTextWidth(text: String, style: TextStyle): Dp {
    val textMeasurer = rememberTextMeasurer()
    val widthInPixels = textMeasurer.measure(text, style).size.width
    return with(LocalDensity.current) { widthInPixels.toDp() }
}

val timeBarFontStyle
    @Composable get() = MaterialTheme.typography.caption

val timeBarFontSizePx
    @Composable get() = with(LocalDensity.current) { timeBarFontStyle.fontSize.toPx() }

@Composable
fun timeBarTextWidth(text: String) = measureTextWidth(text, timeBarFontStyle)


fun LocalTime.timelineOffsetStart() =
    timelineItemWidthDp.times(hour + (minute / 60f))

fun LoopBase.timelineOffsetStart() =
    loopStart.toLocalTime().timelineOffsetStart()


fun LoopBase.timelineWidth(): Dp {
    val startTime = loopStart.toLocalTime()
    val endTime = loopEnd.toLocalTime()

    return timelineItemWidthDp.times(
        (endTime.hour - startTime.hour) + ((endTime.minute - startTime.minute) / 60f)
    )
}

@Composable
fun timelineHourFormat(hour: Int, withAmPm: Boolean = true): String {
    return stringResource(
        id = if (withAmPm) {
            if (hour < 12) R.string.timeline_am_hour else R.string.timeline_pm_hour
        } else {
            R.string.timeline_hour
        },
        formatArgs = arrayOf((hour % 12).run { if (this == 0) 12 else this })
    )
}


@Composable
fun rememberLocalTime(): State<LocalTime> {
    val localTime = remember { mutableStateOf(LocalTime.now()) }
    LaunchedEffect(key1 = Unit) {
        while (isActive) {
            val next = LocalTime.of(
                localTime.value.hour + if (localTime.value.minute >= 59) 1 else 0,
                localTime.value.minute + if (localTime.value.minute < 59) 1 else 0
            )
            delay(localTime.value.until(next, ChronoUnit.MILLIS))
            localTime.value = LocalTime.now()
        }
    }
    return localTime
}
