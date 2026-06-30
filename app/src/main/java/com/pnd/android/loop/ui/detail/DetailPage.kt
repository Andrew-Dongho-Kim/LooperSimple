package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.common.NO_REPEAT
import com.pnd.android.loop.data.currentTimeStat
import com.pnd.android.loop.ui.common.SimpleAd
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.common.chart.AdvancedLineChart
import com.pnd.android.loop.ui.common.chart.BarChart
import com.pnd.android.loop.ui.home.LoopDoneOrSkip
import com.pnd.android.loop.ui.home.LoopOnOffSwitch
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.secondary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.annotatedString
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toLocalDateTime
import java.time.LocalDate
import kotlin.math.roundToInt

private val adId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-2341430172816266/5981213088"
}

/** Inner padding shared by every card on the detail screen. */
private val CardPadding = 20.dp

/** Vertical gap between rows inside a card (info rows, header → content). */
private val CardInnerSpacing = 16.dp


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
                        modifier = Modifier.padding(end = Dimens.screenHorizontalPadding),
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
            .padding(horizontal = Dimens.screenHorizontalPadding)
            .padding(top = Dimens.contentPadding, bottom = 48.dp)
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing),
    ) {
        StatusCard(
            detailViewModel = detailViewModel,
            loop = loop,
        )

        LoopRateSummary(detailViewModel = detailViewModel)

        ScheduleCard(loop = loop)

        SimpleAd(adId = adId)

        DoneHistorySection(
            detailViewModel = detailViewModel,
            loop = loop,
        )

        DailyDoneRateChart(detailViewModel = detailViewModel)

        MonthlyDoneRateChart(detailViewModel = detailViewModel)

        DayOfWeekDoneRateChart(detailViewModel = detailViewModel)

        DetailPageDebug(
            detailViewModel = detailViewModel,
            loop = loop,
        )
    }
}

/**
 * Shared container for every section on the detail screen: a lifted surface with soft
 * rounding and a hairline border so cards read as a distinct layer over the background in
 * both light and dark themes (mirrors the card styling used on the home screen).
 */
@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundShapes.large,
        color = AppColor.surfaceElevated,
        border = BorderStroke(
            width = 0.5.dp,
            color = AppColor.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(all = CardPadding),
            content = content,
        )
    }
}

/** Bold title shown at the top of a card to label the section below it. */
@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        style = AppTypography.titleMedium.copy(
            color = AppColor.onSurface,
            fontWeight = FontWeight.Bold,
        ),
    )
}

/**
 * Hero card showing where the loop sits in today's window ("32 mins left", "Finished", …),
 * led by a dot in the loop's own color. Once the window has passed it also exposes the
 * done / skip action so the day can be checked off without leaving the screen.
 */
@Composable
private fun StatusCard(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    if (!loop.enabled) return

    val timeStat = loop.currentTimeStat
    val statusText = timeStat.asString(LocalContext.current, false)
    if (statusText.isEmpty() && !timeStat.isPast()) return

    val accent = Color(loop.color).compositeOverOnSurface()

    DetailCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(accent),
            )

            Text(
                modifier = Modifier
                    .padding(start = 12.dp)
                    .weight(1f),
                text = annotatedString(text = statusText, color = accent),
                style = AppTypography.headlineSmall.copy(color = AppColor.onSurface),
            )

            if (timeStat.isPast()) {
                LoopDoneOrSkip(
                    modifier = Modifier.height(36.dp),
                    loop = loop,
                    onStateChanged = { changed, doneState ->
                        detailViewModel.doneLoop(loop = changed, doneState = doneState)
                    },
                )
            }
        }
    }
}

/**
 * Card that collects the loop's schedule into a single, scannable place: start / end time,
 * repeat interval (hidden when the loop never repeats), creation date with the running day
 * count, and the active weekdays rendered as chips.
 */
@Composable
private fun ScheduleCard(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    val anyTime = stringResource(id = R.string.anytime)
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
    val dayCount = LocalDate.now().toEpochDay() - createdDate.toEpochDay() + 1

    DetailCard(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.detail_schedule))

        Column(
            modifier = Modifier.padding(top = CardInnerSpacing),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoRow(
                icon = Icons.Outlined.Schedule,
                label = stringResource(id = R.string.start),
                value = if (loop.isAnyTime) anyTime else loop.startInDay.formatHourMinute(),
            )
            InfoRow(
                icon = Icons.Outlined.Schedule,
                label = stringResource(id = R.string.end),
                value = if (loop.isAnyTime) anyTime else loop.endInDay.formatHourMinute(),
            )
            if (loop.interval != NO_REPEAT) {
                InfoRow(
                    icon = Icons.Outlined.Repeat,
                    label = stringResource(id = R.string.detail_repeat),
                    value = intervalString(loop.interval),
                )
            }
            InfoRow(
                icon = Icons.Outlined.DateRange,
                label = stringResource(id = R.string.created_date),
                value = createdDate.formatYearMonthDateDays(),
                trailing = stringResource(id = R.string.n_days, dayCount),
            )

            ActiveDaysRow(
                modifier = Modifier.padding(top = 2.dp),
                activeDays = loop.activeDays,
            )
        }
    }
}

/** A single "icon · label … value" line used inside [ScheduleCard]. */
@Composable
private fun InfoRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = AppColor.onSurface.copy(alpha = 0.5f),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = label,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (trailing != null) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = trailing,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** The seven weekdays as compact chips; active days are filled and tinted with the accent. */
@Composable
private fun ActiveDaysRow(
    modifier: Modifier = Modifier,
    activeDays: Int,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val selected = activeDays.isOn(LoopDay.fromIndex(index))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (selected) {
                            AppColor.primary.copy(alpha = 0.14f)
                        } else {
                            AppColor.surfaceContainer
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(id = dayResId),
                    style = AppTypography.labelMedium.copy(
                        color = if (selected) {
                            AppColor.primary
                        } else {
                            AppColor.onSurface.copy(alpha = 0.4f)
                        },
                    ),
                )
            }
        }
    }
}

/**
 * Three side-by-side stat cards summarising how the loop has been answered overall:
 * done, skipped and total response rate. Each shows the percentage, the raw count and a
 * thin progress bar so the numbers are comparable at a glance.
 */
@Composable
private fun LoopRateSummary(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
) {
    val total by detailViewModel.allEnabledCount.collectAsState(initial = 0)
    val doneCount by detailViewModel.doneCount.collectAsState(initial = 0)
    val skipCount by detailViewModel.skipCount.collectAsState(initial = 0)
    val responseCount by detailViewModel.respondCount.collectAsState(initial = 0)

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.done_rate),
            count = doneCount,
            total = total,
            accent = AppColor.primary,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.skip_rate),
            count = skipCount,
            total = total,
            accent = AppColor.secondary,
        )
        StatCard(
            modifier = Modifier.weight(1f),
            label = stringResource(id = R.string.response_rate),
            count = responseCount,
            total = total,
            accent = AppColor.onSurface,
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    label: String,
    count: Int,
    total: Int,
    accent: Color,
) {
    val fraction = if (total == 0) 0f else (count.toFloat() / total).coerceIn(0f, 1f)
    val percent = (fraction * 100).roundToInt()

    Surface(
        modifier = modifier,
        shape = RoundShapes.large,
        color = AppColor.surfaceElevated,
        border = BorderStroke(
            width = 0.5.dp,
            color = AppColor.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = label,
                style = AppTypography.labelLarge.copy(
                    color = AppColor.onSurface.copy(alpha = 0.6f),
                ),
            )
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = "$percent%",
                style = AppTypography.headlineMedium.copy(color = accent),
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = "($count)",
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.45f),
                ),
            )
            StatProgressBar(
                modifier = Modifier.padding(top = 10.dp),
                fraction = fraction,
                accent = accent,
            )
        }
    }
}

/** Rounded track + accent fill used as a compact rate indicator inside [StatCard]. */
@Composable
private fun StatProgressBar(
    modifier: Modifier = Modifier,
    fraction: Float,
    accent: Color,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(4.dp)
            .clip(CircleShape)
            .background(AppColor.surfaceContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction)
                .fillMaxHeight()
                .clip(CircleShape)
                .background(accent),
        )
    }
}

/** Calendar-style done/skip history, wrapped in a titled card. */
@Composable
private fun DoneHistorySection(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    DetailCard(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.daily_record))

        DoneHistoryGrid(
            modifier = Modifier
                .padding(top = CardInnerSpacing)
                .fillMaxWidth()
                .height(264.dp),
            detailViewModel = detailViewModel,
            loop = loop,
        )
    }
}

@Composable
private fun DailyDoneRateChart(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
) {
    val loop by detailViewModel.loop.collectAsState(initial = LoopVo.create())

    DetailCard(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.daily_done_rate_chart))

        AdvancedLineChart(
            modifier = Modifier.padding(top = CardInnerSpacing),
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
            val rate = if (allCount == 0) 0f else doneCount.toFloat() / allCount
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
    val loop by detailViewModel.loop.collectAsState(initial = LoopVo.create())

    DetailCard(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.monthly_done_rate_chart))

        BarChart(
            modifier = Modifier.padding(top = CardInnerSpacing),
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
            val rate = if (allCount == 0) 0f else doneCount.toFloat() / allCount
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
    val loop by detailViewModel.loop.collectAsState(initial = LoopVo.create())

    DetailCard(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.day_of_week_done_rate_chart))

        BarChart(
            modifier = Modifier.padding(top = CardInnerSpacing),
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
            // The chart may probe the formatter with extrapolated axis values;
            // clamp to the valid day-of-week range to avoid index out of bounds.
            val index = value.toInt().coerceIn(0, DAYS_WITH_3CHARS.lastIndex)
            context.getString(DAYS_WITH_3CHARS[index])
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