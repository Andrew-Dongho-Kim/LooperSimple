package com.pnd.android.loop.ui.home.group

import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopGroupVo
import com.pnd.android.loop.data.LoopRelationVo
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject

class LoopGroupRepository @Inject constructor(
    appDb: AppDatabase
) {
    private val loopGroupDao = appDb.loopGroupDao()

    fun getAllGroupsFlow() = loopGroupDao.getAllGroupsFlow()
    fun getAllGroupsWithLoopsFlow() = loopGroupDao.getAllGroupsWithLoopsFlow()
    fun getGroupWithLoopsFlow(loopGroupId: Int) = loopGroupDao.getGroupWithLoopsFlow(loopGroupId)
    @OptIn(ExperimentalCoroutinesApi::class)
    fun hasLoopInGroupFlow(
        loopGroupId: Int,
        loopId: Int
    ) = loopGroupDao.getRelationFlow(
        loopGroupId = loopGroupId,
        loopId = loopId,
    ).mapLatest { relation ->
        relation != null
    }

    suspend fun addGroup(vararg groups: LoopGroupVo) {
        loopGroupDao.insert(*groups)
    }

    suspend fun addToGroup(vararg relations: LoopRelationVo) {
        loopGroupDao.insert(*relations)
    }

    suspend fun removeFromGroup(
        loopGroupId: Int,
        loopId: Int,
    ) {
        loopGroupDao.removeFromGroup(
            loopGroupId = loopGroupId,
            loopId = loopId
        )
    }

    suspend fun deleteGroup(
        loopGroupId: Int
    ) {
        loopGroupDao.delete(loopGroupId = loopGroupId)
    }
}