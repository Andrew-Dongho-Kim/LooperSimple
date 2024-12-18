package com.pnd.android.loop.ui.detail

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.DayOfWeek
import java.time.LocalDate

class DonePagingSource(
    appDb: AppDatabase,
    private val loopId: Int,
    private val pageSize: Int,
) : PagingSource<LocalDate, LoopDoneVo>() {

    private val logger = Logger("DonePagingSource")

    private val loopDao = appDb.loopDao()
    private val loopDoneDao = appDb.loopDoneDao()

    private lateinit var loop: LoopBase
    private val created by lazy { loop.created.toLocalDate() }
    private val minDate by lazy {
        var dateTime = created.minusMonths(3).withDayOfMonth(1)
        while (dateTime.dayOfWeek != DayOfWeek.SUNDAY)
            dateTime = dateTime.minusDays(1)
        dateTime
    }

    override fun getRefreshKey(
        state: PagingState<LocalDate, LoopDoneVo>
    ): LocalDate? {
        return state.anchorPosition?.let { anchorPosition ->
            val closestPage = state.closestPageToPosition(anchorPosition)
            closestPage?.prevKey?.plusDays(state.config.pageSize.toLong())
                ?: closestPage?.nextKey?.plusDays(state.config.pageSize.toLong())
        }.also {
            logger.d { "getRefreshKey:$it" }
        }
    }

    override suspend fun load(
        params: LoadParams<LocalDate>
    ): LoadResult<LocalDate, LoopDoneVo> {
        return try {
            initIfNeed()
            val curr = params.key ?: LocalDate.now().plusDays(1)
            val prev = prevKey(curr, pageSize)
            val next = nextKey(curr, pageSize)
            val data = load(prev, curr)

            logger.d { "load[${data.size}] ($prev ~ $curr], next:$next" }
            LoadResult.Page(
                data = data,
                prevKey = prev,
                nextKey = next,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun load(
        prev: LocalDate?,
        curr: LocalDate
    ): List<LoopDoneVo> {
        val from = prev ?: return emptyList()

        val doneStates = loopDoneDao.getAllBetween(
            loopId = loopId,
            from = from.toMs(),
            to = curr.toMs()
        )

        val result = mutableListOf<LoopDoneVo>()
        var date = from
        var index = 0
        while (date.isBefore(curr)) {
            result.add(
                if (index < doneStates.size && date == doneStates[index].date.toLocalDate()) {
                    doneStates[index++]
                } else {
                    LoopDoneVo(
                        loopId = loopId,
                        date = date.toMs()
                    )
                }
            )

            date = date.plusDays(1)
        }
        return result
    }

    private suspend fun initIfNeed() {
        if (::loop.isInitialized) return

        loop = loopDao.getLoop(loopId = loopId)
        logger.d { "paging source for loop:$loop" }
    }

    private fun prevKey(curr: LocalDate, loadSize: Int): LocalDate? {
        if (curr == minDate) return null

        val prevKey = curr.minusDays(loadSize.toLong())
        return if (prevKey > minDate) prevKey else minDate
    }

    private fun nextKey(curr: LocalDate, loadSize: Int): LocalDate? {
        val tomorrow = LocalDate.now().plusDays(1)
        if (curr == tomorrow) return null

        val nextKey = curr.plusDays(loadSize.toLong())
        return if (nextKey < tomorrow) nextKey else tomorrow
    }
}