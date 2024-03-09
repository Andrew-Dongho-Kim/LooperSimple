package com.pnd.android.loop.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.DonePagingSource
import com.pnd.android.loop.ui.Screen
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject

private const val PAGE_SIZE = 150

@HiltViewModel
class LoopDetailViewModel @Inject constructor(
    appDb: AppDatabase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val loopId: Int = savedStateHandle[Screen.ARGS_ID] ?: -1

    private val loopDao = appDb.loopDao()
    private val loopDoneDao = appDb.loopDoneDao()

    val loop = loopDao.flowLoop(loopId)

    val allCount = loopDoneDao.flowAllCount(loopId)
    val responseCount = loopDoneDao.flowResponseCount(loopId)
    val doneCount = loopDoneDao.flowDoneCount(loopId)
    val skipCount = loopDoneDao.flowSkipCount(loopId)

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

    suspend fun allDoneStates(loopId: Int) =
        loopDoneDao.allDoneStates(loopId = loopId)

    suspend fun allCountBefore(loopId: Int, date: LocalDate) =
        loopDoneDao.allCountBefore(loopId, date.toMs())

    suspend fun doneCountBefore(loopId: Int, date: LocalDate) =
        loopDoneDao.doneCountBefore(loopId, date.toMs())

    suspend fun allCountBetween(loopId: Int, from: LocalDate, to: LocalDate) =
        loopDoneDao.allCountBetween(loopId, from.toMs(), to.toMs())

    suspend fun doneCountBetween(loopId: Int, from: LocalDate, to: LocalDate) =
        loopDoneDao.doneCountBetween(loopId, from.toMs(), to.toMs())
}