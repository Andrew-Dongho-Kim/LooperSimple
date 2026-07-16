package com.pnd.android.loop.data.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pnd.android.loop.data.LoopRetrospectVo
import kotlinx.coroutines.flow.Flow

@Dao
interface LoopRetrospectDao {

    @Query("SELECT * FROM loop_memo WHERE loopId=:loopId AND date=:localDate")
    suspend fun getRetrospect(
        loopId: Int,
        localDate: Long
    ): LoopRetrospectVo?

    /**
     * 한 루프의 회고 메모 전체를 관찰한다. 상세 화면 달력에서 "메모가 있는 날"에
     * 마커를 찍기 위해 사용하며, 메모가 추가/수정되면 달력이 즉시 갱신된다.
     */
    @Query("SELECT * FROM loop_memo WHERE loopId=:loopId")
    fun getRetrospectsFlow(loopId: Int): Flow<List<LoopRetrospectVo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(
        memoVo: LoopRetrospectVo,
    )
}