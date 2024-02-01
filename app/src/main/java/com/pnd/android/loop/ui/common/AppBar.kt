package com.pnd.android.loop.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.onSurface


@Composable
fun AppBar(
    modifier: Modifier = Modifier,
    navigationIcon: @Composable (() -> Unit)? = null,
    title: @Composable RowScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Column(
        modifier = modifier,
    ) {
        TopAppBar(
            backgroundColor = Color.Transparent,
            elevation = 0.dp, // No shadow needed
            contentColor = AppColor.onSurface,
            navigationIcon = navigationIcon,
            actions = actions,
            title = { Row { title() } },
        )
    }
}

@Composable
fun AppBarIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    color: Color,
    @StringRes descriptionResId: Int,
    onClick: () -> Unit = {}
) = Icon(
    modifier = modifier
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 16.dp)
        .height(24.dp),
    imageVector = imageVector,
    tint = color,
    contentDescription = stringResource(descriptionResId)
)