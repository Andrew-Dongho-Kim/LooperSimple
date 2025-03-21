package com.pnd.android.loop.data

import androidx.annotation.IntDef
import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.DISABLED
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.DONE
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.IN_PROGRESS
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.NO_RESPONSE
import com.pnd.android.loop.data.LoopDoneVo.DoneState.Companion.SKIP

@Immutable
@Entity(
    tableName = "loop_done",
    primaryKeys = ["loopId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = LoopVo::class,
            parentColumns = ["loopId"],
            childColumns = ["loopId"],
            onUpdate = CASCADE,
            onDelete = CASCADE,
        )
    ]
)
data class LoopDoneVo(
    val loopId: Int,
    val date: Long,
    @ColumnInfo(defaultValue = "0")
    val startInDay: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val endInDay: Long = 0L,
    val done: Int = NO_RESPONSE
) {
    fun isDisabled() = done.isDisabled()
    fun isDone() = done.isDone()

    fun isSkip() = done.isSkip()

    fun isRespond() = done.isRespond()

    @Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION)
    @IntDef(DISABLED, IN_PROGRESS, DONE, SKIP, NO_RESPONSE)
    annotation class DoneState {
        companion object {
            const val DISABLED = -1
            const val IN_PROGRESS = 3
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


