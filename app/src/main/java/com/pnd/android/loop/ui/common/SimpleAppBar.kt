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
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface


/**
 * 뒤로가기 + 타이틀만 있는 단순 앱바.
 *
 * [titleAlpha]는 본문에 같은 제목을 큰 글씨로 두는 화면(상세 화면)을 위한 것이다. 진입 시점엔 0f 로
 * 두어 같은 이름이 두 번 보이지 않게 하고, 본문 제목이 스크롤로 밀려 사라질 때 1f 로 올려 앱바가
 * 이름을 이어받게 한다. 그런 사정이 없는 화면은 기본값(1f)을 그대로 쓰면 된다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SimpleAppBar(
    modifier: Modifier = Modifier,
    title: String,
    onNavigateUp: () -> Unit,
    titleAlpha: Float = 1f,
    actions: @Composable RowScope.() -> Unit = {},
) {
    TopAppBar(
        modifier = modifier.background(color = AppColor.surface),
        title = {
            Text(
                modifier = Modifier.alpha(titleAlpha),
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
