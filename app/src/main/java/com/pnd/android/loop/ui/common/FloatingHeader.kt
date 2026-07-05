package com.pnd.android.loop.ui.common

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// --- 떠 있는(플로팅) 헤더 공용 정의 -----------------------------------------------------------
// 홈과 기록 화면 모두 "스크롤하면 접히는 헤더"를 쓴다. 접힘 진행도 계산과 플로팅 알약(pill)의
// 공용 치수/모양·나타남 곡선을 여기 모아, 두 화면이 같은 규칙으로 동작하도록 한다.

/**
 * 두 화면의 플로팅 알약이 공유하는 높이. 접힌 액션 아이콘/탭이 모두 이 높이로 맞춰진다.
 */
val FloatingPillHeight = 52.dp

/** 콘텐츠와 확실히 분리돼 보이도록 완전히 둥근 캡슐 모양을 쓴다. */
val FloatingHeaderShape = RoundedCornerShape(percent = 50)

/** 플로팅 배경이 나타나기 시작하는 접힘 진행도. */
private const val SurfaceRevealStart = 0.9f

/**
 * 플로팅 배경이 얼마나 나타났는지(0f..1f). 접힘이 거의 끝날 때까지 숨어 있다가 빠르게 나타나서,
 * 스크롤 중간에 반쯤 나타난 어색한 알약이 보이지 않도록 한다.
 */
fun surfaceReveal(progress: Float): Float =
    ((progress - SurfaceRevealStart) / (1f - SurfaceRevealStart)).coerceIn(0f, 1f)


@Composable
fun Modifier.floatingHeaderPadding() = padding(
    horizontal = if (LocalConfiguration.current.isPortrait()) 8.dp else 0.dp
)


/**
 * 리스트 스크롤 정도로부터 헤더 접힘 진행도(0f..1f)를 만든다.
 * - `0f` : 기준 위치(펼침) — 헤더가 완전히 보인다.
 * - `1f` : [collapseDistance]만큼 스크롤됨(접힘) — 타이틀이 사라지고 아이콘이 플로팅된다.
 *
 * [reverseLayout]이 true인 리스트(예: 최신이 하단에 오는 채팅형)는 기준점이 마지막 인덱스이므로,
 * 그 인덱스에서 얼마나 벗어났는지로 진행도를 계산한다. `derivedStateOf`를 사용해 진행도가 실제로
 * 바뀔 때만 읽는 쪽이 리컴포즈된다.
 */
@Composable
fun rememberListCollapseProgress(
    lazyListState: LazyListState,
    collapseDistance: Dp,
    reverseLayout: Boolean = false,
): State<Float> {
    val collapseDistancePx = with(LocalDensity.current) { collapseDistance.toPx() }
    return remember(lazyListState, collapseDistancePx, reverseLayout) {
        derivedStateOf {
            // 콘텐츠가 화면보다 짧아 스크롤 자체가 불가능하면 접힘도 없다(0f).
            if (!lazyListState.canScrollForward && !lazyListState.canScrollBackward) {
                return@derivedStateOf 0f
            }
            if (reverseLayout) {
                reverseCollapseProgress(lazyListState, collapseDistancePx)
            } else {
                forwardCollapseProgress(lazyListState, collapseDistancePx)
            }
        }
    }
}

/**
 * 정방향 리스트의 접힘 진행도. 기준점은 최상단(인덱스 0)이며, 최상단 항목이 위로 스크롤된
 * 픽셀만큼 접힌다. 다른 항목으로 넘어가면(인덱스 0을 벗어나면) 완전히 접힌 것(1f)으로 본다.
 */
private fun forwardCollapseProgress(
    lazyListState: LazyListState,
    collapseDistancePx: Float,
): Float {
    val scrolled = if (lazyListState.firstVisibleItemIndex != 0) {
        collapseDistancePx
    } else {
        lazyListState.firstVisibleItemScrollOffset.toFloat()
    }
    return (scrolled / collapseDistancePx).coerceIn(0f, 1f)
}

/**
 * 역방향(reverseLayout) 리스트의 접힘 진행도.
 *
 * 이 리스트는 가장 최신 항목(마지막 인덱스)이 화면 상단에 놓인 상태에서 시작한다. 이때
 * `firstVisibleItemIndex`는 화면 '맨 아래' 항목(더 작은 인덱스)을 가리키므로, 정방향처럼
 * 특정 인덱스를 기준점으로 삼으면 시작하자마자 접힘으로 오판된다(스크롤하지 않아도 타이틀이
 * 사라지는 버그).
 *
 * 그래서 최신 항목 자체의 위치로 진행도를 계산한다. 최신 항목의 윗변이 쉬는(펼침) 위치에서
 * 위로 얼마나 밀려 올라갔는지를 [collapseDistancePx]에 대한 비율로 본다.
 * - 최신 항목이 제자리에 있으면 0f(펼침), [collapseDistancePx]만큼 밀려 올라가면 1f(접힘).
 * - 최신 항목이 상단 밖으로 완전히 사라지면 1f(완전히 접힘)로 본다.
 */
private fun reverseCollapseProgress(
    lazyListState: LazyListState,
    collapseDistancePx: Float,
): Float {
    if (collapseDistancePx <= 0) return 0f

    val layoutInfo = lazyListState.layoutInfo
    val lastIndex = layoutInfo.totalItemsCount - 1
    if (lastIndex < 0) return 0f

    // 1. 가장 마지막(최신) 아이템이 보이는지 확인
    val newestItem = layoutInfo.visibleItemsInfo.firstOrNull { it.index == lastIndex }
        ?: return 1f // 화면에 없으면 이미 완전히 스크롤되어 올라간 상태로 간주

    val scrollUp =
        (layoutInfo.afterContentPadding + newestItem.offset + newestItem.size) - layoutInfo.viewportEndOffset
    return (scrollUp / collapseDistancePx).coerceIn(0f, 1f)
}
