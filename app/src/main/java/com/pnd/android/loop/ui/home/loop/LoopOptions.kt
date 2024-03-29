package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceDark
import com.pnd.android.loop.ui.theme.onSurfaceLight

@Composable
fun LoopOptions(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    title: String,
    color: Color,
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    Row(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.3f),
                shape = RoundShapes.large
            )
            .clip(
                shape = RoundShapes.large
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OptionIcon(
            imageVector = Icons.Outlined.ModeEdit,
            text = stringResource(R.string.edit),
            onClick = onEdit
        )

        OptionIcon(
            imageVector = Icons.Outlined.Delete,
            text = stringResource(R.string.delete),
            onClick = {
                showDeleteDialog = true
                blurState.on()
            }
        )

        Spacer(modifier = Modifier.weight(1f))

        EnableSwitch(
            modifier = Modifier.padding(end = 12.dp),
            enabled = enabled,
            onEnabled = onEnabled,
        )
    }
    if (showDeleteDialog) {
        DeleteLoopDialog(
            loopTitle = title,
            onDismiss = {
                showDeleteDialog = false
                blurState.off()
            },
            onDelete = onDelete
        )
    }
}

@Composable
private fun OptionIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    text: String,
    onClick: (() -> Unit) = {}
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val color = AppColor.onSurface.copy(alpha = if (isSystemInDarkTheme()) 0.7f else 0.5f)
        Icon(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(18.dp),
            painter = rememberVectorPainter(image = imageVector),
            tint = color,
            contentDescription = text
        )

        Text(
            text = text,
            style = AppTypography.labelMedium.copy(
                color = color
            )
        )
    }
}

@Composable
private fun EnableSwitch(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    onEnabled: (Boolean) -> Unit,
) {
    val isDarkTheme = isSystemInDarkTheme()

    Switch(
        modifier = modifier,
        checked = enabled,
        thumbContent = {
            Icon(
                modifier = Modifier
                    .size(SwitchDefaults.IconSize),
                imageVector = if (enabled) Icons.Filled.Check else Icons.Filled.Close,
                contentDescription = null,
            )
        },
        colors = SwitchDefaults.colors(
            checkedTrackColor = AppColor.onSurfaceDark.copy(alpha = if (isDarkTheme) 0.3f else 0.2f),
            checkedBorderColor = if (isDarkTheme) {
                AppColor.onSurfaceDark.copy(0.2f)
            } else {
                AppColor.onSurfaceDark.copy(alpha = 0.9f).compositeOver(
                    AppColor.onSurfaceLight.copy(alpha = 0.5f)
                )
            },
            checkedThumbColor = AppColor.onSurfaceDark.copy(alpha = if (isDarkTheme) 0.5f else 0.8f),
            uncheckedTrackColor = AppColor.onSurfaceLight.copy(alpha = if (isDarkTheme) 0.3f else 0.1f),
            uncheckedBorderColor = AppColor.onSurfaceLight.copy(alpha = if (isDarkTheme) 0.3f else 0.1f),
            uncheckedThumbColor = AppColor.onSurfaceLight.copy(alpha = if (isDarkTheme) 0.3f else 0.1f)
        ),
        onCheckedChange = onEnabled
    )

}
