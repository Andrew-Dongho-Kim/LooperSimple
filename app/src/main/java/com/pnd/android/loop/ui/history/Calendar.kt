package com.pnd.android.loop.ui.history

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopWithDoneStates
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.isSameMonth
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun Calendar(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    allLoopsWithDoneStates: List<LoopWithDoneStates>?,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    Column(modifier = modifier) {
        CalendarHeader()
        HorizontalPager(
            modifier = Modifier.padding(top = 8.dp),
            state = pagerState,
            reverseLayout = true,
        ) { page ->
            CalendarPage(
                selectedDate = selectedDate,
                localDate = LocalDate.now().minusMonths(page.toLong()).withDayOfMonth(1),
                onSelectDate = onSelectDate
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
                        color = DayOfWeek.of(if (index == 0) 7 else index).color()
                    )
                )
            }
        }
    }
}


@Composable
private fun CalendarPage(
    modifier: Modifier = Modifier,
    selectedDate: LocalDate,
    localDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit
) {
    val itemMonth = localDate
    val start = remember(itemMonth) { itemMonth.dayOfWeek.value % 7L }

    val days = remember(itemMonth, start) { YearMonth.from(itemMonth).lengthOfMonth() + start }
    val rows = remember(days) { ceil(days.toFloat() / DAYS_OF_WEEK).toInt() }

    Column(modifier = modifier) {
        var month = itemMonth.minusDays(start)
        repeat(rows) {
            key(month) {
                CalendarRow(
                    modifier = Modifier.weight(1f),
                    itemDate = month,
                    selectedDate = selectedDate,
                    onSelectDate = onSelectDate
                )
            }
            month = month.plusWeeks(1)
        }
    }
}

@Composable
private fun CalendarRow(
    modifier: Modifier = Modifier,
    itemDate: LocalDate,
    selectedDate: LocalDate,
    onSelectDate: (LocalDate) -> Unit,
) {
    var itDate = itemDate
    Row(modifier = modifier.fillMaxWidth()) {
        repeat(DAYS_OF_WEEK) {
            key(itDate) {
                CalendarDateItem(
                    modifier = Modifier
                        .weight(1f)
                        .alpha(
                            alpha = if (selectedDate.isSameMonth(itDate)) {
                                1f
                            } else {
                                0.3f
                            }
                        ),
                    itemDate = itDate,
                    isToday = itDate == LocalDate.now(),
                    isSelected = itDate == selectedDate,
                    onSelectDate = onSelectDate,
                )
            }
            itDate = itDate.plusDays(1)
        }
    }
}

@Composable
private fun CalendarDateItem(
    modifier: Modifier = Modifier,
    itemDate: LocalDate,
    isToday: Boolean,
    isSelected: Boolean,
    onSelectDate: (LocalDate) -> Unit
) {
    val selectedBackground = compositeOverSurface()
    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(all = 2.dp)
            .clickable { onSelectDate(itemDate) }
            .drawBehind {
                if (isSelected) {
                    drawRoundRect(
                        color = selectedBackground,
                        cornerRadius = CornerRadius(
                            x = 4.dp.toPx(),
                            y = 4.dp.toPx()
                        )
                    )
                }
            }
    ) {
        Text(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            text = "${itemDate.dayOfMonth}",
            textAlign = TextAlign.Center,
            style = AppTypography.bodyMedium.copy(
                color = itemDate.dayOfWeek.color(),
                fontWeight = if (isToday) FontWeight.Bold else FontWeight.Normal
            )
        )
    }
}


private const val DAYS_OF_WEEK = 7
