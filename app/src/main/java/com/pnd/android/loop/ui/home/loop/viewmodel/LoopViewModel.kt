package com.pnd.android.loop.ui.home.loop.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.ResponseStoppedException
import com.google.ai.client.generativeai.type.generationConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val GENERATIVE_AI_KEY = "AIzaSyDBPQuAGgOyw3m03HCpBSonqes-ojqMZ7A"

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

    private val generativeModel = GenerativeModel(
        modelName = "gemini-1.0-pro",
        apiKey = GENERATIVE_AI_KEY,
        generationConfig = generationConfig {
            temperature = 0.7f
        }
    )
    private val chat = generativeModel.startChat()

    private val _wiseSaying = MutableStateFlow("")
    val wiseSaying: StateFlow<String> = _wiseSaying

    fun loadWiseSaying() {
        viewModelScope.launch {
            try {
                val response =
                    chat.sendMessage(application.getString(R.string.prompt_for_wise_saying))
                _wiseSaying.emit(response.text ?: "")
            } catch (e: ResponseStoppedException) {
                // don't anything, just catch
            }
        }
    }

    val localDate = loopRepository.localDate
    val localDateTime = loopRepository.localDateTime

    val loopsNoResponseYesterday = loopRepository.loopsNoResponseYesterday
    val loopsWithDoneAll = loopRepository.loopsWithDoneAll

    val countInActive = loopRepository.countInActive
    val countInTodayRemain = loopRepository.countInTodayRemain

    private val allCount = loopRepository.allEnabledCount
    private val allResponseCount = loopRepository.allRespondCount
    private val doneCount = loopRepository.doneCount
    private val skipCount = loopRepository.skipCount

    @OptIn(ExperimentalCoroutinesApi::class)
    val allResponseRate = allCount.flatMapLatest { all ->
        allResponseCount.map { response ->
            if (all > 0) (response.toFloat() / all.toFloat() * 100) else 100
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val doneRate = allCount.flatMapLatest { all ->
        doneCount.map { doneCount ->
            if (all > 0) (doneCount.toFloat() / all.toFloat() * 100) else 100
        }
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val skipRate = allCount.flatMapLatest { all ->
        skipCount.map { skipCount ->
            if (all > 0) (skipCount.toFloat() / all.toFloat() * 100) else 100
        }
    }

    suspend fun maxOfIntersects(loop: LoopBase) =
        loopRepository.maxOfIntersects(loop = loop)

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