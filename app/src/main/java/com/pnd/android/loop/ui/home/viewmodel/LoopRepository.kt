package com.pnd.android.loop.ui.home.viewmodel

import com.pnd.android.loop.alarm.LoopScheduler
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.data.history.LoopHistory
import com.pnd.android.loop.data.history.LoopHistoryRepository
import com.pnd.android.loop.data.history.LoopMutationStore
import com.pnd.android.loop.data.history.localDate
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.isOvernight
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn

class LoopRepository @Inject constructor(
    appDb: AppDatabase,
    private val loopScheduler: LoopScheduler,
    val historyRepository: LoopHistoryRepository,
    private val mutations: LoopMutationStore,
) {
    private val logger = log("LoopRepository")

    private val loopDao = appDb.loopDao()
    private val coroutineScope = CoroutineScope(SupervisorJob())

    val localDateTime = flow {
        while (true) {
            emit(LocalDateTime.now())
            delay(1000L)
        }
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LocalDateTime.now()
    )

    val localDate = flow {
        while (true) {
            val delayInMs = min(
                LocalTime.now().until(LocalTime.MAX, ChronoUnit.MILLIS),
                60_000L
            )

            logger.i { "delay:${delayInMs.toLocalTime()}" }
            emit(LocalDate.now())
            delay(delayInMs)
        }
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LocalDate.now()
    )

    // Shared so the multiple downstream consumers (UI sections, active/today counts)
    // collect a single DB stream instead of each re-running the query.
    // initialValue = null 은 "아직 DB에서 로딩 전"을 뜻한다. 로딩 완료 후 값이 비어 있으면(진짜
    // 루프 0개) emptyList()가 방출된다. UI는 이 null/empty 구분으로 로딩 중 빈 화면 깜빡임을 막는다.
    val allLoopsWithDoneStates: Flow<List<LoopWithDone>?> = combine(
        historyRepository.snapshots, localDate,
    ) { snapshot, date -> snapshot.timelines.map { it.liveLoop(date) } }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000L), null)

    // 로딩 여부(null)를 신경 쓰지 않는 내부 소비자용 non-null 뷰. 상위 stateIn을 공유하므로
    // 추가 DB 쿼리는 발생하지 않고, 로딩 전(null)에는 아무 값도 흘려보내지 않는다.
    val loadedLoops: Flow<List<LoopWithDone>> = allLoopsWithDoneStates.filterNotNull()

    /**
     * 어제 날짜 행과 조인한 루프 전체.
     *
     * 자정을 넘기는 루프는 done 기록이 "시작한 날"인 어제 행에 있으므로, 오늘 화면에서 그 몫을
     * 다루려면 오늘 행만으로는 부족하다([com.pnd.android.loop.data.TodayOccurrence] 참고).
     */
    val yesterdayLoops: Flow<List<LoopWithDone>> = combine(
        historyRepository.snapshots, localDate,
    ) { snapshot, date -> snapshot.timelines.map { it.liveLoop(date.minusDays(1)) } }
        .stateIn(coroutineScope, SharingStarted.WhileSubscribed(5_000L), emptyList())

    // @formatter:off
    /**
     * 어제 미응답 카드에 올릴 루프.
     *
     * 시간 미지정(anytime) 루프도 포함한다. 어제 시작조차 하지 않았다면 어제 행은 미응답으로
     * 남고 오늘 화면 어디에도 걸치지 않아, 여기서 빼면 답할 자리가 사라지기 때문이다.
     * (어제 시작해 아직 진행 중인 몫은 미응답이 아니라 IN_PROGRESS 이므로 여기 걸리지 않고,
     * 오늘 목록이 어제 행 그대로 정지 버튼과 함께 맡는다.)
     *
     * 자정을 넘기는 루프는 제외한다. 그 어젯밤 몫은 오늘 아침에 끝나 오늘 화면에 그대로 걸치므로,
     * 오늘 목록의 "응답 대기" 항목이 대신 맡는다. 여기까지 넣으면 같은 화면에 두 번 나온다.
     */
    val loopsNoResponseYesterday = yesterdayLoops.map { loops ->
        loops.filter { loop ->
            !loop.isDisabled &&
            !loop.isOvernight &&
            loop.isNotRespond &&
            loop.created.toLocalDate().isBefore(LocalDate.now()) &&
            loop.isActiveDay(LocalDate.now().minusDays(1))
        }
    }
    // @formatter:on

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeLoops = localDateTime.flatMapLatest { now ->
        loadedLoops.map { loops -> loops.filter { loop -> loop.isActive(now) } }
    }.flowOn(Dispatchers.Default)
    val countInActive = activeLoops.map { it.size }

    // countInToday and countInTodayRemain both scan the same list, so compute them in a
    // single pass off the main thread and expose each value as a cheap projection.
    private val todayCounts = loadedLoops.map { loops ->
        var today = 0
        var remain = 0
        loops.forEach { loop ->
            if (loop.isActiveDay()) {
                today++
                if (loop.isNotRespond) remain++
            }
        }
        today to remain
    }.flowOn(Dispatchers.Default)
        .stateIn(
            scope = coroutineScope,
            started = SharingStarted.WhileSubscribed(5_000L),
            initialValue = 0 to 0
        )
    val countInToday = todayCounts.map { it.first }
    val countInTodayRemain = todayCounts.map { it.second }

    internal val settledDays = combine(historyRepository.snapshots, localDate) { snapshot, today ->
        snapshot.settled(snapshot.firstDate ?: today, today, today)
    }.flowOn(Dispatchers.Default)
        .shareIn(coroutineScope, SharingStarted.WhileSubscribed(5_000L), replay = 1)

    val doneDates = settledDays.map { days ->
        days.filter { it.response.isDone() }.map { it.date.toMs() }.distinct()
    }
    internal val todaySettled = combine(settledDays, localDate) { days, today -> days.filter { it.date == today } }

    /** Missing scheduled days and inactive spans use the same timeline as every rate. */
    val allDoneHistory: Flow<Map<Int, Map<Long, Int>>> = combine(
        historyRepository.snapshots, localDate,
    ) { snapshot, today ->
        snapshot.timelines.associate { timeline ->
            val states = mutableMapOf<Long, Int>()
            var date = timeline.createdDate
            while (date <= today) {
                states[date.toMs()] = timeline.responseOn(date)?.done
                    ?: timeline.day(date)?.response?.done
                    ?: if (!timeline.settingsOn(date).enabled) LoopDoneVo.DoneState.DISABLED else HISTORY_NOT_SCHEDULED
                date = date.plusDays(1)
            }
            timeline.current.loopId to states
        }
    }.flowOn(Dispatchers.Default)

    fun syncLoops() = loopScheduler.syncLoops()

    suspend fun numberOfLoopsAtTheSameTime(loop: LoopBase) =
        loopDao.numberOfLoopsAtTheSameTime(another = loop)

    /** 추가/갱신된 루프를 (자동 생성된 loopId가 채워진 상태로) 반환한다. 실행취소 등에서 활용한다. */
    suspend fun addOrUpdateLoop(vararg loops: LoopVo): List<LoopVo> {
        val saved = mutations.saveSettings(loops.toList())
        loopScheduler.syncLoops()
        return saved
    }

    suspend fun deleteLoop(loop: LoopBase): LoopHistory? {
        val deleted = mutations.delete(loop.loopId)
        loopScheduler.cancelAlarm(loop)
        loopScheduler.cancelLoopPrompts(loop.loopId)
        loopScheduler.refreshOngoingNotification()
        return deleted
    }

    suspend fun restoreLoop(deleted: LoopHistory) {
        mutations.restore(deleted)
        loopScheduler.syncLoops()
    }

    suspend fun changeLoopState(
        loop: LoopBase,
        localDate: LocalDate = LocalDate.now(),
        @LoopDoneVo.DoneState doneState: Int,
        suppliedTimes: Pair<Long, Long>? = null,
    ) {
        if (doneState == LoopDoneVo.DoneState.IN_PROGRESS) mutations.start(loop.loopId)
        else mutations.setResponse(loop.loopId, localDate, doneState, suppliedTimes)
        refreshAfterResponse(loop.loopId)
    }

    suspend fun setRecordedState(loopId: Int, date: LocalDate, state: Int) {
        mutations.setResponse(loopId, date, state)
        refreshAfterResponse(loopId)
    }

    private fun refreshAfterResponse(loopId: Int) {
        loopScheduler.refreshOngoingNotification()
        loopScheduler.cancelLoopPrompts(loopId)
    }

    suspend fun editableLoop(loopId: Int): LoopVo? = historyRepository.snapshot().byId[loopId]?.current

    suspend fun setEnabled(loopId: Int, enabled: Boolean) {
        mutations.setEnabled(loopId, enabled)
        loopScheduler.syncLoops()
    }

    suspend fun getMemo(loopId: Int, localDate: LocalDate): LoopRetrospectVo? =
        historyRepository.snapshot().byId[loopId]?.history?.notes?.firstOrNull { it.localDate() == localDate }

    suspend fun saveMemo(loopId: Int, localDate: LocalDate, text: String) =
        mutations.saveNote(loopId, localDate, text)
}
/** Presentation-only value: never persisted in loop_done. */
const val HISTORY_NOT_SCHEDULED = -2
