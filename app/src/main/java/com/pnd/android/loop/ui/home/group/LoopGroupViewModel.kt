package com.pnd.android.loop.ui.home.group

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pnd.android.loop.data.LoopGroupVo
import com.pnd.android.loop.data.LoopRelationVo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject

@Stable
@HiltViewModel
class LoopGroupViewModel @Inject constructor(
    private val repository: LoopGroupRepository
) : ViewModel() {

    val allGroups = repository.getAllGroupsFlow()

    val allGroupsWithLoops = repository
        .getAllGroupsWithLoopsFlow()
        .map { groups ->
            groups.filter { group ->
                group.group != null
            }
        }

    fun getGroupWithLoopsFlow(groupId: Int) = repository.getGroupWithLoopsFlow(groupId = groupId)

    fun addGroup(title: String) {
        viewModelScope.launch {
            repository.addGroup(
                LoopGroupVo(
                    loopGroupId = 0,
                    groupTitle = title
                )
            )
        }
    }

    fun addToGroup(
        loopGroupId: Int,
        loopId: Int
    ) {
        viewModelScope.launch {
            repository.addToGroup(
                LoopRelationVo(
                    loopGroupId = loopGroupId,
                    loopId = loopId,
                )
            )
        }
    }

    fun removeFromGroup(
        loopGroupId: Int,
        loopId: Int
    ) {
        viewModelScope.launch {
            repository.removeFromGroup(
                loopGroupId = loopGroupId,
                loopId = loopId
            )
        }
    }

    fun deleteGroup(
        loopGroupId:Int,
    ) {
        viewModelScope.launch {
            repository.deleteGroup(
                loopGroupId = loopGroupId
            )
        }
    }
}