package com.pnd.android.loop.data

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import javax.annotation.concurrent.Immutable

@Immutable
@Entity(tableName = "loop_group")
data class LoopGroupVo(
    @PrimaryKey(autoGenerate = true) val loopGroupId: Int,
    val groupTitle: String,
)

data class LoopGroupWithLoops(
    @Embedded val group: LoopGroupVo?,
    @Relation(
        parentColumn = "loopGroupId",
        entityColumn = "loopId",
        associateBy = Junction(LoopRelationVo::class)
    )
    val loops: List<LoopVo>
)