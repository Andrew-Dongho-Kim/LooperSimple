package com.pnd.android.loop.ui.home.loop

import androidx.compose.runtime.Stable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.pnd.android.loop.alarm.AlarmController
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toLocalTime
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@Stable
@HiltViewModel
class LoopViewModel @Inject constructor(
    appDb: AppDatabase,
    private val alarmController: AlarmController,
) : ViewModel() {
    private val logger = log("LoopViewModel")

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.e { "coroutine exception is passed: $throwable" }
    }
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineExceptionHandler)
    private val loopDao = appDb.loopDao()
    private val loopDonDao = appDb.loopDoneDao()
    private val loopWithDoneDao = appDb.loopWithDoneDao()

    val localDate = flow {
        while (currentCoroutineContext().isActive) {
            val now = LocalDateTime.now()
            emit(now.toLocalDate())

            val delayInMs = now.toLocalTime().until(LocalTime.MAX, ChronoUnit.MILLIS)
            delay(delayInMs)
        }
    }

    val localDateTime = flow {
        while (currentCoroutineContext().isActive) {
            emit(LocalDateTime.now())
            delay(1000L)
        }
    }

    val loops: LiveData<List<LoopVo>> = loopDao.allLoopsLiveData()

    @OptIn(ExperimentalCoroutinesApi::class)
    val loopsWithDone =
        localDate.flatMapLatest { currDate -> loopWithDoneDao.flowAllLoops(currDate.toLocalTime()) }

    val activeLoops = loops.map { loops -> loops.filter { loop -> loop.isActive() } }

    val countInActive = activeLoops.map { it.size }
    val total = loops.map { loops -> loops.filter { loop -> loop.isActiveDay() }.size }

    fun addOrUpdateLoop(vararg loops: LoopVo) {
        coroutineScope.launch {
            loopDao.addOrUpdate(*loops).forEachIndexed { index, id ->
                val loop = loops[index].copy(id = id.toInt())
                logger.d { "$loop is added or updated" }

                if (loop.enabled) alarmController.reserveAlarm(loop)
                else alarmController.cancelAlarm(loop)
            }
        }
    }

    fun removeLoop(loop: LoopBase) {
        coroutineScope.launch {
            alarmController.cancelAlarm(loop)
            loopDao.remove(loop.id)
        }
    }

    fun doneLoop(
        loop: LoopBase,
        localDate: LocalDate = LocalDate.now(),
        @LoopDoneVo.DoneState doneState: Int
    ) {
        coroutineScope.launch {
            loopDonDao.addOrUpdate(
                loop = loop,
                localDate = localDate,
                doneState = doneState
            )
        }
    }

    fun syncAlarms() = alarmController.syncAlarms()
}