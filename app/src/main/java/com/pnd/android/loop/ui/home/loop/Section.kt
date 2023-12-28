package com.pnd.android.loop.ui.home.loop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.home.LoopViewModel


fun LazyListScope.Section(
    section: Section,
    loopViewModel: LoopViewModel
) {
    when (section) {
        is Section.None -> SimpleSection(section, loopViewModel)
        is Section.Expandable -> ExpandableSection(section, loopViewModel)
    }
}

private fun LazyListScope.SimpleSection(
    section: Section.None,
    loopViewModel: LoopViewModel,
) {
    val loops by section.items
    items(
        items = loops,
        key = { loop -> loop.id },
        contentType = { ContentTypes.LOOP_CARD }
    ) { loop ->
        LoopCard(
            loopViewModel = loopViewModel,
            loop = loop,
            showActiveDays = section.showActiveDays,
        )
    }
}

private fun LazyListScope.ExpandableSection(
    section: Section.Expandable,
    loopViewModel: LoopViewModel,
) {
    var isExpanded by section.isExpanded
    item(
        key = section.title,
        contentType = ContentTypes.EXPANDABLE_HEADER
    ) {
        ExpandableHeader(
            modifier = Modifier.padding(top = 8.dp),
            headText = section.title,
            isExpanded = isExpanded,
            onExpandChanged = { expanded -> isExpanded = expanded }
        )
    }

    val duration = 500
    val loops by section.items
    items(
        items = loops,
        key = { loop -> loop.id },
        contentType = { ContentTypes.LOOP_CARD }
    ) { loop ->
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
            exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))
        ) {
            LoopCard(
                loopViewModel = loopViewModel,
                loop = loop,
                showActiveDays = section.showActiveDays
            )
        }
    }
}


@Composable
private fun ExpandableHeader(
    modifier: Modifier = Modifier,
    headText: String,
    isExpanded: Boolean,
    onExpandChanged: (isExpanded: Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpandChanged(!isExpanded) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = headText,
            style = MaterialTheme.typography.subtitle2.copy(
                color = MaterialTheme.colors.onSurface,
            )
        )

        Box(modifier = Modifier.weight(1f))

        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) -180f else 0f,
            animationSpec = tween(500),
            label = ""
        )

        Image(
            modifier = Modifier.graphicsLayer {
                rotationX = rotation
            },
            imageVector = Icons.Rounded.ExpandMore,
            colorFilter = ColorFilter.tint(color = MaterialTheme.colors.onSurface),
            contentDescription = ""
        )
    }
}

enum class ContentTypes {
    EXPANDABLE_HEADER, LOOP_CARD,
}

sealed class Section {
    val items = mutableStateOf<List<LoopVo>>(emptyList())

    class None(val showActiveDays: Boolean) : Section()

    class Expandable(
        val title: String,
        val showActiveDays: Boolean,
    ) : Section() {

        val isExpanded = mutableStateOf(false)
    }
}