package com.pnd.android.loop.data

import androidx.room.Dao
import androidx.room.Ignore
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

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

data class LoopsByDate(
    val date: LocalDate,
    val id: Int,
    val title: String,
    val color: Int
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

    @Query(
        "SELECT date, id, title, color FROM loop_done LEFT JOIN loop ON loop.id == loop_done.loopId " +
                "WHERE loop_done.done == ${LoopDoneVo.DoneState.DONE} AND " +
                ":from <= loop_done.date AND loop_done.date <= :to " +
                "ORDER BY loop_done.date ASC, loop.id ASC"
    )
    fun flowDoneLoopsByDate(
        from: Long,
        to: Long,
    ): Flow<List<LoopsByDate>>
}

val LoopBase.doneState get() = (this as? LoopWithDone)?.done

val LoopBase.isRespond get() = doneState != LoopDoneVo.DoneState.NO_RESPONSE
val LoopBase.isNotRespond get() = doneState == LoopDoneVo.DoneState.NO_RESPONSE