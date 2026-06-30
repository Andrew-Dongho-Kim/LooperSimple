package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.annotation.IntDef
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Timeline
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.ExpandableNativeAd
import com.pnd.android.loop.ui.home.Section.AllAndTodayTab.Companion.TAB_ALL
import com.pnd.android.loop.ui.home.Section.AllAndTodayTab.Companion.TAB_TODAY
import com.pnd.android.loop.ui.home.timeline.LoopTimeline
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer

val HOME_NATIVE_AD_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/2247696110"
} else {
    "ca-app-pub-2341430172816266/9323327804"
}

fun LazyListScope.section(
    section: Section,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onEdit: (LoopBase) -> Unit,
    onSectionTabChanged: (Int) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    when (section) {
        is Section.HeaderCard -> sectionHeader(
            section = section,
            loopViewModel = loopViewModel,
        )

        is Section.Today -> sectionToday(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            onEdit = onEdit,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
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
            onEdit = onEdit,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )

        is Section.All -> sectionAll(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            onEdit = onEdit,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )

        is Section.AllAndTodayTab -> sectionAllAndTodayTab(
            section = section,
            onTabChanged = onSectionTabChanged
        )
    }
}

private fun LazyListScope.sectionHeader(
    section: Section.HeaderCard,
    loopViewModel: LoopViewModel,
) {
    item(
        contentType = ContentTypes.STATISTICS_CARD,
        key = section.key
    ) {
        // Group / statistics / history navigation now lives in the home app bar, so the
        // header item only carries the stats card itself.
        LoopHeaderCard(
            modifier = Modifier.padding(
                horizontal = Dimens.screenHorizontalPadding,
                vertical = Dimens.contentPadding,
            ),
            loopViewModel = loopViewModel,
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
    onEdit: (LoopBase) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
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
            onEdit = onEdit,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
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
        TodayFinishedState(modifier = modifier)
    }
}

/**
 * Celebration-style placeholder shown once every loop scheduled for today is done or
 * skipped. A soft tinted badge plus title/subtitle reads as a reward rather than an
 * "empty" screen, and relies only on theme colors so it adapts to light/dark mode.
 */
@Composable
private fun TodayFinishedState(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.screenHorizontalPadding,
                vertical = 56.dp,
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .background(
                    color = AppColor.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(36.dp),
                imageVector = Icons.Rounded.TaskAlt,
                tint = AppColor.primary,
                contentDescription = null
            )
        }

        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = stringResource(id = R.string.today_loops_finished),
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold,
            )
        )

        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = stringResource(id = R.string.today_loops_finished_hint),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            )
        )
    }
}

private fun LazyListScope.sectionTodayBody(
    section: Section.Today,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    onEdit: (LoopBase) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {

    val isTimeline by section.isSelected

    // View-mode toggle sits above the content so switching list <-> timeline is discoverable.
    item(
        contentType = ContentTypes.TIMELINE_TOGGLE_BUTTON,
        key = section.key
    ) {
        val context = LocalContext.current
        ViewModeToggle(
            modifier = Modifier.padding(bottom = Dimens.cardSpacing),
            isTimeline = isTimeline,
            onSelected = { selected ->
                section.save(context = context, selected = selected)
            }
        )
    }

    if (isTimeline) {
        item(
            contentType = ContentTypes.LOOP_TIMELINE,
            key = "LoopTimeline",
        ) {
            LoopTimeline(
                modifier = Modifier.padding(top = Dimens.cardSpacing),
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
                modifier = Modifier
                    .animateItem()
                    .padding(
                        horizontal = Dimens.screenHorizontalPadding,
                        vertical = Dimens.cardSpacing,
                    ),
                blurState = blurState,
                loopViewModel = loopViewModel,
                loop = loop,
                cardValues = LoopCardValues(
                    syncWithTime = true,
                    isHighlighted = highlightId == loop.loopId,
                ),
                onEdit = onEdit,
                onNavigateToGroupPicker = onNavigateToGroupPicker,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
        }
    }
}

/**
 * Compact segmented control that flips the Today section between a plain card
 * list and the hourly timeline view. Replaces the old italic text button so the
 * two view modes read as equal, tappable options.
 */
@Composable
private fun ViewModeToggle(
    modifier: Modifier = Modifier,
    isTimeline: Boolean,
    onSelected: (timeline: Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.screenHorizontalPadding),
        horizontalArrangement = Arrangement.End,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundShapes.medium)
                .background(color = AppColor.surfaceContainer)
                .padding(3.dp),
            horizontalArrangement = Arrangement.spacedBy(3.dp),
        ) {
            ViewModeButton(
                icon = Icons.Outlined.ViewAgenda,
                contentDescription = stringResource(R.string.list_view),
                selected = !isTimeline,
                onClick = { onSelected(false) },
            )
            ViewModeButton(
                icon = Icons.Outlined.Timeline,
                contentDescription = stringResource(R.string.timeline),
                selected = isTimeline,
                onClick = { onSelected(true) },
            )
        }
    }
}

@Composable
private fun ViewModeButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundShapes.small)
            .background(color = if (selected) AppColor.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = if (selected) AppColor.onPrimary else AppColor.onSurface.copy(alpha = 0.5f),
            contentDescription = contentDescription,
        )
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
                    horizontal = Dimens.screenHorizontalPadding,
                    vertical = Dimens.contentPadding,
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
                top = Dimens.sectionSpacing,
                bottom = Dimens.contentPadding,
            ),
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )
    }
}

private fun LazyListScope.sectionAllAndTodayTab(
    section: Section.AllAndTodayTab,
    onTabChanged: (tab: Int) -> Unit = {},
) {
    item {
        SegmentedTabs(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Dimens.screenHorizontalPadding)
                .padding(top = Dimens.cardSpacing, bottom = Dimens.contentPadding),
            selectedTab = section.selectedTab,
            onTabSelected = { tab ->
                if (tab != section.selectedTab) {
                    section.selectedTab = tab
                    onTabChanged(tab)
                }
            },
        )
    }
}

/** Standard two-segment control to switch between today's loops and all loops. */
@Composable
private fun SegmentedTabs(
    modifier: Modifier = Modifier,
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(color = AppColor.surfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        SegmentTab(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.tab_today),
            selected = selectedTab == TAB_TODAY,
            onClick = { onTabSelected(TAB_TODAY) },
        )
        SegmentTab(
            modifier = Modifier.weight(1f),
            text = stringResource(id = R.string.tab_all),
            selected = selectedTab == TAB_ALL,
            onClick = { onTabSelected(TAB_ALL) },
        )
    }
}

@Composable
private fun SegmentTab(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(RoundShapes.small)
            .background(color = if (selected) AppColor.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = if (selected) AppColor.onPrimary else AppColor.onSurface.copy(alpha = 0.6f),
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
            ),
        )
    }
}

private fun LazyListScope.sectionAll(
    section: Section.All,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onEdit: (LoopBase) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,

    ) {
    val loops by section.items
    items(
        items = loops,
        contentType = { ContentTypes.LOOP_CARD },
        key = { loop -> loop.loopId }
    ) { loop ->
        LoopCardWithOption(
            modifier = Modifier
                .padding(
                    horizontal = Dimens.screenHorizontalPadding,
                    vertical = Dimens.cardSpacing,
                ),
            blurState = blurState,
            loopViewModel = loopViewModel,
            loop = loop,
            onEdit = onEdit,
            cardValues = LoopCardValues(
                syncWithTime = false,
                isHighlighted = false,
            ),
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
    item {
        Spacer(modifier = Modifier.height(Dimens.sectionSpacing))
    }
}

private fun LazyListScope.sectionLater(
    section: Section.Later,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onEdit: (LoopBase) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
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
                modifier = Modifier
                    .padding(
                        horizontal = Dimens.screenHorizontalPadding,
                        vertical = Dimens.cardSpacing,
                    ),
                blurState = blurState,
                loopViewModel = loopViewModel,
                loop = loop,
                cardValues = LoopCardValues(
                    syncWithTime = false,
                    isHighlighted = false
                ),
                onEdit = onEdit,
                onNavigateToGroupPicker = onNavigateToGroupPicker,
                onNavigateToDetailPage = onNavigateToDetailPage,
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
            .padding(horizontal = Dimens.screenHorizontalPadding, vertical = Dimens.contentPadding),
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

    class HeaderCard : Section(key = "HeaderCard")

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

    class AllAndTodayTab(
        @Tab tab: Int = TAB_TODAY
    ) : Section(
        key = "AllAndTodayTabSection",
    ) {
        override val size = 1

        var selectedTab by mutableIntStateOf(tab)

        companion object {
            const val TAB_ALL = 0
            const val TAB_TODAY = 1

            @IntDef(TAB_ALL, TAB_TODAY)
            annotation class Tab

            val Saver = listSaver(
                save = {
                    listOf(it.selectedTab)
                },
                restore = { list ->
                    AllAndTodayTab(
                        tab = list[0]
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