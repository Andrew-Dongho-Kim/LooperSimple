package com.pnd.android.loop.ui.home.loop.input.selector

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pnd.android.loop.R
import com.pnd.android.loop.util.annotatedString
import com.pnd.android.loop.util.intervalString
import java.util.concurrent.TimeUnit

@Composable
fun IntervalSelector(
    modifier: Modifier = Modifier,
    selectedInterval: Long,
    maxInterval: Long,
    onIntervalSelected: (Long) -> Unit,
) {
    val description = stringResource(R.string.desc_interval_selector)
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .semantics { contentDescription = description }
    ) {
        intervals.forEach { interval ->
            TextSelectorItem(
                modifier = Modifier.alpha(
                    if (interval > maxInterval) 0.3f else 1f
                ),
                text = annotatedString(intervalString(interval, "#")),
                selected = selectedInterval == interval,
                onClick = { onIntervalSelected(interval) }
            )
        }
    }
}


private val intervals = listOf(
    TimeUnit.MINUTES.toMillis(0),
    TimeUnit.MINUTES.toMillis(5),
    TimeUnit.MINUTES.toMillis(10),
    TimeUnit.MINUTES.toMillis(20),
    TimeUnit.MINUTES.toMillis(30),
    TimeUnit.MINUTES.toMillis(40),
    TimeUnit.MINUTES.toMillis(50),
    TimeUnit.HOURS.toMillis(1),
    TimeUnit.HOURS.toMillis(2),
    TimeUnit.HOURS.toMillis(3),
    TimeUnit.HOURS.toMillis(4)
)