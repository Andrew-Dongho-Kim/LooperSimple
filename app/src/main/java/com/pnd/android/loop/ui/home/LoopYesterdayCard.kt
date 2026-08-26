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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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
import com.pnd.android.loop.ui.theme.surfaceContainer
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.pnd.android.loop.util.annotatedString
import java.time.LocalDate

/** '완료로 기록' 다이얼로그의 대상 loopId 가 이 값이면 대상이 없다는 뜻이라 다이얼로그를 띄우지 않는다. */
private const val NO_RECORD_DONE_TARGET = -1

@Composable
fun LoopYesterdayCard(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    snackBarHostState: SnackbarHostState,
    isExpanded: Boolean,
    onExpandChanged: (isExpanded: Boolean) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val yesterday = LocalDate.now().minusDays(1)

    // 다이얼로그로 확인받는 대신, 요청 즉시 상태를 바꾸고 스낵바로 알린다.
    // 스낵바의 "실행취소"를 누르면 어제 상태를 미응답(NO_RESPONSE)으로 되돌린다.
    val onAction: (LoopBase, Int) -> Unit = { loop, doneState ->
        loopViewModel.changeLoopState(
            loop = loop,
            localDate = yesterday,
            doneState = doneState
        )

        val messageRes = if (doneState == LoopDoneVo.DoneState.DONE) {
            R.string.done_snack_message
        } else {
            R.string.skip_snack_message
        }
        scope.launch {
            // 새 동작이 오면 이전 스낵바는 대체되도록 먼저 정리한다.
            snackBarHostState.currentSnackbarData?.dismiss()
            val result = snackBarHostState.showSnackbar(
                message = context.getString(messageRes, loop.title),
                actionLabel = context.getString(R.string.action_undo),
                duration = SnackbarDuration.Short,
            )
            if (result == SnackbarResult.ActionPerformed) {
                loopViewModel.changeLoopState(
                    loop = loop,
                    localDate = yesterday,
                    doneState = LoopDoneVo.DoneState.NO_RESPONSE
                )
            }
        }
    }

    // 시간 미지정(anytime) 루프는 완료로 남길 시각이 루프에 없다. 그래서 Done 을 누르면 바로
    // 기록하지 않고 시작·종료를 입력받는다. 대상은 회전 등으로 재구성돼도 유지되도록 id 만
    // 저장하고, 실제 루프는 목록에서 찾아 쓴다(목록에서 사라지면 다이얼로그도 함께 닫힌다).
    var recordDoneLoopId by rememberSaveable { mutableIntStateOf(NO_RECORD_DONE_TARGET) }
    val recordDoneLoop = loops.firstOrNull { loop -> loop.loopId == recordDoneLoopId }
    if (recordDoneLoop != null) {
        // 다이얼로그가 떠 있는 동안만 배경을 흐리게 한다. onDispose 에서 반드시 되돌리므로
        // 마지막 항목에 응답해 카드가 목록에서 사라져도 화면이 흐린 채 남지 않는다.
        DisposableEffect(Unit) {
            blurState.on()
            onDispose { blurState.off() }
        }

        RecordDoneDialog(
            loop = recordDoneLoop,
            // 입력받은 시각은 어제 행에 남겨야 하므로, 그 시각을 실은 루프로 상태를 바꾼다.
            onConfirm = { startInDay, endInDay ->
                onAction(
                    recordDoneLoop.copyAs(startInDay = startInDay, endInDay = endInDay),
                    LoopDoneVo.DoneState.DONE,
                )
            },
            onDismiss = { recordDoneLoopId = NO_RECORD_DONE_TARGET },
        )
    }

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
                        // 시간 미지정 루프만 시각을 물어보고, 시간제 루프는 예정 시각이 그대로
                        // 기록되므로 지금까지처럼 한 번에 완료로 남긴다.
                        onRequestDone = {
                            if (loop.isAnyTime) {
                                recordDoneLoopId = loop.loopId
                            } else {
                                onAction(loop, LoopDoneVo.DoneState.DONE)
                            }
                        },
                        onRequestSkip = { onAction(loop, LoopDoneVo.DoneState.SKIP) },
                        onNavigateToDetailPage = onNavigateToDetailPage,
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

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