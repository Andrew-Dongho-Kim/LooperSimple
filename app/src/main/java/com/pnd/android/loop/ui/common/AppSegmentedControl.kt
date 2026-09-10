package com.pnd.android.loop.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.graphics.Color
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
    Row(
        modifier = modifier
            .fillMaxWidth()
            .selectableGroup()
            .clip(CircleShape)
            .background(AppColor.surfaceContainer)
            .padding(3.dp),
    ) {
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

@Composable
private fun AppSegment(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    minHeight: Dp,
    modifier: Modifier = Modifier,
) {
    val fill by animateColorAsState(
        if (selected) AppColor.surfaceElevated else Color.Transparent,
        label = "segmentFill",
    )
    val ink by animateColorAsState(
        if (selected) AppColor.primary else AppColor.onSurfaceVariant,
        label = "segmentInk",
    )
    Box(
        modifier = modifier
            .clip(CircleShape)
            .background(fill)
            .border(1.dp, if (selected) AppColor.outlineVariant else Color.Transparent, CircleShape)
            .selectable(selected = selected, role = Role.Tab, onClick = onClick)
            .heightIn(min = minHeight)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, style = AppTypography.bodyMedium, color = ink, textAlign = TextAlign.Center)
    }
}
