package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.WineRed
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import java.time.LocalTime
import kotlin.math.ceil


@Composable
fun HorizontalTimeBar(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
) {
    val amPmHour by rememberAmPmHour(scrollState = horizontalScrollState)
    Row(modifier = modifier.horizontalScroll(horizontalScrollState)) {
        Box(modifier = Modifier.width(timelineWidth)) {
            (1..24).forEach { hour ->
                key(hour) {
                    TimeBarHeaderText(
                        hour = hour,
                        withAmPm = amPmHour.first == hour || amPmHour.second == hour
                    )
                }
            }
            LocalTimeIndicator()
        }
    }
}

@Composable
private fun BoxScope.TimeBarHeaderText(
    modifier: Modifier = Modifier,
    hour: Int,
    withAmPm: Boolean,
) {
    val text = timelineHourFormat(hour = hour, withAmPm = withAmPm)
    val start = timelineItemWidthDp.times(hour) - (timeBarTextWidth(text) / 2)

    Text(
        modifier = modifier
            .align(Alignment.CenterStart)
            .offset {
                IntOffset(
                    x = start.roundToPx(),
                    y = 0
                )
            }
            .width(timelineItemWidthDp),
        text = text,
        style = timeBarFontStyle.copy(
            color = AppColor.onSurface.copy(alpha = ContentAlpha.medium),
            textAlign = TextAlign.Start
        ),
        maxLines = 2,
        overflow = TextOverflow.Clip
    )

}

@Composable
private fun BoxScope.LocalTimeIndicator(
    modifier: Modifier = Modifier
) {
    val localTime by rememberLocalTime()

    val text = stringResource(
        id = R.string.format_hour_minute_24, localTime.hour, localTime.minute
    )
    val backgroundBounds = timeIndicatorBackgroundBounds(
        localTime = localTime,
        indicatorText = text,
    )

    val textBounds = localTime.timelineTextBounds(text = text)

    Box(modifier = modifier
        .offset {
            IntOffset(x = backgroundBounds.first.roundToPx(), y = 0)
        }
        .background(color = AppColor.surface)
        .background(color = AppColor.background)
        .width(backgroundBounds.second - backgroundBounds.first)
    ) {
        val start = textBounds.first - backgroundBounds.first
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    IntOffset(x = start.roundToPx(), y = 0)
                },
            text = text,
            style = timeBarFontStyle.copy(
                color = WineRed,
            )
        )
    }

}

@Composable
private fun rememberAmPmHour(
    scrollState: ScrollState,
): State<Pair<Int, Int>> {
    val itemWidth = timeLineItemWidthPx
    val textWidth = timeBarFontSizePx
    return remember(scrollState) {
        derivedStateOf {
            val index = ceil((scrollState.value + (textWidth / 2)) / itemWidth).toInt()
            val amIndex = if (index >= 12) -1 else index
            val pmIndex = if (index < 12) 12 else index

            amIndex to pmIndex
        }
    }
}

@Composable
private fun timeIndicatorBackgroundBounds(
    localTime: LocalTime,
    indicatorText: String,
): Pair<Dp, Dp> {
    val currHour = localTime.hour
    val hours = listOf(currHour - 1, currHour, currHour + 1)
    var newBounds = localTime.timelineTextBounds(indicatorText)

    hours.forEach { hour ->
        val headerText = timelineHourFormat(hour = hour, withAmPm = true)
        val headerBounds = hour.timelineTextBounds(text = headerText)

        if (newBounds.first > headerBounds.second) return@forEach
        if (newBounds.second < headerBounds.first) return@forEach

        newBounds = min(newBounds.first, headerBounds.first) to
                max(newBounds.second, headerBounds.second)
    }
    return newBounds
}

@Composable
fun LocalTime.timelineTextBounds(text: String): Pair<Dp, Dp> {
    val width = timeBarTextWidth(text = text)
    val start = timelineItemWidthDp.times(hour + (minute / 60f)) - width / 2
    val end = start + width
    return start to end
}

@Composable
fun Int.timelineTextBounds(text: String): Pair<Dp, Dp> {
    val width = timeBarTextWidth(text = text)
    val start = timelineItemWidthDp.times(this) - width / 2
    val end = start + width
    return start to end
}
