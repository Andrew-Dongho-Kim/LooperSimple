package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material.ContentAlpha
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface

@Composable
fun StatisticsCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    Card(
        modifier = modifier.clickable {  },
        colors = CardDefaults.cardColors(
            containerColor = AppColor.surface,
            contentColor = AppColor.onSurface,
        ),
        elevation = CardDefaults.elevatedCardElevation()
    ) {
        Row(
            modifier = Modifier.padding(
                vertical = 8.dp,
                horizontal = 12.dp,
            )
        ) {
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
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )

        val responseRate by loopViewModel.allResponseRate.collectAsState(initial = 0f)
        Text(
            text = String.format("%.2f%%", responseRate),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = ContentAlpha.medium)
            )
        )
    }
}

@Composable
private fun DoneRate(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.done_rate) + ": ",
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )

        val responseRate by loopViewModel.doneRate.collectAsState(initial = 0f)
        Text(
            text = String.format("%.2f%%", responseRate),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.primary
            )
        )
    }
}

@Composable
private fun SkipRate(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(id = R.string.skip_rate) + ": ",
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )

        val responseRate by loopViewModel.skipRate.collectAsState(initial = 0f)
        Text(
            text = String.format("%.2f%%", responseRate),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = ContentAlpha.medium)
            )
        )
    }
}