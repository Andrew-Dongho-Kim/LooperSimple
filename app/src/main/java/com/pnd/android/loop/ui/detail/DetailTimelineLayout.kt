package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.state.isRespond
import com.pnd.android.loop.state.stateLabelRes
import com.pnd.android.loop.ui.common.HistoryCalculationNote
import com.pnd.android.loop.ui.theme.*
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

private val JournalCardShape = RoundedCornerShape(22.dp)
private val JournalCellShape = RoundedCornerShape(14.dp)

@Composable
private fun JournalCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier.fillMaxWidth().clip(JournalCardShape)
            .background(AppColor.surfaceElevated)
            .border(1.dp, AppColor.onSurface.copy(alpha = 0.06f), JournalCardShape)
            .padding(16.dp),
        content = content,
    )
}

@Composable
internal fun TimelineSummaryHeader(
    loop: LoopBase,
    stats: DetailStats,
    responses: Map<LocalDate, LoopDoneVo>,
) {
    val accent = Color(loop.color).compositeOverOnSurface()
    val month = YearMonth.from(stats.today)
    val monthRecords = remember(responses, month, stats.today) {
        responses.filter { (date, response) ->
            YearMonth.from(date) == month && date <= stats.today && response.done == DoneState.DONE
        }
    }
    val monthlyDuration = remember(monthRecords) {
        monthRecords.mapNotNull { (date, response) -> recordedTimeOf(response, date) }
            .filter { it.source == LoopDoneVo.TimeSource.MEASURED || it.source == LoopDoneVo.TimeSource.USER_ENTERED }
            .mapNotNull { it.durationMs }.takeIf { it.isNotEmpty() }?.sum()
    }
    Column(Modifier.fillMaxWidth()) {
        Text(
            stringResource(R.string.detail_routine_eyebrow),
            style = AppTypography.labelSmall, color = accent,
        )
        Text(
            loop.title, modifier = Modifier.padding(top = 8.dp),
            style = AppTypography.headlineLarge, color = AppColor.onSurface,
        )
        Text(
            stringResource(R.string.detail_routine_caption),
            modifier = Modifier.padding(top = 6.dp),
            style = AppTypography.bodySmall, color = AppColor.onSurface.copy(alpha = 0.55f),
        )
        if (!loop.enabled) {
            Text(stringResource(R.string.detail_loop_active_off),
                modifier = Modifier.padding(top = 8.dp),
                style = AppTypography.bodySmall, color = AppColor.error)
        }
        Row(
            Modifier.fillMaxWidth().padding(vertical = 24.dp).height(IntrinsicSize.Min),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            SummaryMetric(
                Modifier.weight(1.3f), stringResource(R.string.detail_month_recorded_time),
                monthlyDuration?.let { journalDurationText(it) } ?: stringResource(R.string.detail_tile_no_data), accent,
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(AppColor.onSurface.copy(alpha = 0.08f)))
            SummaryMetric(
                Modifier.weight(1f), stringResource(R.string.detail_month_recorded_days),
                stringResource(R.string.detail_kpi_streak_days, monthRecords.size), accent,
            )
            Box(Modifier.width(1.dp).fillMaxHeight().background(AppColor.onSurface.copy(alpha = 0.08f)))
            SummaryMetric(
                Modifier.weight(1f), stringResource(R.string.detail_kpi_streak),
                stringResource(R.string.detail_kpi_streak_days, stats.streak.current), accent,
            )
        }
        // Keep the weekly goal, but remove the second, non-interactive calendar strip.
        Row(
            Modifier.fillMaxWidth().clip(JournalCellShape)
                .background(accent.copy(alpha = 0.08f)).padding(horizontal = 14.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(stringResource(if (stats.weekly.hasGoal) R.string.detail_week_goal_label else R.string.detail_kpi_this_week),
                Modifier.weight(1f), style = AppTypography.bodySmall, color = AppColor.onSurface)
            Text(
                stringResource(R.string.detail_kpi_week_ratio, stats.weekly.done, stats.weekly.target),
                style = AppTypography.titleMedium, color = accent,
            )
            HistoryCalculationNote(stats.hasEstimatedHistory)
        }
        if (stats.weekly.target > 0) {
            Box(Modifier.padding(top = 8.dp).fillMaxWidth().height(3.dp).clip(CircleShape)
                .background(accent.copy(alpha = 0.1f))
                .semantics { progressBarRangeInfo = ProgressBarRangeInfo(stats.weekly.fraction, 0f..1f) }) {
                if (stats.weekly.fraction > 0) Box(Modifier.fillMaxWidth(stats.weekly.fraction).fillMaxHeight().background(accent))
            }
        }
        stats.pendingGoal?.let { goal ->
            Text(
                stringResource(if (goal > 0) R.string.history_goal_next_week else R.string.history_goal_removed_next_week, goal),
                Modifier.padding(top = 8.dp), style = AppTypography.bodySmall,
                color = AppColor.onSurface.copy(alpha = 0.6f),
            )
        }
    }
}

@Composable
private fun SummaryMetric(modifier: Modifier, label: String, value: String, accent: Color) {
    Column(modifier.semantics(mergeDescendants = true) {}) {
        Text(label, style = AppTypography.labelSmall, color = AppColor.onSurface.copy(alpha = 0.55f))
        Text(value, Modifier.padding(top = 8.dp), style = AppTypography.titleLarge, color = accent)
    }
}

@Composable
internal fun JournalCalendarCard(
    stats: DetailStats,
    accent: Color,
    selectedDate: LocalDate,
    visibleDate: LocalDate,
    monthly: Boolean,
    onMonthlyChange: (Boolean) -> Unit,
    onNavigate: (LocalDate) -> Unit,
    onSelect: (LocalDate) -> Unit,
    onToday: () -> Unit,
) {
    val month = YearMonth.from(visibleDate)
    val week = weekDatesOf(visibleDate)
    val canPrev = if (monthly) month > YearMonth.from(stats.createdDate) else week.first() > stats.createdDate
    val canNext = if (monthly) month < YearMonth.from(stats.today) else week.last() < stats.today
    JournalCard {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(visibleDate.formatYearMonth(), Modifier.weight(1f), style = AppTypography.titleMedium, color = AppColor.onSurface)
            Row(Modifier.clip(RoundedCornerShape(12.dp)).background(AppColor.onSurface.copy(alpha = 0.045f))
                .padding(3.dp).selectableGroup()) {
                CalendarModeButton(stringResource(R.string.detail_calendar_week), !monthly, accent) { onMonthlyChange(false) }
                CalendarModeButton(stringResource(R.string.detail_calendar_month), monthly, accent) { onMonthlyChange(true) }
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            NavArrowButton(
                icon = Icons.Outlined.ChevronLeft,
                contentDescription = stringResource(if (monthly) R.string.detail_prev_month else R.string.detail_prev_week),
                enabled = canPrev,
                onClick = { onNavigate(if (monthly) visibleDate.minusMonths(1) else visibleDate.minusWeeks(1)) },
            )
            TextActionButton(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.detail_today), onClick = onToday,
            )
            NavArrowButton(
                icon = Icons.Outlined.ChevronRight,
                contentDescription = stringResource(if (monthly) R.string.detail_next_month else R.string.detail_next_week),
                enabled = canNext,
                onClick = { onNavigate(if (monthly) visibleDate.plusMonths(1) else visibleDate.plusWeeks(1)) },
            )
        }
        if (monthly) {
            Row(Modifier.fillMaxWidth().clearAndSetSemantics {}) {
                DAYS_WITH_3CHARS.forEach { label ->
                    Box(Modifier.weight(1f).padding(vertical = 8.dp), contentAlignment = Alignment.Center) {
                        Text(stringResource(label), style = AppTypography.labelSmall, color = AppColor.onSurface.copy(alpha = 0.45f))
                    }
                }
            }
            val blanks = month.atDay(1).dayOfWeek.value - 1
            val rows = (blanks + month.lengthOfMonth() + 6) / 7
            repeat(rows) { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    repeat(7) { column ->
                        val day = row * 7 + column - blanks + 1
                        if (day in 1..month.lengthOfMonth()) {
                            JournalDateCell(Modifier.weight(1f), month.atDay(day), stats, selectedDate, accent, false, onSelect)
                        } else Spacer(Modifier.weight(1f))
                    }
                }
            }
        } else {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                week.forEach { date ->
                    JournalDateCell(Modifier.weight(1f), date, stats, selectedDate, accent, true, onSelect)
                }
            }
        }
        Text(
            stringResource(R.string.detail_calendar_legend), Modifier.padding(top = 12.dp),
            style = AppTypography.labelSmall, color = AppColor.onSurface.copy(alpha = 0.5f),
        )
    }
}

@Composable
private fun CalendarModeButton(text: String, selected: Boolean, accent: Color, onClick: () -> Unit) {
    Box(
        Modifier.clip(RoundedCornerShape(9.dp))
            .background(if (selected) AppColor.surfaceElevated else Color.Transparent)
            .selectable(selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = 48.dp).padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = AppTypography.labelLarge, color = if (selected) accent else AppColor.onSurface.copy(alpha = 0.45f))
    }
}

@Composable
private fun JournalDateCell(
    modifier: Modifier,
    date: LocalDate,
    stats: DetailStats,
    selectedDate: LocalDate,
    accent: Color,
    showWeekday: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val chosen = date == selectedDate
    val enabled = date in stats.createdDate..stats.today
    val state = stats.doneStateByDate[date]
    val hasMemo = date in stats.memoDates
    val foreground = when {
        chosen -> if (accent.luminance() > 0.179f) Color.Black else Color.White
        !enabled -> AppColor.onSurface.copy(alpha = 0.25f)
        else -> AppColor.onSurface
    }
    val description = listOfNotNull(
        date.formatMonthDateDay(),
        if (date == stats.today) stringResource(R.string.detail_today) else null,
        stringResource(state.stateLabelRes()),
        if (hasMemo) stringResource(R.string.detail_has_memo) else null,
    ).joinToString(", ")
    Column(
        modifier.padding(vertical = 3.dp).clip(JournalCellShape)
            .background(when { chosen -> accent; state == DoneState.DONE -> accent.copy(alpha = 0.09f); else -> Color.Transparent })
            .then(if (date == stats.today && !chosen) Modifier.border(1.dp, accent.copy(alpha = 0.55f), JournalCellShape) else Modifier)
            .selectable(chosen, enabled = enabled, role = Role.Button, onClick = { onSelect(date) })
            .semantics(mergeDescendants = true) { contentDescription = description }
            .heightIn(min = if (showWeekday) 84.dp else 48.dp).padding(vertical = 7.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(5.dp, Alignment.CenterVertically),
    ) {
        if (showWeekday) Text(
            stringResource(DAYS_WITH_3CHARS[date.dayOfWeek.value - 1]),
            Modifier.clearAndSetSemantics {}, style = AppTypography.labelSmall, color = foreground.copy(alpha = if (enabled) 0.7f else 1f),
        )
        Text(date.dayOfMonth.toString(), Modifier.clearAndSetSemantics {}, style = AppTypography.bodyMedium,
            fontWeight = if (chosen || date == stats.today) FontWeight.Bold else FontWeight.Medium, color = foreground)
        Row(Modifier.height(12.dp).clearAndSetSemantics {}, horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
            if (state.isRespond()) Text(if (state == DoneState.DONE) "✓" else "−", style = AppTypography.labelSmall, color = foreground)
            if (hasMemo) Box(Modifier.size(3.dp).clip(CircleShape).background(foreground))
        }
    }
}

@Composable
private fun JournalSectionHeading(title: String, date: LocalDate) {
    Column(Modifier.fillMaxWidth().padding(bottom = 12.dp)) {
        Text(title, style = AppTypography.titleMedium, color = AppColor.onSurface)
        Text(date.formatMonthDateDay(), Modifier.padding(top = 4.dp), style = AppTypography.labelSmall, color = AppColor.onSurface.copy(alpha = 0.5f))
    }
}

@Composable
internal fun RecordedDayCard(
    date: LocalDate,
    response: LoopDoneVo?,
    state: Int?,
    accent: Color,
) {
    val interval = remember(response, date) { response?.let { recordedTimeOf(it, date) } }
    // Schedule-only and unanswered records never masquerade as completed activity.
    val hasActivity = response != null && (response.isRespond() || interval?.running == true)
    Column {
        JournalSectionHeading(stringResource(R.string.detail_day_timeline), date)
        JournalCard {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(state.stateLabelRes()), Modifier.weight(1f), style = AppTypography.bodySmall, color = AppColor.onSurface)
                if (hasActivity && interval?.durationMs != null) {
                    Text(
                        journalDurationText(interval.durationMs),
                        Modifier.clip(CircleShape).background(accent.copy(alpha = 0.1f)).padding(horizontal = 10.dp, vertical = 5.dp),
                        style = AppTypography.labelSmall, color = accent,
                    )
                }
            }
            if (hasActivity && interval != null) {
                val format = DateTimeFormatter.ofPattern("HH:mm")
                val start = interval.start.format(format)
                val end = interval.end?.format(format)
                val endLabel = when {
                    interval.running -> stringResource(R.string.detail_time_running)
                    end == null -> stringResource(R.string.detail_time_end_unknown)
                    interval.endDayOffset == 1L -> stringResource(R.string.detail_time_next_day, end)
                    interval.endDayOffset > 0 -> stringResource(R.string.detail_time_day_offset, interval.endDayOffset, end)
                    else -> end
                }
                Row(Modifier.fillMaxWidth().padding(top = 18.dp).height(IntrinsicSize.Min), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Column(Modifier.width(IntrinsicSize.Max).widthIn(min = 46.dp).fillMaxHeight().clearAndSetSemantics {}, horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(start, style = AppTypography.labelSmall, color = AppColor.onSurface.copy(alpha = 0.5f))
                        Box(Modifier.padding(top = 8.dp).size(6.dp).clip(CircleShape).background(accent))
                        Box(Modifier.weight(1f).width(1.dp).heightIn(min = 25.dp).background(accent.copy(alpha = 0.25f)))
                        Box(Modifier.size(6.dp).clip(CircleShape).background(accent.copy(alpha = 0.4f)))
                        Text(end ?: "–", Modifier.padding(top = 8.dp), style = AppTypography.labelSmall, color = AppColor.onSurface.copy(alpha = 0.5f))
                    }
                    Column(
                        Modifier.weight(1f).clip(JournalCellShape).background(accent.copy(alpha = 0.08f))
                            .padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text("$start – $endLabel", style = AppTypography.titleMedium, color = AppColor.onSurface)
                        Text(
                            stringResource(when (interval.source) {
                                LoopDoneVo.TimeSource.MEASURED -> R.string.detail_time_measured
                                LoopDoneVo.TimeSource.USER_ENTERED -> R.string.detail_time_entered
                                LoopDoneVo.TimeSource.PLANNED -> R.string.detail_time_planned
                                else -> R.string.detail_time_legacy
                            }),
                            style = AppTypography.bodySmall, color = AppColor.onSurface.copy(alpha = 0.6f),
                        )
                    }
                }
            } else {
                Text(stringResource(if (hasActivity) R.string.detail_time_unknown else R.string.detail_day_empty),
                    Modifier.padding(top = 18.dp, bottom = 8.dp), style = AppTypography.bodySmall, color = AppColor.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
internal fun JournalMemoCard(date: LocalDate, text: String, onEdit: () -> Unit) {
    Column {
        JournalSectionHeading(stringResource(R.string.detail_day_note), date)
        JournalCard {
            Text(
                text.ifBlank { stringResource(R.string.detail_note_empty) },
                style = AppTypography.bodyMedium, color = AppColor.onSurface.copy(alpha = if (text.isBlank()) 0.45f else 0.8f),
            )
            TextActionButton(
                Modifier.padding(top = 10.dp),
                text = stringResource(if (text.isBlank()) R.string.detail_write_note else R.string.detail_edit_note),
                onClick = onEdit,
            )
        }
    }
}

@Composable
internal fun journalDurationText(ms: Long): String {
    val minutes = ms / 60_000
    return when {
        minutes == 0L && ms > 0 -> stringResource(R.string.detail_time_under_minute)
        minutes < 60 -> stringResource(R.string.duration_minutes, minutes)
        minutes % 60 == 0L -> stringResource(R.string.duration_hours, minutes / 60)
        else -> stringResource(R.string.duration_hours_minutes, minutes / 60, minutes % 60)
    }
}
