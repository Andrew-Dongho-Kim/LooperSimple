package com.pnd.android.loop.ui.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.map
import androidx.lifecycle.switchMap
import com.pnd.android.loop.alarm.AlarmController
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopFilter
import com.pnd.android.loop.data.LoopFilter.Companion.filter
import com.pnd.android.loop.data.LoopVo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

@HiltViewModel
class LoopViewModel @Inject constructor(
    appDb: AppDatabase,
    private val alarmController: AlarmController,
) : ViewModel() {
    private val logger = log("HomeViewModel")

    private val coroutineScope = CoroutineScope(SupervisorJob())
    private val loopDao = appDb.loopDao()
    private val loopFilterDao = appDb.loopFilterDao()

    val localTime = flow {
        while (currentCoroutineContext().isActive) {
            emit(LocalDate.now())
            delay(1000L)
        }
    }

//    private val _loops = liveData {
//        emitSource(loopDao.getAll())
//    } as MutableLiveData<List<LoopVo>>

    val loopFilter = loopFilterDao.get().map {
        it ?: LoopFilter.DEFAULT
    }

    private val _filteredLoops = MediatorLiveData<List<LoopVo>>().apply {
        var savedFilter: LoopFilter = LoopFilter.DEFAULT
        var savedLoops: List<LoopVo> = emptyList()
        fun result(): List<LoopVo> {
            return savedLoops.filter { loop -> loop.filter(savedFilter) }
        }
        addSource(loopFilter) { filter ->
            savedFilter = filter ?: LoopFilter.DEFAULT
            value = result()
        }
        addSource(loopDao.getAll()) { loops ->
            savedLoops = loops ?: emptyList()
            value = result()
        }
    }

    val loops: LiveData<List<LoopVo>> = _filteredLoops

    private val loopsInProgress = loops.switchMap { loops ->
        liveData {
            emit(loops.filter { it.enabled })
        }
    }

    val countInProgress = loopsInProgress.map {
        logger.d { "loop in progress : ${it.size}" }
        it.size
    }

    val total = loops.map { it.size }

//    fun notifyLoops() {
//        _loops.postValue(_loops.value)
//    }

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

    fun removeLoop(loop:LoopVo) {
        coroutineScope.launch {
            alarmController.cancelAlarm(loop)
            loopDao.remove(loop.id)
        }
    }

    fun syncAlarms() = alarmController.syncAlarms()
}