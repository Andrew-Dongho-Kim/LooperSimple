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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.ui.home.loop.timeline.LoopTimeline
import com.pnd.android.loop.ui.theme.elevatedSurface


fun LazyListScope.Section(
    section: Section,
    loopViewModel: LoopViewModel
) {
    when (section) {
        is Section.Today -> TodaySection(section, loopViewModel)
        is Section.Summary -> SummarySection(section, loopViewModel)
        is Section.Later -> LaterSection(section, loopViewModel)
    }
}

private fun LazyListScope.TodaySection(
    section: Section.Today,
    loopViewModel: LoopViewModel,
) {
    var isSelected by section.isSelected

    if (isSelected) {
        item {
            LoopTimeline(
                loopViewModel = loopViewModel
            )
        }
    } else {
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

    item(
        contentType = ContentTypes.TOGGLE_HEADER
    ) {
        TimelineHeaderButton(
            isSelected = isSelected,
            onSelected = { selected -> isSelected = selected }
        )
    }
}

@Composable
private fun TimelineHeaderButton(
    modifier: Modifier = Modifier,
    isSelected: Boolean,
    onSelected: (Boolean) -> Unit
) {
    Box(modifier = modifier.fillMaxWidth()) {
        val backgroundColor = MaterialTheme.colors.elevatedSurface(3.dp)
        val borderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.2f)
        Row(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(top = 8.dp, bottom = 8.dp, end = 24.dp)
                .clickable { onSelected(!isSelected) }
                .drawBehind {
                    if (isSelected) {
                        drawRoundRect(
                            color = backgroundColor,
                            cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        )
                    }
                    drawRoundRect(
                        color = borderColor,
                        cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                        style = Stroke(width = 0.5.dp.toPx())
                    )
                }
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            val normalColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
            val selectedColor = MaterialTheme.colors.primary
            val contentColor = if (isSelected) selectedColor else normalColor
            Text(
                text = stringResource(R.string.timeline),
                style = MaterialTheme.typography.body1.copy(
                    color = contentColor,
                    fontWeight = FontWeight.Bold,
                    fontStyle = FontStyle.Italic
                )

            )
            Image(
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(12.dp),
                imageVector = Icons.Filled.Timeline,
                colorFilter = ColorFilter.tint(
                    color = contentColor
                ),
                contentDescription = ""
            )
        }
    }
}

private fun LazyListScope.SummarySection(
    section: Section.Summary,
    loopViewModel: LoopViewModel,
) {
    item(
        key = "Summary",
        contentType = ContentTypes.LOOP_SUMMARY_CARD
    ) {
        LoopSummaryCard(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 12.dp
            ),
            section = section,
            loopViewModel = loopViewModel,
        )
    }
}

private fun LazyListScope.LaterSection(
    section: Section.Later,
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
    EXPANDABLE_HEADER,
    TOGGLE_HEADER,
    LOOP_CARD,
    LOOP_SUMMARY_CARD
}

sealed class Section {
    val items = mutableStateOf<List<LoopWithDone>>(emptyList())

    val size
        get() = items.value.size

    class Today(val showActiveDays: Boolean) : Section() {
        val isSelected = mutableStateOf(false)
    }

    class Summary : Section()

    class Later(
        val title: String,
        val showActiveDays: Boolean,
    ) : Section() {

        val isExpanded = mutableStateOf(false)
    }
}