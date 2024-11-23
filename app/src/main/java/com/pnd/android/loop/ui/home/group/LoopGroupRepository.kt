package com.pnd.android.loop.ui.home.group

import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopGroupVo
import javax.inject.Inject

class LoopGroupRepository @Inject constructor(
    appDb: AppDatabase
) {
    private val loopGroupDao = appDb.loopGroupDao()

    fun getAllGroupsWithLoopsFlow() = loopGroupDao.getAllGroupsWithLoopsFlow()

    fun getGroupWithLoopsFlow(groupId: Int) = loopGroupDao.getGroupWithLoopsFlow(groupId)

    suspend fun addGroup(vararg groups: LoopGroupVo) {
        loopGroupDao.insert(*groups)
    }
}