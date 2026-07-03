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
import androidx.compose.foundation.lazy.LazyColumn
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
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
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
    var selectedPeriod by remember { mutableStateOf(StatisticsPeriod.TOTAL) }

    val uiState by remember(selectedPeriod) {
        statisticsViewModel.flowStatistics(selectedPeriod)
    }.collectAsState(initial = StatisticsUiState())

    val ranking by remember(selectedPeriod) {
        statisticsViewModel.flowLoopRanking(selectedPeriod)
    }.collectAsState(initial = emptyList())

    // 연속 달성 스트릭과 월별 투자 시간은 기간 선택과 무관하게 항상 전체 기록을 기준으로 한다.
    val streak by remember {
        statisticsViewModel.flowStreak()
    }.collectAsState(initial = StreakStat(current = 0, longest = 0))

    val monthlyInvestedTimes by remember {
        statisticsViewModel.flowMonthlyInvestedTime()
    }.collectAsState(initial = emptyList())

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
        item(key = "period") {
            StatisticsPeriodSelector(
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { selectedPeriod = it },
            )
        }

        item(key = "summary") {
            SummarySection(summary = uiState.summary)
        }

        // 완료 기록이 한 번이라도 있을 때만 스트릭 섹션을 노출한다.
        if (streak.longest > 0) {
            item(key = "streak") {
                StreakSection(streak = streak)
            }
        }

        item(key = "weekly") {
            WeeklyConsistencySection(stats = uiState.dayOfWeekStats)
        }

        if (monthlyInvestedTimes.isNotEmpty()) {
            item(key = "monthly") {
                MonthlyInvestedSection(monthlyInvestedTimes = monthlyInvestedTimes)
            }
        }

        rankingSection(
            ranking = ranking,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )

        if (uiState.isEmpty && ranking.isEmpty()) {
            item(key = "empty") { EmptyHint() }
        }
    }
}

// region Period selector -----------------------------------------------------

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

// region Summary KPI cards ---------------------------------------------------

@Composable
private fun SummarySection(
    modifier: Modifier = Modifier,
    summary: StatisticsSummary,
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
                color = if (accent) AppColor.primary else AppColor.onSurface,
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

// region Streak --------------------------------------------------------------

/**
 * 현재/최장 연속 달성 스트릭을 두 개의 KPI 카드로 보여주는 섹션.
 * 기간 선택과 무관한 전체 기록 기준이므로 별도 헤더로 구분한다.
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

private fun androidx.compose.foundation.lazy.LazyListScope.rankingSection(
    ranking: List<LoopWithStatistics>,
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
            Column(verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing)) {
                ranking.forEachIndexed { index, item ->
                    LoopRankingItem(
                        order = index + 1,
                        item = item,
                        onClick = { onNavigateToDetailPage(item.loopId) },
                    )
                }
            }
        }
    }
}

@Composable
private fun LoopRankingItem(
    modifier: Modifier = Modifier,
    order: Int,
    item: LoopWithStatistics,
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
                doneRate = item.doneRate,
            )
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = "${(item.doneRate * 100).toInt()}%",
            style = AppTypography.titleMedium.copy(
                color = AppColor.primary.copy(alpha = 0.4f + 0.6f * item.doneRate),
                fontWeight = FontWeight.Bold,
            ),
        )
    }
}

@Composable
private fun DoneRateBar(
    modifier: Modifier = Modifier,
    doneRate: Float,
) {
    val animatedRate by animateFloatAsState(
        targetValue = doneRate.coerceIn(0f, 1f),
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
                .background(color = AppColor.primary),
        )
    }
}

// endregion

// region Shared --------------------------------------------------------------

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
