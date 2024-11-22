package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

@Dao
interface LoopDoneDao {

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId ORDER BY date DESC")
    fun flowGetAll(loopId: Int): Flow<List<LoopDoneVo>>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND :from <= date AND date <= :to ORDER BY date ASC")
    suspend fun getAllBetween(
        loopId: Int,
        from: Long,
        to: Long
    ): List<LoopDoneVo>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED}")
    suspend fun getAllEnabled(
        loopId: Int,
    ): List<LoopDoneVo>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND date=:date LIMIT 1")
    suspend fun getDoneState(
        loopId: Int,
        date: Long
    ): LoopDoneVo?

    @Query("SELECT COUNT(*) FROM loop_done WHERE done != ${LoopDoneVo.DoneState.DISABLED}")
    fun flowAllEnabledCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED}")
    fun flowAllEnabledCount(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED} AND :from >= date")
    suspend fun allEnabledCountBefore(loopId: Int, from: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED} AND :from <= date AND date <= :to")
    suspend fun allEnabledCountBetween(loopId: Int, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE (done == ${LoopDoneVo.DoneState.DONE} OR done == ${LoopDoneVo.DoneState.SKIP})")
    fun flowRespondCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND (done == ${LoopDoneVo.DoneState.DONE} OR done == ${LoopDoneVo.DoneState.SKIP})")
    fun flowRespondCount(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE}")
    fun flowDoneCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId")
    fun flowDoneCount(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId AND :from >= date")
    suspend fun doneCountBefore(loopId: Int, from: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId AND :from <= date AND date <= :to")
    suspend fun doneCountBetween(loopId: Int, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.SKIP}")
    fun flowSkipCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.SKIP} AND loopId=:loopId")
    fun flowSkipCount(loopId: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(doneVo: LoopDoneVo)

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
                loopId = loop.id,
                date = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                done = doneState
            )
        )
}