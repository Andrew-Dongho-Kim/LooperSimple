package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.data.LoopWithStatistics
import kotlinx.coroutines.flow.Flow

@Dao
interface FullLoopDao {

    @Query(
        "SELECT loop.id, loop.color, loop.title, loop.created, loop.startInDay, loop.endInDay, loop.activeDays, loop.interval, loop.enabled, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.id == loop_done.loopId AND loop_done.date ==:date " +
                "ORDER BY loop.enabled DESC, loop.endInDay ASC, loop.startInDay ASC, loop.title ASC"
    )
    fun flowAllLoops(date: Long): Flow<List<LoopWithDone>>

    @Query(
        "SELECT loop.id, loop.color, loop.title, loop.created, loop.startInDay, loop.endInDay, loop.activeDays, loop.interval, loop.enabled, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.id == loop_done.loopId AND loop_done.date ==:date " +
                "ORDER BY loop.enabled DESC, loop.endInDay ASC, loop.startInDay ASC, loop.title ASC"
    )
    suspend fun allLoops(date: Long): List<LoopWithDone>

    @Query(
        "SELECT loop.id, loop.color, loop.title, loop.created, loop.startInDay, loop.endInDay, loop.activeDays, loop.interval, loop.enabled, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.id == loop_done.loopId AND loop_done.date ==:date WHERE loop.enabled == 1 " +
                "ORDER BY loop.endInDay ASC, loop.startInDay ASC, loop.title ASC"
    )
    suspend fun allEnabledLoops(date: Long): List<LoopWithDone>

    @Query(
        "SELECT loop_done.date as date, id, title, color, text as retrospect FROM loop_done LEFT JOIN loop ON loop.id == loop_done.loopId LEFT JOIN loop_memo ON loop_done.loopId == loop_memo.loopId AND loop_done.date == loop_memo.date " +
                "WHERE loop_done.done == ${LoopDoneVo.DoneState.DONE} AND " +
                ":from <= loop_done.date AND loop_done.date <= :to " +
                "ORDER BY loop_done.date ASC, loop.id ASC"
    )
    fun flowDoneLoopsByDate(
        from: Long,
        to: Long,
    ): Flow<List<LoopByDate>>


    @Query(
        "SELECT loop_done.date as date, id, title, color, text as retrospect FROM loop_done LEFT JOIN loop ON loop.id == loop_done.loopId LEFT JOIN loop_memo ON loop_done.loopId == loop_memo.loopId AND loop_done.date == loop_memo.date " +
                "WHERE  loop_done.done != ${LoopDoneVo.DoneState.DONE} AND " +
                "loop_done.done != ${LoopDoneVo.DoneState.DISABLED} AND " +
                ":from <= loop_done.date AND loop_done.date <= :to " +
                "ORDER BY loop_done.date ASC, loop.id ASC"
    )
    fun flowNoDoneLoopsByDate(
        from: Long,
        to: Long,
    ): Flow<List<LoopByDate>>

    @Query(
        """SELECT id, title, color, doneCount /  CAST(allCount AS REAL) AS doneRate FROM 
                (SELECT *,
                    (SELECT COUNT(*) FROM loop_done WHERE loopId==id AND :from <= date AND date <= :to) AS allCount, 
                    (SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId==id AND :from <= date AND date <= :to) AS doneCount
                FROM loop WHERE created <= :to) 
            ORDER BY doneRate DESC
        """
    )
    fun flowLoopsWithStatistics(from: Long, to: Long): Flow<List<LoopWithStatistics>>
}