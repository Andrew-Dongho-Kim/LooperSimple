package com.pnd.android.loop.data.history

import com.pnd.android.loop.data.*
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.state.NOT_SCHEDULED
import com.pnd.android.loop.state.isSettledOn
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

    /** Recorded responses take precedence over settings; a note alone is not an occurrence. */
    fun stateOn(date: LocalDate): Int {
        if (date < createdDate) return NOT_SCHEDULED
        responseOn(date)?.let { return it.done }
        val day = day(date)
        if (day?.hasOccurrence == true) return day.response.done
        return if (settingsOn(date).enabled) NOT_SCHEDULED else DoneState.DISABLED
    }

    /** Includes disabled and unscheduled days so every calendar uses the same interpretation. */
    fun states(from: LocalDate, to: LocalDate): Map<LocalDate, Int> = buildMap {
        var date = maxOf(from, createdDate)
        while (date <= to) {
            put(date, stateOn(date))
            date = date.plusDays(1)
        }
    }

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

    /**
     * Current labels, but the plan/state of the requested occurrence.
     *
     * 그날 몫이 없어도([day] 가 null 이어도) 미응답 행을 만들어 돌려준다. 그러므로 "그날
     * 답하지 않았다"를 판정하는 데 써서는 안 된다. 그런 곳은 [occurrenceOn] 을 쓴다.
     *
     * [LoopWithDone.enabled] 만은 그 날짜가 아니라 현재 값이다. 오늘 기준으로 켜짐/꺼짐을
     * 봐야 하는 곳(알람 예약·전체 탭의 비활성 묶음)이 이 값을 쓴다. 그 날짜에 편성돼 있었는지는
     * [day] 의 [ResolvedLoopDay.scheduled] / [ResolvedLoopDay.hasOccurrence] 로 봐야 한다.
     */
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

    /**
     * [date] 에 실제로 걸치는 occurrence. 그날 예정도 기록도 없었으면 null.
     *
     * 비활성이거나 활동 요일이 아니어서 애초에 할 일이 없었던 날을 [liveLoop] 은 미응답 행으로
     * 만들어 낸다. 그 행을 그대로 쓰면 꺼 둔 루프가 "답하지 않은 루프"로 계속 보이므로,
     * 그날 몫이 있었는지를 따져야 하는 곳은 [liveLoop] 대신 이 함수를 쓴다.
     */
    fun occurrenceOn(date: LocalDate): LoopWithDone? =
        if (day(date)?.hasOccurrence == true) liveLoop(date) else null
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
    fun isSettled(today: LocalDate): Boolean = hasOccurrence && response.done.isSettledOn(date, today)

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

    /**
     * 이력 도입 이전 구간이 있어 과거 일정을 초기 설정으로 추정한 루프가 하나라도 있는가.
     *
     * 추정 구간에서는 그날 예정이었는지 알 수 없어 놓친 날이 분모에서 누락될 수 있고, 그만큼
     * 완료율이 실제보다 높게 나온다. 완료율을 보여 주는 화면은 이 값으로 그 사실을 고지한다.
     */
    val hasEstimatedHistory: Boolean = timelines.any { timeline ->
        timeline.history.revisions.any { it.knownFrom > timeline.createdDate.toEpochDay() }
    }

    fun days(from: LocalDate, to: LocalDate): List<ResolvedLoopDay> = timelines
        .flatMap { it.days(from, to) }
        .sortedWith(compareBy<ResolvedLoopDay> { it.date }.thenBy { it.loop.startInDay }.thenBy { it.loop.loopId })

    fun settled(from: LocalDate, to: LocalDate, today: LocalDate): List<ResolvedLoopDay> =
        days(from, minOf(to, today)).filter { it.isSettled(today) }
}
