package com.pnd.android.loop.ui.detail

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.listSaver
import com.pnd.android.loop.data.LoopBase

/** 저장 전까지 화면 안에만 머무는 초안. 일정 검증과 보정은 ScheduleDraft에 위임한다. */
@Immutable
internal data class LoopEditorDraft(
    val title: String,
    val color: Int,
    val schedule: ScheduleDraft,
) {
    fun applyTo(loop: LoopBase): LoopBase = schedule.applyTo(loop).copyAs(
        title = title.trim(),
        color = color,
        isMock = loop.isMock,
    )

    fun hasChanges(loop: LoopBase): Boolean = title.trim() != loop.title ||
        color != loop.color || schedule.hasChanges(loop)

    companion object {
        fun from(loop: LoopBase) = LoopEditorDraft(loop.title, loop.color, ScheduleDraft.from(loop))

        val Saver = listSaver<LoopEditorDraft, Any>(
            save = {
                listOf(it.title, it.color, it.schedule.activeDays, it.schedule.isAnyTime,
                    it.schedule.startInDay, it.schedule.endInDay, it.schedule.weeklyGoal)
            },
            restore = {
                LoopEditorDraft(
                    title = it[0] as String,
                    color = it[1] as Int,
                    schedule = ScheduleDraft(it[2] as Int, it[3] as Boolean,
                        it[4] as Long, it[5] as Long, it[6] as Int),
                )
            },
        )
    }
}
