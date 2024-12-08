package com.pnd.android.loop.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary

@Composable
fun LoopHeaderCard(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = AppColor.primary.compositeOverSurface(
                alpha = if (isSystemInDarkTheme()) 0.20f else 0.08f
            ),
            contentColor = AppColor.onSurface,
        ),
    ) {
        Row(
            modifier = Modifier.padding(
                vertical = 8.dp,
                horizontal = 12.dp,
            ),
            verticalAlignment = Alignment.CenterVertically,
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

        WiseSayingText(loopViewModel = loopViewModel)
        PageIcons(
            modifier = Modifier.padding(
                vertical = 8.dp,
                horizontal = 12.dp,
            ),
            onNavigateToGroupPage = onNavigateToGroupPage,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )

    }
}

@Composable
private fun PageIcons(
    modifier: Modifier = Modifier,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PageIcon(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.GroupWork,
            iconText = stringResource(id = R.string.group),
            onNavigateToPage = onNavigateToGroupPage
        )

        PageIcon(
            modifier = Modifier.weight(1f),
            icon = Icons.Outlined.BarChart,
            iconText = stringResource(R.string.statistics),
            onNavigateToPage = onNavigateToStatisticsPage
        )

        PageIcon(
            modifier = Modifier.weight(1f),
            icon = Icons.Filled.CalendarMonth,
            iconText = stringResource(id = R.string.daily_record),
            onNavigateToPage = onNavigateToHistoryPage,
        )
    }
}

@Composable
private fun PageIcon(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    iconText: String,
    onNavigateToPage: () -> Unit,
) {
    Column(
        modifier.clickable { onNavigateToPage() },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            imageVector = icon,
            colorFilter = ColorFilter.tint(
                color = AppColor.onSurface.compositeOverSurface(
                    alpha = 0.7f
                )
            ),
            contentDescription = ""
        )
        Text(
            text = iconText,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )
    }
}

@Composable
private fun WiseSayingText(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    val wiseSaying by loopViewModel.wiseSaying.collectAsState(loopViewModel.wiseSayingText)
    if (wiseSaying.isNotBlank()) {
        Text(
            modifier = modifier
                .padding(
                    horizontal = 24.dp,
                    vertical = 8.dp,
                )
                .fillMaxWidth()
                .border(
                    width = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.3f),
                    shape = RoundShapes.small
                )
                .padding(all = 12.dp),
            text = wiseSaying,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f)
            )
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
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )

        val responseRate by loopViewModel.allResponseRate.collectAsState(initial = 0f)
        Text(
            text = String.format("%.2f%%", responseRate),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f)
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

        val doneRate by loopViewModel.doneRate.collectAsState(initial = 0f)
        Text(
            text = String.format("%.2f%%", doneRate),
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

        val skipRate by loopViewModel.skipRate.collectAsState(initial = 0f)
        Text(
            text = String.format("%.2f%%", skipRate),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f)
            )
        )
    }
}