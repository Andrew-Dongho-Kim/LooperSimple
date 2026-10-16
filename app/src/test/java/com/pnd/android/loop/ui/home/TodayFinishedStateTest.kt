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

    @Test fun `yesterdays waiting occurrence does not count as todays unfinished work`() {
        val section = section(DoneState.DONE)
        val waiting = TodayOccurrence(loop(2, DoneState.NO_RESPONSE), today.minusDays(1), true)
        section.groups.value = listOf(TodayLoopGroup(TodayGroup.AWAITING, listOf(waiting)))
        assertTrue(section.isFinished)
        section.groups.value = listOf(TodayLoopGroup(TodayGroup.AWAITING, listOf(waiting.copy(isCarriedOver = false))))
        assertFalse(section.isFinished)
    }
}
