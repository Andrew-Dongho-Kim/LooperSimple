package com.pnd.android.loop.data

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
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
    private val min by lazy { created.minusMonths(3).withDayOfMonth(1) }

    override fun getRefreshKey(
        state: PagingState<LocalDate, LoopDoneVo>
    ): LocalDate? {
        return state.anchorPosition?.let { anchorPosition ->
            val closestPage = state.closestPageToPosition(anchorPosition)
            closestPage?.prevKey?.plusDays(state.config.pageSize.toLong())
                ?: closestPage?.nextKey?.plusDays(state.config.pageSize.toLong())
        }
    }

    override suspend fun load(
        params: LoadParams<LocalDate>
    ): LoadResult<LocalDate, LoopDoneVo> {
        return try {
            initIfNeed()
            val curr = params.key ?: LocalDate.now()
            val prev = prevKey(curr, pageSize)
            val next = nextKey(curr, pageSize)

            logger.d { "load[${pageSize}] ($curr ~ $next], prev:$prev" }
            LoadResult.Page(
                data = load(curr, next),
                prevKey = prev,
                nextKey = next,
            )

        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    private suspend fun load(curr: LocalDate, next: LocalDate?): List<LoopDoneVo> {
        next ?: return emptyList()


        val doneStates = loopDoneDao.doneStates(
            loopId = loopId.toLong(),
            from = curr.toMs(),
            to = next.toMs()
        )

        val result = mutableListOf<LoopDoneVo>()
        var date = curr
        var index = 0
        while (date.isBefore(next)) {
            result.add(
                if (index < doneStates.size && date.toMs() == doneStates[index].date) {
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

        loop = loopDao.loop(loopId = loopId)
        logger.d { "paging source for loop:$loop" }
    }

    private fun prevKey(curr: LocalDate, loadSize: Int): LocalDate? {
        if (curr == min) return null

        val prevKey = curr.minusDays(loadSize.toLong())
        return if (prevKey > min) prevKey else min
    }

    private fun nextKey(curr: LocalDate, loadSize: Int): LocalDate? {
        val now = LocalDate.now()
        if (curr == now) return null

        val nextKey = curr.plusDays(loadSize.toLong())
        return if (nextKey < now) nextKey else now
    }
}