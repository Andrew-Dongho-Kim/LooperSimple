package com.pnd.android.loop.ui.history

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopByDate
import com.pnd.android.loop.ui.statisctics.StreakStat
import com.pnd.android.loop.ui.statisctics.computeStreak
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject

private const val PAGE_SIZE = 30
private const val PREF_NAME = "daily_achievement"
private const val KEY_VIEW_MODE = "key_view_mode"

@HiltViewModel
class DailyAchievementViewModel @Inject constructor(
    app: Application,
    appDb: AppDatabase,
) : AndroidViewModel(app) {

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

    private val pref = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _flowViewMode = MutableStateFlow(
        DailyAchievementPageViewMode.valueOf(
            pref.getString(KEY_VIEW_MODE, null) ?: "${DailyAchievementPageViewMode.COLOR_DOT}"
        )
    )
    val flowViewMode: Flow<DailyAchievementPageViewMode> = _flowViewMode
    fun toggleViewMode() {
        _flowViewMode.value = if (_flowViewMode.value == DailyAchievementPageViewMode.COLOR_DOT) {
            DailyAchievementPageViewMode.DESCRIPTION_TEXT
        } else {
            DailyAchievementPageViewMode.COLOR_DOT
        }
        pref.edit {
            putString(KEY_VIEW_MODE, "${_flowViewMode.value}")
        }
    }

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

    // 연속 달성 스트릭(전체 기록 기준). 통계 화면·홈 헤더와 동일한 computeStreak 규칙을 재사용해
    // 어디서 보든 스트릭 값이 일치하게 한다.
    fun flowStreak(): Flow<StreakStat> = loopWithDoneDao.getDoneDatesFlow()
        .map { millis -> computeStreak(doneDates = millis.map { it.toLocalDate() }) }

    /**
     * 특정 달([yearMonth])의 달성 요약. 완료/미완료 기록과 투자 시간 스트림을 하나로 합쳐,
     * 달력 상단 배너가 한 번의 방출로 그려질 수 있게 한다.
     * 달성률 분모(totalCount)는 완료 + 미완료(건너뜀·무응답)이며, 비활성(DISABLED)은 제외된다.
     */
    fun flowMonthSummary(yearMonth: YearMonth): Flow<MonthAchievementSummary> {
        val from = yearMonth.atDay(1).toMs()
        val to = yearMonth.atEndOfMonth().toMs()
        val doneFlow = loopWithDoneDao.getDoneLoopsByDateFlow(from = from, to = to)
        val missedFlow = loopWithDoneDao.getNoDoneLoopsByDateFlow(from = from, to = to)
        val investedFlow = loopWithDoneDao.getInvestedTimeFlow(from = from, to = to)

        return combine(doneFlow, missedFlow, investedFlow) { done, missed, investedTimeMs ->
            val total = done.size + missed.size
            MonthAchievementSummary(
                doneCount = done.size,
                totalCount = total,
                completionRate = if (total == 0) 0f else done.size.toFloat() / total,
                activeDays = done.map { it.date }.distinct().size,
                retrospectCount = (done + missed).count { !it.retrospect.isNullOrBlank() },
                investedTimeMs = investedTimeMs,
            )
        }
    }

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

enum class DailyAchievementPageViewMode {
    COLOR_DOT, DESCRIPTION_TEXT
}