package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import kotlin.math.ceil


@Composable
fun HorizontalTimeBar(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
) {
    val amPmIndex by rememberAmPmDisplayIndex(scrollState = horizontalScrollState)
    val hourTextHalfWidth = timeBarFontSizeDp(timelineHourFormat(hour = 1, withAmPm = false)) / 2
    Row(
        modifier = modifier
            .horizontalScroll(horizontalScrollState)
            .padding(start = timelineItemWidthDp - hourTextHalfWidth)
    ) {
        (1..24).forEach { hour ->
            TimeBarHeaderText(
                hour = hour,
                withAmPm = amPmIndex.first == hour || amPmIndex.second == hour
            )
        }
        Spacer(modifier = Modifier.width(timelineWidthExtra))
    }
}

@Composable
private fun TimeBarHeaderText(
    modifier: Modifier = Modifier,
    hour: Int,
    withAmPm: Boolean,
) {
    Box(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .width(timelineItemWidthDp),
            text = timelineHourFormat(hour = hour, withAmPm = withAmPm),
            style = timeBarFontStyle.copy(
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                textAlign = TextAlign.Start
            ),
            maxLines = 2,
            overflow = TextOverflow.Clip
        )
    }
}

@Composable
private fun rememberAmPmDisplayIndex(
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

