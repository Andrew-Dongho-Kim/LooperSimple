package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.*
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.LocalDate

/** An indexed, immutable history. Construct once per DB emission, reuse across statistics. */
class LoopTimeline(val history: LoopHistory) {
    val current: LoopVo get() = history.loop
    val createdDate: LocalDate = history.revisions.minOfOrNull { it.effectiveFrom }
        ?.let(LocalDate::ofEpochDay) ?: current.created.toLocalDate()
    private val revisions = history.revisions.sortedWith(
        compareBy<LoopRevisionVo> { it.effectiveFrom }.thenBy { it.revisionId },
    )
    private val byRevision = revisions.associateBy { it.revisionId }
    private val responses = history.responses.associateBy { it.localDate() }
    private val notes = history.notes.associateBy { it.localDate() }

    fun revisionOn(date: LocalDate): LoopRevisionVo? {
        val epoch = date.toEpochDay()
        // upper bound: multiple edits on one date are ordered by the durable revision ID.
        var low = 0
        var high = revisions.size
        while (low < high) {
            val middle = (low + high) / 2
            if (revisions[middle].effectiveFrom <= epoch) low = middle + 1 else high = middle
        }
        return revisions.getOrNull(low - 1)
    }

    fun settingsOn(date: LocalDate): LoopVo =
        revisionOn(date)?.settings?.applyTo(current) ?: current

    fun responseOn(date: LocalDate): LoopDoneVo? = responses[date]

    fun goalOn(date: LocalDate): Int = revisions
        .filter { it.goalEffectiveFrom <= date.toEpochDay() }
        .maxWithOrNull(compareBy<LoopRevisionVo> { it.goalEffectiveFrom }.thenBy { it.revisionId })
        ?.settings?.weeklyGoal ?: 0

    fun day(date: LocalDate): ResolvedLoopDay? {
        if (date < createdDate) return null
        val saved = responseOn(date)
        if (saved?.done == DoneState.DISABLED) return null
        val revision = saved?.revisionId?.let(byRevision::get) ?: revisionOn(date)
        val settings = revision?.settings?.applyTo(current) ?: current
        val scheduled = settings.enabled && (settings.activeDays and dayForLoop(date)) != 0
        if (saved == null && !scheduled && notes[date]?.text.isNullOrBlank()) return null
        val response = saved ?: LoopDoneVo(
            loopId = current.loopId, date = date.toMs(), localEpochDay = date.toEpochDay(),
            startInDay = -1, endInDay = -1,
        )
        return ResolvedLoopDay(
            date = date, loop = settings, response = response,
            note = notes[date]?.text.orEmpty(),
            estimated = revision == null || date.toEpochDay() < revision.knownFrom,
            scheduled = scheduled,
            hasOccurrence = saved != null || scheduled,
        )
    }

    fun days(from: LocalDate, to: LocalDate): List<ResolvedLoopDay> = buildList {
        var date = maxOf(from, createdDate)
        while (date <= to) {
            day(date)?.let(::add)
            date = date.plusDays(1)
        }
    }

    /** Current labels, but the plan/state of the requested occurrence. */
    fun liveLoop(date: LocalDate): LoopWithDone {
        val day = day(date)
        val plan = day?.loop ?: settingsOn(date)
        val response = responseOn(date) ?: LoopDoneVo(
            current.loopId, date.toMs(), -1, -1,
        )
        return plan.copy(
            title = current.title, color = current.color, enabled = current.enabled,
            created = createdDate.toMs(),
        ).toLoopWithDone(response.copy(date = date.toMs()))
    }
}

fun LoopDoneVo.localDate(): LocalDate = localEpochDay?.let(LocalDate::ofEpochDay) ?: date.toLocalDate()
fun LoopRetrospectVo.localDate(): LocalDate = localEpochDay?.let(LocalDate::ofEpochDay) ?: date.toLocalDate()

data class ResolvedLoopDay(
    val date: LocalDate,
    val loop: LoopVo,
    val response: LoopDoneVo,
    val note: String,
    val estimated: Boolean,
    val scheduled: Boolean,
    val hasOccurrence: Boolean = true,
) {
    fun isSettled(today: LocalDate): Boolean = hasOccurrence && date <= today && when (response.done) {
        DoneState.DONE, DoneState.SKIP -> true
        DoneState.NO_RESPONSE -> date < today
        else -> false
    }

    fun asFullLoop(): FullLoopVo = loop.toFullLoopVo(
        LoopRetrospectVo(loop.loopId, date.toMs(), note), response.copy(date = date.toMs()),
    )

    fun asResponseRecord(): LoopResponseRecord = LoopResponseRecord(
        loopId = loop.loopId, title = loop.title, color = loop.color,
        date = date.toMs(), done = response.done,
        startInDay = response.startInDay, endInDay = response.endInDay,
        plannedStartInDay = if (estimated) -1 else loop.startInDay,
        isAnyTime = loop.isAnyTime, retrospect = note,
        timeSource = response.timeSource,
        measuredDurationMs = response.measuredDurationMs(),
    )
}

class LoopHistorySnapshot(histories: List<LoopHistory>) {
    val timelines = histories.map(::LoopTimeline)
    val byId = timelines.associateBy { it.current.loopId }
    val firstDate: LocalDate? = timelines.minOfOrNull { it.createdDate }

    fun days(from: LocalDate, to: LocalDate): List<ResolvedLoopDay> = timelines
        .flatMap { it.days(from, to) }
        .sortedWith(compareBy<ResolvedLoopDay> { it.date }.thenBy { it.loop.startInDay }.thenBy { it.loop.loopId })

    fun settled(from: LocalDate, to: LocalDate, today: LocalDate): List<ResolvedLoopDay> =
        days(from, minOf(to, today)).filter { it.isSettled(today) }
}
