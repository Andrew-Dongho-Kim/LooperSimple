package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.util.MS_1MIN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleDraftTest {
    private val loop = LoopVo(
        loopId = 7,
        title = "Read",
        color = 123,
        created = 42L,
        startInDay = 21 * 60 * MS_1MIN,
        endInDay = (21 * 60 + 30) * MS_1MIN,
        activeDays = LoopDay.WEEKDAYS,
        enabled = true,
        isAnyTime = false,
        weeklyGoal = 3,
    )

    @Test
    fun `unchanged schedule is not dirty`() {
        val draft = ScheduleDraft.from(loop)
        assertFalse(draft.hasChanges(loop))
        assertTrue(draft.validDays)
        assertTrue(draft.validTime)
    }

    @Test
    fun `schedule save preserves latest non-schedule fields`() {
        val latest = loop.copy(title = "New name", color = 999, enabled = false, isMock = true)
        val edited = ScheduleDraft.from(loop).copy(weeklyGoal = 2).applyTo(latest)
        assertEquals(latest.title, edited.title)
        assertEquals(latest.color, edited.color)
        assertEquals(latest.enabled, edited.enabled)
        assertEquals(latest.created, edited.created)
        assertEquals(latest.loopId, edited.loopId)
        assertEquals(latest.isMock, edited.isMock)
        assertEquals(2, edited.weeklyGoal)
    }

    @Test
    fun `fewer days clamp weekly goal`() {
        val draft = ScheduleDraft.from(loop).withDays(LoopDay.WEEKENDS)
        assertEquals(2, draft.weeklyGoal)
        assertTrue(draft.hasChanges(loop))
        assertEquals(2, draft.applyTo(loop).weeklyGoal)
    }

    @Test
    fun `empty days cannot be saved`() {
        val draft = ScheduleDraft.from(loop).withDays(0)
        assertFalse(draft.validDays)
        assertEquals(0, draft.weeklyGoal)
    }

    @Test
    fun `no explicit goal stays unset when days change`() {
        val draft = ScheduleDraft.from(loop).copy(weeklyGoal = 0).withDays(LoopDay.MONDAY)
        assertEquals(0, draft.applyTo(loop).weeklyGoal)
    }

    @Test
    fun `anytime clears persisted times but preserves timed draft`() {
        val timed = ScheduleDraft.from(loop)
        val anytime = timed.copy(isAnyTime = true)
        assertEquals(ANY_TIME, anytime.applyTo(loop).startInDay)
        assertEquals(ANY_TIME, anytime.applyTo(loop).endInDay)
        assertEquals(timed, anytime.copy(isAnyTime = false))
    }

    @Test
    fun `opening anytime initializes valid clock values without making it dirty`() {
        val anytime = loop.copy(isAnyTime = true, startInDay = ANY_TIME, endInDay = ANY_TIME)
        val draft = ScheduleDraft.from(anytime)
        assertFalse(draft.hasChanges(anytime))
        assertTrue(draft.copy(isAnyTime = false).validTime)
        assertTrue(draft.copy(isAnyTime = false).hasChanges(anytime))
    }

    @Test
    fun `minimum duration is fifteen minutes`() {
        val draft = ScheduleDraft.from(loop)
        assertFalse(draft.copy(endInDay = draft.startInDay + 14 * MS_1MIN).validTime)
        assertFalse(draft.copy(endInDay = draft.startInDay).validTime)
        assertTrue(draft.copy(endInDay = draft.startInDay + 15 * MS_1MIN).validTime)
    }

    @Test
    fun `overnight spans preserve existing duration rules`() {
        val draft = ScheduleDraft.from(loop).copy(startInDay = (23 * 60 + 50) * MS_1MIN)
        assertTrue(draft.copy(endInDay = 5 * MS_1MIN).validTime)
        assertFalse(draft.copy(endInDay = 4 * MS_1MIN).validTime)
    }

    @Test
    fun `out of range clock values cannot be saved in timed mode`() {
        val draft = ScheduleDraft.from(loop)
        assertFalse(draft.copy(startInDay = -1).validTime)
        assertFalse(draft.copy(endInDay = 24 * 60 * MS_1MIN).validTime)
        assertTrue(draft.copy(isAnyTime = true, startInDay = -1, endInDay = -1).validTime)
    }
}
