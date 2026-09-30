package com.pnd.android.loop.data

import androidx.compose.runtime.Immutable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.ForeignKey.Companion.CASCADE
import androidx.room.Index
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.state.DoneState.Companion.NO_RESPONSE
import com.pnd.android.loop.state.isDisabled
import com.pnd.android.loop.state.isDone
import com.pnd.android.loop.state.isRespond
import com.pnd.android.loop.state.isSkip

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
    ],
    // loopId: speeds up FK cascades and the many WHERE loopId=... / JOIN lookups.
    // date: speeds up the frequent ":from <= date AND date <= :to" range queries.
    indices = [
        Index(value = ["loopId"]),
        Index(value = ["date"]),
    ]
)
data class LoopDoneVo(
    val loopId: Int,
    val date: Long,
    @ColumnInfo(defaultValue = "0")
    val startInDay: Long = 0L,
    @ColumnInfo(defaultValue = "0")
    val endInDay: Long = 0L,
    @DoneState val done: Int = NO_RESPONSE,
    val revisionId: Long? = null,
    val localEpochDay: Long? = null,
    @ColumnInfo(defaultValue = "0") val timeSource: Int = TimeSource.UNKNOWN,
    val startedAt: Long? = null,
    val endedAt: Long? = null
) {
    fun measuredDurationMs(): Long? = startedAt?.let { start ->
        endedAt?.let { end -> (end - start).takeIf { it >= 0 } }
    }

    object TimeSource {
        const val UNKNOWN = 0
        const val MEASURED = 1
        const val PLANNED = 2
        const val USER_ENTERED = 3
    }

    fun isDisabled() = done.isDisabled()
    fun isDone() = done.isDone()

    fun isSkip() = done.isSkip()

    fun isRespond() = done.isRespond()
}
