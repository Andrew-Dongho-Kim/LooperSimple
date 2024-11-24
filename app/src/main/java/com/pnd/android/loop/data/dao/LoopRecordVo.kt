package com.pnd.android.loop.data.dao

import androidx.compose.runtime.Immutable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.pnd.android.loop.data.LoopVo

@Immutable
@Entity(
    tableName = "loop_record",
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
data class LoopRecordVo(
    @PrimaryKey(autoGenerate = true) val recordId: Int,
    val loopId: Int,
    val startInDay: Long,
    val endInDay: Long,
)