package com.pnd.android.loop.ui.home

import android.content.Context
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
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.edit
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.ExpandableNativeAd
import com.pnd.android.loop.ui.home.timeline.LoopTimeline
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
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
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
    onEdit: (LoopBase) -> Unit,
) {
    when (section) {
        is Section.Statistics -> sectionStatistics(
            section = section,
            loopViewModel = loopViewModel,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            onNavigateToGroupPage = onNavigateToGroupPage,
        )

        is Section.Today -> sectionToday(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onEdit = onEdit,
        )

        is Section.Yesterday -> sectionYesterday(
            section = section,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage
        )

        is Section.Ad -> sectionAd(section = section)

        is Section.DoneSkip -> sectionDoneSkip(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage
        )

        is Section.Later -> sectionLater(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onEdit = onEdit,
        )

        is Section.All -> sectionAll(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onEdit = onEdit,
        )

    }
}

private fun LazyListScope.sectionStatistics(
    section: Section.Statistics,
    loopViewModel: LoopViewModel,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    item(
        contentType = ContentTypes.STATISTICS_CARD,
        key = section.key
    ) {
        LoopStatisticsCard(
            modifier = Modifier
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                )
                .padding(
                    bottom = 12.dp
                ),
            loopViewModel = loopViewModel,
            onNavigateToGroupPage = onNavigateToGroupPage,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )
    }
}


private fun LazyListScope.sectionYesterday(
    section: Section.Yesterday,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
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
            onExpandChanged = { isExpanded = it },
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

private fun LazyListScope.sectionToday(
    section: Section.Today,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
) {
    val loops by section.items

    if (loops.isEmpty()) {
        sectionTodayEmpty()
    } else {
        sectionTodayBody(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            loops = loops,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onEdit = onEdit
        )
    }

}

private fun LazyListScope.sectionTodayEmpty(
    modifier: Modifier = Modifier
) {
    item(
        contentType = ContentTypes.LOOP_EMPTY,
        key = "LoopEmpty"
    ) {
        Box(
            modifier = modifier
                .padding(vertical = 48.dp)
                .fillMaxWidth()
                .padding(
                    horizontal = 4.dp,
                    vertical = 12.dp,
                )
                .padding(
                    bottom = 12.dp
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = stringResource(id = R.string.today_loops_finished),
                style = AppTypography.titleMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.8f),
                    fontSize = 16.sp
                )
            )
        }
    }
}

private fun LazyListScope.sectionTodayBody(
    section: Section.Today,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
) {

    val isSelected by section.isSelected
    if (isSelected) {
        item(
            contentType = ContentTypes.LOOP_TIMELINE,
            key = "LoopTimeline",
        ) {
            LoopTimeline(
                modifier = Modifier.padding(top = 24.dp),
                blurState = blurState,
                loopViewModel = loopViewModel,
                loops = loops.filter { loop -> loop.enabled },
                onNavigateToDetailPage = onNavigateToDetailPage,
                onEdit = onEdit,
            )
        }
    } else {
        items(
            items = loops,
            contentType = { ContentTypes.LOOP_CARD },
            key = { loop -> loop.loopId },
        ) { loop ->
            val highlightId by loopViewModel.highlightId.collectAsState()

            LoopCardWithOption(
                modifier = Modifier.animateItem(),
                blurState = blurState,
                loopViewModel = loopViewModel,
                loop = loop,
                onNavigateToDetailPage = onNavigateToDetailPage,
                onEdit = onEdit,
                isSyncTime = true,
                isHighlighted = highlightId == loop.loopId
            )
        }
    }

    item(
        contentType = ContentTypes.TIMELINE_TOGGLE_BUTTON,
        key = section.key
    ) {
        val context = LocalContext.current
        TimelineHeaderButton(
            modifier = Modifier.padding(vertical = 12.dp),
            isSelected = isSelected,
            onSelected = { selected ->
                section.save(
                    context = context,
                    selected = selected
                )
            }
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
            val normalColor = AppColor.onSurface.copy(alpha = 0.7f)
            val selectedColor = AppColor.primary
            val contentColor = if (isSelected) selectedColor else normalColor
            Text(
                text = stringResource(R.string.timeline),
                style = AppTypography.bodyMedium.copy(
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
            modifier = Modifier
                .padding(
                    horizontal = 8.dp,
                    vertical = 12.dp
                )
                .padding(
                    top = 12.dp
                ),
            adId = HOME_NATIVE_AD_ID
        )
    }
}

private fun LazyListScope.sectionDoneSkip(
    section: Section.DoneSkip,
    blurState: BlurState,
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
                top = 48.dp,
                bottom = 12.dp
            ),
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )
    }
}

private fun LazyListScope.sectionAll(
    section: Section.All,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
) {
    val loops by section.items
    items(
        items = loops,
        contentType = { ContentTypes.LOOP_CARD },
        key = { loop -> loop.loopId }
    ) { loop ->
        LoopCardWithOption(
            blurState = blurState,
            loopViewModel = loopViewModel,
            loop = loop,
            onEdit = onEdit,
            isSyncTime = false,
            isHighlighted = false,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

private fun LazyListScope.sectionLater(
    section: Section.Later,
    blurState: BlurState,
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
        key = { loop -> loop.loopId },
    ) { loop ->
        AnimatedVisibility(
            visible = isExpanded,
            enter = fadeIn(tween(duration)) + expandVertically(tween(duration)),
            exit = fadeOut(tween(duration)) + shrinkVertically(tween(duration))
        ) {
            LoopCardWithOption(
                blurState = blurState,
                loopViewModel = loopViewModel,
                loop = loop,
                onNavigateToDetailPage = onNavigateToDetailPage,
                onEdit = onEdit,
                isSyncTime = false,
                isHighlighted = false
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
    LOOP_EMPTY,
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

    class Statistics : Section(key = "StatisticsCard")

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
        isSelected: Boolean = false
    ) : Section(
        key = "TodaySection"
    ) {
        private val _isSelected = mutableStateOf(isSelected)
        val isSelected: State<Boolean> = _isSelected

        fun load(context: Context) {
            _isSelected.value = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_SELECTED, false)
        }

        fun save(context: Context, selected: Boolean) {
            _isSelected.value = selected
            context
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit {
                    putBoolean(KEY_IS_SELECTED, selected)
                }
        }

        // Always visible
        override val size = 1

        companion object {
            private const val PREF_NAME = "loops_timeline"
            private const val KEY_IS_SELECTED = "key_is_selected"
            val Saver = listSaver(
                save = {
                    listOf(
                        it.isSelected.value
                    )
                },
                restore = { list ->
                    Today(
                        isSelected = list[0]
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
    ) {
        override val size = 1
    }

    class Later(
        val title: String,
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
                        it.isExpanded.value,
                    )
                },
                restore = { list ->
                    Later(
                        title = list[0] as String,
                        isExpanded = list[1] as Boolean,
                    )
                }
            )
        }
    }

    class All : Section(key = "AllSection")
}