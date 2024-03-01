package com.pnd.android.loop.ui.detail

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailAppBar(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    onNavigateUp: () -> Unit,
) {
    TopAppBar(
        modifier = modifier,
        title = {
            Text(
                text = loop.title,
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
                descriptionResId = R.string.about_app,
                onClick = onNavigateUp
            )
        }

    )
}