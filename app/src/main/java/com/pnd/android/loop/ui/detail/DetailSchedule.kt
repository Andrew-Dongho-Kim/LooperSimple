package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer

@Composable
internal fun LoopEnabledSwitch(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    val stateLabel = stringResource(
        if (enabled) R.string.detail_loop_state_active else R.string.detail_loop_state_inactive,
    )
    val controlLabel = stringResource(R.string.detail_schedule_enabled)
    Column(
        modifier = Modifier
            .clip(RoundShapes.medium)
            .toggleable(value = enabled, role = Role.Switch, onValueChange = onEnabledChange)
            .semantics(mergeDescendants = true) {
                contentDescription = controlLabel
                stateDescription = stateLabel
            }
            .sizeIn(minWidth = MinTouchTarget, minHeight = MinTouchTarget)
            .padding(horizontal = DetailSpacing.related),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            stateLabel,
            style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.7f)),
        )
        Switch(
            checked = enabled,
            onCheckedChange = null,
            colors = SwitchDefaults.colors(
                checkedTrackColor = AppColor.primary,
                checkedThumbColor = AppColor.onPrimary,
                uncheckedTrackColor = AppColor.surfaceContainer,
                uncheckedThumbColor = AppColor.onSurface.copy(alpha = 0.6f),
                uncheckedBorderColor = AppColor.onSurface.copy(alpha = 0.3f),
            ),
        )
    }
}

@Composable
internal fun ScheduleSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(
        modifier = modifier
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .sizeIn(minHeight = MinTouchTarget)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text,
            style = AppTypography.labelLarge.copy(
                color = AppColor.onSurface.copy(alpha = if (enabled) 1f else 0.4f),
            ),
        )
    }
}
