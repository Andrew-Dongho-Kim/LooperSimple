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

    val loop = loopDao.getLoopFlow(loopId)

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

    suspend fun allEnabledDoneStates(loopId: Int) =
        loopDoneDao.getAllEnabled(loopId = loopId)

    suspend fun allEnabledCountBefore(loopId: Int, date: LocalDate) =
        loopDoneDao.getAllEnabledCountBefore(loopId, date.toMs())

    suspend fun doneCountBefore(loopId: Int, date: LocalDate) =
        loopDoneDao.getDoneCountBefore(loopId, date.toMs())

    suspend fun allEnabledCountBetween(loopId: Int, from: LocalDate, to: LocalDate) =
        loopDoneDao.getAllEnabledCountBetween(loopId, from.toMs(), to.toMs())

    suspend fun doneCountBetween(loopId: Int, from: LocalDate, to: LocalDate) =
        loopDoneDao.getDoneCountBetween(loopId, from.toMs(), to.toMs())

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