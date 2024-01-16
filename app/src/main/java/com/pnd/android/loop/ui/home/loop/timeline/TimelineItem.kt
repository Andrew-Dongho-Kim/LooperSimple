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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.Tooltip
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface

@Composable
fun TimelineItem(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
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
                loop = loop,
                onNavigateToDetailPage = onNavigateToDetailPage,
            )
        },
        tooltipBackground = AppColor.surface,
        tooltipShape = RoundShapes.small
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
            .padding(start = loop.timelineOffsetStart())
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
            style = MaterialTheme.typography.caption.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@Composable
private fun LoopDetailAndOption(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    Column(modifier = modifier) {
        Text(
            text = loop.title,
            style = AppTypography.body1.copy(color = AppColor.onSurface)
        )
        LoopOptions(
            modifier = modifier.padding(top = 4.dp),
            loop = loop,
        )
    }
}

@Composable
private fun LoopOptions(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onEdit: (LoopBase) -> Unit = {},
    onDelete: (LoopBase) -> Unit = {}
) {
    Row(modifier = modifier) {
        Image(
            modifier = Modifier
                .padding(start = 8.dp)
                .clickable { onEdit(loop) }
                .padding(all = 4.dp)
                .size(20.dp),
            imageVector = Icons.Outlined.Edit,
            colorFilter = ColorFilter.tint(AppColor.onSurface.copy(alpha = ContentAlpha.medium)),
            contentDescription = stringResource(id = R.string.edit)
        )
        Image(
            modifier = Modifier

                .clickable { onDelete(loop) }
                .padding(all = 4.dp)
                .size(20.dp),
            imageVector = Icons.Outlined.Delete,
            colorFilter = ColorFilter.tint(AppColor.onSurface.copy(alpha = ContentAlpha.medium)),
            contentDescription = stringResource(id = R.string.delete)
        )
    }
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


