package com.pnd.android.loop.ui.history

import androidx.paging.PagingSource
import androidx.paging.PagingState
import com.pnd.android.loop.common.Logger
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.Day.Companion.isOn
import com.pnd.android.loop.data.FullLoopVo
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.toFullLoopVo
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.DayOfWeek
import java.time.LocalDate

class DailyAchievementPagingSource(
    appDb: AppDatabase,
    private val pageSize: Int,
) : PagingSource<LocalDate, List<FullLoopVo>>() {

    private val logger = Logger("HistoryPagingSource")

    private val loopDao = appDb.loopDao()
    private val loopDoneDao = appDb.loopDoneDao()
    private val loopRetrospectDao = appDb.loopRetrospectDao()

    private lateinit var minDate: LocalDate
    private val maxDate = LocalDate.now().plusDays(1L)

    private val loopsByDayOfWeek = mutableMapOf<DayOfWeek, List<LoopBase>>()

    override fun getRefreshKey(state: PagingState<LocalDate, List<FullLoopVo>>): LocalDate? {
        return state.anchorPosition?.let { anchorPosition ->
            val closestPage = state.closestPageToPosition(anchorPosition)
            closestPage?.prevKey?.plusDays(state.config.pageSize.toLong())
                ?: closestPage?.nextKey?.plusDays(state.config.pageSize.toLong())
        }
    }

    override suspend fun load(
        params: LoadParams<LocalDate>
    ): LoadResult<LocalDate, List<FullLoopVo>> {
        return try {
            init()
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
            logger.e { "error:$e" }
            LoadResult.Error(e)
        }
    }

    private suspend fun load(
        prev: LocalDate?,
        curr: LocalDate
    ): List<List<FullLoopVo>> {
        val from = prev ?: return emptyList()

        val results = mutableListOf<List<FullLoopVo>>()
        var date = from
        while (date.isBefore(curr)) {
            val loops = loopsByDayOfWeek[date.dayOfWeek]
            if (loops.isNullOrEmpty()) {
                date = date.plusDays(1)
                continue
            }

            results.add(
                loops
                    .filter { loop ->
                        !loop.created.toLocalDate().isAfter(date)
                    }.map { loop ->
                        val doneVo = loopDoneDao.getDoneState(
                            loopId = loop.id,
                            date = date.toMs()
                        )
                        val retrospectVo = loopRetrospectDao.getRetrospect(
                            loopId = loop.id,
                            localDate = date.toMs(),
                        )
                        loop.toFullLoopVo(
                            retrospectVo = retrospectVo,
                            doneVo = doneVo ?: LoopDoneVo(
                                loopId = loop.id,
                                done = if (loop.enabled) {
                                    DoneState.NO_RESPONSE
                                } else {
                                    DoneState.DISABLED
                                },
                                date = date.toMs()
                            )
                        )
                    }
            )
            date = date.plusDays(1)
        }
        return results
    }

    private suspend fun init() {
        if (loopsByDayOfWeek.isNotEmpty()) return

        val allLoops = loopDao.allLoops()
        minDate = allLoops.minOf { it.created }.toLocalDate()

        for (day in DayOfWeek.entries) {
            loopsByDayOfWeek[day] = allLoops.filter {
                it.loopActiveDays.isOn(dayForLoop(day))
            }
        }
        logger.d { "init loopsByDayOfWeek:$loopsByDayOfWeek" }
    }

    private fun prevKey(curr: LocalDate, loadSize: Int): LocalDate? {
        if (curr == minDate) return null

        val prevKey = curr.minusDays(loadSize.toLong())
        return if (prevKey > minDate) prevKey else minDate
    }

    private fun nextKey(curr: LocalDate, loadSize: Int): LocalDate? {
        if (curr == maxDate) return null

        val nextKey = curr.plusDays(loadSize.toLong())
        return if (nextKey < maxDate) nextKey else maxDate
    }
}