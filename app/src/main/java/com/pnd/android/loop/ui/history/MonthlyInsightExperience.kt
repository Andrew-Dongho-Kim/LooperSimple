package com.pnd.android.loop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.statisctics.investedDurationText
import com.pnd.android.loop.ui.theme.*
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.roundToInt

/** One report drives the sheet and its full-screen analysis, with a single back stack. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonthlyInsightExperience(
    initialDate: LocalDate,
    minDate: LocalDate,
    viewModel: DailyAchievementViewModel,
    onOpenDate: (LocalDate) -> Unit,
    onDismiss: () -> Unit,
) {
    val today = LocalDate.now()
    var monthEpoch by rememberSaveable { mutableStateOf(initialDate.withDayOfMonth(1).toEpochDay()) }
    val month = YearMonth.from(LocalDate.ofEpochDay(monthEpoch))
    var route by rememberSaveable { mutableStateOf("summary") }
    var notesReturn by rememberSaveable { mutableStateOf("summary") }
    var help by rememberSaveable { mutableStateOf(false) }
    var retry by remember { mutableStateOf(0) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val analysisState = rememberSaveableStateHolder()
    val openNotes = { notesReturn = route; route = "notes" }
    val back = { route = if (route == "notes") notesReturn else "summary" }

    key(month, retry) {
        val state by remember { viewModel.flowMonthReport(month, today) }
            .collectAsState(initial = AchievementLoadState.Loading)
        val report = (state as? AchievementLoadState.Ready)?.value
        val monthHeader: @Composable () -> Unit = {
            InsightMonthHeader(
                month, minDate, today,
                onMonth = { monthEpoch = it.atDay(1).toEpochDay() },
                onHelp = { help = true },
            )
        }
        val content: @Composable () -> Unit = {
            when (val result = state) {
                AchievementLoadState.Loading -> HistoryLoading()
                AchievementLoadState.Error -> HistoryError { retry++ }
                is AchievementLoadState.Ready -> {
                    if (result.value.totalCount == 0) {
                        Text(
                            stringResource(R.string.mi_empty),
                            Modifier.fillMaxWidth().padding(vertical = 32.dp),
                            textAlign = TextAlign.Center, color = AppColor.onSurfaceVariant,
                        )
                    } else {
                        when (route) {
                            "summary" -> InsightSummary(result.value, { route = "analysis" }, openNotes)
                            "notes" -> InsightNotes(result.value)
                            else -> analysisState.SaveableStateProvider(month.toString()) {
                                MonthInsightAnalysis(
                                    report = result.value,
                                    onOpenDate = { onOpenDate(it); onDismiss() },
                                    onOpenNotes = openNotes,
                                )
                            }
                        }
                    }
                }
            }
        }
        if (route == "summary") {
            ModalBottomSheet(
                onDismissRequest = onDismiss,
                sheetState = sheetState,
                containerColor = AppColor.surfaceElevated,
                contentColor = AppColor.onSurface,
            ) {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth(),
                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    item { monthHeader() }
                    item { content() }
                }
            }
        } else {
            Dialog(
                onDismissRequest = back,
                properties = DialogProperties(usePlatformDefaultWidth = false, decorFitsSystemWindows = false),
            ) {
                Scaffold(containerColor = AppColor.background, contentColor = AppColor.onSurface) { padding ->
                    Column(Modifier.fillMaxSize().padding(padding)) {
                        Row(
                            Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            IconButton(onClick = back) {
                                Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.history_back))
                            }
                            Text(
                                stringResource(if (route == "notes") R.string.mi_notes else R.string.mi_analysis),
                                Modifier.weight(1f), style = AppTypography.titleLarge,
                            )
                            IconButton(onClick = { help = true }) {
                                Icon(Icons.Outlined.Info, stringResource(R.string.mi_definitions))
                            }
                        }
                        if (route == "notes" && report != null) {
                            InsightNotes(report)
                        } else {
                            LazyColumn(
                                Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 28.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                item { monthHeader() }
                                item { content() }
                            }
                        }
                    }
                }
            }
        }
    }
    if (help) {
        AlertDialog(
            onDismissRequest = { help = false },
            title = { Text(stringResource(R.string.mi_definitions)) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    item { Text(stringResource(R.string.mi_definition_rate)) }
                    item { Text(stringResource(R.string.mi_definition_comparison)) }
                    item { Text(stringResource(R.string.mi_definition_time)) }
                    item { Text(stringResource(R.string.mi_definition_streak)) }
                    item { Text(stringResource(R.string.mi_definition_samples)) }
                }
            },
            confirmButton = { TextButton(onClick = { help = false }) { Text(stringResource(R.string.history_close)) } },
        )
    }
}

@Composable
private fun InsightMonthHeader(
    month: YearMonth, minDate: LocalDate, today: LocalDate,
    onMonth: (YearMonth) -> Unit, onHelp: () -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(month.atDay(1).formatYearMonth(), style = AppTypography.headlineMedium)
                Text(
                    stringResource(R.string.mi_period, month.atDay(1).formatMonthDateDay(), minOf(month.atEndOfMonth(), today).formatMonthDateDay()),
                    style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant,
                )
            }
            IconButton(onClick = { onMonth(month.minusMonths(1)) }, enabled = month > YearMonth.from(minDate)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, stringResource(R.string.history_previous_month))
            }
            IconButton(onClick = { onMonth(month.plusMonths(1)) }, enabled = month < YearMonth.from(today)) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, stringResource(R.string.history_next_month))
            }
        }
        TextButton(onClick = onHelp, contentPadding = PaddingValues(0.dp)) {
            Text(stringResource(R.string.mi_definitions), style = AppTypography.labelMedium)
        }
    }
}

@Composable
internal fun InsightComparison(report: MonthInsightReport) {
    val delta = report.deltaPoints
    if (delta == null) {
        Text(stringResource(R.string.mi_no_comparison), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
        return
    }
    Text(
        stringResource(R.string.mi_delta, delta),
        color = if (delta > 0) AppColor.primary else AppColor.onSurfaceVariant,
        style = AppTypography.bodyMedium,
    )
    Text(
        stringResource(
            R.string.mi_comparison_period,
            report.month.atDay(1).formatMonthDateDay(), report.comparison.currentEnd.formatMonthDateDay(),
            report.comparison.previousStart.formatMonthDateDay(), report.comparison.previousEnd.formatMonthDateDay(),
        ),
        style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant,
    )
}

@Composable
private fun InsightSummary(report: MonthInsightReport, onAnalysis: () -> Unit, onNotes: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        Row(
            Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            val progressLabel = stringResource(R.string.achievement_done_ratio, report.doneCount, report.totalCount)
            Box(
                Modifier.size(104.dp).semantics { contentDescription = progressLabel },
                contentAlignment = Alignment.Center,
            ) {
                AchievementRing(report.completionRate, AppColor.primary, diameter = 104.dp, stroke = 7.dp)
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("${(report.completionRate * 100).roundToInt()}%", style = AppTypography.headlineLarge)
                    Text(stringResource(R.string.mi_completion), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(stringResource(R.string.mi_small_steps, report.doneCount), style = AppTypography.titleMedium)
                Text(stringResource(R.string.mi_planned, report.totalCount), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                InsightComparison(report)
            }
        }
        InsightOutcomeBreakdown(report)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            InsightMetric(stringResource(R.string.mi_active_days), stringResource(R.string.mi_days, report.activeDays), Modifier.weight(1f), onAnalysis)
            InsightMetric(stringResource(R.string.mi_time), investedDurationText(report.timeMs), Modifier.weight(1f), onAnalysis)
            InsightMetric(stringResource(R.string.mi_streak), stringResource(R.string.mi_days, report.longestStreak), Modifier.weight(1f), onAnalysis)
        }
        Column {
            Text(stringResource(R.string.mi_discoveries), style = AppTypography.titleMedium)
            val best = report.bestWeekday
            if (best != null) {
                InsightLink(
                    Icons.Outlined.CalendarMonth,
                    stringResource(R.string.mi_best_weekday, best.day.getDisplayName(TextStyle.FULL, Locale.getDefault())),
                    stringResource(R.string.mi_sample, best.doneCount, best.totalCount, best.observedDays),
                    onAnalysis,
                )
            }
            val improved = report.mostImproved
            if (improved != null) {
                InsightLink(
                    Icons.Outlined.Insights,
                    stringResource(R.string.mi_improved_loop, improved.title),
                    stringResource(R.string.mi_loop_gain, improved.deltaPoints ?: 0, improved.doneCount, improved.totalCount),
                    onAnalysis,
                )
            }
            if (best == null && improved == null) {
                Text(
                    stringResource(R.string.mi_insufficient),
                    Modifier.padding(vertical = 12.dp), style = AppTypography.bodyMedium, color = AppColor.onSurfaceVariant,
                )
            }
        }
        Column {
            if (report.notes.isNotEmpty()) {
                Button(onClick = onNotes, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Outlined.EditNote, null)
                    Text(stringResource(R.string.mi_notes_count, report.notes.size), Modifier.padding(start = 8.dp))
                }
            }
            TextButton(onClick = onAnalysis, modifier = Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.mi_analysis))
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null)
            }
        }
    }
}

@Composable
internal fun InsightMetric(label: String, value: String, modifier: Modifier = Modifier, onClick: (() -> Unit)? = null) {
    Column(
        modifier.then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .heightIn(min = 64.dp).padding(vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(5.dp),
    ) {
        Text(label, style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
        Text(value, style = AppTypography.titleLarge)
    }
}

@Composable
internal fun InsightLink(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(Modifier.size(34.dp).background(AppColor.primary.copy(alpha = 0.09f), CircleShape), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = AppColor.primary, modifier = Modifier.size(18.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = AppTypography.bodyMedium)
            Text(subtitle, style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = AppColor.onSurfaceVariant)
    }
}

@Composable
internal fun InsightOutcomeBreakdown(report: MonthInsightReport) {
    val counts = listOf(
        Triple(R.string.done, report.doneCount, AppColor.primary),
        Triple(R.string.skip, report.skippedCount, AppColor.onSurfaceVariant),
        Triple(R.string.history_no_response, report.pendingCount, AppColor.onSurface.copy(alpha = 0.25f)),
        Triple(R.string.history_in_progress, report.inProgressCount, AppColor.primary.copy(alpha = 0.45f)),
    )
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(Modifier.fillMaxWidth().height(6.dp), horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            counts.filter { it.second > 0 }.forEach { (_, count, color) ->
                Box(Modifier.weight(count.toFloat()).fillMaxHeight().background(color, CircleShape))
            }
        }
        // Two columns keep four statuses readable at large font scales.
        counts.chunked(2).forEach { pair ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEach { (label, count, color) ->
                    Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.size(6.dp).background(color, CircleShape))
                        Text(stringResource(R.string.mi_status_count, stringResource(label), count), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightNotes(report: MonthInsightReport) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        item { Text(report.month.atDay(1).formatYearMonth(), style = AppTypography.titleLarge) }
        if (report.notes.isEmpty()) item { Text(stringResource(R.string.mi_no_notes), color = AppColor.onSurfaceVariant) }
        items(report.notes, key = { "${it.loopId}:${it.date}" }) { note ->
            AppCard {
                Text(note.title, style = AppTypography.titleSmall)
                Text(note.date.toLocalDate().formatMonthDateDay(), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                Text(note.retrospect, Modifier.padding(top = 10.dp), style = AppTypography.bodyMedium)
            }
        }
    }
}
