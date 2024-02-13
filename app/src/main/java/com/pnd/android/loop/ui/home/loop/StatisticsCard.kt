package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.layout.Row
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary

@Composable
fun StatisticsCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    Row(modifier = modifier) {
        ResponseRate(
            modifier = Modifier.weight(1f),
            loopViewModel = loopViewModel
        )
        DoneRate(
            modifier = Modifier.weight(1f),
            loopViewModel = loopViewModel,
        )
        SkipRate(
            modifier = Modifier.weight(1f),
            loopViewModel = loopViewModel
        )
    }
}

@Composable
private fun ResponseRate(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.response_rate) + ": ",
            style = AppTypography.body1.copy(
                color = AppColor.onSurface
            )
        )

        val responseRate by loopViewModel.allResponseRate.collectAsState(initial = 0f)
        Text(
            text = String.format("%.2f%%", responseRate),
            style = AppTypography.body1.copy(
                color = AppColor.primary
            )
        )
    }
}

@Composable
private fun DoneRate(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel
) {

}

@Composable
private fun SkipRate(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel
) {

}