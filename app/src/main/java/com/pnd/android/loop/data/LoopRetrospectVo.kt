package com.pnd.android.loop.data

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey

@Immutable
@Entity(
    tableName = "loop_memo",
    primaryKeys = ["loopId", "date"],
    foreignKeys = [
        ForeignKey(
            entity = LoopVo::class,
            parentColumns = ["loopId"],
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

