package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pnd.android.loop.data.LoopDoneVo
import kotlinx.coroutines.flow.Flow

/** Raw persistence only. Resolve schedules and statistics through LoopTimeline. */
@Dao
interface LoopDoneDao {
    @Query("SELECT * FROM loop_done WHERE loopId=:loopId ORDER BY date DESC")
    fun getAllFlow(loopId: Int): Flow<List<LoopDoneVo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(doneVo: LoopDoneVo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIfAbsent(doneVo: LoopDoneVo)

    @Query("DELETE FROM loop_done WHERE loopId=:loopId AND date=:date")
    suspend fun delete(loopId: Int, date: Long)
}
