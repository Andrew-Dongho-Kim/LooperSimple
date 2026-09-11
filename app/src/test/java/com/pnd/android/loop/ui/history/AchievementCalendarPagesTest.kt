package com.pnd.android.loop.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

class AchievementCalendarPagesTest {
    private val first = LocalDate.of(2026, 8, 26)
    private val today = LocalDate.of(2026, 9, 11)

    @Test fun weekPagesCoverPartialFirstAndCurrentWeeks() {
        val pages = AchievementCalendarPages(first, today, expanded = false)
        assertEquals(3, pages.count)
        assertEquals(LocalDate.of(2026, 8, 23), pages.startOf(0))
        assertEquals(LocalDate.of(2026, 9, 6), pages.startOf(2))
        assertEquals(2, pages.pageOf(today))
    }

    @Test fun weekPagingPreservesTheSelectedWeekdayAcrossMonths() {
        val pages = AchievementCalendarPages(first, today, expanded = false)
        assertEquals(LocalDate.of(2026, 9, 4), pages.selectionInWeek(1, today))
        assertEquals(LocalDate.of(2026, 8, 28), pages.selectionInWeek(0, today))
    }

    @Test fun partialWeeksClampSelectionToAvailableDates() {
        val pages = AchievementCalendarPages(first, today, expanded = false)
        assertEquals(first, pages.selectionInWeek(0, LocalDate.of(2026, 9, 6)))
        assertEquals(today, pages.selectionInWeek(2, LocalDate.of(2026, 9, 5)))
    }

    @Test fun monthPagesCrossYearsWithoutSkippingFebruary() {
        val pages = AchievementCalendarPages(LocalDate.of(2027, 12, 31), LocalDate.of(2028, 3, 2), expanded = true)
        assertEquals(4, pages.count)
        assertEquals(LocalDate.of(2028, 2, 1), pages.startOf(2))
        assertEquals(2, pages.pageOf(LocalDate.of(2028, 2, 29)))
        assertEquals(3, pages.pageOf(LocalDate.of(2028, 3, 2)))
    }

    @Test fun datesOutsideHistoryResolveToBoundaryPages() {
        for (expanded in listOf(false, true)) {
            val pages = AchievementCalendarPages(first, today, expanded)
            assertEquals(0, pages.pageOf(first.minusYears(1)))
            assertEquals(pages.count - 1, pages.pageOf(today.plusYears(1)))
        }
    }

    @Test fun emptyOrFutureHistoryStillHasOnePage() {
        for (expanded in listOf(false, true)) {
            assertEquals(1, AchievementCalendarPages(today, today, expanded).count)
            assertEquals(1, AchievementCalendarPages(today.plusDays(3), today, expanded).count)
        }
    }

    @Test fun eachPageStartMapsBackToItsIndex() {
        for (expanded in listOf(false, true)) {
            val pages = AchievementCalendarPages(first.minusYears(2), today, expanded)
            for (page in 0 until pages.count) assertEquals(page, pages.pageOf(pages.startOf(page)))
        }
    }

    @Test fun loadingEarlierHistoryRebasesTodayToTheNewLastPage() {
        for (expanded in listOf(false, true)) {
            val initial = AchievementCalendarPages(today, today, expanded)
            val loaded = AchievementCalendarPages(first, today, expanded)
            assertEquals(0, initial.pageOf(today))
            assertEquals(loaded.count - 1, loaded.pageOf(today))
            assertEquals(initial.startOf(0), loaded.startOf(loaded.pageOf(today)))
        }
    }
}
