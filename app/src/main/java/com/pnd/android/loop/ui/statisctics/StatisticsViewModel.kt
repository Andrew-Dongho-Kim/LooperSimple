package com.pnd.android.loop.ui.statisctics

import androidx.lifecycle.ViewModel
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.data.LoopWithStatistics
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.pnd.android.loop.util.toLocalDate
import kotlinx.coroutines.flow.map
import java.time.DayOfWeek
import java.time.YearMonth
import javax.inject.Inject

// 월별 투자 시간 차트에 노출할 최근 개월 수.
private const val MONTHLY_CHART_MONTHS = 6

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
        val investedFlow = fullLoopDao.getInvestedTimeFlow(from = period.from(), to = period.to())

        return combine(doneFlow, missedFlow, investedFlow) { done, missed, investedTimeMs ->
            StatisticsUiState(
                summary = summaryOf(done = done, missed = missed, investedTimeMs = investedTimeMs),
                dayOfWeekStats = dayOfWeekStatsOf(done = done),
                isEmpty = done.isEmpty() && missed.isEmpty(),
            )
        }
    }

    /**
     * 월별 루프 투자 시간을 최근 [MONTHLY_CHART_MONTHS]개월만 잘라내어, 표시되는 달들 중
     * 최댓값 대비 비율([MonthlyInvestedTime.ratio])과 함께 반환한다. 기간 선택과 무관하게
     * 항상 전체 데이터의 최근 흐름을 보여준다.
     */
    fun flowMonthlyInvestedTime(): Flow<List<MonthlyInvestedTime>> =
        fullLoopDao.getMonthlyInvestedTimeFlow().map { rows ->
            val recent = rows.takeLast(MONTHLY_CHART_MONTHS)
            val busiest = recent.maxOfOrNull { it.durationMs } ?: 0L
            recent.map { row ->
                MonthlyInvestedTime(
                    yearMonth = YearMonth.of(row.year, row.month),
                    investedTimeMs = row.durationMs,
                    ratio = if (busiest == 0L) 0f else row.durationMs.toFloat() / busiest,
                )
            }
        }

    /**
     * 완료한 날짜 목록으로부터 현재/최장 연속 달성 스트릭을 계산한다.
     * 기간 선택과 무관하게 전체 기록을 대상으로 한다.
     */
    fun flowStreak(): Flow<StreakStat> =
        fullLoopDao.getDoneDatesFlow().map { millis ->
            computeStreak(doneDates = millis.map { it.toLocalDate() })
        }

    private fun summaryOf(
        done: List<LoopByDate>,
        missed: List<LoopByDate>,
        investedTimeMs: Long,
    ): StatisticsSummary {
        val totalResponses = done.size + missed.size
        return StatisticsSummary(
            completedCount = done.size,
            completionRate = if (totalResponses == 0) 0f else done.size.toFloat() / totalResponses,
            activeLoops = (done + missed).map { it.loopId }.distinct().size,
            activeDays = done.map { it.date }.distinct().size,
            investedTimeMs = investedTimeMs,
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
