package com.pnd.android.loop.ui.history

import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.toMs
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth

class MonthInsightModelsTest {
    private val august = YearMonth.of(2026, 8)
    private fun loop(id: Int = 1, enabled: Boolean = true, days: Int = 127, created: LocalDate = LocalDate.of(2026, 7, 1)) =
        LoopVo(id, "Loop $id", 0, created.toMs(), 9 * 3_600_000L, 10 * 3_600_000L, days, enabled, false, 3)

    private fun record(day: LocalDate, state: Int = DoneState.DONE, id: Int = 1, start: Long = 9 * 3_600_000L, end: Long = 10 * 3_600_000L) =
        LoopDoneVo(id, day.toMs(), start, end, state)

    @Test fun calendarIncludesScheduledRunsWithoutSavedResponses() {
        val date = august.atDay(1)
        val day = resolveInsightDays(date, date, listOf(loop(), loop(2)), listOf(record(date)), emptyList()).single()
        assertEquals(1, day.doneCount)
        assertEquals(2, day.totalCount)
        assertEquals(0.5f, day.rate!!, 0.001f)
    }

    @Test fun weekAndMonthAgreeForTheSameDateAcrossMonthBoundary() {
        val date = august.atDay(1)
        val loops = listOf(loop(), loop(2))
        val saved = listOf(record(date), record(date.minusDays(1), id = 2))
        val notes = listOf(LoopRetrospectVo(1, date.toMs(), "A note"))
        val weekStart = achievementWeekStart(date)
        val weekDay = resolveInsightDays(weekStart, weekStart.plusDays(6), loops, saved, notes).first { it.date == date }
        val monthDay = buildMonthInsightReport(august, date.plusDays(2), loops, saved, notes).days.first { it.date == date }
        assertEquals(monthDay.doneCount, weekDay.doneCount)
        assertEquals(monthDay.totalCount, weekDay.totalCount)
        assertEquals(monthDay.rate, weekDay.rate)
        assertTrue(weekDay.hasNote)
        assertEquals(monthDay.hasNote, weekDay.hasNote)
    }

    @Test fun partialMonthUsesEqualElapsedPeriods() {
        val period = monthComparisonPeriod(YearMonth.of(2026, 3), LocalDate.of(2026, 3, 31))
        assertEquals(LocalDate.of(2026, 3, 28), period.currentEnd)
        assertEquals(LocalDate.of(2026, 2, 28), period.previousEnd)
    }

    @Test fun leapFebruaryUsesTwentyNineComparisonDays() {
        val period = monthComparisonPeriod(YearMonth.of(2028, 3), LocalDate.of(2028, 3, 30))
        assertEquals(LocalDate.of(2028, 3, 29), period.currentEnd)
        assertEquals(LocalDate.of(2028, 2, 29), period.previousEnd)
    }

    @Test fun completedMonthUsesBothFullMonthsAcrossYearBoundary() {
        val period = monthComparisonPeriod(YearMonth.of(2026, 1), LocalDate.of(2026, 9, 11))
        assertEquals(LocalDate.of(2025, 12, 1), period.previousStart)
        assertEquals(LocalDate.of(2025, 12, 31), period.previousEnd)
        assertEquals(LocalDate.of(2026, 1, 31), period.currentEnd)
    }

    @Test fun futureDatesAndBeforeCreationAreExcluded() {
        val today = august.atDay(3)
        val report = buildMonthInsightReport(august, today, listOf(loop(created = august.atDay(2))),
            listOf(record(august.atDay(1)), record(august.atDay(4))), emptyList())
        assertEquals(3, report.days.size)
        assertEquals(2, report.totalCount)
        assertEquals(0, report.doneCount)
    }

    @Test fun savedHistoryOverridesChangedScheduleAndDisabledStateIsExcluded() {
        val date = august.atDay(1)
        val records = listOf(record(date), record(date, DoneState.DISABLED, 2))
        val days = resolveInsightDays(date, date, listOf(loop(enabled = false, days = 0), loop(2)), records, emptyList())
        assertEquals(1, days.single().totalCount)
        assertEquals(1, days.single().doneCount)
    }

    @Test fun noPriorSavedRecordsDoesNotInventZeroPercentComparison() {
        val report = buildMonthInsightReport(august, august.atDay(10), listOf(loop()),
            listOf(record(august.atDay(1))), emptyList())
        assertNull(report.deltaPoints)
        assertNull(report.loops.single().deltaPoints)
    }

    @Test fun deltaUsesComparableCurrentDaysNotTheEntirePartialMonth() {
        val month = YearMonth.of(2026, 3)
        val loop = loop(created = LocalDate.of(2026, 2, 1))
        val saved = (1..28).map { record(LocalDate.of(2026, 2, it)) } +
            (1..28).map { record(month.atDay(it)) }
        val report = buildMonthInsightReport(month, month.atDay(31), listOf(loop), saved, emptyList())
        assertEquals(31, report.totalCount)
        assertEquals(28, report.comparableCount)
        assertEquals(0, report.deltaPoints)
        assertEquals(0, report.loops.single().deltaPoints)
    }

    @Test fun timeSkipsMissingTimestampsAndHandlesMidnight() {
        val saved = listOf(
            record(august.atDay(1), start = 23 * 3_600_000L, end = 3_600_000L),
            record(august.atDay(2), start = -1, end = -1),
            record(august.atDay(3), state = DoneState.SKIP),
        )
        val report = buildMonthInsightReport(august, august.atDay(3), listOf(loop()), saved, emptyList())
        assertEquals(2 * 3_600_000L, report.timeMs)
        assertEquals(1, report.timedCount)
        assertEquals(2, report.doneCount)
    }

    @Test fun nextDayRecoveryExcludesUnfinishedToday() {
        val report = buildMonthInsightReport(august, august.atDay(5), listOf(loop()), listOf(record(august.atDay(2))), emptyList())
        assertEquals(2, report.restartOpportunities.size) // 1 -> 2 and 3 -> 4; not 4 -> unfinished 5.
        assertEquals(1, report.restartedCount)
    }

    @Test fun streakIsCalendarConsecutiveAndPerfectDaysRequireEveryLoop() {
        val saved = listOf(record(august.atDay(1)), record(august.atDay(2)),
            record(august.atDay(2), id = 2), record(august.atDay(4)))
        val report = buildMonthInsightReport(august, august.atDay(4), listOf(loop(), loop(2)), saved, emptyList())
        assertEquals(2, report.longestStreak)
        assertEquals(3, report.activeDays)
        assertEquals(1, report.perfectDays)
    }

    @Test fun notesAreScopedToOccurrenceAndBlankNotesAreIgnored() {
        val notes = listOf(
            LoopRetrospectVo(1, august.atDay(1).toMs(), "A note"),
            LoopRetrospectVo(1, august.atDay(2).toMs(), " "),
            LoopRetrospectVo(1, august.atDay(4).toMs(), "Future note"),
        )
        val report = buildMonthInsightReport(august, august.atDay(3), listOf(loop()), emptyList(), notes)
        assertEquals(1, report.notes.size)
        assertEquals("A note", report.notes.single().retrospect)
    }

    @Test fun statusBreakdownAccountsForAllScheduledRuns() {
        val saved = listOf(record(august.atDay(1)), record(august.atDay(2), DoneState.SKIP),
            record(august.atDay(3), DoneState.IN_PROGRESS))
        val report = buildMonthInsightReport(august, august.atDay(4), listOf(loop()), saved, emptyList())
        assertEquals(report.totalCount, report.doneCount + report.skippedCount + report.pendingCount + report.inProgressCount)
        assertNull(report.bestWeekday) // One week is not enough to announce a weekday pattern.
    }
}
