package com.pnd.android.loop.ui.statisctics

import androidx.lifecycle.ViewModel
import com.pnd.android.loop.data.AppDatabase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class StatisticsViewModel @Inject constructor(
    appDb: AppDatabase,
) : ViewModel() {

    private val loopWithDoneDao = appDb.fullLoopDao()

    fun flowLoopsWithStatistics(from: Long, to: Long) = loopWithDoneDao.getLoopsWithStatisticsFlow(
        from = from,
        to = to
    )
}
