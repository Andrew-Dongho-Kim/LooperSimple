package com.pnd.android.loop.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAppBar(
    modifier: Modifier = Modifier,
    title: String,
    onNavigateUp: () -> Unit,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier.background(color = AppColor.surface),
        title = {
            Text(
                text = title,
                style = AppTypography.headlineSmall.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Normal
                )
            )
        },
        navigationIcon = {
            AppBarIcon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                color = AppColor.onSurface,
                descriptionResId = R.string.navi_up,
                onClick = onNavigateUp
            )
        },
        actions = actions
    )
}
