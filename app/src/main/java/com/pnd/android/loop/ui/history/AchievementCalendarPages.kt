package com.pnd.android.loop.ui.history

import java.time.LocalDate
import java.time.temporal.ChronoUnit

/** A finite, chronological pager range: the first recorded period through the current one. */
internal class AchievementCalendarPages(minDate: LocalDate, private val today: LocalDate, val expanded: Boolean) {
    private val earliestDate = minOf(minDate, today)
    private val unit = if (expanded) ChronoUnit.MONTHS else ChronoUnit.WEEKS
    private fun periodStart(date: LocalDate) = if (expanded) date.withDayOfMonth(1) else achievementWeekStart(date)
    private val first = periodStart(earliestDate)

    val count = unit.between(first, periodStart(today)).toInt() + 1

    fun pageOf(date: LocalDate): Int = unit.between(first, periodStart(date)).toInt().coerceIn(0, count - 1)

    fun startOf(page: Int): LocalDate {
        require(page in 0 until count)
        return first.plus(page.toLong(), unit)
    }

    /** Preserve the selected weekday when paging, clamping partial first/current weeks. */
    fun selectionInWeek(page: Int, selectedDate: LocalDate): LocalDate {
        require(!expanded)
        val weekdayOffset = (selectedDate.dayOfWeek.value % 7).toLong()
        return startOf(page).plusDays(weekdayOffset).coerceIn(earliestDate, today)
    }
}
