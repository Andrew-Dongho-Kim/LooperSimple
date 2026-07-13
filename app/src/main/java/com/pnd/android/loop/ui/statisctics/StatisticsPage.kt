package com.pnd.android.loop.ui.statisctics

import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopWithStatistics
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.Yellow800
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.MS_1HOUR
import com.pnd.android.loop.util.MS_1MIN

private val CardShape = RoundedCornerShape(16.dp)

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
    var selectedTab by remember { mutableStateOf(StatisticsTab.SUMMARY) }
    var selectedPeriod by remember { mutableStateOf(StatisticsPeriod.TOTAL) }
    var rankingSortOrder by remember { mutableStateOf(RankingSortOrder.COMPLETION_RATE) }

    // 기간에 따라 달라지는 지표들.
    val periodStats by remember(selectedPeriod) {
        statisticsViewModel.flowPeriodStats(selectedPeriod)
    }.collectAsState(initial = PeriodStats())

    val ranking by remember(selectedPeriod) {
        statisticsViewModel.flowLoopRanking(selectedPeriod)
    }.collectAsState(initial = emptyList())

    // 기간과 무관하게 항상 전체(또는 최근) 흐름을 보는 지표들.
    val streak by remember {
        statisticsViewModel.flowStreak()
    }.collectAsState(initial = StreakStat(current = 0, longest = 0))

    val monthlyInvestedTimes by remember {
        statisticsViewModel.flowMonthlyInvestedTime()
    }.collectAsState(initial = emptyList())

    val completionTrend by remember {
        statisticsViewModel.flowCompletionTrend()
    }.collectAsState(initial = emptyList())

    val projection by remember {
        statisticsViewModel.flowMonthlyProjection()
    }.collectAsState(initial = MonthlyProjection.Empty)

    val habitHealth by remember {
        statisticsViewModel.flowHabitHealth()
    }.collectAsState(initial = emptyList())

    val newLoopSettling by remember {
        statisticsViewModel.flowNewLoopSettling()
    }.collectAsState(initial = emptyList())

    val milestones by remember {
        statisticsViewModel.flowMilestones()
    }.collectAsState(initial = emptyList())

    // 선택된 정렬 기준으로 순위를 내림차순 정렬한다. (정렬은 목록이 작아 클라이언트에서 처리한다.)
    val sortedRanking = remember(ranking, rankingSortOrder) {
        ranking.sortedByDescending { rankingSortOrder.selector(it) }
    }

    LazyColumn(
        modifier = modifier,
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

        item(key = "period") {
            StatisticsPeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it },
            )
        }

        when (selectedTab) {
            StatisticsTab.SUMMARY -> summaryTab(
                periodStats = periodStats,
                projection = projection,
                completionTrend = completionTrend,
                habitHealth = habitHealth,
                milestones = milestones,
                rankingEmpty = ranking.isEmpty(),
            )

            StatisticsTab.PATTERN -> patternTab(
                periodStats = periodStats,
                monthlyInvestedTimes = monthlyInvestedTimes,
            )

            StatisticsTab.HABIT -> habitTab(
                ranking = sortedRanking,
                sortOrder = rankingSortOrder,
                onSortSelected = { rankingSortOrder = it },
                planVsActual = periodStats.planVsActual,
                habitHealth = habitHealth,
                newLoopSettling = newLoopSettling,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )

            StatisticsTab.ACHIEVEMENT -> achievementTab(
                streak = streak,
                milestones = milestones,
                retrospect = periodStats.retrospect,
            )
        }
    }
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
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onTabSelected(tab) }
                    .padding(vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = stringResource(id = tab.titleRes),
                    style = AppTypography.bodyMedium.copy(
                        color = if (isSelected) AppColor.primary else AppColor.onSurface.copy(alpha = 0.6f),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                // 선택된 탭 아래에만 강조 밑줄을 둔다.
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .width(20.dp)
                        .height(2.dp)
                        .clip(CircleShape)
                        .background(color = if (isSelected) AppColor.primary else Color.Transparent),
                )
            }
        }
    }
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
            .clip(RoundedCornerShape(12.dp))
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

// region Tab content builders ------------------------------------------------

/** 요약 탭: 인사이트 피드 + 요약 KPI + 완료율 추세. */
private fun LazyListScope.summaryTab(
    periodStats: PeriodStats,
    projection: MonthlyProjection,
    completionTrend: List<CompletionRatePoint>,
    habitHealth: List<HabitHealth>,
    milestones: List<Milestone>,
    rankingEmpty: Boolean,
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

    item(key = "summary") {
        SummarySection(
            summary = periodStats.summary,
            perfectDays = periodStats.perfectDays,
            skipCount = periodStats.skipCount,
        )
    }

    if (completionTrend.size >= 2) {
        item(key = "trend") {
            CompletionTrendSection(points = completionTrend)
        }
    }

    if (periodStats.isEmpty && rankingEmpty) {
        item(key = "empty") { EmptyHint() }
    }
}

/** 패턴 탭: 시간대 히트맵 + 요일 꾸준함 + 월별 투자 시간. */
private fun LazyListScope.patternTab(
    periodStats: PeriodStats,
    monthlyInvestedTimes: List<MonthlyInvestedTime>,
) {
    item(key = "hourly") {
        HourlyHeatmapSection(hourlyStats = periodStats.hourlyStats)
    }

    item(key = "weekly") {
        WeeklyConsistencySection(stats = periodStats.dayOfWeekStats)
    }

    if (monthlyInvestedTimes.isNotEmpty()) {
        item(key = "monthly") {
            MonthlyInvestedSection(monthlyInvestedTimes = monthlyInvestedTimes)
        }
    }
}

/** 습관 탭: 루프 순위 + 계획대비 실제 + 습관 건강 + 신규 루프 정착. */
private fun LazyListScope.habitTab(
    ranking: List<LoopWithStatistics>,
    sortOrder: RankingSortOrder,
    onSortSelected: (RankingSortOrder) -> Unit,
    planVsActual: PlanVsActualStat,
    habitHealth: List<HabitHealth>,
    newLoopSettling: List<NewLoopSettling>,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    rankingSection(
        ranking = ranking,
        sortOrder = sortOrder,
        onSortSelected = onSortSelected,
        onNavigateToDetailPage = onNavigateToDetailPage,
    )

    if (planVsActual.hasData) {
        item(key = "planVsActual") {
            PlanVsActualSection(stat = planVsActual)
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

/** 성취 탭: 연속 달성 + 마일스톤 + 회고 작성. */
private fun LazyListScope.achievementTab(
    streak: StreakStat,
    milestones: List<Milestone>,
    retrospect: RetrospectStat,
) {
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

    if (retrospect.hasData) {
        item(key = "retrospect") {
            RetrospectSection(stat = retrospect)
        }
    }
}

// endregion

// region Insight feed (요약 상단) --------------------------------------------

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
                    accent = if (worst.level == HabitHealthLevel.AT_RISK) AppColor.error else Yellow800,
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
            .padding(horizontal = Dimens.contentPadding, vertical = 14.dp),
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
                valueColor = if (skipCount > 0) Yellow800 else null,
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
    val animatedRatio by animateFloatAsState(
        targetValue = point.rate,
        label = "completionRateBar",
    )
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text = "${(point.rate * 100).toInt()}",
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = if (point.rate > 0f) 0.8f else 0.3f),
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
                    .background(color = AppColor.primary.copy(alpha = 0.35f + 0.65f * point.rate)),
            )
        }
        Text(
            modifier = Modifier.padding(top = Dimens.itemSpacing),
            text = stringResource(id = ABB_MONTHS[point.yearMonth.monthValue - 1]),
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )
    }
}

// endregion

// region Hourly heatmap (①) --------------------------------------------------

@Composable
private fun HourlyHeatmapSection(
    modifier: Modifier = Modifier,
    hourlyStats: List<HourlyCompletion>,
) {
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
            val peak = hourlyStats.maxByOrNull { it.count }?.takeIf { it.count > 0 }
            Text(
                text = peak?.let { stringResource(id = R.string.stat_hourly_peak, it.hour) }
                    ?: stringResource(id = R.string.stat_hourly_none),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface.copy(alpha = if (peak != null) 0.8f else 0.5f),
                    fontWeight = if (peak != null) FontWeight.Bold else FontWeight.Normal,
                ),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = Dimens.contentPadding)
                    .height(40.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                hourlyStats.forEach { hourly ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(3.dp))
                            .background(
                                color = if (hourly.count == 0) {
                                    AppColor.onSurface.copy(alpha = 0.06f)
                                } else {
                                    AppColor.primary.copy(alpha = 0.25f + 0.75f * hourly.ratio)
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
    val animatedRatio by animateFloatAsState(
        targetValue = stat.ratio,
        label = "dayOfWeekBar",
    )
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text = "${stat.completedCount}",
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = if (stat.completedCount > 0) 0.8f else 0.3f),
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
                    .background(
                        color = AppColor.primary.copy(
                            alpha = 0.35f + 0.65f * stat.ratio,
                        ),
                    ),
            )
        }
        Text(
            modifier = Modifier.padding(top = Dimens.itemSpacing),
            text = stringResource(id = DAYS_WITH_3CHARS[stat.dayOfWeek.value - 1]),
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )
    }
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
    val animatedRatio by animateFloatAsState(
        targetValue = monthly.ratio,
        label = "monthlyInvestedBar",
    )
    // 막대 위 라벨은 요일 차트와 동일하게 시간(hour) 단위 숫자만 간결하게 표기한다.
    val hours = (monthly.investedTimeMs / MS_1HOUR).toInt()
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Bottom,
    ) {
        Text(
            modifier = Modifier.padding(bottom = 4.dp),
            text = "$hours",
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = if (hours > 0) 0.8f else 0.3f),
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
                    .background(
                        color = AppColor.primary.copy(
                            alpha = 0.35f + 0.65f * monthly.ratio,
                        ),
                    ),
            )
        }
        Text(
            modifier = Modifier.padding(top = Dimens.itemSpacing),
            text = stringResource(id = ABB_MONTHS[monthly.yearMonth.monthValue - 1]),
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )
    }
}

// endregion

// region Loop ranking --------------------------------------------------------

private fun LazyListScope.rankingSection(
    ranking: List<LoopWithStatistics>,
    sortOrder: RankingSortOrder,
    onSortSelected: (RankingSortOrder) -> Unit,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    if (ranking.isEmpty()) return

    // 헤더와 순위 목록을 하나의 섹션 아이템으로 묶는다.
    // (개별 행을 LazyColumn 아이템으로 두면 섹션 간격이 행 사이에도 적용돼 여백이 과하게 벌어진다.)
    // 행 사이 간격은 카드 간격(cardSpacing)만 사용해 촘촘하게 유지한다.
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
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
                ranking.forEachIndexed { index, item ->
                    LoopRankingItem(
                        order = index + 1,
                        item = item,
                        sortOrder = sortOrder,
                        onClick = { onNavigateToDetailPage(item.loopId) },
                    )
                }
            }
        }
    }
}

/**
 * 순위 정렬 기준(완료율/누적시간/완료횟수)을 고르는 세그먼트 컨트롤.
 * 상단 기간 선택기와 동일한 스타일을 사용해 일관성을 유지한다.
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
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
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
            .padding(horizontal = Dimens.contentPadding, vertical = 14.dp),
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
                    color = if (diffMinutes > 0) Yellow800 else AppColor.primary,
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
    val levelColor = if (health.level == HabitHealthLevel.AT_RISK) AppColor.error else Yellow800
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = Dimens.contentPadding, vertical = 14.dp),
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
        SettlingLevel.SETTLING -> Yellow800 to R.string.stat_settling_settling
        SettlingLevel.STRUGGLING -> AppColor.error to R.string.stat_settling_struggling
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(CardShape)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = Dimens.contentPadding, vertical = 14.dp),
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
            .padding(horizontal = Dimens.contentPadding, vertical = 14.dp),
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

/** 상태/등급을 나타내는 작은 알약 모양 칩. */
@Composable
private fun LevelChip(
    modifier: Modifier = Modifier,
    text: String,
    color: Color,
) {
    Text(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
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

@Composable
private fun EmptyHint(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 64.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = R.string.stat_empty),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
    }
}

// endregion
