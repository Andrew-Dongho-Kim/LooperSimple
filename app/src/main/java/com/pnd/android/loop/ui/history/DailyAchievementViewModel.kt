package com.pnd.android.loop.ui.history

import android.app.Application
import android.content.Context
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.paging.Pager
import androidx.paging.PagingConfig
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

private const val PAGE_SIZE = 30
private const val PREF_NAME = "daily_achievement"
private const val KEY_VIEW_MODE = "key_view_mode"

@HiltViewModel
class DailyAchievementViewModel @Inject constructor(
    app: Application,
    appDb: AppDatabase,
) : AndroidViewModel(app) {

    private val loopDao = appDb.loopDao()
    private val loopWithDoneDao = appDb.loopWithDoneDao()

    val flowMinCreatedDate = loopDao.flowMinCreatedTime()
        .map { it.toLocalDate() }

    val achievementPager = Pager(
        PagingConfig(pageSize = PAGE_SIZE),
        pagingSourceFactory = {
            HistoryPagingSource(
                appDb = appDb,
                pageSize = PAGE_SIZE,
            )
        }
    ).flow

    private val pref = app.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    private val _flowViewMode = MutableStateFlow(
        DailyAchievementPageViewMode.valueOf(
            pref.getString(KEY_VIEW_MODE, null) ?: "${DailyAchievementPageViewMode.COLOR_DOT}"
        )
    )
    val flowViewMode: Flow<DailyAchievementPageViewMode> = _flowViewMode
    fun toggleViewMode() {
        _flowViewMode.value = if (_flowViewMode.value == DailyAchievementPageViewMode.COLOR_DOT) {
            DailyAchievementPageViewMode.DESCRIPTION_TEXT
        } else {
            DailyAchievementPageViewMode.COLOR_DOT
        }
        pref.edit {
            putString(KEY_VIEW_MODE, "${_flowViewMode.value}")
        }
    }

    // inclusive all
    fun flowsDoneLoopsByDate(from: LocalDate, to: LocalDate) = loopWithDoneDao.flowDoneLoopsByDate(
        from = from.toMs(),
        to = to.toMs(),
    ).map { doneLoops ->
        doneLoops.groupBy { it.date }
    }

    fun flowsNoDonLoopsByDate(from: LocalDate, to: LocalDate) = loopWithDoneDao.flowNoDoneLoopsByDate(
        from = from.toMs(),
        to = to.toMs(),
    ).map { doneLoops ->
        doneLoops.groupBy { it.date }
    }


}

enum class DailyAchievementPageViewMode {
    COLOR_DOT, DESCRIPTION_TEXT
}