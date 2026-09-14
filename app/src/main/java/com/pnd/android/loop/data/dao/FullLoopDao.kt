package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import com.pnd.android.loop.data.history.LoopHistory
import com.pnd.android.loop.data.history.LoopHistorySnapshot

/** Operational readers also resolve today and yesterday from one committed DB snapshot. */
@Dao
abstract class FullLoopDao {
    @Transaction
    @Query("SELECT * FROM loop ORDER BY loopId")
    protected abstract suspend fun readHistories(): List<LoopHistory>

    suspend fun getSnapshot(): LoopHistorySnapshot = LoopHistorySnapshot(readHistories())
}
