package com.pnd.android.loop.data.history

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pnd.android.loop.data.LoopVo

/** Append-only settings. Epoch days are local calendar dates, not midnight instants. */
@Entity(
    tableName = "loop_revision",
    foreignKeys = [ForeignKey(
        entity = LoopVo::class, parentColumns = ["loopId"], childColumns = ["loopId"],
        onDelete = ForeignKey.CASCADE,
    )],
    indices = [Index(value = ["loopId", "effectiveFrom", "revisionId"])],
)
data class LoopRevisionVo(
    @PrimaryKey(autoGenerate = true) val revisionId: Long = 0,
    val loopId: Int,
    val recordedAt: Long,
    val effectiveFrom: Long,
    val goalEffectiveFrom: Long,
    /** Dates before this boundary use a frozen, estimated legacy schedule. */
    val knownFrom: Long,
    @Embedded val settings: LoopSettings,
)
