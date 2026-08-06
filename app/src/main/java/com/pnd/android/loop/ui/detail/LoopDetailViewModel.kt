package com.pnd.android.loop.ui.detail

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.ui.home.viewmodel.LoopRepository
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

private const val PAGE_SIZE = 150

@HiltViewModel
class LoopDetailViewModel @Inject constructor(
    private val app: Application,
    appDb: AppDatabase,
    savedStateHandle: SavedStateHandle,
    private val loopRepository: LoopRepository,
) : ViewModel() {

    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val loopId: Int = savedStateHandle[NavigatePage.ARGS_ID] ?: -1

    private val loopDao = appDb.loopDao()
    private val loopDoneDao = appDb.loopDoneDao()
    private val loopRetrospectDao = appDb.loopRetrospectDao()

    // 루프를 삭제하면 Room 이 이 자리에 null 을 흘려보낸다. 화면이 닫히는 몇 프레임 동안
    // 마지막으로 유효했던 값을 그대로 쓰도록 걸러 내, 삭제 직후 NPE 가 나지 않게 한다.
    val loop = loopDao.getLoopFlow(loopId).mapNotNull { it }

    // 이 루프에 남긴 회고 메모 전체. 달력에서 "메모가 있는 날"에 마커를 찍는 데 사용한다.
    val retrospects = loopRetrospectDao.getRetrospectsFlow(loopId)

    val allEnabledCount = loopDoneDao.getAllEnabledCountFlow(loopId)
    val respondCount = loopDoneDao.getRespondCountFlow(loopId)
    val doneCount = loopDoneDao.getDoneCountFlow(loopId)
    val skipCount = loopDoneDao.getSkipCountFlow(loopId)

    val allResponses = loopDoneDao.getAllFlow(loopId);
    val donePager = Pager(
        PagingConfig(pageSize = PAGE_SIZE),
        pagingSourceFactory = {
            DonePagingSource(
                appDb = appDb,
                loopId = loopId,
                pageSize = PAGE_SIZE,
            )
        }
    ).flow

    /** 선택한 날짜에 남긴 회고 메모 본문. 없으면 null. */
    suspend fun retrospectOf(date: LocalDate): String? =
        loopRetrospectDao.getRetrospect(loopId = loopId, localDate = date.toMs())?.text

    /**
     * 선택한 날짜의 회고 메모를 저장한다. 내용이 비어 있으면 null 로 지워, 달력의 메모 마커도
     * 함께 사라지게 한다.
     */
    fun saveRetrospect(date: LocalDate, text: String) {
        coroutineScope.launch {
            loopRetrospectDao.insert(
                LoopRetrospectVo(
                    loopId = loopId,
                    date = date.toMs(),
                    text = text.ifBlank { null },
                )
            )
        }
    }

    fun doneLoop(
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
            AppWidgetUpdateWorker.updateWidget(app)
        }
    }

    fun enableLoop(
        loop: LoopBase,
        enabled: Boolean
    ) {
        coroutineScope.launch {
            loopRepository.addOrUpdateLoop(loop.copyAs(enabled = enabled).asLoopVo())
            AppWidgetUpdateWorker.updateWidget(app)
        }
    }

    /** 상세 화면에서 인라인으로 고친 이름·시간 등을 저장한다. */
    fun updateLoop(loop: LoopBase) {
        coroutineScope.launch {
            loopRepository.addOrUpdateLoop(loop.asLoopVo())
            AppWidgetUpdateWorker.updateWidget(app)
        }
    }

    /** 시간대를 바꿔 저장하기 전, 같은 시간에 겹치는 루프가 몇 개인지 확인한다. */
    suspend fun numberOfLoopsAtTheSameTime(loop: LoopBase) =
        loopRepository.numberOfLoopsAtTheSameTime(loop = loop)

    fun deleteLoop(loop: LoopBase) {
        coroutineScope.launch {
            loopRepository.deleteLoop(loop)
            AppWidgetUpdateWorker.updateWidget(app)
        }
    }
}