package com.pnd.android.loop.ui.home.loop.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.map
import com.pnd.android.loop.appwidget.enqueueUpdateWidget
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
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
    private val application: Application,
    private val loopRepository: LoopRepository,
) : ViewModel() {
    private val logger = log("LoopViewModel")

    private val coroutineExceptionHandler = CoroutineExceptionHandler { _, throwable ->
        logger.e { "coroutine exception is passed: $throwable" }
    }
    private val coroutineScope = CoroutineScope(SupervisorJob() + coroutineExceptionHandler)


    val localDate = loopRepository.localDate
    val localDateTime = flow {
        while (currentCoroutineContext().isActive) {
            emit(LocalDateTime.now())
            delay(1000L)
        }
    }

    val loopsWithDone = loopRepository.loopsWithDone
    val countInActive = loopRepository.countInActive
    val total = loopRepository.total


    fun addOrUpdateLoop(vararg loops: LoopVo) {
        coroutineScope.launch {
            loopRepository.addOrUpdateLoop(*loops)
            enqueueUpdateWidget(application)
        }
    }

    fun removeLoop(loop: LoopBase) {
        coroutineScope.launch {
            loopRepository.removeLoop(loop)
            enqueueUpdateWidget(application)
        }
    }

    fun doneLoop(
        loop: LoopBase,
        localDate: LocalDate = LocalDate.now(),
        @LoopDoneVo.DoneState doneState: Int
    ) {
        coroutineScope.launch {
            loopRepository.doneLoop(
                loop = loop,
                localDate = localDate,
                doneState = doneState,
            )
            enqueueUpdateWidget(application)
        }
    }

    fun syncAlarms() {
        loopRepository.syncAlarms()
        enqueueUpdateWidget(application)
    }
}