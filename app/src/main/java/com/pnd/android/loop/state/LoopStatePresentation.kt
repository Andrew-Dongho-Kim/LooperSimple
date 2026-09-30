package com.pnd.android.loop.state

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.ui.graphics.vector.ImageVector
import com.pnd.android.loop.R

/** Shared state labels for records, calendars, and accessibility descriptions. Null means no record. */
@StringRes
fun Int?.stateLabelRes(): Int = when (this) {
    DoneState.DONE -> R.string.done
    DoneState.SKIP -> R.string.skip
    DoneState.IN_PROGRESS -> R.string.history_in_progress
    DoneState.NO_RESPONSE -> R.string.no_response
    DoneState.DISABLED -> R.string.detail_loop_state_inactive
    NOT_SCHEDULED -> R.string.all_history_inactive_day
    else -> R.string.detail_day_no_record
}

fun Int?.stateIcon(): ImageVector = when (this) {
    DoneState.DONE -> Icons.Outlined.Check
    DoneState.SKIP, NOT_SCHEDULED -> Icons.Outlined.Remove
    DoneState.IN_PROGRESS -> Icons.Outlined.Schedule
    DoneState.DISABLED -> Icons.Outlined.Pause
    else -> Icons.Outlined.MoreHoriz
}
