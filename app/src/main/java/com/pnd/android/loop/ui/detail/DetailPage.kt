package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.model.lineSeries
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.Day.Companion.isOn
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.TimeStat
import com.pnd.android.loop.data.timeStatAsFlow
import com.pnd.android.loop.ui.common.AdvancedLineChart
import com.pnd.android.loop.ui.common.SimpleAd
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.annotatedString
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toLocalDateTime
import java.time.LocalDate
import kotlin.math.roundToInt

private val adId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-2341430172816266/5981213088"
}

private val paddingVerticalNormal = 14.dp
private val paddingVerticalLarge = 24.dp
private val paddingVerticalExtraLarge = 48.dp


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
            .fillMaxHeight(),
        topBar = {
            DetailAppBar(
                modifier = Modifier
                    .background(
                        color = AppColor.surface
                    )
                    .statusBarsPadding(),
                loop = loop,
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
            modifier = modifier
                .padding(top = paddingVerticalExtraLarge)
                .fillMaxWidth()
                .height(224.dp),
            detailViewModel = detailViewModel,
            loop = loop,
        )
        LoopResponseDoneSkipRate(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = paddingVerticalNormal),
            detailViewModel = detailViewModel,
        )

        DoneRateChart(
            modifier = Modifier.padding(
                top = paddingVerticalExtraLarge
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
private fun DoneHistoryGrid(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    val doneHistory = detailViewModel.donePager.collectAsLazyPagingItems()

    val lazyGridState = rememberLazyGridState()
    LaunchedEffect(key1 = Unit) { lazyGridState.scrollToItem(index = Int.MAX_VALUE) }

    Column(modifier = modifier) {
        DoneHistoryGridIconsDescription(
            doneColor = Color(loop.color)
        )

        Row(modifier = Modifier.padding(top = 8.dp)) {
            LazyHorizontalGrid(
                modifier = Modifier.weight(1f),
                state = lazyGridState,
                rows = GridCells.Fixed(7)
            ) {
                items(
                    lazyPagingItems = doneHistory,
                    key = { item -> item.date }
                ) { item ->
                    DoneHistoryItem(
                        modifier = Modifier.size(32.dp),
                        created = loop.created,
                        doneVo = item,
                        color = Color(loop.color).copy(alpha = 0.7f),
                        activeDays = loop.loopActiveDays
                    )
                }
            }
            DoneHistoryDayHeader(
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun DoneHistoryGridIconsDescription(
    modifier: Modifier = Modifier,
    doneColor: Color
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemDescriptionItem(
            doneState = LoopDoneVo.DoneState.DONE,
            text = stringResource(id = R.string.done),
            color = doneColor,
        )
        ItemDescriptionItem(
            modifier = Modifier.padding(start = 12.dp),
            doneState = LoopDoneVo.DoneState.SKIP,
            text = stringResource(id = R.string.skip),
            color = AppColor.onSurface,
        )

        ItemDescriptionItem(
            modifier = Modifier.padding(start = 12.dp),
            doneState = LoopDoneVo.DoneState.NO_RESPONSE,
            text = stringResource(id = R.string.no_response),
            color = AppColor.onSurface
        )
    }
}

@Composable
private fun ItemDescriptionItem(
    modifier: Modifier = Modifier,
    @LoopDoneVo.DoneState doneState: Int,
    text: String,
    color: Color
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            imageVector = when (doneState) {
                LoopDoneVo.DoneState.DONE -> Icons.Outlined.Circle
                LoopDoneVo.DoneState.SKIP -> Icons.Outlined.Close
                else -> Icons.Outlined.Remove
            },
            colorFilter = ColorFilter.tint(
                color.copy(alpha = ContentAlpha.disabled)
            ),
            contentDescription = null
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )
    }
}

@Composable
private fun DoneHistoryDayHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ABB_DAYS.forEach { dayResId ->
            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(align = Alignment.CenterVertically),
                text = stringResource(id = dayResId),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface
                )
            )
        }
    }
}

@Composable
private fun DoneHistoryItem(
    modifier: Modifier = Modifier,
    created: Long,
    doneVo: LoopDoneVo,
    color: Color,
    activeDays: Int
) {
    val createdDate = remember(created) { created.toLocalDate() }
    val localDate = remember(doneVo.date) { doneVo.date.toLocalDate() }
    val isActive = remember(localDate, activeDays) { activeDays.isOn(dayForLoop(localDate)) }
    val isBeforeCreated = remember(localDate, createdDate) { localDate.isBefore(createdDate) }
    val firstDateOfMonth = localDate.dayOfMonth == 1

    val shape = RoundShapes.small
    Box(
        modifier = modifier
            .padding(1.dp)
            .clip(shape = shape)
            .background(
                color = if (firstDateOfMonth) {
                    AppColor.onSurface.copy(alpha = 0.1f)
                } else {
                    Color.Transparent
                }
            )
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.2f),
                shape = shape
            )
            .alpha(if (!isActive || isBeforeCreated) 0.3f else 1.0f)
    ) {
        if (isActive &&
            (localDate.isAfter(createdDate) || localDate.isEqual(createdDate))
        ) {
            Image(
                modifier = Modifier.align(Alignment.Center),
                imageVector = when {
                    doneVo.isDone() -> Icons.Outlined.Circle
                    doneVo.isSkip() -> Icons.Outlined.Close
                    else -> Icons.Outlined.Remove
                },
                colorFilter = ColorFilter.tint(
                    if (doneVo.isDone()) {
                        color
                    } else {
                        AppColor.onSurface
                    }.copy(alpha = ContentAlpha.disabled)
                ),
                contentDescription = null
            )
        }

        DoneHistoryItemText(
            modifier = Modifier.align(Alignment.Center),
            createdDate = createdDate,
            localDate = localDate,
        )
    }
}

@Composable
private fun DoneHistoryItemText(
    modifier: Modifier = Modifier,
    createdDate: LocalDate,
    localDate: LocalDate,
) {
    val now = LocalDate.now()
    val firstDateOfMonth = localDate.dayOfMonth == 1

    val text = when {
        now == localDate -> stringResource(id = R.string.today)
        localDate == createdDate -> stringResource(id = R.string.start)
        firstDateOfMonth -> "${localDate.month.value}/1"
        else -> "${localDate.dayOfMonth}"
    }

    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.labelMedium.copy(
            color = AppColor.onSurface,
            fontWeight = FontWeight.Normal
        )
    )
}

fun <T : Any> LazyGridScope.items(
    lazyPagingItems: LazyPagingItems<T>,
    key: ((item: T) -> Any),
    itemContent: @Composable LazyGridItemScope.(value: T) -> Unit
) {
    items(
        count = lazyPagingItems.itemCount,
        key = { index -> key(requireNotNull(lazyPagingItems[index])) },
        contentType = { "Item" }
    ) { index ->
        itemContent(requireNotNull(lazyPagingItems[index]))
    }
}

@Composable
private fun DoneRateChart(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
) {
    val loop by detailViewModel.loop.collectAsState(initial = LoopBase.default())

    val background = AppColor.onSurface
    val color =
        remember(loop.color) { Color(loop.color).copy(alpha = 0.8f).compositeOver(background) }


    AdvancedLineChart(
        modifier = modifier,
        modelProducer = rememberChardModel(
            loop = loop,
            detailViewModel = detailViewModel,
        ),
        chartColors = listOf(
            color.copy(alpha = 0.9f),
            AppColor.error.copy(alpha = 0.8f),
        ),
        titleStartAxis = stringResource(id = R.string.done_rate),
        bottomAxisValueFormatter = rememberDonRateChartBottomAxisFormatter()
    )
}

@Composable
private fun rememberChardModel(
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
            y.add((rate * 100))

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
private fun rememberDonRateChartBottomAxisFormatter(): AxisValueFormatter<AxisPosition.Horizontal.Bottom> {
    val context = LocalContext.current
    return remember {
        AxisValueFormatter<AxisPosition.Horizontal.Bottom> { value, _, _ ->
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