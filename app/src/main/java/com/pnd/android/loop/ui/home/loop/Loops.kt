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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.LoopViewModel
import com.pnd.android.loop.util.isActiveDay

@Composable
fun Loops(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    loopViewModel: LoopViewModel,
) {
    val sections by loopViewModel.observeSectionsAsState()

    Box(modifier = modifier.background(MaterialTheme.colors.onSurface.copy(alpha = 0.02f))) {
        if (sections.isEmpty()) {
            EmptyLoops(modifier = Modifier.fillMaxSize())
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState,
            ) {
                item { Spacer(modifier = Modifier.height(64.dp)) }

                sections.forEach { section ->
                    Section(
                        section = section,
                        loopViewModel = loopViewModel
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
                    color = MaterialTheme.colors.onSurface
                )
            )
        }
    }
}

@Composable
private fun LoopViewModel.observeSectionsAsState(): State<List<Section>> {
    val loops by loops.observeAsState(emptyList())

    return if (loops.isEmpty()) {
        remember { mutableStateOf(emptyList()) }
    } else {
        val todaySection = remember {
            Section.None(showActiveDays = false)
        }.apply {
            items.value = loops.filter { it.isActiveDay() }
        }

        val title = stringResource(id = R.string.later)
        val laterSection = remember {
            Section.Expandable(
                title = title,
                showActiveDays = true
            )
        }.apply {
            items.value = loops.filter { !it.isActiveDay() }
        }
        remember { mutableStateOf(listOf(todaySection, laterSection)) }
    }
}