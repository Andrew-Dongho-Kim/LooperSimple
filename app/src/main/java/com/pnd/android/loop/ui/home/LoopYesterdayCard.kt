package com.pnd.android.loop.ui.home

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.annotatedString
import java.time.LocalDate

@Composable
fun LoopYesterdayCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    isExpanded: Boolean,
    onExpandChanged: (isExpanded: Boolean) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    // The loop awaiting a done/skip confirmation, or null when no dialog is shown.
    var pendingAction by remember { mutableStateOf<YesterdayAction?>(null) }

    Box(
        modifier = modifier
            .padding(vertical = 16.dp, horizontal = 20.dp)
            .clip(RoundShapes.large)
            .background(color = AppColor.surfaceContainer)
            .animateContentSize()
    ) {
        // 카드 왼쪽 끝의 얇은 강조 바 — "미확인" 상태를 알리는 포인트 컬러
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .align(Alignment.CenterStart)
                .background(color = AppColor.error)
        )

        Column(modifier = Modifier.padding(start = 3.dp)) {
            LoopYesterdayHeader(
                count = loops.size,
                isExpanded = isExpanded,
                onExpandChanged = onExpandChanged
            )

            if (isExpanded) {
                loops.forEach { loop ->
                    YesterdayDivider()
                    LoopYesterdayItem(
                        loop = loop,
                        onRequestDone = { pendingAction = YesterdayAction(loop, LoopDoneVo.DoneState.DONE) },
                        onRequestSkip = { pendingAction = YesterdayAction(loop, LoopDoneVo.DoneState.SKIP) },
                        onNavigateToDetailPage = onNavigateToDetailPage,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    pendingAction?.let { action ->
        LoopYesterdayConfirmDialog(
            action = action,
            onConfirm = {
                loopViewModel.changeLoopState(
                    loop = action.loop,
                    localDate = LocalDate.now().minusDays(1),
                    doneState = action.doneState
                )
            },
            onDismiss = { pendingAction = null }
        )
    }
}

/**
 * A loop together with the [LoopDoneVo.DoneState] the user is about to apply to it,
 * held while the confirmation dialog is visible.
 */
private data class YesterdayAction(
    val loop: LoopBase,
    @LoopDoneVo.DoneState val doneState: Int,
)

@Composable
private fun YesterdayDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 1.dp,
        color = AppColor.onSurface.copy(alpha = 0.08f)
    )
}

@Composable
private fun LoopYesterdayHeader(
    modifier: Modifier = Modifier,
    count: Int,
    isExpanded: Boolean,
    onExpandChanged: (isExpanded: Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(shape = RoundShapes.large)
            .clickable { onExpandChanged(!isExpanded) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = Icons.Outlined.History,
            tint = AppColor.error.copy(alpha = 0.8f),
            contentDescription = null
        )

        Text(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            text = annotatedString(stringResource(id = R.string.unchecked_loops, count)),
            style = AppTypography.bodyMedium.copy(color = AppColor.onSurface)
        )

        ExpandChevron(isExpanded = isExpanded)
    }
}

@Composable
private fun ExpandChevron(
    modifier: Modifier = Modifier,
    isExpanded: Boolean,
) {
    val rotation by animateFloatAsState(
        targetValue = if (isExpanded) -180f else 0f,
        animationSpec = tween(durationMillis = 400),
        label = "chevronRotation"
    )

    Icon(
        modifier = modifier.graphicsLayer { rotationX = rotation },
        imageVector = Icons.Rounded.ExpandMore,
        tint = AppColor.onSurface.copy(alpha = 0.6f),
        contentDescription = null
    )
}

@Composable
private fun LoopYesterdayItem(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onRequestDone: () -> Unit,
    onRequestSkip: () -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onNavigateToDetailPage(loop) }
            .padding(start = 16.dp, end = 12.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoopTitle(
            modifier = Modifier.weight(1f),
            title = loop.title
        )
        Spacer(modifier = Modifier.width(8.dp))
        YesterdayActionButton(
            icon = Icons.Filled.Done,
            tint = AppColor.primary,
            contentDescription = stringResource(id = R.string.done),
            onClick = onRequestDone
        )
        YesterdayActionButton(
            icon = Icons.Filled.Close,
            tint = AppColor.onSurface,
            contentDescription = stringResource(id = R.string.skip),
            onClick = onRequestSkip
        )
    }
}

@Composable
private fun YesterdayActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    tint: Color,
    contentDescription: String,
    onClick: () -> Unit,
) {
    Icon(
        modifier = modifier
            .padding(start = 4.dp)
            .size(36.dp)
            .clip(CircleShape)
            .background(tint.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(9.dp),
        imageVector = icon,
        tint = tint.copy(alpha = 0.9f),
        contentDescription = contentDescription
    )
}

@Composable
private fun LoopTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        style = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.8f)
        )
    )
}

@Composable
private fun LoopYesterdayConfirmDialog(
    action: YesterdayAction,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val messageRes = when (action.doneState) {
        LoopDoneVo.DoneState.DONE -> R.string.done_confirm_message
        else -> R.string.skip_confirm_message
    }

    AlertDialog(
        modifier = Modifier.padding(horizontal = 32.dp),
        shape = RoundShapes.medium,
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = stringResource(id = messageRes, action.loop.title),
                style = AppTypography.titleMedium.copy(color = AppColor.onSurface)
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    style = AppTypography.titleMedium
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onConfirm()
                onDismiss()
            }) {
                Text(
                    text = stringResource(id = R.string.ok),
                    style = AppTypography.titleMedium
                )
            }
        },
        containerColor = AppColor.surface,
        tonalElevation = 0.dp,
    )
}