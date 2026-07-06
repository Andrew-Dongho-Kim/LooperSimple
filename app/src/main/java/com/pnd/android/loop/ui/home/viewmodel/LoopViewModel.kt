package com.pnd.android.loop.ui.home.viewmodel

import android.app.Application
import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.generationConfig
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.TodayLoopOrder
import com.pnd.android.loop.ui.statisctics.DayOfWeekStat
import com.pnd.android.loop.ui.statisctics.StreakStat
import com.pnd.android.loop.ui.statisctics.computeStreak
import com.pnd.android.loop.ui.statisctics.computeWeekdayStats
import com.pnd.android.loop.util.toLocalDate
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
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

    private val generativeModel by lazy {
        GenerativeModel(
            modelName = "gemini-1.0-pro",
            apiKey = GENERATIVE_AI_KEY,
            generationConfig = generationConfig {
                temperature = 0.7f
            }
        )
    }
    private val chat by lazy { generativeModel.startChat() }

    private val _wiseSaying = MutableStateFlow("")
    val wiseSayingText get() = _wiseSaying.value
    val wiseSaying: StateFlow<String> = _wiseSaying

    fun loadWiseSaying() {
        viewModelScope.launch {
//            try {
//                val response =
//                    chat.sendMessage(application.getString(R.string.prompt_for_wise_saying))
//                _wiseSaying.emit(response.text ?: "")
//            } catch (e: ResponseStoppedException) {
//                // don't anything, just catch
//            } catch (e: InvalidStateException) {
//                // don't anything, just catch
//            } catch (e: QuotaExceededException) {
//                // don't anything, just catch
//            }
        }
    }

    val localDate = loopRepository.localDate
    val localDateTime = loopRepository.localDateTime

    val loopsNoResponseYesterday = loopRepository.loopsNoResponseYesterday

    @OptIn(ExperimentalCoroutinesApi::class)
    val allLoopsWithDoneStates = loopRepository.allLoopsWithDoneStates.mapLatest { loops ->
        loops.sortedWith(TodayLoopOrder())
    }

    val countInActive = loopRepository.countInActive
    val countInToday = loopRepository.countInToday
    val countInTodayRemain = loopRepository.countInTodayRemain

    private val allCount = loopRepository.allEnabledCount
    private val allResponseCount = loopRepository.allRespondCount
    private val doneCount = loopRepository.doneCount
    private val skipCount = loopRepository.skipCount

    private val todayCount = loopRepository.todayEnabledCount
    private val todayDoneCount = loopRepository.todayDoneCount
    private val todayResponseCount = loopRepository.todayRespondCount
    private val todaySkipCount = loopRepository.todaySkipCount


    private val _highlightId = MutableStateFlow(NavigatePage.UNKNOWN_ID)
    val highlightId: StateFlow<Int> = _highlightId
    private var resetHighlightJob: Job? = null
    private var savedHighlightId: Int = NavigatePage.UNKNOWN_ID
    private var savedHighlightKey: Int = 0

    fun setHighlightId(id: Int, highlightKey: Int) {
        if (id == savedHighlightId && savedHighlightKey == highlightKey) return

        _highlightId.value = id
        savedHighlightId = id
        savedHighlightKey = highlightKey
        resetHighlightJob?.cancel()
        resetHighlightJob = coroutineScope.launch {
            delay(2_500L)
            _highlightId.value = NavigatePage.UNKNOWN_ID
            resetHighlightJob = null
        }
    }

    /**
     * Done / response / skip rates bundled per scope so the header can swap them as the
     * 오늘 / 전체 tab changes. Rates are percentages (0..100); a scope with no recorded
     * activity yields 0% across the board rather than a misleading 100%.
     */
    val overallRates: Flow<LoopRates> = combine(
        allCount,
        doneCount,
        allResponseCount,
        skipCount,
    ) { total, done, response, skip ->
        LoopRates(
            doneRate = percentOf(done, total),
            responseRate = percentOf(response, total),
            skipRate = percentOf(skip, total),
        )
    }

    val todayRates: Flow<LoopRates> = combine(
        todayCount,
        todayDoneCount,
        todayResponseCount,
        todaySkipCount,
    ) { total, done, response, skip ->
        LoopRates(
            doneRate = percentOf(done, total),
            responseRate = percentOf(response, total),
            skipRate = percentOf(skip, total),
        )
    }

    /**
     * 연속 달성 스트릭(현재·최고). 오늘 탭 헤더는 현재 연속을, 전체 탭 헤더는 최고 연속을
     * 보여준다. 전체 완료 기록을 기준으로 하므로 탭과 무관하게 동일한 값을 공유한다.
     */
    val streak: Flow<StreakStat> = loopRepository.doneDates.map { millis ->
        computeStreak(doneDates = millis.map { it.toLocalDate() })
    }

    /** 전체 탭 헤더의 요일별 달성 패턴(월~일). 전체 완료 기록을 요일로 묶어 계산한다. */
    val weekdayStats: Flow<List<DayOfWeekStat>> = loopRepository.doneDates.map { millis ->
        computeWeekdayStats(doneDates = millis.map { it.toLocalDate() })
    }

    /** 전체 탭 하단 기록 그리드: loopId -> (날짜(ms) -> done 상태). */
    val allDoneHistory: Flow<Map<Int, Map<Long, Int>>> = loopRepository.allDoneHistory

    private fun percentOf(count: Int, total: Int): Float =
        if (total > 0) count.toFloat() / total * 100f else 0f

    override fun onCleared() {
        coroutineScope.cancel()
        super.onCleared()
    }

    suspend fun numberOfLoopsAtTheSameTime(loop: LoopBase) =
        loopRepository.numberOfLoopsAtTheSameTime(loop = loop)

    fun addOrUpdateLoop(vararg loops: LoopVo) {
        coroutineScope.launch {
            loopRepository.addOrUpdateLoop(*loops)
            AppWidgetUpdateWorker.updateWidget(application)
        }
    }

    fun deleteLoop(loop: LoopBase) {
        coroutineScope.launch {
            loopRepository.deleteLoop(loop)
            AppWidgetUpdateWorker.updateWidget(application)
        }
    }

    fun changeLoopState(
        loop: LoopBase,
        localDate: LocalDate = LocalDate.now(),
        @LoopDoneVo.DoneState doneState: Int
    ) {
        coroutineScope.launch {
            loopRepository.changeLoopState(
                loop = loop,
                localDate = localDate,
                doneState = doneState,
            )
            AppWidgetUpdateWorker.updateWidget(application)
        }
    }

    suspend fun getMemo(
        loopId: Int,
        localDate: LocalDate,
    ) = loopRepository.getMemo(
        loopId = loopId,
        localDate = localDate,
    )

    fun saveMemo(
        loopId: Int,
        localDate: LocalDate,
        text: String
    ) {
        coroutineScope.launch {
            loopRepository.saveMemo(
                loopId = loopId,
                localDate = localDate,
                text = text
            )
        }
    }

    fun syncLoops() {
        loopRepository.syncLoops()
        AppWidgetUpdateWorker.updateWidget(application)
    }
}

/**
 * The three headline habit rates for a single scope (today or all-time), each a
 * percentage in 0..100. Grouping them lets the home header show one coherent set that
 * flips wholesale when the 오늘 / 전체 tab changes.
 */
data class LoopRates(
    val doneRate: Float,
    val responseRate: Float,
    val skipRate: Float,
) {
    companion object {
        val Empty = LoopRates(doneRate = 0f, responseRate = 0f, skipRate = 0f)
    }
}