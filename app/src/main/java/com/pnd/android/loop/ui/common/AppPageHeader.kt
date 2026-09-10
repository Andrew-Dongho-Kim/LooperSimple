package com.pnd.android.loop.ui.common

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.onSurface

// 고정 제목 아래로 본문이 들어오기 전에 헤더 배경을 완전히 드러낸다.
private const val HeaderSurfaceRevealFraction = 0.25f

/** 상태바 여백을 한 번만 적용하고, 스크롤해도 제목과 액션 위치를 유지하는 하위 화면 헤더. */
@Composable
fun AppPageHeader(
    title: String,
    onNavigateUp: () -> Unit,
    progress: Float,
    backdrop: BackdropState?,
    modifier: Modifier = Modifier,
    titleLeading: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topInset),
    ) {
        FloatingSurface(
            modifier = Modifier
                .floatingHeaderPadding()
                .fillMaxWidth(),
            progress = (progress / HeaderSurfaceRevealFraction).coerceIn(0f, 1f),
            shape = FloatingHeaderShape,
            backdrop = backdrop,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(Dimens.appBarHeight),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppBarIcon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    color = AppColor.onSurface,
                    descriptionResId = R.string.navi_up,
                    onClick = onNavigateUp,
                )
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 4.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    titleLeading()
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = AppTypography.titleLarge,
                        color = AppColor.onSurface,
                    )
                }
                actions()
            }
        }
    }
}
