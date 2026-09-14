package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.*
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.ui.detail.computeDetailStats
import com.pnd.android.loop.ui.history.buildMonthInsightReport
import com.pnd.android.loop.ui.home.computeRecentLoopCompletion
import com.pnd.android.loop.ui.statisctics.*
import com.pnd.android.loop.util.currentOccurrenceDate
import org.junit.Assert.*
import org.junit.Test
import java.time.YearMonth

class HistoryStatisticsTest {
    @Test fun `detail month statistics and home chip agree after a schedule change`() {
        val before = testLoop()
        val after = before.copy(title = "After", activeDays = LoopDay.MONDAY)
        val today = firstDay.plusDays(13)
        val history = timeline(after, listOf(revision(1, before), revision(2, after, firstDay.plusDays(7))),
            listOf(response(firstDay.plusDays(1)), response(firstDay.plusDays(2), DoneState.SKIP),
                response(firstDay.plusDays(7)), response(today, DoneState.IN_PROGRESS, 2)))
        val snapshot = LoopHistorySnapshot(listOf(history.history))
        val detail = computeDetailStats(history, today)
        val month = buildMonthInsightReport(YearMonth.from(today), today, snapshot)
        val chip = computeRecentLoopCompletion(history, today)!!
        val statistics = computePeriodStats(snapshot.settled(firstDay, today, today).map { it.asResponseRecord() })
        assertEquals(8, detail.totalCount)
        assertEquals(detail.totalCount, month.totalCount)
        assertEquals(detail.totalCount, chip.totalCount)
        assertEquals(25, chip.percent)
        assertEquals(25, detail.donePercent)
        assertEquals(0.25f, month.completionRate, 0.0001f)
        assertEquals(month.completionRate, statistics.summary.completionRate, 0.0001f)
        assertEquals("After", month.loops.single().title)
        assertEquals("Before", month.days.first().records.single().title)
    }

    @Test fun `today pending remains visible without marking a partially completed day perfect`() {
        val today = firstDay
        val one = timeline(responses = listOf(response(today)))
        val other = testLoop().copy(loopId = 2)
        val two = LoopHistory(other, listOf(revision(2, other)), emptyList(), emptyList())
        val report = buildMonthInsightReport(YearMonth.from(today), today,
            LoopHistorySnapshot(listOf(one.history, two)))
        assertEquals(2, report.occurrenceCount)
        assertEquals(1, report.totalCount)
        assertEquals(1f, report.completionRate, 0f)
        assertEquals(1, report.pendingCount)
        assertEquals(0, report.perfectDays)
    }

    @Test fun `historical start is compared with user entered time and planned placeholders are excluded`() {
        val before = testLoop()
        val after = before.copy(startInDay = 12 * 3_600_000L)
        val record = response(firstDay).copy(startInDay = before.startInDay + 15 * 60_000L,
            timeSource = LoopDoneVo.TimeSource.USER_ENTERED)
        val history = timeline(after, listOf(revision(1, before), revision(2, after, firstDay.plusDays(1))), listOf(record))
        val actual = history.day(firstDay)!!.asResponseRecord()
        val result = computePlanVsActual(listOf(actual,
            actual.copy(timeSource = LoopDoneVo.TimeSource.PLANNED),
            actual.copy(timeSource = LoopDoneVo.TimeSource.UNKNOWN)))
        assertEquals(1, result.sampleCount)
        assertEquals(15 * 60_000L, result.avgStartDiffMs)
        assertEquals(0, result.onTimeCount)
    }

    @Test fun `estimated plan does not claim measured punctuality`() {
        val loop = testLoop()
        val record = response(firstDay).copy(timeSource = LoopDoneVo.TimeSource.USER_ENTERED)
        val day = timeline(loop, listOf(revision(1, loop, knownFrom = firstDay.plusDays(1))), listOf(record)).day(firstDay)!!
        assertEquals(0, computePlanVsActual(listOf(day.asResponseRecord())).sampleCount)
    }

    @Test fun `actual clock displacement wraps correctly over midnight`() {
        val record = timeline(responses = listOf(response(firstDay))).day(firstDay)!!.asResponseRecord().copy(
            plannedStartInDay = 23 * 3_600_000L + 55 * 60_000L,
            startInDay = 5 * 60_000L, timeSource = LoopDoneVo.TimeSource.USER_ENTERED,
        )
        assertEquals(10 * 60_000L, computePlanVsActual(listOf(record)).avgStartDiffMs)
    }

    @Test fun `duration never turns missing timestamps into one day and prefers measured instants`() {
        val record = timeline(responses = listOf(response(firstDay))).day(firstDay)!!.asResponseRecord()
        assertEquals(0, record.copy(startInDay = -1).investedTimeMs())
        assertEquals(2 * 3_600_000L, record.copy(startInDay = 23 * 3_600_000L, endInDay = 3_600_000L).investedTimeMs())
        assertEquals(26 * 3_600_000L, record.copy(measuredDurationMs = 26 * 3_600_000L).investedTimeMs())
    }

    @Test fun `new morning schedule does not replace yesterday overnight occurrence`() {
        val overnight = testLoop().copy(startInDay = 23 * 3_600_000L, endInDay = 3_600_000L)
        val daytime = overnight.copy(startInDay = 8 * 3_600_000L, endInDay = 9 * 3_600_000L)
        val today = firstDay.plusDays(1)
        val history = timeline(daytime, listOf(revision(1, overnight), revision(2, daytime, today)),
            listOf(response(firstDay, DoneState.NO_RESPONSE)))
        assertEquals(firstDay, currentOccurrenceDate(history.liveLoop(today), history.liveLoop(firstDay), today.atTime(0, 30)))
        assertEquals(today, currentOccurrenceDate(history.liveLoop(today), history.liveLoop(firstDay), today.atTime(1, 0)))
    }

    @Test fun `changing from anytime preserves the previous running occurrence`() {
        val anytime = testLoop().copy(isAnyTime = true, startInDay = -1, endInDay = -1)
        val timed = testLoop()
        val today = firstDay.plusDays(1)
        val history = timeline(timed, listOf(revision(1, anytime), revision(2, timed, today)),
            listOf(response(firstDay, DoneState.IN_PROGRESS)))
        assertEquals(firstDay, currentOccurrenceDate(history.liveLoop(today), history.liveLoop(firstDay), today.atTime(12, 0)))
    }

    @Test fun `weekly goal remains this weeks goal while next week choice is pending`() {
        val old = testLoop()
        val updated = old.copy(weeklyGoal = 2)
        val stats = computeDetailStats(timeline(updated, listOf(revision(1, old),
            revision(2, updated, firstDay, nextWeekStart(firstDay)))), firstDay)
        assertEquals(5, stats.weekly.target)
        assertEquals(2, stats.pendingGoal)
    }
}
