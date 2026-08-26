package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.annotation.StringRes
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
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.ViewAgenda
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.TodayOccurrence
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.isInProgress
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.ui.common.AppEmptyState
import com.pnd.android.loop.ui.common.ExpandableNativeAd
import com.pnd.android.loop.ui.home.timeline.LoopCircularDial
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.toMs
import java.time.LocalDateTime

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
    snackBarHostState: SnackbarHostState,
    @HomeTab.Type selectedTab: Int,
    // 하단 패널에서 편집 중인 루프 id. 목록에서 해당 카드를 스포트라이트하고 나머지를 흐리게 하는 데 쓴다.
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onStateChanged: (LoopBase, Int) -> Unit,
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

        // 오늘 섹션에는 onStateChanged 를 넘기지 않는다. 항목(occurrence)마다 응답을 기록할
        // 날짜가 달라, 섹션 안에서 날짜를 실은 콜백을 직접 만든다.
        is Section.Today -> sectionToday(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            editingLoopId = editingLoopId,
            onEdit = onEdit,
            onDelete = onDelete,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )

        is Section.Yesterday -> sectionYesterday(
            section = section,
            blurState = blurState,
            loopViewModel = loopViewModel,
            snackBarHostState = snackBarHostState,
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
        // Statistics / history navigation now lives in the home app bar, so the
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
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    snackBarHostState: SnackbarHostState,
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
            blurState = blurState,
            loopViewModel = loopViewModel,
            loops = loops,
            snackBarHostState = snackBarHostState,
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
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val loops by section.items
    val groups by section.groups

    // 오늘 예정된 루프 자체가 없으면 전환할 뷰가 없으므로 토글 없이 안내만 보여준다.
    // "예정 없음"을 "모두 완료"로 오인하지 않도록 축하 화면과 톤을 분리한다.
    if (loops.isEmpty()) {
        sectionTodayNoSchedule()
        // 오늘 예정이 없어도 어젯밤에서 넘어온 몫은 답을 기다리고 있을 수 있다.
        sectionTodayCarriedOver(
            groups = groups,
            blurState = blurState,
            loopViewModel = loopViewModel,
            editingLoopId = editingLoopId,
            onEdit = onEdit,
            onDelete = onDelete,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
        return
    }

    // 예정이 있으면 완료 여부와 무관하게 뷰 토글을 항상 유지한다. 다 끝낸 뒤에도
    // 다이얼로 하루를 돌아볼 수 있어야 하기 때문이다.
    sectionTodayViewModeToggle(section = section)
    sectionTodayContent(
        section = section,
        blurState = blurState,
        loopViewModel = loopViewModel,
        editingLoopId = editingLoopId,
        onEdit = onEdit,
        onDelete = onDelete,
        onNavigateToDetailPage = onNavigateToDetailPage,
    )
}

/** 뷰 모드(목록/다이얼) 토글. 오늘 섹션 최상단에 항상 고정으로 노출한다. */
private fun LazyListScope.sectionTodayViewModeToggle(
    section: Section.Today,
) {
    item(
        contentType = ContentTypes.VIEW_MODE_TOGGLE_BUTTON,
        key = section.key,
    ) {
        val context = LocalContext.current
        val viewMode by section.viewMode
        ViewModeToggle(
            modifier = Modifier.padding(bottom = HomeCardSpacing),
            viewMode = viewMode,
            onSelected = { selected -> section.save(context = context, mode = selected) },
        )
    }
}

/**
 * 선택된 뷰 모드에 맞는 오늘 콘텐츠를 그린다.
 * 목록·다이얼 모두 오늘 몫이 하나도 남지 않으면(= 오늘 걸 모두 완료/스킵) 축하 화면으로 대체한다.
 *
 * 어젯밤에서 넘어온 몫은 어느 뷰에서든 카드로 덧붙인다([sectionTodayCarriedOver]). 시간축 뷰와
 * 축하 화면에는 그 몫을 그릴 자리가 없는데, 어제 미응답 카드에서도 빠져 있어 여기서 그리지
 * 않으면 답할 수 있는 화면이 아예 없어지기 때문이다.
 */
private fun LazyListScope.sectionTodayContent(
    section: Section.Today,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    // 목록 뷰에 그릴 그룹(지금 진행 중 / 다음 예정 / 응답 대기). 응답을 마친 몫은 이미 빠져 있다.
    val groups by section.groups
    // 축하 화면은 "오늘 몫"을 다 끝냈을 때만 띄운다. 어젯밤에서 넘어온 몫은 어제 것이라
    // 남아 있어도 오늘을 끝낸 것으로 본다(그 카드는 축하 화면 아래에 따로 남는다).
    val isTodayFinished = groups.none { group ->
        group.occurrences.any { occurrence -> !occurrence.isCarriedOver }
    }
    val hasCarriedOver = groups.any { group ->
        group.occurrences.any { occurrence -> occurrence.isCarriedOver }
    }

    // 시간축 뷰(다이얼)에는 어젯밤 몫을 그릴 자리가 없다. 카드로 따로 덧붙이지 않으면
    // 어제 미응답 카드에서도 빠져 있어 앱 안에서 답할 길이 사라진다.
    fun LazyListScope.carriedOverCards() = sectionTodayCarriedOver(
        groups = groups,
        blurState = blurState,
        loopViewModel = loopViewModel,
        editingLoopId = editingLoopId,
        onEdit = onEdit,
        onDelete = onDelete,
        onNavigateToDetailPage = onNavigateToDetailPage,
    )

    when (section.viewMode.value) {
        // 다이얼은 어젯밤 몫도 호로 그리므로(레인이 갈려 오늘 밤 몫과 겹치지 않는다)
        // 아래에 카드를 따로 덧붙이지 않는다.
        TodayViewMode.DIAL -> if (isTodayFinished && !hasCarriedOver) {
            sectionTodayFinished()
        } else {
            sectionTodayDial(
                blurState = blurState,
                loopViewModel = loopViewModel,
                occurrences = groups.flatMap { group -> group.occurrences },
                onEdit = onEdit,
                onDelete = onDelete,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
        }

        TodayViewMode.LIST -> if (isTodayFinished) {
            sectionTodayFinished()
            // 목록 뷰도 오늘 몫이 없으면 축하 화면으로 넘어가므로, 어젯밤 몫은 여기서 덧붙인다.
            carriedOverCards()
        } else {
            // onStateChanged 를 넘기지 않는 것은 의도적이다. 목록 항목은 occurrence 마다 기록할
            // 날짜가 달라, 이 함수 안에서 항목별 콜백을 따로 만든다.
            sectionTodayList(
                blurState = blurState,
                loopViewModel = loopViewModel,
                groups = groups,
                editingLoopId = editingLoopId,
                onEdit = onEdit,
                onDelete = onDelete,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
        }
    }
}

/**
 * 24시간 다이얼. 목록 뷰와 같은 occurrence 를 그대로 받아, 어젯밤에서 넘어온 몫도 호 하나로
 * 함께 그린다. 같은 시간대의 두 몫은 겹치므로 다이얼의 레인 배치가 서로 다른 동심원으로
 * 떼어 놓고, 어젯밤 몫은 속 빈 호(응답 대기)로 그려져 오늘 밤 몫과 구분된다.
 */
private fun LazyListScope.sectionTodayDial(
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    occurrences: List<TodayOccurrence>,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    item(
        contentType = ContentTypes.LOOP_DIAL,
        key = "LoopDial",
    ) {
        LoopCircularDial(
            modifier = Modifier.padding(
                horizontal = Dimens.screenHorizontalPadding,
                vertical = HomeCardSpacing,
            ),
            blurState = blurState,
            occurrences = occurrences,
            // 응답은 호가 대표하는 몫이 시작한 날짜 행에 기록한다. 다이얼이 그 날짜를 함께 넘긴다.
            onStateChanged = { loop, date, doneState ->
                loopViewModel.changeLoopState(
                    loop = loop,
                    localDate = date,
                    doneState = doneState,
                )
            },
            onEdit = onEdit,
            onDelete = onDelete,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

/**
 * 오늘 목록 뷰. 그룹([TodayLoopGroup])마다 캡션 한 줄을 먼저 놓고 그 아래에 카드를 쌓아,
 * "지금 하고 있는 것"과 "아직 남은 것"의 경계가 스크롤 중에도 보이게 한다.
 *
 * 한 줄의 단위는 루프가 아니라 occurrence 다([TodayOccurrence]). 자정을 넘기는 루프는 어젯밤
 * 몫과 오늘 밤 몫이 한 화면에 함께 걸치므로, 같은 루프가 서로 다른 그룹에 두 번 나올 수 있다.
 */
private fun LazyListScope.sectionTodayList(
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    groups: List<TodayLoopGroup>,
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    // 그룹이 하나뿐이면 구분할 대상이 없다. 이때 캡션은 정보 없이 높이만 차지하므로 생략한다.
    val showCaptions = groups.size > 1

    groups.forEach { group ->
        todayGroupItems(
            group = group,
            showCaption = showCaptions,
            blurState = blurState,
            loopViewModel = loopViewModel,
            editingLoopId = editingLoopId,
            onEdit = onEdit,
            onDelete = onDelete,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

/**
 * 시간축 뷰(다이얼)나 축하/예정 없음 안내 아래에 덧붙이는 어젯밤 몫 카드.
 *
 * 그 화면들에는 어젯밤 몫을 그릴 자리가 없고, 어제 미응답 카드에서도 자정 넘김 루프는 빠져
 * 있다([LoopRepository.loopsNoResponseYesterday]). 여기서 그리지 않으면 어느 화면에서도
 * 답할 수 없게 된다.
 */
private fun LazyListScope.sectionTodayCarriedOver(
    groups: List<TodayLoopGroup>,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val carriedOver = groups
        .flatMap { group -> group.occurrences }
        .filter { occurrence -> occurrence.isCarriedOver }
    if (carriedOver.isEmpty()) return

    todayGroupItems(
        group = TodayLoopGroup(group = TodayGroup.AWAITING, occurrences = carriedOver),
        showCaption = true,
        blurState = blurState,
        loopViewModel = loopViewModel,
        editingLoopId = editingLoopId,
        onEdit = onEdit,
        onDelete = onDelete,
        onNavigateToDetailPage = onNavigateToDetailPage,
    )
}

/** 그룹 하나를 캡션 한 줄 + 카드들로 쌓는다. 목록 뷰와 어젯밤 몫 카드가 함께 쓴다. */
private fun LazyListScope.todayGroupItems(
    group: TodayLoopGroup,
    showCaption: Boolean,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    editingLoopId: Int?,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val occurrences = group.occurrences
    if (showCaption) {
        item(
            contentType = ContentTypes.TODAY_GROUP_CAPTION,
            key = "TodayGroupCaption-${group.group}",
        ) {
            TodayGroupCaption(
                modifier = Modifier.animateItem(),
                group = group.group,
                count = occurrences.size,
            )
        }
    }

    items(
        items = occurrences,
        contentType = { ContentTypes.LOOP_CARD },
        // 같은 루프가 두 몫으로 나올 수 있으므로 날짜까지 넣어야 키가 겹치지 않는다.
        key = { occurrence -> "${occurrence.loop.loopId}-${occurrence.date}" },
    ) { occurrence ->
        val loop = occurrence.loop
        val highlightId by loopViewModel.highlightId.collectAsState()
        val isEditing = editingLoopId != null && loop.loopId == editingLoopId

        // 응답은 이 항목이 대표하는 occurrence 가 시작한 날짜 행에 기록해야 한다. 자정을
        // 넘기는 루프는 어젯밤 몫과 오늘 밤 몫의 저장 날짜가 다르므로 항목마다 날짜를 싣는다.
        val onStateChanged: (LoopBase, Int) -> Unit = remember(occurrence.date) {
            { changedLoop, doneState ->
                loopViewModel.changeLoopState(
                    loop = changedLoop,
                    localDate = occurrence.date,
                    doneState = doneState,
                )
            }
        }

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
                isCarriedOver = occurrence.isCarriedOver,
            ),
            onEdit = onEdit,
            onDelete = onDelete,
            onStateChanged = onStateChanged,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

/**
 * 그룹 캡션 한 줄("지금 진행 중 · 2"). "지금 진행 중"만 primary 색과 굵기로 세우고 나머지는
 * 대비를 낮춰, 목록을 훑을 때 시선이 진행 중 그룹에 먼저 닿게 한다.
 */
@Composable
private fun TodayGroupCaption(
    modifier: Modifier = Modifier,
    group: TodayGroup,
    count: Int,
) {
    val isNow = group == TodayGroup.NOW
    val label = stringResource(id = group.labelResId)

    Text(
        modifier = modifier.padding(
            // 카드 왼쪽 모서리에 맞춰 들여쓰고, 위쪽 그룹과는 넉넉히 띄워 경계를 만든다.
            start = Dimens.screenHorizontalPadding + 4.dp,
            end = Dimens.screenHorizontalPadding,
            top = 14.dp,
            bottom = 2.dp,
        ),
        text = stringResource(id = R.string.today_group_caption, label, count),
        style = AppTypography.labelMedium.copy(
            color = if (isNow) AppColor.primary else AppColor.onSurface.copy(alpha = 0.5f),
            fontWeight = if (isNow) FontWeight.SemiBold else FontWeight.Normal,
        ),
    )
}

/** 오늘 예정된 루프를 전부 완료/스킵했을 때 목록·다이얼 자리에 대신 그리는 축하 화면. */
private fun LazyListScope.sectionTodayFinished() {
    item(
        contentType = ContentTypes.LOOP_EMPTY,
        key = "TodayFinished",
    ) {
        TodayFinishedState()
    }
}

/** 오늘 예정된 루프 자체가 없을 때 그리는 중립 안내. */
private fun LazyListScope.sectionTodayNoSchedule() {
    item(
        contentType = ContentTypes.LOOP_EMPTY,
        key = "TodayNoSchedule",
    ) {
        TodayNoScheduleState()
    }
}

/**
 * 오늘 예정된 루프를 전부 완료/스킵했을 때의 축하 화면. 공용 [AppEmptyState]를 그대로
 * 써서 "루프 없음" 상태와 같은 문법으로 읽히되, 문구와 아이콘으로 보상의 느낌을 준다.
 */
@Composable
private fun TodayFinishedState(
    modifier: Modifier = Modifier,
) {
    AppEmptyState(
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

/**
 * 오늘 예정된 루프가 하나도 없을 때의 안내. [TodayFinishedState]와 같은 레이아웃을 쓰되
 * 달력 아이콘과 중립적인 문구로 "완료"가 아니라 "예정 없음"임을 분명히 한다.
 */
@Composable
private fun TodayNoScheduleState(
    modifier: Modifier = Modifier,
) {
    AppEmptyState(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.screenHorizontalPadding,
                vertical = 56.dp,
            ),
        icon = Icons.Outlined.CalendarToday,
        title = stringResource(id = R.string.today_no_scheduled_loops),
        hint = stringResource(id = R.string.today_no_scheduled_loops_hint),
    )
}

/**
 * Compact segmented control that flips the Today section between two view modes:
 * a plain card list and the 24-hour circular dial. Each mode reads as an equal,
 * tappable icon option.
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
                .selectableGroup()
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
            // selectable(role = RadioButton) 로 스크린리더가 "라디오 버튼 · 선택됨"으로 읽게 한다.
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
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
    VIEW_MODE_TOGGLE_BUTTON,
    TODAY_GROUP_CAPTION,
    LOOP_EMPTY,
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
 * 오늘 목록에서 루프가 속하는 그룹. 목록은 여기 선언한 순서대로 캡션과 함께 쌓인다.
 * - [NOW]: 지금 시간창 안에 있거나(시간제), 시작 버튼을 눌러 진행 중인(시간 미지정) 루프
 * - [UPCOMING]: 아직 시작하지 않은 루프. 시간 미지정 루프도 여기에 들어가되 그룹 맨 아래로 간다.
 * - [AWAITING]: 시간창이 끝났는데 완료/건너뜀 응답이 아직 없는 루프
 */
enum class TodayGroup {
    NOW,
    UPCOMING,
    AWAITING,
}

/**
 * 그룹 라벨 문자열. 홈 목록의 캡션과 위젯의 그룹 헤더가 함께 쓴다. 같은 분류에 다른 말이
 * 붙지 않도록 라벨 매핑은 여기 한 곳에만 둔다.
 */
@get:StringRes
val TodayGroup.labelResId: Int
    get() = when (this) {
        TodayGroup.NOW -> R.string.today_group_now
        TodayGroup.UPCOMING -> R.string.today_group_upcoming
        TodayGroup.AWAITING -> R.string.today_group_awaiting
    }

/** 캡션 한 줄과 그 아래에 쌓일 항목들. 비어 있는 그룹은 [buildTodayGroups]가 미리 걸러낸다. */
data class TodayLoopGroup(
    val group: TodayGroup,
    val occurrences: List<TodayOccurrence>,
)

/**
 * 오늘 목록에 그릴 occurrence 를 그룹으로 나눈다.
 *
 * 완료/건너뜀으로 이미 응답한 몫은 Done/Skip 카드가 따로 보여주므로 여기서 제외한다.
 * (진행 중(IN_PROGRESS)인 시간 미지정 루프는 정지 버튼과 함께 목록에 남아야 하므로 유지한다.)
 *
 * 그룹 안의 순서는 상위 정렬([com.pnd.android.loop.data.TodayLoopOrder])을 그대로 따르되,
 * 시간 미지정 루프만 뒤로 민다. 시각이 있는 "다음 예정"이 먼저 읽혀야 하기 때문이다.
 */
fun buildTodayGroups(
    occurrences: List<TodayOccurrence>,
    now: LocalDateTime,
): List<TodayLoopGroup> {
    val pending = occurrences.filter { it.loop.isNotRespond || it.loop.isInProgress }
    val byGroup = pending.groupBy { occurrence -> occurrence.todayGroup(now) }

    return TodayGroup.entries.mapNotNull { group ->
        val groupOccurrences = byGroup[group] ?: return@mapNotNull null
        TodayLoopGroup(
            group = group,
            // sortedBy 는 안정 정렬이라 시간 미지정만 뒤로 가고 나머지 순서는 그대로 남는다.
            occurrences = groupOccurrences.sortedBy { it.loop.isAnyTime },
        )
    }
}

/**
 * 지금([now]) 기준으로 이 occurrence 가 오늘 목록의 어느 그룹에 속하는지.
 *
 * 분기 순서와 기준은 카드가 제 상태를 정하는 [com.pnd.android.loop.data.TimeStat] 과 일부러
 * 똑같이 맞췄다. 캡션과 카드 본문이 다른 말을 하지 않게 하기 위해서다. 특히
 * [com.pnd.android.loop.util.isPast] 는 쓰지 않는다. 자정을 넘기는 루프(예: 22:00~06:30)에서는
 * 아침에 끝난 뒤 오늘 밤 다시 시작할 때까지의 낮 시간 전체를 "지난" 것으로 보기 때문에,
 * 그대로 쓰면 8시간 뒤 시작할 루프가 응답 대기로 잡힌다.
 */
private fun TodayOccurrence.todayGroup(now: LocalDateTime): TodayGroup {
    // 어젯밤 몫은 이미 끝난 occurrence 다. 시계는 다음 시작 전을 가리키지만 답을 기다리는 상태다.
    if (isCarriedOver) return TodayGroup.AWAITING

    val nowInDay = now.toLocalTime().toMs()
    return when {
        // 시간제는 시간창 안에 있을 때, 시간 미지정은 시작 버튼을 눌러 진행 중일 때가 "지금"이다.
        loop.isActive(now) || loop.isInProgress -> TodayGroup.NOW
        // 시간 미지정 루프는 시작 시각이 없어 시각으로 비교할 수 없다. 시작 전이면 예정으로 둔다.
        loop.isAnyTime -> TodayGroup.UPCOMING
        // 오늘의 시작 시각이 아직 오지 않았다. 자정을 넘기는 루프도 여기서 "오늘 밤 시작"으로 잡힌다.
        nowInDay < loop.startInDay -> TodayGroup.UPCOMING
        else -> TodayGroup.AWAITING
    }
}

/**
 * 오늘 섹션의 표시 방식.
 * - [LIST]: 기본 카드 목록
 * - [DIAL]: 24시간 원형 다이얼(시안 A)
 *
 * SharedPreferences 에는 [ordinal] 로 저장하므로, 뒤에 새 모드를 추가하는 것은 안전하지만
 * 순서를 바꾸면 저장된 값의 의미가 달라진다. 항목 순서는 유지할 것.
 */
enum class TodayViewMode {
    LIST,
    DIAL;

    companion object {
        /**
         * 저장된 ordinal 이 범위를 벗어나면 안전하게 [LIST] 로 되돌린다.
         * 삭제된 TIMELINE(구 ordinal 1) 뒤에 있던 DIAL 은 2 로 저장돼 있을 수 있어 따로 받아 준다.
         * 구 TIMELINE 자리(1)는 이제 [DIAL] 이 쓰므로, 타임라인을 쓰던 사용자는 다이얼로 넘어간다.
         */
        fun fromOrdinal(ordinal: Int): TodayViewMode = when (ordinal) {
            LEGACY_DIAL_ORDINAL -> DIAL
            else -> entries.getOrElse(ordinal) { LIST }
        }

        private const val LEGACY_DIAL_ORDINAL = 2
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

        /**
         * 목록 뷰가 그릴 그룹. 그룹 판정에는 현재 시각이 필요해 [items] 에서 바로 유도할 수 없으므로,
         * 시각을 아는 컴포저블(rememberTodaySection)이 계산해 여기에 넣어 준다.
         */
        val groups = mutableStateOf<List<TodayLoopGroup>>(emptyList())

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