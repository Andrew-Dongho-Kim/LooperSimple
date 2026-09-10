package com.pnd.android.loop.ui.detail

import androidx.compose.runtime.saveable.SaverScope
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.util.MS_1MIN
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LoopEditorDraftTest {
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
    fun `unchanged draft and outer whitespace do not enable save`() {
        val draft = LoopEditorDraft.from(loop)
        assertFalse(draft.hasChanges(loop))
        assertFalse(draft.copy(title = "  Read  ").hasChanges(loop))
        assertTrue(draft.copy(title = "Write").hasChanges(loop))
        assertTrue(draft.copy(color = 456).hasChanges(loop))
        assertTrue(draft.copy(schedule = draft.schedule.copy(weeklyGoal = 2)).hasChanges(loop))
    }

    @Test
    fun `save applies all editable fields and preserves identity and status`() {
        val latest = loop.copy(enabled = false, isMock = true)
        val draft = LoopEditorDraft.from(loop).copy(title = "  Write  ", color = 456)
        val edited = draft.copy(schedule = draft.schedule.withDays(LoopDay.WEEKENDS)).applyTo(latest)
        assertEquals("Write", edited.title)
        assertEquals(456, edited.color)
        assertEquals(LoopDay.WEEKENDS, edited.activeDays)
        assertEquals(2, edited.weeklyGoal)
        assertEquals(latest.loopId, edited.loopId)
        assertEquals(latest.created, edited.created)
        assertEquals(latest.enabled, edited.enabled)
        assertEquals(latest.isMock, edited.isMock)
        assertEquals(latest.startInDay, edited.startInDay)
        assertEquals(latest.endInDay, edited.endInDay)
    }

    @Test
    fun `anytime draft keeps valid clock defaults without becoming dirty`() {
        val anytime = loop.copy(isAnyTime = true, startInDay = ANY_TIME, endInDay = ANY_TIME)
        val draft = LoopEditorDraft.from(anytime)
        assertFalse(draft.hasChanges(anytime))
        assertTrue(draft.schedule.copy(isAnyTime = false).validTime)
        assertEquals(ANY_TIME, draft.applyTo(anytime).startInDay)
        assertEquals(ANY_TIME, draft.applyTo(anytime).endInDay)
    }

    @Test
    fun `saved state restores name color and full schedule draft`() {
        val draft = LoopEditorDraft.from(loop).copy(title = "  새 이름  ", color = 456)
        val changed = draft.copy(schedule = draft.schedule.withDays(LoopDay.WEEKENDS).copy(isAnyTime = true))
        val saved = with(LoopEditorDraft.Saver) {
            SaverScope { true }.save(changed)
        }
        assertEquals(changed, LoopEditorDraft.Saver.restore(requireNotNull(saved)))
    }

    @Test
    fun `empty day selection is invalid and clears unreachable goal`() {
        val draft = LoopEditorDraft.from(loop)
        val changed = draft.copy(schedule = draft.schedule.withDays(0))
        assertFalse(changed.schedule.validDays)
        assertEquals(0, changed.schedule.weeklyGoal)
    }
}
