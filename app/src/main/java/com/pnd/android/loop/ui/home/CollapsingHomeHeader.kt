package com.pnd.android.loop.ui.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.common.FloatingHeaderShape
import com.pnd.android.loop.ui.common.FloatingPillHeight
import com.pnd.android.loop.ui.common.FloatingSurface
import com.pnd.android.loop.ui.common.rememberListCollapseProgress
import com.pnd.android.loop.ui.common.surfaceReveal
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.Dimens

// --- Header geometry -------------------------------------------------------------------------
// The expanded header is a normal action bar (title + icons) with the 오늘/전체 tabs on the row
// beneath it. As the list scrolls the tabs travel up into the action-bar row, which is exactly
// the vertical distance the collapse animation runs over.

private val TabsRowTopPadding = 8.dp
private val TabsRowBottomPadding = 16.dp

/** Height of the tabs row (track + its top/bottom padding) shown below the action bar at rest. */
val HomeTabsRowHeight = TabsRowTopPadding + HomeTabsTrackHeight + TabsRowBottomPadding

/** Scroll distance over which the header fully collapses — the tabs' travel into the action bar. */
val HomeHeaderCollapseDistance = HomeTabsRowHeight

/** Collapsed width of the tabs once they float in the action-bar row. */
private val CollapsedTabWidth = 148.dp

/** Breathing room between the floating white background and the tabs it sits behind. */
private val TabFloatingRim = 5.dp

/** Track height so the tabs' pill (track + rim on both sides) equals [FloatingPillHeight]. */
private val CollapsedTabTrackHeight = FloatingPillHeight - TabFloatingRim * 2

/**
 * Full height the header occupies at rest, so the scrolling content can start just below it.
 * [includeTabs]가 false면(루프가 없는 빈 상태) 탭 행 높이를 빼서 헤더가 인사말/아이콘 줄까지만
 * 차지하도록 한다.
 */
fun homeHeaderExpandedHeight(topInset: Dp, includeTabs: Boolean = true) =
    topInset + HomeActionBarHeight + (if (includeTabs) HomeTabsRowHeight else 0.dp)

/** Top of the collapsed tabs pill, vertically centered in the action-bar row like the icons. */
private fun collapsedTabTop(topInset: Dp) =
    topInset + (HomeActionBarHeight - FloatingPillHeight) / 2

/**
 * 홈 헤더의 접힘 진행도(`0f..1f`). 공용 [rememberListCollapseProgress]에 홈의 접힘 거리를 넘겨
 * 계산한다. 홈 리스트는 일반(정방향) 레이아웃이라 최상단이 기준점이다.
 */
@Composable
fun rememberHomeHeaderCollapseProgress(lazyListState: LazyListState): State<Float> =
    rememberListCollapseProgress(
        lazyListState = lazyListState,
        collapseDistance = HomeHeaderCollapseDistance,
    )

/**
 * The One UI-style collapsing header, drawn as an overlay above the scrolling list.
 *
 * [progress] is the collapse fraction in `0f..1f` driven by the list scroll:
 * - `0f` (at rest) — a plain action bar: greeting/date on the left, icons on the right, full
 *   width tabs on the row below. No floating backgrounds.
 * - `1f` (scrolled) — the greeting/date has faded out, the icons float in place on the right, and
 *   the tabs have slid up into the action-bar row, shrunk, and float as a pill on the left.
 */
@Composable
fun CollapsingHomeHeader(
    progress: Float,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    backdrop: BackdropState?,
    modifier: Modifier = Modifier,
    showTabs: Boolean = true,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenPadding = Dimens.screenHorizontalPadding
    // Backgrounds/shadows appear only near the end of the collapse; the tabs still move smoothly
    // over the whole scroll via the raw [progress].
    val surfaceProgress = surfaceReveal(progress)
    // Both floating pills tuck 8dp further in from the screen edges as they collapse.
    val floatingMargin = lerp(0.dp, 0.dp, progress)

    // 탭이 없는 빈 상태에서는 헤더 높이도 그만큼 줄여 위쪽 공백을 없앤다.
    val headerHeight = homeHeaderExpandedHeight(topInset, includeTabs = showTabs)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(headerHeight),
    ) {
        // Greeting + date: pinned to the action-bar row, fading out as the list scrolls.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topInset, start = screenPadding)
                .height(HomeActionBarHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            HomeTitle(
                modifier = Modifier.alpha(1f - progress),
                loopViewModel = loopViewModel,
            )
        }

        // Action icons: keep their spot on the right and gain a floating background on scroll.
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topInset, end = screenPadding - 8.dp + floatingMargin)
                .height(HomeActionBarHeight),
            contentAlignment = Alignment.CenterEnd,
        ) {
            FloatingSurface(
                progress = surfaceProgress,
                shape = FloatingHeaderShape,
                backdrop = backdrop,
            ) {
                HomeActionIcons(
                    modifier = Modifier
                        .height(FloatingPillHeight)
                        .padding(horizontal = 8.dp),
                    onNavigateToGroupPage = onNavigateToGroupPage,
                    onNavigateToStatisticsPage = onNavigateToStatisticsPage,
                    onNavigateToHistoryPage = onNavigateToHistoryPage,
                )
            }
        }

        // 오늘 / 전체 tabs: slide up from below into the action-bar row, shrinking as they go and
        // settling into a floating pill on the left (aligned with the icons on the right).
        val tabTop = lerp(
            start = topInset + HomeActionBarHeight + TabsRowTopPadding,
            stop = collapsedTabTop(topInset),
            fraction = progress,
        )
        val tabWidth = lerp(this.maxWidth - screenPadding * 2, CollapsedTabWidth, progress)
        val tabTrackHeight = lerp(HomeTabsTrackHeight, CollapsedTabTrackHeight, progress)
        val tabRim = lerp(0.dp, TabFloatingRim, progress)

        // 루프가 없는 OOBE 상태에서는 탭을 숨기고, 첫 루프가 생기면 아래에서 부드럽게 나타난다.
        AnimatedVisibility(
            visible = showTabs,
            modifier = Modifier.align(Alignment.TopStart),
            enter = fadeIn() + slideInVertically(initialOffsetY = { it / 3 }),
            exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 3 }),
        ) {
            FloatingSurface(
                progress = surfaceProgress,
                shape = FloatingHeaderShape,
                backdrop = backdrop,
                modifier = Modifier
                    .offset(x = screenPadding + floatingMargin, y = tabTop)
                    .width(tabWidth),
            ) {
                HomeTabs(
                    modifier = Modifier.padding(horizontal = tabRim, vertical = tabRim),
                    selectedTab = selectedTab,
                    onTabSelected = onTabSelected,
                    trackHeight = tabTrackHeight,
                )
            }
        }
    }
}
