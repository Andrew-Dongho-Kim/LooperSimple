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
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.doneState
import com.pnd.android.loop.ui.home.loop.input.UserInputState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.isActiveDay

@Composable
fun Loops(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    lazyListState: LazyListState,
    loopViewModel: LoopViewModel,
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
                item { Spacer(modifier = Modifier.height(64.dp)) }

                sections.forEach { section ->
                    section(
                        section = section,
                        loopViewModel = loopViewModel,
                        onEdit = onEdit,
                    )
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
                style = MaterialTheme.typography.subtitle1.copy(
                    color = AppColor.onSurface
                )
            )
        }
    }
}

@Composable
private fun LoopViewModel.observeSectionsAsState(
    inputState: UserInputState,
): State<List<Section>> {
    val loops by loopsWithDone.collectAsState(emptyList())

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
            rememberTodaySection(resultLoops, inputState),
            rememberAdSection(),
            rememberDoneSection(resultLoops),
            rememberLaterSection(resultLoops)
        ).filter { it.size > 0 }

        remember(sections) { mutableStateOf(sections) }
    }
}

@Composable
private fun rememberAdSection() = remember {
    Section.Ad()
}

@Composable
private fun rememberDoneSection(loops: List<LoopBase>) = remember {
    Section.Summary()
}.apply {
    items.value =
        loops.filter { it.isActiveDay() && it.doneState != LoopDoneVo.DoneState.NO_RESPONSE }
}

@Composable
private fun rememberTodaySection(
    loops: List<LoopBase>,
    inputState: UserInputState,
): Section {
    val filtered = loops.filter {
        it.isActiveDay() && it.doneState == LoopDoneVo.DoneState.NO_RESPONSE
    }

    val resultLoops = mutableListOf(*filtered.toTypedArray())
    if (inputState.mode == UserInputState.Mode.New) {
        resultLoops.add(0, inputState.value)
    }

    return remember {
        Section.Today(showActiveDays = false)
    }.apply {
        items.value = resultLoops
    }
}

@Composable
private fun rememberLaterSection(loops: List<LoopBase>): Section {
    val title = stringResource(id = R.string.later)
    return remember {
        Section.Later(
            title = title,
            showActiveDays = true
        )
    }.apply {
        items.value = loops.filter { !it.isActiveDay() }
    }
}