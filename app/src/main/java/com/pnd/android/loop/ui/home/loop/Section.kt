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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
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
import com.pnd.android.loop.ui.common.ExpandableNativeAd
import com.pnd.android.loop.ui.home.loop.timeline.LoopTimeline
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.elevatedSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary

val HOME_NATIVE_AD_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/2247696110"
} else {
    "ca-app-pub-2341430172816266/9323327804"
}

fun LazyListScope.section(
    section: Section,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onEdit: (LoopBase) -> Unit,
) {
    when (section) {
        is Section.Statistics -> sectionStatistics(
            section = section,
            loopViewModel = loopViewModel,
        )

        is Section.Today -> sectionToday(
            section = section,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onEdit = onEdit,
        )

        is Section.Yesterday -> sectionYesterday(
            section = section,
            loopViewModel = loopViewModel,
        )

        is Section.Ad -> sectionAd(section = section)

        is Section.DoneSkip -> sectionDoneSkip(
            section = section,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage
        )

        is Section.Later -> sectionLater(
            section = section,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onEdit = onEdit,
        )
    }
}

private fun LazyListScope.sectionStatistics(
    section: Section.Statistics,
    loopViewModel: LoopViewModel,
) {
    item(
        contentType = ContentTypes.STATISTICS_CARD,
        key = section.key
    ) {
        LoopStatisticsCard(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 12.dp
            ),
            loopViewModel = loopViewModel
        )
    }
}

private fun LazyListScope.sectionYesterday(
    section: Section.Yesterday,
    loopViewModel: LoopViewModel,
) {
    val loops by section.items
    if (loops.isEmpty()) return

    var isExpanded by section.isExpanded
    item(
        contentType = ContentTypes.YESTERDAY_CARD,
        key = section.key
    ) {
        LoopYesterdayCard(
            loopViewModel = loopViewModel,
            loops = loops,
            isExpanded = isExpanded,
            onExpandChanged = { isExpanded = it }
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
private fun LazyListScope.sectionToday(
    section: Section.Today,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
) {
    var isSelected by section.isSelected
    val loops by section.items

    if (isSelected) {
        item(
            contentType = ContentTypes.LOOP_TIMELINE,
            key = "",
        ) {
            LoopTimeline(
                loopViewModel = loopViewModel,
                loops = loops,
                onNavigateToDetailPage = onNavigateToDetailPage,
                onEdit = onEdit,
            )
        }
    } else {
        items(
            items = loops,
            contentType = { ContentTypes.LOOP_CARD },
            key = { loop -> loop.id },
        ) { loop ->
            LoopCardWithOption(
                modifier = Modifier.animateItemPlacement(),
                loopViewModel = loopViewModel,
                loop = loop,
                onNavigateToDetailPage = onNavigateToDetailPage,
                onEdit = onEdit,
                showActiveDays = section.showActiveDays,
            )
        }
    }

    item(
        contentType = ContentTypes.TIMELINE_TOGGLE_BUTTON,
        key = section.key
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
        val borderColor = AppColor.onSurface.copy(alpha = 0.2f)
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
            val normalColor = AppColor.onSurface.copy(alpha = ContentAlpha.medium)
            val selectedColor = AppColor.primary
            val contentColor = if (isSelected) selectedColor else normalColor
            Text(
                text = stringResource(R.string.timeline),
                style = AppTypography.bodyMedium .copy(
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
        contentType = ContentTypes.AD_CARD,
        key = section.key,
    ) {
        ExpandableNativeAd(
            modifier = Modifier.padding(
                horizontal = 8.dp,
                vertical = 12.dp
            ),
            adId = HOME_NATIVE_AD_ID
        )
    }
}

private fun LazyListScope.sectionDoneSkip(
    section: Section.DoneSkip,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    item(
        contentType = ContentTypes.DONE_SKIP_CARD,
        key = section.key,
    ) {
        LoopDoneSkipCard(
            modifier = Modifier.padding(
                vertical = 12.dp
            ),
            section = section,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )
    }
}

private fun LazyListScope.sectionLater(
    section: Section.Later,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
) {
    var isExpanded by section.isExpanded
    item(
        contentType = ContentTypes.LATER_HEADER,
        key = section.key,
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
        contentType = { ContentTypes.LOOP_CARD },
        key = { loop -> loop.id },
    ) { loop ->
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
            exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))
        ) {
            LoopCardWithOption(
                loopViewModel = loopViewModel,
                loop = loop,
                onNavigateToDetailPage = onNavigateToDetailPage,
                onEdit = onEdit,
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
            style = AppTypography.titleSmall.copy(
                color = AppColor.onSurface,
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
            colorFilter = ColorFilter.tint(color = AppColor.onSurface),
            contentDescription = ""
        )
    }
}

enum class ContentTypes {
    STATISTICS_CARD,
    TIMELINE_TOGGLE_BUTTON,
    LOOP_TIMELINE,
    LATER_HEADER,
    YESTERDAY_CARD,
    LOOP_CARD,
    DONE_SKIP_CARD,
    AD_CARD,
}

sealed class Section(val key: String) {
    val items = mutableStateOf<List<LoopBase>>(emptyList())

    open val size
        get() = items.value.size

    class Statistics : Section(key = "StatisticsCard") {
        override val size = 1
    }

    class Yesterday(
        isSelected: Boolean = false
    ) : Section(
        key = "YesterdaySection",
    ) {
        val isExpanded = mutableStateOf(isSelected)

        companion object {
            val Saver = listSaver(
                save = {
                    listOf(
                        it.isExpanded.value
                    )
                },
                restore = { list ->
                    Yesterday(
                        isSelected = list[0]
                    )
                }
            )
        }
    }

    class Today(
        val showActiveDays: Boolean,
        isSelected: Boolean = false
    ) : Section(
        key = "TodaySection"
    ) {
        val isSelected = mutableStateOf(isSelected)

        companion object {
            val Saver = listSaver(
                save = {
                    listOf(
                        it.showActiveDays,
                        it.isSelected.value
                    )
                },
                restore = { list ->
                    Today(
                        showActiveDays = list[0],
                        isSelected = list[1]
                    )
                }
            )
        }
    }

    class Ad : Section(
        key = "AdSection"
    ) {
        override val size = 1
    }

    class DoneSkip : Section(
        key = "DoneSkipSection"
    )

    class Later(
        val title: String,
        val showActiveDays: Boolean,
        isExpanded: Boolean = false
    ) : Section(
        key = "LaterSection"
    ) {

        val isExpanded = mutableStateOf(isExpanded)

        companion object {
            val Saver = listSaver(
                save = {
                    listOf(
                        it.title,
                        it.showActiveDays,
                        it.isExpanded.value,
                    )
                },
                restore = { list ->
                    Later(
                        title = list[0] as String,
                        showActiveDays = list[1] as Boolean,
                        isExpanded = list[2] as Boolean,
                    )
                }
            )
        }
    }
}