package com.pnd.android.loop.ui.home.viewmodel

import com.pnd.android.loop.alarm.LoopScheduler
import com.pnd.android.loop.alarm.LoopScheduler.Companion.scheduleStart
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import kotlin.math.min

class LoopRepository @Inject constructor(
    appDb: AppDatabase,
    private val loopScheduler: LoopScheduler,
) {
    private val logger = log("LoopRepository")

    private val loopDao = appDb.loopDao()
    private val fullLoopDao = appDb.fullLoopDao()
    private val loopDoneDao = appDb.loopDoneDao()
    private val loopMemoDao = appDb.loopRetrospectDao()
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
    val allLoopsWithDoneStates: Flow<List<LoopWithDone>> = localDate.transform { currDate ->
        emit(fullLoopDao.getAllLoops(currDate.toLocalTime()))
        emitAll(fullLoopDao.getAllLoopsFlow(currDate.toLocalTime()))
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = emptyList()
    )

    // @formatter:off
    val loopsNoResponseYesterday = localDate.transform { currDate ->
        emitAll(fullLoopDao.getAllLoopsFlow(currDate.minusDays(1).toLocalTime()))
    }.map { loops ->
        loops.filter { loop ->
            !loop.isDisabled &&
            !loop.isAnyTime &&
            loop.isNotRespond &&
            loop.created.toLocalDate().isBefore(LocalDate.now()) &&
            loop.isActiveDay(LocalDate.now().minusDays(1))
        }
    }
    // @formatter:on

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeLoops = localDateTime.flatMapLatest { now ->
        allLoopsWithDoneStates.map { loops -> loops.filter { loop -> loop.isActive(now) } }
    }.flowOn(Dispatchers.Default)
    val countInActive = activeLoops.map { it.size }

    // countInToday and countInTodayRemain both scan the same list, so compute them in a
    // single pass off the main thread and expose each value as a cheap projection.
    private val todayCounts = allLoopsWithDoneStates.map { loops ->
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

    val allEnabledCount = loopDoneDao.getAllEnabledCountFlow()
    val allRespondCount = loopDoneDao.getRespondCountFlow()
    val doneCount = loopDoneDao.getDoneCountFlow()
    val skipCount = loopDoneDao.getSkipCountFlow()

    // 완료(DONE) 기록이 있는 날짜(전체 기간). 헤더의 연속 달성/요일 패턴 계산에 쓰인다.
    val doneDates = fullLoopDao.getDoneDatesFlow()

    // Counts scoped to the current day so the header can show "today's" done rate
    // separately from the all-time figures above. They re-query whenever the day rolls over.
    @OptIn(ExperimentalCoroutinesApi::class)
    val todayEnabledCount = localDate.flatMapLatest { date ->
        loopDoneDao.getEnabledCountByDateFlow(date.toMs())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayDoneCount = localDate.flatMapLatest { date ->
        loopDoneDao.getDoneCountByDateFlow(date.toMs())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todayRespondCount = localDate.flatMapLatest { date ->
        loopDoneDao.getRespondCountByDateFlow(date.toMs())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val todaySkipCount = localDate.flatMapLatest { date ->
        loopDoneDao.getSkipCountByDateFlow(date.toMs())
    }

    fun syncLoops() = loopScheduler.syncLoops()

    suspend fun numberOfLoopsAtTheSameTime(loop: LoopBase) =
        loopDao.numberOfLoopsAtTheSameTime(another = loop)

    suspend fun addOrUpdateLoop(vararg loops: LoopVo) {
        loopDao.addOrUpdate(*loops).forEachIndexed { index, id ->
            val loop = loops[index].copy(loopId = id)
            logger.i { "$loop is added or updated" }


            if (loop.isActiveDay() || !loop.enabled) {
                loopDoneDao.addOrUpdate(
                    LoopDoneVo(
                        loopId = loop.loopId,
                        date = LocalDate.now().toMs(),
                        startInDay = loop.startInDay,
                        endInDay = loop.endInDay,
                        done = if (loop.enabled) {
                            LoopDoneVo.DoneState.NO_RESPONSE
                        } else {
                            LoopDoneVo.DoneState.DISABLED
                        }
                    )
                )
            } else {
                loopDoneDao.delete(
                    loopId = loop.loopId,
                    date = LocalDate.now().toMs()
                )
            }

            if (loop.enabled) {
                loopScheduler.reserveAlarm(scheduleStart(loop))
            } else {
                loopScheduler.cancelAlarm(loop)
            }
        }
    }

    suspend fun deleteLoop(loop: LoopBase) {
        loopScheduler.cancelAlarm(loop)
        loopDao.delete(loop.loopId)
    }

    suspend fun changeLoopState(
        loop: LoopBase,
        localDate: LocalDate = LocalDate.now(),
        @LoopDoneVo.DoneState doneState: Int
    ) {
        loopDoneDao.addOrUpdate(
            loop = loop,
            localDate = localDate,
            doneState = doneState
        )

        // 완료/스킵으로 응답한 순간, 통합 알림을 갱신해 이 루프를 목록에서 뺀다.
        if (doneState == LoopDoneVo.DoneState.DONE || doneState == LoopDoneVo.DoneState.SKIP) {
            loopScheduler.refreshOngoingNotification()
        }
    }

    suspend fun getMemo(
        loopId: Int,
        localDate: LocalDate
    ) = loopMemoDao.getRetrospect(
        loopId = loopId,
        localDate = localDate.toMs(),
    )

    suspend fun saveMemo(
        loopId: Int,
        localDate: LocalDate,
        text: String
    ) = loopMemoDao.insert(
        LoopRetrospectVo(
            loopId = loopId,
            date = localDate.toMs(),
            text = text
        )
    )
}