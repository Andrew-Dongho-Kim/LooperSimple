package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo.Factory.ANY_TIME
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

@Dao
interface LoopDoneDao {

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId ORDER BY date DESC")
    fun getAllFlow(loopId: Int): Flow<List<LoopDoneVo>>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND :from <= date AND date <= :to ORDER BY date ASC")
    suspend fun getAllBetween(
        loopId: Int,
        from: Long,
        to: Long
    ): List<LoopDoneVo>

    /**
     * 전체 탭 하단 기록 그리드용: 모든 루프의 상태 기록.
     * 완료·건너뜀·비활성(DISABLED)을 각각 색으로 구분해 그리므로 이 셋을 모두 가져온다.
     * NO_RESPONSE(미응답)는 별도 행 없이 "기록 없음"으로 판단하므로 제외해 데이터를 가볍게 유지한다.
     */
    @Query(
        "SELECT * FROM loop_done " +
                "WHERE done == ${LoopDoneVo.DoneState.DONE} " +
                "OR done == ${LoopDoneVo.DoneState.SKIP} " +
                "OR done == ${LoopDoneVo.DoneState.DISABLED} " +
                "ORDER BY date ASC"
    )
    fun getAllHistoryFlow(): Flow<List<LoopDoneVo>>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED}")
    suspend fun getAllEnabled(
        loopId: Int,
    ): List<LoopDoneVo>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND date=:date LIMIT 1")
    suspend fun getDoneState(
        loopId: Int,
        date: Long
    ): LoopDoneVo?

    /**
     * anytime 루프가 실제로 시작된 날의 기록을 최신순으로. 습관 시작 시각 추정의 표본이 된다.
     *
     * startInDay 에 실제 시계 시각이 들어가는 것은 사용자가 "시작"을 누른 날뿐이다(진행 중이거나
     * 그 뒤 완료된 날). 미응답·건너뛰기·비활성 행은 ANY_TIME(-1) 이므로 startInDay >= 0 조건
     * 하나로 "안 한 날"이 자연히 걸러진다.
     *
     * 오늘 기록은 제외한다([today] 미만) — 오늘 알릴 시각을 오늘의 시작으로 추정할 수는 없다.
     */
    @Query(
        "SELECT * FROM loop_done " +
                "WHERE loopId=:loopId AND startInDay >= 0 AND :since <= date AND date < :today " +
                "ORDER BY date DESC LIMIT :limit"
    )
    suspend fun getRecentStarts(
        loopId: Int,
        since: Long,
        today: Long,
        limit: Int,
    ): List<LoopDoneVo>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done != ${LoopDoneVo.DoneState.DISABLED}")
    fun getAllEnabledCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done != ${LoopDoneVo.DoneState.DISABLED} AND date=:date")
    fun getEnabledCountByDateFlow(date: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED}")
    fun getAllEnabledCountFlow(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED} AND :from >= date")
    suspend fun getAllEnabledCountBefore(loopId: Int, from: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED} AND :from <= date AND date <= :to")
    suspend fun getAllEnabledCountBetween(loopId: Int, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE (done == ${LoopDoneVo.DoneState.DONE} OR done == ${LoopDoneVo.DoneState.SKIP})")
    fun getRespondCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE (done == ${LoopDoneVo.DoneState.DONE} OR done == ${LoopDoneVo.DoneState.SKIP}) AND date=:date")
    fun getRespondCountByDateFlow(date: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND (done == ${LoopDoneVo.DoneState.DONE} OR done == ${LoopDoneVo.DoneState.SKIP})")
    fun getRespondCountFlow(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE}")
    fun getDoneCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND date=:date")
    fun getDoneCountByDateFlow(date: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId")
    fun getDoneCountFlow(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId AND :from >= date")
    suspend fun getDoneCountBefore(loopId: Int, from: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId AND :from <= date AND date <= :to")
    suspend fun getDoneCountBetween(loopId: Int, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.SKIP}")
    fun getSkipCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.SKIP} AND date=:date")
    fun getSkipCountByDateFlow(date: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.SKIP} AND loopId=:loopId")
    fun getSkipCountFlow(loopId: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(doneVo: LoopDoneVo)

    @Query("DELETE FROM loop_done WHERE loopId=:loopId AND date=:date")
    suspend fun delete(loopId: Int, date: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIfAbsent(doneVo: LoopDoneVo)

    @Query("DELETE FROM loop_done WHERE done==${LoopDoneVo.DoneState.NO_RESPONSE}")
    suspend fun deleteNoResponseAll()

    suspend fun addOrUpdate(
        loop: LoopBase,
        localDate: LocalDate,
        @LoopDoneVo.DoneState doneState: Int
    ) =
        addOrUpdate(
            LoopDoneVo(
                loopId = loop.loopId,
                date = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                startInDay = if (loop.isAnyTime && doneState == LoopDoneVo.DoneState.NO_RESPONSE) {
                    ANY_TIME
                } else {
                    loop.startInDay
                },
                endInDay = if (loop.isAnyTime && doneState == LoopDoneVo.DoneState.NO_RESPONSE) {
                    ANY_TIME
                } else {
                    loop.endInDay
                },
                done = doneState
            )
        )
}