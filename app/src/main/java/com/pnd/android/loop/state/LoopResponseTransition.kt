package com.pnd.android.loop.state

import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.TimeSource
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.history.localDate
import com.pnd.android.loop.util.toMs
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime

private const val MILLIS_PER_DAY = 86_400_000L
private const val NO_TIME = -1L

/** Creates a response with the identity and planned times of its original occurrence. */
internal fun plannedRecord(loop: LoopVo, date: LocalDate, revisionId: Long?): LoopDoneVo = LoopDoneVo(
    loopId = loop.loopId,
    date = date.toMs(),
    localEpochDay = date.toEpochDay(),
    revisionId = revisionId,
    startInDay = loop.startInDay,
    endInDay = loop.endInDay,
    timeSource = if (loop.isAnyTime) TimeSource.UNKNOWN else TimeSource.PLANNED,
)

/**
 * Pure response transition. Database transactions and scheduling stay with the caller.
 * State-only edits keep recorded times, except starting/resetting a timer or ending a running one.
 */
internal fun updatedResponse(
    existing: LoopDoneVo,
    plan: LoopVo,
    @DoneState state: Int,
    suppliedTimes: Pair<Long, Long>?,
    clock: Clock,
): LoopDoneVo {
    require(state.isWritableResponse()) { "Invalid response state: $state" }
    val base = existing.copy(done = state, localEpochDay = existing.localDate().toEpochDay())
    return when {
        state.isInProgress() -> base.startRecording(clock)
        state.isNoResponse() && plan.isAnyTime -> base.clearRecording()
        state.isDone() && suppliedTimes != null -> base.withEnteredTimes(suppliedTimes)
        state.isRespond() && existing.done.isInProgress() -> base.finishRecording(clock)
        else -> base
    }
}

private fun LoopDoneVo.startRecording(clock: Clock): LoopDoneVo = copy(
    startInDay = LocalDateTime.now(clock).toLocalTime().toMs(),
    endInDay = NO_TIME,
    startedAt = clock.millis(),
    endedAt = null,
    timeSource = TimeSource.MEASURED,
)

private fun LoopDoneVo.clearRecording(): LoopDoneVo = copy(
    startInDay = NO_TIME,
    endInDay = NO_TIME,
    startedAt = null,
    endedAt = null,
    timeSource = TimeSource.UNKNOWN,
)

private fun LoopDoneVo.withEnteredTimes(times: Pair<Long, Long>): LoopDoneVo {
    val (start, end) = times
    require(start in 0 until MILLIS_PER_DAY && end in 0 until MILLIS_PER_DAY)
    return copy(
        startInDay = start,
        endInDay = end,
        startedAt = null,
        endedAt = null,
        timeSource = TimeSource.USER_ENTERED,
    )
}

/** Skipping also ends the timer; a later correction to DONE can reuse its actual duration. */
private fun LoopDoneVo.finishRecording(clock: Clock): LoopDoneVo = copy(
    endInDay = LocalDateTime.now(clock).toLocalTime().toMs(),
    endedAt = clock.millis(),
    timeSource = if (startedAt != null) TimeSource.MEASURED else timeSource,
)
