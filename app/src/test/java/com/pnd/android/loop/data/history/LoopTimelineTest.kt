package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.*
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.util.toMs
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.util.TimeZone

class LoopTimelineTest {
    private val change = firstDay.plusDays(7)

    @Test fun `later settings never rewrite earlier scheduled days`() {
        val old = testLoop()
        val new = old.copy(title = "After", color = 20, activeDays = LoopDay.MONDAY,
            startInDay = 12 * 3_600_000L, endInDay = 13 * 3_600_000L)
        val history = timeline(new, listOf(revision(2, new, change), revision(1, old)))
        assertEquals("Before", history.day(firstDay)!!.loop.title)
        assertEquals(10, history.day(firstDay)!!.loop.color)
        assertEquals(old.startInDay, history.day(firstDay)!!.loop.startInDay)
        assertNull(history.day(change)) // Tuesday removed from the schedule.
        assertEquals("After", history.day(change.plusDays(6))!!.loop.title)
    }

    @Test fun `saved occurrence retains the revision from before a same day edit`() {
        val old = testLoop()
        val new = old.copy(title = "New", color = 25, activeDays = LoopDay.MONDAY, isAnyTime = true,
            startInDay = -1, endInDay = -1)
        for (state in listOf(DoneState.DONE, DoneState.SKIP, DoneState.IN_PROGRESS, DoneState.NO_RESPONSE)) {
            val history = timeline(new, listOf(revision(1, old), revision(2, new, change)), listOf(response(change, state)))
            assertEquals(old, history.day(change)!!.loop)
            assertEquals(state, history.day(change)!!.response.done)
            assertEquals("New", history.liveLoop(change).title)
            assertEquals(25, history.liveLoop(change).color)
            assertFalse(history.liveLoop(change).isAnyTime)
        }
    }

    @Test fun `multiple same day edits resolve by revision id not collection order`() {
        val old = testLoop()
        val last = old.copy(title = "Last")
        val history = timeline(last, listOf(revision(3, last, change), revision(1, old),
            revision(2, old.copy(title = "Middle"), change)))
        assertEquals("Last", history.settingsOn(change).title)
        assertEquals("Before", history.settingsOn(change.minusDays(1)).title)
    }

    @Test fun `weekly goal changes begin next week including subsequent metadata edits`() {
        val old = testLoop()
        val new = old.copy(weeklyGoal = 2)
        val monday = nextWeekStart(change)
        val history = timeline(new, listOf(revision(1, old), revision(2, new, change, monday),
            revision(3, new.copy(title = "Renamed"), change.plusDays(1), monday)))
        assertEquals(5, history.goalOn(change))
        assertEquals(5, history.goalOn(monday.minusDays(1)))
        assertEquals(2, history.goalOn(monday))
    }

    @Test fun `revising pending goal applies latest choice without changing this week`() {
        val old = testLoop()
        val monday = nextWeekStart(change)
        val history = timeline(old.copy(weeklyGoal = 3), listOf(revision(1, old),
            revision(2, old.copy(weeklyGoal = 2), change, monday),
            revision(3, old.copy(weeklyGoal = 3), change.plusDays(1), monday)))
        assertEquals(5, history.goalOn(change.plusDays(2)))
        assertEquals(3, history.goalOn(monday))
    }

    @Test fun `disabled interval is excluded and enabling does not backfill it`() {
        val old = testLoop()
        val history = timeline(old, listOf(revision(1, old), revision(2, old.copy(enabled = false), change),
            revision(3, old, change.plusDays(3))))
        assertNull(history.day(change))
        assertNull(history.day(change.plusDays(2)))
        assertNotNull(history.day(change.plusDays(3)))
        assertNotNull(history.day(change.minusDays(1)))
    }

    @Test fun `legacy disabled row remains excluded despite current enabled settings`() {
        assertNull(timeline(responses = listOf(response(change, DoneState.DISABLED))).day(change))
    }

    @Test fun `missing legacy dates retain frozen estimated baseline after edits`() {
        val old = testLoop()
        val new = old.copy(activeDays = LoopDay.MONDAY)
        val history = timeline(new, listOf(revision(1, old, knownFrom = change),
            revision(2, new, change, knownFrom = change)))
        assertTrue(history.day(firstDay)!!.estimated)
        assertEquals(7, history.days(firstDay, change.minusDays(1)).size)
        assertFalse(history.day(change.plusDays(6))!!.estimated)
    }

    @Test fun `today pending and any running run do not lower final rate`() {
        val yesterday = change.minusDays(1)
        assertFalse(timeline().day(change)!!.isSettled(change))
        assertTrue(timeline().day(yesterday)!!.isSettled(change))
        for (state in listOf(DoneState.DONE, DoneState.SKIP)) {
            assertTrue(timeline(responses = listOf(response(change, state))).day(change)!!.isSettled(change))
        }
        assertFalse(timeline(responses = listOf(response(yesterday, DoneState.IN_PROGRESS)))
            .day(yesterday)!!.isSettled(change))
        assertFalse(timeline().day(change.plusDays(1))!!.isSettled(change))
    }

    @Test fun `explicit off schedule run is preserved in numerator and denominator`() {
        val loop = testLoop().copy(activeDays = LoopDay.MONDAY)
        val history = timeline(loop, responses = listOf(response(change)))
        assertFalse(history.day(change)!!.scheduled)
        assertTrue(history.day(change)!!.isSettled(change))
    }

    @Test fun `note on an unscheduled day is visible without creating a denominator`() {
        val loop = testLoop().copy(activeDays = LoopDay.MONDAY)
        val history = timeline(loop, notes = listOf(LoopRetrospectVo(1, change.toMs(), "Note", change.toEpochDay())))
        val day = history.day(change)!!
        assertEquals("Note", day.note)
        assertFalse(day.hasOccurrence)
        assertFalse(day.isSettled(change.plusDays(1)))
    }

    @Test fun `creation date is included and earlier dates never contribute`() {
        val history = timeline(responses = listOf(response(firstDay.minusDays(1))))
        assertNull(history.day(firstDay.minusDays(1)))
        assertNotNull(history.day(firstDay))
    }

    @Test fun `stored local day survives timezone changes`() {
        val record = response(change)
        val previousZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            assertEquals(change, record.localDate())
        } finally { TimeZone.setDefault(previousZone) }
    }
    @Test fun `creation day stays stable after changing timezone`() {
        val history = timeline().history
        val previousZone = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val moved = LoopTimeline(history)
            assertEquals(firstDay, moved.createdDate)
            assertNull(moved.day(firstDay.minusDays(1)))
        } finally { TimeZone.setDefault(previousZone) }
    }

}
