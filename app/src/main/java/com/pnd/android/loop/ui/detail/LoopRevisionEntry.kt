package com.pnd.android.loop.ui.detail

import com.pnd.android.loop.data.history.LoopRevisionVo
import com.pnd.android.loop.data.history.LoopSettings

/** One saved edit, with a stable order for its visible fields. */
internal data class LoopRevisionEntry(
    val revision: LoopRevisionVo,
    val previous: LoopSettings?,
    val fields: List<LoopRevisionField>,
) {
    val isInitial: Boolean get() = previous == null
}

internal enum class LoopRevisionField {
    NAME, COLOR, DAYS, TIME, WEEKLY_GOAL, ENABLED;

    fun changed(before: LoopSettings, after: LoopSettings): Boolean = when (this) {
        NAME -> before.title != after.title
        COLOR -> before.color != after.color
        DAYS -> before.activeDays != after.activeDays
        TIME -> before.isAnyTime != after.isAnyTime ||
            (!after.isAnyTime && (before.startInDay != after.startInDay || before.endInDay != after.endInDay))
        WEEKLY_GOAL -> before.weeklyGoal != after.weeklyGoal
        ENABLED -> before.enabled != after.enabled
    }
}

/** Compare adjacent snapshots in commit order, then show the newest edit first. */
internal fun buildLoopRevisionEntries(revisions: List<LoopRevisionVo>): List<LoopRevisionEntry> {
    val ordered = revisions.sortedBy { it.revisionId }
    return ordered.mapIndexedNotNull { index, revision ->
        val previous = ordered.getOrNull(index - 1)?.settings
        val fields = LoopRevisionField.entries.filter { field ->
            previous == null || field.changed(previous, revision.settings)
        }
        if (fields.isEmpty()) null else LoopRevisionEntry(revision, previous, fields)
    }.asReversed()
}
