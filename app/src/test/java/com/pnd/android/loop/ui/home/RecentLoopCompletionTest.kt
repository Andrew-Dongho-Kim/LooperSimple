package com.pnd.android.loop.ui.home

import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toMs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate

class RecentLoopCompletionTest {
    private val today = LocalDate.of(2026, 9, 11)
    private fun loop(created: LocalDate = today.minusDays(100), days: Int = LoopDay.EVERYDAY) =
        LoopVo.create(id = 1, title = "Test", color = 0, created = created.toMs(),
            startInDay = 0, endInDay = 1, activeDays = days, enabled = true, isMock = false)

    @Test fun `thirty day boundaries exclude today and older responses`() {
        val history = listOf(0L, 1L, 30L, 31L).associate { today.minusDays(it).toMs() to DoneState.DONE }
        val stats = computeRecentLoopCompletion(loop(), history, today)!!
        assertEquals(today.minusDays(30), stats.start)
        assertEquals(today.minusDays(1), stats.end)
        assertEquals(30, stats.totalCount)
        assertEquals(2, stats.doneCount)
        assertEquals(28, stats.unansweredCount)
        assertEquals(7, stats.percent)
    }

    @Test fun `skips and missing responses stay in denominator but disabled days do not`() {
        val history = mapOf(
            today.minusDays(1).toMs() to DoneState.DONE,
            today.minusDays(2).toMs() to DoneState.SKIP,
            today.minusDays(3).toMs() to DoneState.DISABLED,
        )
        val stats = computeRecentLoopCompletion(loop(today.minusDays(6)), history, today)!!
        assertEquals(5, stats.totalCount)
        assertEquals(1, stats.doneCount)
        assertEquals(1, stats.skipCount)
        assertEquals(3, stats.unansweredCount)
        assertEquals(20, stats.percent)
    }

    @Test fun `creation day contributes from the first scheduled occurrence`() {
        val created = today.minusDays(4)
        val stats = computeRecentLoopCompletion(loop(created), mapOf(created.toMs() to DoneState.DONE), today)!!
        assertEquals(created, stats.start)
        assertEquals(4, stats.totalCount)
        assertEquals(25, stats.percent)
    }

    @Test fun `insufficient samples are hidden but genuine zero is shown`() {
        assertNull(computeRecentLoopCompletion(loop(today), null, today))
        assertNull(computeRecentLoopCompletion(loop(today.minusDays(2)), null, today))
        assertEquals(0, computeRecentLoopCompletion(loop(today.minusDays(4)), null, today)!!.percent)
        assertNull(computeRecentLoopCompletion(loop(today.plusDays(1)), null, today))
    }

    @Test fun `explicitly saved runs count even outside scheduled weekdays`() {
        val scheduledLoop = loop(days = LoopDay.WEEKDAYS)
        val history = (1L..30L).associate { today.minusDays(it).toMs() to DoneState.DONE }
        val stats = computeRecentLoopCompletion(scheduledLoop, history, today)!!
        val expected = 30
        assertEquals(expected, stats.totalCount)
        assertEquals(expected, stats.doneCount)
        assertEquals(100, stats.percent)
    }

    @Test fun `rounded percentage and explanation counts agree`() {
        val history = (1L..12L).associate { today.minusDays(it).toMs() to DoneState.DONE }
        val stats = computeRecentLoopCompletion(loop(today.minusDays(15)), history, today)!!
        assertEquals(15, stats.totalCount)
        assertEquals(12, stats.doneCount)
        assertEquals(3, stats.unansweredCount)
        assertEquals(80, stats.percent)
    }

    @Test fun `disabled and mock loops never produce a chip`() {
        assertNull(computeRecentLoopCompletion(loop().copy(enabled = false), null, today))
        assertNull(computeRecentLoopCompletion(loop().copy(isMock = true), null, today))
        val history = (1L..30L).associate { today.minusDays(it).toMs() to DoneState.DISABLED }
        assertNull(computeRecentLoopCompletion(loop(), history, today))
    }

    @Test fun `rollover drops the oldest day and includes the newly settled date`() {
        val history = mapOf(today.toMs() to DoneState.DONE, today.minusDays(30).toMs() to DoneState.SKIP)
        val before = computeRecentLoopCompletion(loop(), history, today)!!
        val after = computeRecentLoopCompletion(loop(), history, today.plusDays(1))!!
        assertEquals(0, before.doneCount)
        assertEquals(1, before.skipCount)
        assertEquals(1, after.doneCount)
        assertEquals(0, after.skipCount)
        assertEquals(today, after.end)
    }
}
