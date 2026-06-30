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
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.pnd.android.loop.util.DAYS_WITH_3CHARS

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

        item(key = "weekly") {
            WeeklyConsistencySection(stats = uiState.dayOfWeekStats)
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

// region Loop ranking --------------------------------------------------------

private fun androidx.compose.foundation.lazy.LazyListScope.rankingSection(
    ranking: List<LoopWithStatistics>,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    if (ranking.isEmpty()) return

    item(key = "ranking_header") {
        SectionHeader(
            title = stringResource(id = R.string.stat_ranking),
            description = stringResource(id = R.string.stat_ranking_desc),
        )
    }

    itemsIndexed(
        items = ranking,
        key = { _, item -> item.loopId },
    ) { index, item ->
        LoopRankingItem(
            modifier = Modifier.padding(top = if (index == 0) 0.dp else Dimens.cardSpacing),
            order = index + 1,
            item = item,
            onClick = { onNavigateToDetailPage(item.loopId) },
        )
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
