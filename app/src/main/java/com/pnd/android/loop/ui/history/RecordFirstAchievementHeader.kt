package com.pnd.android.loop.ui.history

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppPageHeader
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.primary

@Composable
internal fun RecordFirstAchievementHeader(
    title: String,
    progress: Float,
    backdrop: BackdropState?,
    calendarExpanded: Boolean,
    onNavigateUp: () -> Unit,
    onToggleCalendar: () -> Unit,
    onMoveToToday: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionLabel = stringResource(
        if (calendarExpanded) R.string.history_calendar_collapse else R.string.history_calendar_expand,
    )
    val calendarState = stringResource(
        if (calendarExpanded) R.string.history_calendar_expanded_state else R.string.history_calendar_collapsed_state,
    )
    val actionColor = if (calendarExpanded) AppColor.primary else AppColor.onSurfaceVariant
    AppPageHeader(
        title = title,
        progress = progress,
        backdrop = backdrop,
        onNavigateUp = onNavigateUp,
        modifier = modifier,
        actions = {
            IconToggleButton(
                checked = calendarExpanded,
                onCheckedChange = { onToggleCalendar() },
                modifier = Modifier.size(48.dp)
                    .background(if (calendarExpanded) AppColor.primary.copy(alpha = 0.1f) else Color.Transparent, CircleShape)
                    .semantics {
                        contentDescription = actionLabel
                        stateDescription = calendarState
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CalendarMonth, null, modifier = Modifier.size(20.dp), tint = actionColor)
                    Icon(
                        if (calendarExpanded) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                        null, modifier = Modifier.size(12.dp), tint = actionColor,
                    )
                }
            }
            TextButton(onClick = onMoveToToday, modifier = Modifier.padding(end = 4.dp)) {
                Text(stringResource(R.string.history_today), color = AppColor.primary)
            }
        },
    )
}
