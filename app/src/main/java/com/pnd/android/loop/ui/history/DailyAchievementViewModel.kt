package com.pnd.android.loop.ui.history

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.MonthlyCompletionCount
import com.pnd.android.loop.data.isDone
import com.pnd.android.loop.data.isSkip
import com.pnd.android.loop.ui.statisctics.computePerfectDays
import com.pnd.android.loop.ui.statisctics.computeStreak
import com.pnd.android.loop.ui.statisctics.investedTimeMs
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

private const val PAGE_SIZE = 30

@HiltViewModel
class DailyAchievementViewModel @Inject constructor(
    appDb: AppDatabase,
) : ViewModel() {

    private val loopDao = appDb.loopDao()
    private val loopWithDoneDao = appDb.fullLoopDao()

    val flowMinCreatedDate = loopDao.getMinCreatedTimeFlow()
        .map { minCreated -> minCreated?.toLocalDate() ?: LocalDate.now() }

    val achievementPager = Pager(
        PagingConfig(pageSize = PAGE_SIZE),
        pagingSourceFactory = {
            DailyAchievementPagingSource(
                appDb = appDb,
                pageSize = PAGE_SIZE,
            )
        }
    ).flow

    // inclusive all
    fun flowsDoneLoopsByDate(from: LocalDate, to: LocalDate) = loopWithDoneDao.getDoneLoopsByDateFlow(
        from = from.toMs(),
        to = to.toMs(),
    ).map { doneLoops ->
        doneLoops.groupBy { it.date }
    }

    fun flowsNoDonLoopsByDate(from: LocalDate, to: LocalDate) = loopWithDoneDao.getNoDoneLoopsByDateFlow(
        from = from.toMs(),
        to = to.toMs(),
    ).map { doneLoops ->
        doneLoops.groupBy { it.date }
    }

    /**
     * 특정 달([yearMonth])의 달성 요약.
     *
     * 그 달의 응답 기록 한 벌([FullLoopDao.getResponsesFlow])만 있으면 완료율·투자 시간·완벽한 날·
     * 월내 최장 연속·건너뜀/무응답까지 모두 계산할 수 있어, 지표를 늘려도 쿼리는 늘지 않는다.
     * 계산은 통계 화면과 같은 순수 함수(`computePerfectDays`, `computeStreak`, `investedTimeMs`)를
     * 재사용해, 같은 값이 두 화면에서 다르게 보이지 않게 한다.
     *
     * 여기에 전체 기간의 월별 집계를 곁들여 지난달 완료율을 함께 넘긴다(전월 대비 증감용).
     * 달성률 분모(totalCount)는 완료 + 미완료(건너뜀·무응답)이며, 비활성(DISABLED)은 제외된다.
     */
    fun flowMonthSummary(yearMonth: YearMonth): Flow<MonthAchievementSummary> {
        val from = yearMonth.atDay(1).toMs()
        val to = yearMonth.atEndOfMonth().toMs()
        val responsesFlow = loopWithDoneDao.getResponsesFlow(from = from, to = to)
        val monthlyFlow = loopWithDoneDao.getMonthlyCompletionCountFlow()

        return combine(responsesFlow, monthlyFlow) { records, monthlyCounts ->
            val doneRecords = records.filter { it.done.isDone() }
            val total = records.size

            MonthAchievementSummary(
                doneCount = doneRecords.size,
                totalCount = total,
                completionRate = if (total == 0) 0f else doneRecords.size.toFloat() / total,
                activeDays = doneRecords.map { it.date }.distinct().size,
                retrospectCount = records.count { !it.retrospect.isNullOrBlank() },
                investedTimeMs = doneRecords.sumOf { it.investedTimeMs() },
                perfectDays = computePerfectDays(records = records),
                // 그 달 안에서만 이어진 최장 연속. 전체 기록 기준 스트릭과 달리 달마다 값이 바뀐다.
                longestStreak = computeStreak(
                    doneDates = doneRecords.map { it.date.toLocalDate() },
                ).longest,
                skippedCount = records.count { it.done.isSkip() },
                noResponseCount = records.count { it.done == LoopDoneVo.DoneState.NO_RESPONSE },
                prevMonthCompletionRate = monthlyCounts.completionRateOf(yearMonth.minusMonths(1)),
            )
        }
    }

    /** 월별 집계 목록에서 [yearMonth]의 완료율(0f..1f)을 찾는다. 기록이 없으면 null. */
    private fun List<MonthlyCompletionCount>.completionRateOf(yearMonth: YearMonth): Float? =
        firstOrNull { it.year == yearMonth.year && it.month == yearMonth.monthValue }
            ?.takeIf { it.respondedCount > 0 }
            ?.let { it.doneCount.toFloat() / it.respondedCount }

    /**
     * 특정 달([yearMonth])에 남긴 회고 모음(최신 날짜 우선). 완료·미완료 기록 중 회고가 있는 것만 모은다.
     * 하루 카드에 흩어져 있던 회고를 한곳에서 훑어볼 수 있게 한다.
     */
    fun flowMonthRetrospects(yearMonth: YearMonth): Flow<List<LoopByDate>> {
        val from = yearMonth.atDay(1).toMs()
        val to = yearMonth.atEndOfMonth().toMs()
        val doneFlow = loopWithDoneDao.getDoneLoopsByDateFlow(from = from, to = to)
        val missedFlow = loopWithDoneDao.getNoDoneLoopsByDateFlow(from = from, to = to)

        return combine(doneFlow, missedFlow) { done, missed ->
            (done + missed)
                .filter { !it.retrospect.isNullOrBlank() }
                .sortedByDescending { it.date }
        }
    }
}