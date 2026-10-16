package com.pnd.android.loop.ui.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val ActivityDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/** Always visible: recent effort, its trend, and a small hint from weekday patterns. */
@Composable
internal fun StatsSection(
    modifier: Modifier = Modifier,
    stats: DetailStats,
    accent: Color,
) {
    val activity = stats.activity
    DetailCard(modifier = modifier) {
        Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.group)) {
            Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.related)) {
                ActivityTitle(stringResource(R.string.detail_motivation_activity_title))
                ActivityCaption(stringResource(R.string.detail_activity_date_range,
                    activity.start.format(ActivityDateFormat), activity.end.format(ActivityDateFormat)))
                activity.doneDelta?.let { delta ->
                    ActivityCaption(when {
                        delta > 0 -> stringResource(R.string.detail_activity_delta_more, delta)
                        delta < 0 -> stringResource(R.string.detail_motivation_gentle_delta, abs(delta))
                        else -> stringResource(R.string.detail_motivation_steady)
                    })
                }
            }
            if (activity.counts.total > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.headerToContent)) {
                    ActivityTitle(stringResource(R.string.detail_activity_periods_title))
                    ActivityPeriodChart(activity.periods, accent)
                }
                // Compare counts, as the label promises; never infer a weekday success rate.
                val mostDone = activity.weekdays.maxOfOrNull { it.counts.done } ?: 0
                val bestDays = activity.weekdays.filter { it.counts.done == mostDone }
                if (mostDone > 0 && bestDays.size < activity.weekdays.size) {
                    val names = bestDays.map { day ->
                        stringResource(DAYS_WITH_3CHARS[day.day.value - 1])
                    }.joinToString(" · ")
                    Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.related)) {
                        ActivityTitle(stringResource(R.string.detail_motivation_best_days))
                        ActivityCaption(names)
                    }
                }
                ActivityOutcomeChart(activity.counts, accent)
            } else {
                ActivityCaption(stringResource(R.string.detail_motivation_empty))
            }
            ActivityCalculationInfo()
        }
    }
}

@Composable
private fun ActivityCalculationInfo() {
    var expanded by rememberSaveable { mutableStateOf(false) }
    val state = stringResource(
        if (expanded) R.string.detail_state_expanded else R.string.detail_state_collapsed,
    )
    Column {
        HairlineDivider()
        Row(
            modifier = Modifier.fillMaxWidth()
                .clickable(role = Role.Button) { expanded = !expanded }
                .semantics { stateDescription = state }
                .sizeIn(minHeight = MinTouchTarget),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(R.string.detail_activity_calculation_title),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
            )
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = AppColor.onSurface.copy(alpha = 0.5f),
            )
        }
        AnimatedVisibility(expanded) {
            Column(Modifier.padding(top = DetailSpacing.related)) {
                ActivityCaption(stringResource(R.string.detail_activity_calculation_body))
            }
        }
    }
}

@Composable
private fun ActivityTitle(text: String) {
    Text(text, style = AppTypography.titleMedium.copy(color = AppColor.onSurface))
}

@Composable
private fun ActivityCaption(text: String) {
    Text(text, style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.65f)))
}
