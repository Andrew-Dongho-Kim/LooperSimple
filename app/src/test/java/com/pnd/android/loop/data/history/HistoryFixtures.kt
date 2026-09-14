package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.*
import com.pnd.android.loop.util.toMs
import java.time.LocalDate

internal val firstDay: LocalDate = LocalDate.of(2026, 9, 1)
internal fun testLoop() = LoopVo.create(
    id = 1, title = "Before", color = 10, created = firstDay.toMs(),
    startInDay = 9 * 3_600_000L, endInDay = 10 * 3_600_000L,
    activeDays = LoopDay.EVERYDAY, enabled = true, weeklyGoal = 5, isMock = false,
)
internal fun revision(id: Long, loop: LoopVo, from: LocalDate = firstDay,
    goalFrom: LocalDate = from, knownFrom: LocalDate = firstDay) = LoopRevisionVo(
    revisionId = id, loopId = loop.loopId, recordedAt = from.toMs(),
    effectiveFrom = from.toEpochDay(), goalEffectiveFrom = goalFrom.toEpochDay(),
    knownFrom = knownFrom.toEpochDay(), settings = LoopSettings.from(loop),
)
internal fun response(date: LocalDate, state: Int = LoopDoneVo.DoneState.DONE, revisionId: Long? = 1) =
    LoopDoneVo(1, date.toMs(), 9 * 3_600_000L, 10 * 3_600_000L, state,
        revisionId = revisionId, localEpochDay = date.toEpochDay())
internal fun timeline(loop: LoopVo = testLoop(), revisions: List<LoopRevisionVo> = listOf(revision(1, loop)),
    responses: List<LoopDoneVo> = emptyList(), notes: List<LoopRetrospectVo> = emptyList()) =
    LoopTimeline(LoopHistory(loop, revisions, responses, notes))
