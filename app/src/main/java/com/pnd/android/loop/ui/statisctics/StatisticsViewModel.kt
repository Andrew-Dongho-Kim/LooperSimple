package com.pnd.android.loop.ui.statisctics

import androidx.lifecycle.ViewModel
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.data.LoopWithStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    appDb: AppDatabase,
) : ViewModel() {

    private val fullLoopDao = appDb.fullLoopDao()

    /**
     * Loops ranked by their done rate within [period]. Loops that never ran in the
     * period (and therefore have no meaningful rate) are dropped so the ranking only
     * shows habits the user actually engaged with.
     */
    fun flowLoopRanking(period: StatisticsPeriod): Flow<List<LoopWithStatistics>> =
        fullLoopDao.getLoopsWithStatisticsFlow(from = period.from(), to = period.to())
            .map { loops -> loops.filter { it.doneRate.isFinite() } }

    /**
     * Summary KPIs and the day-of-week distribution for [period], merged from the
     * "done" and "missed" event streams into a single UI state emission.
     */
    fun flowStatistics(period: StatisticsPeriod): Flow<StatisticsUiState> {
        val doneFlow = fullLoopDao.getDoneLoopsByDateFlow(from = period.from(), to = period.to())
        val missedFlow = fullLoopDao.getNoDoneLoopsByDateFlow(from = period.from(), to = period.to())

        return combine(doneFlow, missedFlow) { done, missed ->
            StatisticsUiState(
                summary = summaryOf(done = done, missed = missed),
                dayOfWeekStats = dayOfWeekStatsOf(done = done),
                isEmpty = done.isEmpty() && missed.isEmpty(),
            )
        }
    }

    private fun summaryOf(
        done: List<LoopByDate>,
        missed: List<LoopByDate>,
    ): StatisticsSummary {
        val totalResponses = done.size + missed.size
        return StatisticsSummary(
            completedCount = done.size,
            completionRate = if (totalResponses == 0) 0f else done.size.toFloat() / totalResponses,
            activeLoops = (done + missed).map { it.loopId }.distinct().size,
            activeDays = done.map { it.date }.distinct().size,
        )
    }

    private fun dayOfWeekStatsOf(done: List<LoopByDate>): List<DayOfWeekStat> {
        val countByDay = done.groupingBy { it.date.dayOfWeek }.eachCount()
        val busiest = countByDay.values.maxOrNull() ?: 0
        return DayOfWeek.entries.map { day ->
            val count = countByDay[day] ?: 0
            DayOfWeekStat(
                dayOfWeek = day,
                completedCount = count,
                ratio = if (busiest == 0) 0f else count.toFloat() / busiest,
            )
        }
    }
}
