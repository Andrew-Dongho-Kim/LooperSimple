package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.home.loop.Loops
import com.pnd.android.loop.ui.input.UserInput

@Composable
fun Home(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    val viewModel: LoopViewModel = viewModel()

    Box(modifier = modifier.fillMaxSize()) {
        Column {
            val scrollState = rememberScrollState()
            var loop by remember { mutableStateOf(LoopVo()) }

            Loops(
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding(),
                scrollState = scrollState,
                loopViewModel = loopViewModel,
            )
            UserInput(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                scrollState = scrollState,
                loop = loop,
                onLoopUpdated = { updatedLoop ->
                    loop = updatedLoop
                },
                onLoopSubmitted = { newLoop -> viewModel.addLoop(newLoop) },
            )
        }
        HomeAppBar(
            modifier = Modifier.statusBarsPadding()
        )
    }

}
