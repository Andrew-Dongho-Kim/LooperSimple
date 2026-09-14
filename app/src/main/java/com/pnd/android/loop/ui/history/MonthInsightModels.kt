package com.pnd.android.loop.ui.history

import com.pnd.android.loop.data.history.*
import com.pnd.android.loop.data.FullLoopVo
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.isDone
import com.pnd.android.loop.data.toFullLoopVo
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.roundToInt

data class MonthComparisonPeriod(
    val currentEnd: LocalDate,
    val previousStart: LocalDate,
    val previousEnd: LocalDate,
)

internal fun monthComparisonPeriod(month: YearMonth, today: LocalDate): MonthComparisonPeriod {
    val previous = month.minusMonths(1)
    // A partial month compares equal day counts, including February and leap years.
    val equalDays = minOf(today.dayOfMonth, previous.lengthOfMonth())
    return if (month == YearMonth.from(today)) {
        MonthComparisonPeriod(month.atDay(equalDays), previous.atDay(1), previous.atDay(equalDays))
    } else {
        MonthComparisonPeriod(month.atEndOfMonth(), previous.atDay(1), previous.atEndOfMonth())
    }
}

data class InsightDay(
    val date: LocalDate,
    val records: List<FullLoopVo>,
    val settledRecords: List<FullLoopVo> = records,
    val estimated: Boolean = false,
    val occurrenceCount: Int = records.size,
    val unansweredCount: Int = records.count { it.done == DoneState.NO_RESPONSE },
) {
    val doneCount get() = records.count { it.done == DoneState.DONE }
    val totalCount get() = settledRecords.size
    val rate get() = if (totalCount == 0) null else doneCount.toFloat() / totalCount
    val timeMs get() = records.sumOf { it.insightDurationMs() }
    val hasNote get() = records.any { it.retrospect.isNotBlank() }
}

data class WeekdayInsight(val day: DayOfWeek, val doneCount: Int, val totalCount: Int, val observedDays: Int) {
    val rate get() = if (totalCount == 0) null else doneCount.toFloat() / totalCount
}

data class LoopMonthInsight(
    val loopId: Int,
    val title: String,
    val doneCount: Int,
    val totalCount: Int,
    val timeMs: Long,
    val previousDoneCount: Int,
    val previousTotalCount: Int,
    val comparableDoneCount: Int,
    val comparableTotalCount: Int,
) {
    val rate get() = doneCount.toFloat() / totalCount
    val deltaPoints: Int? get() =
        if (previousTotalCount < 5 || comparableTotalCount < 5) null
        else ((comparableDoneCount.toFloat() / comparableTotalCount -
            previousDoneCount.toFloat() / previousTotalCount) * 100).roundToInt()
}

data class MonthInsightReport(
    val month: YearMonth,
    val today: LocalDate,
    val end: LocalDate,
    val days: List<InsightDay>,
    val comparison: MonthComparisonPeriod,
    val previousCount: Int,
    val previousDone: Int,
    val comparableCount: Int,
    val comparableDone: Int,
    val weekdays: List<WeekdayInsight>,
    val loops: List<LoopMonthInsight>,
) {
    val records get() = days.flatMap { it.records }
    val hasEstimatedHistory get() = days.any { it.estimated }
    val totalCount get() = days.sumOf { it.totalCount }
    val doneCount get() = days.sumOf { it.doneCount }
    val completionRate get() = if (totalCount == 0) 0f else doneCount.toFloat() / totalCount
    val skippedCount get() = records.count { it.done == DoneState.SKIP }
    val pendingCount get() = days.sumOf { it.unansweredCount }
    val inProgressCount get() = records.count { it.done == DoneState.IN_PROGRESS }
    val activeDays get() = days.count { it.doneCount > 0 }
    val perfectDays get() = days.count { it.occurrenceCount > 0 && it.doneCount == it.occurrenceCount }
    val occurrenceCount get() = days.sumOf { it.occurrenceCount }
    val timeMs get() = days.sumOf { it.timeMs }
    val notes get() = records.filter { it.retrospect.isNotBlank() }.sortedByDescending { it.date }
    val timedCount get() = records.count { it.done.isDone() && it.hasInsightTime() }
    val longestStreak: Int get() {
        var current = 0
        var longest = 0
        days.forEach { day ->
            current = if (day.doneCount > 0) current + 1 else 0
            longest = maxOf(longest, current)
        }
        return longest
    }
    val restartOpportunities get() = days.zipWithNext().filter { (a, b) ->
        a.totalCount > 0 && a.doneCount == 0 && b.totalCount > 0 &&
            (b.date.isBefore(today) || b.doneCount > 0)
    }
    val restartedCount get() = restartOpportunities.count { (_, b) -> b.doneCount > 0 }
    val deltaPoints: Int? get() = if (previousCount == 0 || comparableCount == 0) null
        else ((comparableDone.toFloat() / comparableCount - previousDone.toFloat() / previousCount) * 100).roundToInt()
    val bestWeekday: WeekdayInsight? get() {
        val eligible = weekdays.filter { it.observedDays >= 3 && it.totalCount >= 5 }
        if (eligible.size < 2 || eligible.map { it.rate }.distinct().size < 2) return null
        return eligible.maxByOrNull { it.rate ?: 0f }
    }
    val mostImproved: LoopMonthInsight? get() =
        loops.filter { (it.deltaPoints ?: 0) > 0 }.maxByOrNull { it.deltaPoints ?: 0 }
}

internal fun FullLoopVo.hasInsightTime(): Boolean =
    actualStartInDay in 0 until 86_400_000L && actualEndInDay in 0 until 86_400_000L

internal fun FullLoopVo.insightDurationMs(): Long {
    if (!done.isDone()) return 0
    measuredDurationMs?.let { return it }
    if (!hasInsightTime()) return 0
    val raw = actualEndInDay - actualStartInDay
    return if (raw >= 0) raw else raw + 86_400_000L
}

/** Compatibility for fixtures and callers that already have the four history components. */
internal fun resolveInsightDays(
    from: LocalDate,
    to: LocalDate,
    loops: List<LoopVo>,
    saved: List<LoopDoneVo>,
    notes: List<LoopRetrospectVo>,
): List<InsightDay> = resolveInsightDays(from, to, historySnapshot(loops, saved, notes), to.plusDays(1))

internal fun resolveInsightDays(
    from: LocalDate,
    to: LocalDate,
    snapshot: LoopHistorySnapshot,
    today: LocalDate,
): List<InsightDay> {
    val byDate = snapshot.days(from, to).groupBy { it.date }
    return buildList {
        var date = from
        while (date <= to) {
            val days = byDate[date].orEmpty()
            add(InsightDay(
                date, days.map { it.asFullLoop() },
                days.filter { it.isSettled(today) }.map { it.asFullLoop() },
                days.any { it.estimated },
                occurrenceCount = days.count { it.hasOccurrence },
                unansweredCount = days.count { it.hasOccurrence && it.response.done == DoneState.NO_RESPONSE },
            ))
            date = date.plusDays(1)
        }
    }
}

private fun historySnapshot(loops: List<LoopVo>, saved: List<LoopDoneVo>, notes: List<LoopRetrospectVo>) =
    LoopHistorySnapshot(loops.map { loop ->
        LoopHistory(loop, emptyList(), saved.filter { it.loopId == loop.loopId }, notes.filter { it.loopId == loop.loopId })
    })

internal fun buildMonthInsightReport(
    month: YearMonth,
    today: LocalDate,
    loops: List<LoopVo>,
    saved: List<LoopDoneVo>,
    notes: List<LoopRetrospectVo>,
): MonthInsightReport = buildMonthInsightReport(month, today, historySnapshot(loops, saved, notes))

internal fun buildMonthInsightReport(
    month: YearMonth,
    today: LocalDate,
    snapshot: LoopHistorySnapshot,
): MonthInsightReport {
    val end = minOf(month.atEndOfMonth(), today)
    val comparison = monthComparisonPeriod(month, today)
    val days = resolveInsightDays(month.atDay(1), end, snapshot, today)
    val previousDays = snapshot.settled(comparison.previousStart, minOf(comparison.previousEnd, today), today)
    // Known scheduled misses are valid comparison samples, even without stored response rows.
    val comparableIds = previousDays.filter { !it.estimated || it.response.revisionId != null ||
        snapshot.byId[it.loop.loopId]?.responseOn(it.date) != null }.map { it.loop.loopId }.toSet()
    val previous = previousDays.map { it.asFullLoop() }
    val comparable = days.filter { it.date <= comparison.currentEnd }.flatMap { it.settledRecords }
    val records = days.flatMap { it.settledRecords }
    val byPreviousLoop = previous.groupBy { it.loopId }
    val byComparableLoop = comparable.groupBy { it.loopId }
    val loopInsights = records.groupBy { it.loopId }.map { (id, values) ->
        val before = byPreviousLoop[id].orEmpty()
        val current = byComparableLoop[id].orEmpty()
        LoopMonthInsight(
            id, snapshot.byId.getValue(id).current.title, values.count { it.done.isDone() }, values.size,
            values.sumOf { it.insightDurationMs() }, before.count { it.done.isDone() },
            if (id in comparableIds) before.size else 0,
            current.count { it.done.isDone() }, current.size,
        )
    }.sortedWith(compareByDescending<LoopMonthInsight> { it.rate }.thenBy { it.title })
    return MonthInsightReport(
        month, today, end, days, comparison,
        if (comparableIds.isNotEmpty()) previous.size else 0,
        previous.count { it.done.isDone() }, comparable.size, comparable.count { it.done.isDone() },
        (0..6).map { index ->
            val weekday = if (index == 0) DayOfWeek.SUNDAY else DayOfWeek.of(index)
            val matching = days.filter { it.date.dayOfWeek == weekday }
            WeekdayInsight(weekday, matching.sumOf { it.doneCount }, matching.sumOf { it.totalCount }, matching.count { it.totalCount > 0 })
        }, loopInsights,
    )
}
