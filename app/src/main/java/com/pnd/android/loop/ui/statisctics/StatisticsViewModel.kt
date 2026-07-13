package com.pnd.android.loop.ui.statisctics

import androidx.lifecycle.ViewModel
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopWithStatistics
import com.pnd.android.loop.data.isDone
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

// 월별 투자 시간 / 완료율 추세 차트에 노출할 최근 개월 수.
private const val MONTHLY_CHART_MONTHS = 6

// 습관 건강 비교 구간(일). 최근 14일 vs 직전 14일을 본다.
private const val HABIT_HEALTH_WINDOW_DAYS = 14

// 신규 루프 정착률에서 '신규'로 볼 최근 생성 기간(일).
private const val NEW_LOOP_WINDOW_DAYS = 30

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
     * 기간([period]) 기반 지표 묶음. 응답 기록을 한 번 조회해 요약 KPI·시간대·요일·완벽한 날·
     * 스킵·회고·계획대비 실제를 한꺼번에 계산한다.
     */
    fun flowPeriodStats(period: StatisticsPeriod): Flow<PeriodStats> =
        fullLoopDao.getResponsesFlow(from = period.from(), to = period.to())
            .map { records -> computePeriodStats(records) }

    /**
     * ② 월별 완료율 추세(최근 [MONTHLY_CHART_MONTHS]개월). 기간 선택과 무관하게 전체 흐름을 본다.
     */
    fun flowCompletionTrend(): Flow<List<CompletionRatePoint>> =
        fullLoopDao.getMonthlyCompletionCountFlow().map { rows ->
            computeCompletionTrend(monthly = rows, months = MONTHLY_CHART_MONTHS)
        }

    /**
     * ⑩ 이번 달 완료 횟수 예측. 이번 달 완료 기록만으로 현재 페이스를 월말로 환산한다.
     */
    fun flowMonthlyProjection(): Flow<MonthlyProjection> {
        val today = LocalDate.now()
        val monthStart = today.withDayOfMonth(1).toMs()
        return fullLoopDao.getResponsesFlow(from = monthStart, to = today.toMs()).map { records ->
            computeMonthlyProjection(
                doneSoFar = records.count { it.done.isDone() },
                today = today,
            )
        }
    }

    /**
     * ⑥ 최근 완료율이 하락 중인 루프 목록. 기간 선택과 무관하게 항상 최근 흐름을 본다.
     */
    fun flowHabitHealth(): Flow<List<HabitHealth>> {
        val today = LocalDate.now()
        // 최근 구간 + 직전 구간(각 windowDays)을 모두 덮도록 2*window - 1 일 전부터 조회한다.
        val from = today.minusDays((2L * HABIT_HEALTH_WINDOW_DAYS - 1)).toMs()
        return fullLoopDao.getResponsesFlow(from = from, to = today.toMs()).map { records ->
            computeHabitHealth(records = records, today = today, windowDays = HABIT_HEALTH_WINDOW_DAYS)
        }
    }

    /**
     * ⑦ 최근 [NEW_LOOP_WINDOW_DAYS]일 안에 만든 루프들의 정착 현황.
     */
    fun flowNewLoopSettling(): Flow<List<NewLoopSettling>> {
        val today = LocalDate.now()
        val since = today.minusDays(NEW_LOOP_WINDOW_DAYS.toLong()).toMs()
        return fullLoopDao.getNewLoopsFlow(since = since).map { rows ->
            computeSettling(newLoops = rows, today = today)
        }
    }

    /**
     * ⑨ 누적 성취 마일스톤(투자시간·총 완료 횟수·최장 스트릭). 전체 기록 기준.
     */
    fun flowMilestones(): Flow<List<Milestone>> {
        val to = LocalDate.now().toMs()
        return combine(
            fullLoopDao.getInvestedTimeFlow(from = 0L, to = to),
            fullLoopDao.getTotalDoneCountFlow(),
            fullLoopDao.getDoneDatesFlow(),
        ) { investedMs, totalDone, doneDates ->
            computeMilestones(
                totalInvestedMs = investedMs,
                totalDoneCount = totalDone,
                longestStreak = computeStreak(doneDates = doneDates.map { it.toLocalDate() }).longest,
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
}
