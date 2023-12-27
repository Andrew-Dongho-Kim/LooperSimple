package com.pnd.android.loop.ui.home.loop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopVo
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
        val todaySection = remember { Section.None() }.apply {
            items.value = loops.filter { it.isActiveDay() }
        }

        val title = stringResource(id = R.string.later)
        val laterSection = remember { Section.Expandable(title) }.apply {
            items.value = loops.filter { !it.isActiveDay() }
        }
        remember { mutableStateOf(listOf(todaySection, laterSection)) }
    }
}


fun LazyListScope.Section(
    section: Section,
    loopViewModel: LoopViewModel,
) {
    val expanded by derivedStateOf {
        if (section is Section.Expandable) section.isExpanded.value else true
    }

    if (section is Section.Expandable) {
        item(
            key = section.title,
            contentType = ContentTypes.EXPANDABLE_HEADER
        ) {
            LoopCardHeader(
                modifier = Modifier.padding(top = 8.dp),
                headText = section.title,
                isExpanded = expanded,
                onExpandChanged = { expanded -> section.isExpanded.value = expanded }
            )
        }
    }

    val duration = 500
    val loops by section.items
    items(
        items = loops,
        key = { loop -> loop.id },
        contentType = { ContentTypes.LOOP_CARD }
    ) { loop ->
        AnimatedVisibility(
            visible = expanded,
            enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
            exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))
        ) {
            LoopCard(loopViewModel = loopViewModel, loop = loop)
        }
    }
}

enum class ContentTypes {
    EXPANDABLE_HEADER, LOOP_CARD,
}

sealed class Section {
    val items = mutableStateOf<List<LoopVo>>(emptyList())

    class None : Section()
    class Expandable(
        val title: String,
    ) : Section() {

        val isExpanded = mutableStateOf(false)
    }
}