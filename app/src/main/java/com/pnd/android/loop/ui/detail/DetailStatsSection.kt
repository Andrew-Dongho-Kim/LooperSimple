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
import androidx.compose.material.icons.outlined.Insights
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
import java.time.format.DateTimeFormatter
import kotlin.math.abs

private val ActivityDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd")

/** 구성 비율 → 기간별 막대 → 요일별 막대. 그래프의 실제 값과 범례는 각 그래프 곁에 둔다. */
@Composable
internal fun StatsSection(
    modifier: Modifier = Modifier,
    stats: DetailStats,
    accent: Color,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
) {
    val activity = stats.activity
    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.Insights,
        title = stringResource(R.string.detail_detailed_stats),
        summary = if (activity.counts.total > 0) {
            stringResource(R.string.detail_activity_collapsed, activity.counts.done)
        } else {
            stringResource(R.string.detail_activity_no_recent_records)
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.group)) {
            RecentActivitySummary(activity, accent)
            if (activity.counts.total > 0) {
                Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.headerToContent)) {
                    ActivityTitle(stringResource(R.string.detail_activity_periods_title))
                    ActivityPeriodChart(activity.periods, accent)
                }
                Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.headerToContent)) {
                    ActivityTitle(stringResource(R.string.detail_activity_weekdays_title))
                    ActivityWeekdayChart(activity.weekdays, accent)
                    ActivityCaption(stringResource(R.string.detail_activity_chart_hint))
                }
            }
            ActivityCalculationInfo()
        }
    }
}

/** 숫자 하나를 크게 보여 주고 기간·비교·기록 구성을 바로 아래에서 설명한다. */
@Composable
private fun RecentActivitySummary(activity: DetailActivityStats, accent: Color) {
    Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.related)) {
        ActivityTitle(stringResource(R.string.detail_activity_recent_title))
        ActivityCaption(
            stringResource(
                R.string.detail_activity_date_range,
                activity.start.format(ActivityDateFormat),
                activity.end.format(ActivityDateFormat),
            ),
        )
        if (activity.counts.total == 0) {
            Text(
                modifier = Modifier.padding(top = 8.dp),
                text = stringResource(R.string.detail_activity_empty),
                style = AppTypography.bodyMedium.copy(color = AppColor.onSurface.copy(alpha = 0.7f)),
            )
            return@Column
        }
        Text(
            modifier = Modifier.padding(top = DetailSpacing.related),
            text = stringResource(R.string.detail_activity_completed_days, activity.counts.done),
            style = AppTypography.headlineLarge.copy(color = AppColor.onSurface),
        )
        activity.doneDelta?.let { delta ->
            ActivityCaption(
                when {
                    delta > 0 -> stringResource(R.string.detail_activity_delta_more, delta)
                    delta < 0 -> stringResource(R.string.detail_activity_delta_fewer, abs(delta))
                    else -> stringResource(R.string.detail_activity_delta_same)
                },
            )
        }
        ActivityCaption(stringResource(R.string.detail_activity_record_basis, activity.counts.total))
        ActivityOutcomeChart(activity.counts, accent)
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
