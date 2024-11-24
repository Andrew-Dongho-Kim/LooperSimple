package com.pnd.android.loop.ui.home.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopGroupVo
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface


@Composable
fun GroupPicker(
    modifier: Modifier = Modifier,
    loopGroupViewModel: LoopGroupViewModel = hiltViewModel(),
    loopId: Int,
    onNavigateUp: () -> Unit,
) {
    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.background),
        topBar = {
            GroupPickerAppBar(
                modifier = modifier.statusBarsPadding(),
                onCreateGroup = { groupTitle -> loopGroupViewModel.addGroup(groupTitle.toString()) },
                onNavigateUp = onNavigateUp,
            )
        },
    )
    { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            GroupPickerContent(
                loopGroupViewModel = loopGroupViewModel,
                loopId = loopId,
                onNavigateUp = onNavigateUp
            )
        }
    }
}

@Composable
private fun GroupPickerContent(
    modifier: Modifier = Modifier,
    loopGroupViewModel: LoopGroupViewModel,
    loopId: Int,
    onNavigateUp: () -> Unit,
) {
    val groups by loopGroupViewModel
        .allGroups
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val lazyListState = rememberLazyListState()

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
    ) {
        itemsIndexed(
            items = groups,
            key = { _, group -> group.loopGroupId }
        ) { index, group ->
            GroupItem(
                modifier = Modifier.padding(all = 8.dp),
                group = group,
                onGroupSelected = { loopGroupId ->
                    loopGroupViewModel.addToGroup(
                        loopGroupId = loopGroupId,
                        loopId = loopId,
                    )
                    onNavigateUp()
                }
            )
            if (index < groups.size - 1) {
                HorizontalDivider(
                    modifier = Modifier.padding(horizontal = 24.dp),
                    thickness = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.3f)
                )
            }
        }
    }
}

@Composable
private fun GroupItem(
    modifier: Modifier = Modifier,
    group: LoopGroupVo,
    onGroupSelected: (groupId: Int) -> Unit
) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onGroupSelected(group.loopGroupId) }
            .padding(start = 24.dp)
            .padding(vertical = 12.dp),
        text = group.groupTitle,
        style = AppTypography.titleMedium.copy(color = AppColor.onSurface)
    )
}

@Composable
private fun GroupPickerAppBar(
    modifier: Modifier = Modifier,
    onCreateGroup: (CharSequence) -> Unit,
    onNavigateUp: () -> Unit,
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    SimpleAppBar(
        modifier = modifier,
        title = stringResource(id = R.string.group),
        onNavigateUp = onNavigateUp,
        actions = {
            AppBarIcon(
                imageVector = Icons.Outlined.Add,
                color = AppColor.onSurface.copy(alpha = 0.8f),
                descriptionResId = R.string.add_group,
                onClick = { showCreateGroupDialog = true }
            )
        }
    )

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onCreate = onCreateGroup,
            onDismiss = { showCreateGroupDialog = false }
        )
    }
}