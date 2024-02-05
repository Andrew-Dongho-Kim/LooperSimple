package com.pnd.android.loop.ui.history

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pnd.android.loop.data.LoopBase
import java.time.LocalDate

class HistoryPagingSource : PagingSource<LocalDate, List<LoopBase>>() {

    override fun getRefreshKey(state: PagingState<LocalDate, List<LoopBase>>): LocalDate? {
        return state.anchorPosition?.let { anchorPosition ->
            val closestPage = state.closestPageToPosition(anchorPosition)
            closestPage?.prevKey?.plusDays(state.config.pageSize.toLong())
                ?: closestPage?.nextKey?.plusDays(state.config.pageSize.toLong())
        }
    }

    override suspend fun load(params: LoadParams<LocalDate>): LoadResult<LocalDate, List<LoopBase>> {
        TODO("Not yet implemented")
    }
}