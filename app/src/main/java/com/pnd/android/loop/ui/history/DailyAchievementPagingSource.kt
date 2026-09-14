package com.pnd.android.loop.ui.history

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pnd.android.loop.data.FullLoopVo
import com.pnd.android.loop.data.history.LoopHistorySnapshot
import java.time.LocalDate

/** Legacy pager, backed by the same immutable history as the current calendar. */
class DailyAchievementPagingSource(
    private val snapshot: LoopHistorySnapshot,
    private val pageSize: Int,
) : PagingSource<LocalDate, List<FullLoopVo>>() {
    private val today = LocalDate.now()
    private val firstDate = snapshot.firstDate ?: today

    override fun getRefreshKey(state: PagingState<LocalDate, List<FullLoopVo>>): LocalDate? =
        state.anchorPosition?.let { state.closestPageToPosition(it)?.nextKey?.minusDays(pageSize.toLong()) }

    override suspend fun load(params: LoadParams<LocalDate>): LoadResult<LocalDate, List<FullLoopVo>> {
        val end = minOf(params.key ?: today.plusDays(1), today.plusDays(1))
        val start = maxOf(firstDate, end.minusDays(params.loadSize.toLong()))
        val days = snapshot.days(start, end.minusDays(1)).groupBy { it.date }
        return LoadResult.Page(
            data = days.toSortedMap().values.map { records -> records.map { it.asFullLoop() } },
            prevKey = start.takeIf { it > firstDate },
            nextKey = end.plusDays(pageSize.toLong()).takeIf { end <= today },
        )
    }
}
