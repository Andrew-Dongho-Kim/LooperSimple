package com.pnd.android.loop.ui.home

import androidx.annotation.IntDef
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated

/**
 * Which set of loops (and which scope of stats) the home screen is showing. Kept as a small
 * standalone type — rather than buried in a list section — because the tab now lives pinned
 * under the app bar and its selection is shared by both the header card and the loop list.
 */
object HomeTab {
    const val ALL = 0
    const val TODAY = 1

    @IntDef(ALL, TODAY)
    annotation class Type
}

/** Default height of the segmented control's track, tuned to sit comfortably under the app bar. */
val HomeTabsTrackHeight = 38.dp

// 트랙과 썸(thumb) 모두 완전한 알약(pill) 형태로 둔다. 이렇게 하면 스크롤 시 트랙이
// 캡슐 모양의 FloatingSurface 안으로 접혀 들어갈 때 두 곡률이 어긋나지 않고 자연스럽게 겹친다.
private val TrackShape = RoundedCornerShape(percent = 50)
private val ThumbShape = RoundedCornerShape(percent = 50)

/** 트랙 안쪽 여백 — 썸이 트랙 테두리에서 살짝 떠 있는 듯한 인셋을 만든다. */
private val TrackPadding = 3.dp

/** 누르는 순간 썸이 살짝 눌리는 정도. 값이 작을수록 미세하고 세련된 반응이 된다. */
private const val PressedThumbScale = 0.94f

/**
 * iOS 스타일 세그먼트 컨트롤. 오늘/전체 사이에서 하나의 둥근 "썸"이 미끄러지듯 이동하며,
 * 각 세그먼트가 개별로 채워지는 방식보다 모던한 플랫폼 컨트롤처럼 읽힌다.
 *
 * 세련미를 위해 손본 부분:
 * - 알약 형태의 트랙/썸으로 Floating(접힘) 모드의 캡슐과 매끈하게 어우러진다.
 * - 썸 이동에 스프링을 적용해 탄력 있고 자연스럽게 안착한다.
 * - 탭을 누르면 썸이 살짝 눌려 촉각적인 피드백을 준다.
 * - 다크 모드에서는 그림자가 보이지 않으므로 얇은 테두리로 썸의 경계를 살린다.
 * - 선택된 라벨은 primary 색으로 은은하게 강조해 현재 위치를 분명히 한다.
 *
 * 색은 모두 테마에서 가져오므로 라이트 모드에서는 떠 있는 흰색 알약, 다크 모드에서는
 * 한 단계 밝은 표면으로 자연스럽게 대응한다.
 */
@Composable
fun HomeTabs(
    modifier: Modifier = Modifier,
    @HomeTab.Type selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    trackHeight: Dp = HomeTabsTrackHeight,
) {
    val isDark = isSystemInDarkTheme()

    // 각 세그먼트의 눌림 상태를 상위에서 모아 썸의 눌림 애니메이션에 함께 반영한다.
    val todayInteraction = remember { MutableInteractionSource() }
    val allInteraction = remember { MutableInteractionSource() }
    val todayPressed by todayInteraction.collectIsPressedAsState()
    val allPressed by allInteraction.collectIsPressedAsState()
    val pressed = todayPressed || allPressed

    // 썸은 TODAY(첫 번째)에서 왼쪽, ALL(두 번째)에서 오른쪽에 놓인다.
    // 스프링으로 이동시켜 tween보다 탄력 있고 부드럽게 안착시킨다.
    val thumbBias by animateFloatAsState(
        targetValue = if (selectedTab == HomeTab.TODAY) -1f else 1f,
        animationSpec = spring(
            dampingRatio = 0.75f,
            stiffness = Spring.StiffnessMediumLow,
        ),
        label = "segmentedThumbBias",
    )

    // 누르는 동안 썸을 살짝 축소해 촉각적인 반응을 준다.
    val thumbScale by animateFloatAsState(
        targetValue = if (pressed) PressedThumbScale else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
        label = "segmentedThumbScale",
    )

    // 다크 모드에서는 그림자가 사실상 보이지 않으므로 얇은 테두리로 경계를 살리고,
    // 라이트 모드에서는 그림자에 의존하므로 테두리를 아주 옅게만 둔다.
    val thumbBorderColor = AppColor.onSurface.copy(alpha = if (isDark) 0.10f else 0.04f)
    val thumbElevation = if (isDark) 0.dp else 3.dp

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(trackHeight)
            .clip(TrackShape)
            .background(color = AppColor.surfaceContainer)
            .padding(TrackPadding),
    ) {
        // 라벨 뒤에서 정확히 절반을 차지하며 미끄러지는 썸.
        Box(
            modifier = Modifier
                .align(BiasAlignment(horizontalBias = thumbBias, verticalBias = 0f))
                .fillMaxWidth(0.5f)
                .fillMaxHeight()
                .graphicsLayer {
                    scaleX = thumbScale
                    scaleY = thumbScale
                }
                .shadow(elevation = thumbElevation, shape = ThumbShape)
                .background(color = AppColor.surfaceElevated, shape = ThumbShape)
                .border(width = 1.dp, color = thumbBorderColor, shape = ThumbShape),
        )

        Row(modifier = Modifier.fillMaxWidth()) {
            SegmentLabel(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.tab_today),
                selected = selectedTab == HomeTab.TODAY,
                interactionSource = todayInteraction,
                onClick = { onTabSelected(HomeTab.TODAY) },
            )
            SegmentLabel(
                modifier = Modifier.weight(1f),
                text = stringResource(id = R.string.tab_all),
                selected = selectedTab == HomeTab.ALL,
                interactionSource = allInteraction,
                onClick = { onTabSelected(HomeTab.ALL) },
            )
        }
    }
}

@Composable
private fun SegmentLabel(
    modifier: Modifier = Modifier,
    text: String,
    selected: Boolean,
    interactionSource: MutableInteractionSource,
    onClick: () -> Unit,
) {
    // 선택된 라벨은 primary로 은은하게 강조하고, 나머지는 흐리게 낮춰 대비를 준다.
    val textColor by animateColorAsState(
        targetValue = if (selected) {
            AppColor.primary
        } else {
            AppColor.onSurface.copy(alpha = 0.5f)
        },
        animationSpec = tween(durationMillis = 250),
        label = "segmentedTextColor",
    )

    Box(
        modifier = modifier
            .fillMaxHeight()
            .clip(ThumbShape)
            // 리플 없음: 미끄러지는 썸 자체가 피드백이라 플랫폼 컨트롤과 동일하게 맞춘다.
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = textColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
            ),
        )
    }
}
