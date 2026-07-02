package com.pnd.android.loop.ui.home

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
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

/**
 * Height shared by both floating pills, so the collapsed tabs stand exactly as tall as the
 * action icons (both are capped to this height).
 */
val FloatingPillHeight = 52.dp

/** Collapsed width of the tabs once they float in the action-bar row. */
private val CollapsedTabWidth = 148.dp

/** Breathing room between the floating white background and the tabs it sits behind. */
private val TabFloatingRim = 5.dp

/** Track height so the tabs' pill (track + rim on both sides) equals [FloatingPillHeight]. */
private val CollapsedTabTrackHeight = FloatingPillHeight - TabFloatingRim * 2

// A fully rounded capsule so the floating pills read as distinctly separate from the content.
private val FloatingShape = RoundedCornerShape(percent = 50)

/** Full height the header occupies at rest, so the scrolling content can start just below it. */
fun homeHeaderExpandedHeight(topInset: Dp) =
    topInset + HomeActionBarHeight + HomeTabsRowHeight

/** Top of the collapsed tabs pill, vertically centered in the action-bar row like the icons. */
private fun collapsedTabTop(topInset: Dp) =
    topInset + (HomeActionBarHeight - FloatingPillHeight) / 2

/** Scroll fraction past which the floating backgrounds start to appear. */
private const val SurfaceRevealStart = 0.9f

/**
 * How present the floating backgrounds are. Unlike the tabs' motion (which tracks the raw
 * progress the whole way), the backgrounds stay hidden until the collapse is nearly done and
 * then appear quickly — so the middle of the scroll never shows a half-faded pill.
 */
private fun surfaceReveal(progress: Float): Float =
    ((progress - SurfaceRevealStart) / (1f - SurfaceRevealStart)).coerceIn(0f, 1f)

/**
 * Collapse fraction (`0f..1f`) derived from how far the list has scrolled. Once the first item
 * has scrolled past, the header is considered fully collapsed. Uses `derivedStateOf` so it only
 * recomposes readers when the fraction actually changes.
 */
@Composable
fun rememberHomeHeaderCollapseProgress(lazyListState: LazyListState): State<Float> {
    val collapseDistancePx = with(LocalDensity.current) { HomeHeaderCollapseDistance.toPx() }
    return remember(lazyListState, collapseDistancePx) {
        derivedStateOf {
            val scrolled = if (lazyListState.firstVisibleItemIndex > 0) {
                collapseDistancePx
            } else {
                lazyListState.firstVisibleItemScrollOffset.toFloat()
            }
            (scrolled / collapseDistancePx).coerceIn(0f, 1f)
        }
    }
}

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
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenPadding = Dimens.screenHorizontalPadding
    // Backgrounds/shadows appear only near the end of the collapse; the tabs still move smoothly
    // over the whole scroll via the raw [progress].
    val surfaceProgress = surfaceReveal(progress)
    // Both floating pills tuck 8dp further in from the screen edges as they collapse.
    val floatingMargin = lerp(0.dp, 0.dp, progress)
    // ...and their content sits 8dp in from the pill's own left/right edges.
    val innerMargin = lerp(0.dp, 8.dp, progress)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(homeHeaderExpandedHeight(topInset)),
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
                shape = FloatingShape,
                backdrop = backdrop,
                contentHorizontalPadding = innerMargin,
            ) {
                HomeActionIcons(
                    modifier = Modifier.height(FloatingPillHeight),
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
        val tabWidth = lerp(maxWidth - screenPadding * 2, CollapsedTabWidth, progress)
        val tabTrackHeight = lerp(HomeTabsTrackHeight, CollapsedTabTrackHeight, progress)
        val tabRim = lerp(0.dp, TabFloatingRim, progress)

        FloatingSurface(
            progress = surfaceProgress,
            shape = FloatingShape,
            backdrop = backdrop,
            contentHorizontalPadding = innerMargin,
            modifier = Modifier
                .align(Alignment.TopStart)
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
