package com.pnd.android.loop.data

import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query


@Immutable
@Entity(
    tableName = "loop_memo",
    primaryKeys = ["loopId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = LoopVo::class,
            parentColumns = ["id"],
            childColumns = ["loopId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        )
    ]
)
data class LoopRetrospectVo(
    val loopId: Int,
    val date: Long,
    val text: String?,
)

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