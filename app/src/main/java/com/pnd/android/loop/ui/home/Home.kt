package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.input.rememberUserInputState
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.surface

@Composable
fun Home(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
) {
    val inputState = rememberUserInputState(context = LocalContext.current)
    val snackBarHostState = remember { SnackbarHostState() }
    val blurState = rememberBlurState()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .blur(radius = blurState.radius),
        topBar = {
            HomeAppBar(
                modifier = Modifier
                    .background(color = AppColor.surface)
                    .statusBarsPadding(),
                loopViewModel = loopViewModel,
            )
        },
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 48.dp + 56.dp)
                    .padding(
                        bottom = if (inputState.isSelectorOpened) {
                            dimensionResource(id = R.dimen.user_input_selector_content_height)
                        } else {
                            0.dp
                        }
                    ),
                hostState = snackBarHostState
            )
        },
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.ime),
    )
    { contentPadding ->
        Loops(
            modifier = Modifier.padding(contentPadding),
            mode = MODE_SECTIONS,
            blurState = blurState,
            inputState = inputState,
            snackBarHostState = snackBarHostState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
        )
    }
}
