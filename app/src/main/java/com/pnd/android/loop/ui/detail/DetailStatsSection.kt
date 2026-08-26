package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import kotlin.math.roundToInt

// ─────────────────────────────────────────────────────────────────────────────
// 통계 섹션 — 한 줄 인사이트 + 2×2 타일
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 통계 섹션. 예전에는 카드 넷(성취 요약 · 응답 요약 · 일별 · 월별 · 요일별)이 세로로 늘어서
 * 화면 두 개 분량을 썼다. 지금은 한 줄 인사이트 아래 반폭 타일 넷으로 접혀 한 화면에 들어간다.
 *
 * 데이터가 없는 타일도 자리를 비우지 않고 "—"로 남긴다. 격자가 들쭉날쭉해지면 오히려 읽기 어렵고,
 * "아직 기록이 없다"는 것도 정보이기 때문이다.
 *
 * 그림만으로는 아무것도 전해지지 않던 스파크라인·막대·구성 막대에는 값을 말로 옮긴 설명을 붙였다.
 */
@Composable
internal fun StatsSection(
    modifier: Modifier = Modifier,
    stats: DetailStats,
    accent: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val dailyTrend = stats.dailyTrend

    val trendTint = when {
        dailyTrend == null -> AppColor.onSurface.copy(alpha = 0.55f)
        dailyTrend.deltaPercent > 0 -> AppColor.primary
        dailyTrend.deltaPercent < 0 -> AppColor.error
        else -> AppColor.onSurface.copy(alpha = 0.55f)
    }

    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.Insights,
        title = stringResource(id = R.string.detail_section_stats),
        summary = dailyTrend?.let { formatDeltaPercent(it.deltaPercent) },
        summaryColor = trendTint,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        if (dailyTrend != null) {
            InsightRow(
                icon = when {
                    dailyTrend.deltaPercent > 0 -> Icons.AutoMirrored.Outlined.TrendingUp
                    dailyTrend.deltaPercent < 0 -> Icons.AutoMirrored.Outlined.TrendingDown
                    else -> Icons.AutoMirrored.Outlined.TrendingFlat
                },
                iconTint = trendTint,
                text = stringResource(
                    id = when {
                        dailyTrend.deltaPercent > 0 -> R.string.detail_trend_daily_up
                        dailyTrend.deltaPercent < 0 -> R.string.detail_trend_daily_down
                        else -> R.string.detail_trend_daily_steady
                    }
                ),
            )
        }

        StatTiles(
            modifier = Modifier.padding(top = if (dailyTrend == null) 0.dp else CardInnerSpacing),
            stats = stats,
            accent = accent,
        )
    }
}

/** 통계 섹션 맨 위의 "아이콘 · 한 줄 결론". 타일이 답할 수 없는 "그래서 어떤데"를 문장으로 말한다. */
@Composable
private fun InsightRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconTint: Color,
    text: String,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = iconTint,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** 반폭 타일 2×2. 같은 줄의 두 타일은 [IntrinsicSize.Min] 으로 높이를 맞춘다. */
@Composable
private fun StatTiles(
    modifier: Modifier = Modifier,
    stats: DetailStats,
    accent: Color,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(TileSpacing),
    ) {
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(TileSpacing),
        ) {
            TrendTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                trend = stats.dailyTrend,
                accent = accent,
            )
            WeekdayTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                rates = stats.weekdayRates,
                accent = accent,
            )
        }
        Row(
            modifier = Modifier.height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(TileSpacing),
        ) {
            MonthlyTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                months = stats.monthlyRates,
                accent = accent,
            )
            CompositionTile(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                stats = stats,
                accent = accent,
            )
        }
    }
}

/** 타일 한 장의 껍데기: 라벨 → 시각화 → 값. 세 줄 구조를 넷이 공유해 격자가 정돈돼 보인다. */
@Composable
private fun StatTile(
    modifier: Modifier = Modifier,
    label: String,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            // 한 타일은 "라벨 · 그림 · 값"이 모여 하나의 뜻을 이룬다. 조각내 읽으면 맥락이 끊긴다.
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
        Text(
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.labelSmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
        content()
    }
}

/** 타일 맨 아랫줄: 큰 값 + (있으면) 변화 배지. */
@Composable
private fun TileValueRow(
    modifier: Modifier = Modifier,
    value: String,
    delta: Int? = null,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = value,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.bodyLarge.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (delta != null) {
            Text(
                modifier = Modifier.padding(start = 6.dp, bottom = 1.dp),
                text = formatDeltaPercent(delta),
                style = AppTypography.labelSmall.copy(
                    color = when {
                        delta > 0 -> AppColor.primary
                        delta < 0 -> AppColor.error
                        else -> AppColor.onSurface.copy(alpha = 0.45f)
                    },
                ),
            )
        }
    }
}

/** 데이터가 아직 없을 때 타일이 보여 주는 자리. */
@Composable
private fun TileEmptyValue(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier.semantics { contentDescription = "" },
        text = stringResource(id = R.string.detail_tile_no_data),
        style = AppTypography.bodyLarge.copy(
            color = AppColor.onSurface.copy(alpha = 0.3f),
        ),
    )
}

/** 완료율 추세 타일: 스파크라인 + 현재 완료율 + 2주 전 대비 변화. */
@Composable
private fun TrendTile(
    modifier: Modifier = Modifier,
    trend: DailyTrend?,
    accent: Color,
) {
    StatTile(
        modifier = modifier,
        label = stringResource(id = R.string.detail_tile_trend),
    ) {
        if (trend == null) {
            TileEmptyValue(modifier = Modifier.padding(top = 10.dp))
            return@StatTile
        }

        Sparkline(
            modifier = Modifier
                .padding(top = 10.dp)
                .fillMaxWidth()
                .height(TileVizHeight),
            values = trend.rates,
            color = accent,
        )
        TileValueRow(
            modifier = Modifier.padding(top = 8.dp),
            value = "${(trend.rates.last() * 100).roundToInt()}%",
            delta = trend.deltaPercent,
        )
    }
}

/** 요일별 타일: 7개 막대 + 가장 잘 지키는 요일. */
@Composable
private fun WeekdayTile(
    modifier: Modifier = Modifier,
    rates: List<Float?>,
    accent: Color,
) {
    val bestIndex = rates
        .withIndex()
        .filter { it.value != null }
        .maxByOrNull { it.value ?: 0f }
        ?.index

    StatTile(
        modifier = modifier,
        label = stringResource(id = R.string.detail_tile_weekday),
    ) {
        if (bestIndex == null) {
            TileEmptyValue(modifier = Modifier.padding(top = 10.dp))
            return@StatTile
        }

        TileBars(
            modifier = Modifier.padding(top = 10.dp),
            fractions = rates.map { it ?: 0f },
            highlightIndex = bestIndex,
            accent = accent,
        )
        TileValueRow(
            modifier = Modifier.padding(top = 8.dp),
            value = stringResource(
                id = R.string.detail_tile_best_day,
                stringResource(id = DAYS_WITH_3CHARS_SUNDAY_FIRST[bestIndex]),
                ((rates[bestIndex] ?: 0f) * 100).roundToInt(),
            ),
        )
    }
}

/** 월별 타일: 최근 몇 달 막대 + 이번 달 완료율과 지난달 대비 변화. */
@Composable
private fun MonthlyTile(
    modifier: Modifier = Modifier,
    months: List<MonthlyRate>,
    accent: Color,
) {
    StatTile(
        modifier = modifier,
        label = stringResource(id = R.string.detail_tile_monthly),
    ) {
        if (months.isEmpty()) {
            TileEmptyValue(modifier = Modifier.padding(top = 10.dp))
            return@StatTile
        }

        val current = months.last()
        val delta = months.getOrNull(months.size - 2)?.let { prev ->
            ((current.rate - prev.rate) * 100).roundToInt()
        }

        TileBars(
            modifier = Modifier.padding(top = 10.dp),
            fractions = months.map { it.rate },
            highlightIndex = months.lastIndex,
            accent = accent,
        )
        TileValueRow(
            modifier = Modifier.padding(top = 8.dp),
            value = stringResource(
                id = R.string.detail_tile_month_rate,
                stringResource(id = ABB_MONTHS[current.month.monthValue - 1]),
                (current.rate * 100).roundToInt(),
            ),
            delta = delta,
        )
    }
}

/** 응답 구성 타일: 완료·건너뜀·미응답 비중 막대 + 개수. */
@Composable
private fun CompositionTile(
    modifier: Modifier = Modifier,
    stats: DetailStats,
    accent: Color,
) {
    StatTile(
        modifier = modifier,
        label = stringResource(id = R.string.detail_response_summary),
    ) {
        if (!stats.hasAnyRecord) {
            TileEmptyValue(modifier = Modifier.padding(top = 10.dp))
            return@StatTile
        }

        CompositionBar(
            modifier = Modifier.padding(top = 10.dp),
            doneCount = stats.doneCount,
            skipCount = stats.skipCount,
            noResponseCount = stats.noResponseCount,
            doneColor = accent,
            skipColor = AppColor.onSurface.copy(alpha = 0.35f),
        )
        TileValueRow(
            modifier = Modifier.padding(top = 8.dp),
            value = "${stringResource(id = R.string.detail_rate_done)} ${stats.doneCount}",
        )
        Text(
            modifier = Modifier.padding(top = 2.dp),
            text = "${stringResource(id = R.string.detail_rate_skip)} ${stats.skipCount} · " +
                    "${stringResource(id = R.string.detail_rate_no_response)} ${stats.noResponseCount}",
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.labelSmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.45f),
            ),
        )
    }
}

/** 타일 안 시각화가 공유하는 높이. 넷이 같은 높이를 쓰면 격자가 흔들리지 않는다. */
private val TileVizHeight = 32.dp

/**
 * 타일용 막대 열. 라벨 없이 형태만 보여 준다 — 반폭 타일에서 7~8개 라벨은 읽을 수 없을 만큼
 * 작아지고, 정작 알아야 할 값은 아래 [TileValueRow] 가 글자로 말해 주기 때문이다.
 *
 * 그래서 그림 자체는 스크린 리더에서 감춘다. 읽을 값은 같은 타일의 값 줄에 이미 있다.
 */
@Composable
private fun TileBars(
    modifier: Modifier = Modifier,
    fractions: List<Float>,
    highlightIndex: Int,
    accent: Color,
    barAreaHeight: Dp = TileVizHeight,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(barAreaHeight)
            .clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(3.dp),
        verticalAlignment = Alignment.Bottom,
    ) {
        fractions.forEachIndexed { index, fraction ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    // 값이 0이어도 바닥에 아주 얇은 자국을 남겨 "칸"이 있음을 알린다.
                    .fillMaxHeight(fraction.coerceIn(0.06f, 1f))
                    .clip(RoundShapes.small)
                    .background(
                        if (index == highlightIndex) accent else accent.copy(alpha = 0.25f),
                    ),
            )
        }
    }
}

/**
 * 완료율(0f..1f) 시계열을 선으로 잇는 가벼운 스파크라인.
 * 절대 높이를 읽을 수 있도록 50% 기준선을 점선으로 깔고, 마지막 값에 점을 찍어 "지금"을 표시한다.
 */
@Composable
private fun Sparkline(
    modifier: Modifier = Modifier,
    values: List<Float>,
    color: Color,
) {
    val guideColor = AppColor.onSurface.copy(alpha = 0.12f)
    Canvas(modifier = modifier.clearAndSetSemantics { }) {
        if (values.size < 2) return@Canvas

        drawLine(
            color = guideColor,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(3.dp.toPx(), 3.dp.toPx()),
            ),
        )

        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height * (1f - value.coerceIn(0f, 1f))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )

        drawCircle(
            color = color,
            radius = 2.5.dp.toPx(),
            center = Offset(
                x = size.width,
                y = size.height * (1f - values.last().coerceIn(0f, 1f)),
            ),
        )
    }
}

/**
 * 완료 · 건너뜀 · 미응답을 이어 붙인 하나의 알약형 막대. 각 구간 너비는 개수에 비례한다.
 * 개수가 0인 구간은 그리지 않아(가중치 0 방지) 크래시를 막고, 미응답 구간은 색을 칠하지 않아
 * 트랙(빈 배경)이 그대로 드러나게 표현한다.
 */
@Composable
private fun CompositionBar(
    modifier: Modifier = Modifier,
    doneCount: Int,
    skipCount: Int,
    noResponseCount: Int,
    doneColor: Color,
    skipColor: Color,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(CircleShape)
            .background(AppColor.onSurface.copy(alpha = 0.08f))
            .clearAndSetSemantics { },
    ) {
        if (doneCount > 0) {
            Box(
                modifier = Modifier
                    .weight(doneCount.toFloat())
                    .fillMaxHeight()
                    .background(doneColor),
            )
        }
        if (skipCount > 0) {
            Box(
                modifier = Modifier
                    .weight(skipCount.toFloat())
                    .fillMaxHeight()
                    .background(skipColor),
            )
        }
        if (noResponseCount > 0) {
            Box(modifier = Modifier.weight(noResponseCount.toFloat()))
        }
    }
}
