package com.pnd.android.loop.data.dao

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.isTogether
import kotlinx.coroutines.flow.Flow

@Dao
interface LoopDao {
    @Query("SELECT * FROM loop ORDER BY startInDay ASC, endInDay ASC")
    fun allLoopsLiveData(): LiveData<List<LoopVo>>

    @Query("SELECT * FROM loop WHERE id=:loopId")
    suspend fun loop(loopId: Int): LoopVo

    @Query("SELECT * FROM loop WHERE id=:loopId")
    fun flowLoop(loopId: Int): Flow<LoopVo>

    @Query("SELECT min(created) FROM loop")
    fun flowMinCreatedTime(): Flow<Long>

    @Query("SELECT * FROM loop")
    suspend fun allLoops(): List<LoopVo>

    @Insert
    suspend fun insert(vararg loops: LoopVo): List<Long>

    @Update
    suspend fun update(vararg loops: LoopVo)

    suspend fun addOrUpdate(vararg loops: LoopVo) =
        try {
            insert(*loops).map { it.toInt() }
        } catch (e: SQLiteConstraintException) {
            update(*loops)
            loops.map { loop -> loop.id }
        }

    @Query("DELETE FROM loop WHERE id = :id")
    suspend fun remove(id: Int)

    suspend fun maxOfIntersects(loopToCompare: LoopBase) =
        LoopDay.ALL.filter { day -> loopToCompare.activeDays.isOn(day) }
            .map { day ->
                allLoops()
                    .filter { loop -> loop.activeDays.isOn(day) }
                    .filter { loop -> loop.isTogether(loopToCompare) }
            }
            .maxOfOrNull { loops -> loops.size }
            ?: 0

}