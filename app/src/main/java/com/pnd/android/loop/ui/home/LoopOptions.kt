package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceDark
import com.pnd.android.loop.ui.theme.onSurfaceLight

@Composable
fun LoopOptions(
    modifier: Modifier = Modifier,
    color: Color,
    enabled: Boolean,
    onShowDeleteDialog: (Boolean) -> Unit,
    onEnabledLoop: (Boolean) -> Unit,
    onEditLoop: () -> Unit,
) {
    // 카드와 동일한 라운드(16dp)를 써서, 스와이프로 드러날 때 카드 모양과 어긋나지 않게 한다.
    val optionsShape = RoundedCornerShape(16.dp)
    Row(
        modifier = modifier
            // 반투명 원색 대신 표면 위에 합성한 은은한 루프 색 틴트 → 다크/라이트 모두에서 깔끔하게 읽힌다.
            .clip(optionsShape)
            .background(color = color.compositeOverSurface(alpha = 0.14f)),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OptionIcon(
            imageVector = Icons.Outlined.ModeEdit,
            text = stringResource(R.string.edit),
            onClick = onEditLoop
        )

        OptionIcon(
            imageVector = Icons.Outlined.Delete,
            text = stringResource(R.string.delete),
            onClick = { onShowDeleteDialog(true) }
        )

        Spacer(modifier = Modifier.weight(1f))

        LoopOnOffSwitch(
            modifier = Modifier.padding(end = 12.dp),
            enabled = enabled,
            onEnabled = onEnabledLoop,
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
fun LoopOnOffSwitch(
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
