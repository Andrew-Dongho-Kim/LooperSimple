package com.pnd.android.loop.ui.statisctics

import androidx.lifecycle.ViewModel
import com.pnd.android.loop.data.*
import com.pnd.android.loop.data.history.LoopHistoryRepository
import com.pnd.android.loop.data.history.LoopHistorySnapshot
import com.pnd.android.loop.data.history.ResolvedLoopDay
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.todayFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    private val histories: LoopHistoryRepository,
) : ViewModel() {
    private fun <T> observe(calculate: (LoopHistorySnapshot, LocalDate) -> T): Flow<T> =
        combine(histories.snapshots, todayFlow(), calculate).flowOn(Dispatchers.Default)

    private fun LoopHistorySnapshot.periodDays(period: StatisticsPeriod, today: LocalDate): List<ResolvedLoopDay> =
        settled(maxOf(firstDate ?: today, period.from(today).toLocalDate()), period.to(today).toLocalDate(), today)

    val hasEstimatedHistory = observe { snapshot, _ ->
        snapshot.timelines.any { timeline ->
            timeline.history.revisions.any { it.knownFrom > timeline.createdDate.toEpochDay() }
        }
    }

    fun flowPeriodStats(period: StatisticsPeriod): Flow<PeriodStats> = observe { snapshot, today ->
        val allDays = snapshot.days(
            maxOf(snapshot.firstDate ?: today, period.from(today).toLocalDate()),
            minOf(today, period.to(today).toLocalDate()),
        )
        computePeriodStats(allDays.filter { it.isSettled(today) }.map { it.asResponseRecord() })
            .copy(perfectDays = allDays.filter { it.hasOccurrence }.groupBy { it.date }
                .count { (_, days) -> days.all { it.response.isDone() } })
    }

    fun flowLoopRanking(period: StatisticsPeriod): Flow<List<LoopWithStatistics>> = observe { snapshot, today ->
        snapshot.periodDays(period, today).groupBy { it.loop.loopId }.map { (id, days) ->
            val loop = snapshot.byId.getValue(id).current
            val done = days.filter { it.response.isDone() }
            LoopWithStatistics(
                loopId = id, title = loop.title, color = loop.color,
                doneRate = done.size.toFloat() / days.size, doneCount = done.size,
                investedTimeMs = done.sumOf { it.asResponseRecord().investedTimeMs() },
            )
        }.sortedByDescending { it.doneRate }
    }

    fun flowCompletionTrend(): Flow<List<CompletionRatePoint>> = observe { snapshot, today ->
        val start = YearMonth.from(today).minusMonths(5).atDay(1)
        snapshot.settled(start, today, today).groupBy { YearMonth.from(it.date) }.map { (month, days) ->
            CompletionRatePoint(month, days.count { it.response.isDone() }.toFloat() / days.size)
        }.sortedBy { it.yearMonth }
    }

    fun flowMonthlyProjection(): Flow<MonthlyProjection> = observe { snapshot, today ->
        computeMonthlyProjection(
            snapshot.settled(today.withDayOfMonth(1), today, today).count { it.response.isDone() }, today,
        )
    }

    fun flowHabitHealth(): Flow<List<HabitHealth>> = observe { snapshot, today ->
        val records = snapshot.settled(today.minusDays(27), today, today).map { day ->
            val current = snapshot.byId.getValue(day.loop.loopId).current
            day.asResponseRecord().copy(title = current.title, color = current.color)
        }
        computeHabitHealth(records, today, windowDays = 14)
    }

    fun flowNewLoopSettling(): Flow<List<NewLoopSettling>> = observe { snapshot, today ->
        val loops = snapshot.timelines.filter { it.current.enabled && it.createdDate >= today.minusDays(30) }
        val records = loops.map { timeline ->
            val days = timeline.days(timeline.createdDate, today).filter { it.isSettled(today) }
            val loop = timeline.current
            NewLoopRecord(loop.loopId, loop.title, loop.color, loop.created, days.size, days.count { it.response.isDone() })
        }
        computeSettling(records, today)
    }

    fun flowMilestones(): Flow<List<Milestone>> = observe { snapshot, today ->
        val days = snapshot.settled(snapshot.firstDate ?: today, today, today).filter { it.response.isDone() }
        computeMilestones(
            totalInvestedMs = days.sumOf { it.asResponseRecord().investedTimeMs() },
            totalDoneCount = days.size,
            longestStreak = computeStreak(days.map { it.date }, today).longest,
        )
    }

    fun flowMonthlyInvestedTime(): Flow<List<MonthlyInvestedTime>> = observe { snapshot, today ->
        val days = snapshot.settled(YearMonth.from(today).minusMonths(5).atDay(1), today, today)
        val totals = days.filter { it.response.isDone() }.groupBy { YearMonth.from(it.date) }
            .mapValues { (_, records) -> records.sumOf { it.asResponseRecord().investedTimeMs() } }
        val maximum = totals.values.maxOrNull() ?: 0L
        totals.toSortedMap().map { (month, time) ->
            MonthlyInvestedTime(month, time, if (maximum == 0L) 0f else time.toFloat() / maximum)
        }
    }

    fun flowStreak(): Flow<StreakStat> = observe { snapshot, today ->
        val dates = snapshot.settled(snapshot.firstDate ?: today, today, today)
            .filter { it.response.isDone() }.map { it.date }
        computeStreak(dates, today)
    }
}
