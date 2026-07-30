package com.pnd.android.loop.ui.home.group

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopGroupVo
import com.pnd.android.loop.ui.common.AppEmptyState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.outline
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer


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
            GroupTopAppBar(
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

    if (groups.isEmpty()) {
        // 담을 그룹이 하나도 없을 때: 공용 빈 상태로 먼저 그룹을 만들도록 안내한다(상단 + 버튼).
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            AppEmptyState(
                modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding),
                icon = Icons.Outlined.Add,
                title = stringResource(id = R.string.group_empty_title),
                hint = stringResource(id = R.string.group_empty_hint),
            )
        }
        return
    }

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        contentPadding = PaddingValues(vertical = Dimens.contentPadding),
    ) {
        itemsIndexed(
            items = groups,
            key = { _, group -> group.loopGroupId }
        ) { index, group ->

            val hasLoopInGroup by loopGroupViewModel.hasLoopInGroupFlow(
                loopGroupId = group.loopGroupId,
                loopId = loopId
            ).collectAsStateWithLifecycle(initialValue = false)

            GroupItem(
                modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding),
                group = group,
                hasLoopInGroup = hasLoopInGroup,
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
                    modifier = Modifier.padding(horizontal = Dimens.screenHorizontalPadding),
                    thickness = 0.5.dp,
                    color = AppColor.outline.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun GroupItem(
    modifier: Modifier = Modifier,
    group: LoopGroupVo,
    hasLoopInGroup: Boolean,
    onGroupSelected: (groupId: Int) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .weight(weight = 1f)
                .clip(RoundShapes.small)
                .clickable(enabled = !hasLoopInGroup) { onGroupSelected(group.loopGroupId) }
                .graphicsLayer {
                    alpha = if (hasLoopInGroup) 0.4f else 1.0f
                }
                .padding(vertical = 14.dp),
            text = group.groupTitle,
            style = AppTypography.titleMedium.copy(color = AppColor.onSurface)
        )

        if (hasLoopInGroup) {
            Text(
                modifier = Modifier
                    .clip(RoundShapes.small)
                    .background(color = AppColor.surfaceContainer)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                text = stringResource(id = R.string.already_added),
                style = AppTypography.bodySmall.copy(
                    color = AppColor.primary
                )
            )
        }
    }
}
