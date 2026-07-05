package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.GroupWork
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.formatYearMonthDateDays
import java.time.LocalDate
import java.time.LocalTime

/** Height of the action-bar row, matching the Material3 small top app bar. */
val HomeActionBarHeight = 64.dp

/**
 * The greeting + date shown on the left of the action bar. It fades out (alpha → 0) as the
 * user scrolls, so the caller drives its visibility via the [Modifier] (e.g. `graphicsLayer`).
 */
@Composable
fun HomeTitle(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    Column(modifier = modifier) {
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
}

/**
 * The group / statistics / history navigation actions shown on the right of the action bar.
 * They keep their spot as the user scrolls and simply gain a floating background there, so
 * they are laid out on their own without the greeting/date.
 */
@Composable
fun HomeActionIcons(
    modifier: Modifier = Modifier,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.End,
    ) {
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
}

/** A single app-bar navigation action, tightened up so the trio fits neatly on the right. */
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
