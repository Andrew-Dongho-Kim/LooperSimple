package com.pnd.android.loop.data.history

import androidx.room.Embedded
import androidx.room.Relation
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.LoopVo

/** Room reads all four relations in one transaction, so observers never mix revisions. */
data class LoopHistory(
    @Embedded val loop: LoopVo,
    @Relation(parentColumn = "loopId", entityColumn = "loopId")
    val revisions: List<LoopRevisionVo>,
    @Relation(parentColumn = "loopId", entityColumn = "loopId")
    val responses: List<LoopDoneVo>,
    @Relation(parentColumn = "loopId", entityColumn = "loopId")
    val notes: List<LoopRetrospectVo>,
)
