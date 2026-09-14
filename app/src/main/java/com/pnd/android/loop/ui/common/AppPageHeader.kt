package com.pnd.android.loop.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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

/** 알약끼리 맞붙어 하나로 보이지 않도록 두는 간격. */
private val PillSpacing = 8.dp

/** 제목 알약의 좌우 여백. 둥근 끝에 글자가 붙지 않도록 한다. */
private val TitlePillPadding = 16.dp

/** 액션 알약의 좌우 여백. 홈의 액션 아이콘 알약과 같은 값을 쓴다. */
private val ActionsPillPadding = 8.dp

/**
 * 상태바 여백을 한 번만 적용하고, 스크롤해도 제목과 액션 위치를 유지하는 하위 화면 헤더.
 *
 * 홈([com.pnd.android.loop.ui.home.CollapsingHomeHeader])과 같은 방식으로, 하나의 넓은 표면
 * 대신 뒤로가기 / 제목 / 액션이 각자 독립된 플로팅 알약을 갖는다. 알약 사이로 본문이 그대로
 * 지나가므로 헤더가 콘텐츠를 가로지르는 띠처럼 보이지 않는다.
 *
 * [actions]가 null이면 액션 알약 자체를 그리지 않는다. 비어 있는 람다를 넘기면 내용 없는 알약이
 * 오른쪽에 덩그러니 떠 보이므로, 표시할 액션이 없을 때는 null을 넘겨야 한다.
 */
@Composable
fun AppPageHeader(
    title: String,
    onNavigateUp: () -> Unit,
    progress: Float,
    backdrop: BackdropState?,
    modifier: Modifier = Modifier,
    titleLeading: @Composable () -> Unit = {},
    actions: (@Composable RowScope.() -> Unit)? = null,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 세 알약이 같은 타이밍에 함께 떠오르도록 진행도는 한 번만 계산해 나눠 쓴다.
    val surfaceProgress = (progress / HeaderSurfaceRevealFraction).coerceIn(0f, 1f)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = topInset)
            .floatingHeaderPadding()
            // 헤더가 차지하는 높이는 예전과 같게 두어, 화면들이 계산해 둔 콘텐츠 상단 여백
            // (topInset + appBarHeight)이 그대로 들어맞게 한다. 알약은 이 행 안에서 세로 중앙.
            .height(Dimens.appBarHeight),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(PillSpacing),
    ) {
        // 뒤로가기: 아이콘 하나만 담는 원형 알약.
        FloatingSurface(
            progress = surfaceProgress,
            shape = FloatingHeaderShape,
            backdrop = backdrop,
        ) {
            Box(
                modifier = Modifier.size(FloatingPillHeight),
                contentAlignment = Alignment.Center,
            ) {
                AppBarIcon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    color = AppColor.onSurface,
                    descriptionResId = R.string.navi_up,
                    onClick = onNavigateUp,
                )
            }
        }

        // 제목: 남은 폭 안에서 글자 길이만큼만 차지하는 알약. 남는 공간은 감싼 Box가 가져가므로
        // 제목이 짧아도 알약이 늘어나지 않고, 액션은 오른쪽 끝에 붙는다.
        Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterStart,
        ) {
            FloatingSurface(
                progress = surfaceProgress,
                shape = FloatingHeaderShape,
                backdrop = backdrop,
            ) {
                Row(
                    modifier = Modifier
                        .height(FloatingPillHeight)
                        .padding(horizontal = TitlePillPadding),
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
            }
        }

        if (actions != null) {
            FloatingSurface(
                progress = surfaceProgress,
                shape = FloatingHeaderShape,
                backdrop = backdrop,
            ) {
                Row(
                    modifier = Modifier
                        .height(FloatingPillHeight)
                        .padding(horizontal = ActionsPillPadding),
                    verticalAlignment = Alignment.CenterVertically,
                    content = actions,
                )
            }
        }
    }
}
