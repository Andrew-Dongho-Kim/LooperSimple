package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.dayForLoop
import java.time.LocalDate

// ─────────────────────────────────────────────────────────────────────────────
// 요약 헤더 — 진입 즉시 보이는 것
// ─────────────────────────────────────────────────────────────────────────────

/** 이번 주 목표 → 보조 성과 순으로 읽는 요약. */
@Composable
internal fun SummaryHeader(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    stats: DetailStats,
) {
    val accent = Color(loop.color).compositeOverOnSurface()

    Column(modifier = modifier.fillMaxWidth()) {
        // 꺼져 있을 때만 결과를 설명한다. 켜져 있을 때의 "정상 동작"은 굳이 한 줄을 쓰지 않는다.
        if (!loop.enabled) {
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = stringResource(id = R.string.detail_loop_active_off),
                style = AppTypography.bodySmall.copy(
                    color = AppColor.error.copy(alpha = 0.8f),
                ),
            )
        }

        WeeklyGoalBar(
            modifier = Modifier.padding(top = DetailSpacing.group),
            weekly = stats.weekly,
            accent = accent,
        )

        // 이번 주(월~일) 흐름. 완료(강조색)·건너뜀(옅음)·그 외를 색으로 구분한다.
        WeekStrip(
            modifier = Modifier.padding(top = 14.dp),
            doneStateByDate = stats.doneStateByDate,
            createdDate = stats.createdDate,
            today = stats.today,
            activeDays = loop.activeDays,
            accent = accent,
        )

        WeeklyTrendCaption(
            modifier = Modifier.padding(top = 10.dp),
            trend = stats.weekly.trend,
        )

        DetailCard(modifier = Modifier.padding(top = DetailSpacing.group)) {
            KpiRow(
                stats = stats,
            )
        }
    }
}

/**
 * 주간 목표를 보조하는 전체 완료율 · 연속 기록.
 * 최고 기록은 지금 기록을 넘어설 때만 연속 칸 아래에 작게 붙인다.
 */
@Composable
private fun KpiRow(
    modifier: Modifier = Modifier,
    stats: DetailStats,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KpiCell(
            modifier = Modifier.weight(1f).padding(end = 12.dp),
            value = if (stats.hasAnyRecord) "${stats.donePercent}%" else stringResource(R.string.detail_tile_no_data),
            label = stringResource(id = R.string.detail_overall_rate),
            caption = stringResource(R.string.detail_done_total, stats.totalCount, stats.doneCount),
        )
        KpiDivider()
        KpiCell(
            modifier = Modifier.weight(1f).padding(start = 12.dp),
            value = stringResource(id = R.string.detail_kpi_streak_days, stats.streak.current),
            label = stringResource(id = R.string.detail_kpi_streak),
            caption = if (stats.streak.longest > stats.streak.current) {
                stringResource(id = R.string.detail_streak_best, stats.streak.longest)
            } else {
                null
            },
        )
    }
}

@Composable
private fun KpiCell(
    modifier: Modifier = Modifier,
    value: String,
    label: String,
    caption: String? = null,
) {
    Column(
        modifier = modifier.semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.Start,
    ) {
        Text(
            text = label,
            style = AppTypography.labelMedium.copy(color = AppColor.onSurface.copy(alpha = 0.65f)),
        )
        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = value,
            // 글꼴을 키운 기기에서도 잘리지 않도록 두 줄까지 흐르게 둔다. 칸이 둘뿐이라
            // 대개는 한 줄로 들어간다.
            maxLines = 2,
            textAlign = TextAlign.Start,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.headlineLarge.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (caption != null) {
            Text(
                modifier = Modifier.padding(top = 1.dp),
                text = caption,
                textAlign = TextAlign.Start,
                style = AppTypography.labelSmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.6f),
                ),
            )
        }
    }
}

@Composable
private fun KpiDivider() {
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .width(0.5.dp)
            .background(AppColor.onSurface.copy(alpha = 0.10f)),
    )
}

/**
 * 이번 주 목표 대비 진행 막대.
 *
 * 예전에는 "이번 주 3/5"라는 숫자 한 쌍만 있었고, 분모는 늘 활동 요일 수였다. 주 3회면 충분한
 * 습관도 그러면 매일 실패로 보인다. 주간 목표를 정해 두면 그 값이 분모가 되고, 정하지 않았다면
 * 종전처럼 활동 요일 수를 쓴다.
 */
@Composable
private fun WeeklyGoalBar(
    modifier: Modifier = Modifier,
    weekly: WeeklyProgress,
    accent: Color,
) {
    val label = if (weekly.hasGoal) {
        stringResource(id = R.string.detail_week_goal_label)
    } else {
        stringResource(id = R.string.detail_kpi_this_week)
    }
    val suffix = if (weekly.target > 0) {
        stringResource(R.string.detail_week_progress_suffix, weekly.target)
    } else {
        stringResource(R.string.detail_week_done_suffix)
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Text(
            text = label,
            style = AppTypography.bodyMedium.copy(color = AppColor.onSurface.copy(alpha = 0.65f)),
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = buildAnnotatedString {
                append(weekly.done.toString())
                withStyle(AppTypography.titleMedium.toSpanStyle().copy(color = AppColor.onSurface.copy(alpha = 0.65f))) {
                    append(" ")
                    append(suffix)
                }
            },
            style = AppTypography.displayLarge.copy(color = AppColor.onSurface, fontWeight = FontWeight.Medium),
        )
        Box(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(AppColor.onSurface.copy(alpha = 0.08f))
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(weekly.fraction, 0f..1f) },
        ) {
            if (weekly.fraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(weekly.fraction)
                        .fillMaxHeight()
                        .clip(CircleShape)
                        .background(accent),
                )
            }
        }
    }
}

/**
 * 이번 주 월~일을 체크·날짜로 표시한다. 오늘은 테두리, 비활성 요일은 대시로 구분한다.
 *
 * 예전에는 "최근 7일"(오늘부터 거꾸로 6일)을 그렸는데, 바로 위 지표는 "이번 주"라고 말하고 있어
 * 두 값이 서로 다른 기간을 가리켰다. 같은 주를 보게 맞추고 요일 글자를 붙여, 어느 칸이 무슨
 * 요일인지 세어 보지 않아도 되게 했다.
 */
@Composable
private fun WeekStrip(
    modifier: Modifier = Modifier,
    doneStateByDate: Map<LocalDate, Int>,
    createdDate: LocalDate,
    today: LocalDate,
    activeDays: Int,
    accent: Color,
) {
    val doneLabel = stringResource(id = R.string.done)
    val skipLabel = stringResource(id = R.string.skip)
    val noRecordLabel = stringResource(id = R.string.detail_day_no_record)
    val notActiveLabel = stringResource(id = R.string.detail_day_not_active)
    // 루프 색은 사용자가 고른다. 밝은 색에도 완료 표시가 묻히지 않게 대비를 맞춘다.
    val checkColor = if (accent.luminance() > 0.179f) Color.Black else Color.White

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        weekDatesOf(today).forEach { date ->
            val state = doneStateByDate[date]
            val fill = when (state) {
                DoneState.DONE -> accent
                DoneState.SKIP -> AppColor.onSurface.copy(alpha = 0.25f)
                else -> AppColor.surfaceContainer
            }
            // 생성 이전이거나 비활성 요일은 흐리게 처리해 "해당 없음"을 구분한다.
            val isActive = !date.isBefore(createdDate) && activeDays.isOn(dayForLoop(date))
            val isFuture = date.isAfter(today)
            val dayLabel = stringResource(id = DAYS_WITH_3CHARS[date.dayOfWeek.value - 1])
            val stateLabel = when {
                !isActive -> notActiveLabel
                isFuture -> noRecordLabel
                state == DoneState.DONE -> doneLabel
                state == DoneState.SKIP -> skipLabel
                else -> noRecordLabel
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    // 색만으로 구분되던 칸이라, 스크린 리더에는 요일과 상태를 글로 말해 준다.
                    .clearAndSetSemantics {
                        contentDescription = "$dayLabel, ${date.dayOfMonth}, $stateLabel"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    modifier = Modifier
                        .alpha(if (date == today) 1f else 0.65f),
                    text = dayLabel,
                    style = AppTypography.labelSmall.copy(
                        color = AppColor.onSurface,
                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
                Box(
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(if (isActive && !isFuture) fill else Color.Transparent)
                        .then(if (date == today) Modifier.border(1.dp, accent, CircleShape) else Modifier),
                    contentAlignment = Alignment.Center,
                ) {
                    when {
                        isActive && !isFuture && state == DoneState.DONE -> Icon(
                            imageVector = Icons.Outlined.Check,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                            tint = checkColor,
                        )
                        else -> Text(
                            text = if (!isActive || state == DoneState.SKIP) "–" else date.dayOfMonth.toString(),
                            style = AppTypography.labelSmall.copy(color = AppColor.onSurface.copy(alpha = 0.65f)),
                        )
                    }
                }
            }
        }
    }
}

/** 주간 스트립 바로 아래 붙는, 지난주 같은 시점 대비 한 줄 피드백. */
@Composable
private fun WeeklyTrendCaption(
    modifier: Modifier = Modifier,
    trend: Int,
) {
    val trendRes = when {
        trend > 0 -> R.string.detail_week_trend_up
        trend < 0 -> R.string.detail_week_trend_down
        else -> R.string.detail_week_trend_same
    }
    Text(
        modifier = modifier,
        text = stringResource(id = trendRes),
        style = AppTypography.bodySmall.copy(
            color = AppColor.onSurface.copy(alpha = 0.5f),
        ),
    )
}
