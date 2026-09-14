package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopVo

/** Persisted user settings, separate from a day's response and UI state. */
data class LoopSettings(
    val title: String,
    val color: Int,
    val startInDay: Long,
    val endInDay: Long,
    val activeDays: Int,
    val enabled: Boolean,
    val isAnyTime: Boolean,
    val weeklyGoal: Int,
) {
    fun applyTo(loop: LoopVo): LoopVo = loop.copy(
        title = title, color = color, startInDay = startInDay, endInDay = endInDay,
        activeDays = activeDays, enabled = enabled, isAnyTime = isAnyTime, weeklyGoal = weeklyGoal,
    )

    fun hasSameSchedule(other: LoopSettings): Boolean =
        startInDay == other.startInDay && endInDay == other.endInDay &&
            activeDays == other.activeDays && enabled == other.enabled && isAnyTime == other.isAnyTime

    companion object {
        fun from(loop: LoopBase) = LoopSettings(
            loop.title, loop.color, loop.startInDay, loop.endInDay,
            loop.activeDays, loop.enabled, loop.isAnyTime, loop.weeklyGoal,
        )
    }
}
