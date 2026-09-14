package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import org.junit.Assert.*
import org.junit.Test
import java.time.Clock
import java.time.ZoneId

class OccurrenceMutationTest {
    private val zone = ZoneId.systemDefault()
    private fun clock(hour: Int, date: java.time.LocalDate = firstDay) =
        Clock.fixed(date.atTime(hour, 0).atZone(zone).toInstant(), zone)

    @Test fun `timed plan is preserved from the exact scheduled start onward`() {
        val day = timeline().day(firstDay)!!
        assertFalse(shouldPreserveOccurrence(day, firstDay.atTime(8, 59)))
        assertTrue(shouldPreserveOccurrence(day, firstDay.atTime(9, 0)))
        assertTrue(shouldPreserveOccurrence(day, firstDay.atTime(23, 0)))
        assertFalse(shouldPreserveOccurrence(null, firstDay.atTime(10, 0)))
    }

    @Test fun `answered and running occurrences are preserved even before planned start`() {
        for (state in listOf(DoneState.DONE, DoneState.SKIP, DoneState.IN_PROGRESS)) {
            val day = timeline(responses = listOf(response(firstDay, state))).day(firstDay)!!
            assertTrue(shouldPreserveOccurrence(day, firstDay.atTime(1, 0)))
        }
    }

    @Test fun `anytime unanswered occurrence remains changeable until start`() {
        val loop = testLoop().copy(isAnyTime = true, startInDay = -1, endInDay = -1)
        assertFalse(shouldPreserveOccurrence(timeline(loop).day(firstDay), firstDay.atTime(23, 0)))
    }

    @Test fun `start and stop across midnight keep date plan and real duration`() {
        val loop = testLoop().copy(isAnyTime = true, startInDay = -1, endInDay = -1)
        val initial = plannedRecord(loop, firstDay, 7)
        val started = updatedResponse(initial, loop, DoneState.IN_PROGRESS, null, clock(23))
        val done = updatedResponse(started, loop, DoneState.DONE, null, clock(1, firstDay.plusDays(1)))
        assertEquals(firstDay, done.localDate())
        assertEquals(7L, done.revisionId)
        assertEquals(2 * 3_600_000L, done.measuredDurationMs())
        assertEquals(LoopDoneVo.TimeSource.MEASURED, done.timeSource)
    }

    @Test fun `measured duration supports more than one day`() {
        val loop = testLoop().copy(isAnyTime = true)
        val started = updatedResponse(plannedRecord(loop, firstDay, 1), loop, DoneState.IN_PROGRESS, null, clock(1))
        val done = updatedResponse(started, loop, DoneState.DONE, null, clock(3, firstDay.plusDays(1)))
        assertEquals(26 * 3_600_000L, done.measuredDurationMs())
    }

    @Test fun `state only changes preserve saved time and revision`() {
        val before = response(firstDay).copy(startInDay = 1234, endInDay = 5678)
        val newSettings = testLoop().copy(startInDay = 1, endInDay = 2)
        val after = updatedResponse(before, newSettings, DoneState.SKIP, null, clock(12))
        assertEquals(before.startInDay, after.startInDay)
        assertEquals(before.endInDay, after.endInDay)
        assertEquals(before.revisionId, after.revisionId)
    }

    @Test fun `manual times equal to plan are still identified as user entered`() {
        val loop = testLoop()
        val after = updatedResponse(plannedRecord(loop, firstDay, 1), loop, DoneState.DONE,
            loop.startInDay to loop.endInDay, clock(12))
        assertEquals(LoopDoneVo.TimeSource.USER_ENTERED, after.timeSource)
    }

    @Test fun `automatic completion preserves planned source rather than inventing actual start`() {
        val loop = testLoop()
        val after = updatedResponse(plannedRecord(loop, firstDay, 1), loop, DoneState.DONE, null, clock(12))
        assertEquals(LoopDoneVo.TimeSource.PLANNED, after.timeSource)
        assertNull(after.startedAt)
    }

    @Test fun `resetting anytime clears duration and keeps the historical plan identity`() {
        val loop = testLoop().copy(isAnyTime = true)
        val before = response(firstDay).copy(startedAt = 1, endedAt = 99)
        val after = updatedResponse(before, loop, DoneState.NO_RESPONSE, null, clock(12))
        assertEquals(-1L, after.startInDay)
        assertEquals(-1L, after.endInDay)
        assertNull(after.measuredDurationMs())
        assertEquals(before.revisionId, after.revisionId)
    }

    @Test fun `next week boundary is Monday even when edited on Monday or Sunday`() {
        val monday = java.time.LocalDate.of(2026, 9, 14)
        assertEquals(monday.plusWeeks(1), nextWeekStart(monday))
        assertEquals(monday, nextWeekStart(monday.minusDays(1)))
    }
    @Test fun `explicit times override the running timer when recording completion manually`() {
        val loop = testLoop().copy(isAnyTime = true)
        val before = response(firstDay, DoneState.IN_PROGRESS).copy(startedAt = 1000)
        val after = updatedResponse(before, loop, DoneState.DONE, 5000L to 9000L, clock(12))
        assertEquals(5000L, after.startInDay)
        assertEquals(9000L, after.endInDay)
        assertEquals(LoopDoneVo.TimeSource.USER_ENTERED, after.timeSource)
        assertNull(after.startedAt)
        assertNull(after.endedAt)
    }

}
