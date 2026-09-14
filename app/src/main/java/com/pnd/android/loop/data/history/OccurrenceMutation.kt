package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.toMs
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

internal fun nextWeekStart(date: LocalDate): LocalDate = date.plusDays((8 - date.dayOfWeek.value).toLong())

internal fun shouldPreserveOccurrence(day: ResolvedLoopDay?, now: LocalDateTime): Boolean {
    if (day == null) return false
    if (day.response.done in listOf(DoneState.DONE, DoneState.SKIP, DoneState.IN_PROGRESS)) return true
    return day.scheduled && !day.loop.isAnyTime && day.loop.startInDay <= now.toLocalTime().toMs()
}

internal fun plannedRecord(loop: LoopVo, date: LocalDate, revisionId: Long?): LoopDoneVo = LoopDoneVo(
    loopId = loop.loopId, date = date.toMs(), localEpochDay = date.toEpochDay(), revisionId = revisionId,
    startInDay = loop.startInDay, endInDay = loop.endInDay,
    timeSource = if (loop.isAnyTime) LoopDoneVo.TimeSource.UNKNOWN else LoopDoneVo.TimeSource.PLANNED,
)

internal fun updatedResponse(
    existing: LoopDoneVo,
    plan: LoopVo,
    state: Int,
    suppliedTimes: Pair<Long, Long>?,
    clock: Clock,
): LoopDoneVo {
    val now = LocalDateTime.now(clock)
    val base = existing.copy(done = state, localEpochDay = existing.localDate().toEpochDay())
    if (state == DoneState.IN_PROGRESS) return base.copy(
        startInDay = now.toLocalTime().toMs(), endInDay = -1,
        startedAt = clock.millis(), endedAt = null, timeSource = LoopDoneVo.TimeSource.MEASURED,
    )
    if (state == DoneState.NO_RESPONSE && plan.isAnyTime) return base.copy(
        startInDay = -1, endInDay = -1, startedAt = null, endedAt = null,
        timeSource = LoopDoneVo.TimeSource.UNKNOWN,
    )
    if (state == DoneState.DONE && suppliedTimes != null) {
        val (start, end) = suppliedTimes
        require(start in 0 until 86_400_000L && end in 0 until 86_400_000L)
        return base.copy(startInDay = start, endInDay = end,
            startedAt = null, endedAt = null, timeSource = LoopDoneVo.TimeSource.USER_ENTERED)
    }
    if (state == DoneState.DONE && existing.done == DoneState.IN_PROGRESS) return base.copy(
        endInDay = now.toLocalTime().toMs(), endedAt = clock.millis(),
        timeSource = if (existing.startedAt != null) LoopDoneVo.TimeSource.MEASURED else existing.timeSource,
    )
    return base
}
