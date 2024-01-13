package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.ui.home.loop.LoopViewModel
import com.pnd.android.loop.ui.home.loop.Loops
import com.pnd.android.loop.ui.home.loop.input.UserInput
import com.pnd.android.loop.ui.home.loop.input.rememberUserInputState

@Composable
fun Home(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    val snackBarHostState = remember { SnackbarHostState() }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 48.dp + 56.dp),
                hostState = snackBarHostState
            )
        },
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.statusBars)
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.ime),
    )
    { contentPadding ->
        HomeContent(
            modifier = Modifier.padding(contentPadding),
            snackBarHostState = snackBarHostState,
            loopViewModel = loopViewModel,
        )
    }
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    snackBarHostState: SnackbarHostState,
    loopViewModel: LoopViewModel,
) {
    Box(modifier = modifier) {
        Column {
            val lazyListState = rememberLazyListState()
            val inputState = rememberUserInputState()

            Loops(
                modifier = Modifier
                    .weight(1f)
                    .statusBarsPadding(),
                inputState = inputState,
                lazyListState = lazyListState,
                loopViewModel = loopViewModel,
            )

            val context = LocalContext.current
            UserInput(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding(),
                inputState = inputState,
                lazyListState = lazyListState,
                onEnsureLoop = { loop ->
                    ensureLoop(
                        context = context,
                        loop = loop,
                        hostState = snackBarHostState
                    )
                },
                onLoopSubmitted = { newLoop -> loopViewModel.addOrUpdateLoop(newLoop.asLoopVo()) }
            )
        }
        HomeAppBar(
            modifier = Modifier.statusBarsPadding(),
            loopViewModel = loopViewModel,
        )
    }
}


private suspend fun ensureLoop(
    context: Context,
    loop: LoopBase,
    hostState: SnackbarHostState
): Boolean {

    // Empty title check
    if (loop.title.trim().isEmpty()) {
        hostState.showSnackbar(
            message = context.getString(R.string.warning_enter_characters_other_than_sapces)
        )
        return false
    }


    return true
}