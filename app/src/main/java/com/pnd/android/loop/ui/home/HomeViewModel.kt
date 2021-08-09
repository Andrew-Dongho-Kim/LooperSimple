package com.pnd.android.loop.ui.home

import androidx.lifecycle.*
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.Loop
import com.pnd.android.loop.data.LoopFilter
import com.pnd.android.loop.data.LoopFilter.Companion.filter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    appDb: AppDatabase
) : ViewModel() {
    private val logger = log("HomeViewModel")

    private val loopDao = appDb.loopDao()
    private val loopFilterDao = appDb.loopFilterDao()

    val loopFilter = Transformations.map(loopFilterDao.get()) {
        it ?: LoopFilter.DEFAULT
    }
    private val _loops = liveData {
        emitSource(loopDao.getAll())
    } as MutableLiveData<List<Loop>>
    private val _filteredLoops = MediatorLiveData<List<Loop>>().apply {
        var savedFilter: LoopFilter = LoopFilter.DEFAULT
        var savedLoops: List<Loop> = emptyList()
        fun result(): List<Loop> {
            return savedLoops.filter { loop -> loop.filter(savedFilter) }
        }
        addSource(loopFilter) { filter ->
            savedFilter = filter ?: LoopFilter.DEFAULT
            value = result()
        }
        addSource(_loops) { loops ->
            savedLoops = loops ?: emptyList()
            value = result()
        }
    }

    val loops: LiveData<List<Loop>> = _filteredLoops

    private val loopsInProgress = Transformations.switchMap(loops) { loops ->
        liveData {
            emit(loops.filter { it.enabled })
        }
    }

    val countInProgress = Transformations.map(loopsInProgress) {
        logger.d { "loop in progress : ${it.size}" }
        it.size
    }

    val total = Transformations.map(loops) {
        it.size
    }

    fun notifyLoops() {
        _loops.postValue(_loops.value)
    }

    fun saveFilter(loopFilter: LoopFilter) {
        GlobalScope.launch { loopFilterDao.update(loopFilter) }
    }

    fun addLoop(vararg loops: Loop, action: ((Loop) -> Unit)? = null) {
        GlobalScope.launch {
            logger.d { "Add loop" }
            loops.forEach { logger.d { " - $it" } }

            loopDao.add(*loops).forEachIndexed { index, id ->
                logger.d { "id of loop for $index is updated to $id" }

                loops[index].id = id.toInt()
                action?.invoke(loops[index])
            }
        }
    }

    fun removeLoop(id: Int) {
        GlobalScope.launch {
            loopDao.remove(id)
        }
    }
}