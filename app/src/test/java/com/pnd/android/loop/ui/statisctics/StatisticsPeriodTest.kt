package com.pnd.android.loop.ui.statisctics

import java.time.LocalDate
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StatisticsPeriodTest {
    private val firstDate = LocalDate.of(2023, 8, 17)
    private val today = LocalDate.of(2026, 10, 6)

    @Test fun `an older leap month includes its last day without adjacent months`() {
        val period = StatisticsPeriod.Month(YearMonth.of(2024, 2))
        assertEquals(LocalDate.of(2024, 2, 1), period.from(firstDate))
        assertEquals(LocalDate.of(2024, 2, 29), period.to(today))
    }

    @Test fun `December remains selected after the year changes`() {
        val period = StatisticsPeriod.Month(YearMonth.of(2025, 12))
        assertEquals(LocalDate.of(2025, 12, 1), period.from(firstDate))
        assertEquals(LocalDate.of(2025, 12, 31), period.to(LocalDate.of(2026, 1, 1)))
        assertEquals(LocalDate.of(2025, 12, 31), period.to(today))
    }

    @Test fun `current month ends today and never includes future scheduled days`() {
        val period = StatisticsPeriod.Month(YearMonth.from(today))
        assertEquals(LocalDate.of(2026, 10, 1), period.from(firstDate))
        assertEquals(today, period.to(today))
    }

    @Test fun `first month starts at first history date and total spans every year`() {
        assertEquals(firstDate, StatisticsPeriod.Month(YearMonth.from(firstDate)).from(firstDate))
        assertEquals(firstDate, StatisticsPeriod.Total.from(firstDate))
        assertEquals(today, StatisticsPeriod.Total.to(today))
    }

    @Test fun `month choices include gaps and exclude prehistory and future months`() {
        val range = statisticsMonthRange(firstDate, today)
        assertEquals(YearMonth.of(2023, 8), range.start)
        assertEquals(YearMonth.of(2026, 10), range.endInclusive)
        assertTrue(YearMonth.of(2024, 5) in range)
        assertFalse(YearMonth.of(2023, 7) in range)
        assertFalse(YearMonth.of(2026, 11) in range)
    }

    @Test fun `without past history only the current month is available`() {
        val current = YearMonth.from(today)
        assertEquals(current..current, statisticsMonthRange(null, today))
        assertEquals(current..current, statisticsMonthRange(today.plusDays(40), today))
    }
}
