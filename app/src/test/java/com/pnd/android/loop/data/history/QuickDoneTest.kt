package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.TimeSource
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.state.QuickDone
import com.pnd.android.loop.state.plannedRecord
import com.pnd.android.loop.state.quickDoneFor
import com.pnd.android.loop.state.updatedResponse
import com.pnd.android.loop.util.toMs
import java.time.Clock
import java.time.ZoneId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QuickDoneTest {
    private val hour = 3_600_000L
    private val minute = 60_000L
    private val anytime = testLoop().copy(isAnyTime = true, startInDay = -1, endInDay = -1)

    @Test fun `timed loop finished early records planned start until now`() {
        val loop = testLoop() // 09:00 - 10:00
        val now = 9 * hour + 40 * minute
        assertEquals(QuickDone.EarlyFinish(9 * hour to now), quickDoneFor(loop, now, isInWindow = true))
    }

    @Test fun `timed loop outside its window keeps the plan`() {
        assertEquals(QuickDone.AsPlanned, quickDoneFor(testLoop(), 11 * hour, isInWindow = false))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `anytime loops have no one tap completion`() {
        quickDoneFor(anytime, 12 * hour, isInWindow = false)
    }

    @Test fun `no time completion drops times recorded by a timer`() {
        val clock = Clock.fixed(firstDay.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
        val running = response(firstDay, DoneState.IN_PROGRESS).copy(startedAt = 1000, timeSource = TimeSource.MEASURED)
        val after = updatedResponse(running, anytime, DoneState.DONE, null, clock, clearTimes = true)
        assertEquals(DoneState.DONE, after.done)
        assertEquals(-1L, after.startInDay)
        assertNull(after.measuredDurationMs())
        assertEquals(TimeSource.UNKNOWN, after.timeSource)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `timed loops cannot drop their times`() {
        val clock = Clock.fixed(firstDay.atTime(12, 0).atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault())
        updatedResponse(plannedRecord(testLoop(), firstDay, 1), testLoop(), DoneState.DONE, null, clock, clearTimes = true)
    }

    // region usual duration

    private fun done(daysAgo: Long, minutes: Long, source: Int = TimeSource.USER_ENTERED, start: Long = 20 * hour) =
        response(firstDay.plusDays(30 - daysAgo)).copy(
            startInDay = start, endInDay = (start + minutes * minute) % (24 * hour), timeSource = source,
        )

    private fun usual(vararg records: LoopDoneVo) =
        timeline(anytime, responses = records.toList()).usualDurationMinutes(firstDay.plusDays(30))

    @Test fun `usual duration needs at least three samples`() {
        assertNull(usual(done(1, 20), done(2, 30)))
        assertEquals(25, usual(done(1, 20), done(2, 30), done(3, 25)))
    }

    @Test fun `usual duration is the median so one forgotten stop does not drag it`() {
        assertEquals(20, usual(done(1, 20), done(2, 18), done(3, 22), done(4, 240)))
    }

    @Test fun `planned and unknown records are not samples`() {
        assertNull(usual(done(1, 20), done(2, 20, TimeSource.UNKNOWN), done(3, 20, TimeSource.PLANNED)))
    }

    @Test fun `measured timer duration is used across midnight`() {
        val measured = response(firstDay.plusDays(29)).copy(
            startInDay = 23 * hour + 50 * minute, endInDay = 20 * minute,
            timeSource = TimeSource.MEASURED, startedAt = 0, endedAt = 30 * minute,
        )
        assertEquals(30, usual(measured, done(2, 30, start = 23 * hour + 45 * minute), done(3, 30)))
    }

    @Test fun `short habits round to the minute and longer ones to five minutes`() {
        assertEquals(3, usual(done(1, 3), done(2, 3), done(3, 4)))
        assertEquals(45, usual(done(1, 43), done(2, 44), done(3, 46)))
    }

    @Test fun `skipped and old records are ignored`() {
        val skipped = done(1, 20).copy(done = DoneState.SKIP)
        assertNull(usual(skipped, done(2, 20), done(3, 20).copy(date = firstDay.minusDays(40).toMs(),
            localEpochDay = firstDay.minusDays(40).toEpochDay())))
    }

    // endregion
}
