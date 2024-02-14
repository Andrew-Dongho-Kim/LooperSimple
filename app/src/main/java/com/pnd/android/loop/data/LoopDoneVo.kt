package com.pnd.android.loop.data

import androidx.annotation.IntDef
import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.DONE
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.NO_RESPONSE
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.SKIP
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.ZoneId

@Immutable
@Entity(
    tableName = "loop_done",
    primaryKeys = ["loopId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = LoopVo::class,
            parentColumns = ["id"],
            childColumns = ["loopId"]
        )
    ]
)
data class LoopDoneVo(
    val loopId: Int,
    val date: Long,
    val done: Int = NO_RESPONSE
) {
    fun isDone() = done == DONE

    fun isSkip() = done == SKIP

    @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
    @IntDef(DONE, SKIP, NO_RESPONSE)
    annotation class DoneState {
        companion object {
            const val DONE = 1
            const val SKIP = 2
            const val NO_RESPONSE = 0
        }
    }
}

@Dao
interface LoopDoneDao {

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND date=:date LIMIT 1")
    suspend fun doneState(
        loopId: Int,
        date: Long
    ): LoopDoneVo?

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND :from <= date AND date <= :to")
    suspend fun doneStates(
        loopId: Int,
        from: Long,
        to: Long
    ): List<LoopDoneVo>

    @Query("SELECT COUNT(*) FROM loop_done where done != $NO_RESPONSE")
    fun flowResponseCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done where done != $NO_RESPONSE AND loopId=:loopId")
    fun flowResponseCount(loopId:Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done where done == $DONE")
    fun flowDoneCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done where done == $DONE AND loopId=:loopId")
    fun flowDoneCount(loopId:Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done where done == $SKIP")
    fun flowSkipCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done where done == $SKIP AND loopId=:loopId")
    fun flowSkipCount(loopId: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(doneVo: LoopDoneVo)

    suspend fun addOrUpdate(
        loop: LoopBase,
        localDate: LocalDate,
        @DoneState doneState: Int
    ) =
        addOrUpdate(
            LoopDoneVo(
                loopId = loop.id,
                date = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                done = doneState
            )
        )
}