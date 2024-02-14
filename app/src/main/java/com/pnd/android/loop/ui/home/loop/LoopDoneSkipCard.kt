package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.formatHourMinute

@Composable
fun LoopDoneSkipCard(
    modifier: Modifier = Modifier,
    section: Section,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
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
        DoneSkipContent(
            loops = loopGroup[DoneState.DONE] ?: emptyList(),
            title = stringResource(id = R.string.done),
            icon = Icons.Filled.Done,
            iconColor = AppColor.primary,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            onUndoDoneState = onUndoDoneState,
        )

        DoneSkipContent(
            modifier = Modifier.padding(top = 12.dp),
            loops = loopGroup[DoneState.SKIP] ?: emptyList(),
            title = stringResource(id = R.string.skip),
            icon = Icons.Filled.Clear,
            iconColor = AppColor.onSurface,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            onUndoDoneState = onUndoDoneState,
        )
    }
}

@Composable
private fun DoneSkipContent(
    modifier: Modifier = Modifier,
    loops: List<LoopBase>,
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onUndoDoneState: (loop: LoopBase) -> Unit,
) {
    if (loops.isEmpty()) return

    Column(
        modifier = modifier.padding(
            horizontal = 12.dp,
            vertical = 8.dp
        )
    ) {
        DoneSkipHeader(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            title = title,
            itemCount = loops.size,
            icon = icon,
            iconColor = iconColor,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )
        Column(
            modifier = Modifier.padding(top = 4.dp)
        ) {
            loops.forEach { loop ->
                key(loop.id) {
                    DoneSkipItem(
                        loop = loop,
                        onNavigateToDetailPage = onNavigateToDetailPage,
                        onUndoDoneState = onUndoDoneState,
                    )
                }
            }
        }
    }

}

@Composable
private fun DoneSkipHeader(
    modifier: Modifier = Modifier,
    title: String,
    itemCount: Int,
    icon: ImageVector,
    iconColor: Color,
    onNavigateToHistoryPage: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(bottom = 8.dp)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.1f),
                shape = RoundShapes.medium
            )
            .clip(RoundShapes.medium)
            .clickable(onClick = onNavigateToHistoryPage)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "($itemCount) $title",
            style = MaterialTheme.typography.subtitle1.copy(
                color = AppColor.onSurface,
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
private fun DoneSkipItem(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onUndoDoneState: (loop: LoopBase) -> Unit,
) {
    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable { onNavigateToDetailPage(loop) },
        verticalAlignment = Alignment.CenterVertically,

        ) {
        DoneSkipItemTitle(
            modifier = Modifier.weight(1f),
            title = loop.title
        )

        if (LocalConfiguration.current.isLargeScreen()) {
            DoneSkipItemStartAndEndTime(
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

        DoneSkipItemDoneStateButton(
            loop = loop,
            onUndoDonState = onUndoDoneState
        )
    }
}

@Composable
private fun DoneSkipItemTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        style = MaterialTheme.typography.body1.copy(
            color = AppColor.onSurface,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DoneSkipItemStartAndEndTime(
    modifier: Modifier = Modifier,
    loopStart: Long,
    loopEnd: Long
) {
    Text(
        modifier = modifier,
        text = "(${loopStart.formatHourMinute(withAmPm = true)} ~ ${
            loopEnd.formatHourMinute(
                withAmPm = true
            )
        })",
        style = MaterialTheme.typography.body2.copy(
            color = AppColor.onSurface.copy(
                alpha = ContentAlpha.disabled
            ),
        )
    )
}

@Composable
private fun DoneSkipItemDoneStateButton(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onUndoDonState: (loop: LoopBase) -> Unit
) {
    Image(
        modifier = modifier
            .clickable { onUndoDonState(loop) }
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.1f),
                shape = RoundShapes.small
            )
            .clip(shape = RoundShapes.small)
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .size(16.dp),
        imageVector = Icons.Filled.Refresh,
        colorFilter = ColorFilter.tint(
            color = AppColor.onSurface.copy(
                alpha = ContentAlpha.medium
            )
        ),
        contentDescription = stringResource(R.string.restore)
    )
}