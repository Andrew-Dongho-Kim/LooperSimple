package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopBase.Companion.MAX_LOOPS_TO_DO_SIMULTANEOUSLY
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.ui.home.input.UserInput
import com.pnd.android.loop.ui.home.input.UserInputState
import com.pnd.android.loop.ui.home.input.rememberUserInputState
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.toMs
import java.time.LocalDateTime

@Composable
fun Home(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
) {
    var mode by rememberSaveable { mutableIntStateOf(MODE_SECTIONS) }
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
                mode = mode,
                onModeChanged = { mode = it }
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
        HomeContent(
            modifier = Modifier.padding(contentPadding),
            mode = mode,
            blurState = blurState,
            inputState = inputState,
            snackBarHostState = snackBarHostState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(
                color = if (blurState.isOn) {
                    AppColor.onSurface.copy(
                        alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f
                    )
                } else {
                    Color.Transparent
                }
            )
    )
}

@Composable
private fun HomeContent(
    modifier: Modifier,
    mode: Int,
    blurState: BlurState,
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
) {
    Box(modifier = modifier.background(color = AppColor.background)) {
        val lazyListState = rememberLazyListState()
        Loops(
            modifier = Modifier
                .fillMaxHeight()
                .navigationBarsPadding()
                .imePadding(),
            mode = mode,
            blurState = blurState,
            inputState = inputState,
            lazyListState = lazyListState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
        )

        val context = LocalContext.current
        UserInput(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .imePadding(),
            blurState = blurState,
            inputState = inputState,
            snackBarHostState = snackBarHostState,
            lazyListState = lazyListState,
            onEnsureLoop = { loop ->
                ensureLoop(
                    context = context,
                    loopViewModel = loopViewModel,
                    loop = loop,
                    hostState = snackBarHostState
                )
            },
            onLoopSubmitted = { newLoop ->
                loopViewModel.addOrUpdateLoop(
                    newLoop.asLoopVo(
                        created = if (newLoop.created == 0L) {
                            LocalDateTime.now().toMs()
                        } else {
                            newLoop.created
                        }
                    )
                )
            }
        )
    }
}


private suspend fun ensureLoop(
    context: Context,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    hostState: SnackbarHostState
): Boolean {

    // Empty title check
    if (loop.title.trim().isEmpty()) {
        hostState.showSnackbar(
            message = context.getString(R.string.warning_enter_characters_other_than_spaces)
        )
        return false
    }

    if (loopViewModel.maxOfIntersects(loop) >= MAX_LOOPS_TO_DO_SIMULTANEOUSLY) {
        hostState.showSnackbar(
            message = context.getString(
                R.string.warning_up_to_max_loops,
                MAX_LOOPS_TO_DO_SIMULTANEOUSLY
            )
        )
        return false
    }

    return true
}
