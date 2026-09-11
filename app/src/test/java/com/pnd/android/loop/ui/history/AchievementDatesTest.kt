package com.pnd.android.loop.ui.history

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

class AchievementDatesTest {
    @Test fun weekCrossesYearWithoutLosingSelectedDay() {
        val selected = LocalDate.of(2026, 1, 1)
        assertEquals(LocalDate.of(2025, 12, 28), achievementWeekStart(selected))
        assertTrue(selected in (0L..6L).map { achievementWeekStart(selected).plusDays(it) })
    }

    @Test fun sundayBelongsToNewWeek() {
        val sunday = LocalDate.of(2026, 9, 6)
        assertEquals(sunday, achievementWeekStart(sunday))
    }

    @Test fun sixWeekMonthIncludesLastDayAndAdjacentDates() {
        val dates = achievementMonthDates(YearMonth.of(2026, 8))
        assertEquals(42, dates.size)
        assertEquals(DayOfWeek.SUNDAY, dates.first().dayOfWeek)
        assertEquals(LocalDate.of(2026, 7, 26), dates.first())
        assertTrue(LocalDate.of(2026, 8, 31) in dates)
        assertEquals(LocalDate.of(2026, 9, 5), dates.last())
        assertEquals(dates.size, dates.distinct().size)
    }

    @Test fun leapDayIsSelectableInFebruaryGrid() {
        val dates = achievementMonthDates(YearMonth.of(2028, 2))
        assertEquals(29, dates.count { it.monthValue == 2 })
        assertTrue(LocalDate.of(2028, 2, 29) in dates)
    }
}
