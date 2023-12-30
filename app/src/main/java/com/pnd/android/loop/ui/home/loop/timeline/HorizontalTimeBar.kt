package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.util.formatHour


@Composable
fun HorizontalTimeBar(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
) {
    Row(
        modifier = modifier.horizontalScroll(horizontalScrollState)
    ) {
        (1..24).forEach { hour ->
            TimeBarHeaderText(hour = hour)
        }
    }
}

@Composable
private fun TimeBarHeaderText(
    modifier: Modifier = Modifier,
    hour:Int
) {
    Box(
        modifier = modifier
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .width(itemGridWidth)
                .padding(end = 4.dp),
            text = formatHour(hour = hour, withAmPm = false),
            style = MaterialTheme.typography.caption.copy(
                color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
                textAlign = TextAlign.End
            ),
            maxLines = 1,
            overflow = TextOverflow.Clip
        )
    }
}