package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.home.loop.LoopViewModel
import com.pnd.android.loop.ui.home.loop.Loops
import com.pnd.android.loop.ui.input.UserInput

@Composable
fun Home(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Column {
            val lazyListState = rememberLazyListState()
            var loop by remember { mutableStateOf(LoopVo()) }

            Loops(
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding(),
                lazyListState = lazyListState,
                loopViewModel = loopViewModel,
            )
            UserInput(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                lazyListState = lazyListState,
                loop = loop,
                onLoopUpdated = { updatedLoop ->
                    loop = updatedLoop
                },
                onLoopSubmitted = { newLoop -> loopViewModel.addOrUpdateLoop(newLoop) },
            )
        }
        HomeAppBar(
            modifier = Modifier.statusBarsPadding(),
            loopViewModel = loopViewModel,
        )
    }
}
