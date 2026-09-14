package com.pnd.android.loop.appwidget

import androidx.annotation.Keep
import com.pnd.android.loop.data.history.LoopHistorySnapshot
import com.pnd.android.loop.data.history.ResolvedLoopDay
import com.pnd.android.loop.data.history.localDate
import com.pnd.android.loop.data.isDone
import com.pnd.android.loop.data.isSkip
import com.pnd.android.loop.ui.statisctics.computeStreak
import com.pnd.android.loop.ui.statisctics.investedTimeMs
import java.time.LocalDate
import java.time.YearMonth

/** No-arg constructor and setters for Jackson, including minified builds. */
@Keep
class StatisticsWidgetData {
    var date: String = ""
    var registered: Int = 0
    var todayDone: Int = 0
    var todayTotal: Int = 0
    var todaySkipped: Int = 0
    var todayTimeMs: Long = 0
    var currentStreak: Int = 0
    var longestStreak: Int = 0
    var weekDone: Int = 0
    var weekTotal: Int = 0
    /** Monday first; -1 means future, not zero completions. */
    var weekCounts: List<Int> = emptyList()
    var bestTitle: String = ""
    var bestDone: Int = 0
    var bestTotal: Int = 0
    var monthDone: Int = 0
    var monthTotal: Int = 0
    /** Six consecutive months, including zero months. */
    var monthTimes: List<Long> = emptyList()
    var estimated: Boolean = false
}

/** Same revision-aware resolver, settled denominator and duration rules as the app.
 * Today includes pending plans. Dates are occurrence/start dates, including overnight work.
 * Time may be planned; do not label it measured focus time.
 */
internal fun computeStatisticsWidgetData(snapshot: LoopHistorySnapshot, today: LocalDate): StatisticsWidgetData {
    val month = YearMonth.from(today)
    val weekStart = today.minusDays(today.dayOfWeek.value - 1L)
    val days = snapshot.days(month.minusMonths(5).atDay(1), today)
    val settled = days.filter { it.isSettled(today) }
    val todayDays = days.filter { it.date == today && it.hasOccurrence }
    val week = settled.filter { it.date >= weekStart }
    val currentMonth = settled.filter { it.date >= month.atDay(1) }
    // Stored completions suffice for streaks; avoid expanding years of missed plans.
    val doneDates = snapshot.timelines.flatMap { timeline ->
        timeline.history.responses.filter { it.done.isDone() }.mapNotNull { response ->
            response.localDate().takeIf { it <= today && timeline.day(it)?.response?.done?.isDone() == true }
        }
    }
    val streak = computeStreak(doneDates, today)
    val best = week.groupBy { it.loop.loopId }.values
        .filter { group -> group.any { it.response.isDone() } }
        .sortedWith(compareByDescending<List<ResolvedLoopDay>> { group ->
            group.count { it.response.isDone() }.toDouble() / group.size
        }.thenByDescending { group -> group.count { it.response.isDone() } }
            .thenBy { group -> group.first().loop.loopId }).firstOrNull()
    val monthlyTimes = settled.filter { it.response.isDone() }.groupBy { YearMonth.from(it.date) }
        .mapValues { (_, group) -> group.sumOf { it.asResponseRecord().investedTimeMs() } }
    return StatisticsWidgetData().apply {
        date = today.toString()
        registered = snapshot.timelines.size
        todayDone = todayDays.count { it.response.isDone() }
        todayTotal = todayDays.size
        todaySkipped = todayDays.count { it.response.isSkip() }
        todayTimeMs = todayDays.filter { it.response.isDone() }.sumOf { it.asResponseRecord().investedTimeMs() }
        currentStreak = streak.current
        longestStreak = streak.longest
        weekDone = week.count { it.response.isDone() }
        weekTotal = week.size
        weekCounts = (0L..6L).map { offset ->
            val day = weekStart.plusDays(offset)
            if (day > today) -1 else week.count { it.date == day && it.response.isDone() }
        }
        best?.let {
            bestTitle = snapshot.byId.getValue(it.first().loop.loopId).current.title
            bestDone = it.count { day -> day.response.isDone() }
            bestTotal = it.size
        }
        monthDone = currentMonth.count { it.response.isDone() }
        monthTotal = currentMonth.size
        monthTimes = (5L downTo 0L).map { monthlyTimes[month.minusMonths(it)] ?: 0L }
        estimated = snapshot.timelines.any { timeline ->
            timeline.history.revisions.any { it.knownFrom > timeline.createdDate.toEpochDay() }
        }
    }
}
