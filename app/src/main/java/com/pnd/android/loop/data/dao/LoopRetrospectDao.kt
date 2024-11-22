package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pnd.android.loop.data.LoopRetrospectVo

@Dao
interface LoopRetrospectDao {

    @Query("SELECT * FROM loop_memo WHERE loopId=:loopId AND date=:localDate")
    suspend fun getRetrospect(
        loopId: Int,
        localDate: Long
    ): LoopRetrospectVo?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveRetrospect(
        memoVo: LoopRetrospectVo,
    )
}