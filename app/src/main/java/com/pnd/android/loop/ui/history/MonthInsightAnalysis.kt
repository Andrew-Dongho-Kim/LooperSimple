package com.pnd.android.loop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.statisctics.investedDurationText
import com.pnd.android.loop.ui.theme.*
import com.pnd.android.loop.util.formatMonthDateDay
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

@Composable
internal fun MonthInsightAnalysis(report: MonthInsightReport, onOpenDate: (LocalDate) -> Unit, onOpenNotes: () -> Unit) {
    var timeMode by rememberSaveable { mutableStateOf(false) }
    var selectedEpoch by rememberSaveable { mutableStateOf(report.end.toEpochDay()) }
    var selectedWeekday by rememberSaveable { mutableStateOf(report.bestWeekday?.day?.value ?: 1) }
    val selected = report.days.firstOrNull { it.date.toEpochDay() == selectedEpoch }
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Column {
                Text(stringResource(R.string.mi_completion), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                Text("${(report.completionRate * 100).roundToInt()}%", style = AppTypography.headlineLarge)
            }
            Column(Modifier.weight(1f)) { InsightComparison(report) }
        }
        AppCard {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = !timeMode, onClick = { timeMode = false }, label = { Text(stringResource(R.string.mi_completion)) })
                FilterChip(selected = timeMode, onClick = { timeMode = true }, label = { Text(stringResource(R.string.mi_time)) })
            }
            InsightCalendar(report, selectedEpoch, timeMode) { selectedEpoch = it.toEpochDay() }
            if (selected != null) {
                HorizontalDivider(Modifier.padding(vertical = 12.dp), color = AppColor.outlineVariant)
                Text(selected.date.formatMonthDateDay(), style = AppTypography.titleSmall)
                if (selected.totalCount == 0) {
                    Text(stringResource(R.string.history_empty_day), style = AppTypography.bodyMedium, color = AppColor.onSurfaceVariant)
                } else {
                    Text(
                        stringResource(R.string.mi_selected_day, selected.doneCount, selected.totalCount, investedDurationText(selected.timeMs)),
                        style = AppTypography.bodyMedium, color = AppColor.onSurfaceVariant,
                    )
                    TextButton(onClick = { onOpenDate(selected.date) }) { Text(stringResource(R.string.mi_open_day)) }
                }
            }
        }
        AppCard {
            Text(stringResource(R.string.mi_weekday_pattern), style = AppTypography.titleMedium)
            Text(stringResource(R.string.mi_tap_weekday), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
            Row(
                Modifier.fillMaxWidth().padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.Bottom,
            ) {
                report.weekdays.forEach { weekday ->
                    val selectedDay = weekday.day.value == selectedWeekday
                    val sample = stringResource(R.string.mi_sample, weekday.doneCount, weekday.totalCount, weekday.observedDays)
                    val label = weekday.day.getDisplayName(TextStyle.FULL, Locale.getDefault()) + ", " + sample
                    Column(
                        Modifier.weight(1f).clip(RoundedCornerShape(8.dp))
                            .selectable(selectedDay, role = Role.Button, onClick = { selectedWeekday = weekday.day.value })
                            .semantics(mergeDescendants = true) { contentDescription = label }
                            .height(140.dp).padding(vertical = 6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Bottom,
                    ) {
                        Text(
                            weekday.rate?.let { "${(it * 100).roundToInt()}%" } ?: "—",
                            style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant,
                        )
                        Box(Modifier.height(80.dp * (weekday.rate ?: 0f)).width(18.dp)
                            .background(AppColor.primary.copy(alpha = if (selectedDay) 1f else 0.4f), RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)))
                        Text(
                            weekday.day.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
                            modifier = Modifier.padding(top = 6.dp), style = AppTypography.labelMedium,
                            color = if (selectedDay) AppColor.primary else AppColor.onSurfaceVariant,
                        )
                    }
                }
            }
            report.weekdays.firstOrNull { it.day.value == selectedWeekday }?.let {
                Text(
                    it.day.getDisplayName(TextStyle.FULL, Locale.getDefault()) + " · " +
                        stringResource(R.string.mi_sample, it.doneCount, it.totalCount, it.observedDays),
                    Modifier.padding(top = 12.dp), style = AppTypography.bodyMedium,
                )
                if (it.observedDays < 3 || it.totalCount < 5) {
                    Text(stringResource(R.string.mi_small_sample), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                }
            }
        }
        AppCard {
            Text(stringResource(R.string.mi_loops), style = AppTypography.titleMedium)
            Text(stringResource(R.string.mi_loop_comparison_hint), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
            report.loops.forEachIndexed { index, loop ->
                if (index > 0) HorizontalDivider(Modifier.padding(vertical = 12.dp), color = AppColor.outlineVariant)
                else Spacer(Modifier.height(12.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(loop.title, Modifier.weight(1f), style = AppTypography.bodyMedium)
                    Text("${(loop.rate * 100).roundToInt()}%", style = AppTypography.titleSmall)
                }
                LinearProgressIndicator(
                    progress = { loop.rate }, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).height(5.dp),
                    color = AppColor.primary, trackColor = AppColor.onSurface.copy(alpha = 0.08f),
                )
                Text(
                    stringResource(R.string.mi_loop_detail, loop.doneCount, loop.totalCount, investedDurationText(loop.timeMs)),
                    style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant,
                )
                Text(
                    loop.deltaPoints?.let { stringResource(R.string.mi_delta, it) } ?: stringResource(R.string.mi_loop_no_comparison),
                    style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant,
                )
            }
        }
        AppCard {
            Text(stringResource(R.string.mi_consistency), style = AppTypography.titleMedium)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InsightMetric(stringResource(R.string.mi_perfect_days), stringResource(R.string.mi_days, report.perfectDays), Modifier.weight(1f))
                InsightMetric(stringResource(R.string.mi_streak), stringResource(R.string.mi_days, report.longestStreak), Modifier.weight(1f))
            }
            Text(
                stringResource(R.string.mi_restarts, report.restartedCount, report.restartOpportunities.size),
                style = AppTypography.bodyMedium,
            )
            Text(stringResource(R.string.mi_restart_definition), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
            Text(
                stringResource(R.string.mi_timed_records, report.timedCount, report.doneCount),
                Modifier.padding(top = 12.dp), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant,
            )
        }
        InsightOutcomeBreakdown(report)
        if (report.notes.isNotEmpty()) {
            OutlinedButton(onClick = onOpenNotes, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Outlined.EditNote, null)
                Text(stringResource(R.string.mi_notes_count, report.notes.size), Modifier.padding(start = 8.dp))
            }
        }
    }
}

@Composable
private fun InsightCalendar(report: MonthInsightReport, selectedEpoch: Long, timeMode: Boolean, onSelect: (LocalDate) -> Unit) {
    val dates = achievementMonthDates(report.month)
    val byDate = remember(report) { report.days.associateBy { it.date } }
    val maxTime = report.days.maxOfOrNull { it.timeMs }?.coerceAtLeast(1L) ?: 1L
    Row(Modifier.fillMaxWidth()) {
        dates.take(7).forEach {
            Text(
                it.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                Modifier.weight(1f).padding(vertical = 8.dp), style = AppTypography.labelSmall,
                textAlign = TextAlign.Center, color = AppColor.onSurfaceVariant,
            )
        }
    }
    dates.chunked(7).forEach { week ->
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            week.forEach { date ->
                val day = byDate[date]
                val selected = date.toEpochDay() == selectedEpoch
                val enabled = day != null
                val level = if (timeMode) (day?.timeMs ?: 0).toFloat() / maxTime else day?.rate ?: 0f
                val description = date.formatMonthDateDay() + ", " + if (day == null) {
                    stringResource(R.string.mi_outside_period)
                } else if (day.totalCount == 0) stringResource(R.string.history_empty_day)
                else stringResource(R.string.mi_selected_day, day.doneCount, day.totalCount, investedDurationText(day.timeMs))
                Column(
                    Modifier.weight(1f).padding(vertical = 2.dp).clip(RoundedCornerShape(10.dp))
                        .background(if (level > 0) AppColor.primary.copy(alpha = 0.06f + level * 0.36f) else Color.Transparent)
                        .then(if (selected) Modifier.border(2.dp, AppColor.primary, RoundedCornerShape(10.dp)) else Modifier)
                        .selectable(selected, enabled = enabled, role = Role.Button, onClick = { onSelect(date) })
                        .semantics(mergeDescendants = true) { contentDescription = description }
                        .heightIn(min = 48.dp).padding(vertical = 6.dp),
                    verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        if (date.month == report.month.month) date.dayOfMonth.toString() else "",
                        style = AppTypography.bodyMedium,
                        color = if (enabled) AppColor.onSurface else AppColor.onSurfaceVariant.copy(alpha = 0.4f),
                    )
                    if (day?.hasNote == true) Icon(Icons.Outlined.EditNote, null, Modifier.size(12.dp), tint = AppColor.onSurfaceVariant)
                }
            }
        }
    }
    Row(
        Modifier.fillMaxWidth().padding(top = 10.dp), verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
        Text(if (timeMode) investedDurationText(0L) else "0%", style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant)
        (0..4).forEach {
            Box(Modifier.padding(start = 4.dp).size(9.dp).background(AppColor.primary.copy(alpha = 0.06f + it * 0.09f), RoundedCornerShape(2.dp)))
        }
        Text(
            if (timeMode) investedDurationText(report.days.maxOfOrNull { it.timeMs } ?: 0) else "100%",
            Modifier.padding(start = 4.dp), style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant,
        )
    }
    Text(
        stringResource(R.string.mi_calendar_note_legend),
        Modifier.fillMaxWidth().padding(top = 6.dp),
        style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant, textAlign = TextAlign.End,
    )
}
