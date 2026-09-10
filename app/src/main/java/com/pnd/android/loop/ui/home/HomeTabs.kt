package com.pnd.android.loop.ui.home

import androidx.annotation.IntDef
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppSegmentedControl
import com.pnd.android.loop.ui.theme.Dimens

object HomeTab {
    const val ALL = 0
    const val TODAY = 1

    @IntDef(ALL, TODAY)
    annotation class Type
}

val HomeTabsTrackHeight = Dimens.selectionTrackHeight

/** 홈은 선택 상태만 전달하고, 탭의 색·모양·접근성은 공통 컨트롤이 관리한다. */
@Composable
fun HomeTabs(
    modifier: Modifier = Modifier,
    @HomeTab.Type selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    trackHeight: Dp = HomeTabsTrackHeight,
) {
    AppSegmentedControl(
        modifier = modifier,
        options = listOf(HomeTab.TODAY, HomeTab.ALL),
        selected = selectedTab,
        onSelected = onTabSelected,
        minHeight = trackHeight,
        label = { tab ->
            stringResource(if (tab == HomeTab.TODAY) R.string.tab_today else R.string.tab_all)
        },
    )
}
