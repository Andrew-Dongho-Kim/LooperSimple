package com.pnd.android.loop.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated

/** 홈 탭과 통계 필터가 공유하는 선택 표현. 높이는 최소값이므로 큰 글꼴에서도 확장된다. */
@Composable
fun <T> AppSegmentedControl(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    minHeight: Dp = Dimens.selectionTrackHeight,
) {
    val selectedIndex = options.indexOf(selected)
    // 선택 표시는 칸마다 하나씩 두지 않고, 트랙 위를 옮겨 다니는 알약 하나로 그린다.
    // 칸마다 배경을 각각 크로스페이드하면 전환하는 동안 두 칸이 동시에 물들어, 누르지 않은
    // 쪽에도 터치 피드백이 뜬 것처럼 보였다. 알약이 하나면 강조되는 칸도 항상 하나뿐이다.
    val thumbPosition by animateFloatAsState(
        targetValue = selectedIndex.coerceAtLeast(0).toFloat(),
        label = "segmentThumb",
    )
    Box(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .clip(CircleShape)
            .background(AppColor.surfaceContainer)
            .padding(3.dp),
    ) {
        // 알약은 트랙 크기에 맞춘 레이어 안에서 한 칸 너비를 차지하고, 선택된 칸만큼 옆으로
        // 밀린다. matchParentSize라 트랙 높이는 아래 Row가 정한다.
        if (selectedIndex >= 0) {
            Box(modifier = Modifier.matchParentSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(1f / options.size)
                        .graphicsLayer { translationX = size.width * thumbPosition }
                        .clip(CircleShape)
                        .background(AppColor.surfaceElevated)
                        .border(1.dp, AppColor.outlineVariant, CircleShape),
                )
            }
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                AppSegment(
                    modifier = Modifier.weight(1f),
                    text = label(option),
                    selected = option == selected,
                    onClick = { onSelected(option) },
                    minHeight = minHeight - 6.dp,
                )
            }
        }
    }
}

@Composable
private fun AppSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    minHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val ink by animateColorAsState(
        if (selected) AppColor.primary else AppColor.onSurfaceVariant,
        label = "segmentInk",
    )
    // 배경은 알약이 그리므로 여기서는 글자와 터치 영역만 맡는다. clip이 리플을 자기 칸 안에
    // 가둔다.
    Box(
        modifier = modifier
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = minHeight)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppTypography.bodyMedium, color = ink, textAlign = TextAlign.Center)
    }
}
