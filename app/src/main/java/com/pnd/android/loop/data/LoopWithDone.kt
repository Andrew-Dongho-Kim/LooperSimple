package com.pnd.android.loop.data

import androidx.room.Dao
import androidx.room.Query
import com.pnd.android.loop.alarm.NO_ALARMS
import kotlinx.coroutines.flow.Flow

data class LoopWithDone(
    override val id: Int = 0,
    val color: Int = LoopVo.DEFAULT_COLOR,
    val title: String = "",
    override val loopStart: Long = 0L,
    override val loopEnd: Long = 0L,
    override val loopActiveDays: Int = LoopVo.Day.EVERYDAY,
    val interval: Long = LoopVo.NO_REPEAT,
    val alarms: Int = NO_ALARMS,
    override val enabled: Boolean = true,
    val date: Long = 0L,
    @LoopDoneVo.DoneState val done: Int = LoopDoneVo.DoneState.NO_RESPONSE,
) : LoopBase {
    fun copyAsLoop(
        id: Int = this.id,
        color: Int = this.color,
        title: String = this.title,
        loopStart: Long = this.loopStart,
        loopEnd: Long = this.loopEnd,
        loopActiveDays: Int = this.loopActiveDays,
        interval: Long = this.interval,
        alarms: Int = this.alarms,
        enabled: Boolean = this.enabled
    ) = LoopVo(
        id = id,
        color = color,
        title = title,
        loopStart = loopStart,
        loopEnd = loopEnd,
        loopActiveDays = loopActiveDays,
        interval = interval,
        alarms = alarms,
        enabled = enabled
    )
}

@Dao
interface LoopWithDoneDao {

    @Query(
        "SELECT loop.id, loop.color, loop.title, loop.loopStart, loop.loopEnd, loop.loopActiveDays, loop.interval, loop.alarms, loop.enabled, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.id = loop_done.loopId AND loop_done.date =:date"
    )
    fun allLoops(date: Long): Flow<List<LoopWithDone>>
}