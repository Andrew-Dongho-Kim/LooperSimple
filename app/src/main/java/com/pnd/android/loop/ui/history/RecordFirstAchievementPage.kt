package com.pnd.android.loop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.data.FullLoopVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.isDone
import com.pnd.android.loop.data.isSkip
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.common.NavigationBarFadingEdge
import com.pnd.android.loop.ui.common.StatusBarFadingEdge
import com.pnd.android.loop.ui.common.backdropSource
import com.pnd.android.loop.ui.common.isPortrait
import com.pnd.android.loop.ui.common.rememberBackdropState
import com.pnd.android.loop.ui.common.rememberListCollapseProgress
import com.pnd.android.loop.ui.common.supportsBackdropBlur
import com.pnd.android.loop.ui.common.appCardSurface
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatStartEndTime
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.toLocalDate
import java.time.LocalDate
import java.time.YearMonth
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
internal fun RecordFirstAchievementPage(
    modifier: Modifier = Modifier,
    achievementViewModel: DailyAchievementViewModel = hiltViewModel(),
    onNavigateToLoopDetail: (Int) -> Unit,
    onNavigateUp: () -> Unit,
) {
    val today = LocalDate.now()
    val minDate by achievementViewModel.flowMinCreatedDate.collectAsState(initial = today)
    var selectedEpoch by rememberSaveable { mutableStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpoch)
    var calendarExpanded by rememberSaveable { mutableStateOf(false) }
    var calendarMonthEpoch by rememberSaveable { mutableStateOf(selectedDate.withDayOfMonth(1).toEpochDay()) }
    val calendarMonth = YearMonth.from(LocalDate.ofEpochDay(calendarMonthEpoch))
    var dayRetry by remember { mutableStateOf(0) }
    var showMonth by rememberSaveable { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val onSelectDate: (LocalDate) -> Unit = { date ->
        if (!date.isBefore(minDate) && !date.isAfter(today)) {
            selectedEpoch = date.toEpochDay()
            calendarMonthEpoch = date.withDayOfMonth(1).toEpochDay()
        }
    }
    val portrait = LocalConfiguration.current.isPortrait()
    val contentWindowInsets = if (portrait) {
        ScaffoldDefaults.contentWindowInsets.exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.statusBars).exclude(WindowInsets.ime)
    } else {
        ScaffoldDefaults.contentWindowInsets.add(WindowInsets.displayCutout)
            .exclude(WindowInsets.statusBars).exclude(WindowInsets.ime)
    }
    val headerHeight = achievementHeaderExpandedHeight(WindowInsets.statusBars.asPaddingValues().calculateTopPadding())
    val bottomInset = if (portrait) WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() else 0.dp
    val backdrop = rememberBackdropState()
    val headerBackdrop = if (supportsBackdropBlur) backdrop else null
    val collapseProgress by rememberListCollapseProgress(listState, AchievementHeaderCollapseDistance)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppColor.background,
        contentColor = AppColor.onSurface,
        contentWindowInsets = contentWindowInsets,
    ) { insets ->
        Box(Modifier.fillMaxSize().padding(insets)) {
            // Date navigation and records share one scroll container, including on small/landscape screens.
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize()
                    .then(headerBackdrop?.let { Modifier.backdropSource(it) } ?: Modifier)
                    .background(AppColor.background),
                contentPadding = PaddingValues(
                    start = Dimens.screenHorizontalPadding,
                    end = Dimens.screenHorizontalPadding,
                    top = headerHeight,
                    bottom = 24.dp + bottomInset,
                ),
                verticalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                item(key = "date-navigation") {
                    AchievementDateNavigation(
                        selectedDate = selectedDate,
                        today = today,
                        minDate = minDate,
                        expanded = calendarExpanded,
                        month = calendarMonth,
                        onMonthChanged = { calendarMonthEpoch = it.atDay(1).toEpochDay() },
                        onDateSelected = onSelectDate,
                        viewModel = achievementViewModel,
                    )
                }
                item(key = "month") {
                    key(YearMonth.from(selectedDate)) {
                        MonthSummaryEntry(selectedDate, achievementViewModel) { showMonth = true }
                    }
                }
                item(key = "day") {
                    key(selectedEpoch, dayRetry) {
                        val state by remember {
                            achievementViewModel.flowDay(selectedDate)
                        }.collectAsState(initial = AchievementLoadState.Loading)
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                selectedDate.formatMonthDateDay(),
                                modifier = Modifier.padding(top = 8.dp),
                                style = AppTypography.titleLarge,
                            )
                            when (val result = state) {
                                AchievementLoadState.Loading -> HistoryLoading()
                                AchievementLoadState.Error -> HistoryError { dayRetry++ }
                                is AchievementLoadState.Ready -> DayRecords(
                                    date = selectedDate,
                                    loops = result.value,
                                    onNavigateToLoopDetail = onNavigateToLoopDetail,
                                )
                            }
                        }
                    }
                }
            }
            StatusBarFadingEdge(modifier = Modifier.align(Alignment.TopCenter))
            if (portrait) NavigationBarFadingEdge(modifier = Modifier.align(Alignment.BottomCenter))
            RecordFirstAchievementHeader(
                title = (if (calendarExpanded) calendarMonth.atDay(1) else selectedDate).formatYearMonth(),
                progress = collapseProgress,
                backdrop = headerBackdrop,
                calendarExpanded = calendarExpanded,
                onNavigateUp = onNavigateUp,
                onToggleCalendar = {
                    if (!calendarExpanded) calendarMonthEpoch = selectedDate.withDayOfMonth(1).toEpochDay()
                    calendarExpanded = !calendarExpanded
                    scope.launch { listState.animateScrollToItem(0) }
                },
                onMoveToToday = {
                    onSelectDate(today)
                    scope.launch { listState.animateScrollToItem(0) }
                },
                modifier = Modifier.align(Alignment.TopCenter),
            )
        }
    }
    if (showMonth) {
        key(YearMonth.from(selectedDate)) {
            MonthlyInsightExperience(
                initialDate = selectedDate,
                minDate = minDate,
                viewModel = achievementViewModel,
                onOpenDate = onSelectDate,
                onDismiss = { showMonth = false },
            )
        }
    }
}

@Composable
private fun MonthSummaryEntry(
    date: LocalDate,
    viewModel: DailyAchievementViewModel,
    onClick: () -> Unit,
) {
    var retry by remember { mutableStateOf(0) }
    val state by remember(retry) {
        viewModel.flowMonthReport(YearMonth.from(date))
    }.collectAsState(initial = AchievementLoadState.Loading)
    Row(
        modifier = Modifier.fillMaxWidth().appCardSurface()
            .clickable(onClick = { if (state == AchievementLoadState.Error) retry++ else onClick() })
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(date.formatYearMonth(), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
            Text(stringResource(R.string.history_month_summary), style = AppTypography.bodyMedium)
        }
        when (val result = state) {
            AchievementLoadState.Loading -> CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp)
            AchievementLoadState.Error -> Text(stringResource(R.string.history_retry), color = AppColor.primary)
            is AchievementLoadState.Ready -> Text(
                if (result.value.totalCount == 0) stringResource(R.string.achievement_no_value)
                else "${(result.value.completionRate * 100).roundToInt()}%",
                style = AppTypography.titleLarge, color = AppColor.primary,
            )
        }
        Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = AppColor.onSurfaceVariant)
    }
}

@Composable
private fun DayRecords(date: LocalDate, loops: List<FullLoopVo>, onNavigateToLoopDetail: (Int) -> Unit) {
    var showNotes by rememberSaveable { mutableStateOf(false) }
    val notes = loops.filter { it.retrospect.isNotBlank() }
    if (loops.isEmpty()) {
        AppCard {
            Text(stringResource(R.string.history_empty_day), style = AppTypography.bodyMedium, color = AppColor.onSurfaceVariant)
        }
        return
    }
    val completed = loops.count { it.done.isDone() }
    val skipped = loops.count { it.done.isSkip() }
    val fraction = completed.toFloat() / loops.size
    AppCard {
        Text(stringResource(R.string.history_day_rate), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text("${(fraction * 100).toInt()}%", style = AppTypography.headlineLarge, color = AppColor.primary)
            Spacer(Modifier.weight(1f))
            Column(horizontalAlignment = Alignment.End) {
                Text(stringResource(R.string.achievement_done_ratio, completed, loops.size), style = AppTypography.bodyMedium)
                if (skipped > 0) Text(
                    stringResource(R.string.achievement_skipped_count, skipped),
                    style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant,
                )
            }
        }
        LinearProgressIndicator(
            progress = { fraction },
            modifier = Modifier.fillMaxWidth().padding(top = 14.dp).height(5.dp),
            color = AppColor.primary,
            trackColor = AppColor.onSurface.copy(alpha = 0.08f),
        )
    }
    AppCard(contentPadding = PaddingValues(0.dp)) {
        loops.forEachIndexed { index, loop ->
            key(loop.loopId) {
                if (index > 0) HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = AppColor.outlineVariant,
                )
                AchievementRecordRow(loop, onClick = { onNavigateToLoopDetail(loop.loopId) })
            }
        }
    }
    if (notes.isNotEmpty()) {
        TextButton(onClick = { showNotes = true }, modifier = Modifier.fillMaxWidth()) {
            Icon(Icons.Outlined.EditNote, null, modifier = Modifier.size(20.dp))
            Text(stringResource(R.string.history_day_notes, notes.size), modifier = Modifier.padding(start = 6.dp))
        }
    }
    if (showNotes) {
        AlertDialog(
            onDismissRequest = { showNotes = false },
            title = { Text(stringResource(R.string.history_notes_title, date.formatMonthDateDay())) },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    items(notes, key = { it.loopId }) { loop ->
                        Column {
                            Text(loop.title, style = AppTypography.titleSmall)
                            Text(loop.retrospect, modifier = Modifier.padding(top = 8.dp), style = AppTypography.bodyMedium)
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showNotes = false }) { Text(stringResource(R.string.history_close)) } },
        )
    }
}

@Composable
private fun AchievementRecordRow(loop: FullLoopVo, onClick: () -> Unit) {
    var expanded by rememberSaveable(loop.loopId) { mutableStateOf(false) }
    var hasOverflow by remember { mutableStateOf(false) }
    val (statusId, statusIcon) = when (loop.done) {
        DoneState.DONE -> R.string.done to Icons.Outlined.Check
        DoneState.SKIP -> R.string.skip to Icons.Outlined.Remove
        DoneState.IN_PROGRESS -> R.string.history_in_progress to Icons.Outlined.Schedule
        else -> R.string.history_no_response to Icons.Outlined.MoreHoriz
    }
    val statusColor = if (loop.done.isDone()) AppColor.primary else AppColor.onSurfaceVariant
    Column(Modifier.fillMaxWidth().padding(bottom = 4.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                Modifier.padding(top = 2.dp).size(26.dp).background(statusColor.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(statusIcon, null, tint = statusColor, modifier = Modifier.size(16.dp))
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(loop.title, style = AppTypography.bodyLarge, fontWeight = FontWeight.Medium)
                Text(loop.formatStartEndTime(), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(stringResource(statusId), style = AppTypography.labelMedium, color = statusColor)
                    if (loop.created.toLocalDate() == loop.date.toLocalDate()) {
                        Text(stringResource(R.string.created), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                    }
                }
            }
            Icon(
                Icons.AutoMirrored.Outlined.KeyboardArrowRight, null,
                modifier = Modifier.size(18.dp), tint = AppColor.onSurfaceVariant,
            )
        }
        if (loop.retrospect.isNotBlank()) {
            Text(
                text = "“${loop.retrospect}”",
                modifier = Modifier.padding(start = 54.dp, end = 16.dp, bottom = 8.dp),
                style = AppTypography.bodyMedium, color = AppColor.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { if (!expanded) hasOverflow = it.hasVisualOverflow },
            )
            if (hasOverflow || expanded) {
                TextButton(onClick = { expanded = !expanded }, modifier = Modifier.padding(start = 42.dp)) {
                    Text(stringResource(if (expanded) R.string.history_less else R.string.history_more))
                }
            }
        }
    }
}

@Composable
internal fun HistoryLoading() {
    Row(Modifier.fillMaxWidth().padding(vertical = 24.dp), horizontalArrangement = Arrangement.Center) {
        CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
        Text(stringResource(R.string.history_loading), modifier = Modifier.padding(start = 12.dp), color = AppColor.onSurfaceVariant)
    }
}

@Composable
internal fun HistoryError(onRetry: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.history_error), style = AppTypography.bodyMedium, color = AppColor.onSurfaceVariant)
        TextButton(onClick = onRetry) { Text(stringResource(R.string.history_retry)) }
    }
}

@Composable
private fun MonthSummaryDialog(date: LocalDate, viewModel: DailyAchievementViewModel, onDismiss: () -> Unit) {
    var retry by remember { mutableStateOf(0) }
    var showNotes by remember { mutableStateOf(false) }
    val state by remember(retry) {
        viewModel.flowMonthSummary(YearMonth.from(date)).asAchievementState()
    }.collectAsState(initial = AchievementLoadState.Loading)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(date.formatYearMonth()) },
        text = {
            when (val result = state) {
                AchievementLoadState.Loading -> HistoryLoading()
                AchievementLoadState.Error -> HistoryError { retry++ }
                is AchievementLoadState.Ready -> {
                    if (result.value.totalCount == 0) Text(stringResource(R.string.no_achievements))
                    else SelectedMonthSummaryBar(summary = result.value, onClickRetrospects = { showNotes = true })
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.history_close)) } },
    )
    if (showNotes) {
        var notesRetry by remember { mutableStateOf(0) }
        val notesState by remember(notesRetry) {
            viewModel.flowMonthRetrospects(YearMonth.from(date)).asAchievementState()
        }.collectAsState(initial = AchievementLoadState.Loading)
        when (val result = notesState) {
            is AchievementLoadState.Ready -> MonthRetrospectsDialog(
                monthLabel = date.formatYearMonth(), retrospects = result.value, onDismiss = { showNotes = false },
            )
            else -> AlertDialog(
                onDismissRequest = { showNotes = false },
                text = { if (result == AchievementLoadState.Error) HistoryError { notesRetry++ } else HistoryLoading() },
                confirmButton = { TextButton(onClick = { showNotes = false }) { Text(stringResource(R.string.history_close)) } },
            )
        }
    }
}
