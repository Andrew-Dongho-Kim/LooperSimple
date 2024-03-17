package com.pnd.android.loop.ui.home.loop.viewmodel

import com.pnd.android.loop.alarm.AlarmController
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toLocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.isActive
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

class LoopRepository @Inject constructor(
    appDb: AppDatabase,
    private val alarmController: AlarmController,
) {
    private val logger = log("LoopRepository")

    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val loopDao = appDb.loopDao()
    private val loopWithDoneDao = appDb.loopWithDoneDao()
    private val loopDoneDao = appDb.loopDoneDao()

    val localDateTime = flow {
        while (currentCoroutineContext().isActive) {
            emit(LocalDateTime.now())
            delay(1000L)
        }
    }

    val localDate = flow {
        while (currentCoroutineContext().isActive) {
            val now = LocalDate.now()
            emit(now)

            val delayInMs = LocalTime.now().until(LocalTime.MAX, ChronoUnit.MILLIS)
            delay(delayInMs)
        }
    }.stateIn(
        initialValue = LocalDate.now(),
        started = SharingStarted.WhileSubscribed(5000),
        scope = coroutineScope
    )

    val loopsWithDoneAll = localDate.transform { currDate ->
        logger.d { "loops with done: $currDate" }
        emitAll(loopWithDoneDao.flowAllLoops(currDate.toLocalTime()))
    }.stateIn(
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(5000),
        scope = coroutineScope
    )


    // @formatter:off
    val loopsNoResponseYesterday = localDate.transform { currDate ->
        emitAll(loopWithDoneDao.flowAllLoops(currDate.minusDays(1).toLocalTime()))
    }.map { loops ->
        loops.filter { loop ->
            loop.isNotRespond &&
            loop.created.toLocalDate().isBefore(LocalDate.now()) &&
            loop.isActiveDay(LocalDate.now().minusDays(1))
        }
    }.stateIn(
        initialValue = emptyList(),
        started = SharingStarted.WhileSubscribed(5000),
        scope = coroutineScope
    )
    // @formatter:on

    @OptIn(ExperimentalCoroutinesApi::class)
    val activeLoops = localDateTime.flatMapLatest { now ->
        loopsWithDoneAll.map { loops -> loops.filter { loop -> loop.isActive(now) } }
    }
    val countInActive = activeLoops.map { it.size }
    val countInTodayRemain = loopsWithDoneAll.map { loops ->
        loops.filter { loop -> loop.isNotRespond && loop.isActiveDay() }.size
    }

    val allCount = loopDoneDao.flowAllCount()
    val allResponseCount = loopDoneDao.flowResponseCount()
    val doneCount = loopDoneDao.flowDoneCount()
    val skipCount = loopDoneDao.flowSkipCount()

    fun syncAlarms() = alarmController.syncAlarms()

    suspend fun maxOfIntersects(loop: LoopBase) = loopDao.maxOfIntersects(loopToCompare = loop)

    suspend fun addOrUpdateLoop(vararg loops: LoopVo) {
        loopDao.addOrUpdate(*loops).forEachIndexed { index, id ->
            val loop = loops[index].copy(id = id)
            logger.d { "$loop is added or updated" }

            if (loop.enabled) alarmController.reserveAlarm(loop)
            else alarmController.cancelAlarm(loop)
        }
    }

    suspend fun removeLoop(loop: LoopBase) {
        alarmController.cancelAlarm(loop)
        loopDao.remove(loop.id)
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
}