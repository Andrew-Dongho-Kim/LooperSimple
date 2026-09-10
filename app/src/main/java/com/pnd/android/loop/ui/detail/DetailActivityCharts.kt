package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import java.time.LocalDate

private val PlotHeight = 144.dp
private val ColumnGap = 6.dp
private val ValueGap = 6.dp
private val BarShape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)

/** 기록 없음은 null, 완료 0일은 0이다. 두 경우의 막대는 비어도 수치와 접근성 설명은 다르다. */
private data class ActivityChartColumn(
    val label: String,
    val value: Float?,
    val valueLabel: String,
    val description: String,
    val caption: String? = null,
)

private data class ChartTick(val value: Float, val label: String)

private data class OutcomeSegment(val label: String, val count: Int, val color: Color)

@Composable
internal fun ActivityPeriodChart(periods: List<ActivityPeriod>, accent: Color) {
    val columns = periods.map { period ->
        val hasRecords = period.counts.total > 0
        val range = stringResource(
            R.string.detail_activity_date_range,
            shortDate(period.start),
            shortDate(period.end),
        )
        val result = if (hasRecords) {
            stringResource(R.string.detail_activity_completed_days, period.counts.done)
        } else {
            stringResource(R.string.detail_activity_no_records)
        }
        ActivityChartColumn(
            label = shortDate(period.start) + "\n– " + shortDate(period.end),
            value = period.counts.done.toFloat().takeIf { hasRecords },
            valueLabel = if (hasRecords) period.counts.done.toString() else "—",
            description = "$range, $result",
        )
    }
    ActivityColumnChart(
        columns = columns,
        maximum = 7f,
        ticks = listOf(ChartTick(0f, "0"), ChartTick(4f, "4"), ChartTick(7f, "7")),
        accent = accent,
    )
}

@Composable
internal fun ActivityWeekdayChart(weekdays: List<WeekdayActivity>, accent: Color) {
    val columns = weekdays.map { weekday ->
        val dayName = stringResource(DAYS_WITH_3CHARS[weekday.day.value - 1])
        val counts = weekday.counts
        val result = if (counts.total == 0) {
            stringResource(R.string.detail_activity_no_records)
        } else {
            stringResource(R.string.detail_activity_sample, counts.total, counts.done)
        }
        ActivityChartColumn(
            // 월~일 순서는 유지하면서 일~토 배열에서 해당 약자를 선택한다.
            label = stringResource(ABB_DAYS[weekday.day.value % 7]),
            value = counts.completionRate?.times(100),
            valueLabel = counts.completionPercent?.let { "$it%" } ?: "—",
            caption = if (counts.total == 0) "—" else "${counts.done}/${counts.total}",
            description = "$dayName, $result",
        )
    }
    ActivityColumnChart(
        columns = columns,
        maximum = 100f,
        ticks = listOf(ChartTick(0f, "0"), ChartTick(50f, "50"), ChartTick(100f, "100%")),
        accent = accent,
    )
}

/**
 * 두 그래프가 공유하는 좌표계. 막대는 0부터 시작하며, 눈금과 실제 높이가 같은 척도를 쓴다.
 * 값 라벨의 줄바꿈 높이를 먼저 측정해 큰 글꼴·좁은 화면에서도 막대 위 공간을 확보한다.
 */
@Composable
private fun ActivityColumnChart(
    columns: List<ActivityChartColumn>,
    maximum: Float,
    ticks: List<ChartTick>,
    accent: Color,
) {
    if (columns.isEmpty()) return
    val density = LocalDensity.current
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = AppTypography.bodySmall.copy(color = AppColor.onSurface)
    val gridColor = AppColor.onSurface.copy(alpha = 0.1f)

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val axisWidth = with(density) {
            ticks.maxOf { textMeasurer.measure(it.label, labelStyle).size.width }.toDp() + 10.dp
        }
        val columnWidth = (this.maxWidth - axisWidth - ColumnGap * (columns.size - 1)) / columns.size
        val textWidth = with(density) { columnWidth.roundToPx().coerceAtLeast(1) }
        val valueHeight = with(density) {
            columns.maxOf { column ->
                textMeasurer.measure(
                    text = column.valueLabel,
                    style = labelStyle,
                    constraints = Constraints(maxWidth = textWidth),
                ).size.height
            }.toDp()
        }
        val labelSpace = valueHeight + ValueGap
        val chartHeight = PlotHeight + labelSpace

        Row {
            Box(Modifier.width(axisWidth).height(chartHeight).clearAndSetSemantics {}) {
                ticks.forEach { tick ->
                    val textHeight = with(density) {
                        textMeasurer.measure(tick.label, labelStyle).size.height.toDp()
                    }
                    Text(
                        modifier = Modifier
                            .offset(y = labelSpace + PlotHeight * (1f - tick.value / maximum) - textHeight / 2)
                            .fillMaxWidth()
                            .padding(end = 10.dp),
                        text = tick.label,
                        style = labelStyle.copy(color = AppColor.onSurface.copy(alpha = 0.55f)),
                        textAlign = TextAlign.End,
                    )
                }
            }
            Box(modifier = Modifier.weight(1f)) {
                Canvas(Modifier.fillMaxWidth().height(chartHeight).clearAndSetSemantics {}) {
                    ticks.forEach { tick ->
                        val y = labelSpace.toPx() + PlotHeight.toPx() * (1f - tick.value / maximum)
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 0.5.dp.toPx())
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(ColumnGap)) {
                    columns.forEach { column ->
                        ActivityColumn(
                            modifier = Modifier.weight(1f),
                            column = column,
                            chartHeight = chartHeight,
                            maximum = maximum,
                            accent = accent,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ActivityColumn(
    column: ActivityChartColumn,
    chartHeight: Dp,
    maximum: Float,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val fraction = ((column.value ?: 0f) / maximum).coerceIn(0f, 1f)
    Column(
        modifier = modifier.clearAndSetSemantics { contentDescription = column.description },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth().height(chartHeight)) {
            Column(
                modifier = Modifier.align(Alignment.BottomCenter).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    // 눈금선이 수치를 가로지르지 않도록 라벨 뒤를 화면 배경으로 가린다.
                    modifier = Modifier.fillMaxWidth().background(AppColor.background),
                    text = column.valueLabel,
                    style = AppTypography.bodySmall.copy(color = AppColor.onSurface),
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(ValueGap))
                Box(
                    Modifier.widthIn(max = 40.dp).fillMaxWidth(0.68f)
                        .height(PlotHeight * fraction)
                        .clip(BarShape)
                        .background(accent),
                )
            }
        }
        Text(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            text = column.label,
            style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.75f)),
            textAlign = TextAlign.Center,
        )
        column.caption?.let { caption ->
            Text(
                modifier = Modifier.fillMaxWidth().padding(top = 3.dp),
                text = caption,
                style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.55f)),
                textAlign = TextAlign.Center,
            )
        }
    }
}

/** 완료·건너뜀·미응답의 전체 비중을 실제 개수에 비례해 표시한다. 범례는 줄바꿈을 허용한다. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ActivityOutcomeChart(counts: ActivityCounts, accent: Color) {
    if (counts.total == 0) return
    val outcomes = listOf(
        OutcomeSegment(stringResource(R.string.detail_rate_done), counts.done, accent),
        OutcomeSegment(stringResource(R.string.detail_rate_skip), counts.skipped, AppColor.onSurface.copy(alpha = 0.45f)),
        OutcomeSegment(stringResource(R.string.detail_rate_no_response), counts.unanswered, AppColor.onSurface.copy(alpha = 0.16f)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(Modifier.fillMaxWidth().height(12.dp).clip(CircleShape).clearAndSetSemantics {}) {
            outcomes.filter { it.count > 0 }.forEach { segment ->
                Box(Modifier.weight(segment.count.toFloat()).fillMaxHeight().background(segment.color))
            }
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            outcomes.forEach { segment ->
                Row(
                    modifier = Modifier.semantics(mergeDescendants = true) {},
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Box(Modifier.size(7.dp).clip(CircleShape).background(segment.color))
                    Text(
                        text = stringResource(R.string.detail_activity_legend_entry, segment.label, segment.count),
                        style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.75f)),
                    )
                }
            }
        }
    }
}

private fun shortDate(date: LocalDate): String = "${date.monthValue}/${date.dayOfMonth}"
