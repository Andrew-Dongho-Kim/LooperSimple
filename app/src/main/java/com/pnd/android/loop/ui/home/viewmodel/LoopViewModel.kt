package com.pnd.android.loop.ui.home.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.TodayLoopOrder
import com.pnd.android.loop.ui.statisctics.DayOfWeekStat
import com.pnd.android.loop.ui.statisctics.StreakStat
import com.pnd.android.loop.ui.statisctics.computeStreak
import com.pnd.android.loop.ui.statisctics.computeWeekdayStats
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val GENERATIVE_AI_KEY = "AIzaSyDBPQuAGgOyw3m03HCpBSonqes-ojqMZ7A"

// 오늘 루프의 "최근 추세" 계산 파라미터.
//  - TREND_WINDOW: 어제 이전에서 최근 몇 개의 활동일 기록을 볼지.
//  - TREND_MIN_RECORDS: 이 개수 미만이면 표본이 부족하다고 보고 추세에서 제외.
//  - TREND_MAX_ITEMS: 각 페이지(잘함/주의)에 최대 몇 개까지 노출할지.
//  - TREND_GOOD_RATE / TREND_BAD_RATE: 잘함/주의로 분류하는 완료율 경계.
private const val TREND_WINDOW = 7
private const val TREND_MIN_RECORDS = 3
private const val TREND_MAX_ITEMS = 3
private const val TREND_GOOD_RATE = 0.6f
private const val TREND_BAD_RATE = 0.5f

// 오늘 탭 헤더 1페이지의 "최근 N일" 잔디 스트립에서 보여줄 날짜 수.
private const val RECENT_DAYS = 7

@Stable
@HiltViewModel
class LoopViewModel @Inject constructor(
    private val application: Application,
    private val loopRepository: LoopRepository,
) : ViewModel() {
    private val logger = log("LoopViewModel")

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.e { "coroutine exception is passed: $throwable" }
    }
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineExceptionHandler)

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.0-pro",
            apiKey = GENERATIVE_AI_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }
    private val chat by lazy { generativeModel.startChat() }

    private val _wiseSaying = MutableStateFlow("")
    val wiseSayingText get() = _wiseSaying.value
    val wiseSaying: StateFlow<String> = _wiseSaying

    fun loadWiseSaying() {
        viewModelScope.launch {
//            try {
//                val response =
//                    chat.sendMessage(application.getString(R.string.prompt_for_wise_saying))
//                _wiseSaying.emit(response.text ?: "")
//            } catch (e: ResponseStoppedException) {
//                // don't anything, just catch
//            } catch (e: InvalidStateException) {
//                // don't anything, just catch
//            } catch (e: QuotaExceededException) {
//                // don't anything, just catch
//            }
        }
    }

    val localDate = loopRepository.localDate
    val localDateTime = loopRepository.localDateTime

    val loopsNoResponseYesterday = loopRepository.loopsNoResponseYesterday

    @OptIn(ExperimentalCoroutinesApi::class)
    val allLoopsWithDoneStates = loopRepository.allLoopsWithDoneStates.mapLatest { loops ->
        loops.sortedWith(TodayLoopOrder())
    }

    val countInActive = loopRepository.countInActive
    val countInToday = loopRepository.countInToday
    val countInTodayRemain = loopRepository.countInTodayRemain

    private val allCount = loopRepository.allEnabledCount
    private val allResponseCount = loopRepository.allRespondCount
    private val doneCount = loopRepository.doneCount
    private val skipCount = loopRepository.skipCount

    private val todayCount = loopRepository.todayEnabledCount
    private val todayDoneCount = loopRepository.todayDoneCount
    private val todayResponseCount = loopRepository.todayRespondCount
    private val todaySkipCount = loopRepository.todaySkipCount


    private val _highlightId = MutableStateFlow(NavigatePage.UNKNOWN_ID)
    val highlightId: StateFlow<Int> = _highlightId
    private var resetHighlightJob: Job? = null
    private var savedHighlightId: Int = NavigatePage.UNKNOWN_ID
    private var savedHighlightKey: Int = 0

    fun setHighlightId(id: Int, highlightKey: Int) {
        if (id == savedHighlightId && savedHighlightKey == highlightKey) return

        _highlightId.value = id
        savedHighlightId = id
        savedHighlightKey = highlightKey
        resetHighlightJob?.cancel()
        resetHighlightJob = coroutineScope.launch {
            delay(2_500L)
            _highlightId.value = NavigatePage.UNKNOWN_ID
            resetHighlightJob = null
        }
    }

    /**
     * Done / response / skip rates bundled per scope so the header can swap them as the
     * 오늘 / 전체 tab changes. Rates are percentages (0..100); a scope with no recorded
     * activity yields 0% across the board rather than a misleading 100%.
     */
    val overallRates: Flow<LoopRates> = combine(
        allCount,
        doneCount,
        allResponseCount,
        skipCount,
    ) { total, done, response, skip ->
        LoopRates(
            doneRate = percentOf(done, total),
            responseRate = percentOf(response, total),
            skipRate = percentOf(skip, total),
            doneCount = done,
            totalCount = total,
        )
    }

    val todayRates: Flow<LoopRates> = combine(
        todayCount,
        todayDoneCount,
        todayResponseCount,
        todaySkipCount,
    ) { total, done, response, skip ->
        LoopRates(
            doneRate = percentOf(done, total),
            responseRate = percentOf(response, total),
            skipRate = percentOf(skip, total),
            doneCount = done,
            totalCount = total,
        )
    }

    /**
     * 오늘 아직 시작하지 않은 루프 중 시작이 가장 가까운 하나. 오늘 탭 헤더의 "다음 루프"에 쓰인다.
     * 현재 시각(localDateTime)은 매초 갱신되지만 남은 시간을 '분' 단위로 내린 뒤 distinctUntilChanged로
     * 걸러, 실제로 분이 바뀔 때만 아래로 흘려보낸다(초당 리컴포지션 방지).
     * 시작 시각이 없는 anytime 루프와 오늘 활동일이 아닌 루프는 후보에서 제외한다.
     */
    val nextLoop: Flow<NextLoopInfo?> = combine(
        loopRepository.allLoopsWithDoneStates,
        localDateTime,
    ) { loops, now ->
        val nowInDayMs = now.toLocalTime().toMs()
        loops
            .filter { loop ->
                loop.enabled &&
                        !loop.isMock &&
                        !loop.isAnyTime &&
                        loop.isActiveDay(now.toLocalDate()) &&
                        loop.startInDay > nowInDayMs
            }
            .minByOrNull { loop -> loop.startInDay }
            ?.let { loop ->
                NextLoopInfo(
                    title = loop.title,
                    remainingMinutes = (loop.startInDay - nowInDayMs) / MS_1MIN,
                )
            }
    }.distinctUntilChanged()

    /**
     * 지금 실제로 진행 중인 루프 중 가장 먼저 끝나는 하나. 오늘 탭 헤더 1페이지 하단 줄에 쓰인다.
     * "진행 중"은 활동 요일이면서 활동 시간대 안(isActive)인 시간제 루프를 뜻한다. anytime 루프는
     * 종료 시각이 없어 "남은 시간"을 셀 수 없으므로 제외한다. 함께 진행 중인 다른 루프 수는
     * [CurrentLoopInfo.othersCount]("외 N개")로 전달한다.
     * 남은 시간은 '분'으로 내린 뒤 distinctUntilChanged로 걸러, 분이 바뀔 때만 아래로 흘려보낸다.
     */
    val currentLoop: Flow<CurrentLoopInfo?> = combine(
        loopRepository.allLoopsWithDoneStates,
        localDateTime,
    ) { loops, now ->
        val nowInDayMs = now.toLocalTime().toMs()
        val active = loops
            .filter { loop -> !loop.isMock && !loop.isAnyTime && loop.isActive(now) }
            .map { loop -> loop to remainingUntilEnd(loop, nowInDayMs) }
            .sortedBy { (_, remainingMs) -> remainingMs }

        active.firstOrNull()?.let { (loop, remainingMs) ->
            CurrentLoopInfo(
                title = loop.title,
                remainingMinutes = (remainingMs / MS_1MIN).coerceAtLeast(0L),
                othersCount = active.size - 1,
            )
        }
    }.distinctUntilChanged()

    /**
     * 최근 [RECENT_DAYS]일 각 날짜에 완료(DONE)한 루프가 하나라도 있었는지. 과거→오늘 순서의
     * 불리언 리스트로, 오늘 탭 헤더 1페이지의 잔디 스트립에 쓰인다.
     */
    val recentDailyDone: Flow<List<Boolean>> = combine(
        loopRepository.doneDates,
        localDate,
    ) { doneMillis, today ->
        val doneDates = doneMillis.map { it.toLocalDate() }.toHashSet()
        (RECENT_DAYS - 1 downTo 0).map { offset ->
            today.minusDays(offset.toLong()) in doneDates
        }
    }.distinctUntilChanged()

    /**
     * 오늘 수행할 루프 각각의 "최근 추세". 오늘 탭 헤더 2·3페이지(잘하고 있는/주의가 필요한 루프)에 쓴다.
     * 오늘 기록은 아직 수행 전일 수 있어 제외하고, 어제까지의 최근 [TREND_WINDOW]개 활동일 기록만으로
     * 완료율·연속을 계산한다. 기록이 부족한 루프([TREND_MIN_RECORDS] 미만)는 후보에서 뺀다.
     */
    val todayLoopTrends: Flow<TodayLoopTrends> = combine(
        loopRepository.allLoopsWithDoneStates,
        loopRepository.allDoneHistory,
        localDate,
    ) { loops, history, today ->
        val todayMs = today.toMs()
        val trends = loops
            .filter { loop -> loop.enabled && !loop.isMock && loop.isActiveDay(today) }
            .mapNotNull { loop ->
                computeLoopTrend(
                    loopId = loop.loopId,
                    title = loop.title,
                    history = history[loop.loopId],
                    todayMs = todayMs,
                )
            }

        TodayLoopTrends(
            // 잘함: 완료율이 높은 순, 같으면 연속 완료가 긴 순.
            doingWell = trends
                .filter { trend -> trend.doneRate >= TREND_GOOD_RATE }
                .sortedWith(
                    compareByDescending<LoopTrend> { it.doneRate }.thenByDescending { it.currentStreak }
                )
                .take(TREND_MAX_ITEMS),
            // 주의: 완료율이 낮은 순, 같으면 연속 놓침이 긴 순.
            needAttention = trends
                .filter { trend -> trend.doneRate <= TREND_BAD_RATE }
                .sortedWith(
                    compareBy<LoopTrend> { it.doneRate }.thenByDescending { it.currentMiss }
                )
                .take(TREND_MAX_ITEMS),
        )
    }.distinctUntilChanged()

    /**
     * 연속 달성 스트릭(현재·최고). 오늘 탭 헤더는 현재 연속을, 전체 탭 헤더는 최고 연속을
     * 보여준다. 전체 완료 기록을 기준으로 하므로 탭과 무관하게 동일한 값을 공유한다.
     */
    val streak: Flow<StreakStat> = loopRepository.doneDates.map { millis ->
        computeStreak(doneDates = millis.map { it.toLocalDate() })
    }

    /** 전체 탭 헤더의 요일별 달성 패턴(월~일). 전체 완료 기록을 요일로 묶어 계산한다. */
    val weekdayStats: Flow<List<DayOfWeekStat>> = loopRepository.doneDates.map { millis ->
        computeWeekdayStats(doneDates = millis.map { it.toLocalDate() })
    }

    /** 전체 탭 하단 기록 그리드: loopId -> (날짜(ms) -> done 상태). */
    val allDoneHistory: Flow<Map<Int, Map<Long, Int>>> = loopRepository.allDoneHistory

    private fun percentOf(count: Int, total: Int): Float =
        if (total > 0) count.toFloat() / total * 100f else 0f

    override fun onCleared() {
        coroutineScope.cancel()
        super.onCleared()
    }

    suspend fun numberOfLoopsAtTheSameTime(loop: LoopBase) =
        loopRepository.numberOfLoopsAtTheSameTime(loop = loop)

    fun addOrUpdateLoop(vararg loops: LoopVo) {
        coroutineScope.launch {
            loopRepository.addOrUpdateLoop(*loops)
            AppWidgetUpdateWorker.updateWidget(application)
        }
    }

    /**
     * 단일 루프를 추가하고, 자동 생성된 loopId가 채워진 루프를 반환한다. 삽입은 뷰모델 스코프에서
     * 수행하므로 호출한 UI 코루틴이 취소돼도 삽입은 유실되지 않는다. (빠른 시작 추가의 실행취소용)
     */
    suspend fun addLoopReturning(loop: LoopVo): LoopBase =
        coroutineScope.async {
            val added = loopRepository.addOrUpdateLoop(loop).first()
            AppWidgetUpdateWorker.updateWidget(application)
            added
        }.await()

    fun deleteLoop(loop: LoopBase) {
        coroutineScope.launch {
            loopRepository.deleteLoop(loop)
            AppWidgetUpdateWorker.updateWidget(application)
        }
    }

    fun changeLoopState(
        loop: LoopBase,
        localDate: LocalDate = LocalDate.now(),
        @LoopDoneVo.DoneState doneState: Int
    ) {
        coroutineScope.launch {
            loopRepository.changeLoopState(
                loop = loop,
                localDate = localDate,
                doneState = doneState,
            )
            AppWidgetUpdateWorker.updateWidget(application)
        }
    }

    suspend fun getMemo(
        loopId: Int,
        localDate: LocalDate,
    ) = loopRepository.getMemo(
        loopId = loopId,
        localDate = localDate,
    )

    fun saveMemo(
        loopId: Int,
        localDate: LocalDate,
        text: String
    ) {
        coroutineScope.launch {
            loopRepository.saveMemo(
                loopId = loopId,
                localDate = localDate,
                text = text
            )
        }
    }

    fun syncLoops() {
        loopRepository.syncLoops()
        AppWidgetUpdateWorker.updateWidget(application)
    }
}

/**
 * The three headline habit rates for a single scope (today or all-time), each a
 * percentage in 0..100. Grouping them lets the home header show one coherent set that
 * flips wholesale when the 오늘 / 전체 tab changes.
 */
data class LoopRates(
    val doneRate: Float,
    val responseRate: Float,
    val skipRate: Float,
    val doneCount: Int,
    val totalCount: Int,
) {
    companion object {
        val Empty = LoopRates(
            doneRate = 0f,
            responseRate = 0f,
            skipRate = 0f,
            doneCount = 0,
            totalCount = 0,
        )
    }
}

/**
 * 오늘 탭 헤더의 "다음 루프" 표시용. [remainingMinutes]가 0이면 1분 미만 남은 "곧 시작"을 뜻한다.
 */
data class NextLoopInfo(
    val title: String,
    val remainingMinutes: Long,
)

/**
 * 오늘 탭 헤더 1페이지 하단의 "진행 중" 표시용. [remainingMinutes]는 종료까지 남은 분(0이면 곧 종료),
 * [othersCount]는 함께 진행 중인 다른 루프 수("외 N개")다.
 */
data class CurrentLoopInfo(
    val title: String,
    val remainingMinutes: Long,
    val othersCount: Int,
)

/**
 * 한 루프의 최근 수행 추세. [recentDoneFlags]는 최신→과거 순의 완료 여부이며(막대/점 표시에 사용),
 * 완료율([doneRate])과 최근부터 이어지는 연속 완료·연속 놓침을 함께 담는다.
 */
data class LoopTrend(
    val loopId: Int,
    val title: String,
    val recentDoneFlags: List<Boolean>,
    val doneCount: Int,
    val totalCount: Int,
    val currentStreak: Int,
    val currentMiss: Int,
) {
    val doneRate: Float get() = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
}

/**
 * 오늘 탭 헤더의 추세 페이지 묶음. [doingWell]은 최근 잘 지키는 루프, [needAttention]은 최근
 * 놓치고 있는 루프. 각 리스트는 표시 상한만큼 이미 잘려 있다.
 */
data class TodayLoopTrends(
    val doingWell: List<LoopTrend>,
    val needAttention: List<LoopTrend>,
) {
    companion object {
        val Empty = TodayLoopTrends(doingWell = emptyList(), needAttention = emptyList())
    }
}

/**
 * [history](날짜(ms)→완료상태)로 한 루프의 추세를 만든다. 오늘([todayMs]) 기록은 아직 미수행일 수
 * 있어 빼고, 루프가 활동하지 않은 날(DISABLED)도 빼서, 어제 이전의 최근 [TREND_WINDOW]개 활동일만
 * 최신순으로 본다. 활동일 기록이 [TREND_MIN_RECORDS] 미만이면 표본 부족으로 null을 돌려준다.
 *
 * DISABLED 날을 포함하면 그날이 완료가 아니라는 이유로 연속 달성이 끊기거나, 최근 비활동일이
 * "연속 놓침"으로 잘못 잡히므로 반드시 제외한다.
 */
/**
 * 진행 중인 루프가 끝날 때까지 남은 시간(ms). 자정을 넘기는 루프(종료<시작)도 올바르게 계산한다.
 * 아직 자정 전(now가 시작 이후)이면 종료 시각에 하루를 더해 남은 시간을 잰다.
 */
private fun remainingUntilEnd(loop: LoopBase, nowInDayMs: Long): Long {
    val crossesMidnight = loop.startInDay > loop.endInDay
    val endMs = if (crossesMidnight && nowInDayMs >= loop.startInDay) {
        loop.endInDay + MS_1DAY
    } else {
        loop.endInDay
    }
    return endMs - nowInDayMs
}

private fun computeLoopTrend(
    loopId: Int,
    title: String,
    history: Map<Long, Int>?,
    todayMs: Long,
): LoopTrend? {
    if (history == null) return null

    val recentDoneFlags = history
        .filterKeys { date -> date < todayMs }
        .filterValues { state -> state != LoopDoneVo.DoneState.DISABLED }
        .entries
        .sortedByDescending { entry -> entry.key }
        .take(TREND_WINDOW)
        .map { entry -> entry.value == LoopDoneVo.DoneState.DONE }

    if (recentDoneFlags.size < TREND_MIN_RECORDS) return null

    return LoopTrend(
        loopId = loopId,
        title = title,
        recentDoneFlags = recentDoneFlags,
        doneCount = recentDoneFlags.count { done -> done },
        totalCount = recentDoneFlags.size,
        currentStreak = recentDoneFlags.takeWhile { done -> done }.size,
        currentMiss = recentDoneFlags.takeWhile { done -> !done }.size,
    )
}