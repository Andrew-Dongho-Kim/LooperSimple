package com.pnd.android.loop.ui.history

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.pnd.android.loop.data.FullLoopVo
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.data.history.LoopHistoryRepository
import com.pnd.android.loop.data.history.LoopHistorySnapshot
import com.pnd.android.loop.util.todayFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

@HiltViewModel
class DailyAchievementViewModel @Inject constructor(
    private val histories: LoopHistoryRepository,
) : ViewModel() {
    private fun <T> observe(calculate: (LoopHistorySnapshot, LocalDate) -> T): Flow<T> =
        combine(histories.snapshots, todayFlow(), calculate).flowOn(Dispatchers.Default)

    fun flowMonthReport(month: YearMonth, today: LocalDate? = null): Flow<AchievementLoadState<MonthInsightReport>> =
        observe { snapshot, currentDate -> buildMonthInsightReport(month, today ?: currentDate, snapshot) }
            .asAchievementState()

    fun flowDay(date: LocalDate): Flow<AchievementLoadState<List<FullLoopVo>>> =
        observe { snapshot, today ->
            if (date > today) emptyList() else snapshot.days(date, date).map { it.asFullLoop() }
        }.asAchievementState()

    fun flowCalendarDays(from: LocalDate, to: LocalDate, today: LocalDate? = null): Flow<AchievementLoadState<Map<LocalDate, AchievementCalendarDay>>> =
        observe { snapshot, currentDate ->
            val end = minOf(to, today ?: currentDate)
            resolveInsightDays(from, end, snapshot, today ?: currentDate).associate { day ->
                day.date to AchievementCalendarDay(day.doneCount, day.totalCount, day.hasNote)
            }
        }.asAchievementState()

    val flowMinCreatedDate = observe { snapshot, today -> snapshot.firstDate ?: today }

    @OptIn(ExperimentalCoroutinesApi::class)
    val achievementPager = histories.snapshots.flatMapLatest { snapshot ->
        Pager(PagingConfig(pageSize = 30)) { DailyAchievementPagingSource(snapshot, 30) }.flow
    }

    private fun flowRecords(from: LocalDate, to: LocalDate, completed: Boolean) = observe { snapshot, today ->
        snapshot.days(from, minOf(to, today)).filter { it.response.isDone() == completed }
            .map { LoopByDate(it.date, it.loop.loopId, it.loop.title, it.loop.color, it.note) }
    }

    fun flowsDoneLoopsByDate(from: LocalDate, to: LocalDate) = flowRecords(from, to, true).map { it.groupBy { record -> record.date } }
    fun flowsNoDonLoopsByDate(from: LocalDate, to: LocalDate) = flowRecords(from, to, false).map { it.groupBy { record -> record.date } }

    /** Legacy presentation uses the exact same report as the current monthly experience. */
    fun flowMonthSummary(month: YearMonth): Flow<MonthAchievementSummary> = observe { snapshot, today ->
        val report = buildMonthInsightReport(month, today, snapshot)
        MonthAchievementSummary(
            report.doneCount, report.totalCount, report.completionRate, report.activeDays,
            report.records.count { it.done == 1 && it.retrospect.isNotBlank() },
            report.timeMs, report.perfectDays, report.longestStreak,
            report.skippedCount, report.pendingCount,
            if (report.previousCount == 0) null else report.previousDone.toFloat() / report.previousCount,
        )
    }

    fun flowMonthRetrospects(month: YearMonth): Flow<List<LoopByDate>> = observe { snapshot, today ->
        snapshot.days(month.atDay(1), minOf(month.atEndOfMonth(), today))
            .filter { it.note.isNotBlank() }
            .map { LoopByDate(it.date, it.loop.loopId, it.loop.title, it.loop.color, it.note) }
            .sortedByDescending { it.date }
    }
}
