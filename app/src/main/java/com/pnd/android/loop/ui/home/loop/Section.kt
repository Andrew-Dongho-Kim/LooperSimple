package com.pnd.android.loop.ui.home.loop

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.NativeAd
import com.pnd.android.loop.ui.home.loop.timeline.LoopTimeline
import com.pnd.android.loop.ui.theme.elevatedSurface

val HOME_NATIVE_AD_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/2247696110"
} else {
    "ca-app-pub-2341430172816266/9323327804"
}

fun LazyListScope.section(
    section: Section,
    loopViewModel: LoopViewModel
) {
    when (section) {
        is Section.Ad -> sectionAd(section)
        is Section.Later -> sectionLater(section, loopViewModel)
        is Section.Today -> sectionToday(section, loopViewModel)
        is Section.Summary -> sectionSummary(section, loopViewModel)
    }
}

@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.sectionToday(
    section: Section.Today,
    loopViewModel: LoopViewModel,
) {
    var isSelected by section.isSelected
    val loops by section.items

    if (isSelected) {
        item {
            LoopTimeline(
                loops = loops
            )
        }
    } else {
        items(
            items = loops,
            key = { loop -> loop.id },
            contentType = { ContentTypes.LOOP_CARD }
        ) { loop ->
            LoopCard(
                modifier = Modifier.animateItemPlacement(),
                loopViewModel = loopViewModel,
                loop = loop,
                showActiveDays = section.showActiveDays,
            )
        }
    }

    item(
        contentType = ContentTypes.TODAY_HEADER,
        key = section.headerKey
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

private fun LazyListScope.sectionAd(
    section: Section.Ad
) {
    item(
        key = section.headerKey,
        contentType = ContentTypes.AD_CARD
    ) {
        NativeAd(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 12.dp
            ),
            adId = HOME_NATIVE_AD_ID
        )
    }
}

private fun LazyListScope.sectionSummary(
    section: Section.Summary,
    loopViewModel: LoopViewModel,
) {
    item(
        key = section.headerKey,
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

private fun LazyListScope.sectionLater(
    section: Section.Later,
    loopViewModel: LoopViewModel,
) {
    var isExpanded by section.isExpanded
    item(
        key = section.headerKey,
        contentType = ContentTypes.LATER_HEADER
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
    LATER_HEADER,
    TODAY_HEADER,
    LOOP_CARD,
    LOOP_SUMMARY_CARD,
    AD_CARD,
}

sealed class Section(val headerKey: String) {
    val items = mutableStateOf<List<LoopBase>>(emptyList())

    open val size
        get() = items.value.size

    class Today(val showActiveDays: Boolean) : Section(
        headerKey = "TodaySection"
    ) {
        val isSelected = mutableStateOf(false)
    }

    class Ad : Section(
        headerKey = "AdSection"
    ) {
        override val size = 1
    }

    class Summary : Section(
        headerKey = "SummarySection"
    )

    class Later(
        val title: String,
        val showActiveDays: Boolean,
    ) : Section(
        headerKey = "LaterSection"
    ) {

        val isExpanded = mutableStateOf(false)
    }
}