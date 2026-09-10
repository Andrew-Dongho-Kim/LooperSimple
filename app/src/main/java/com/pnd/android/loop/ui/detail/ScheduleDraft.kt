package com.pnd.android.loop.ui.detail

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.saveable.listSaver
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import com.pnd.android.loop.ui.home.input.selector.isLoopDurationTooShort
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN

/** 일정만 저장한다. 편집하는 동안 바뀐 이름·색·활성 상태는 덮어쓰지 않는다. */
@Immutable
internal data class ScheduleDraft(
    val activeDays: Int,
    val isAnyTime: Boolean,
    val startInDay: Long,
    val endInDay: Long,
    val weeklyGoal: Int,
) {
    val validDays: Boolean get() = activeDayCount(activeDays) > 0
    val validTime: Boolean get() = isAnyTime || (
        startInDay in 0 until MS_1DAY && endInDay in 0 until MS_1DAY &&
            !isLoopDurationTooShort(startInDay, endInDay)
        )

    fun withDays(days: Int) = copy(
        activeDays = days,
        weeklyGoal = weeklyGoal.coerceIn(0, activeDayCount(days)),
    )

    fun applyTo(loop: LoopBase): LoopBase = loop.copyAs(
        activeDays = activeDays,
        isAnyTime = isAnyTime,
        startInDay = if (isAnyTime) ANY_TIME else startInDay,
        endInDay = if (isAnyTime) ANY_TIME else endInDay,
        weeklyGoal = weeklyGoal.coerceIn(0, activeDayCount(activeDays)),
        isMock = loop.isMock,
    )

    fun hasChanges(loop: LoopBase): Boolean = activeDays != loop.activeDays ||
        isAnyTime != loop.isAnyTime || weeklyGoal != loop.weeklyGoal ||
        (!isAnyTime && (startInDay != loop.startInDay || endInDay != loop.endInDay))

    companion object {
        fun from(loop: LoopBase) = ScheduleDraft(
            activeDays = loop.activeDays,
            isAnyTime = loop.isAnyTime,
            // ANY_TIME(-1)을 시각 선택기에 전달하지 않는다.
            startInDay = loop.startInDay.takeIf { it in 0 until MS_1DAY } ?: (9 * 60 * MS_1MIN),
            endInDay = loop.endInDay.takeIf { it in 0 until MS_1DAY } ?: ((9 * 60 + 30) * MS_1MIN),
            weeklyGoal = loop.weeklyGoal,
        )

        val Saver = listSaver<ScheduleDraft, Any>(
            save = { listOf(it.activeDays, it.isAnyTime, it.startInDay, it.endInDay, it.weeklyGoal) },
            restore = { ScheduleDraft(it[0] as Int, it[1] as Boolean, it[2] as Long, it[3] as Long, it[4] as Int) },
        )
    }
}
