package com.pnd.android.loop.data.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay.Companion.all
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.isTogether
import kotlinx.coroutines.flow.Flow

@Dao
interface LoopDao {

    @Query("SELECT * FROM loop ORDER BY startInDay ASC, endInDay ASC")
    suspend fun getAllLoops(): List<LoopVo>

    @Query("SELECT * FROM loop ORDER BY startInDay ASC, endInDay ASC")
    fun getAllLoopsLiveData(): LiveData<List<LoopVo>>

    // Nullable: the loop may have been deleted (e.g. a stale widget button or a
    // detail/paging screen observing a removed loop).
    @Query("SELECT * FROM loop WHERE loopId=:loopId")
    suspend fun getLoop(loopId: Int): LoopVo?

    @Query("SELECT * FROM loop WHERE loopId=:loopId")
    fun getLoopFlow(loopId: Int): Flow<LoopVo>

    // Nullable: min() returns NULL when the loop table is empty.
    @Query("SELECT min(created) FROM loop")
    fun getMinCreatedTimeFlow(): Flow<Long?>


    @Insert
    suspend fun insert(vararg loops: LoopVo): List<Long>

    @Update
    suspend fun update(vararg loops: LoopVo)

    suspend fun addOrUpdate(vararg loops: LoopVo) =
        try {
            insert(*loops).map { it.toInt() }
        } catch (e: SQLiteConstraintException) {
            update(*loops)
            loops.map { loop -> loop.loopId }
        }

    @Query("DELETE FROM loop WHERE loopId = :id")
    suspend fun delete(id: Int)

    suspend fun numberOfLoopsAtTheSameTime(another: LoopBase): Int {
        // Query the DB once and reuse the result for every active day. Previously
        // getAllLoops() ran once per active day (up to 7 queries) for a single check.
        val allLoops = getAllLoops()
        val loopsTogether = allLoops.filter { loop -> loop.isTogether(another) }
        return another.activeDays
            .all()
            .maxOfOrNull { day ->
                loopsTogether.count { loop -> loop.activeDays.isOn(day) }
            }
            ?: 0
    }

}