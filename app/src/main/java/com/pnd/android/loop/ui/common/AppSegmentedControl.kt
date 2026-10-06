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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
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

/**
 * 홈 탭과 통계 필터가 공유하는 선택 표현. 높이는 최소값이므로 큰 글꼴에서도 확장된다.
 *
 * @param menuLabel 칸에 하위 선택(메뉴)이 있으면 그 동작 이름(예: "월 선택")을, 없으면 null을 돌려준다.
 *   메뉴가 있는 칸은 선택된 상태에서만 ▾를 붙이고, 그 상태에서 다시 누르면 [onMenuClick]을 부른다.
 *   선택되지 않은 칸을 누르면 메뉴 유무와 관계없이 [onSelected]로 선택만 바꾼다. 한 번의 탭이
 *   두 가지 일을 하지 않도록, 메뉴는 이미 선택된 칸에서만 열린다.
 * @param description 화면 낭독기가 읽을 칸 이름. null이면 [label]을 읽는다.
 */
@Composable
fun <T> AppSegmentedControl(
    options: List<T>,
    selected: T,
    onSelected: (T) -> Unit,
    label: @Composable (T) -> String,
    modifier: Modifier = Modifier,
    minHeight: Dp = Dimens.selectionTrackHeight,
    menuLabel: @Composable (T) -> String? = { null },
    onMenuClick: (T) -> Unit = {},
    description: @Composable (T) -> String? = { null },
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
                val isSelected = option == selected
                // 메뉴는 선택된 칸에서만 연다. 선택 안 된 칸의 메뉴 이름은 쓰지 않는다.
                val activeMenuLabel = menuLabel(option)?.takeIf { isSelected }
                AppSegment(
                    modifier = Modifier.weight(1f),
                    text = label(option),
                    description = description(option),
                    selected = isSelected,
                    menuLabel = activeMenuLabel,
                    onClick = {
                        if (activeMenuLabel != null) onMenuClick(option) else onSelected(option)
                    },
                    minHeight = minHeight - 6.dp,
                )
            }
        }
    }
}

@Composable
private fun AppSegment(
    text: String,
    description: String?,
    selected: Boolean,
    menuLabel: String?,
    onClick: () -> Unit,
    minHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val ink by animateColorAsState(
        if (selected) AppColor.primary else AppColor.onSurfaceVariant,
        label = "segmentInk",
    )
    // semantics 블록 안의 onClick(label, action)과 이름이 겹치지 않도록 따로 잡아 둔다.
    val handleClick = onClick
    // 배경은 알약이 그리므로 여기서는 글자와 터치 영역만 맡는다. clip이 리플을 자기 칸 안에
    // 가둔다.
    Box(
        modifier = modifier
            .clip(CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            // 메뉴가 열리는 칸은 낭독기가 "두 번 탭하여 <메뉴 이름>"으로 안내하도록 동작 이름을 붙인다.
            .semantics(mergeDescendants = true) {
                if (description != null) contentDescription = description
                if (menuLabel != null) {
                    onClick(label = menuLabel) { handleClick(); true }
                }
            }
            .heightIn(min = minHeight)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f, fill = false),
                text = text,
                style = AppTypography.bodyMedium,
                color = ink,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (menuLabel != null) {
                // 선택된 칸을 다시 누르면 하위 선택이 열린다는 표시.
                Icon(
                    modifier = Modifier.size(18.dp),
                    imageVector = Icons.Outlined.KeyboardArrowDown,
                    contentDescription = null,
                    tint = ink,
                )
            }
        }
    }
}
