package com.pnd.android.loop.ui.history

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

private const val PAGE_SIZE = 30

@HiltViewModel
class DailyAchievementViewModel @Inject constructor(
    appDb: AppDatabase,
) : ViewModel() {

    private val loopDao = appDb.loopDao()
    private val loopWithDoneDao = appDb.loopWithDoneDao()

    val flowMinCreatedDate = loopDao.flowMinCreatedTime()
        .map { it.toLocalDate() }


    // inclusive all
    fun flowsDoneLoopsByDate(from: LocalDate, to: LocalDate) = loopWithDoneDao.flowDoneLoopsByDate(
        from = from.toMs(),
        to = to.toMs(),
    ).map { doneLoops ->
        Log.e("TEST-DH", "$doneLoops")
        doneLoops.groupBy { it.date }
    }

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