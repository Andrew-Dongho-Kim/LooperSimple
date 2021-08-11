package com.pnd.android.loop.ui.home

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pnd.android.loop.alarm.AlarmHelper
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.home.loop.Loops
import com.pnd.android.loop.ui.input.UserInput
import dev.chrisbanes.accompanist.insets.navigationBarsWithImePadding
import dev.chrisbanes.accompanist.insets.statusBarsPadding

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun Home(
    alarmHelper: AlarmHelper,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    val viewModel: HomeViewModel = viewModel()
    val editedLoop = remember { mutableStateOf<LoopVo?>(null) }

    Surface(modifier = modifier) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(Modifier.fillMaxSize()) {
                Loops(
                    alarmHelper = alarmHelper,
                    scrollState = scrollState,
                    editedLoop = editedLoop,
                    modifier = Modifier
                        .weight(1f)
                        .statusBarsPadding()
                )
                UserInput(
                    scrollStateLoops = scrollState,
                    onLoopSubmitted = { loop ->
                        viewModel.addLoop(loop) {
                            if (it.enabled) alarmHelper.reserveRepeat(it)
                        }
                    },
                    loopInEdit = editedLoop,
                    // Use navigationBarsWithImePadding(), to move the input panel above both the
                    // navigation bar, and on-screen keyboard (IME)
                    modifier = Modifier.navigationBarsWithImePadding()
                )
            }
            HomeAppBar(
                modifier = Modifier.statusBarsPadding()
            )
        }
    }
}
