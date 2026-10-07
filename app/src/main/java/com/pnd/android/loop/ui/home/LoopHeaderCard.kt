package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.common.AppCard
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.Dimens

/**
 * Today focuses on collecting water; All focuses on the tree's long-term growth.
 * Each tab subscribes only to the state its card displays.
 */
@Suppress("UNUSED_PARAMETER")
@Composable
fun LoopHeaderCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    val isToday = selectedTab == HomeTab.TODAY
    AppCard(
        modifier = modifier,
        color = if (isToday) waterCardColor() else treeCardColor(),
        contentPadding = PaddingValues(horizontal = Dimens.contentPadding, vertical = 12.dp),
    ) {
        if (isToday) {
            val rates by loopViewModel.todayRates.collectAsState()
            LoopWaterCard(rates)
        } else {
            val rates by loopViewModel.overallRates.collectAsState()
            val vitality by loopViewModel.treeVitality.collectAsState()
            val estimated by loopViewModel.hasEstimatedHistory.collectAsState(initial = false)
            LoopTreeCard(
                overallRates = rates,
                vitality = vitality,
                hasEstimatedHistory = estimated,
            )
        }
    }
}