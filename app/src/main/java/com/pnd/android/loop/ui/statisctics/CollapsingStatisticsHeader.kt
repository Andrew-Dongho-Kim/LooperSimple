package com.pnd.android.loop.ui.statisctics

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.AppSegmentedControl
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.common.FloatingHeaderShape
import com.pnd.android.loop.ui.common.FloatingPillHeight
import com.pnd.android.loop.ui.common.FloatingSurface
import com.pnd.android.loop.ui.common.isPortrait
import com.pnd.android.loop.ui.common.rememberListCollapseProgress
import com.pnd.android.loop.ui.common.surfaceReveal
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.onSurface

// --- Header geometry -------------------------------------------------------------------------
// 홈([com.pnd.android.loop.ui.home.CollapsingHomeHeader])과 같은 구성이다. 펼친 상태에서는
// 액션바(뒤로가기 + 제목) 아래 줄에 월간/전체 탭이 놓이고, 스크롤하면 탭이 액션바 줄로 올라가
// 뒤로가기 옆에 플로팅 알약으로 붙는다. 탭이 올라가는 거리가 곧 접힘 거리다.

private val TabsRowTopPadding = 8.dp
private val TabsRowBottomPadding = 16.dp
private val TabsTrackHeight = Dimens.selectionTrackHeight

/** 액션바 아래에 펼쳐진 탭 행의 높이(트랙 + 위/아래 여백). */
private val StatisticsTabsRowHeight = TabsRowTopPadding + TabsTrackHeight + TabsRowBottomPadding

/** 헤더가 완전히 접히는 스크롤 거리 — 탭이 액션바 줄까지 올라가는 거리. */
private val StatisticsHeaderCollapseDistance = StatisticsTabsRowHeight

/**
 * 액션바 줄로 올라가 플로팅된 탭의 폭. 월별 칸에 월 이름(다른 해면 "25년 10월")과 ▾가 함께 들어가야
 * 하므로 홈의 접힌 탭보다 넓다. 뒤로가기 옆 남은 폭을 넘지는 않는다.
 */
private val CollapsedTabWidth = 200.dp

/** 플로팅 배경과 그 안의 탭 사이 여백. */
private val TabFloatingRim = 5.dp

/** 탭 알약(트랙 + 양쪽 여백)이 [FloatingPillHeight]와 같아지도록 맞춘 트랙 높이. */
private val CollapsedTabTrackHeight = FloatingPillHeight - TabFloatingRim * 2

/** 알약끼리 맞붙어 하나로 보이지 않도록 두는 간격. [com.pnd.android.loop.ui.common.AppPageHeader]와 같다. */
private val PillSpacing = 8.dp

/** 제목의 좌우 여백. [com.pnd.android.loop.ui.common.AppPageHeader]의 제목 알약과 위치를 맞춘다. */
private val TitlePadding = 16.dp

/**
 * 펼친 헤더가 차지하는 전체 높이. 리스트는 이 아래에서 시작한다.
 * [includeTabs]가 false면(기간과 무관한 탭) 탭 행 없이 액션바 줄까지만 차지한다.
 */
fun statisticsHeaderExpandedHeight(topInset: Dp, includeTabs: Boolean = true) =
    topInset + Dimens.appBarHeight + (if (includeTabs) StatisticsTabsRowHeight else 0.dp)

/** 통계 헤더의 접힘 진행도(`0f..1f`). */
@Composable
fun rememberStatisticsHeaderCollapseProgress(lazyListState: LazyListState): State<Float> =
    rememberListCollapseProgress(
        lazyListState = lazyListState,
        collapseDistance = StatisticsHeaderCollapseDistance,
    )

/**
 * 통계 화면의 접히는 헤더. 스크롤되는 리스트 위에 오버레이로 그린다.
 *
 * [progress]는 리스트 스크롤로 정해지는 접힘 정도(`0f..1f`)다.
 * - `0f`(펼침) — 뒤로가기와 "통계" 제목이 있는 일반 액션바, 그 아래 줄에 전체 폭 탭. 플로팅 배경 없음.
 * - `1f`(접힘) — 제목은 사라지고, 탭이 액션바 줄로 올라와 줄어든 채 뒤로가기 옆에 플로팅 알약으로 고정된다.
 *
 * 월별 칸에는 "월별" 대신 지금 고른 월([monthLabel])을 쓴다. 전체를 보는 동안에도 월별로 돌아가면
 * 어느 달이 나올지 보이게 하기 위해서다. 월별이 선택된 상태에서 그 칸을 다시 누르면
 * [onMonthMenuClick]으로 월 선택 시트가 열린다.
 *
 * [showPeriodTabs]가 false면(기간과 무관한 탭) 탭 없이 뒤로가기와 제목만 두고, 스크롤하면 제목은
 * 사라지고 뒤로가기만 플로팅으로 남는다. 이때 [progress]는 액션바 높이만큼의 스크롤로 정한다.
 */
@Composable
fun CollapsingStatisticsHeader(
    progress: Float,
    showPeriodTabs: Boolean,
    showTotal: Boolean,
    monthLabel: String,
    monthDescription: String,
    onTotalSelected: (Boolean) -> Unit,
    onMonthMenuClick: () -> Unit,
    onNavigateUp: () -> Unit,
    backdrop: BackdropState?,
    modifier: Modifier = Modifier,
) {
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val screenPadding = Dimens.screenHorizontalPadding
    // AppPageHeader의 floatingHeaderPadding과 같은 값으로, 뒤로가기 알약 위치를 다른 하위 화면과 맞춘다.
    val headerStartPadding = if (LocalConfiguration.current.isPortrait()) 8.dp else 0.dp
    // 배경/그림자는 접힘이 거의 끝날 때 나타나고, 탭 이동은 원래 진행도로 스크롤 전체에 걸쳐 부드럽게 움직인다.
    val surfaceProgress = surfaceReveal(progress)
    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .height(statisticsHeaderExpandedHeight(topInset, includeTabs = showPeriodTabs)),
    ) {
        // 뒤로가기: 액션바 줄 왼쪽에 고정되고, 스크롤하면 플로팅 배경을 얻는다.
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topInset, start = headerStartPadding)
                .height(Dimens.appBarHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
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
        }

        // 뒤로가기 오른쪽에서 시작하는 영역. 제목과 접힌 탭이 같은 자리를 쓴다.
        val contentStart = headerStartPadding + FloatingPillHeight + PillSpacing
        val contentWidth = (maxWidth - contentStart - screenPadding).coerceAtLeast(0.dp)

        // 제목: 액션바 줄에 고정된 채 스크롤에 따라 사라진다(탭이 올라와 그 자리를 차지한다).
        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(top = topInset, start = contentStart)
                .width(contentWidth)
                .height(Dimens.appBarHeight),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(
                modifier = Modifier
                    .padding(horizontal = TitlePadding)
                    .alpha(1f - progress),
                text = stringResource(R.string.statistics),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.titleLarge,
                color = AppColor.onSurface,
            )
        }

        if (!showPeriodTabs) return@BoxWithConstraints

        // 월간 / 전체 탭: 아래 줄에서 액션바 줄로 올라오며 줄어들고, 뒤로가기 옆 플로팅 알약으로 자리 잡는다.
        val tabX = lerp(screenPadding, contentStart, progress)
        val tabTop = lerp(
            start = topInset + Dimens.appBarHeight + TabsRowTopPadding,
            stop = topInset + (Dimens.appBarHeight - FloatingPillHeight) / 2,
            fraction = progress,
        )
        val tabWidth = lerp(
            maxWidth - screenPadding * 2,
            minOf(CollapsedTabWidth, contentWidth),
            progress,
        )
        val tabTrackHeight = lerp(TabsTrackHeight, CollapsedTabTrackHeight, progress)
        val tabRim = lerp(0.dp, TabFloatingRim, progress)

        FloatingSurface(
            progress = surfaceProgress,
            shape = FloatingHeaderShape,
            backdrop = backdrop,
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(x = tabX, y = tabTop)
                .width(tabWidth),
        ) {
            AppSegmentedControl(
                modifier = Modifier.padding(tabRim),
                options = listOf(false, true),
                selected = showTotal,
                onSelected = onTotalSelected,
                minHeight = tabTrackHeight,
                label = { isTotal -> if (isTotal) stringResource(R.string.total) else monthLabel },
                description = { isTotal -> if (isTotal) null else monthDescription },
                menuLabel = { isTotal -> if (isTotal) null else stringResource(R.string.stat_choose_month) },
                onMenuClick = { onMonthMenuClick() },
            )
        }
    }
}
