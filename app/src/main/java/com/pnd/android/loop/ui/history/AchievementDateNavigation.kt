package com.pnd.android.loop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.formatMonthDateDay
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/** Browsing a month never changes the selected record until a date is explicitly chosen. */
@Composable
internal fun AchievementDateNavigation(
    selectedDate: LocalDate,
    today: LocalDate,
    minDate: LocalDate,
    expanded: Boolean,
    month: YearMonth,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    viewModel: DailyAchievementViewModel,
) {
    // Rebase page indices when the first record loads or week/month mode changes.
    key(minDate, today, expanded) {
        AchievementDatePager(selectedDate, today, minDate, expanded, month, onMonthChanged, onDateSelected, viewModel)
    }
}

@Composable
private fun AchievementDatePager(
    selectedDate: LocalDate,
    today: LocalDate,
    minDate: LocalDate,
    expanded: Boolean,
    month: YearMonth,
    onMonthChanged: (YearMonth) -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    viewModel: DailyAchievementViewModel,
) {
    val weekStart = achievementWeekStart(selectedDate)
    val pages = remember(minDate, today, expanded) { AchievementCalendarPages(minDate, today, expanded) }
    val requestedPage = pages.pageOf(if (expanded) month.atDay(1) else selectedDate)
    val pagerState = rememberPagerState(initialPage = requestedPage) { pages.count }
    val scope = rememberCoroutineScope()
    val onPageSettled by rememberUpdatedState<(Int) -> Unit> { page ->
        if (expanded) onMonthChanged(YearMonth.from(pages.startOf(page)))
        else onDateSelected(pages.selectionInWeek(page, selectedDate))
    }
    LaunchedEffect(pagerState, requestedPage) {
        // External date selection (including Today) wins before observing user paging.
        // Observing only settled pages avoids changing records halfway through a drag.
        if (pagerState.currentPage != requestedPage || pagerState.currentPageOffsetFraction != 0f || pagerState.isScrollInProgress) {
            pagerState.scrollToPage(requestedPage)
        }
        snapshotFlow { if (pagerState.isScrollInProgress) null else pagerState.settledPage }
            .collect { page -> if (page != null && page != requestedPage) onPageSettled(page) }
    }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                enabled = pagerState.currentPage > 0 && !pagerState.isScrollInProgress,
                onClick = {
                    scope.launch { pagerState.animateScrollToPage((pagerState.currentPage - 1).coerceAtLeast(0)) }
                },
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowLeft,
                    stringResource(if (expanded) R.string.history_previous_month else R.string.history_previous_week),
                )
            }
            Text(
                if (expanded) stringResource(R.string.history_choose_date)
                else stringResource(R.string.history_week_range, weekStart.formatMonthDateDay(), weekStart.plusDays(6).formatMonthDateDay()),
                modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant,
            )
            IconButton(
                enabled = pagerState.currentPage < pages.count - 1 && !pagerState.isScrollInProgress,
                onClick = {
                    scope.launch { pagerState.animateScrollToPage((pagerState.currentPage + 1).coerceAtMost(pages.count - 1)) }
                },
            ) {
                Icon(
                    Icons.AutoMirrored.Outlined.KeyboardArrowRight,
                    stringResource(if (expanded) R.string.history_next_month else R.string.history_next_week),
                )
            }
        }
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxWidth(),
            pageSpacing = 12.dp,
            beyondViewportPageCount = 1,
            verticalAlignment = Alignment.Top,
            key = { pages.startOf(it).toEpochDay() },
        ) { page ->
            val pageStart = pages.startOf(page)
            val pageMonth = YearMonth.from(pageStart)
            val dates = if (expanded) achievementMonthDates(pageMonth) else List(7) { pageStart.plusDays(it.toLong()) }
            AchievementCalendarGrid(dates, selectedDate, today, minDate, expanded, pageMonth, onDateSelected, viewModel)
        }
    }
}

@Composable
private fun AchievementCalendarGrid(
    dates: List<LocalDate>,
    selectedDate: LocalDate,
    today: LocalDate,
    minDate: LocalDate,
    expanded: Boolean,
    month: YearMonth,
    onDateSelected: (LocalDate) -> Unit,
    viewModel: DailyAchievementViewModel,
) {
    key(dates.first(), dates.last()) {
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            var retry by remember { mutableStateOf(0) }
            val state by remember(retry) {
                viewModel.flowCalendarDays(dates.first(), dates.last())
            }.collectAsState(initial = AchievementLoadState.Loading)
            val records = (state as? AchievementLoadState.Ready)?.value.orEmpty()
            if (expanded) {
                Row(Modifier.fillMaxWidth()) {
                    dates.take(7).forEach { day ->
                        Text(
                            day.dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.getDefault()),
                            modifier = Modifier.weight(1f).padding(vertical = 6.dp),
                            style = AppTypography.labelSmall, textAlign = TextAlign.Center,
                            color = AppColor.onSurfaceVariant,
                        )
                    }
                }
            }
            dates.chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                    week.forEach { date ->
                        AchievementDateCell(
                            date = date, selected = date == selectedDate, today = date == today,
                            enabled = !date.isBefore(minDate) && !date.isAfter(today),
                            inMonth = !expanded || YearMonth.from(date) == month,
                            compact = !expanded, record = records[date],
                            modifier = Modifier.weight(1f), onClick = { onDateSelected(date) },
                        )
                    }
                }
            }
            if (state == AchievementLoadState.Error) {
                HistoryError { retry++ }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(stringResource(R.string.history_heat_low), style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant)
                    (0..4).forEach { level ->
                        Box(Modifier.padding(start = 4.dp).size(9.dp).background(
                            AppColor.primary.copy(alpha = 0.04f + level * 0.07f), RoundedCornerShape(2.dp),
                        ))
                    }
                    Text(
                        stringResource(R.string.history_heat_high),
                        modifier = Modifier.padding(start = 4.dp),
                        style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant,
                    )
                }
                Row(
                    Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(12.dp), tint = AppColor.onSurfaceVariant)
                    Text(stringResource(R.string.history_has_note), style = AppTypography.labelSmall, color = AppColor.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun AchievementDateCell(
    date: LocalDate,
    selected: Boolean,
    today: Boolean,
    enabled: Boolean,
    inMonth: Boolean,
    compact: Boolean,
    record: AchievementCalendarDay?,
    modifier: Modifier,
    onClick: () -> Unit,
) {
    val primary = AppColor.primary
    val textColor = when {
        !enabled || !inMonth -> AppColor.onSurfaceVariant.copy(alpha = 0.5f)
        selected || today -> primary
        else -> AppColor.onSurface
    }
    val rate = record?.takeIf { it.totalCount > 0 }?.let { it.doneCount.toFloat() / it.totalCount } ?: 0f
    val cellColor = when {
        rate > 0 && inMonth && enabled -> primary.copy(alpha = 0.05f + rate * 0.27f)
        else -> Color.Transparent
    }
    val label = listOfNotNull(
        date.formatMonthDateDay(),
        if (today) stringResource(R.string.history_today) else null,
        record?.let { stringResource(R.string.achievement_done_ratio, it.doneCount, it.totalCount) },
        if (record?.hasNote == true) stringResource(R.string.history_has_note) else null,
    ).joinToString(", ")
    Column(
        modifier = modifier.padding(vertical = 2.dp)
            .clip(RoundedCornerShape(if (compact) 16.dp else 10.dp))
            .background(cellColor)
            .then(if (selected) Modifier.border(2.dp, primary, RoundedCornerShape(if (compact) 16.dp else 10.dp)) else Modifier)
            .selectable(selected = selected, enabled = enabled, role = Role.Button, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = label }
            .heightIn(min = if (compact) 76.dp else 48.dp)
            .padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (compact) Text(
            date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault()),
            style = AppTypography.labelSmall, color = textColor,
        )
        Text(
            date.dayOfMonth.toString(),
            style = if (compact) AppTypography.titleMedium else AppTypography.bodyMedium,
            fontWeight = if (selected || today) FontWeight.SemiBold else FontWeight.Normal,
            color = textColor,
        )
        if (record?.hasNote == true) {
            Icon(Icons.Outlined.Edit, null, modifier = Modifier.size(10.dp), tint = textColor)
        } else {
            Box(Modifier.size(5.dp).background(if (today) textColor else Color.Transparent, CircleShape))
        }
    }
}
