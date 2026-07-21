package com.pnd.android.loop.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.input.selector.TimeStepperCard
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

/**
 * '완료로 기록' 다이얼로그. 예정된 시간창과 무관하게 완료로 남기되, 시작·종료 시각을 직접
 * 입력할 수 있다. 예정 시각이 있으면 그대로 채워(그 시간에 한 것으로 간주) 저장하기 쉽게 하고,
 * anytime 처럼 예정 시각이 없으면(-1) 현재 시각으로 채운다.
 *
 * 배경 블러는 이 다이얼로그를 여는 쪽(LoopCardWithOption)이 BlurState 로 켜고 끈다. 여기서는
 * 블러된 배경 위에서 경계가 또렷하도록 헤어라인 외곽선을 두른다. 색은 모두 AppColor 토큰이라
 * 라이트·다크 모두에서 같은 위계로 읽힌다.
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

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundShapes.large,
            color = AppColor.surfaceElevated,
            // 카드·팝업 메뉴와 같은 헤어라인 외곽선. 블러된 배경 위에서도 경계를 또렷하게 잡아준다.
            border = BorderStroke(width = 1.dp, color = AppColor.outlineVariant),
            tonalElevation = 0.dp,
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
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

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            style = AppTypography.titleMedium.copy(
                                color = AppColor.onSurface.copy(alpha = 0.6f),
                            ),
                        )
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                    TextButton(
                        enabled = isValidRange,
                        onClick = {
                            onConfirm(startMs, endMs)
                            onDismiss()
                        },
                    ) {
                        Text(
                            text = stringResource(id = R.string.done),
                            style = AppTypography.titleMedium.copy(
                                // 저장 불가 상태에서는 확인 버튼을 흐리게 낮춰 비활성임을 알린다.
                                color = if (isValidRange) {
                                    AppColor.primary
                                } else {
                                    AppColor.onSurface.copy(alpha = 0.3f)
                                },
                                fontWeight = FontWeight.Bold,
                            ),
                        )
                    }
                }
            }
        }
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
