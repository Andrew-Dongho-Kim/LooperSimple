package com.pnd.android.loop.data.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
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

    @Query("SELECT * FROM loop WHERE loopId=:loopId")
    suspend fun getLoop(loopId: Int): LoopVo

    @Query("SELECT * FROM loop WHERE loopId=:loopId")
    fun getLoopFlow(loopId: Int): Flow<LoopVo>

    @Query("SELECT min(created) FROM loop")
    fun getMinCreatedTimeFlow(): Flow<Long>


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

    suspend fun numberOfLoopsAtTheSameTime(loop: LoopBase) =
        LoopDay.ALL.filter { day -> loop.activeDays.isOn(day) }
            .map { day ->
                getAllLoops()
                    .filter { loop -> loop.activeDays.isOn(day) }
                    .filter { loop -> loop.isTogether(loop) }
            }
            .maxOfOrNull { loops -> loops.size }
            ?: 0

}