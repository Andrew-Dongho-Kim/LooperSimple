package com.pnd.android.loop.ui.home.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopWithDone
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

// 루프의 "최근 추세" 계산 파라미터.
//  - TREND_WINDOW: 어제 이전에서 최근 몇 개의 활동일을 볼지.
//  - TREND_SCAN_DAYS: 그 활동일을 찾기 위해 달력을 거슬러 볼 최대 일수. 활동 요일이 드문 루프
//    (예: 주 1회)도 표본을 채울 수 있도록 창보다 넉넉히 둔다.
//  - TREND_MIN_RECORDS: 이 개수 미만이면 표본이 부족하다고 보고 추세에서 제외.
//  - TREND_MAX_ITEMS: 각 페이지(잘함/주의)에 최대 몇 개까지 노출할지.
//  - TREND_GOOD_RATE / TREND_BAD_RATE: 잘함/주의로 분류하는 완료율 경계.
private const val TREND_WINDOW = 7
private const val TREND_SCAN_DAYS = 90
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

    /** 어제 날짜 행과 조인한 루프. 오늘 목록이 자정을 넘기는 루프의 어젯밤 몫을 만들 때 쓴다. */
    val yesterdayLoops = loopRepository.yesterdayLoops

    // null = 아직 DB 로딩 전. UI는 이 값이 null인 동안 빈 화면(EmptyLoops)을 그리지 않는다.
    //
    // StateFlow로 노출하는 것이 중요하다. 콜드 플로우로 두면 홈으로 복귀할 때마다 collectAsState가
    // null부터 다시 시작해, 저장소에 이미 값이 있는데도 첫 프레임이 "로딩 중"으로 그려지고 오늘/전체
    // 탭이 한 박자 늦게 등장한다. StateFlow면 첫 컴포지션에서 캐시된 값을 그대로 읽는다.
    // 정렬은 메인 스레드를 피해 기본 디스패처에서 수행한다.
    val allLoopsWithDoneStates: StateFlow<List<LoopWithDone>?> =
        loopRepository.allLoopsWithDoneStates
            .map { loops -> loops?.sortedWith(TodayLoopOrder()) }
            .flowOn(Dispatchers.Default)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000L),
                initialValue = null,
            )

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
            delay(2_500L.milliseconds)
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
        loopRepository.loadedLoops,
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
        loopRepository.loadedLoops,
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
     * 각 루프의 "최근 추세". 전체 탭 헤더 2·3페이지(잘 지키는/놓치고 있는 루프)에 쓴다.
     * 오늘 기록은 아직 수행 전일 수 있어 제외하고, 어제까지의 최근 [TREND_WINDOW]개 활동일만으로
     * 완료율·연속을 계산한다([computeLoopTrend]). 표본이 부족한 루프([TREND_MIN_RECORDS] 미만)는
     * 후보에서 뺀다.
     */
    val loopTrends: Flow<LoopTrends> = combine(
        loopRepository.loadedLoops,
        loopRepository.allDoneHistory,
        localDate,
    ) { loops, history, today ->
        val trends = loops
            .filter { loop -> loop.enabled && !loop.isMock }
            .mapNotNull { loop ->
                computeLoopTrend(
                    loop = loop,
                    history = history[loop.loopId],
                    today = today,
                )
            }

        LoopTrends(
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
            analyzedCount = trends.size,
        )
        // 루프마다 달력을 최대 [TREND_SCAN_DAYS]일 거슬러 훑으므로 메인 스레드에서 비켜 계산한다.
    }.flowOn(Dispatchers.Default).distinctUntilChanged()

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
 * 한 루프의 최근 수행 추세.
 *
 * [recentStates]는 최신→과거 순으로 이 루프의 활동일 하나당 한 칸씩 담은 상태
 * ([LoopDoneVo.DoneState]의 DONE / SKIP / NO_RESPONSE)다. 활동 요일이 아닌 날과 루프가 꺼져 있던 날은
 * 애초에 수행 대상이 아니므로 칸이 만들어지지 않는다.
 *
 * 건너뜀(SKIP)은 의도적인 응답이므로 [currentStreak](연속 완료)을 끊기는 하지만
 * [currentMiss](연속 놓침)로 세지는 않는다. 다만 완료한 날은 아니므로 완료율([doneRate])의 분모에는 든다.
 */
data class LoopTrend(
    val loopId: Int,
    val title: String,
    val recentStates: List<Int>,
    val doneCount: Int,
    val totalCount: Int,
    val currentStreak: Int,
    val currentMiss: Int,
) {
    val doneRate: Float get() = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
}

/**
 * 추세 페이지 묶음. [doingWell]은 최근 잘 지키는 루프, [needAttention]은 최근
 * 놓치고 있는 루프. 각 리스트는 표시 상한만큼 이미 잘려 있다.
 *
 * 두 목록은 완료율 경계(TREND_GOOD_RATE / TREND_BAD_RATE)로 나뉘므로, 기록이 넉넉해도 한쪽이 빌 수 있다.
 * (예: 모든 루프를 잘 지키고 있으면 [needAttention]이 빈다.) 빈 목록을 "기록 부족"으로 오해하지 않도록
 * 실제로 추세를 낸 루프 수를 [analyzedCount]로 함께 전달한다.
 */
data class LoopTrends(
    val doingWell: List<LoopTrend>,
    val needAttention: List<LoopTrend>,
    val analyzedCount: Int,
) {
    companion object {
        val Empty = LoopTrends(
            doingWell = emptyList(),
            needAttention = emptyList(),
            analyzedCount = 0,
        )
    }
}

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

/**
 * [history](날짜(ms)→상태)로 한 루프의 추세를 만든다.
 *
 * history 에는 DONE/SKIP/DISABLED 행만 담겨 있고 미응답한 날은 키가 아예 없다
 * ([com.pnd.android.loop.data.dao.LoopDoneDao.getAllHistoryFlow] 참고). 그래서 map 의 entry 를 훑으면
 * "놓친 날"이 존재하지 않는 날처럼 사라져, 끊긴 연속 완료가 이어져 보이고(예: 완료→3일 놓침→완료→완료가
 * 3연속 완료로 집계) 연속 놓침은 사실상 연속 건너뜀만 세게 된다. 완료율의 분모도 응답한 날만 세어
 * 부풀려진다. 그래서 map 이 아니라 **달력**을 기준으로 훑는다: 어제부터 과거로 내려가며 이 루프의
 * 활동일만 한 칸씩 쌓고, 그 날 상태를 map 에서 조회해 기록이 없으면 미응답(놓침)으로 본다.
 *
 * 표본에서 빼는 날:
 * - 오늘 — 아직 수행 전일 수 있다.
 * - 생성일과 그 이전 — 생성 당일은 이미 시간 창이 지난 뒤일 수 있어 무조건 놓침이 된다.
 * - 활동 요일이 아닌 날, 루프가 꺼져 있던 날(DISABLED) — 수행 대상이 아니었으므로 칸을 만들지 않고,
 *   따라서 연속을 끊지도 않는다.
 *
 * 활동일 표본이 [TREND_MIN_RECORDS] 미만이면 표본 부족으로 null 을 돌려준다.
 */
private fun computeLoopTrend(
    loop: LoopBase,
    history: Map<Long, Int>?,
    today: LocalDate,
): LoopTrend? {
    val createdDate = loop.created.toLocalDate()
    val recentStates = ArrayList<Int>(TREND_WINDOW)

    var date = today.minusDays(1L)
    var scanned = 0
    while (recentStates.size < TREND_WINDOW &&
        scanned < TREND_SCAN_DAYS &&
        date.isAfter(createdDate)
    ) {
        scanned++
        if (loop.isActiveDay(date)) {
            val state = history?.get(date.toMs())
            // 기록이 없으면(=NO_RESPONSE 행은 저장되지 않는다) 그 날은 놓친 날이다.
            if (state != LoopDoneVo.DoneState.DISABLED) {
                recentStates.add(state ?: LoopDoneVo.DoneState.NO_RESPONSE)
            }
        }
        date = date.minusDays(1L)
    }

    if (recentStates.size < TREND_MIN_RECORDS) return null

    return LoopTrend(
        loopId = loop.loopId,
        title = loop.title,
        recentStates = recentStates,
        doneCount = recentStates.count { state -> state == LoopDoneVo.DoneState.DONE },
        totalCount = recentStates.size,
        currentStreak = recentStates
            .takeWhile { state -> state == LoopDoneVo.DoneState.DONE }
            .size,
        // 건너뜀은 응답한 날이므로 놓침으로 세지 않는다(연속 놓침을 끊는다).
        currentMiss = recentStates
            .takeWhile { state -> state == LoopDoneVo.DoneState.NO_RESPONSE }
            .size,
    )
}