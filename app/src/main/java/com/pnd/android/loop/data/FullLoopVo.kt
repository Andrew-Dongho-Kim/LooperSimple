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
): FullLoopVo {
    // 시간이 정해진 루프는 정의된 스케줄을 그대로 쓴다. 반면 'anytime' 루프는 고정된
    // 시간이 없어, 그날 실제로 시작/종료한 구간이 done 기록에 남는다(건너뛰거나 시작 전이면
    // ANY_TIME). 그래서 anytime 루프의 시간은 done 기록에서 가져온다.
    val effectiveStart = if (isAnyTime) doneVo.startInDay else startInDay
    val effectiveEnd = if (isAnyTime) doneVo.endInDay else endInDay

    return FullLoopVo(
        loopId = loopId,
        title = title,
        color = color,
        created = created,
        startInDay = effectiveStart,
        endInDay = effectiveEnd,
        activeDays = activeDays,
        interval = interval,
        enabled = enabled,
        date = doneVo.date,
        retrospect = retrospectVo?.text ?: "",
        done = doneVo.done,
        // 시작/종료 중 하나라도 값이 없으면(ANY_TIME) 고정 시간이 없는 것으로 본다.
        isAnyTime = effectiveStart < 0 || effectiveEnd < 0,
        isMock = isMock,
    )
}