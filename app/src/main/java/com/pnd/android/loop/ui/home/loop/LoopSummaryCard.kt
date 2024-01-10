package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.ui.common.isLargeScreen
import com.pnd.android.loop.ui.shape.CutShape
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.util.formatHourMinute

@Composable
fun LoopSummaryCard(
    modifier: Modifier = Modifier,
    section: Section,
    loopViewModel: LoopViewModel,
) {
    val loops by section.items
    val loopGroup = loops.groupBy { (it as LoopWithDone).done }

    val onUndoDoneState = remember {
        { loop: LoopBase ->
            loopViewModel.doneLoop(
                loop = loop,
                doneState = DoneState.NO_RESPONSE
            )
        }
    }

    Column(modifier = modifier) {
        val doneList = loopGroup[DoneState.DONE] ?: emptyList()
        val skipList = loopGroup[DoneState.SKIP] ?: emptyList()

        val hasDone = doneList.isNotEmpty()
        val hasSkip = skipList.isNotEmpty()
        Summary(
            loops = doneList,
            title = stringResource(id = R.string.done),
            shape = remember(hasSkip) {
                CutShape(
                    topLeft = 24.dp,
                    topRight = 24.dp,
                    bottomLeft = if (!hasSkip) 24.dp else 0.dp,
                    bottomRight = if (!hasSkip) 24.dp else 0.dp,
                )
            },
            icon = Icons.Filled.Done,
            iconColor = MaterialTheme.colors.primary,
            onUndoDoneState = onUndoDoneState,
        )

        Summary(
            modifier = Modifier.padding(top = 8.dp),
            loops = skipList,
            title = stringResource(id = R.string.skip),
            shape = remember(hasDone) {
                CutShape(
                    topLeft = if (!hasDone) 24.dp else 0.dp,
                    topRight = if (!hasDone) 24.dp else 0.dp,
                    bottomLeft = 24.dp,
                    bottomRight = 24.dp,
                )
            },
            icon = Icons.Filled.Clear,
            iconColor = MaterialTheme.colors.onSurface,
            onUndoDoneState = onUndoDoneState,
        )
    }

}

@Composable
private fun Summary(
    modifier: Modifier = Modifier,
    loops: List<LoopBase>,
    title: String,
    shape: Shape,
    icon: ImageVector,
    iconColor: Color,
    onUndoDoneState: (loop: LoopBase) -> Unit,
) {
    if (loops.isEmpty()) return

    Card(
        modifier = modifier,
        shape = shape,
        border = BorderStroke(
            width = 0.5.dp,
            color = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
        )
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = 12.dp,
                vertical = 8.dp
            )
        ) {
            SummaryHeader(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                title = title,
                itemCount = loops.size,
                icon = icon,
                iconColor = iconColor,
            )
            Column(
                modifier = Modifier.padding(top = 4.dp)
            ) {
                loops.forEach { loop ->
                    key(loop.id) {
                        SummaryItem(
                            loop = loop,
                            onUndoDoneState = onUndoDoneState,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryHeader(
    modifier: Modifier = Modifier,
    title: String,
    itemCount: Int,
    icon: ImageVector,
    iconColor: Color,
) {
    Row(
        modifier = modifier.padding(bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "($itemCount) $title",
            style = MaterialTheme.typography.subtitle2.copy(
                color = MaterialTheme.colors.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(12.dp),
            imageVector = icon,
            colorFilter = ColorFilter.tint(color = iconColor),
            contentDescription = title
        )

    }
}

@Composable
private fun SummaryItem(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onUndoDoneState: (loop: LoopBase) -> Unit,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,

        ) {
        SummaryItemTitle(
            modifier = Modifier.weight(1f),
            title = loop.title
        )

        if (LocalConfiguration.current.isLargeScreen()) {
            SummaryItemStartAndEndTime(
                modifier = Modifier.padding(start = 4.dp),
                loopStart = loop.loopStart,
                loopEnd = loop.loopEnd
            )
        }

        LoopCardColor(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(end = 12.dp)
                .size(8.dp),
            color = loop.color
        )

        SummaryItemDoneStateButton(
            loop = loop,
            onUndoDonState = onUndoDoneState
        )
    }
}

@Composable
private fun SummaryItemTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        style = MaterialTheme.typography.body1.copy(
            color = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.medium
            ),
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun SummaryItemStartAndEndTime(
    modifier: Modifier = Modifier,
    loopStart: Long,
    loopEnd: Long
) {
    Text(
        modifier = modifier,
        text = "(${loopStart.formatHourMinute(true)} ~ ${loopEnd.formatHourMinute(true)})",
        style = MaterialTheme.typography.body2.copy(
            color = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.disabled
            ),
        )
    )
}

@Composable
private fun SummaryItemDoneStateButton(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onUndoDonState: (loop: LoopBase) -> Unit
) {
    Image(
        modifier = modifier
            .clickable { onUndoDonState(loop) }
            .border(
                width = 0.5.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.1f),
                shape = RoundShapes.small
            )
            .clip(shape = RoundShapes.small)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .size(16.dp),
        imageVector = Icons.Filled.Refresh,
        colorFilter = ColorFilter.tint(
            color = MaterialTheme.colors.onSurface.copy(
                alpha = ContentAlpha.medium
            )
        ),
        contentDescription = ""
    )
}