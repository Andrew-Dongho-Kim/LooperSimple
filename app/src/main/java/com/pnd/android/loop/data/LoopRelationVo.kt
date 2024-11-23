package com.pnd.android.loop.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "loop_relation",
    primaryKeys = ["loopGroupId", "loopId"],
    foreignKeys = [
        ForeignKey(
            entity = LoopGroupVo::class,
            parentColumns = ["loopGroupId"],
            childColumns = ["loopGroupId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = LoopVo::class,
            parentColumns = ["loopId"],
            childColumns = ["loopId"],
            onUpdate = ForeignKey.CASCADE,
            onDelete = ForeignKey.CASCADE,
        )
    ],
    indices = [Index(value = arrayOf("loopGroupId")), Index(value = arrayOf("loopId"))]
)
data class LoopRelationVo(
    val loopGroupId: Int,
    val loopId: Int,
)
