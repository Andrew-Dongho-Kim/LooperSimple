package com.pnd.android.loop.data

import androidx.annotation.IntDef
import androidx.compose.runtime.Immutable
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.DISABLED
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
            childColumns = ["loopId"],
            onUpdate = CASCADE,
            onDelete = CASCADE,
        )
    ]
)
data class LoopDoneVo(
    val loopId: Int,
    val date: Long,
    val done: Int = NO_RESPONSE
) {
    fun isDisabled() = done.isDisabled()
    fun isDone() = done.isDone()

    fun isSkip() = done.isSkip()

    fun isRespond() = done.isRespond()

    @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
    @IntDef(DISABLED, DONE, SKIP, NO_RESPONSE)
    annotation class DoneState {
        companion object {
            const val DISABLED = -1
            const val DONE = 1
            const val SKIP = 2
            const val NO_RESPONSE = 0
        }
    }
}

fun Int.isDisabled() = this == DISABLED
fun Int.isDone() = this == DONE
fun Int.isSkip() = this == SKIP
fun Int.isRespond() = this == DONE || this == SKIP


@Dao
interface LoopDoneDao {

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId ORDER BY date DESC")
    fun flowGetAll(loopId: Int): Flow<List<LoopDoneVo>>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND :from <= date AND date <= :to ORDER BY date ASC")
    suspend fun getAllBetween(
        loopId: Int,
        from: Long,
        to: Long
    ): List<LoopDoneVo>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND done != $DISABLED")
    suspend fun getAllEnabled(
        loopId: Int,
    ): List<LoopDoneVo>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND date=:date LIMIT 1")
    suspend fun getDoneState(
        loopId: Int,
        date: Long
    ): LoopDoneVo?

    @Query("SELECT COUNT(*) FROM loop_done WHERE done != $DISABLED")
    fun flowAllEnabledCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != $DISABLED")
    fun flowAllEnabledCount(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != $DISABLED AND :from >= date")
    suspend fun allEnabledCountBefore(loopId: Int, from: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND done != $DISABLED AND :from <= date AND date <= :to")
    suspend fun allEnabledCountBetween(loopId: Int, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE (done == $DONE OR done == $SKIP)")
    fun flowRespondCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE loopId=:loopId AND (done == $DONE OR done == $SKIP)")
    fun flowRespondCount(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == $DONE")
    fun flowDoneCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == $DONE AND loopId=:loopId")
    fun flowDoneCount(loopId: Int): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == $DONE AND loopId=:loopId AND :from >= date")
    suspend fun doneCountBefore(loopId: Int, from: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == $DONE AND loopId=:loopId AND :from <= date AND date <= :to")
    suspend fun doneCountBetween(loopId: Int, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == $SKIP")
    fun flowSkipCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM loop_done WHERE done == $SKIP AND loopId=:loopId")
    fun flowSkipCount(loopId: Int): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addOrUpdate(doneVo: LoopDoneVo)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addIfAbsent(doneVo: LoopDoneVo)

    @Query("DELETE FROM loop_done WHERE done==$NO_RESPONSE")
    suspend fun deleteNoResponseAll()

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