package com.pnd.android.loop.data

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "loop_relation",
    primaryKeys = ["loopGroupId", "loopId"],
    foreignKeys = [
        ForeignKey(
            entity = LoopGroupVo::class,
            parentColumns = ["id"],
            childColumns = ["loopGroupId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LoopVo::class,
            parentColumns = ["id"],
            childColumns = ["loopId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        )
    ]

)
data class LoopRelationVo(
    val loopGroupId: Int,
    val loopId: Int,
)
