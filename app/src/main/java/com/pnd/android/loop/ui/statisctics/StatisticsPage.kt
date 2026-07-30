package com.pnd.android.loop.ui.statisctics

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
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
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.pnd.android.loop.ui.common.AppEmptyState
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.warning
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.MS_1HOUR
import com.pnd.android.loop.util.MS_1MIN
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 카드 모서리는 다른 화면(홈/기록/상세)과 동일하게 RoundShapes.large(12dp)로 통일한다.
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
    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColor.background),
        containerColor = AppColor.background,
        topBar = {
            SimpleAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = stringResource(id = R.string.statistics),
                onNavigateUp = onNavigateUp,
            )
        },
    ) { contentPadding ->
        StatisticsPageContent(
            modifier = Modifier
                .padding(contentPadding)
                .fillMaxSize(),
            statisticsViewModel = statisticsViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

@Composable
private fun StatisticsPageContent(
    modifier: Modifier = Modifier,
    statisticsViewModel: StatisticsViewModel,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    // 화면 회전·프로세스 사망에도 사용자의 선택이 유지되도록 rememberSaveable을 쓴다.
    var selectedTab by rememberSaveable { mutableStateOf(StatisticsTab.SUMMARY) }
    var selectedPeriod by rememberSaveable { mutableStateOf(StatisticsPeriod.TOTAL) }
    var rankingSortOrder by rememberSaveable { mutableStateOf(RankingSortOrder.COMPLETION_RATE) }
    var rankingExpanded by rememberSaveable { mutableStateOf(false) }

    // 기간에 따라 달라지는 지표들.
    val periodStats = rememberLoadable(selectedPeriod) { statisticsViewModel.flowPeriodStats(selectedPeriod) }
    val ranking = rememberLoadable(selectedPeriod) { statisticsViewModel.flowLoopRanking(selectedPeriod) }

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

    // 선택된 정렬 기준으로 순위를 내림차순 정렬한다. (정렬은 목록이 작아 클라이언트에서 처리한다.)
    val sortedRanking = remember(rankingValue, rankingSortOrder) {
        rankingValue.sortedByDescending { rankingSortOrder.selector(it) }
    }

    // 탭마다 독립된 스크롤 상태를 둔다. 탭을 바꾸면 새 상태가 생겨 항상 맨 위에서 시작하므로,
    // 다른 탭에서 스크롤한 위치에 어정쩡하게 놓이는 일이 없다.
    val listState = remember(selectedTab) { LazyListState() }

    LazyColumn(
        modifier = modifier,
        state = listState,
        contentPadding = PaddingValues(
            start = Dimens.screenHorizontalPadding,
            end = Dimens.screenHorizontalPadding,
            top = Dimens.contentPadding,
            bottom = Dimens.sectionSpacing,
        ),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing),
    ) {
        item(key = "tabs") {
            StatisticsTabRow(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it },
            )
        }

        // 기간에 반응하는 탭에서만 기간 선택기를 노출한다. 그 외 탭에는 스코프를 알리는 안내 문구를 둔다.
        item(key = "scope") {
            if (selectedTab.usesPeriod) {
                StatisticsPeriodSelector(
                    selectedPeriod = selectedPeriod,
                    onPeriodSelected = { selectedPeriod = it },
                )
            } else {
                StatisticsScopeCaption()
            }
        }

        when (selectedTab) {
            StatisticsTab.SUMMARY -> statefulTab(
                isLoading = periodStats.isLoading || ranking.isLoading,
                isEmpty = statsValue.isEmpty && sortedRanking.isEmpty(),
                isOverall = false,
            ) {
                summaryContent(
                    stats = statsValue,
                    ranking = sortedRanking,
                    sortOrder = rankingSortOrder,
                    onSortSelected = { rankingSortOrder = it },
                    rankingExpanded = rankingExpanded,
                    onToggleRanking = { rankingExpanded = !rankingExpanded },
                    onNavigateToDetailPage = onNavigateToDetailPage,
                )
            }

            StatisticsTab.PATTERN -> statefulTab(
                isLoading = periodStats.isLoading,
                isEmpty = statsValue.isEmpty,
                isOverall = false,
            ) {
                patternContent(stats = statsValue)
            }

            StatisticsTab.TREND -> statefulTab(
                isLoading = completionTrend.isLoading || monthlyInvestedTimes.isLoading,
                isEmpty = trendValue.size < 2 && monthlyValue.isEmpty(),
                isOverall = true,
            ) {
                trendContent(completionTrend = trendValue, monthlyInvestedTimes = monthlyValue)
            }

            StatisticsTab.ACHIEVEMENT -> statefulTab(
                isLoading = projection.isLoading || habitHealth.isLoading || milestones.isLoading ||
                    streak.isLoading || newLoopSettling.isLoading,
                isEmpty = !hasInsights(projectionValue, habitHealthValue, milestonesValue) &&
                    streakValue.longest == 0 && milestonesValue.isEmpty() &&
                    habitHealthValue.isEmpty() && settlingValue.isEmpty(),
                isOverall = true,
            ) {
                achievementContent(
                    projection = projectionValue,
                    streak = streakValue,
                    milestones = milestonesValue,
                    habitHealth = habitHealthValue,
                    newLoopSettling = settlingValue,
                )
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
    return loadableFlow.collectAsState(initial = Loadable.Loading).value
}

// region Tabs & selectors ----------------------------------------------------

@Composable
private fun StatisticsTabRow(
    modifier: Modifier = Modifier,
    selectedTab: StatisticsTab,
    onTabSelected: (StatisticsTab) -> Unit,
) {
    Row(modifier = modifier.fillMaxWidth()) {
        StatisticsTab.entries.forEach { tab ->
            val isSelected = tab == selectedTab
            // 선택 전환이 뚝 끊기지 않도록 색과 밑줄 길이를 부드럽게 애니메이션한다.
            val textColor by animateColorAsState(
                targetValue = if (isSelected) AppColor.primary else AppColor.onSurface.copy(alpha = 0.6f),
                label = "tabTextColor",
            )
            val indicatorWidth by animateDpAsState(
                targetValue = if (isSelected) 20.dp else 0.dp,
                label = "tabIndicatorWidth",
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundShapes.medium)
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = tab.titleRes),
                    style = AppTypography.bodyMedium.copy(
                        color = textColor,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                // 선택된 탭 아래에만 강조 밑줄을 둔다. (폭이 0이면 그려지지 않는다.)
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(indicatorWidth)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(color = AppColor.primary),
                )
            }
        }
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

@Composable
private fun StatisticsPeriodSelector(
    modifier: Modifier = Modifier,
    selectedPeriod: StatisticsPeriod,
    onPeriodSelected: (StatisticsPeriod) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatisticsPeriod.entries.forEach { period ->
            PeriodSegment(
                modifier = Modifier.weight(1f),
                text = stringResource(id = period.titleRes()),
                isSelected = period == selectedPeriod,
                onClick = { onPeriodSelected(period) },
            )
        }
    }
}

@Composable
private fun PeriodSegment(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    Text(
        modifier = modifier
            .clip(RoundShapes.large)
            .background(color = if (isSelected) AppColor.primary else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        text = text,
        textAlign = TextAlign.Center,
        style = AppTypography.bodyMedium.copy(
            color = if (isSelected) AppColor.onPrimary else AppColor.onSurface.copy(alpha = 0.6f),
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
        ),
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
            item(key = "skeleton_header") { SkeletonCard(height = 44.dp) }
            item(key = "skeleton_1") { SkeletonCard(height = 120.dp) }
            item(key = "skeleton_2") { SkeletonCard(height = 120.dp) }
        }

        isEmpty -> item(key = "empty") { EmptyHint(isOverall = isOverall) }

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
    item(key = "summary") {
        SummarySection(
            summary = stats.summary,
            perfectDays = stats.perfectDays,
            skipCount = stats.skipCount,
        )
    }

    if (stats.planVsActual.hasData) {
        item(key = "planVsActual") {
            PlanVsActualSection(stat = stats.planVsActual)
        }
    }

    if (stats.retrospect.hasData) {
        item(key = "retrospect") {
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
    item(key = "hourly") {
        HourlyHeatmapSection(hourlyStats = stats.hourlyStats)
    }

    item(key = "weekly") {
        WeeklyConsistencySection(stats = stats.dayOfWeekStats)
    }
}

/** 추세 탭(전체 기준): 완료율 추세 + 월별 투자 시간. */
private fun LazyListScope.trendContent(
    completionTrend: List<CompletionRatePoint>,
    monthlyInvestedTimes: List<MonthlyInvestedTime>,
) {
    if (completionTrend.size >= 2) {
        item(key = "trend") {
            CompletionTrendSection(points = completionTrend)
        }
    }

    if (monthlyInvestedTimes.isNotEmpty()) {
        item(key = "monthly") {
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
        item(key = "insight") {
            InsightFeedSection(
                projection = projection,
                habitHealth = habitHealth,
                milestones = milestones,
            )
        }
    }

    if (streak.longest > 0) {
        item(key = "streak") {
            StreakSection(streak = streak)
        }
    }

    if (milestones.isNotEmpty()) {
        item(key = "milestones") {
            MilestonesSection(milestones = milestones)
        }
    }

    if (habitHealth.isNotEmpty()) {
        item(key = "health") {
            HabitHealthSection(items = habitHealth)
        }
    }

    if (newLoopSettling.isNotEmpty()) {
        item(key = "settling") {
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color = accent.copy(alpha = 0.1f))
            .padding(horizontal = Dimens.contentPadding, vertical = Dimens.contentPadding),
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
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = Dimens.contentPadding, vertical = 20.dp),
    ) {
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
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color = AppColor.primary.copy(alpha = 0.1f))
            .padding(horizontal = Dimens.contentPadding, vertical = 20.dp),
    ) {
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
                .clip(CardShape)
                .background(color = AppColor.surfaceContainer)
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
                .clip(CardShape)
                .background(color = AppColor.surfaceContainer)
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
                .clip(CardShape)
                .background(color = AppColor.surfaceContainer)
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
                .clip(CardShape)
                .background(color = AppColor.surfaceContainer)
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
    item(key = "ranking") {
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
            .padding(4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            modifier = Modifier.padding(start = 8.dp, end = 4.dp),
            text = stringResource(id = R.string.stat_ranking_sort_label),
            style = AppTypography.labelMedium.copy(color = AppColor.onSurface.copy(alpha = 0.5f)),
        )
        RankingSortOrder.entries.forEach { sortOrder ->
            PeriodSegment(
                modifier = Modifier.weight(1f),
                text = stringResource(id = sortOrder.titleRes),
                isSelected = sortOrder == selectedSortOrder,
                onClick = { onSortSelected(sortOrder) },
            )
        }
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
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
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
                .clip(CardShape)
                .background(color = AppColor.surfaceContainer)
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
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
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
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
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
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
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
                .clip(CardShape)
                .background(color = AppColor.surfaceContainer)
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
            style = AppTypography.titleLarge.copy(color = AppColor.onSurface),
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = description,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
    }
}

/**
 * 투자 시간(ms)을 사람이 읽기 좋은 문자열로 변환한다.
 * 하루 이상이면 "N일 N시간", 한 시간 이상이면 "N시간 N분", 그 미만이면 "N분"으로 표기한다.
 */
@Composable
private fun investedDurationText(investedTimeMs: Long): String {
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
