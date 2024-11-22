package com.pnd.android.loop.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import javax.annotation.concurrent.Immutable

@Immutable
@Entity(tableName = "loop_group")
data class LoopGroupVo(
    @PrimaryKey(autoGenerate = true) val id: Int,
    val title: String,
)