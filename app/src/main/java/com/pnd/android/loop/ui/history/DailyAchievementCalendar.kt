package com.pnd.android.loop.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.isSameMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DailyAchievementCalendar(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    achievementViewModel: DailyAchievementViewModel,
    viewMode: DailyAchievementPageViewMode,
    minDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    Column(modifier = modifier) {
        CalendarHeader()
        HorizontalPager(
            modifier = Modifier.padding(top = 8.dp),
            state = pagerState,
            reverseLayout = true,
        ) { page ->
            CalendarPage(
                achievementViewModel = achievementViewModel,
                viewMode = viewMode,
                minDate = minDate,
                selectedDate = selectedDate,
                firstDateOfMonth = LocalDate.now().minusMonths(page.toLong()).withDayOfMonth(1),
                onDateSelected = onDateSelected
            )
        }
    }
}

@Composable
fun CalendarHeader(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Row(
            modifier = Modifier.padding(top = 16.dp)
        ) {
            DAYS_WITH_3CHARS_SUNDAY_FIRST.forEachIndexed { index, dayResId ->
                val day = stringResource(id = dayResId)
                Text(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically),
                    textAlign = TextAlign.Center,
                    text = day,
                    style = AppTypography.bodySmall.copy(
                        color = DayOfWeek.of(
                            if (index == 0) {
                                DayOfWeek.SUNDAY.value
                            } else {
                                index
                            }
                        ).color()
                    )
                )
            }
        }
    }
}


@Composable
private fun CalendarPage(
    modifier: Modifier = Modifier,
    achievementViewModel: DailyAchievementViewModel,
    viewMode: DailyAchievementPageViewMode,
    minDate: LocalDate,
    selectedDate: LocalDate,
    firstDateOfMonth: LocalDate,
    onDateSelected: (LocalDate) -> Unit
) {
    val start = remember(firstDateOfMonth) {
        firstDateOfMonth.dayOfWeek.value % 7L
    }
    val days = remember(firstDateOfMonth, start) {
        YearMonth.from(firstDateOfMonth).lengthOfMonth() + start
    }
    val rows = remember(days) {
        ceil(days.toFloat() / DAYS_OF_WEEK).toInt()
    }

    var itemDate = firstDateOfMonth.minusDays(start)
    val doneLoopsByDate by achievementViewModel.flowsDoneLoopsByDate(
        from = itemDate,
        to = itemDate.plusDays((rows * DAYS_OF_WEEK - 1).toLong())
    ).collectAsState(initial = emptyMap())

    val noDoneLoopsByDate by achievementViewModel.flowsNoDonLoopsByDate(
        from = itemDate,
        to = itemDate.plusDays((rows * DAYS_OF_WEEK - 1).toLong())
    ).collectAsState(initial = emptyMap())

    Column(modifier = modifier) {
        repeat(rows) {
            key(itemDate) {
                CalendarRow(
                    modifier = Modifier.weight(1f),
                    viewMode = viewMode,
                    doneLoopsByDate = doneLoopsByDate,
                    noDoneLoopsByDate = noDoneLoopsByDate,
                    itemDate = itemDate,
                    minDate = minDate,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            }
            itemDate = itemDate.plusWeeks(1)
        }
    }
}

@Composable
private fun CalendarRow(
    modifier: Modifier = Modifier,
    viewMode: DailyAchievementPageViewMode,
    doneLoopsByDate: Map<LocalDate, List<LoopByDate>>,
    noDoneLoopsByDate: Map<LocalDate, List<LoopByDate>>,
    itemDate: LocalDate,
    minDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
) {
    var itDate = itemDate
    Row(modifier = modifier.fillMaxWidth()) {
        repeat(DAYS_OF_WEEK) {
            key(itDate) {
                val isThisMonth = selectedDate.isSameMonth(itDate)
                val isBeforeNow = itDate.isBefore(LocalDate.now().plusDays(1))
                val isAfterMinDate = itDate.isAfter(minDate.minusDays(1))
                val isInterest = isThisMonth && isBeforeNow && isAfterMinDate

                CalendarDateItem(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(
                            alpha = if (isInterest) {
                                1f
                            } else {
                                0.3f
                            }
                        ),
                    viewMode = viewMode,
                    doneLoops = doneLoopsByDate[itDate] ?: emptyList(),
                    noDoneLoops = noDoneLoopsByDate[itDate] ?: emptyList(),
                    itemDate = itDate,
                    isInterest = isInterest,
                    isToday = itDate == LocalDate.now(),
                    isSelected = itDate == selectedDate,
                    onDateSelected = onDateSelected,
                )
            }
            itDate = itDate.plusDays(1)
        }
    }
}

@Composable
private fun CalendarDateItem(
    modifier: Modifier = Modifier,
    viewMode: DailyAchievementPageViewMode,
    doneLoops: List<LoopByDate>,
    noDoneLoops: List<LoopByDate>,
    itemDate: LocalDate,
    isInterest: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    onDateSelected: (LocalDate) -> Unit
) {
    val selectedBackground = compositeOverSurface()
    val primaryColor = AppColor.primary
    Column(
        modifier = modifier
            .fillMaxHeight()
            .padding(all = 2.dp)
            .clickable { onDateSelected(itemDate) }
            .drawBehind {
                if (viewMode == DailyAchievementPageViewMode.DESCRIPTION_TEXT && isInterest) {
                    val doneRate = (doneLoops.size.toFloat() / (doneLoops.size + noDoneLoops.size))
                    if (doneRate > 0.1f) {
                        drawRoundRect(
                            color = primaryColor.copy((0.4f * doneRate) * (0.4f * doneRate)),
                            cornerRadius = CornerRadius(
                                x = 4.dp.toPx(),
                                y = 4.dp.toPx()
                            )
                        )
                    }
                }
                if (isSelected) {
                    drawRoundRect(
                        color = selectedBackground,
                        cornerRadius = CornerRadius(
                            x = 4.dp.toPx(),
                            y = 4.dp.toPx()
                        )
                    )
                    drawRoundRect(
                        color = primaryColor.copy(alpha = 0.7f),
                        cornerRadius = CornerRadius(
                            x = 4.dp.toPx(),
                            y = 4.dp.toPx()
                        ),
                        style = Stroke(width = 1f)
                    )
                }
            }
    ) {

        Text(
            modifier = Modifier
                .padding(top = 4.dp)
                .fillMaxWidth(),
            text = "${itemDate.dayOfMonth}",
            textAlign = TextAlign.Center,
            style = AppTypography.bodyMedium.copy(
                color = itemDate.dayOfWeek.color(),
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        )

        val hasRetrospect = doneLoops.any { it.retrospect != null } ||
                noDoneLoops.any { it.retrospect != null }
        if (hasRetrospect) {
            Image(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .size(10.dp),
                imageVector = Icons.AutoMirrored.Filled.Chat,
                alpha = 0.6f,
                contentDescription = ""
            )
        }


        if (isInterest) {
            AchievementIndicators(
                modifier = Modifier
                    .padding(top = if (hasRetrospect) 4.dp else 14.dp)
                    .align(Alignment.CenterHorizontally),
                viewMode = viewMode,
                doneLoops = doneLoops,
                noDoneLoops = noDoneLoops,
            )
        }
    }
}

@Composable
private fun AchievementIndicators(
    modifier: Modifier = Modifier,
    viewMode: DailyAchievementPageViewMode,
    doneLoops: List<LoopByDate>,
    noDoneLoops: List<LoopByDate>,
) {
    when (viewMode) {
        DailyAchievementPageViewMode.COLOR_DOT -> ColorDotIndicator(
            modifier = modifier,
            doneLoops = doneLoops,
        )

        DailyAchievementPageViewMode.DESCRIPTION_TEXT -> DescriptionTextIndicator(
            modifier = modifier,
            doneCount = doneLoops.size,
            otherCount = noDoneLoops.size,
        )
    }
}

@Composable
private fun DescriptionTextIndicator(
    modifier: Modifier = Modifier,
    doneCount: Int,
    otherCount: Int,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = String.format(
                " %d/%d", doneCount, doneCount + otherCount
            ),
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.4f)
            )
        )
    }
}

@Composable
private fun ColorDotIndicator(
    modifier: Modifier = Modifier,
    doneLoops: List<LoopByDate>,
) {
    Row(modifier = modifier.padding(top = 2.dp)) {
        doneLoops.forEach { loop ->
            Box(
                modifier = Modifier
                    .padding(horizontal = 1.dp)
                    .size(4.dp)
                    .background(
                        color = loop.color
                            .compositeOverOnSurface()
                            .copy(alpha = 0.8f),
                        shape = CircleShape
                    )
            )
        }
    }

}


private const val DAYS_OF_WEEK = 7
