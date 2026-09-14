package com.pnd.android.loop.data

import androidx.room.Ignore
import com.pnd.android.loop.data.common.DEFAULT_WEEKLY_GOAL

data class FullLoopVo @JvmOverloads constructor(
    override val loopId: Int,
    override val color: Int,
    override val title: String,
    override val created: Long,
    override val startInDay: Long,
    override val endInDay: Long,
    override val activeDays: Int,
    override val enabled: Boolean,
    val actualStartInDay: Long,
    val actualEndInDay: Long,
    val date: Long,
    val retrospect: String,
    @LoopDoneVo.DoneState val done: Int,
    override val isAnyTime: Boolean = false,
    override val weeklyGoal: Int = DEFAULT_WEEKLY_GOAL,
    @Ignore override val isMock: Boolean = false,
    val measuredDurationMs: Long? = null,
) : LoopBase {

    override fun copyAs(
        loopId: Int,
        title: String,
        color: Int,
        created: Long,
        startInDay: Long,
        endInDay: Long,
        activeDays: Int,
        enabled: Boolean,
        isAnyTime: Boolean,
        weeklyGoal: Int,
        isMock: Boolean,
    ): LoopBase = FullLoopVo(
        loopId = loopId,
        title = title,
        color = color,
        created = created,
        startInDay = startInDay,
        endInDay = endInDay,
        activeDays = activeDays,
        enabled = enabled,
        actualStartInDay = this.actualStartInDay,
        actualEndInDay = this.actualEndInDay,
        date = this.date,
        done = this.done,
        isAnyTime = isAnyTime,
        weeklyGoal = weeklyGoal,
        isMock = isMock,
        retrospect = this.retrospect,
        measuredDurationMs = this.measuredDurationMs,
    )
}

fun LoopBase.toFullLoopVo(
    retrospectVo: LoopRetrospectVo?,
    doneVo: LoopDoneVo
): FullLoopVo {
    return FullLoopVo(
        loopId = loopId,
        title = title,
        color = color,
        created = created,
        startInDay = startInDay,
        endInDay = endInDay,
        activeDays = activeDays,
        enabled = enabled,
        actualStartInDay = doneVo.startInDay,
        actualEndInDay = doneVo.endInDay,
        date = doneVo.date,
        retrospect = retrospectVo?.text ?: "",
        done = doneVo.done,
        isAnyTime = isAnyTime,
        weeklyGoal = weeklyGoal,
        isMock = isMock,
        measuredDurationMs = doneVo.measuredDurationMs(),
    )
}
