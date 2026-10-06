package com.pnd.android.loop.ui.statisctics

import com.pnd.android.loop.data.history.*
import org.junit.Assert.*
import org.junit.Test

class StatisticsEnabledLoopsTest {
    @Test fun `disabled loops are excluded even when their historical settings were enabled`() {
        val active = timeline(responses = listOf(response(firstDay))).history
        val disabled = active.copy(loop = active.loop.copy(enabled = false))
        val otherLoop = testLoop().copy(loopId = 2)
        val other = timeline(otherLoop, responses = listOf(response(firstDay).copy(loopId = 2))).history
        val source = LoopHistorySnapshot(listOf(disabled, other))

        val filtered = source.forStatistics()
        assertEquals(listOf(2), filtered.timelines.map { it.current.loopId })
        val days = filtered.settled(firstDay, firstDay, firstDay)
        assertEquals(1, computePeriodStats(days.map { it.asResponseRecord() }).summary.completedCount)
        assertEquals(2, source.timelines.size)

        val reenabled = LoopHistorySnapshot(listOf(active, other)).forStatistics()
        assertEquals(2, reenabled.settled(firstDay, firstDay, firstDay).size)
    }

    @Test fun `all disabled loops produce empty statistics`() {
        val disabled = timeline(responses = listOf(response(firstDay))).history.let {
            it.copy(loop = it.loop.copy(enabled = false))
        }
        val filtered = LoopHistorySnapshot(listOf(disabled)).forStatistics()
        assertTrue(filtered.timelines.isEmpty())
        assertNull(filtered.firstDate)
        assertTrue(computePeriodStats(filtered.settled(firstDay, firstDay, firstDay)
            .map { it.asResponseRecord() }).isEmpty)
    }
}
