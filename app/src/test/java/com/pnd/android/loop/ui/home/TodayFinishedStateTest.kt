package com.pnd.android.loop.ui.home

import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.TodayOccurrence
import com.pnd.android.loop.data.toLoopWithDone
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TodayFinishedStateTest {
    private val today = LocalDate.of(2026, 10, 16)

    private fun loop(id: Int, state: Int) = LoopVo.create(
        id = id, title = "Loop $id", color = 1, created = today.minusDays(2).toMs(),
        startInDay = LoopVo.ANY_TIME, endInDay = LoopVo.ANY_TIME,
        activeDays = LoopDay.EVERYDAY, enabled = true, isAnyTime = true,
    ).toLoopWithDone(LoopDoneVo(loopId = id, date = today.toMs(), done = state))

    private fun section(vararg states: Int) = Section.Today().apply {
        items.value = states.mapIndexed { index, state -> loop(index + 1, state) }
    }

    @Test fun `no scheduled loops is not a finished day`() {
        assertFalse(section().isFinished)
    }

    @Test fun `done and skip both settle the day`() {
        assertTrue(section(DoneState.DONE, DoneState.DONE).isFinished)
        assertTrue(section(DoneState.DONE, DoneState.SKIP).isFinished)
        assertTrue(section(DoneState.SKIP, DoneState.SKIP).isFinished)
    }

    @Test fun `undo and running loops return to the actionable view`() {
        assertFalse(section(DoneState.DONE, DoneState.NO_RESPONSE).isFinished)
        assertFalse(section(DoneState.DONE, DoneState.IN_PROGRESS).isFinished)
    }

    @Test fun `last response updates the same progress without treating skip as completion`() {
        val section = section(DoneState.DONE, DoneState.DONE, DoneState.SKIP, DoneState.NO_RESPONSE)
        assertEquals(TodaySectionProgress(done = 2, skipped = 1, remaining = 1), section.progress)
        section.items.value = section.items.value.dropLast(1) + loop(4, DoneState.DONE)
        assertEquals(TodaySectionProgress(done = 3, skipped = 1, remaining = 0), section.progress)
        section.items.value = section.items.value.dropLast(1) + loop(4, DoneState.SKIP)
        assertEquals(TodaySectionProgress(done = 2, skipped = 2, remaining = 0), section.progress)
        section.items.value = section.items.value.dropLast(1) + loop(4, DoneState.NO_RESPONSE)
        assertEquals(TodaySectionProgress(done = 2, skipped = 1, remaining = 1), section.progress)
    }

    @Test fun `yesterdays waiting occurrence does not count as todays unfinished work`() {
        val section = section(DoneState.DONE)
        val waiting = TodayOccurrence(loop(2, DoneState.NO_RESPONSE), today.minusDays(1), true)
        section.groups.value = listOf(TodayLoopGroup(TodayGroup.AWAITING, listOf(waiting)))
        assertTrue(section.isFinished)
        section.groups.value = listOf(TodayLoopGroup(TodayGroup.AWAITING, listOf(waiting.copy(isCarriedOver = false))))
        assertFalse(section.isFinished)
    }
}
