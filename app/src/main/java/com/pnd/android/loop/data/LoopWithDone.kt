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
    val actualStartInDay: Long,
    val actualEndInDay: Long,
    val date: Long,
    @DoneState val done: Int,
    override val isAnyTime: Boolean,
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
        actualStartInDay = this.actualStartInDay,
        actualEndInDay = this.actualEndInDay,
        date = this.date,
        done = this.done,
        isAnyTime = isAnyTime,
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
    actualStartInDay = doneVo.startInDay,
    actualEndInDay = doneVo.endInDay,
    date = doneVo.date,
    done = doneVo.done,
    isAnyTime = isAnyTime,
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
    // 기간 내 완료(DONE)한 횟수와 그 완료들에 투자한 시간(ms) 총합.
    // 순위 정렬 기준(완료율/누적시간/완료횟수)에 사용한다.
    val doneCount: Int,
    val investedTimeMs: Long,
)

/**
 * 월(연/월) 단위로 집계한, 완료한 루프에 투자한 시간 합계.
 *
 * @param year 연도 (예: 2026)
 * @param month 월 (1~12)
 * @param durationMs 해당 월에 완료한 루프들의 소요 시간(ms) 총합
 */
data class MonthlyLoopDuration(
    val year: Int,
    val month: Int,
    val durationMs: Long,
)


