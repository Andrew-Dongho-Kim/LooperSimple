package com.pnd.android.loop.data

import androidx.room.Ignore

data class FullLoopVo @JvmOverloads constructor(
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
    val retrospect: String,
    @LoopDoneVo.DoneState val done: Int,
    override val isAnyTime: Boolean = false,
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
        isAnyTime: Boolean,
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
        isAnyTime = isAnyTime,
        isMock = isMock,
    )
}

fun LoopBase.toFullLoopVo(
    retrospectVo: LoopRetrospectVo?,
    doneVo: LoopDoneVo
) = FullLoopVo(
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
    retrospect = retrospectVo?.text ?: "",
    done = doneVo.done,
    isMock = isMock
)