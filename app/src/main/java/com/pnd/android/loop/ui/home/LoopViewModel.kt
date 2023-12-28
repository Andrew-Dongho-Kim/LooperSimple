package com.pnd.android.loop.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.pnd.android.loop.alarm.AlarmController
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopFilter
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit
import javax.inject.Inject

@HiltViewModel
class LoopViewModel @Inject constructor(
    appDb: AppDatabase,
    private val alarmController: AlarmController,
) : ViewModel() {
    private val logger = log("LoopViewModel")

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val loopDao = appDb.loopDao()
    private val loopFilterDao = appDb.loopFilterDao()

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

    val loops: LiveData<List<LoopVo>> = loopDao.getAll()

    val countInActive = loops.map { loops -> loops.filter { loop -> loop.isActive() }.size }
    val total = loops.map { loops -> loops.filter { loop -> loop.isActiveDay() }.size  }

    fun saveFilter(loopFilter: LoopFilter) {
        coroutineScope.launch { loopFilterDao.update(loopFilter) }
    }

    fun addLoop(vararg loops: LoopVo) {
        coroutineScope.launch {
            logger.d { "Add loop" }
            loops.forEach { logger.d { " - $it" } }

            loopDao.add(*loops).forEachIndexed { index, id ->
                logger.d { "id of loop for $index is updated to $id" }

                val loop = loops[index]
                if (loop.enabled) alarmController.reserveAlarm(loop)
                else alarmController.cancelAlarm(loop)
            }
        }
    }

    fun removeLoop(loop: LoopVo) {
        coroutineScope.launch {
            alarmController.cancelAlarm(loop)
            loopDao.remove(loop.id)
        }
    }

    fun syncAlarms() = alarmController.syncAlarms()
}