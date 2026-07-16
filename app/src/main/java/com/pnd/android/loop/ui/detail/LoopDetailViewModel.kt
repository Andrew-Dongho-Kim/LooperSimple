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

    val loop = loopDao.getLoopFlow(loopId)

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
}