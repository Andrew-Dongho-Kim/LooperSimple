package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.Flag
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.common.NO_WEEKLY_GOAL
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.formatYearMonthDateDays
import java.time.LocalDate

// ─────────────────────────────────────────────────────────────────────────────
// 스케줄 섹션 — "언제, 얼마나 자주"
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 스케줄 섹션. 접힌 상태의 요약이 "오전 7:00 – 오전 7:30 · 주중"처럼 그 자체로 답이 되게 했다.
 * 펼치면 하루 타임라인 · 활동 요일 · 주간 목표 · 생성일이 나온다.
 *
 * 고치는 일은 이 안에서 하지 않는다. 색 · 이름 · 시간 · 요일 · 목표는 액션 바의 연필 하나로
 * [LoopEditor] 에서 함께 고친다 — 섹션마다 편집 모드를 따로 두면 같은 값을 고치는 길이 화면에
 * 여러 개 생기고, 어느 길로 가야 하는지 사용자가 고민하게 된다.
 */
@Composable
internal fun ScheduleSection(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    today: LocalDate,
    createdDate: LocalDate,
    overlappingCount: Int,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val daysLabel = activeDaysLabel(activeDays = loop.activeDays)
    val summary = if (loop.isAnyTime) {
        "${stringResource(id = R.string.anytime)} · $daysLabel"
    } else {
        "${loop.startInDay.formatHourMinute()} – ${loop.endInDay.formatHourMinute()} · $daysLabel"
    }

    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.Schedule,
        title = stringResource(id = R.string.detail_schedule),
        summary = summary,
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        ScheduleDetails(
            loop = loop,
            today = today,
            createdDate = createdDate,
            overlappingCount = overlappingCount,
        )
    }
}

/** 활동 요일을 한 낱말로 줄인다. 매일/주중/주말이 아니면 "주 N일"로 센다. */
@Composable
internal fun activeDaysLabel(activeDays: Int): String = when (activeDays) {
    LoopDay.EVERYDAY -> stringResource(id = R.string.everyday)
    LoopDay.WEEKDAYS -> stringResource(id = R.string.weekdays)
    LoopDay.WEEKENDS -> stringResource(id = R.string.weekends)
    else -> stringResource(id = R.string.detail_days_per_week, activeDayCount(activeDays))
}

/** 활동 요일 전체를 풀어 읽어 주는 접근성 문구("활동 요일: 월, 화, 수"). */
@Composable
private fun activeDaysDescription(activeDays: Int): String {
    val names = LoopDay.ALL.mapIndexedNotNull { index, day ->
        if (activeDays.isOn(day)) {
            stringResource(id = DAYS_WITH_3CHARS_SUNDAY_FIRST[index])
        } else {
            null
        }
    }
    return stringResource(id = R.string.detail_active_days_desc, names.joinToString(", "))
}

/**
 * 스케줄 섹션의 본문.
 * 하루 중 활동 구간을 24시간 막대 위에 강조해 "언제 하는 습관인지"를 공간적으로 보여준다.
 * '언제든지' 루프는 고정 구간이 없으므로 막대를 그리지 않는다.
 */
@Composable
private fun ScheduleDetails(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    today: LocalDate,
    createdDate: LocalDate,
    overlappingCount: Int,
) {
    // "N일째"는 자정을 넘기면 하루 늘어야 한다. 그래서 오늘을 화면 밖에서 받아 온다.
    val dayCount = today.toEpochDay() - createdDate.toEpochDay() + 1
    val accent = Color(loop.color).compositeOverOnSurface()

    Column(modifier = modifier.fillMaxWidth()) {
        if (!loop.isAnyTime) {
            val durationMinutes =
                ((loop.endInDay - loop.startInDay) / MS_1MIN).toInt().coerceAtLeast(0)
            val durationText = if (durationMinutes >= 60) {
                stringResource(
                    id = R.string.stat_duration_hm,
                    durationMinutes / 60,
                    durationMinutes % 60,
                )
            } else {
                stringResource(id = R.string.stat_duration_m, durationMinutes)
            }
            Text(
                text = durationText,
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.6f),
                ),
            )

            DayTimeline(
                modifier = Modifier
                    .padding(top = 10.dp)
                    .fillMaxWidth(),
                startFraction = loop.startInDay.toFloat() / MS_1DAY,
                endFraction = loop.endInDay.toFloat() / MS_1DAY,
                accent = accent,
                contentDescription = stringResource(
                    id = R.string.detail_timeline_desc,
                    loop.startInDay.formatHourMinute(),
                    loop.endInDay.formatHourMinute(),
                ),
            )
            HourTicks(modifier = Modifier.padding(top = 6.dp))
        }

        ActiveDaysRow(
            modifier = Modifier.padding(top = if (loop.isAnyTime) 0.dp else 18.dp),
            activeDays = loop.activeDays,
            contentDescription = activeDaysDescription(activeDays = loop.activeDays),
        )

        Column(
            modifier = Modifier.padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            InfoRow(
                icon = Icons.Outlined.Flag,
                label = stringResource(id = R.string.detail_week_goal_label),
                value = if (loop.weeklyGoal > NO_WEEKLY_GOAL) {
                    stringResource(id = R.string.detail_week_goal_times, loop.weeklyGoal)
                } else {
                    stringResource(id = R.string.detail_week_goal_none)
                },
            )
            InfoRow(
                icon = Icons.Outlined.DateRange,
                label = stringResource(id = R.string.created_date),
                value = createdDate.formatYearMonthDateDays(),
                trailing = stringResource(id = R.string.n_days, dayCount),
            )
            // 겹치는 루프가 있으면 알려 준다. 같은 시간대에 몇 개가 몰려 있는지 모른 채
            // 시간을 옮길지 판단하기는 어렵다.
            if (overlappingCount > 0) {
                InfoRow(
                    icon = Icons.Outlined.Layers,
                    label = stringResource(id = R.string.detail_overlap_label),
                    value = stringResource(id = R.string.detail_overlap_count, overlappingCount),
                )
            }
        }
    }
}
