package com.pnd.android.loop.ui.home.group

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopGroupWithLoops
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.LoopCardWithOption
import com.pnd.android.loop.ui.home.rememberBlurState
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
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
            GroupPageAppBar(
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
            )
        }
    }
}

@Composable
private fun GroupItem(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    groupWithLoops: LoopGroupWithLoops,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Text(
            text = groupWithLoops.group!!.groupTitle
        )
        groupWithLoops.loops.onEach { loop ->
            LoopCardWithOption(
                modifier = Modifier.padding(vertical = 12.dp),
                blurState = blurState,
                loopViewModel = loopViewModel,
                loop = loop,
                onNavigateToDetailPage = {},
                onEdit = {},
                isSyncTime = true,
                isHighlighted = false
            )
        }
    }

}

@Composable
private fun CreateGroupDialog(
    modifier: Modifier = Modifier,
    onCreate: (CharSequence) -> Unit,
    onDismiss: () -> Unit,
) {
    val textFiled = rememberTextFieldState()
    AlertDialog(
        modifier = modifier.padding(horizontal = 32.dp),
        shape = RoundShapes.medium,
        onDismissRequest = onDismiss,
        text = {
            if (textFiled.text.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.enter_group_title),
                    style = AppTypography.titleMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.3f)
                    ),
                )
            }
            BasicTextField(
                state = textFiled,
                textStyle = AppTypography.titleMedium.copy(
                    color = AppColor.onSurface
                ),
                cursorBrush = SolidColor(value = AppColor.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    style = AppTypography.titleMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onCreate(textFiled.text)
                onDismiss()
            }) {
                Text(
                    text = stringResource(id = R.string.ok),
                    style = AppTypography.titleMedium,
                )
            }
        },
        containerColor = AppColor.surface,
        tonalElevation = 0.dp,
    )
}

@Composable
private fun GroupPageAppBar(
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