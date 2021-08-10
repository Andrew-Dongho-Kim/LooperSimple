package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.alarm.AlarmHelper
import com.pnd.android.loop.common.log
import com.pnd.android.loop.ui.home.HomeViewModel

private val logger = log("LoopUi")

@ExperimentalMaterialApi
@ExperimentalFoundationApi
@Composable
fun Loops(
    alarmHelper: AlarmHelper,
    scrollState: ScrollState,
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel()
    val vmState = viewModel.loops.observeAsState()

    Box(modifier = modifier.background(MaterialTheme.colors.onSurface.copy(alpha = 0.02f))) {
        val loops = vmState.value
        if (loops.isNullOrEmpty()) {
            EmptyLoops(
                modifier = Modifier.fillMaxSize()
            )
            logger.d { "Empty loops" }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(scrollState, reverseScrolling = true)
            ) {
                Spacer(modifier = Modifier.height(64.dp))

                loops.forEach { loop ->
                    key(loop.id) {
                        Loop(alarmHelper = alarmHelper, loop = loop)
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
                style = MaterialTheme.typography.h6
            )
        }
    }
}
