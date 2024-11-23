package com.pnd.android.loop.data

import androidx.room.Ignore
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import java.time.LocalDate

data class LoopWithDone @JvmOverloads constructor(
    override val loopId: Int,
    override val color: Int,
    override val title: String,
    override val created: Long,
    override val startInDay: Long,
    override val endInDay: Long,
    override val activeDays: Int,
    override val interval: Long,
    override val enabled: Boolean,
    val date: Long,
    @DoneState val done: Int,
    @Ignore override val isMock: Boolean = false,
) : LoopBase {
    override fun copyAs(
        loopId: Int,
        title: String,
        color: Int,
        created: Long,
        startInDay: Long,
        endInDay: Long,
        activeDays: Int,
        interval: Long,
        enabled: Boolean,
        isMock: Boolean,
    ): LoopBase = LoopWithDone(
        loopId = loopId,
        title = title,
        color = color,
        created = created,
        startInDay = startInDay,
        endInDay = endInDay,
        activeDays = activeDays,
        interval = interval,
        enabled = enabled,
        date = this.date,
        done = this.done,
        isMock = isMock,
    )
}

fun LoopBase.toLoopWithDone(
    doneVo: LoopDoneVo
) = LoopWithDone(
    loopId = loopId,
    title = title,
    color = color,
    created = created,
    startInDay = startInDay,
    endInDay = endInDay,
    activeDays = activeDays,
    interval = interval,
    enabled = enabled,
    date = doneVo.date,
    done = doneVo.done,
    isMock = isMock
)

data class LoopByDate(
    val date: LocalDate,
    val loopId: Int,
    val title: String,
    val color: Int,
    val retrospect: String?,
)

data class LoopWithStatistics(
    val loopId: Int,
    val title: String,
    val color: Int,
    val doneRate: Float,
)


