package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.common.MAX_LOOPS_TOGETHER
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.ui.home.input.UserInput
import com.pnd.android.loop.ui.home.input.UserInputState
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toMs
import java.time.LocalDateTime


@Composable
fun Loops(
    modifier: Modifier,
    mode: Int,
    blurState: BlurState,
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
    loopViewModel: LoopViewModel,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToGroupPage: () -> Unit,
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
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToGroupPage = onNavigateToGroupPage,
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
}

@Composable
fun Loops(
    modifier: Modifier = Modifier,
    mode: Int,
    blurState: BlurState,
    inputState: UserInputState,
    lazyListState: LazyListState,
    loopViewModel: LoopViewModel,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
) {
    val sections by loopViewModel.observeSectionsAsState(mode, inputState)
    val onEdit = remember { { loop: LoopBase -> inputState.edit(loop) } }

    Box(modifier = modifier.background(AppColor.background)) {
        if (sections.isEmpty()) {
            EmptyLoops(
                modifier = Modifier.fillMaxSize()
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState,
            ) {
                sections.forEach { section ->
                    section(
                        section = section,
                        blurState = blurState,
                        loopViewModel = loopViewModel,
                        onEdit = onEdit,
                        onNavigateToGroupPicker = onNavigateToGroupPicker,
                        onNavigateToGroupPage = onNavigateToGroupPage,
                        onNavigateToDetailPage = onNavigateToDetailPage,
                        onNavigateToHistoryPage = onNavigateToHistoryPage,
                        onNavigateToStatisticsPage = onNavigateToStatisticsPage,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(150.dp))
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
        Text(
            modifier = Modifier.alpha(0.7f),
            text = stringResource(R.string.desc_no_loops),
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface
            )
        )
    }
}

@Composable
private fun LoopViewModel.observeSectionsAsState(
    mode: Int,
    inputState: UserInputState,
): State<List<Section>> {
    val loops by allLoopsWithDoneStates.collectAsState(emptyList())
    val yesterdayLoops by loopsNoResponseYesterday.collectAsState(initial = emptyList())

    return if (loops.isEmpty() && inputState.mode == UserInputState.Mode.None) {
        remember { mutableStateOf(emptyList()) }
    } else {
        val resultLoops = ArrayList<LoopBase>(loops)
        if (inputState.mode == UserInputState.Mode.Edit) {
            val edited = inputState.value
            val index = resultLoops.indexOfFirst { it.loopId == edited.loopId }
            resultLoops[index] = inputState.value
        }

        val sections = if (mode == MODE_ALL_LOOPS) {
            mutableListOf(
                rememberAllSection(
                    loops = resultLoops,
                    inputState = inputState
                ),
                rememberAdSection(),
            )
        } else {
            mutableListOf(
                rememberStatisticsSection(resultLoops),
                rememberTodaySection(resultLoops, inputState),
                rememberYesterdaySection(yesterdayLoops),
                rememberAdSection(),
                rememberDoneSection(resultLoops),
            ).filter { it.size > 0 }
        }


        remember(sections) { mutableStateOf(sections) }
    }
}

@Composable
private fun rememberStatisticsSection(
    loops: List<LoopBase>,
): Section = remember {
    Section.Statistics()
}.apply {
    items.value = loops
}

@Composable
private fun rememberYesterdaySection(
    loops: List<LoopBase>,
): Section {
    return rememberSaveable(saver = Section.Yesterday.Saver) {
        Section.Yesterday()
    }.apply {
        items.value = loops
    }
}

@Composable
private fun rememberTodaySection(
    loops: List<LoopBase>,
    inputState: UserInputState,
): Section {
    val filtered = loops.filter {
        (it.isActiveDay() && it.isNotRespond) || it.isDisabled
    }

    val resultLoops = mutableListOf(*filtered.toTypedArray())
    if (inputState.mode == UserInputState.Mode.New) {
        resultLoops.add(0, inputState.value)
    }

    val context = LocalContext.current
    return rememberSaveable(saver = Section.Today.Saver) {
        Section.Today().apply { load(context) }
    }.apply {
        items.value = resultLoops
    }
}


@Composable
private fun rememberAllSection(
    loops: List<LoopBase>,
    inputState: UserInputState,
): Section {

    val resultLoops = mutableListOf(*loops.toTypedArray())
    if (inputState.mode == UserInputState.Mode.New) {
        resultLoops.add(0, inputState.value)
    }

    return remember {
        Section.All()
    }.apply {
        items.value = resultLoops
    }
}

@Composable
private fun rememberAdSection() = remember { Section.Ad() }

@Composable
private fun rememberDoneSection(loops: List<LoopBase>) = remember {
    Section.DoneSkip()
}.apply {
    items.value =
        loops.filter { it.isActiveDay() && it.isRespond }
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

    if (loopViewModel.maxOfIntersects(loop) >= MAX_LOOPS_TOGETHER) {
        hostState.showSnackbar(
            message = context.getString(
                R.string.warning_up_to_max_loops,
                MAX_LOOPS_TOGETHER
            )
        )
        return false
    }

    return true
}
