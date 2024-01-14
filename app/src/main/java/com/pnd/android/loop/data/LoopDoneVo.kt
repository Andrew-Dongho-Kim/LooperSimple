package com.pnd.android.loop.data

import androidx.annotation.IntDef
import androidx.compose.runtime.Immutable
import androidx.lifecycle.LiveData
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
    fun liveDataDoneState(
        loopId: Long,
        date: Long
    ): LiveData<LoopDoneVo>

    @Query("SELECT * FROM loop_done WHERE loopId=:loopId AND :from <= date AND date <= :to")
    suspend fun doneStates(
        loopId: Long,
        from: Long,
        to: Long
    ): List<LoopDoneVo>

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