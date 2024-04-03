package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.loop.input.UserInputState
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.isActiveDay

@Composable
fun Loops(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    inputState: UserInputState,
    lazyListState: LazyListState,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
) {
    val sections by loopViewModel.observeSectionsAsState(inputState)
    val onEdit = remember { { loop: LoopBase -> inputState.edit(loop) } }

    Box(modifier = modifier.background(AppColor.background)) {
        if (sections.isEmpty()) {
            EmptyLoops(modifier = Modifier.fillMaxSize())
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
                        onNavigateToDetailPage = onNavigateToDetailPage,
                        onNavigateToHistoryPage = onNavigateToHistoryPage,
                        onNavigateToStatisticsPage = onNavigateToStatisticsPage,
                        onEdit = onEdit,
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
    inputState: UserInputState,
): State<List<Section>> {
    val loops by loopsWithDoneAll.collectAsState(emptyList())
    val yesterdayLoops by loopsNoResponseYesterday.collectAsState(initial = emptyList())

    return if (loops.isEmpty()) {
        remember { mutableStateOf(emptyList()) }
    } else {
        val resultLoops = ArrayList<LoopBase>(loops)
        if (inputState.mode == UserInputState.Mode.Edit) {
            val edited = inputState.value
            val index = resultLoops.indexOfFirst { it.id == edited.id }
            resultLoops[index] = inputState.value
        }

        val sections = mutableListOf(
            rememberStatisticsSection(),
            rememberTodaySection(resultLoops, inputState),
            rememberYesterdaySection(yesterdayLoops),
            rememberAdSection(),
            rememberDoneSection(resultLoops),
        ).filter { it.size > 0 }

        remember(sections) { mutableStateOf(sections) }
    }
}

@Composable
private fun rememberStatisticsSection(): Section = remember { Section.Statistics() }

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

    return rememberSaveable(saver = Section.Today.Saver) {
        Section.Today(showActiveDays = false)
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