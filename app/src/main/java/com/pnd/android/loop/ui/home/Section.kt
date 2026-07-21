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
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
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
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.isInProgress
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.ui.common.ExpandableNativeAd
import com.pnd.android.loop.ui.home.timeline.LoopCircularDial
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

/** 홈 리스트에서 카드 사이 간격(카드당 상하 패딩). 다른 화면과 별개로 홈만 살짝 촘촘하게 둔다. */
private val HomeCardSpacing = 6.dp

val HOME_NATIVE_AD_ID = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/2247696110"
} else {
    "ca-app-pub-2341430172816266/9323327804"
}

fun LazyListScope.section(
    section: Section,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
    // 하단 패널에서 편집 중인 루프 id. 목록에서 해당 카드를 스포트라이트하고 나머지를 흐리게 하는 데 쓴다.
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onStateChanged: (LoopBase, Int) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    when (section) {
        is Section.HeaderCard -> sectionHeader(
            section = section,
            loopViewModel = loopViewModel,
            selectedTab = selectedTab,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )

        is Section.Today -> sectionToday(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            editingLoopId = editingLoopId,
            onEdit = onEdit,
            onDelete = onDelete,
            onStateChanged = onStateChanged,
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
            onDelete = onDelete,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )

        is Section.All -> sectionAll(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            editingLoopId = editingLoopId,
            onEdit = onEdit,
            onDelete = onDelete,
            onStateChanged = onStateChanged,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )

        is Section.AllHistoryGrid -> sectionAllHistoryGrid(
            section = section,
            loopViewModel = loopViewModel,
        )
    }
}

private fun LazyListScope.sectionHeader(
    section: Section.HeaderCard,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
    onNavigateToDetailPage: (LoopBase) -> Unit,
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
            selectedTab = selectedTab,
            onNavigateToDetailPage = onNavigateToDetailPage,
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
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onStateChanged: (LoopBase, Int) -> Unit,
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
            editingLoopId = editingLoopId,
            onEdit = onEdit,
            onDelete = onDelete,
            onStateChanged = onStateChanged,
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
 * 오늘 예정된 루프를 전부 완료/스킵했을 때의 축하 화면. 공용 [HomeEmptyState]를 그대로
 * 써서 "루프 없음" 상태와 같은 문법으로 읽히되, 문구와 아이콘으로 보상의 느낌을 준다.
 */
@Composable
private fun TodayFinishedState(
    modifier: Modifier = Modifier,
) {
    HomeEmptyState(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.screenHorizontalPadding,
                vertical = 56.dp,
            ),
        icon = Icons.Rounded.TaskAlt,
        title = stringResource(id = R.string.today_loops_finished),
        hint = stringResource(id = R.string.today_loops_finished_hint),
    )
}

private fun LazyListScope.sectionTodayBody(
    section: Section.Today,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onStateChanged: (LoopBase, Int) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {

    val viewMode by section.viewMode

    // View-mode toggle sits above the content so switching list / timeline / dial is discoverable.
    item(
        contentType = ContentTypes.TIMELINE_TOGGLE_BUTTON,
        key = section.key
    ) {
        val context = LocalContext.current
        ViewModeToggle(
            modifier = Modifier.padding(bottom = HomeCardSpacing),
            viewMode = viewMode,
            onSelected = { selected ->
                section.save(context = context, mode = selected)
            }
        )
    }
    when (viewMode) {
        TodayViewMode.TIMELINE -> item(
            contentType = ContentTypes.LOOP_TIMELINE,
            key = "LoopTimeline",
        ) {
            LoopTimeline(
                modifier = Modifier.padding(top = HomeCardSpacing),
                blurState = blurState,
                loops = loops.filter { loop -> loop.enabled },
                onNavigateToDetailPage = onNavigateToDetailPage,
                onEdit = onEdit,
                onDelete = onDelete,
            )
        }

        TodayViewMode.DIAL -> item(
            contentType = ContentTypes.LOOP_DIAL,
            key = "LoopDial",
        ) {
            LoopCircularDial(
                modifier = Modifier.padding(
                    horizontal = Dimens.screenHorizontalPadding,
                    vertical = HomeCardSpacing,
                ),
                blurState = blurState,
                loops = loops,
                onStateChanged = onStateChanged,
                onEdit = onEdit,
                onDelete = onDelete,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
        }

        // 미응답(NO_RESPONSE)뿐 아니라 진행 중(IN_PROGRESS)인 루프도 오늘 목록에 남긴다.
        // anytime 루프를 "시작"하면 상태가 IN_PROGRESS 로 바뀌는데, 이때 목록에서 사라지지
        // 않고 정지 버튼과 함께 그대로 보여야 한다. 완료/스킵(isRespond)만 Done/Skip 으로 이동한다.
        TodayViewMode.LIST -> items(
            items = loops.filter { loop -> loop.isNotRespond || loop.isInProgress },
            contentType = { ContentTypes.LOOP_CARD },
            key = { loop -> loop.loopId },
        ) { loop ->
            val highlightId by loopViewModel.highlightId.collectAsState()
            val isEditing = editingLoopId != null && loop.loopId == editingLoopId

            LoopCardWithOption(
                modifier = Modifier
                    .animateItem()
                    .padding(
                        horizontal = Dimens.screenHorizontalPadding,
                        vertical = HomeCardSpacing,
                    ),
                blurState = blurState,
                loopViewModel = loopViewModel,
                loop = loop,
                cardValues = LoopCardValues(
                    syncWithTime = true,
                    isHighlighted = highlightId == loop.loopId,
                    isEditing = isEditing,
                    isEditDimmed = editingLoopId != null && !isEditing,
                ),
                onEdit = onEdit,
                onDelete = onDelete,
                onStateChanged = onStateChanged,
                onNavigateToGroupPicker = onNavigateToGroupPicker,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
        }
    }
}

/**
 * Compact segmented control that flips the Today section between three view modes:
 * a plain card list, the hourly timeline, and the 24-hour circular dial. Each mode
 * reads as an equal, tappable icon option.
 */
@Composable
private fun ViewModeToggle(
    modifier: Modifier = Modifier,
    viewMode: TodayViewMode,
    onSelected: (mode: TodayViewMode) -> Unit,
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
                selected = viewMode == TodayViewMode.LIST,
                onClick = { onSelected(TodayViewMode.LIST) },
            )
            ViewModeButton(
                icon = Icons.Outlined.Timeline,
                contentDescription = stringResource(R.string.timeline),
                selected = viewMode == TodayViewMode.TIMELINE,
                onClick = { onSelected(TodayViewMode.TIMELINE) },
            )
            ViewModeButton(
                icon = Icons.Outlined.Schedule,
                contentDescription = stringResource(R.string.dial_view),
                selected = viewMode == TodayViewMode.DIAL,
                onClick = { onSelected(TodayViewMode.DIAL) },
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

private fun LazyListScope.sectionAll(
    section: Section.All,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onStateChanged: (LoopBase, Int) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,

    ) {
    val loops by section.items
    // 활성/비활성 루프를 분리한다. 상위 정렬(TodayLoopOrder)이 이미 활성→비활성 순이라
    // 여기서 필터만 나눠도 각 그룹 내부 순서는 그대로 유지된다.
    val enabledLoops = loops.filter { loop -> loop.enabled }
    val disabledLoops = loops.filter { loop -> !loop.enabled }

    // 활성 루프는 기존과 동일하게 카드 목록으로 노출한다.
    items(
        items = enabledLoops,
        contentType = { ContentTypes.LOOP_CARD },
        key = { loop -> loop.loopId }
    ) { loop ->
        val isEditing = editingLoopId != null && loop.loopId == editingLoopId
        LoopCardWithOption(
            modifier = Modifier
                .padding(
                    horizontal = Dimens.screenHorizontalPadding,
                    vertical = Dimens.cardSpacing,
                ),
            cardValues = LoopCardValues(
                syncWithTime = false,
                isHighlighted = false,
                isEditing = isEditing,
                isEditDimmed = editingLoopId != null && !isEditing,
                // 전체 탭은 루프 관리가 목적이라 완료/건너뜀 기록 메뉴는 숨긴다.
                showRecordActions = false,
            ),
            blurState = blurState,
            loopViewModel = loopViewModel,
            loop = loop,
            onEdit = onEdit,
            onDelete = onDelete,
            onStateChanged = onStateChanged,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }

    // 비활성 루프가 하나라도 있을 때만, 이들을 하나의 카드로 묶어 접었다 펼치는 아이템을 덧붙인다.
    if (disabledLoops.isNotEmpty()) {
        item(
            contentType = ContentTypes.ALL_DISABLED_GROUP,
            key = "AllDisabledGroup",
        ) {
            var isExpanded by section.isExpanded
            // "언제부터 비활성인지"는 done 이력에서 유도하므로 함께 전달한다.
            val doneHistory by loopViewModel.allDoneHistory.collectAsState(initial = emptyMap())
            DisabledLoopsCard(
                modifier = Modifier.padding(
                    horizontal = Dimens.screenHorizontalPadding,
                    vertical = Dimens.cardSpacing,
                ),
                loops = disabledLoops,
                doneHistory = doneHistory,
                isExpanded = isExpanded,
                onExpandChanged = { expanded -> isExpanded = expanded },
                // 활성화 버튼: 루프의 enabled 플래그만 켜서 저장한다.
                onEnable = { loop ->
                    loopViewModel.addOrUpdateLoop(loop.copyAs(enabled = true).asLoopVo())
                },
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
        }
    }

    item {
        Spacer(modifier = Modifier.height(Dimens.sectionSpacing))
    }
}

/**
 * 전체 탭 하단의 기록 그리드 섹션. 전체 루프의 done/skip 이력을
 * (행=루프, 열=생성일~오늘) 매트릭스로 보여준다.
 */
private fun LazyListScope.sectionAllHistoryGrid(
    section: Section.AllHistoryGrid,
    loopViewModel: LoopViewModel,
) {
    item(
        contentType = ContentTypes.ALL_HISTORY_GRID,
        key = section.key,
    ) {
        val loops by section.items
        val doneHistory by loopViewModel.allDoneHistory.collectAsState(initial = emptyMap())
        AllDoneHistoryGrid(
            modifier = Modifier.padding(
                horizontal = Dimens.screenHorizontalPadding,
                vertical = Dimens.contentPadding,
            ),
            loops = loops,
            doneHistory = doneHistory,
        )
    }
}

private fun LazyListScope.sectionLater(
    section: Section.Later,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
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
    val onStateChanged: (LoopBase, Int) -> Unit = { loop, doneState ->
        loopViewModel.changeLoopState(loop = loop, doneState = doneState)
    }
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
                        vertical = HomeCardSpacing,
                    ),
                blurState = blurState,
                loopViewModel = loopViewModel,
                loop = loop,
                cardValues = LoopCardValues(
                    syncWithTime = false,
                    isHighlighted = false
                ),
                onEdit = onEdit,
                onDelete = onDelete,
                onStateChanged = onStateChanged,
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
    LOOP_DIAL,
    LATER_HEADER,
    ALL_DISABLED_GROUP,
    YESTERDAY_CARD,
    LOOP_CARD,
    DONE_SKIP_CARD,
    AD_CARD,
    ALL_HISTORY_GRID,
}

/**
 * 오늘 섹션의 표시 방식.
 * - [LIST]: 기본 카드 목록
 * - [TIMELINE]: 가로 스크롤 시간대 타임라인
 * - [DIAL]: 24시간 원형 다이얼(시안 A)
 *
 * SharedPreferences 에는 [ordinal] 로 저장하므로, 뒤에 새 모드를 추가하는 것은 안전하지만
 * 순서를 바꾸면 저장된 값의 의미가 달라진다. 항목 순서는 유지할 것.
 */
enum class TodayViewMode {
    LIST,
    TIMELINE,
    DIAL;

    companion object {
        /** 저장된 ordinal 이 범위를 벗어나면 안전하게 [LIST] 로 되돌린다. */
        fun fromOrdinal(ordinal: Int): TodayViewMode =
            entries.getOrElse(ordinal) { LIST }
    }
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
        viewMode: TodayViewMode = TodayViewMode.LIST
    ) : Section(
        key = "TodaySection"
    ) {
        private val _viewMode = mutableStateOf(viewMode)
        val viewMode: State<TodayViewMode> = _viewMode

        fun load(context: Context) {
            val ordinal = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .getInt(KEY_VIEW_MODE, TodayViewMode.LIST.ordinal)
            _viewMode.value = TodayViewMode.fromOrdinal(ordinal)
        }

        fun save(context: Context, mode: TodayViewMode) {
            _viewMode.value = mode
            context
                .getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
                .edit {
                    putInt(KEY_VIEW_MODE, mode.ordinal)
                }
        }

        // Always visible
        override val size = 1

        companion object {
            private const val PREF_NAME = "loops_timeline"
            private const val KEY_VIEW_MODE = "key_view_mode"
            val Saver = listSaver(
                save = {
                    listOf(
                        it.viewMode.value.ordinal
                    )
                },
                restore = { list ->
                    Today(
                        viewMode = TodayViewMode.fromOrdinal(list[0])
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

    class All(
        isExpanded: Boolean = false
    ) : Section(key = "AllSection") {
        // 전체 탭에서 비활성 루프 그룹의 펼침 상태. 기본은 접힘이라 활성 루프에 집중되도록 한다.
        val isExpanded = mutableStateOf(isExpanded)

        companion object {
            val Saver = listSaver(
                save = {
                    listOf(
                        it.isExpanded.value
                    )
                },
                restore = { list ->
                    All(
                        isExpanded = list[0]
                    )
                }
            )
        }
    }

    /** 전체 탭 하단 기록 그리드 섹션. 표시할 루프가 있을 때만(size>0) 노출된다. */
    class AllHistoryGrid : Section(key = "AllHistoryGridSection")
}