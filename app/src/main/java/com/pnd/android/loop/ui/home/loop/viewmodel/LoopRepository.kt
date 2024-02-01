package com.pnd.android.loop.ui.home.loop.viewmodel

import com.pnd.android.loop.alarm.AlarmController
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.toLocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
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

    private val loopDao = appDb.loopDao()
    private val loopWithDoneDao = appDb.loopWithDoneDao()
    private val loopDonDao = appDb.loopDoneDao()

    val localDate = flow {
        while (currentCoroutineContext().isActive) {
            val now = LocalDateTime.now()
            emit(now.toLocalDate())

            val delayInMs = now.toLocalTime().until(LocalTime.MAX, ChronoUnit.MILLIS)
            delay(delayInMs)
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val loopsWithDone =
        localDate.flatMapLatest { currDate -> loopWithDoneDao.flowAllLoops(currDate.toLocalTime()) }

    val activeLoops = loopsWithDone.map { loops -> loops.filter { loop -> loop.isActive() } }
    val countInActive = activeLoops.map { it.size }
    val total = loopsWithDone.map { it.size }

    fun syncAlarms() = alarmController.syncAlarms()

    suspend fun addOrUpdateLoop(vararg loops: LoopVo) {
        loopDao.addOrUpdate(*loops).forEachIndexed { index, id ->
            val loop = loops[index].copy(id = id.toInt())
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
        loopDonDao.addOrUpdate(
            loop = loop,
            localDate = localDate,
            doneState = doneState
        )
    }
}