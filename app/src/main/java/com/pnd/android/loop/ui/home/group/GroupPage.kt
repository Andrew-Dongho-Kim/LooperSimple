package com.pnd.android.loop.ui.home.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopGroupVo
import com.pnd.android.loop.data.LoopGroupWithLoops
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.LoopCardValues
import com.pnd.android.loop.ui.home.LoopCardWithOption
import com.pnd.android.loop.ui.home.rememberBlurState
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface

@Composable
fun GroupPage(
    modifier: Modifier = Modifier,
    loopGroupViewModel: LoopGroupViewModel = hiltViewModel(),
    loopViewModel: LoopViewModel,
    onNavigateUp: () -> Unit,
) {
    val blurState = rememberBlurState()

    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .blur(radius = blurState.radius)
            .background(color = AppColor.background),
        topBar = {
            GroupPickerAppBar(
                modifier = modifier.statusBarsPadding(),
                onNavigateUp = onNavigateUp,
                onCreateGroup = { groupTitle ->
                    loopGroupViewModel.addGroup(groupTitle.toString())
                }
            )
        },
    )
    { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            GroupPageContent(
                blurState = blurState,
                loopGroupViewModel = loopGroupViewModel,
                loopViewModel = loopViewModel,
            )
        }
    }
}

@Composable
private fun GroupPageContent(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopGroupViewModel: LoopGroupViewModel,
    loopViewModel: LoopViewModel,
) {
    Column(modifier = modifier) {
        Groups(
            blurState = blurState,
            loopGroupViewModel = loopGroupViewModel,
            loopViewModel = loopViewModel,
        )
    }
}

@Composable
private fun Groups(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopGroupViewModel: LoopGroupViewModel,
    loopViewModel: LoopViewModel,
) {
    val groups by loopGroupViewModel
        .allGroupsWithLoops
        .collectAsStateWithLifecycle(initialValue = emptyList())

    val lazyListState = rememberLazyListState()

    var showDeleteGroupDialog by rememberSaveable { mutableStateOf(false) }
    var showRemoveFromGroupDialog by rememberSaveable { mutableStateOf(false) }

    var deleteGroup by rememberSaveable { mutableStateOf<LoopGroupVo?>(null) }
    var deleteLoop by rememberSaveable { mutableStateOf<LoopBase?>(null) }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
    ) {
        itemsIndexed(
            items = groups,
            key = { _, groupWithLoops -> groupWithLoops.group!!.loopGroupId }
        ) { _, groupWithLoops ->
            GroupItem(
                modifier = Modifier.padding(12.dp),
                blurState = blurState,
                loopViewModel = loopViewModel,
                groupWithLoops = groupWithLoops,
                onRemoveFromGroup = { group, loop ->
                    deleteGroup = group
                    deleteLoop = loop
                    showRemoveFromGroupDialog = true
                    blurState.on()
                },
                onDeleteGroup = { group ->
                    deleteGroup = group
                    showDeleteGroupDialog = true
                    blurState.on()
                }
            )
        }
    }

    if (showDeleteGroupDialog) {
        DeleteDialog(
            title = stringResource(
                id = R.string.delete_group_message,
                deleteGroup?.groupTitle ?: ""
            ),
            onDelete = {
                loopGroupViewModel.deleteGroup(
                    loopGroupId = deleteGroup?.loopGroupId ?: -1
                )
                showDeleteGroupDialog = false
                blurState.off()
            },
            onDismiss = {
                showDeleteGroupDialog = false
                blurState.off()
            },
        )
    }

    if (showRemoveFromGroupDialog) {
        DeleteDialog(
            title = stringResource(
                id = R.string.remove_from_group_message,
                deleteLoop?.title ?: "",
                deleteGroup?.groupTitle ?: ""
            ),
            onDelete = {
                loopGroupViewModel.removeFromGroup(
                    loopGroupId = deleteGroup?.loopGroupId ?: -1,
                    loopId = deleteLoop?.loopId ?: -1
                )
                showRemoveFromGroupDialog = false
                blurState.off()
            },
            onDismiss = {
                showRemoveFromGroupDialog = false
                blurState.off()
            },
        )
    }
}

@Composable
private fun GroupItem(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    groupWithLoops: LoopGroupWithLoops,
    onRemoveFromGroup: (group: LoopGroupVo, LoopBase: LoopBase) -> Unit,
    onDeleteGroup: (group: LoopGroupVo) -> Unit
) {
    val group = groupWithLoops.group ?: return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = AppColor.surface,
        ),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        GroupTitle(
            title = group.groupTitle,
            onDeleteGroup = { onDeleteGroup(group) }
        )

        if (groupWithLoops.loops.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
        }

        groupWithLoops.loops.onEach { loop ->
            LoopCardItem(
                modifier = Modifier.padding(bottom = 12.dp),
                blurState = blurState,
                loopViewModel = loopViewModel,
                loop = loop,
                onRemoveFromGroup = { deleteLoop -> onRemoveFromGroup(group, deleteLoop) }
            )
        }

        if (groupWithLoops.loops.isNotEmpty()) {
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun GroupTitle(
    modifier: Modifier = Modifier,
    title: String,
    onDeleteGroup: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .padding(
                    start = 24.dp,
                    top = 12.dp,
                    bottom = 12.dp,
                )
                .weight(weight = 1f),
            text = title
        )

        DeleteGroupButton(
            modifier = modifier
                .padding(end = 12.dp)
                .size(32.dp),
            onDeleteGroup = onDeleteGroup,
        )
    }
}

@Composable
private fun DeleteGroupButton(
    modifier: Modifier = Modifier,
    onDeleteGroup: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onDeleteGroup),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = Icons.Outlined.Delete,
            tint = AppColor.onSurface.copy(alpha = 0.6f),
            contentDescription = stringResource(id = R.string.delete_group)
        )
    }
}

@Composable
private fun LoopCardItem(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    onRemoveFromGroup: (loop: LoopBase) -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoopCardWithOption(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .weight(weight = 1f),
            blurState = blurState,
            loopViewModel = loopViewModel,
            loop = loop,
            cardValues = LoopCardValues(
                syncWithTime = false,
                isHighlighted = false,
                showAddToGroup = false,
            ),
            onEdit = {},
            onNavigateToGroupPicker = {},
            onNavigateToDetailPage = {},
        )
        LoopCardItemRemoveIcon(
            modifier = Modifier
                .padding(end = 12.dp)
                .size(32.dp),
            onRemoveFromGroup = { onRemoveFromGroup(loop) }
        )
    }
}

@Composable
private fun LoopCardItemRemoveIcon(
    modifier: Modifier = Modifier,
    onRemoveFromGroup: () -> Unit,
) {
    Box(
        modifier = modifier
            .clip(CircleShape)
            .clickable(onClick = onRemoveFromGroup),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            modifier = Modifier.size(24.dp),
            imageVector = Icons.Outlined.Remove,
            tint = AppColor.error.copy(alpha = 0.7f),
            contentDescription = stringResource(id = R.string.remove_from_group),
        )
    }
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