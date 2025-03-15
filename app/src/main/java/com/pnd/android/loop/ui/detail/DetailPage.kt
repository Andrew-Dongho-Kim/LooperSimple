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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.currentTimeStat
import com.pnd.android.loop.ui.common.SimpleAd
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.common.chart.AdvancedLineChart
import com.pnd.android.loop.ui.common.chart.BarChart
import com.pnd.android.loop.ui.home.LoopDoneOrSkip
import com.pnd.android.loop.ui.home.LoopOnOffSwitch
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
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

private val paddingHorizontalNormal = 16.dp
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
    val loop by detailViewModel.loop.collectAsState(LoopVo.create())
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
                onNavigateUp = onNavigateUp,
                actions = {
                    LoopOnOffSwitch(
                        modifier = Modifier.padding(end = 24.dp),
                        enabled = loop.enabled,
                        onEnabled = { enabled -> detailViewModel.enableLoop(loop, enabled) }
                    )
                }
            )
        },
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
        LoopState(
            detailViewModel = detailViewModel,
            loop = loop,
        )

        LoopStartAndEndTime(
            modifier = Modifier.padding(top = paddingVerticalLarge),
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

        LoopResponseDoneSkipRate(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingVerticalSuperExtraLarge),
            detailViewModel = detailViewModel,
        )

        DoneHistoryGrid(
            modifier = Modifier
                .padding(top = paddingVerticalLarge)
                .fillMaxWidth()
                .height(264.dp),
            detailViewModel = detailViewModel,
            loop = loop,
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

        DayOfWeekDoneRateChart(
            modifier = Modifier.padding(
                top = paddingVerticalSuperExtraLarge
            ),
            detailViewModel = detailViewModel,
        )

        DetailPageDebug(
            modifier = Modifier.padding(top = paddingVerticalExtraLarge),
            detailViewModel = detailViewModel,
            loop = loop
        )
    }
}

@Composable
private fun LoopState(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    if (!loop.enabled) return
    Column(
        modifier = modifier
            .padding(all = 12.dp)
            .fillMaxWidth()
    ) {

        Row {
            val timeStat = loop.currentTimeStat

            Text(
                text = annotatedString(
                    text = timeStat.asString(LocalContext.current, false),
                    color = Color(loop.color).copy(alpha = 0.5f).compositeOverOnSurface(),
                ),
                style = AppTypography.headlineMedium.copy(
                    color = AppColor.onSurface
                )
            )

            Spacer(modifier = Modifier.weight(1f))

            if (timeStat.isPast()) {
                LoopDoneOrSkip(
                    modifier = Modifier.height(36.dp),
                    loop = loop,
                    onStateChanged = { loop, doneState ->
                        detailViewModel.doneLoop(
                            loop = loop,
                            doneState = doneState
                        )
                    },
                )
            }
        }
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
                .padding(start = paddingHorizontalNormal),
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
                modifier = Modifier.padding(start = paddingHorizontalNormal),
                text = if (loop.isAnyTime) stringResource(id = R.string.anytime) else loop.startInDay.formatHourMinute(),
                style = AppTypography.bodyMedium.copy(
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
                modifier = Modifier.padding(start = paddingHorizontalNormal),
                text = if (loop.isAnyTime) stringResource(id = R.string.anytime) else loop.endInDay.formatHourMinute(),
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
        val duration by detailViewModel.allEnabledCount.collectAsState(initial = 0)
        val doneCount by detailViewModel.doneCount.collectAsState(initial = 0)
        LoopRate(
            text = stringResource(id = R.string.done_rate),
            rate = if (duration == 0) 0f else (doneCount.toFloat() / duration) * 100,
            count = doneCount,
        )

        val skipCount by detailViewModel.skipCount.collectAsState(initial = 0)
        LoopRate(
            modifier = Modifier.padding(start = 24.dp),
            text = stringResource(id = R.string.skip_rate),
            rate = if (duration == 0) 0f else (skipCount.toFloat() / duration) * 100,
            count = skipCount,
        )

        val responseCount by detailViewModel.respondCount.collectAsState(initial = 0)
        LoopRate(
            modifier = Modifier.padding(start = 24.dp),
            text = stringResource(id = R.string.response_rate),
            rate = if (duration == 0) 0f else (responseCount.toFloat() / duration) * 100,
            count = responseCount,
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
            style = AppTypography.headlineSmall.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            )
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
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
        val loop by detailViewModel.loop.collectAsState(initial = LoopVo.create())

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

        var days = 0
        var date = LocalDate.now().plusDays(1L)
        val x = mutableListOf<Int>()
        val y = mutableListOf<Float>()
        while (date.isAfter(createdDate)) {
            val doneCount = detailViewModel.doneCountBefore(loop.loopId, date)
            val allCount = detailViewModel.allEnabledCountBefore(loop.loopId, date)
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
        val loop by detailViewModel.loop.collectAsState(initial = LoopVo.create())

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
            barColors = listOf(loop.color.compositeOverOnSurface()),
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

        var months = 0
        var date = LocalDate.now().plusDays(1L)
        val x = mutableListOf<Int>()
        val y = mutableListOf<Float>()
        while (date.isAfter(createdDate)) {
            val firstDateOfMonth = date.withDayOfMonth(1)

            val allCount = detailViewModel.allEnabledCountBetween(
                loopId = loop.loopId,
                from = firstDateOfMonth,
                to = date
            )
            val doneCount = detailViewModel.doneCountBetween(
                loopId = loop.loopId,
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
private fun DayOfWeekDoneRateChart(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
) {
    Column(modifier = modifier) {
        val loop by detailViewModel.loop.collectAsState(initial = LoopVo.create())

        Text(
            modifier = Modifier.padding(bottom = paddingVerticalNormal),
            text = stringResource(id = R.string.day_of_week_done_rate_chart),
            style = AppTypography.headlineSmall.copy(
                color = AppColor.onSurface
            )
        )

        BarChart(
            modelProducer = rememberDayOfWeekDoneRate(
                loop = loop,
                detailViewModel = detailViewModel,
            ),
            barColors = listOf(
                AppColor.onSurface.copy(alpha = 0.8f),
                loop.color.compositeOverOnSurface(0.8f),
            ),
            legends = listOf(
                stringResource(id = R.string.response_rate),
                stringResource(id = R.string.done_rate)
            ),
            bottomAxisValueFormatter = rememberDayOfWeekDonRateChartBottomAxisFormatter()
        )
    }
}

@Composable
private fun rememberDayOfWeekDoneRate(
    loop: LoopBase,
    detailViewModel: LoopDetailViewModel,
): CartesianChartModelProducer {

    val modelProducer = remember(loop) { CartesianChartModelProducer.build() }
    LaunchedEffect(key1 = loop) {
        val allEnabledDoneStates = detailViewModel.allEnabledDoneStates(loop.loopId)
        val doneStatesByDayOfWeek = allEnabledDoneStates.groupBy { doneVo ->
            doneVo.date.toLocalDate().dayOfWeek
        }

        val doneX = mutableListOf<Int>()
        val doneY = mutableListOf<Float>()

        val responseX = mutableListOf<Int>()
        val responseY = mutableListOf<Float>()
        doneStatesByDayOfWeek.forEach { entry ->
            val doneStates = entry.value
            val all = doneStates.size
            val numberOfDone = doneStates.count { doneVo -> doneVo.isDone() }
            val numberOfResponse = doneStates.count { doneVo -> doneVo.isRespond() }

            doneX.add(entry.key.value - 1)
            doneY.add((if (all == 0) 0f else numberOfDone.toFloat() / all) * 100)

            responseX.add(entry.key.value - 1)
            responseY.add((if (all == 0) 0f else numberOfResponse.toFloat() / all) * 100)
        }
        modelProducer.tryRunTransaction {
            columnSeries {
                series(x = responseX, y = responseY)
                series(x = doneX, y = doneY)
            }
        }
    }
    return modelProducer
}

@Composable
private fun rememberDayOfWeekDonRateChartBottomAxisFormatter()
        : AxisValueFormatter<AxisPosition.Horizontal.Bottom> {
    val context = LocalContext.current
    return remember {
        AxisValueFormatter { value, _, _ ->
            context.getString(DAYS_WITH_3CHARS[value.toInt()])
        }
    }
}

@Composable
private fun DetailPageDebug(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase
) {
    if (!BuildConfig.DEBUG) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppColor.onSurface.copy(alpha = 0.1f),
                shape = RoundShapes.medium
            )
            .padding(all = 12.dp)
    ) {
        Text(
            text = "[DEBUG]",
            style = AppTypography.titleMedium
        )

        Text(
            modifier = Modifier.padding(top = 18.dp),
            text = "Loop id: ${loop.loopId}",
            style = AppTypography.titleMedium
        )

        val responses by detailViewModel.allResponses.collectAsState(initial = emptyList())
        LazyColumn(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .height(100.dp)
        ) {
            items(
                items = responses,
                key = { it.date },
            ) { item ->

                Row {
                    Text(
                        text = "${item.date.toLocalDate()}"
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = when (item.done) {
                            LoopDoneVo.DoneState.DONE -> "O"
                            LoopDoneVo.DoneState.SKIP -> "."
                            LoopDoneVo.DoneState.NO_RESPONSE -> " "
                            LoopDoneVo.DoneState.DISABLED -> "D"
                            else -> "?"
                        }
                    )
                }
            }
        }
    }
}