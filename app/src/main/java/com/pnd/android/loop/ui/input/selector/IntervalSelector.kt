package com.pnd.android.loop.ui.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.input.common.TextSelectorItem
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.textFormatter
import java.util.concurrent.TimeUnit

@Composable
fun IntervalSelector(
    selectedInterval: Long,
    onIntervalSelected: (Long) -> Unit,
    focusRequester: FocusRequester
) {
    val description = stringResource(R.string.desc_interval_selector)
    Column(
        modifier = Modifier
            .height(dimensionResource(id = R.dimen.user_input_selector_content_height))
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .focusRequester(focusRequester)
            .focusTarget()
            .semantics { contentDescription = description }
    ) {

        intervals.forEach { interval ->
            TextSelectorItem(
                text = textFormatter(intervalString(interval, "#")),
                selected = selectedInterval == interval,
                onClick = { onIntervalSelected(interval) }
            )
        }
    }
}


private val intervals = listOf(
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