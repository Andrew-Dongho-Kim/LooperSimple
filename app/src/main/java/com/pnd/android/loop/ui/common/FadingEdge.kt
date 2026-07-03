package com.pnd.android.loop.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.unit.Dp
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.background

/**
 * 상태바 높이만큼의 세로 그라데이션 오버레이. 위쪽(상태바)은 테마 배경색(불투명),
 * 아래로 갈수록 투명해지도록 그려서 스크롤되는 콘텐츠가 상태바 밑으로 부드럽게 사라지게 한다.
 *
 * 홈과 기록 화면 등 상태바 뒤로 콘텐츠가 스크롤되는 화면에서 공통으로 사용한다.
 */
@Composable
fun StatusBarFadingEdge(
    modifier: Modifier = Modifier,
) {
    val statusBarHeight = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    SystemBarFadingEdge(
        modifier = modifier,
        height = statusBarHeight,
        opaqueAtTop = true,
    )
}

/**
 * 내비게이션 바 높이만큼의 세로 그라데이션 오버레이. 아래쪽(내비게이션 바)은 테마 배경색(불투명),
 * 위로 갈수록 투명해지도록 그려서 스크롤되는 콘텐츠가 내비게이션 바 밑으로 부드럽게 사라지게 한다.
 */
@Composable
fun NavigationBarFadingEdge(
    modifier: Modifier = Modifier,
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    SystemBarFadingEdge(
        modifier = modifier,
        height = navigationBarHeight,
        opaqueAtTop = false,
    )
}

/**
 * 상태바/내비게이션 바 공용 페이딩 엣지. 시스템 바에 가까운 쪽을 불투명하게, 콘텐츠 쪽을
 * 투명하게 그린다. 색은 [AppColor.background](= MaterialTheme.colorScheme.background)에서
 * 가져오므로 라이트/다크 모드에 자동으로 대응한다.
 *
 * @param opaqueAtTop true면 위쪽(상태바), false면 아래쪽(내비게이션 바)이 불투명해진다.
 */
@Composable
fun SystemBarFadingEdge(
    modifier: Modifier,
    height: Dp,
    opaqueAtTop: Boolean,
) {
    val backgroundColor = AppColor.background
    val transparent = backgroundColor.copy(alpha = 0f)
    val gradientColors = if (opaqueAtTop) {
        listOf(backgroundColor, transparent)
    } else {
        listOf(transparent, backgroundColor)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(brush = Brush.verticalGradient(colors = gradientColors)),
    )
}
