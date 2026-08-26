package com.pnd.android.loop.data

import androidx.compose.runtime.Immutable
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.common.NO_WEEKLY_GOAL
import com.pnd.android.loop.util.overlapsInTime
import com.pnd.android.loop.util.toTimeTextForLog

@Immutable
interface LoopBase {
    val loopId: Int
    val title: String
    val color: Int
    val created: Long
    val startInDay: Long
    val endInDay: Long
    val activeDays: Int
    val enabled: Boolean
    val isAnyTime: Boolean

    /** 주간 목표 횟수. 0([NO_WEEKLY_GOAL])이면 활동 요일 전부를 해야 하는 날로 본다. */
    val weeklyGoal: Int
    val isMock: Boolean
    fun copyAs(
        loopId: Int = this.loopId,
        title: String = this.title,
        color: Int = this.color,
        created: Long = this.created,
        startInDay: Long = this.startInDay,
        endInDay: Long = this.endInDay,
        activeDays: Int = this.activeDays,
        enabled: Boolean = this.enabled,
        isAnyTime: Boolean = this.isAnyTime,
        weeklyGoal: Int = this.weeklyGoal,
        isMock: Boolean = false,
    ): LoopBase
}

/** 이 루프가 한 주에 해야 하는 횟수. 목표가 없으면 활동 요일 수를 그대로 쓴다. */
fun LoopBase.weeklyTarget(): Int =
    if (weeklyGoal > 0) weeklyGoal else LoopDay.ALL.count { activeDays.isOn(it) }


// 두 루프의 시간대가 겹치는지(자정을 넘겨 감기는 구간까지 고려). 원형 24시간 겹침 판정을 재사용한다.
fun LoopBase.isTogether(loop: LoopBase) = overlapsInTime(loop)

fun LoopBase.description() =
    """ -->
    |*Loop
    | title : $title
    | loopStart : ${startInDay.toTimeTextForLog()}
    | loopEnd : ${endInDay.toTimeTextForLog()}
    | activeDays : ${LoopDay.description(activeDays)}
    | enabled : $enabled
    | isMock : $isMock""".trimMargin()


val LoopBase.actualStartInDay get() = (this as? LoopWithDone)?.actualStartInDay ?: startInDay
val LoopBase.actualEndInDay get() = (this as? LoopWithDone)?.actualEndInDay ?: endInDay
val LoopBase.doneState get() = (this as? LoopWithDone)?.done
val LoopBase.isRespond get() = doneState == DoneState.DONE || doneState == DoneState.SKIP
val LoopBase.isNotRespond get() = doneState == DoneState.NO_RESPONSE
val LoopBase.isInProgress get() = doneState == DoneState.IN_PROGRESS
val LoopBase.isDisabled get() = doneState == DoneState.DISABLED

