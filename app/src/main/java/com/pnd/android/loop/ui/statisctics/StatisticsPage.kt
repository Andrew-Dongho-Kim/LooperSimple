package com.pnd.android.loop.ui.statisctics

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopWithStatistics
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import kotlin.math.max
import kotlin.math.min

@Composable
fun StatisticsPage(
    modifier: Modifier = Modifier,
    statisticsViewModel: StatisticsViewModel = hiltViewModel(),
    onNavigateToDetailPage: (Int) -> Unit,
    onNavigateUp: () -> Unit,
) {
    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.background),
        topBar = {
            SimpleAppBar(
                modifier = Modifier
                    .statusBarsPadding(),
                title = stringResource(id = R.string.statistics),
                onNavigateUp = onNavigateUp
            )
        }
    )
    { contentPadding ->
        StatisticsPageContent(
            modifier = Modifier
                .padding(contentPadding)
                .padding(horizontal = 24.dp)
                .fillMaxWidth()
                .fillMaxHeight(),
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
    Column(modifier = modifier) {
        LoopsOrderByDoneRate(
            modifier = Modifier.height(280.dp),
            statisticsViewModel = statisticsViewModel,
            onNavigateToDetailPage = onNavigateToDetailPage,
        )
    }
}

@Composable
private fun LoopsOrderByDoneRate(
    modifier: Modifier = Modifier,
    statisticsViewModel: StatisticsViewModel,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    var selectedTab by remember { mutableStateOf(Tab.Total) }

    val loopsWithStatistics by statisticsViewModel
        .flowLoopsWithStatistics(
            from = selectedTab.from(),
            to = selectedTab.to()
        )
        .collectAsState(initial = emptyList())


    Column(modifier = modifier) {
        LazyColumn(
            modifier = Modifier.weight(1f),
            state = rememberLazyListState(),
        ) {
            items(
                items = loopsWithStatistics,
                key = { item -> item.id }
            ) { item ->
                LoopItemWithDoneRate(
                    item = item,
                    onNavigateToDetailPage = onNavigateToDetailPage,
                )
            }
        }
        LoopsOrderTab(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .background(color = AppColor.surface)
                .padding(top = 24.dp),
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }

}

@Composable
private fun LoopItemWithDoneRate(
    modifier: Modifier = Modifier,
    item: LoopWithStatistics,
    onNavigateToDetailPage: (Int) -> Unit,
) {
    Row(
        modifier = modifier
            .clickable { onNavigateToDetailPage(item.id) }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(
                    color = item.color.compositeOverOnSurface(),
                    shape = CircleShape
                )
        )
        Text(
            modifier = Modifier
                .padding(start = 16.dp)
                .weight(1f),
            text = item.title,
            style = AppTypography.bodyLarge.copy(
                color = AppColor.onSurface
            )
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = String.format("%.2f%%", item.doneRate * 100f),
            style = AppTypography.labelMedium.copy(
                color = rememberDoneRateColor(doneRate = item.doneRate * 100),
            )
        )
    }
}

@Composable
private fun LoopsOrderTab(
    modifier: Modifier = Modifier,
    selectedTab: Tab,
    onTabSelected: (Tab) -> Unit,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val tabs = rememberTabs()
        tabs.forEach { tab ->
            LoopsOrderTabItem(
                text = stringResource(id = tab.text()),
                isSelected = tab == selectedTab,
                onSelected = { onTabSelected(tab) }
            )
        }
    }
}

@Composable
private fun LoopsOrderTabItem(
    modifier: Modifier = Modifier,
    text: String,
    isSelected: Boolean = false,
    onSelected: () -> Unit,
) {
    Text(
        modifier = modifier
            .clickable { onSelected() }
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.4f),
            )
            .background(
                color = if (isSelected) AppColor.surface.compositeOverOnSurface() else AppColor.surface,
            )
            .padding(all = 12.dp)
            .wrapContentWidth(Alignment.CenterHorizontally),
        text = text,
        style = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface
        )
    )
}

@Composable
private fun rememberDoneRateColor(
    doneRate: Float
): Color {
    val positiveColor = AppColor.primary
    return remember(doneRate) {
        val ratio = (DONE_RATE_GREAT - max(
            min(doneRate, DONE_RATE_GREAT),
            DONE_RATE_POOR
        )) / (DONE_RATE_GREAT - DONE_RATE_POOR)
        val alpha =
            MAX_DONE_RATE_COLOR_ALPHA - ratio * (MAX_DONE_RATE_COLOR_ALPHA - MIN_DONE_RATE_COLOR_ALPHA)

        positiveColor.copy(alpha = alpha)
    }
}

private const val DONE_RATE_GREAT = 85f
private const val DONE_RATE_POOR = 15f

private const val MAX_DONE_RATE_COLOR_ALPHA = 1f
private const val MIN_DONE_RATE_COLOR_ALPHA = 0.4f

@Composable
private fun rememberTabs() = remember {
    Tab.entries.toList()
}

private enum class Tab(
    @StringRes val text: () -> Int,
    val from: () -> Long,
    val to: () -> Long,
) {
    Total(
        text = { R.string.total },
        from = { 0L },
        to = { LocalDate.now().toMs() }
    ),
    THIS_MONTH(
        text = {
            ABB_MONTHS[LocalDate.now().month.value - 1]
        },
        from = {
            LocalDate.now()
                .withDayOfMonth(1)
                .toMs()
        },
        to = { LocalDate.now().toMs() }
    ),
    ONE_MONTH_AGO(
        text = {
            ABB_MONTHS[LocalDate.now().minusMonths(1).month.value - 1]
        },
        from = {
            LocalDate.now()
                .minusMonths(1)
                .withDayOfMonth(1)
                .toMs()
        },
        to = {
            val date = LocalDate.now()
                .minusMonths(1)
            date.withDayOfMonth(date.lengthOfMonth())
                .toMs()
        }
    ),
    TWO_MONTH_AGO(
        text = {
            ABB_MONTHS[LocalDate.now().minusMonths(2).month.value - 1]
        },
        from = {
            LocalDate.now()
                .minusMonths(2)
                .withDayOfMonth(1)
                .toMs()
        },
        to = {
            val date = LocalDate.now()
                .minusMonths(2)
            date.withDayOfMonth(date.lengthOfMonth())
                .toMs()
        }
    )
}