package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.pnd.android.loop.data.history.LoopHistory
import com.pnd.android.loop.data.history.LoopRevisionVo
import kotlinx.coroutines.flow.Flow

@Dao
interface LoopHistoryDao {
    @Transaction
    @Query("SELECT * FROM loop ORDER BY loopId")
    fun observeAll(): Flow<List<LoopHistory>>

    @Transaction
    @Query("SELECT * FROM loop WHERE loopId = :loopId")
    suspend fun get(loopId: Int): LoopHistory?

    @Transaction
    @Query("SELECT * FROM loop ORDER BY loopId")
    suspend fun getAll(): List<LoopHistory>

    @Insert
    suspend fun insertRevision(revision: LoopRevisionVo): Long

    @Insert
    suspend fun restoreRevisions(revisions: List<LoopRevisionVo>)
}
