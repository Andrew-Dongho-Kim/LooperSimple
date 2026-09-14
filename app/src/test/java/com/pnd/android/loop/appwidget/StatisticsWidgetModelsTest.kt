package com.pnd.android.loop.appwidget

import com.fasterxml.jackson.databind.ObjectMapper
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.history.*
import com.pnd.android.loop.ui.statisctics.computePeriodStats
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class StatisticsWidgetModelsTest {
    @Test fun `today progress includes pending but statistics rates do not`() {
        val today = firstDay
        val one = timeline(responses = listOf(response(today)))
        val two = testLoop().copy(loopId = 2)
        val three = testLoop().copy(loopId = 3)
        val snapshot = LoopHistorySnapshot(listOf(one.history,
            LoopHistory(two, listOf(revision(2, two)), emptyList(), emptyList()),
            LoopHistory(three, listOf(revision(3, three)), listOf(response(today, DoneState.SKIP).copy(loopId = 3, revisionId = 3)), emptyList())))
        val data = computeStatisticsWidgetData(snapshot, today)
        assertEquals(3, data.todayTotal)
        assertEquals(1, data.todayDone)
        assertEquals(1, data.todaySkipped)
        assertEquals(2, data.weekTotal)
        assertEquals(1, data.weekDone)
        assertEquals(2, data.monthTotal)
        assertEquals(listOf(0, 1, -1, -1, -1, -1, -1), data.weekCounts)
    }

    @Test fun `schedule changes and missing past responses agree with app statistics`() {
        val before = testLoop()
        val after = before.copy(title = "Renamed", activeDays = LoopDay.MONDAY)
        val today = firstDay.plusDays(13)
        val history = timeline(after, listOf(revision(1, before), revision(2, after, firstDay.plusDays(7))),
            listOf(response(firstDay.plusDays(1)), response(firstDay.plusDays(2), DoneState.SKIP),
                response(firstDay.plusDays(7)), response(today, DoneState.IN_PROGRESS, 2)))
        val snapshot = LoopHistorySnapshot(listOf(history.history))
        val data = computeStatisticsWidgetData(snapshot, today)
        val records = snapshot.settled(firstDay, today, today).map { it.asResponseRecord() }
        val app = computePeriodStats(records)
        assertEquals(records.size, data.monthTotal)
        assertEquals(app.summary.completedCount, data.monthDone)
        assertEquals(app.summary.investedTimeMs, data.monthTimes.last())
        assertEquals(0, data.weekTotal) // Monday today is still running.
        assertEquals("", data.bestTitle)
    }

    @Test fun `month slots zero fill and reset over year boundary`() {
        val today = LocalDate.of(2027, 1, 1)
        val history = timeline(responses = listOf(response(LocalDate.of(2026, 12, 31))))
        val data = computeStatisticsWidgetData(LoopHistorySnapshot(listOf(history.history)), today)
        assertEquals(6, data.monthTimes.size)
        assertEquals(listOf(0L, 0L, 0L, 0L, 3_600_000L, 0L), data.monthTimes)
        assertEquals(0, data.monthTotal)
        assertEquals(1, data.currentStreak) // Yesterday anchors a still-live streak.
    }

    @Test fun `overnight and measured durations remain on their start dates`() {
        val today = firstDay.plusDays(1)
        val overnight = response(firstDay).copy(startInDay = 23 * 3_600_000L, endInDay = 3_600_000L)
        val measured = response(today).copy(startedAt = 1_000L, endedAt = 26 * 3_600_000L + 1_000L)
        val data = computeStatisticsWidgetData(LoopHistorySnapshot(listOf(timeline(responses = listOf(overnight, measured)).history)), today)
        assertEquals(1, data.todayDone)
        assertEquals(26 * 3_600_000L, data.todayTimeMs)
        assertEquals(28 * 3_600_000L, data.monthTimes.last())
        assertEquals(2, data.currentStreak)
        assertEquals(2, data.longestStreak)
    }

    @Test fun `empty and disabled plans never produce false achievements`() {
        val empty = computeStatisticsWidgetData(LoopHistorySnapshot(emptyList()), firstDay)
        assertEquals(0, empty.registered)
        assertEquals(0, empty.todayTotal)
        assertEquals(0, empty.currentStreak)
        val disabled = testLoop().copy(enabled = false)
        val data = computeStatisticsWidgetData(LoopHistorySnapshot(listOf(timeline(disabled,
            listOf(revision(1, disabled)), listOf(response(firstDay, DoneState.DISABLED))).history)), firstDay)
        assertEquals(1, data.registered)
        assertEquals(0, data.todayTotal)
        assertEquals(0, data.monthTotal)
    }

    @Test fun `state survives JSON with unicode titles and long durations`() {
        val data = computeStatisticsWidgetData(LoopHistorySnapshot(listOf(timeline(
            testLoop().copy(title = "독서 \"집중\" 📚"), responses = listOf(response(firstDay))).history)), firstDay)
        val mapper = ObjectMapper()
        val restored = mapper.readValue(mapper.writeValueAsString(data), StatisticsWidgetData::class.java)
        assertEquals(data.bestTitle, restored.bestTitle)
        assertEquals(data.weekCounts, restored.weekCounts)
        assertEquals(data.monthTimes, restored.monthTimes)
        assertEquals(data.date, restored.date)
    }
}
