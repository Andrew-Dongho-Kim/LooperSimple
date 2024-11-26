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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
    private val alarmController: LoopScheduler,
) {
    private val logger = log("LoopRepository")

    private val loopDao = appDb.loopDao()
    private val loopWithDoneDao = appDb.fullLoopDao()
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

            logger.d { "delay:${delayInMs.toLocalTime()}" }
            emit(LocalDate.now())
            delay(delayInMs)
        }
    }.stateIn(
        scope = coroutineScope,
        started = SharingStarted.WhileSubscribed(5_000L),
        initialValue = LocalDate.now()
    )

    val allLoopsWithDoneStates: Flow<List<LoopWithDone>> = localDate.transform { currDate ->
        emit(loopWithDoneDao.getAllLoops(currDate.toLocalTime()))
        emitAll(loopWithDoneDao.getAllLoopsFlow(currDate.toLocalTime()))
    }

    // @formatter:off
    val loopsNoResponseYesterday = localDate.transform { currDate ->
        emitAll(loopWithDoneDao.getAllLoopsFlow(currDate.minusDays(1).toLocalTime()))
    }.map { loops ->
        loops.filter { loop ->
            !loop.isDisabled &&
            loop.isNotRespond &&
            loop.created.toLocalDate().isBefore(LocalDate.now()) &&
            loop.isActiveDay(LocalDate.now().minusDays(1))
        }
    }
    // @formatter:on

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeLoops = localDateTime.flatMapLatest { now ->
        allLoopsWithDoneStates.map { loops -> loops.filter { loop -> loop.isActive(now) } }
    }
    val countInActive = activeLoops.map { it.size }
    val countInToday = allLoopsWithDoneStates.map { loops ->
        loops.filter { loop -> loop.isActiveDay() }.size
    }
    val countInTodayRemain = allLoopsWithDoneStates.map { loops ->
        loops.filter { loop -> loop.isNotRespond && loop.isActiveDay() }.size
    }

    val allEnabledCount = loopDoneDao.getAllEnabledCountFlow()
    val allRespondCount = loopDoneDao.getRespondCountFlow()
    val doneCount = loopDoneDao.getDoneCountFlow()
    val skipCount = loopDoneDao.getSkipCountFlow()

    fun syncAlarms() = alarmController.syncLoops()

    suspend fun numberOfLoopsAtTheSameTime(loop: LoopBase) =
        loopDao.numberOfLoopsAtTheSameTime(another = loop)

    suspend fun addOrUpdateLoop(vararg loops: LoopVo) {
        loopDao.addOrUpdate(*loops).forEachIndexed { index, id ->
            val loop = loops[index].copy(loopId = id)
            logger.d { "$loop is added or updated" }


            if (loop.isActiveDay() || !loop.enabled) {
                loopDoneDao.addOrUpdate(
                    LoopDoneVo(
                        loopId = loop.loopId,
                        date = LocalDate.now().toMs(),
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
                alarmController.reserveAlarm(scheduleStart(loop))
            } else {
                alarmController.cancelAlarm(loop)
            }
        }
    }

    suspend fun deleteLoop(loop: LoopBase) {
        alarmController.cancelAlarm(loop)
        loopDao.delete(loop.loopId)
    }

    suspend fun doneLoop(
        loop: LoopBase,
        localDate: LocalDate = LocalDate.now(),
        @LoopDoneVo.DoneState doneState: Int
    ) {
        loopDoneDao.addOrUpdate(
            loop = loop,
            localDate = localDate,
            doneState = doneState
        )
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