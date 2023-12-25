package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.LoopViewModel

@Composable
fun Loops(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    loopViewModel: LoopViewModel,
) {
    val loops by loopViewModel.loops.observeAsState(emptyList())

    Box(modifier = modifier.background(MaterialTheme.colors.onSurface.copy(alpha = 0.02f))) {
        if (loops.isEmpty()) {
            EmptyLoops(modifier = Modifier.fillMaxSize())
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState, reverseScrolling = true)
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                loops.forEach { loop ->
                    key(loop.id) {
                        LoopCard(
                            loopViewModel = loopViewModel,
                            loop = loop,
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyLoops(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Text(
                text = stringResource(R.string.desc_no_loops),
                style = MaterialTheme.typography.subtitle1
            )
        }
    }
}
