package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Query
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.data.LoopWithStatistics
import com.pnd.android.loop.data.MonthlyLoopDuration
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

@Dao
interface FullLoopDao {

    @Query(
        "SELECT loop.loopId, loop.color, loop.title, loop.created, loop.startInDay, loop.endInDay, loop.activeDays, loop.interval, loop.enabled, loop.isAnyTime, " +
                "loop_done.startInDay as actualStartInDay, loop_done.endInDay as actualEndInDay, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.loopId == loop_done.loopId AND loop_done.date ==:date " +
                "ORDER BY loop.enabled DESC, loop.endInDay ASC, loop.startInDay ASC, loop.title ASC"
    )
    fun getAllLoopsFlow(date: Long): Flow<List<LoopWithDone>>

    @Query(
        "SELECT loop.loopId, loop.color, loop.title, loop.created, loop.startInDay, loop.endInDay, loop.activeDays, loop.interval, loop.enabled, loop.isAnyTime, " +
                "loop_done.startInDay as actualStartInDay, loop_done.endInDay as actualEndInDay, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.loopId == loop_done.loopId AND loop_done.date ==:date " +
                "ORDER BY loop.enabled DESC, loop.endInDay ASC, loop.startInDay ASC, loop.title ASC"
    )
    suspend fun getAllLoops(date: Long = LocalDate.now().toMs()): List<LoopWithDone>

    @Query(
        "SELECT loop.loopId, loop.color, loop.title, loop.created, loop.startInDay, loop.endInDay, loop.activeDays, loop.interval, loop.enabled, loop.isAnyTime, " +
                "loop_done.startInDay as actualStartInDay, loop_done.endInDay as actualEndInDay, loop_done.date, loop_done.done " +
                "FROM loop LEFT JOIN loop_done " +
                "ON loop.loopId == loop_done.loopId AND loop_done.date ==:date WHERE loop.enabled == 1 " +
                "ORDER BY loop.endInDay ASC, loop.startInDay ASC, loop.title ASC"
    )
    suspend fun getAllEnabledLoops(date: Long): List<LoopWithDone>

    @Query(
        "SELECT loop_done.date as date, loop_done.loopId as loopId, title, color, text as retrospect " +
                "FROM loop_done " +
                "LEFT JOIN loop ON loop.loopId == loop_done.loopId " +
                "LEFT JOIN loop_memo ON loop_done.loopId == loop_memo.loopId AND loop_done.date == loop_memo.date " +
                "WHERE loop_done.done == ${LoopDoneVo.DoneState.DONE} AND :from <= loop_done.date AND loop_done.date <= :to " +
                "ORDER BY loop_done.date ASC, loopId ASC"
    )
    fun getDoneLoopsByDateFlow(
        from: Long,
        to: Long,
    ): Flow<List<LoopByDate>>


    @Query(
        "SELECT loop_done.date as date, loop_done.loopId as loopId, title, color, text as retrospect " +
                "FROM loop_done " +
                "LEFT JOIN loop ON loop.loopId == loop_done.loopId " +
                "LEFT JOIN loop_memo ON loop_done.loopId == loop_memo.loopId AND loop_done.date == loop_memo.date " +
                "WHERE  loop_done.done != ${LoopDoneVo.DoneState.DONE} AND loop_done.done != ${LoopDoneVo.DoneState.DISABLED} AND :from <= loop_done.date AND loop_done.date <= :to " +
                "ORDER BY loop_done.date ASC, loopId ASC"
    )
    fun getNoDoneLoopsByDateFlow(
        from: Long,
        to: Long,
    ): Flow<List<LoopByDate>>

    @Query(
        """SELECT loopId, title, color, doneCount /  CAST(allCount AS REAL) AS doneRate FROM 
                (SELECT *,
                    (SELECT COUNT(*) FROM loop_done WHERE loopId==loop.loopId AND :from <= date AND date <= :to) AS allCount, 
                    (SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId==loop.loopId AND :from <= date AND date <= :to) AS doneCount
                FROM loop WHERE created <= :to) 
            ORDER BY doneRate DESC
        """
    )
    fun getLoopsWithStatisticsFlow(from: Long, to: Long): Flow<List<LoopWithStatistics>>

    /**
     * 기간(:from..:to) 내에 완료(DONE)한 루프들에 투자한 시간(ms)의 총합.
     *
     * 한 번의 완료에 투자한 시간은 (endInDay - startInDay) 이며, 자정을 넘겨 끝나는
     * 루프(end < start)는 하루(86400000ms)를 더해 보정한다. 완료 기록이 없으면 0.
     */
    @Query(
        "SELECT COALESCE(SUM(" +
                "CASE WHEN loop_done.endInDay >= loop_done.startInDay " +
                "THEN loop_done.endInDay - loop_done.startInDay " +
                "ELSE loop_done.endInDay - loop_done.startInDay + 86400000 END" +
                "), 0) " +
                "FROM loop_done " +
                "WHERE loop_done.done == ${LoopDoneVo.DoneState.DONE} " +
                "AND :from <= loop_done.date AND loop_done.date <= :to"
    )
    fun getInvestedTimeFlow(from: Long, to: Long): Flow<Long>

    /**
     * 전체 기간에 대해, 완료(DONE)한 루프에 투자한 시간(ms)을 월(연/월)별로 집계한다.
     * date(에폭 ms)를 로컬 타임존 기준 연/월로 변환해 그룹화하며, 오래된 달부터 정렬한다.
     */
    @Query(
        "SELECT " +
                "CAST(strftime('%Y', loop_done.date / 1000, 'unixepoch', 'localtime') AS INTEGER) AS year, " +
                "CAST(strftime('%m', loop_done.date / 1000, 'unixepoch', 'localtime') AS INTEGER) AS month, " +
                "COALESCE(SUM(" +
                "CASE WHEN loop_done.endInDay >= loop_done.startInDay " +
                "THEN loop_done.endInDay - loop_done.startInDay " +
                "ELSE loop_done.endInDay - loop_done.startInDay + 86400000 END" +
                "), 0) AS durationMs " +
                "FROM loop_done " +
                "WHERE loop_done.done == ${LoopDoneVo.DoneState.DONE} " +
                "GROUP BY year, month ORDER BY year ASC, month ASC"
    )
    fun getMonthlyInvestedTimeFlow(): Flow<List<MonthlyLoopDuration>>

    /**
     * 완료(DONE)한 기록이 하나라도 있는 날짜(에폭 ms)를 중복 없이 오래된 순으로 반환한다.
     * 연속 달성 스트릭 계산에 사용한다.
     */
    @Query(
        "SELECT DISTINCT date FROM loop_done " +
                "WHERE done == ${LoopDoneVo.DoneState.DONE} ORDER BY date ASC"
    )
    fun getDoneDatesFlow(): Flow<List<Long>>
}