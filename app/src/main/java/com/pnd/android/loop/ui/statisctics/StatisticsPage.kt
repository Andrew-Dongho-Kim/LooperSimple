package com.pnd.android.loop.ui.statisctics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.semantics.Role
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.common.FloatingHeaderShape
import com.pnd.android.loop.ui.common.FloatingSurface
import com.pnd.android.loop.ui.common.NavigationBarFadingEdge
import com.pnd.android.loop.ui.common.isPortrait
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.SpaceDashboard
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.key
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import com.pnd.android.loop.util.formatYearMonth
import kotlin.math.abs
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopWithStatistics
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.common.AppEmptyState
import com.pnd.android.loop.ui.common.AppSegmentedControl
import com.pnd.android.loop.ui.common.HistoryCalculationNote
import com.pnd.android.loop.ui.common.StatusBarFadingEdge
import com.pnd.android.loop.ui.common.appCardSurface
import com.pnd.android.loop.ui.common.backdropSource
import com.pnd.android.loop.ui.common.rememberBackdropState
import com.pnd.android.loop.ui.common.rememberListCollapseProgress
import com.pnd.android.loop.ui.common.supportsBackdropBlur
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.primarySurface
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.ui.theme.warning
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.MS_1HOUR
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.todayFlow
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withTimeoutOrNull

// 로딩 자리 표시자와 보조 동작도 앱 공통 모서리를 따른다.
private val CardShape = RoundShapes.large

// 순위 목록은 기본적으로 상위 N개만 접어서 보여준다. (긴 목록을 한 번에 렌더하지 않기 위한 상한)
private const val RANKING_COLLAPSED_COUNT = 5

@Composable
fun StatisticsPage(
    modifier: Modifier = Modifier,
    statisticsViewModel: StatisticsViewModel = hiltViewModel(),
    onNavigateToDetailPage: (Int) -> Unit,
    onNavigateUp: () -> Unit,
) {
    var selectedTab by rememberSaveable { mutableStateOf(StatisticsTab.OVERVIEW) }
    // 기간 선택(월별/전체, 월)은 헤더 탭·월 선택 시트·본문이 함께 쓰므로 여기서 한 곳에서 관리한다.
    // 화면 회전·프로세스 사망에도 사용자의 선택이 유지되도록 rememberSaveable을 쓴다.
    var showTotal by rememberSaveable { mutableStateOf(false) }
    val today by remember { todayFlow() }.collectAsState(LocalDate.now())
    val currentMonth = YearMonth.from(today)
    // null은 이번 달을 뜻한다. 월이 바뀌면 이번 달 표시는 따라가고, 과거 월 선택은 유지한다.
    var selectedMonthEpoch by rememberSaveable { mutableStateOf<Long?>(null) }
    val availableMonths = rememberLoadable { statisticsViewModel.availableMonths }
    val requestedMonth = selectedMonthEpoch?.let { YearMonth.from(LocalDate.ofEpochDay(it)) } ?: currentMonth
    val firstMonth = availableMonths.valueOrNull?.start ?: minOf(requestedMonth, currentMonth)
    val selectedMonth = requestedMonth.coerceIn(minOf(firstMonth, currentMonth), currentMonth)
    val selectedPeriod = if (showTotal) StatisticsPeriod.Total else StatisticsPeriod.Month(selectedMonth)
    val selectMonth: (YearMonth) -> Unit = { month ->
        selectedMonthEpoch = if (month == currentMonth) null else month.atDay(1).toEpochDay()
        showTotal = false
    }
    var showMonthSheet by rememberSaveable { mutableStateOf(false) }

    val listState = remember(selectedTab) { LazyListState() }
    val backdrop = rememberBackdropState()
    val headerBackdrop = if (supportsBackdropBlur) backdrop else null
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 기간을 쓰는 탭에서만 월간/전체 탭을 헤더에 띄운다. 그 외 탭은 뒤로가기와 제목만 두고, 제목이
    // 액션바 높이만큼의 스크롤에 걸쳐 사라진다.
    val showPeriodTabs = selectedTab.usesPeriod
    val liveHeaderProgress = if (showPeriodTabs) {
        rememberStatisticsHeaderCollapseProgress(listState)
    } else {
        rememberListCollapseProgress(listState, Dimens.appBarHeight)
    }
    // 기간 전환 중에는 목록이 잠깐 짧아져 스크롤이 맨 위로 밀리는데, 그 값을 헤더가 그대로 따르면
    // 헤더가 펼쳐졌다 접히며 깜빡인다. 전환하는 동안은 시작 직전의 진행도로 헤더를 고정한다.
    // 하단 탭을 바꾸면(= listState 교체) 고정도 함께 풀린다.
    var frozenHeaderProgress by remember(listState) { mutableStateOf<Float?>(null) }
    val headerProgress = frozenHeaderProgress ?: liveHeaderProgress.value

    // 플로팅 하단 탭 알약의 측정 높이와, 세로 화면에서 그 아래 깔리는 내비게이션 바 높이.
    var bottomNavigationHeight by remember { mutableStateOf(0.dp) }
    val bottomNavigationInset = if (LocalConfiguration.current.isPortrait()) {
        WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    } else {
        0.dp
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColor.background),
        containerColor = AppColor.background,
        contentColor = AppColor.onSurface,
        // 홈과 같이 세로 화면에서는 내비게이션 바 영역까지 콘텐츠를 그리고, 그 위에 페이딩 엣지와
        // 플로팅 하단 탭을 얹는다. 가로 화면은 내비게이션 바가 옆에 붙으므로 기존처럼 비켜 둔다.
        contentWindowInsets = if (LocalConfiguration.current.isPortrait()) {
            ScaffoldDefaults.contentWindowInsets
                .exclude(WindowInsets.navigationBars)
                .exclude(WindowInsets.statusBars)
        } else {
            ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.statusBars)
        },
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding).fillMaxSize()) {
            StatisticsPageContent(
                modifier = Modifier
                    .fillMaxSize()
                    .backdropSource(backdrop)
                    .background(AppColor.background),
                statisticsViewModel = statisticsViewModel,
                selectedTab = selectedTab,
                selectedPeriod = selectedPeriod,
                today = today,
                isPeriodLoading = availableMonths.isLoading,
                // 기간 선택이 헤더로 올라가 본문에서는 좌우로 밀어 한 달씩 넘기는 보조 수단만 둔다.
                onSwipeMonth = { delta ->
                    val target = selectedMonth.plusMonths(delta.toLong())
                    if (target in firstMonth..currentMonth) selectMonth(target)
                },
                onPeriodTransitionChanged = { inProgress ->
                    frozenHeaderProgress = if (inProgress) {
                        // 연달아 바뀌어 이미 고정 중이면, 처음 고정한 값(밀려나기 전 값)을 유지한다.
                        frozenHeaderProgress ?: liveHeaderProgress.value
                    } else {
                        null
                    }
                },
                listState = listState,
                // 탭 행 높이에는 이미 아래 여백이 들어 있으므로 contentPadding을 더하지 않는다.
                topPadding = if (showPeriodTabs) {
                    statisticsHeaderExpandedHeight(topInset)
                } else {
                    topInset + Dimens.appBarHeight + Dimens.contentPadding
                },
                // 마지막 카드가 플로팅 하단 탭과 내비게이션 바에 가리지 않고 그 위까지 올라오게 한다.
                bottomPadding = bottomNavigationInset + BottomNavigationMargin +
                    bottomNavigationHeight + Dimens.sectionSpacing,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
            StatusBarFadingEdge(modifier = Modifier.align(Alignment.TopCenter))
            // 내비게이션 바 밑으로 지나가는 콘텐츠가 시스템 버튼/제스처 바와 겹쳐 보이지 않게 흐려 준다.
            NavigationBarFadingEdge(modifier = Modifier.align(Alignment.BottomCenter))
            StatisticsBottomNavigation(
                modifier = Modifier.align(Alignment.BottomCenter),
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
                backdrop = headerBackdrop,
                onHeightChanged = { bottomNavigationHeight = it },
            )
            CollapsingStatisticsHeader(
                modifier = Modifier.align(Alignment.TopCenter),
                progress = headerProgress,
                showPeriodTabs = showPeriodTabs,
                showTotal = showTotal,
                monthLabel = statisticsMonthTabLabel(month = selectedMonth, today = today),
                monthDescription = stringResource(
                    R.string.stat_tab_month_description,
                    selectedMonth.atDay(1).formatYearMonth(),
                ),
                onTotalSelected = { showTotal = it },
                onMonthMenuClick = { if (!availableMonths.isLoading) showMonthSheet = true },
                onNavigateUp = onNavigateUp,
                backdrop = headerBackdrop,
            )
        }
    }

    if (showMonthSheet && showPeriodTabs && !showTotal) {
        StatisticsMonthSheet(
            selectedMonth = selectedMonth,
            firstMonth = firstMonth,
            currentMonth = currentMonth,
            onMonthStepped = selectMonth,
            onMonthSelected = { selectMonth(it); showMonthSheet = false },
            onDismiss = { showMonthSheet = false },
        )
    }
}

@Composable
private fun StatisticsPageContent(
    modifier: Modifier = Modifier,
    statisticsViewModel: StatisticsViewModel,
    selectedTab: StatisticsTab,
    selectedPeriod: StatisticsPeriod,
    today: LocalDate,
    isPeriodLoading: Boolean,
    onSwipeMonth: (delta: Int) -> Unit,
    onPeriodTransitionChanged: (inProgress: Boolean) -> Unit,
    listState: LazyListState,
    topPadding: Dp,
    bottomPadding: Dp,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    var rankingSortOrder by rememberSaveable { mutableStateOf(RankingSortOrder.COMPLETION_RATE) }
    var rankingExpanded by rememberSaveable { mutableStateOf(false) }

    val estimated by remember { statisticsViewModel.hasEstimatedHistory }.collectAsState(false)

    // 실제로 데이터를 불러와 그리는 기간. 탭·기간 선택기는 [selectedPeriod]를 곧바로 따르지만, 본문은
    // 이전 기간의 콘텐츠를 페이드 아웃한 뒤에야 이 값으로 넘어간다([crossfadePeriodContent]).
    var displayedPeriod by remember { mutableStateOf(selectedPeriod) }
    val isOverall = displayedPeriod == StatisticsPeriod.Total

    // 기간에 따라 달라지는 지표들.
    val periodStats = rememberLoadable(displayedPeriod) { statisticsViewModel.flowPeriodStats(displayedPeriod) }
    val ranking = rememberLoadable(displayedPeriod) { statisticsViewModel.flowLoopRanking(displayedPeriod) }

    // 기간과 무관하게 항상 전체(또는 최근) 흐름을 보는 지표들.
    val streak = rememberLoadable { statisticsViewModel.flowStreak() }
    val monthlyInvestedTimes = rememberLoadable { statisticsViewModel.flowMonthlyInvestedTime() }
    val completionTrend = rememberLoadable { statisticsViewModel.flowCompletionTrend() }
    val projection = rememberLoadable { statisticsViewModel.flowMonthlyProjection() }
    val habitHealth = rememberLoadable { statisticsViewModel.flowHabitHealth() }
    val newLoopSettling = rememberLoadable { statisticsViewModel.flowNewLoopSettling() }
    val milestones = rememberLoadable { statisticsViewModel.flowMilestones() }

    // 로딩/빈 상태 판단과 콘텐츠 렌더에 쓸 실제 값(로딩 중에는 콘텐츠가 호출되지 않으므로 기본값이면 충분).
    val statsValue = periodStats.valueOrNull ?: PeriodStats()
    val rankingValue = ranking.valueOrNull ?: emptyList()
    val streakValue = streak.valueOrNull ?: StreakStat(current = 0, longest = 0)
    val trendValue = completionTrend.valueOrNull ?: emptyList()
    val monthlyValue = monthlyInvestedTimes.valueOrNull ?: emptyList()
    val projectionValue = projection.valueOrNull ?: MonthlyProjection.Empty
    val habitHealthValue = habitHealth.valueOrNull ?: emptyList()
    val settlingValue = newLoopSettling.valueOrNull ?: emptyList()
    val milestonesValue = milestones.valueOrNull ?: emptyList()

    // 선택한 기준으로 전체 목록을 정렬한 뒤 순위 섹션에서 상위 항목을 표시한다.
    val topRanking = remember(rankingValue, rankingSortOrder) {
        rankingValue.sortedByDescending(rankingSortOrder.selector)
    }

    // 개요 = 기간과 무관한 추세·성취, 리듬 = 기간별 패턴, 기록 = 기간별 요약·순위.
    val overviewLoading = completionTrend.isLoading || monthlyInvestedTimes.isLoading || projection.isLoading ||
        habitHealth.isLoading || milestones.isLoading || streak.isLoading || newLoopSettling.isLoading
    val rhythmLoading = isPeriodLoading || periodStats.isLoading
    val recordsLoading = isPeriodLoading || periodStats.isLoading || ranking.isLoading
    val isLoading = when (selectedTab) {
        StatisticsTab.OVERVIEW -> overviewLoading
        StatisticsTab.RHYTHM -> rhythmLoading
        StatisticsTab.RECORDS -> recordsLoading
    }
    val settledPeriod = RetainScrollAcrossReload(
        listState = listState,
        period = displayedPeriod,
        isLoading = isLoading,
    )
    val contentAlpha = crossfadePeriodContent(
        targetPeriod = selectedPeriod,
        settledPeriod = settledPeriod,
        onSwitchPeriod = { period ->
            val changed = period != displayedPeriod
            if (changed) {
                displayedPeriod = period
                // 다른 기간의 순위는 길이가 달라 펼침 상태를 이어 갈 이유가 없다.
                rankingExpanded = false
            }
            changed
        },
        onTransitionChanged = onPeriodTransitionChanged,
    )
    // 월별을 보는 동안에만 좌우로 밀어 한 달씩 넘긴다(헤더의 월 선택을 보조하는 지름길).
    val swipeModifier = if (selectedTab.usesPeriod && selectedPeriod is StatisticsPeriod.Month) {
        Modifier.swipeToChangeMonth(onSwipeMonth)
    } else {
        Modifier
    }

    CompositionLocalProvider(LocalStatisticsContentAlpha provides { contentAlpha.value }) {
        LazyColumn(
            modifier = modifier.then(swipeModifier),
            state = listState,
            contentPadding = PaddingValues(
                start = Dimens.screenHorizontalPadding,
                end = Dimens.screenHorizontalPadding,
                top = topPadding,
                bottom = bottomPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing),
        ) {
            // 기간에 반응하는 탭은 지금 보는 기간의 범위를, 그 외 탭은 스코프를 알리는 안내 문구를 둔다.
            // 범위는 수치와 함께 바뀌어야 하므로 표시 중인 기간을 따르고 함께 페이드된다.
            if (selectedTab.usesPeriod) {
                fadingItem(key = "scope") {
                    StatisticsPeriodCaption(period = displayedPeriod, today = today)
                }
            } else {
                item(key = "scope") {
                    StatisticsScopeCaption()
                }
            }

            item(key = "calculation") {
                HistoryCalculationNote(estimated)
            }
            when (selectedTab) {
                StatisticsTab.OVERVIEW -> statefulTab(
                    isLoading = overviewLoading,
                    isEmpty = trendValue.size < 2 && monthlyValue.isEmpty() && streakValue.longest == 0 &&
                        !hasInsights(projectionValue, habitHealthValue, milestonesValue) && settlingValue.isEmpty(),
                    isOverall = true,
                ) {
                    trendContent(completionTrend = trendValue, monthlyInvestedTimes = monthlyValue)
                    achievementContent(
                        projection = projectionValue, streak = streakValue, milestones = milestonesValue,
                        habitHealth = habitHealthValue, newLoopSettling = settlingValue,
                    )
                }

                StatisticsTab.RHYTHM -> statefulTab(
                    isLoading = rhythmLoading,
                    isEmpty = statsValue.isEmpty,
                    isOverall = isOverall,
                ) {
                    patternContent(stats = statsValue)
                }

                StatisticsTab.RECORDS -> statefulTab(
                    isLoading = recordsLoading,
                    isEmpty = statsValue.isEmpty && topRanking.isEmpty(),
                    isOverall = isOverall,
                ) {
                    summaryContent(
                        stats = statsValue,
                        ranking = topRanking,
                        sortOrder = rankingSortOrder,
                        onSortSelected = { rankingSortOrder = it },
                        rankingExpanded = rankingExpanded,
                        onToggleRanking = { rankingExpanded = !rankingExpanded },
                        onNavigateToDetailPage = onNavigateToDetailPage,
                    )
                }

            }
        }
    }
}

/** Flow를 [Loadable]로 감싸 수집한다. 첫 방출 전에는 [Loadable.Loading]을 반환한다. */
@Composable
private fun <T> rememberLoadable(
    vararg keys: Any?,
    factory: () -> Flow<T>,
): Loadable<T> {
    val loadableFlow = remember(*keys) {
        factory().map<T, Loadable<T>> { Loadable.Loaded(it) }
    }
    // 새 월을 조회하는 동안 이전 월의 수치가 새 제목 아래 노출되지 않게 수집 상태도 초기화한다.
    return key(loadableFlow) { loadableFlow.collectAsState(initial = Loadable.Loading).value }
}

/**
 * 기간(월간/전체, 월)을 바꿔도 스크롤 위치를 유지한다.
 *
 * 기간이 바뀌면 [rememberLoadable]이 이전 수치를 감추려고 [Loadable.Loading]으로 돌아가고, 그동안
 * 목록은 짧은 스켈레톤 카드로 바뀐다. 목록이 짧아지면서 스크롤이 맨 위로 밀려나고, 데이터가 다시
 * 채워져도 원래 자리로 돌아오지 않았다. 그래서 콘텐츠가 보이는 동안의 위치를 계속 기억해 두었다가
 * 로딩이 끝나면 그 위치로 되돌린다.
 *
 * 로딩이 시작되면 [isLoading] 키가 바뀌어 기록 코루틴이 레이아웃(= 스크롤이 밀려나는 시점)보다
 * 먼저 취소되므로, 밀려난 위치가 저장된 값을 덮어쓰지 않는다. 하단 탭을 바꾸면 [listState]가 새로
 * 만들어지므로 저장된 위치도 함께 초기화된다.
 *
 * 돌려주는 값은 스크롤을 마지막으로 제자리에 되돌린 기간이다. [period]의 데이터가 다 들어와 위치를
 * 되돌린 뒤에야 그 기간이 된다. 기간 전환([crossfadePeriodContent])은 이 값이 새 기간과 같아질 때까지
 * 기다렸다가 콘텐츠를 드러낸다. (로딩 여부 같은 불리언으로 알리면, 기간을 바꾼 직후 아직 로딩이
 * 반영되지 않은 프레임에서 이전 기간의 "완료"를 새 기간의 것으로 오인할 수 있다.)
 */
@Composable
private fun RetainScrollAcrossReload(
    listState: LazyListState,
    period: StatisticsPeriod,
    isLoading: Boolean,
): MutableState<StatisticsPeriod?> {
    val savedPosition = remember(listState) { intArrayOf(0, 0) }
    val settledPeriod = remember(listState) { mutableStateOf<StatisticsPeriod?>(null) }
    LaunchedEffect(listState, period, isLoading) {
        if (isLoading) return@LaunchedEffect
        listState.scrollToItem(index = savedPosition[0], scrollOffset = savedPosition[1])
        settledPeriod.value = period
        snapshotFlow { listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset }
            .collect { (index, offset) ->
                savedPosition[0] = index
                savedPosition[1] = offset
            }
    }
    return settledPeriod
}

private const val PERIOD_FADE_OUT_MS = 150
private const val PERIOD_FADE_IN_MS = 220

/** 새 기간 데이터가 이보다 늦게 오면 기다리지 않고 페이드 인해, 스켈레톤이라도 보여 준다. */
private const val PERIOD_LOAD_WAIT_MS = 400L

/** 기간에 따라 바뀌는 본문 항목([fadingItem])이 따르는 불투명도. 람다라 읽는 쪽은 그리기 단계에서만 갱신된다. */
private val LocalStatisticsContentAlpha = compositionLocalOf<() -> Float> { { 1f } }

/**
 * 월간/전체(또는 월) 전환 시 본문을 페이드 아웃 → 기간 교체 → 페이드 인으로 바꾼다.
 *
 * 기간을 바꾸는 즉시 새 기간으로 넘어가면 이전 수치가 스켈레톤으로 툭 바뀌고, 목록이 짧아졌다가
 * 스크롤이 복원되는 과정까지 그대로 보인다. 그래서 먼저 이전 콘텐츠를 감춘 뒤 [onSwitchPeriod]로
 * 기간을 넘기고, 새 데이터가 들어와 스크롤이 제자리를 찾은 다음 프레임에 다시 드러낸다.
 *
 * 연달아 바꾸면(예: 이전 달 화살표 연타) 진행 중이던 전환은 취소되고 현재 불투명도에서 이어서 시작한다.
 *
 * 전환하는 동안 목록이 스켈레톤으로 짧아지면 스크롤이 맨 위로 밀려나는데, 헤더의 접힘 진행도는
 * 스크롤을 따르므로 그대로 두면 헤더가 잠깐 펼쳐졌다(제목이 보이고 탭·뒤로가기의 플로팅 배경이
 * 깜빡임). 그래서 전환을 시작할 때 [onTransitionChanged]`(true)`로 헤더를 지금 모습에 고정하고,
 * 스크롤이 제자리를 찾아 한 번 배치된 뒤에 `(false)`로 풀어 준다.
 */
@Composable
private fun crossfadePeriodContent(
    targetPeriod: StatisticsPeriod,
    settledPeriod: MutableState<StatisticsPeriod?>,
    onSwitchPeriod: (StatisticsPeriod) -> Boolean,
    onTransitionChanged: (inProgress: Boolean) -> Unit,
): State<Float> {
    val alpha = remember { Animatable(1f) }
    val currentOnSwitchPeriod by rememberUpdatedState(onSwitchPeriod)
    val currentOnTransitionChanged by rememberUpdatedState(onTransitionChanged)
    // 첫 컴포지션에서는 이미 targetPeriod를 그리고 있으므로 전환하지 않는다.
    var isFirstRun by remember { mutableStateOf(true) }
    LaunchedEffect(targetPeriod) {
        if (isFirstRun) {
            isFirstRun = false
            return@LaunchedEffect
        }
        // 연달아 바뀌어 이전 전환이 취소된 경우에도 고정은 이미 걸려 있으므로 그대로 이어 간다.
        currentOnTransitionChanged(true)
        alpha.animateTo(0f, tween(PERIOD_FADE_OUT_MS))
        // 실제로 기간이 바뀌어 다시 불러오게 되면, A→B→A처럼 되돌아왔을 때 예전에 A로 기록된 값이
        // 남아 있을 수 있으므로 비워 둔다. 페이드 아웃 도중 원래 기간으로 되돌아와 바뀌지 않았다면
        // 기록을 그대로 두어, 이미 자리 잡았으면 곧바로, 이전 로딩이 진행 중이면 그 끝을 기다린다.
        // (교체와 비우기 사이에 중단 지점이 없어 그 틈에 새 기록이 끼어들 수 없다.)
        if (currentOnSwitchPeriod(targetPeriod)) settledPeriod.value = null
        // 새 기간의 데이터가 들어와 스크롤이 제자리를 찾기를 기다린다.
        val isSettled = { settledPeriod.value == targetPeriod }
        val settledInTime = withTimeoutOrNull(PERIOD_LOAD_WAIT_MS) {
            snapshotFlow(isSettled).first { it }
        } != null
        if (!settledInTime) {
            // 로딩이 길어지면 스켈레톤이라도 먼저 보여 주고, 헤더 고정은 스크롤이 돌아올 때까지 유지한다.
            alpha.animateTo(1f, tween(PERIOD_FADE_IN_MS))
            snapshotFlow(isSettled).first { it }
        }
        // 되돌린 스크롤 위치로 목록이 한 번 배치되어야 헤더가 읽는 위치도 맞는다.
        withFrameNanos { }
        currentOnTransitionChanged(false)
        alpha.animateTo(1f, tween(PERIOD_FADE_IN_MS))
    }
    return alpha.asState()
}

/** 이만큼 넘게 가로로 밀어야 달이 바뀐다. 세로 스크롤 중 살짝 흔들린 손가락에 반응하지 않도록 넉넉히 둔다. */
private val MonthSwipeThreshold = 72.dp

/**
 * 본문을 좌우로 밀어 한 달씩 넘긴다. 왼쪽으로 밀면 다음 달, 오른쪽으로 밀면 이전 달(RTL에서는 반대).
 * 가로 터치 슬롭을 넘겨야 제스처를 가져가므로 리스트의 세로 스크롤과 다투지 않는다.
 * [onSwipe]에는 이전 달이면 -1, 다음 달이면 +1을 넘긴다. 범위를 벗어나는지는 호출하는 쪽이 판단한다.
 */
@Composable
private fun Modifier.swipeToChangeMonth(onSwipe: (delta: Int) -> Unit): Modifier {
    val currentOnSwipe by rememberUpdatedState(onSwipe)
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    val thresholdPx = with(LocalDensity.current) { MonthSwipeThreshold.toPx() }
    return pointerInput(isRtl, thresholdPx) {
        var dragX = 0f
        detectHorizontalDragGestures(
            onDragStart = { dragX = 0f },
            onDragEnd = {
                if (abs(dragX) >= thresholdPx) {
                    val towardNext = (dragX < 0) != isRtl
                    currentOnSwipe(if (towardNext) 1 else -1)
                }
            },
            onHorizontalDrag = { change, amount ->
                change.consume()
                dragX += amount
            },
        )
    }
}

/** 기간 전환 페이드([crossfadePeriodContent])를 따르는 항목. 기간에 따라 바뀌는 본문은 모두 이것으로 추가한다. */
private fun LazyListScope.fadingItem(
    key: Any,
    content: @Composable () -> Unit,
) {
    item(key = key) {
        val alpha = LocalStatisticsContentAlpha.current
        Box(modifier = Modifier.graphicsLayer { this.alpha = alpha() }) {
            content()
        }
    }
}

// region Tabs & selectors ----------------------------------------------------

/** 플로팅 하단 탭과 내비게이션 바(또는 화면 아래 끝) 사이 간격. */
private val BottomNavigationMargin = 12.dp

/** 플로팅 하단 탭 알약과 그 안의 탭 사이 여백. 헤더의 탭 알약과 같은 값이다. */
private val BottomNavigationRim = 5.dp

/**
 * 개요/리듬/기록 하단 탭. 화면 아래 가운데에 떠 있는 알약으로, 헤더 알약과 같은 블러 배경
 * ([FloatingSurface])을 써서 아래로 지나가는 콘텐츠가 비쳐 보인다.
 *
 * 세로 화면에서는 콘텐츠가 내비게이션 바 영역까지 그려지므로 그만큼 띄운다. 알약의 실제 높이
 * (아래 간격 제외)는 [onHeightChanged]로 알려, 목록이 마지막 카드를 이 탭 위까지 올릴 수 있게 한다(큰 글꼴에서도 맞도록
 * 고정값 대신 측정값을 쓴다).
 */
@Composable
private fun StatisticsBottomNavigation(
    selectedTab: StatisticsTab,
    onTabSelected: (StatisticsTab) -> Unit,
    backdrop: BackdropState?,
    onHeightChanged: (Dp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val isPortrait = LocalConfiguration.current.isPortrait()
    FloatingSurface(
        modifier = modifier
            .then(if (isPortrait) Modifier.navigationBarsPadding() else Modifier)
            .padding(bottom = BottomNavigationMargin)
            .onSizeChanged { onHeightChanged(with(density) { it.height.toDp() }) },
        progress = 1f,
        shape = FloatingHeaderShape,
        backdrop = backdrop,
    ) {
        Row(
            modifier = Modifier
                .selectableGroup()
                .padding(BottomNavigationRim),
        ) {
            StatisticsTab.entries.forEach { tab ->
                StatisticsBottomNavigationItem(
                    tab = tab,
                    selected = tab == selectedTab,
                    onClick = { onTabSelected(tab) },
                )
            }
        }
    }
}

@Composable
private fun StatisticsBottomNavigationItem(
    tab: StatisticsTab,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val ink by animateColorAsState(
        if (selected) AppColor.primary else AppColor.onSurfaceVariant,
        label = "bottomNavInk",
    )
    val indicator by animateColorAsState(
        if (selected) AppColor.primarySurface else AppColor.primarySurface.copy(alpha = 0f),
        label = "bottomNavIndicator",
    )
    Column(
        modifier = Modifier
            .clip(FloatingHeaderShape)
            .background(indicator)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .widthIn(min = 76.dp)
            .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val icon = when (tab) {
            StatisticsTab.OVERVIEW -> Icons.Outlined.SpaceDashboard
            StatisticsTab.RHYTHM -> Icons.Outlined.BarChart
            StatisticsTab.RECORDS -> Icons.Outlined.History
        }
        Icon(modifier = Modifier.size(22.dp), imageVector = icon, contentDescription = null, tint = ink)
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = stringResource(tab.titleRes),
            style = AppTypography.labelMedium,
            color = ink,
            maxLines = 1,
        )
    }
}

/** 기간과 무관한 탭임을 알리는 안내 문구. (기간 선택기를 대체해 스코프 혼동을 막는다.) */
@Composable
private fun StatisticsScopeCaption(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        text = stringResource(id = R.string.stat_scope_overall),
        textAlign = TextAlign.Center,
        style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.5f)),
    )
}

// endregion

// region Loading / empty scaffolding -----------------------------------------

/**
 * 탭 본문을 로딩/빈/정상 세 상태로 감싼다.
 * - 로딩 중: 스켈레톤 카드로 자리를 잡아 "빈 화면 → 갑자기 채워짐" 깜빡임을 없앤다.
 * - 비어 있음: 해당 스코프(기간/전체)에 맞는 안내를 보여 백지 화면을 막는다.
 * - 정상: [content]를 그대로 방출한다.
 */
private fun LazyListScope.statefulTab(
    isLoading: Boolean,
    isEmpty: Boolean,
    isOverall: Boolean,
    content: LazyListScope.() -> Unit,
) {
    when {
        isLoading -> {
            fadingItem(key = "skeleton_header") { SkeletonCard(height = 44.dp) }
            fadingItem(key = "skeleton_1") { SkeletonCard(height = 120.dp) }
            fadingItem(key = "skeleton_2") { SkeletonCard(height = 120.dp) }
        }

        isEmpty -> fadingItem(key = "empty") { EmptyHint(isOverall = isOverall) }

        else -> content()
    }
}

/** 로딩 중 자리를 잡아 주는 은은하게 깜빡이는 플레이스홀더 카드. */
@Composable
private fun SkeletonCard(
    modifier: Modifier = Modifier,
    height: Dp,
) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.04f,
        targetValue = 0.12f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .clip(CardShape)
            .background(color = AppColor.onSurface.copy(alpha = alpha)),
    )
}

@Composable
private fun EmptyHint(
    modifier: Modifier = Modifier,
    isOverall: Boolean = false,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        // 다른 화면과 같은 공용 빈 상태(틴트 원 아이콘 + 안내 문구)로 통일한다.
        AppEmptyState(
            icon = Icons.Outlined.BarChart,
            title = stringResource(
                id = if (isOverall) R.string.stat_empty_overall else R.string.stat_empty,
            ),
        )
    }
}

// endregion

// region Tab content builders ------------------------------------------------

/** 요약 탭(기간 기준): 요약 KPI + 계획대비 실제 + 회고 + 루프 순위. */
private fun LazyListScope.summaryContent(
    stats: PeriodStats,
    ranking: List<LoopWithStatistics>,
    sortOrder: RankingSortOrder,
    onSortSelected: (RankingSortOrder) -> Unit,
    rankingExpanded: Boolean,
    onToggleRanking: () -> Unit,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    fadingItem(key = "summary") {
        SummarySection(
            summary = stats.summary,
            perfectDays = stats.perfectDays,
            skipCount = stats.skipCount,
        )
    }

    if (stats.planVsActual.hasData) {
        fadingItem(key = "planVsActual") {
            PlanVsActualSection(stat = stats.planVsActual)
        }
    }

    if (stats.retrospect.hasData) {
        fadingItem(key = "retrospect") {
            RetrospectSection(stat = stats.retrospect)
        }
    }

    rankingSection(
        ranking = ranking,
        sortOrder = sortOrder,
        onSortSelected = onSortSelected,
        expanded = rankingExpanded,
        onToggleExpanded = onToggleRanking,
        onNavigateToDetailPage = onNavigateToDetailPage,
    )
}

/** 패턴 탭(기간 기준): 시간대 히트맵 + 요일 꾸준함. */
private fun LazyListScope.patternContent(stats: PeriodStats) {
    fadingItem(key = "hourly") {
        HourlyHeatmapSection(hourlyStats = stats.hourlyStats)
    }

    fadingItem(key = "weekly") {
        WeeklyConsistencySection(stats = stats.dayOfWeekStats)
    }
}

/** 추세 탭(전체 기준): 완료율 추세 + 월별 투자 시간. */
private fun LazyListScope.trendContent(
    completionTrend: List<CompletionRatePoint>,
    monthlyInvestedTimes: List<MonthlyInvestedTime>,
) {
    if (completionTrend.size >= 2) {
        fadingItem(key = "trend") {
            CompletionTrendSection(points = completionTrend)
        }
    }

    if (monthlyInvestedTimes.isNotEmpty()) {
        fadingItem(key = "monthly") {
            MonthlyInvestedSection(monthlyInvestedTimes = monthlyInvestedTimes)
        }
    }
}

/** 성취 탭(전체 기준): 인사이트 피드 + 연속 달성 + 마일스톤 + 습관 건강 + 신규 루프 정착. */
private fun LazyListScope.achievementContent(
    projection: MonthlyProjection,
    streak: StreakStat,
    milestones: List<Milestone>,
    habitHealth: List<HabitHealth>,
    newLoopSettling: List<NewLoopSettling>,
) {
    if (hasInsights(projection, habitHealth, milestones)) {
        fadingItem(key = "insight") {
            InsightFeedSection(
                projection = projection,
                habitHealth = habitHealth,
                milestones = milestones,
            )
        }
    }

    if (streak.longest > 0) {
        fadingItem(key = "streak") {
            StreakSection(streak = streak)
        }
    }

    if (milestones.isNotEmpty()) {
        fadingItem(key = "milestones") {
            MilestonesSection(milestones = milestones)
        }
    }

    if (habitHealth.isNotEmpty()) {
        fadingItem(key = "health") {
            HabitHealthSection(items = habitHealth)
        }
    }

    if (newLoopSettling.isNotEmpty()) {
        fadingItem(key = "settling") {
            NewLoopSettlingSection(items = newLoopSettling)
        }
    }
}

// endregion

// region Insight feed (성취 상단) --------------------------------------------

/** 인사이트 카드를 하나라도 보여줄 수 있는지 판단한다. (없으면 피드 섹션 자체를 숨긴다.) */
private fun hasInsights(
    projection: MonthlyProjection,
    habitHealth: List<HabitHealth>,
    milestones: List<Milestone>,
): Boolean = projection.hasData || habitHealth.isNotEmpty() || milestones.any { it.reached > 0 }

@Composable
private fun InsightFeedSection(
    modifier: Modifier = Modifier,
    projection: MonthlyProjection,
    habitHealth: List<HabitHealth>,
    milestones: List<Milestone>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_insight_title),
            description = stringResource(id = R.string.stat_insight_desc),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
            // ⑥ 가장 많이 하락한 습관 경고를 가장 먼저 노출한다(행동을 유도).
            habitHealth.firstOrNull()?.let { worst ->
                InsightCard(
                    accent = if (worst.level == HabitHealthLevel.AT_RISK) AppColor.error else AppColor.warning,
                    title = stringResource(id = R.string.stat_insight_at_risk, worst.title),
                    description = stringResource(
                        id = R.string.stat_insight_at_risk_desc,
                        (worst.previousRate * 100).toInt(),
                        (worst.recentRate * 100).toInt(),
                    ),
                )
            }
            // ⑩ 이번 달 완료 예측.
            if (projection.hasData) {
                InsightCard(
                    accent = AppColor.primary,
                    title = stringResource(id = R.string.stat_insight_projection, projection.projectedTotal),
                    description = stringResource(id = R.string.stat_insight_projection_desc, projection.doneSoFar),
                )
            }
            // ⑨ 달성한 마일스톤 축하(가장 동기부여되는 순서로 하나만).
            milestoneInsight(milestones)?.let { (title, desc) ->
                InsightCard(accent = AppColor.primary, title = title, description = desc)
            }
        }
    }
}

/** 달성한 마일스톤 중 하나를 골라 축하 문구(title, desc)를 만든다. (스트릭 > 투자시간 > 총 완료 순) */
@Composable
private fun milestoneInsight(milestones: List<Milestone>): Pair<String, String>? {
    val order = listOf(MilestoneType.LONGEST_STREAK, MilestoneType.INVESTED_HOURS, MilestoneType.TOTAL_DONE)
    val achieved = order.firstNotNullOfOrNull { type ->
        milestones.firstOrNull { it.type == type && it.reached > 0 }
    } ?: return null

    val reachedText = milestoneValueText(type = achieved.type, value = achieved.reached)
    val title = stringResource(id = R.string.stat_insight_milestone, reachedText)
    val desc = achieved.next?.let {
        stringResource(id = R.string.stat_milestone_next, milestoneValueText(achieved.type, it))
    } ?: stringResource(id = R.string.stat_milestone_max)
    return title to desc
}

@Composable
private fun InsightCard(
    modifier: Modifier = Modifier,
    accent: Color,
    title: String,
    description: String,
) {
    AppCard(
        modifier = modifier,
        color = accent.copy(alpha = 0.10f).compositeOver(AppColor.surfaceElevated),
    ) {
        Text(
            text = title,
            style = AppTypography.bodyLarge.copy(color = accent, fontWeight = FontWeight.Bold),
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = description,
            style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
        )
    }
}

// endregion

// region Summary KPI cards ---------------------------------------------------

@Composable
private fun SummarySection(
    modifier: Modifier = Modifier,
    summary: StatisticsSummary,
    perfectDays: Int,
    skipCount: Int,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing),
    ) {
        // 기간 내 루프에 투자한 총 누적 시간을 강조해 보여주는 대표 카드.
        InvestedTimeCard(investedTimeMs = summary.investedTimeMs)
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${summary.completedCount}",
                label = stringResource(id = R.string.stat_summary_completed),
                accent = true,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${(summary.completionRate * 100).toInt()}%",
                label = stringResource(id = R.string.stat_summary_completion_rate),
                accent = true,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "$perfectDays",
                label = stringResource(id = R.string.stat_summary_perfect_days),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "$skipCount",
                label = stringResource(id = R.string.stat_summary_skipped),
                valueColor = if (skipCount > 0) AppColor.warning else null,
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${summary.activeLoops}",
                label = stringResource(id = R.string.stat_summary_active_loops),
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = "${summary.activeDays}",
                label = stringResource(id = R.string.stat_summary_active_days),
            )
        }
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    accent: Boolean = false,
    valueColor: Color? = null,
) {
    AppCard(modifier = modifier) {
        Text(
            text = value,
            style = AppTypography.headlineMedium.copy(
                color = valueColor ?: if (accent) AppColor.primary else AppColor.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            modifier = Modifier.padding(top = Dimens.itemSpacing),
            text = label,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )
    }
}

/**
 * 루프에 투자한 총 누적 시간을 강조하는 대표(히어로) 카드.
 * primary 색을 옅게 깐 배경으로 다른 KPI 카드와 시각적으로 구분한다.
 * (배경/글자 모두 테마 색을 사용하므로 다크/라이트 모드에 자동으로 대응한다.)
 */
@Composable
private fun InvestedTimeCard(
    modifier: Modifier = Modifier,
    investedTimeMs: Long,
) {
    AppCard(modifier = modifier, color = AppColor.primarySurface) {
        Text(
            text = stringResource(id = R.string.stat_summary_invested),
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )
        Text(
            modifier = Modifier.padding(top = Dimens.itemSpacing),
            text = investedDurationText(investedTimeMs = investedTimeMs),
            style = AppTypography.headlineLarge.copy(
                color = AppColor.primary,
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// endregion

// region Completion-rate trend (②) -------------------------------------------

@Composable
private fun CompletionTrendSection(
    modifier: Modifier = Modifier,
    points: List<CompletionRatePoint>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_trend_title),
            description = stringResource(id = R.string.stat_trend_desc),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appCardSurface()
                .padding(Dimens.contentPadding),
        ) {
            // 마지막 두 달의 완료율 변화(퍼센트포인트)를 상단에 배지로 요약한다.
            val deltaPoints = (points.last().rate - points[points.size - 2].rate) * 100
            val deltaText = "${if (deltaPoints >= 0) "+" else ""}${deltaPoints.toInt()}%p"
            Text(
                text = deltaText,
                style = AppTypography.titleMedium.copy(
                    color = if (deltaPoints >= 0) AppColor.primary else AppColor.error,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.contentPadding)
                    .height(140.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(Dimens.itemSpacing),
            ) {
                points.forEach { point ->
                    CompletionRateBar(
                        modifier = Modifier.weight(1f),
                        point = point,
                    )
                }
            }
        }
    }
}

@Composable
private fun CompletionRateBar(
    modifier: Modifier = Modifier,
    point: CompletionRatePoint,
) {
    // 완료율(0~1)을 그대로 막대 높이 비율로 쓴다(정규화하지 않아 절대 수준이 드러난다).
    val percent = (point.rate * 100).toInt()
    val monthName = stringResource(id = ABB_MONTHS[point.yearMonth.monthValue - 1])
    VerticalBar(
        modifier = modifier,
        ratio = point.rate,
        topLabel = "$percent",
        topLabelAlpha = if (point.rate > 0f) 0.8f else 0.3f,
        bottomLabel = monthName,
        barColor = AppColor.primary.copy(alpha = 0.35f + 0.65f * point.rate),
        description = stringResource(id = R.string.stat_cd_trend_bar, monthName, percent),
    )
}

// endregion

// region Hourly heatmap (①) --------------------------------------------------

@Composable
private fun HourlyHeatmapSection(
    modifier: Modifier = Modifier,
    hourlyStats: List<HourlyCompletion>,
) {
    // 사용자가 특정 시간대를 탭하면 그 시각의 정확한 완료 횟수를 헤더에 보여준다(색만으로 읽기 어려운 값을 보완).
    var selectedHour by remember(hourlyStats) { mutableStateOf<Int?>(null) }
    val peak = hourlyStats.maxByOrNull { it.count }?.takeIf { it.count > 0 }

    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_hourly_title),
            description = stringResource(id = R.string.stat_hourly_desc),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appCardSurface()
                .padding(Dimens.contentPadding),
        ) {
            val hasHighlight = selectedHour != null || peak != null
            val headerText = when {
                selectedHour != null -> {
                    val count = hourlyStats.getOrNull(selectedHour!!)?.count ?: 0
                    stringResource(id = R.string.stat_hourly_selected, selectedHour!!, count)
                }

                peak != null -> stringResource(id = R.string.stat_hourly_peak, peak.hour)
                else -> stringResource(id = R.string.stat_hourly_none)
            }
            Text(
                text = headerText,
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface.copy(alpha = if (hasHighlight) 0.8f else 0.5f),
                    fontWeight = if (hasHighlight) FontWeight.Bold else FontWeight.Normal,
                ),
            )

            // 스크린리더에는 24개 칸을 따로 읽어 주기보다 대표 문구 하나로 요약한다.
            val cellsDescription = peak
                ?.let { stringResource(id = R.string.stat_hourly_peak, it.hour) }
                ?: stringResource(id = R.string.stat_hourly_none)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.contentPadding)
                    .height(40.dp)
                    .clearAndSetSemantics { contentDescription = cellsDescription },
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                hourlyStats.forEach { hourly ->
                    val isSelected = selectedHour == hourly.hour
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .clickable { selectedHour = if (isSelected) null else hourly.hour }
                            .background(
                                color = when {
                                    isSelected -> AppColor.primary
                                    hourly.count == 0 -> AppColor.onSurface.copy(alpha = 0.06f)
                                    else -> AppColor.primary.copy(alpha = 0.25f + 0.75f * hourly.ratio)
                                },
                            ),
                    )
                }
            }
            // 0/6/12/18/23시 눈금만 표시해 시간대 위치를 가늠하게 한다.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                listOf(0, 6, 12, 18, 23).forEach { hour ->
                    Text(
                        text = "$hour",
                        style = AppTypography.labelMedium.copy(
                            color = AppColor.onSurface.copy(alpha = 0.4f),
                        ),
                    )
                }
            }
        }
    }
}

// endregion

// region Weekly consistency chart --------------------------------------------

@Composable
private fun WeeklyConsistencySection(
    modifier: Modifier = Modifier,
    stats: List<DayOfWeekStat>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_weekly_consistency),
            description = stringResource(id = R.string.stat_weekly_consistency_desc),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appCardSurface()
                .padding(Dimens.contentPadding)
                .height(160.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemSpacing),
        ) {
            stats.forEach { stat ->
                DayOfWeekBar(
                    modifier = Modifier.weight(1f),
                    stat = stat,
                )
            }
        }
    }
}

@Composable
private fun DayOfWeekBar(
    modifier: Modifier = Modifier,
    stat: DayOfWeekStat,
) {
    val dayName = stringResource(id = DAYS_WITH_3CHARS[stat.dayOfWeek.value - 1])
    VerticalBar(
        modifier = modifier,
        ratio = stat.ratio,
        topLabel = "${stat.completedCount}",
        topLabelAlpha = if (stat.completedCount > 0) 0.8f else 0.3f,
        bottomLabel = dayName,
        barColor = AppColor.primary.copy(alpha = 0.35f + 0.65f * stat.ratio),
        description = stringResource(id = R.string.stat_cd_weekly_bar, dayName, stat.completedCount),
    )
}

// endregion

// region Monthly invested time chart -----------------------------------------

@Composable
private fun MonthlyInvestedSection(
    modifier: Modifier = Modifier,
    monthlyInvestedTimes: List<MonthlyInvestedTime>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_monthly_invested),
            description = stringResource(id = R.string.stat_monthly_invested_desc),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .appCardSurface()
                .padding(Dimens.contentPadding)
                .height(160.dp),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(Dimens.itemSpacing),
        ) {
            monthlyInvestedTimes.forEach { monthly ->
                MonthlyInvestedBar(
                    modifier = Modifier.weight(1f),
                    monthly = monthly,
                )
            }
        }
    }
}

@Composable
private fun MonthlyInvestedBar(
    modifier: Modifier = Modifier,
    monthly: MonthlyInvestedTime,
) {
    // 막대 위 라벨은 요일 차트와 동일하게 시간(hour) 단위 숫자만 간결하게 표기한다.
    val hours = (monthly.investedTimeMs / MS_1HOUR).toInt()
    val monthName = stringResource(id = ABB_MONTHS[monthly.yearMonth.monthValue - 1])
    VerticalBar(
        modifier = modifier,
        ratio = monthly.ratio,
        topLabel = "$hours",
        topLabelAlpha = if (hours > 0) 0.8f else 0.3f,
        bottomLabel = monthName,
        barColor = AppColor.primary.copy(alpha = 0.35f + 0.65f * monthly.ratio),
        description = stringResource(id = R.string.stat_cd_month_bar, monthName, hours),
    )
}

// endregion

// region Loop ranking --------------------------------------------------------

private fun LazyListScope.rankingSection(
    ranking: List<LoopWithStatistics>,
    sortOrder: RankingSortOrder,
    onSortSelected: (RankingSortOrder) -> Unit,
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    if (ranking.isEmpty()) return

    // 헤더와 순위 목록을 하나의 섹션 아이템으로 묶는다.
    // (개별 행을 LazyColumn 아이템으로 두면 섹션 간격이 행 사이에도 적용돼 여백이 과하게 벌어진다.)
    // 행 사이 간격은 카드 간격(cardSpacing)만 사용해 촘촘하게 유지한다.
    // 기본은 상위 N개만 접어 두고, 나머지는 '전체 보기'로 펼친다(긴 목록을 한 번에 렌더하지 않기 위함).
    fadingItem(key = "ranking") {
        Column(modifier = Modifier.fillMaxWidth()) {
            SectionHeader(
                title = stringResource(id = R.string.stat_ranking),
                description = stringResource(id = R.string.stat_ranking_desc),
            )
            RankingSortSelector(
                modifier = Modifier.padding(bottom = Dimens.cardSpacing),
                selectedSortOrder = sortOrder,
                onSortSelected = onSortSelected,
            )
            val visible = if (expanded) ranking else ranking.take(RANKING_COLLAPSED_COUNT)
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
                visible.forEachIndexed { index, item ->
                    LoopRankingItem(
                        order = index + 1,
                        item = item,
                        sortOrder = sortOrder,
                        onClick = { onNavigateToDetailPage(item.loopId) },
                    )
                }
            }
            if (ranking.size > RANKING_COLLAPSED_COUNT) {
                RankingExpandToggle(
                    modifier = Modifier.padding(top = Dimens.cardSpacing),
                    expanded = expanded,
                    totalCount = ranking.size,
                    onToggle = onToggleExpanded,
                )
            }
        }
    }
}

/** 접힌 순위를 펼치거나 다시 접는 버튼. */
@Composable
private fun RankingExpandToggle(
    modifier: Modifier = Modifier,
    expanded: Boolean,
    totalCount: Int,
    onToggle: () -> Unit,
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .clickable(onClick = onToggle)
            .padding(vertical = 12.dp),
        text = if (expanded) {
            stringResource(id = R.string.stat_ranking_show_less)
        } else {
            stringResource(id = R.string.stat_ranking_show_all, totalCount)
        },
        textAlign = TextAlign.Center,
        style = AppTypography.bodyMedium.copy(color = AppColor.primary, fontWeight = FontWeight.Bold),
    )
}

/**
 * 순위 정렬 기준(완료율/누적시간/완료횟수)을 고르는 세그먼트 컨트롤.
 * 상단 기간 선택기와 모양이 비슷하므로, 앞에 '정렬' 라벨을 붙여 무엇을 제어하는지 구분되게 한다.
 */
@Composable
private fun RankingSortSelector(
    modifier: Modifier = Modifier,
    selectedSortOrder: RankingSortOrder,
    onSortSelected: (RankingSortOrder) -> Unit,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Dimens.itemSpacing)) {
        Text(
            text = stringResource(R.string.stat_ranking_sort_label),
            style = AppTypography.bodySmall,
            color = AppColor.onSurfaceVariant,
        )
        AppSegmentedControl(
            options = RankingSortOrder.entries,
            selected = selectedSortOrder,
            onSelected = onSortSelected,
            label = { stringResource(it.titleRes) },
        )
    }
}

@Composable
private fun LoopRankingItem(
    modifier: Modifier = Modifier,
    order: Int,
    item: LoopWithStatistics,
    sortOrder: RankingSortOrder,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .appCardSurface()
            .clickable(onClick = onClick)
            .padding(horizontal = Dimens.contentPadding, vertical = Dimens.contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            modifier = Modifier.size(24.dp),
            text = "$order",
            textAlign = TextAlign.Center,
            style = AppTypography.titleMedium.copy(
                color = if (order <= 3) AppColor.primary else AppColor.onSurface.copy(alpha = 0.5f),
                fontWeight = if (order <= 3) FontWeight.Bold else FontWeight.Normal,
            ),
        )
        Box(
            modifier = Modifier
                .padding(start = Dimens.contentPadding)
                .size(10.dp)
                .background(
                    color = item.color.compositeOverOnSurface(),
                    shape = CircleShape,
                ),
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = item.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodyLarge.copy(color = AppColor.onSurface),
            )
            DoneRateBar(
                modifier = Modifier.padding(top = 8.dp),
                ratio = item.doneRate,
            )
        }
        // 정렬 기준에 맞춰 강조 값을 다르게 보여준다. (완료율=%, 누적시간=기간, 완료횟수=회)
        val trailingText = when (sortOrder) {
            RankingSortOrder.COMPLETION_RATE -> "${(item.doneRate * 100).toInt()}%"
            RankingSortOrder.INVESTED_TIME -> investedDurationText(investedTimeMs = item.investedTimeMs)
            RankingSortOrder.DONE_COUNT -> stringResource(id = R.string.stat_unit_count, item.doneCount)
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = trailingText,
            style = AppTypography.titleMedium.copy(
                color = AppColor.primary.copy(alpha = 0.4f + 0.6f * item.doneRate),
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

// endregion

// region Plan vs actual (⑤) --------------------------------------------------

@Composable
private fun PlanVsActualSection(
    modifier: Modifier = Modifier,
    stat: PlanVsActualStat,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_plan_title),
            description = stringResource(id = R.string.stat_plan_desc),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appCardSurface()
                .padding(Dimens.contentPadding),
        ) {
            val diffMinutes = (stat.avgStartDiffMs / MS_1MIN).toInt()
            val headline = when {
                diffMinutes > 0 -> stringResource(id = R.string.stat_plan_late, diffMinutes)
                diffMinutes < 0 -> stringResource(id = R.string.stat_plan_early, -diffMinutes)
                else -> stringResource(id = R.string.stat_plan_ontime)
            }
            Text(
                text = headline,
                style = AppTypography.titleMedium.copy(
                    color = if (diffMinutes > 0) AppColor.warning else AppColor.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = stringResource(id = R.string.stat_plan_ontime_rate, (stat.onTimeRate * 100).toInt()),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
            )
            DoneRateBar(
                modifier = Modifier.padding(top = 8.dp),
                ratio = stat.onTimeRate,
            )
        }
    }
}

// endregion

// region Habit health (⑥) ----------------------------------------------------

@Composable
private fun HabitHealthSection(
    modifier: Modifier = Modifier,
    items: List<HabitHealth>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_health_title),
            description = stringResource(id = R.string.stat_health_desc),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
            items.forEach { health ->
                HabitHealthItem(health = health)
            }
        }
    }
}

@Composable
private fun HabitHealthItem(
    modifier: Modifier = Modifier,
    health: HabitHealth,
) {
    val levelColor = if (health.level == HabitHealthLevel.AT_RISK) AppColor.error else AppColor.warning
    Row(
        modifier = modifier
            .fillMaxWidth()
            .appCardSurface()
            .padding(horizontal = Dimens.contentPadding, vertical = Dimens.contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = health.color.compositeOverOnSurface(), shape = CircleShape),
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = health.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodyLarge.copy(color = AppColor.onSurface),
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = stringResource(
                    id = R.string.stat_health_change,
                    (health.previousRate * 100).toInt(),
                    (health.recentRate * 100).toInt(),
                ),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
            )
        }
        LevelChip(
            text = stringResource(
                id = if (health.level == HabitHealthLevel.AT_RISK) {
                    R.string.stat_health_at_risk
                } else {
                    R.string.stat_health_watch
                },
            ),
            color = levelColor,
        )
    }
}

// endregion

// region New loop settling (⑦) -----------------------------------------------

@Composable
private fun NewLoopSettlingSection(
    modifier: Modifier = Modifier,
    items: List<NewLoopSettling>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_settling_title),
            description = stringResource(id = R.string.stat_settling_desc),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
            items.forEach { settling ->
                NewLoopSettlingItem(settling = settling)
            }
        }
    }
}

@Composable
private fun NewLoopSettlingItem(
    modifier: Modifier = Modifier,
    settling: NewLoopSettling,
) {
    val (levelColor, levelRes) = when (settling.level) {
        SettlingLevel.SETTLED -> AppColor.primary to R.string.stat_settling_settled
        SettlingLevel.SETTLING -> AppColor.warning to R.string.stat_settling_settling
        SettlingLevel.STRUGGLING -> AppColor.error to R.string.stat_settling_struggling
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .appCardSurface()
            .padding(horizontal = Dimens.contentPadding, vertical = Dimens.contentPadding),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color = settling.color.compositeOverOnSurface(), shape = CircleShape),
        )
        Column(
            modifier = Modifier
                .padding(start = 12.dp)
                .weight(1f),
        ) {
            Text(
                text = settling.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodyLarge.copy(color = AppColor.onSurface),
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = stringResource(id = R.string.stat_settling_days, settling.daysSinceCreated),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
            )
        }
        Text(
            modifier = Modifier.padding(end = 12.dp),
            text = "${(settling.doneRate * 100).toInt()}%",
            style = AppTypography.titleMedium.copy(color = levelColor, fontWeight = FontWeight.Bold),
        )
        LevelChip(text = stringResource(id = levelRes), color = levelColor)
    }
}

// endregion

// region Streak --------------------------------------------------------------

/**
 * 현재/최장 연속 달성 스트릭을 두 개의 KPI 카드로 보여주는 섹션.
 * 기간 선택과 무관한 전체 기록 기준이다.
 */
@Composable
private fun StreakSection(
    modifier: Modifier = Modifier,
    streak: StreakStat,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_streak),
            description = stringResource(id = R.string.stat_streak_desc),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
            StatCard(
                modifier = Modifier.weight(1f),
                value = stringResource(id = R.string.stat_streak_days, streak.current),
                label = stringResource(id = R.string.stat_streak_current),
                accent = true,
            )
            StatCard(
                modifier = Modifier.weight(1f),
                value = stringResource(id = R.string.stat_streak_days, streak.longest),
                label = stringResource(id = R.string.stat_streak_longest),
            )
        }
    }
}

// endregion

// region Milestones (⑨) ------------------------------------------------------

@Composable
private fun MilestonesSection(
    modifier: Modifier = Modifier,
    milestones: List<Milestone>,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_milestone_title),
            description = stringResource(id = R.string.stat_milestone_desc),
        )
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
            milestones.forEach { milestone ->
                MilestoneItem(milestone = milestone)
            }
        }
    }
}

@Composable
private fun MilestoneItem(
    modifier: Modifier = Modifier,
    milestone: Milestone,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .appCardSurface()
            .padding(horizontal = Dimens.contentPadding, vertical = Dimens.contentPadding),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(id = milestone.type.labelRes),
                style = AppTypography.bodyLarge.copy(color = AppColor.onSurface),
            )
            Text(
                text = milestoneValueText(type = milestone.type, value = milestone.value),
                style = AppTypography.titleMedium.copy(
                    color = AppColor.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        DoneRateBar(
            modifier = Modifier.padding(top = 8.dp),
            ratio = milestone.progress,
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = milestone.next?.let {
                stringResource(id = R.string.stat_milestone_next, milestoneValueText(milestone.type, it))
            } ?: stringResource(id = R.string.stat_milestone_max),
            style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.5f)),
        )
    }
}

/** 마일스톤 종류에 맞는 단위로 값을 문자열화한다(시간/횟수/일). */
@Composable
private fun milestoneValueText(type: MilestoneType, value: Long): String = when (type) {
    MilestoneType.INVESTED_HOURS -> stringResource(id = R.string.stat_milestone_unit_hours, value.toInt())
    MilestoneType.TOTAL_DONE -> stringResource(id = R.string.stat_milestone_unit_count, value.toInt())
    MilestoneType.LONGEST_STREAK -> stringResource(id = R.string.stat_milestone_unit_days, value.toInt())
}

// endregion

// region Retrospect (⑧) ------------------------------------------------------

@Composable
private fun RetrospectSection(
    modifier: Modifier = Modifier,
    stat: RetrospectStat,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        SectionHeader(
            title = stringResource(id = R.string.stat_retrospect_title),
            description = stringResource(id = R.string.stat_retrospect_desc),
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .appCardSurface()
                .padding(Dimens.contentPadding),
        ) {
            Text(
                text = "${(stat.rate * 100).toInt()}%",
                style = AppTypography.headlineMedium.copy(
                    color = AppColor.primary,
                    fontWeight = FontWeight.Bold,
                ),
            )
            Text(
                modifier = Modifier.padding(top = Dimens.itemSpacing),
                text = stringResource(id = R.string.stat_retrospect_ratio, stat.writtenCount, stat.doneCount),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
            )
            DoneRateBar(
                modifier = Modifier.padding(top = 8.dp),
                ratio = stat.rate,
            )
        }
    }
}

// endregion

// region Shared --------------------------------------------------------------

/**
 * 세로 막대 하나(위 라벨 · 자라는 막대 · 아래 라벨). 완료율/요일/월별 차트가 공유한다.
 *
 * [animateFloatAsState]는 최초 표시나 스크롤 재진입 때는 목표값에서 시작해 튀지 않고,
 * 값이 바뀔 때(예: 기간 전환)에만 부드럽게 자란다.
 * 스크린리더에는 [contentDescription] 한 줄로 묶어 읽어 준다(막대 자체는 의미를 못 읽으므로).
 */
@Composable
private fun VerticalBar(
    modifier: Modifier = Modifier,
    ratio: Float,
    topLabel: String,
    topLabelAlpha: Float,
    bottomLabel: String,
    barColor: Color,
    description: String,
) {
    val animatedRatio by animateFloatAsState(targetValue = ratio, label = "verticalBar")
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clearAndSetSemantics { contentDescription = description },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text = topLabel,
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = topLabelAlpha),
            ),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.BottomCenter,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(fraction = animatedRatio.coerceAtLeast(0.02f))
                    .clip(RoundedCornerShape(6.dp))
                    .background(color = barColor),
            )
        }
        Text(
            modifier = Modifier.padding(top = Dimens.itemSpacing),
            text = bottomLabel,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )
    }
}

/** 상태/등급을 나타내는 작은 알약 모양 칩. */
@Composable
private fun LevelChip(
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
) {
    Text(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(color = color.copy(alpha = 0.15f))
            .padding(horizontal = 8.dp, vertical = 4.dp),
        text = text,
        style = AppTypography.labelMedium.copy(color = color, fontWeight = FontWeight.Bold),
    )
}

@Composable
private fun DoneRateBar(
    modifier: Modifier = Modifier,
    ratio: Float,
    color: Color = AppColor.primary,
) {
    val animatedRate by animateFloatAsState(
        targetValue = ratio.coerceIn(0f, 1f),
        label = "doneRateBar",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(color = AppColor.onSurface.copy(alpha = 0.08f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = animatedRate)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(color = color),
        )
    }
}

@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    description: String,
) {
    Column(modifier = modifier.padding(bottom = Dimens.contentPadding)) {
        Text(
            text = title,
            style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = description,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurfaceVariant,
            ),
        )
    }
}

/**
 * 투자 시간(ms)을 사람이 읽기 좋은 문자열로 변환한다.
 * 하루 이상이면 "N일 N시간", 한 시간 이상이면 "N시간 N분", 그 미만이면 "N분"으로 표기한다.
 *
 * 기록 화면의 월 요약 배너도 같은 표기를 쓰도록 이 함수를 공유한다(같은 값이 두 화면에서 다르게
 * 보이지 않게 한다).
 */
@Composable
fun investedDurationText(investedTimeMs: Long): String {
    val totalMinutes = investedTimeMs / MS_1MIN
    val days = totalMinutes / (60 * 24)
    val hours = (totalMinutes / 60) % 24
    val minutes = totalMinutes % 60
    return when {
        days > 0 -> stringResource(id = R.string.stat_duration_dh, days.toInt(), hours.toInt())
        hours > 0 -> stringResource(id = R.string.stat_duration_hm, hours.toInt(), minutes.toInt())
        else -> stringResource(id = R.string.stat_duration_m, minutes.toInt())
    }
}

// endregion
