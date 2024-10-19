package com.pnd.android.loop.ui.home.loop

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
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
    Column(
        modifier = modifier
            .padding(vertical = 16.dp)
            .padding(horizontal = 24.dp)
            .background(
                color = AppColor.error.copy(alpha = 0.1f),
                shape = RoundShapes.medium
            )
            .animateContentSize()
    ) {
        LoopYesterdayHeader(
            count = loops.size,
            isExpanded = isExpanded,
            onExpandChanged = onExpandChanged
        )

        if (isExpanded) {
            loops.forEachIndexed { index, loop ->
                LoopYesterdayItem(
                    modifier = if (index == 0) Modifier.padding(top = 12.dp) else Modifier,
                    loop = loop,
                    onDone = { done ->
                        loopViewModel.doneLoop(
                            loop = loop,
                            localDate = LocalDate.now().minusDays(1),
                            doneState = if (done) LoopDoneVo.DoneState.DONE else LoopDoneVo.DoneState.SKIP
                        )
                    },
                    onNavigateToDetailPage = onNavigateToDetailPage,
                )
            }
        }
    }
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
            .clip(shape = RoundShapes.medium)
            .clickable { onExpandChanged(!isExpanded) }
            .padding(all = 8.dp)
            .padding(start = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = annotatedString(stringResource(id = R.string.unchecked_loops, count)),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )

        Box(modifier = Modifier.weight(1f))

        val rotation by animateFloatAsState(
            targetValue = if (isExpanded) -180f else 0f,
            animationSpec = tween(500),
            label = ""
        )

        Image(
            modifier = Modifier.graphicsLayer {
                rotationX = rotation
            },
            imageVector = Icons.Rounded.ExpandMore,
            colorFilter = ColorFilter.tint(color = AppColor.onSurface),
            contentDescription = ""
        )
    }
}

@Composable
private fun LoopYesterdayItem(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onDone: (Boolean) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    Row(
        modifier = modifier
            .padding(
                vertical = 8.dp,
                horizontal = 24.dp
            )
            .clickable {
                onNavigateToDetailPage(loop)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoopTitle(title = loop.title)
        Spacer(modifier = Modifier.weight(1f))
        LoopDoneOrSkip(
            modifier = Modifier.height(36.dp),
            onDone = onDone,
        )
    }
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