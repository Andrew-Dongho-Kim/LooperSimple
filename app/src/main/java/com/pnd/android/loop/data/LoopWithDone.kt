package com.pnd.android.loop.data

import androidx.room.Dao
import androidx.room.Query
import com.pnd.android.loop.alarm.NO_ALARMS
import kotlinx.coroutines.flow.Flow

data class LoopWithDone(
    override val id: Int = 0,
    override val color: Int,
    override val title: String,
    override val loopStart: Long,
    override val loopEnd: Long,
    override val loopActiveDays: Int,
    override val interval: Long,
    val alarms: Int = NO_ALARMS,
    override val enabled: Boolean,
    val date: Long = 0L,
    @LoopDoneVo.DoneState val done: Int = LoopDoneVo.DoneState.NO_RESPONSE,
) : LoopBase {
    override fun copy(
        id: Int,
        title: String,
        color: Int,
        loopStart: Long,
        loopEnd: Long,
        loopActiveDays: Int,
        interval: Long,
        enabled: Boolean
    ): LoopBase = LoopWithDone(
        id = id,
        title = title,
        color = color,
        loopStart = loopStart,
        loopEnd = loopEnd,
        loopActiveDays = loopActiveDays,
        interval = interval,
        enabled = enabled,
        date = this.date,
        done = this.done,
    )
}

@Dao
interface LoopWithDoneDao {

    @Query(
        "SELECT loop.id, loop.color, loop.title, loop.loopStart, loop.loopEnd, loop.loopActiveDays, loop.interval, loop.alarms, loop.enabled, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.id = loop_done.loopId AND loop_done.date =:date " +
                "ORDER BY loop.loopStart ASC, loop.loopEnd ASC"
    )
    fun allLoops(date: Long): Flow<List<LoopWithDone>>
}

val LoopBase.doneState get() = (this as? LoopWithDone)?.done