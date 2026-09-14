package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.history.LoopRevisionVo
import com.pnd.android.loop.data.history.LoopSettings
import org.junit.Assert.*
import org.junit.Test

class LoopRevisionEntryTest {
    private val initial = LoopSettings("Before", 0xff336699.toInt(), 32_400_000, 36_000_000,
        activeDays = 127, enabled = true, isAnyTime = false, weeklyGoal = 5)

    private fun revision(id: Long, settings: LoopSettings = initial, recordedAt: Long = id * 1000) =
        LoopRevisionVo(id, loopId = 1, recordedAt = recordedAt, effectiveFrom = 20_710,
            goalEffectiveFrom = 20_717, knownFrom = 20_710, settings = settings)

    @Test fun `empty history has no invented initial snapshot`() {
        assertTrue(buildLoopRevisionEntries(emptyList()).isEmpty())
    }

    @Test fun `initial snapshot is distinct from edits and retains all settings`() {
        val entry = buildLoopRevisionEntries(listOf(revision(1))).single()
        assertTrue(entry.isInitial)
        assertNull(entry.previous)
        assertEquals(LoopRevisionField.entries, entry.fields)
        assertEquals(initial, entry.revision.settings)
    }

    @Test fun `adjacent saved snapshots are compared in commit order even if clock moved back`() {
        val renamed = initial.copy(title = "Renamed")
        val recolored = renamed.copy(color = 0xffeeeeee.toInt())
        val entries = buildLoopRevisionEntries(listOf(
            revision(3, recolored, recordedAt = 500), revision(1), revision(2, renamed),
        ))
        assertEquals(listOf(3L, 2L, 1L), entries.map { it.revision.revisionId })
        assertEquals(listOf(LoopRevisionField.COLOR), entries[0].fields)
        assertEquals(renamed, entries[0].previous)
        assertEquals(listOf(LoopRevisionField.NAME), entries[1].fields)
        assertEquals(initial.title, entries[1].previous!!.title)
        assertEquals("Renamed", entries[1].revision.settings.title)
    }

    @Test fun `multi field edits retain before after values and the separate goal effective date`() {
        val after = initial.copy(title = "After", color = 0xff112233.toInt(), activeDays = 2,
            startInDay = 40_000_000, endInDay = 45_000_000, weeklyGoal = 2, enabled = false)
        val entry = buildLoopRevisionEntries(listOf(revision(1), revision(2, after))).first()
        assertEquals(LoopRevisionField.entries, entry.fields)
        assertEquals(initial, entry.previous)
        assertEquals(after, entry.revision.settings)
        assertEquals(20_710L, entry.revision.effectiveFrom)
        assertEquals(20_717L, entry.revision.goalEffectiveFrom)
    }

    @Test fun `time and anytime switch are displayed as one meaningful change`() {
        val anytime = initial.copy(isAnyTime = true, startInDay = -1, endInDay = -1)
        val entries = buildLoopRevisionEntries(listOf(revision(1), revision(2, anytime), revision(3, initial)))
        assertEquals(listOf(LoopRevisionField.TIME), entries[0].fields)
        assertEquals(listOf(LoopRevisionField.TIME), entries[1].fields)
        assertTrue(entries[0].previous!!.isAnyTime)
        assertFalse(entries[0].revision.settings.isAnyTime)
    }

    @Test fun `redundant snapshots do not add fake edits or hide a later real edit`() {
        val entries = buildLoopRevisionEntries(listOf(revision(1), revision(2),
            revision(3, initial.copy(weeklyGoal = 3))))
        assertEquals(listOf(3L, 1L), entries.map { it.revision.revisionId })
        assertEquals(listOf(LoopRevisionField.WEEKLY_GOAL), entries[0].fields)
        assertEquals(5, entries[0].previous!!.weeklyGoal)
    }

    @Test fun `unused times do not look like a time change while both snapshots are anytime`() {
        val anytime = initial.copy(isAnyTime = true, startInDay = -1, endInDay = -1)
        val entries = buildLoopRevisionEntries(listOf(revision(1, anytime),
            revision(2, anytime.copy(title = "After", startInDay = 10, endInDay = 20))))
        assertEquals(listOf(LoopRevisionField.NAME), entries.first().fields)
    }
}
