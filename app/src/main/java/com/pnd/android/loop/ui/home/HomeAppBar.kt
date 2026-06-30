package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppBar
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.divider
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.formatYearMonthDateDays
import java.time.LocalDate
import java.time.LocalTime

@Composable
fun HomeAppBar(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    val dividerColor = AppColor.divider.copy(alpha = 0.12f)
    AppBar(
        modifier = modifier.drawBehind {
            // Hairline that cleanly separates the app bar from the scrolling content.
            drawRect(
                color = dividerColor,
                topLeft = Offset(0f, size.height - 0.5.dp.toPx()),
                size = Size(size.width, 0.5.dp.toPx())
            )
        },
        title = {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(id = greetingResId()),
                    style = AppTypography.labelMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.5f)
                    )
                )

                val localDate by loopViewModel.localDate.collectAsState(initial = LocalDate.now())
                Text(
                    text = localDate.formatYearMonthDateDays(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.titleLarge.copy(
                        color = AppColor.onSurface
                    )
                )
            }
        },
        // Group / statistics / history live here as compact app-bar actions so the
        // scrolling content stays focused on loops. They share one monochrome tint for a
        // clean Material 3 look that reads well on both light and dark themes.
        actions = {
            HomeAppBarAction(
                imageVector = Icons.Outlined.GroupWork,
                descriptionResId = R.string.group,
                onClick = onNavigateToGroupPage,
            )
            HomeAppBarAction(
                imageVector = Icons.Outlined.BarChart,
                descriptionResId = R.string.statistics,
                onClick = onNavigateToStatisticsPage,
            )
            HomeAppBarAction(
                imageVector = Icons.Outlined.CalendarMonth,
                descriptionResId = R.string.daily_record,
                onClick = onNavigateToHistoryPage,
            )
        }
    )
}

/** A single app-bar navigation action, tightened up so the trio fits beside the date. */
@Composable
private fun HomeAppBarAction(
    imageVector: ImageVector,
    descriptionResId: Int,
    onClick: () -> Unit,
) {
    AppBarIcon(
        imageVector = imageVector,
        color = AppColor.onSurface.copy(alpha = 0.7f),
        descriptionResId = descriptionResId,
        horizontalPadding = 8.dp,
        onClick = onClick,
    )
}

@Composable
private fun greetingResId(): Int {
    val hour = LocalTime.now().hour
    return when (hour) {
        in 5..11 -> R.string.greeting_morning
        in 12..17 -> R.string.greeting_afternoon
        in 18..21 -> R.string.greeting_evening
        else -> R.string.greeting_night
    }
}
