package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.pnd.android.loop.ui.home.loop.LoopViewModel

@Composable
fun LoopTimeline(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    val horizontalScrollState = rememberScrollState()
    Column(modifier = modifier) {
        TimelineContent(
            horizontalScrollState = horizontalScrollState,
        )
        HorizontalTimeBar(
            horizontalScrollState = horizontalScrollState,
        )
    }
}

@Composable
private fun TimelineContent(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
) {
    Box(modifier = modifier) {
        TimeGrid(
            horizontalScrollState = horizontalScrollState
        )
    }
}

@Composable
private fun TimeGrid(
    modifier: Modifier = Modifier,
    horizontalScrollState: ScrollState,
) {
    Row(modifier = Modifier) {

    }
}