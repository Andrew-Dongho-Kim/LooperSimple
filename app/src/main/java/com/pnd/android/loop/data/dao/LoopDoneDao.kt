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
    fun getAllFlow(loopId: Int): Flow<List<LoopDoneVo>>

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
    fun getAllEnabledCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED}")
    fun getAllEnabledCountFlow(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED} AND :from >= date")
    suspend fun getAllEnabledCountBefore(loopId: Int, from: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != ${LoopDoneVo.DoneState.DISABLED} AND :from <= date AND date <= :to")
    suspend fun getAllEnabledCountBetween(loopId: Int, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE (done == ${LoopDoneVo.DoneState.DONE} OR done == ${LoopDoneVo.DoneState.SKIP})")
    fun getRespondCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND (done == ${LoopDoneVo.DoneState.DONE} OR done == ${LoopDoneVo.DoneState.SKIP})")
    fun getRespondCountFlow(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE}")
    fun getDoneCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId")
    fun getDoneCountFlow(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId AND :from >= date")
    suspend fun getDoneCountBefore(loopId: Int, from: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.DONE} AND loopId=:loopId AND :from <= date AND date <= :to")
    suspend fun getDoneCountBetween(loopId: Int, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.SKIP}")
    fun getSkipCountFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == ${LoopDoneVo.DoneState.SKIP} AND loopId=:loopId")
    fun getSkipCountFlow(loopId: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(doneVo: LoopDoneVo)

    @Query("DELETE FROM loop_done WHERE loopId=:loopId AND date=:date")
    suspend fun delete(loopId: Int, date:Long)

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
                done = doneState
            )
        )
}