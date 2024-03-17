package com.pnd.android.loop.ui.home.loop.timeline

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.Tooltip
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.loop.DeleteLoopDialog
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface

@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loop: LoopBase,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
) {
    var isTooltipShown by rememberSaveable { mutableStateOf(false) }

    Tooltip(
        modifier = modifier,
        anchorContent = { anchorModifier ->
            LoopInTimeline(
                modifier = anchorModifier,
                loop = loop
            )
        },
        tooltipContent = {
            LoopDetailAndOption(
                blurState = blurState,
                loop = loop,
                onNavigateToDetailPage = { loop ->
                    onNavigateToDetailPage(loop)
                },
                onTooltipDismiss = {
                    isTooltipShown = false
                },
                onEdit = { loop ->
                    onEdit(loop)
                },
                onDelete = { loop ->
                    onDelete(loop)
                },
            )
        },
        isShown = isTooltipShown,
        onShown = { shown -> isTooltipShown = shown }
    )
}


@Composable
private fun LoopInTimeline(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    val alpha = animateCardAlphaWithMock(loopBase = loop)
    val shape = RoundShapes.small
    Box(
        modifier = modifier
            .alpha(0.7f)
            .padding(1.dp)
            .graphicsLayer { this.alpha = alpha }
            .background(
                color = Color(loop.color),
                shape = shape
            )
            .background(
                color = AppColor.surface.copy(alpha = 0.25f),
                shape = shape
            )
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.2f),
                shape = shape
            )
            .width(loop.timelineWidth())
            .height(timelineItemHeightDp)
    ) {
        Text(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(2.dp),
            text = loop.title,
            overflow = TextOverflow.Ellipsis,
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
private fun LoopDetailAndOption(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loop: LoopBase,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onTooltipDismiss: () -> Unit,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
) {
    Column {
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 12.dp),
            text = loop.title,
            style = AppTypography.bodyMedium.copy(color = AppColor.onSurface)
        )
        LoopOptions(
            modifier = modifier.padding(top = 8.dp),
            blurState = blurState,
            loop = loop,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onTooltipDismiss = onTooltipDismiss,
            onEdit = onEdit,
            onDelete = onDelete
        )
    }
}

@Composable
private fun LoopOptions(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loop: LoopBase,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onTooltipDismiss: () -> Unit = {},
    onEdit: (LoopBase) -> Unit = {},
    onDelete: (LoopBase) -> Unit = {}
) {

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .padding(
                start = 4.dp,
                end = 4.dp,
                bottom = 4.dp
            )
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.2f),
                shape = RoundShapes.small
            )
    ) {
        LoopOptionButton(
            imageVector = Icons.Outlined.Edit,
            contentDescription = stringResource(id = R.string.edit),
            onClick = {
                onTooltipDismiss()
                onEdit(loop)
            }
        )
        LoopOptionButton(
            imageVector = Icons.Outlined.Delete,
            contentDescription = stringResource(id = R.string.delete),
            onClick = {
                showDeleteDialog = true
//                onTooltipDismiss()
                blurState.on()
            }
        )
        LoopOptionButton(
            imageVector = Icons.Outlined.MoreVert,
            onClick = {
                onTooltipDismiss()
                onNavigateToDetailPage(loop)
            }
        )
    }

    if (showDeleteDialog) {
        DeleteLoopDialog(
            loopTitle = loop.title,
            onDismiss = {
                blurState.off()
                showDeleteDialog = false
            },
            onDelete = { onDelete(loop) }
        )
    }
}

@Composable
private fun LoopOptionButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String = "",
    onClick: () -> Unit,
) {
    Image(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(all = 4.dp)
            .padding(horizontal = 4.dp)
            .width(28.dp)
            .height(20.dp),
        imageVector = imageVector,
        colorFilter = ColorFilter.tint(AppColor.onSurface.copy(alpha = 0.7f)),
        contentDescription = contentDescription
    )
}


@Composable
private fun animateCardAlphaWithMock(loopBase: LoopBase): Float {
    if (!loopBase.isMock) {
        return 1f
    }
    val transition = rememberInfiniteTransition("CreateLoopTransitions")
    val alpha by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 0.3f,
        animationSpec = infiniteRepeatable(
            tween(
                durationMillis = 1_500,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "AlphaAnimForCreateLoop",
    )

    return alpha
}


