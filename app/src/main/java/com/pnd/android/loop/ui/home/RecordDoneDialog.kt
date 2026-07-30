package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.AppDialog
import com.pnd.android.loop.ui.common.DialogButtons
import com.pnd.android.loop.ui.home.input.selector.TimeStepperCard
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

/**
 * '완료로 기록' 다이얼로그. 예정된 시간창과 무관하게 완료로 남기되, 시작·종료 시각을 직접
 * 입력할 수 있다. 예정 시각이 있으면 그대로 채워(그 시간에 한 것으로 간주) 저장하기 쉽게 하고,
 * anytime 처럼 예정 시각이 없으면(-1) 현재 시각으로 채운다.
 *
 * 배경 블러는 이 다이얼로그를 여는 쪽(LoopCardWithOption)이 BlurState 로 켜고 끈다. 컨테이너(모양·
 * 배경·헤어라인 외곽선)와 하단 버튼은 공통 [AppDialog]/[DialogButtons]를 그대로 따르므로,
 * 다른 다이얼로그와 같은 위계로 라이트·다크 모두에서 일관되게 읽힌다.
 */
@Composable
fun RecordDoneDialog(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onConfirm: (startInDay: Long, endInDay: Long) -> Unit,
    onDismiss: () -> Unit,
) {
    val nowMs = remember { LocalTime.now().toMs() }
    var startMs by rememberSaveable { mutableStateOf(if (loop.startInDay >= 0) loop.startInDay else nowMs) }
    var endMs by rememberSaveable { mutableStateOf(if (loop.endInDay >= 0) loop.endInDay else nowMs) }
    // 종료가 시작보다 빠르면 소요 시간이 음수가 되므로 저장을 막는다.
    val isValidRange = endMs >= startMs

    AppDialog(modifier = modifier, onDismiss = onDismiss) {
        RecordDoneDialogTitle()

        RecordDoneLoopInfo(
            modifier = Modifier.padding(top = 16.dp),
            loop = loop,
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TimeStepperCard(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.start),
                localTime = startMs.toLocalTime(),
                enabled = true,
                onTimeChanged = { startMs = it.toMs() },
            )
            TimeStepperCard(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.end),
                localTime = endMs.toLocalTime(),
                enabled = true,
                onTimeChanged = { endMs = it.toMs() },
            )
        }

        if (!isValidRange) {
            Text(
                modifier = Modifier.padding(top = 10.dp),
                text = stringResource(id = R.string.loop_record_done_invalid_time),
                style = AppTypography.labelMedium.copy(color = AppColor.error),
            )
        }

        // 종료가 시작보다 빠르면(!isValidRange) 확인을 비활성화해 잘못된 기록을 막는다.
        DialogButtons(
            confirmText = stringResource(id = R.string.done),
            confirmEnabled = isValidRange,
            onConfirm = {
                onConfirm(startMs, endMs)
                onDismiss()
            },
            onCancel = onDismiss,
        )
    }
}

@Composable
private fun RecordDoneDialogTitle(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color = AppColor.primary.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Filled.Done,
                tint = AppColor.primary,
                contentDescription = null,
            )
        }
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = stringResource(id = R.string.loop_record_done_title),
            style = AppTypography.titleLarge.copy(color = AppColor.onSurface),
        )
    }
}

/** 다이얼로그 상단의 대상 루프 표시(색 도트 + 제목). 어떤 루프를 완료로 남기는지 확인시켜 준다. */
@Composable
private fun RecordDoneLoopInfo(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoopCardColor(
            modifier = Modifier
                .padding(end = 10.dp)
                .size(8.dp),
            color = loop.color,
        )
        Text(
            modifier = Modifier.weight(1f),
            text = loop.title,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.SemiBold,
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
