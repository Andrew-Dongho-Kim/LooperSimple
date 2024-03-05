package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.model.columnSeries
import com.patrykandpatrick.vico.core.model.lineSeries
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.TimeStat
import com.pnd.android.loop.data.timeStatAsFlow
import com.pnd.android.loop.ui.common.SimpleAd
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.common.chart.AdvancedLineChart
import com.pnd.android.loop.ui.common.chart.BarChart
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.annotatedString
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toLocalDateTime
import java.time.LocalDate

private val adId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-2341430172816266/5981213088"
}

private val paddingVerticalNormal = 14.dp
private val paddingVerticalLarge = 24.dp
private val paddingVerticalExtraLarge = 48.dp
private val paddingVerticalSuperExtraLarge = 60.dp


@Composable
fun DetailPage(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val loop by detailViewModel.loop.collectAsState(LoopBase.default())
    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.background),
        topBar = {
            SimpleAppBar(
                modifier = Modifier
                    .statusBarsPadding(),
                title = loop.title,
                onNavigateUp = onNavigateUp
            )
        }
    )
    { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            DetailPageContent(
                detailViewModel = detailViewModel,
                loop = loop,
            )
        }
    }
}

@Composable
private fun DetailPageContent(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = paddingVerticalNormal)
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState())
    ) {

        LoopStartAndEndTime(
            loop = loop
        )
        LoopCreatedDate(
            modifier = Modifier.padding(top = paddingVerticalNormal),
            created = loop.created
        )

        SimpleAd(
            modifier = Modifier.padding(top = paddingVerticalLarge),
            adId = adId
        )

        DoneHistoryGrid(
            modifier = Modifier
                .padding(top = paddingVerticalExtraLarge)
                .fillMaxWidth()
                .height(264.dp),
            detailViewModel = detailViewModel,
            loop = loop,
        )
        LoopResponseDoneSkipRate(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    top = paddingVerticalNormal,
                    bottom = paddingVerticalNormal,
                ),
            detailViewModel = detailViewModel,
        )

        DailyDoneRateChart(
            modifier = Modifier.padding(
                top = paddingVerticalSuperExtraLarge
            ),
            detailViewModel = detailViewModel,
        )

        MonthlyDoneRateChart(
            modifier = Modifier.padding(
                top = paddingVerticalSuperExtraLarge
            ),
            detailViewModel = detailViewModel,
        )

        WeeklyDoneRateChart(
            modifier = Modifier.padding(
                top = paddingVerticalSuperExtraLarge
            ),
            detailViewModel = detailViewModel,
        )
    }
}

@Composable
private fun LoopCreatedDate(
    modifier: Modifier = Modifier,
    created: Long,
) {
    val createdDate = remember(created) { created.toLocalDate() }
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier,
            text = stringResource(id = R.string.created_date),
            style = AppTypography.headlineSmall.copy(
                color = AppColor.onSurface
            )
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            text = createdDate.formatYearMonthDateDays(),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )

        Text(
            modifier = Modifier,
            text = stringResource(
                id = R.string.n_days,
                LocalDate.now().toEpochDay() - createdDate.toEpochDay() + 1
            ),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )
    }
}

@Composable
private fun LoopStartAndEndTime(
    modifier: Modifier = Modifier,
    loop: LoopBase
) {
    var timeStat by remember { mutableStateOf<TimeStat>(TimeStat.NotToday) }
    LaunchedEffect(loop) {
        loop.timeStatAsFlow().collect { timeStat = it }
    }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.start),
                style = AppTypography.headlineSmall.copy(
                    color = AppColor.onSurface
                )
            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = loop.loopStart.formatHourMinute(),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface
                )
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            Text(
                text = annotatedString(timeStat.asString(LocalContext.current, false)),
                style = AppTypography.headlineSmall.copy(
                    color = AppColor.onSurface
                )
            )
        }
        Row(
            modifier = Modifier
                .padding(top = paddingVerticalNormal)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(id = R.string.end),
                style = AppTypography.headlineSmall.copy(
                    color = AppColor.onSurface
                )
            )
            Text(
                modifier = Modifier.padding(start = 12.dp),
                text = loop.loopEnd.formatHourMinute(),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface
                )
            )
        }
    }
}

@Composable
private fun LoopResponseDoneSkipRate(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel
) {
    Row(modifier = modifier.horizontalScroll(state = rememberScrollState())) {
        val duration by detailViewModel.allCount.collectAsState(initial = 0)
        val responseCount by detailViewModel.responseCount.collectAsState(initial = 0)
        LoopRate(
            text = stringResource(id = R.string.response_rate),
            rate = (responseCount.toFloat() / duration) * 100,
            count = responseCount,
        )

        val doneCount by detailViewModel.doneCount.collectAsState(initial = 0)
        LoopRate(
            modifier = Modifier.padding(start = 24.dp),
            text = stringResource(id = R.string.done_rate),
            rate = (doneCount.toFloat() / duration) * 100,
            count = doneCount,
        )

        val skipCount by detailViewModel.skipCount.collectAsState(initial = 0)
        LoopRate(
            modifier = Modifier.padding(start = 24.dp),
            text = stringResource(id = R.string.skip_rate),
            rate = (skipCount.toFloat() / duration) * 100,
            count = skipCount,
        )
    }
}

@Composable
private fun LoopRate(
    modifier: Modifier = Modifier,
    text: String,
    rate: Float,
    count: Int
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            )
        )

        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = String.format("%.2f%% (%d)", rate, count),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )
    }
}

@Composable
private fun DailyDoneRateChart(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
) {
    Column(modifier = modifier) {
        val loop by detailViewModel.loop.collectAsState(initial = LoopBase.default())

        Text(
            modifier = Modifier.padding(bottom = paddingVerticalNormal),
            text = stringResource(id = R.string.daily_done_rate_chart),
            style = AppTypography.headlineSmall.copy(
                color = AppColor.onSurface
            )
        )

        AdvancedLineChart(
            modelProducer = rememberDailyDoneRateModel(
                loop = loop,
                detailViewModel = detailViewModel,
            ),
            chartColors = listOf(
                loop.color.compositeOverOnSurface(),
                AppColor.error.copy(alpha = 0.8f),
            ),
            titleStartAxis = stringResource(id = R.string.done_rate),
            bottomAxisValueFormatter = rememberDailyDonRateChartBottomAxisFormatter()
        )
    }
}

@Composable
private fun rememberDailyDoneRateModel(
    loop: LoopBase,
    detailViewModel: LoopDetailViewModel,
): CartesianChartModelProducer {

    val modelProducer = remember(loop) { CartesianChartModelProducer.build() }
    LaunchedEffect(key1 = loop) {
        val createdDate = loop.created.toLocalDateTime().toLocalDate()
        val now = LocalDate.now()

        var days = 0
        var date = now
        val x = mutableListOf<Int>()
        val y = mutableListOf<Float>()
        while (date.isAfter(createdDate)) {
            val doneCount = detailViewModel.doneCountBefore(loop.id, date)
            val allCount = detailViewModel.allCountBefore(loop.id, date)
            val rate = doneCount.toFloat() / allCount
            x.add(days++)
            y.add(rate * 100)

            date = date.minusDays(1)
        }

        modelProducer.tryRunTransaction {
            lineSeries {
                series(x = x, y = y)
            }
        }
    }
    return modelProducer
}

@Composable
private fun rememberDailyDonRateChartBottomAxisFormatter(): AxisValueFormatter<AxisPosition.Horizontal.Bottom> {
    val context = LocalContext.current
    return remember {
        AxisValueFormatter { value, _, _ ->
            val now = LocalDate.now()
            val date = now.minusDays(value.toLong())

            context.getString(
                R.string.format_month_date,
                context.getString(ABB_MONTHS[date.monthValue - 1]),
                "${date.dayOfMonth}"
            )
        }
    }
}

@Composable
private fun MonthlyDoneRateChart(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
) {
    Column(modifier = modifier) {
        val loop by detailViewModel.loop.collectAsState(initial = LoopBase.default())

        Text(
            modifier = Modifier.padding(bottom = paddingVerticalNormal),
            text = stringResource(id = R.string.monthly_done_rate_chart),
            style = AppTypography.headlineSmall.copy(
                color = AppColor.onSurface
            )
        )

        BarChart(
            modelProducer = rememberMonthlyDoneRate(
                loop = loop,
                detailViewModel = detailViewModel,
            ),
            barColor = loop.color.compositeOverOnSurface(),
            titleStartAxis = stringResource(id = R.string.done_rate),
            bottomAxisValueFormatter = rememberMonthlyDonRateChartBottomAxisFormatter()
        )
    }
}

@Composable
private fun rememberMonthlyDoneRate(
    loop: LoopBase,
    detailViewModel: LoopDetailViewModel,
): CartesianChartModelProducer {

    val modelProducer = remember(loop) { CartesianChartModelProducer.build() }
    LaunchedEffect(key1 = loop) {
        val createdDate = loop.created.toLocalDateTime().toLocalDate()
        val now = LocalDate.now()

        var months = 0
        var date = now
        val x = mutableListOf<Int>()
        val y = mutableListOf<Float>()
        while (date.isAfter(createdDate)) {
            val firstDateOfMonth = date.withDayOfMonth(1)

            val allCount = detailViewModel.allCountBetween(
                loopId = loop.id,
                from = firstDateOfMonth,
                to = date
            )
            val doneCount = detailViewModel.doneCountBetween(
                loopId = loop.id,
                from = firstDateOfMonth,
                to = date
            )
            val rate = doneCount.toFloat() / allCount
            x.add(months++)
            y.add(rate * 100)

            date = firstDateOfMonth.minusDays(1)
        }


        modelProducer.tryRunTransaction {
            columnSeries {
                series(x = x, y = y)
            }
        }
    }
    return modelProducer
}

@Composable
private fun rememberMonthlyDonRateChartBottomAxisFormatter()
        : AxisValueFormatter<AxisPosition.Horizontal.Bottom> {
    val context = LocalContext.current
    return remember {
        AxisValueFormatter { value, _, _ ->
            val now = LocalDate.now()
            val date = now.minusMonths(value.toLong())

            context.getString(
                R.string.format_month,
                context.getString(ABB_MONTHS[date.monthValue - 1]),
            )
        }
    }
}

@Composable
private fun WeeklyDoneRateChart(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
) {

}