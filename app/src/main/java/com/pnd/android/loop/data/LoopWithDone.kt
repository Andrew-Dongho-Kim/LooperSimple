package com.pnd.android.loop.data

import androidx.room.Dao
import androidx.room.Ignore
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class LoopWithDone @JvmOverloads constructor(
    override val id: Int,
    override val color: Int,
    override val title: String,
    override val loopStart: Long,
    override val loopEnd: Long,
    override val loopActiveDays: Int,
    override val interval: Long,
    override val enabled: Boolean,
    val date: Long,
    @LoopDoneVo.DoneState val done: Int,
    @Ignore override val isMock: Boolean = false,
) : LoopBase {

    override fun copy(
        id: Int,
        title: String,
        color: Int,
        loopStart: Long,
        loopEnd: Long,
        loopActiveDays: Int,
        interval: Long,
        enabled: Boolean,
        isMock: Boolean,
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
        isMock = isMock,
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