package com.pnd.android.loop.data

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Immutable
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.common.DEFAULT_ACTIVE_DAYS
import com.pnd.android.loop.data.common.DEFAULT_COLOR
import com.pnd.android.loop.data.common.DEFAULT_CREATED
import com.pnd.android.loop.data.common.DEFAULT_ENABLED
import com.pnd.android.loop.data.common.DEFAULT_INTERVAL
import com.pnd.android.loop.data.common.DEFAULT_IS_MOCK
import com.pnd.android.loop.data.common.DEFAULT_TITLE
import com.pnd.android.loop.data.common.defaultEndInDay
import com.pnd.android.loop.data.common.defaultStartInDay
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.toLocalTime

@Immutable
interface LoopBase {
    val id: Int
    val title: String
    val color: Int
    val created: Long
    val startInDay: Long
    val endInDay: Long
    val activeDays: Int
    val interval: Long
    val enabled: Boolean
    val isMock: Boolean
    fun copyAs(
        id: Int = this.id,
        title: String = this.title,
        color: Int = this.color,
        created: Long = this.created,
        startInDay: Long = this.startInDay,
        endInDay: Long = this.endInDay,
        activeDays: Int = this.activeDays,
        interval: Long = this.interval,
        enabled: Boolean = this.enabled,
        isMock: Boolean = false,
    ): LoopBase
}

fun LoopBase.isTogether(loop: LoopBase) =
    loop.endInDay in (startInDay + 1)..endInDay ||
            loop.startInDay in startInDay..<endInDay

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

