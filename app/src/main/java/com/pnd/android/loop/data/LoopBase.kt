package com.pnd.android.loop.data

import android.content.Context
import androidx.compose.runtime.Immutable
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.toLocalTime

@Immutable
interface LoopBase {
    val loopId: Int
    val title: String
    val color: Int
    val created: Long
    val startInDay: Long
    val endInDay: Long
    val activeDays: Int
    val interval: Long
    val enabled: Boolean
    val isAnyTime: Boolean
    val isMock: Boolean
    fun copyAs(
        loopId: Int = this.loopId,
        title: String = this.title,
        color: Int = this.color,
        created: Long = this.created,
        startInDay: Long = this.startInDay,
        endInDay: Long = this.endInDay,
        activeDays: Int = this.activeDays,
        interval: Long = this.interval,
        enabled: Boolean = this.enabled,
        isAnyTime: Boolean = this.isAnyTime,
        isMock: Boolean = false,
    ): LoopBase
}

fun LoopBase.isTogether(loop: LoopBase) =
    (loop.startInDay in startInDay..<endInDay) || (loop.endInDay in (startInDay + 1)..<endInDay)

fun LoopBase.description(context: Context) =
    """ -->
    |*Loop  
    | title : $title
    | loopStart : ${startInDay.toLocalTime()}
    | loopEnd : ${endInDay.toLocalTime()}
    | activeDays : ${LoopDay.description(activeDays)}
    | interval : ${intervalString(context, interval)}
    | enabled : $enabled
    | isMock : $isMock""".trimMargin()


val LoopBase.doneState get() = (this as? LoopWithDone)?.done
val LoopBase.isRespond get() = doneState == DoneState.DONE || doneState == DoneState.SKIP
val LoopBase.isNotRespond get() = doneState == DoneState.NO_RESPONSE
val LoopBase.isDisabled get() = doneState == DoneState.DISABLED

