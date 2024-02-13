package com.pnd.android.loop.ui.home.loop.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.util.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import kotlin.math.roundToInt

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
    val localDateTime = loopRepository.localDateTime

    val loopsNoResponseYesterday = loopRepository.loopsNoResponseYesterday
    val loopsWithDoneAll = loopRepository.loopsWithDoneAll

    val countInActive = loopRepository.countInActive
    val countInTodayRemain = loopRepository.countInTodayRemain

    private val allCount = loopsWithDoneAll.map { loops ->
        val now = LocalDate.now()
        var count = 0L
        loops.forEach { loop ->
            count += (now.toEpochDay() - loop.created.toLocalDate().toEpochDay())
        }
        count
    }
    private val allResponseCount = loopRepository.allResponseCount

    @OptIn(ExperimentalCoroutinesApi::class)
    val allResponseRate = allCount.flatMapLatest { all ->
        allResponseCount.map { response ->
            (response.toFloat() / all.toFloat() * 100)
        }
    }

    fun addOrUpdateLoop(vararg loops: LoopVo) {
        coroutineScope.launch {
            loopRepository.addOrUpdateLoop(*loops)
            AppWidgetUpdateWorker.updateWidget(application)
        }
    }

    fun removeLoop(loop: LoopBase) {
        coroutineScope.launch {
            loopRepository.removeLoop(loop)
            AppWidgetUpdateWorker.updateWidget(application)
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
            AppWidgetUpdateWorker.updateWidget(application)
        }
    }

    fun syncAlarms() {
        loopRepository.syncAlarms()
        AppWidgetUpdateWorker.updateWidget(application)
    }
}