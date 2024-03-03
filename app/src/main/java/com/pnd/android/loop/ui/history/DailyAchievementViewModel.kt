package com.pnd.android.loop.ui.history

import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.pnd.android.loop.data.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

private const val PAGE_SIZE = 30

@HiltViewModel
class DailyAchievementViewModel @Inject constructor(
    appDb: AppDatabase,
) : ViewModel() {

    private val loopWithDoneDao = appDb.loopWithDoneDao()

    val flowAllLoopsWithDoneStates = loopWithDoneDao.flowAllLoopsWithDoneStates()

    val achievementPager = Pager(
        PagingConfig(pageSize = PAGE_SIZE),
        pagingSourceFactory = {
            HistoryPagingSource(
                appDb = appDb,
                pageSize = PAGE_SIZE,
            )
        }
    ).flow
}