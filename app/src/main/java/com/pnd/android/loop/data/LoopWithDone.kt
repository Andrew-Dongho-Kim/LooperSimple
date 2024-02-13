package com.pnd.android.loop.data

import androidx.room.Dao
import androidx.room.Ignore
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class LoopWithDone @JvmOverloads constructor(
    override val id: Int,
    override val color: Int,
    override val title: String,
    override val created: Long,
    override val loopStart: Long,
    override val loopEnd: Long,
    override val loopActiveDays: Int,
    override val interval: Long,
    override val enabled: Boolean,
    val date: Long,
    @LoopDoneVo.DoneState val done: Int,
    @Ignore override val isMock: Boolean = false,
) : LoopBase {

    override fun copyAs(
        id: Int,
        title: String,
        color: Int,
        created: Long,
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
        created = created,
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

fun LoopBase.toLoopWithDone(
    doneVo: LoopDoneVo
) = LoopWithDone(
    id = id,
    title = title,
    color = color,
    created = created,
    loopStart = loopStart,
    loopEnd = loopEnd,
    loopActiveDays = loopActiveDays,
    interval = interval,
    enabled = enabled,
    date = doneVo.date,
    done = doneVo.done,
    isMock = isMock
)

@Dao
interface LoopWithDoneDao {

    @Query(
        "SELECT loop.id, loop.color, loop.title, loop.created, loop.loopStart, loop.loopEnd, loop.loopActiveDays, loop.interval, loop.enabled, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.id = loop_done.loopId AND loop_done.date =:date " +
                "ORDER BY loop.loopStart ASC, loop.loopEnd ASC, loop.title ASC"
    )
    fun flowAllLoops(date: Long): Flow<List<LoopWithDone>>

    @Query(
        "SELECT loop.id, loop.color, loop.title, loop.created, loop.loopStart, loop.loopEnd, loop.loopActiveDays, loop.interval, loop.enabled, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.id = loop_done.loopId AND loop_done.date =:date " +
                "ORDER BY loop.loopStart ASC, loop.loopEnd ASC, loop.title ASC"
    )
    suspend fun allLoops(date: Long): List<LoopWithDone>
}

val LoopBase.doneState get() = (this as? LoopWithDone)?.done

val LoopBase.isNotResponsed get() = doneState == LoopDoneVo.DoneState.NO_RESPONSE