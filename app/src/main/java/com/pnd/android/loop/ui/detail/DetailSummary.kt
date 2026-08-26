package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.dayForLoop
import java.time.LocalDate

// ─────────────────────────────────────────────────────────────────────────────
// 요약 헤더 — 진입 즉시 보이는 것
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 화면 맨 위 요약. 카드로 감싸지 않아 "이 화면 전체의 머리말"로 읽히고, 아래 섹션 카드와 층이 갈린다.
 *
 * 이름과 색은 액션 바가 맡는다. 여기서는 "지금 켜져 있는가"와 "얼마나 잘 지키고 있는가"만 말한다 —
 * 무엇을 고치는 곳이 아니라 상태를 읽는 곳이다.
 *
 * 지표는 완료율·연속 두 칸으로 줄이고, "이번 주"는 목표 대비 진행 막대로 따로 세웠다. 세 칸으로
 * 나누면 글꼴을 키운 기기에서 "100%"나 "1,234일" 같은 값이 잘렸고, 주간 목표는 숫자 하나보다
 * 진행 막대로 보여야 남은 양이 읽힌다.
 */
@Composable
internal fun SummaryHeader(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    stats: DetailStats,
    onEnabledChange: (Boolean) -> Unit,
) {
    val accent = Color(loop.color).compositeOverOnSurface()

    Column(modifier = modifier.fillMaxWidth()) {
        ActiveStateToggle(
            modifier = Modifier.padding(top = 4.dp),
            enabled = loop.enabled,
            onEnabledChanged = onEnabledChange,
        )

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

        KpiRow(
            modifier = Modifier.padding(top = 20.dp),
            donePercent = stats.donePercent,
            streakDays = stats.streak.current,
            bestStreakDays = stats.streak.longest,
        )

        WeeklyGoalBar(
            modifier = Modifier.padding(top = 20.dp),
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
    }
}

/**
 * 활성 / 비활성 2분할 토글. 라벨 없는 스위치를 대신한다.
 *
 * 스위치는 켜짐·꺼짐이 트랙 색만으로 구분되는데, 이 앱의 스위치 색은 두 상태가 모두 무채색이라
 * 상세 화면에서 "지금 어느 쪽인지"가 한눈에 읽히지 않았다. 두 칸 중 하나가 채워지는 형태로 바꾸면
 * 현재 상태(채워진 칸)와 바꾸는 방법(반대 칸을 누른다)이 글자로 함께 드러난다.
 *
 * 두 칸은 라디오 버튼 한 묶음으로 읽히게 해, 스크린 리더에도 선택 여부가 전해진다. 이미 선택된
 * 칸을 눌러도 같은 값을 다시 저장하지는 않는다.
 */
@Composable
private fun ActiveStateToggle(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onEnabledChanged: (Boolean) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.large)
            .background(AppColor.surfaceContainer)
            .padding(all = 3.dp)
            .selectableGroup(),
    ) {
        ActiveStateSegment(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.Check,
            label = stringResource(id = R.string.detail_loop_state_active),
            selected = enabled,
            selectedColor = AppColor.primary,
            onClick = { if (!enabled) onEnabledChanged(true) },
        )
        ActiveStateSegment(
            modifier = Modifier.weight(1f),
            // 활성 ✓ / 비활성 ✕ 짝. 홈 카드 스위치의 손잡이 아이콘과 같은 짝이라 두 화면이 어긋나지 않는다.
            icon = Icons.Outlined.Close,
            label = stringResource(id = R.string.detail_loop_state_inactive),
            selected = !enabled,
            selectedColor = AppColor.error,
            onClick = { if (enabled) onEnabledChanged(false) },
        )
    }
}

/** [ActiveStateToggle]의 한 칸. 선택된 칸만 표면을 한 층 띄우고 색·굵기를 입힌다. */
@Composable
private fun ActiveStateSegment(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    selected: Boolean,
    selectedColor: Color,
    onClick: () -> Unit,
) {
    val contentColor = if (selected) selectedColor else AppColor.onSurface.copy(alpha = 0.45f)
    val isSelected = selected
    Row(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(if (selected) AppColor.surfaceElevated else Color.Transparent)
            .then(
                if (selected) {
                    Modifier.border(
                        width = 0.5.dp,
                        color = selectedColor.copy(alpha = 0.35f),
                        shape = RoundShapes.medium,
                    )
                } else {
                    Modifier
                }
            )
            .clickable(role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { this.selected = isSelected }
            .sizeIn(minHeight = MinTouchTarget - 6.dp)
            .padding(vertical = 9.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            tint = contentColor,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 6.dp),
            text = label,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.labelLarge.copy(
                color = contentColor,
                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
            ),
        )
    }
}

/**
 * 핵심 지표 두 칸: 완료율 · 연속. 세로 실선으로만 나눠 카드 없이도 한 묶음으로 읽힌다.
 * 최고 기록은 지금 기록을 넘어설 때만 연속 칸 아래에 작게 붙인다.
 */
@Composable
private fun KpiRow(
    modifier: Modifier = Modifier,
    donePercent: Int,
    streakDays: Int,
    bestStreakDays: Int,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        KpiCell(
            modifier = Modifier.weight(1f),
            value = "$donePercent%",
            label = stringResource(id = R.string.detail_kpi_done_rate),
        )
        KpiDivider()
        KpiCell(
            modifier = Modifier.weight(1f),
            value = stringResource(id = R.string.detail_kpi_streak_days, streakDays),
            label = stringResource(id = R.string.detail_kpi_streak),
            caption = if (bestStreakDays > streakDays) {
                stringResource(id = R.string.detail_streak_best, bestStreakDays)
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
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = value,
            // 글꼴을 키운 기기에서도 잘리지 않도록 두 줄까지 흐르게 둔다. 칸이 둘뿐이라
            // 대개는 한 줄로 들어간다.
            maxLines = 2,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.headlineSmall.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            modifier = Modifier.padding(top = 3.dp),
            text = label,
            textAlign = TextAlign.Center,
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
        if (caption != null) {
            Text(
                modifier = Modifier.padding(top = 1.dp),
                text = caption,
                textAlign = TextAlign.Center,
                style = AppTypography.labelSmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.35f),
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
    val ratio = stringResource(id = R.string.detail_kpi_week_ratio, weekly.done, weekly.target)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = label,
                style = AppTypography.labelMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.5f),
                ),
            )
            Text(
                text = ratio,
                style = AppTypography.labelLarge.copy(
                    color = if (weekly.isAchieved) accent else AppColor.onSurface,
                    fontWeight = FontWeight.Bold,
                ),
            )
        }
        Box(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(6.dp)
                .clip(CircleShape)
                .background(AppColor.onSurface.copy(alpha = 0.08f)),
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
 * 이번 주 월~일을 작은 알약으로 늘어놓는다.
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
                        contentDescription = "$dayLabel, $stateLabel"
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(CircleShape)
                        .background(fill)
                        .alpha(if (isActive && !isFuture) 1f else 0.4f),
                )
                Text(
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .alpha(if (date == today) 1f else 0.45f),
                    text = dayLabel,
                    style = AppTypography.labelSmall.copy(
                        color = AppColor.onSurface,
                        fontWeight = if (date == today) FontWeight.Bold else FontWeight.Normal,
                    ),
                )
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
