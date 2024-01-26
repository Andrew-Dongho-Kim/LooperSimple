package com.pnd.android.loop.ui.detail

import androidx.compose.material.ContentAlpha
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBackIos
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.navigation.compose.rememberNavController
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.AppBar
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface

@Composable
fun DetailAppBar(
    modifier: Modifier = Modifier,
    loop: LoopBase
) {
    AppBar(
        modifier = modifier,
        title = {
            Text(
                text = loop.title,
                style = AppTypography.h6.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Normal
                )
            )
        },
        navigationIcon = {
            AppBarIcon(
                imageVector = Icons.Outlined.ArrowBackIos,
                color = AppColor.onSurface.copy(alpha = ContentAlpha.medium),
                descriptionResId = R.string.about_app,
                onClick = { }
            )
        }

    )
}