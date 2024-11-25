package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.pnd.android.loop.data.LoopGroupVo
import com.pnd.android.loop.data.LoopGroupWithLoops
import com.pnd.android.loop.data.LoopRelationVo
import kotlinx.coroutines.flow.Flow

@Dao
interface LoopGroupDao {

    @Query("SELECT * FROM loop_group")
    fun getAllGroupsFlow(): Flow<List<LoopGroupVo>>

    @Transaction
    @Query("SELECT * FROM loop_group")
    fun getAllGroupsWithLoopsFlow(): Flow<List<LoopGroupWithLoops>>

    @Transaction
    @Query("SELECT * FROM loop_relation WHERE loopGroupId=:groupId")
    fun getGroupWithLoopsFlow(groupId: Int): Flow<List<LoopGroupWithLoops>>

    @Query("SELECT * FROM loop_relation WHERE loopGroupId=:loopGroupId AND loopId=:loopId")
    fun getRelationFlow(
        loopGroupId: Int,
        loopId: Int
    ): Flow<LoopRelationVo?>

    @Insert
    suspend fun insert(vararg relations: LoopRelationVo): List<Long>

    @Insert
    suspend fun insert(vararg groups: LoopGroupVo): List<Long>

    @Update
    suspend fun update(vararg loops: LoopGroupVo)

    @Query("DELETE FROM loop_group WHERE loopGroupId=:loopGroupId")
    suspend fun delete(loopGroupId: Int)

    @Query("DELETE FROM loop_relation WHERE loopGroupId=:loopGroupId AND loopId=:loopId")
    suspend fun removeFromGroup(
        loopGroupId: Int,
        loopId: Int
    )
}