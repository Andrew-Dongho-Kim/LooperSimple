package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.history.LoopSettings
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.toLocalDateTime
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

private const val HISTORY_PAGE_SIZE = 5
private val RevisionDateFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd", Locale.ROOT)
private val RevisionTimeFormat = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss", Locale.ROOT)

/** Read-only history. Rendering more entries never changes settings or execution records. */
@Composable
internal fun RevisionHistorySection(
    loopId: Int,
    entries: List<LoopRevisionEntry>?,
    expanded: Boolean,
    onExpandedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    var visibleCount by rememberSaveable(loopId) { mutableIntStateOf(HISTORY_PAGE_SIZE) }
    val editCount = entries?.count { !it.isInitial } ?: 0
    ExpandableSection(
        modifier = modifier,
        icon = Icons.Outlined.History,
        title = stringResource(R.string.revision_history_title),
        summary = when {
            entries == null -> stringResource(R.string.revision_history_loading)
            editCount == 0 -> stringResource(R.string.revision_history_no_edits)
            else -> stringResource(R.string.revision_history_count, editCount)
        },
        expanded = expanded,
        onExpandedChange = onExpandedChange,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.group)) {
            when {
                entries == null -> HistoryExplanation(stringResource(R.string.revision_history_loading))
                entries.isEmpty() -> HistoryExplanation(stringResource(R.string.revision_history_empty))
                else -> {
                    HistoryExplanation(stringResource(R.string.revision_history_policy))
                    entries.take(visibleCount).forEach { entry ->
                        key(entry.revision.revisionId) { RevisionHistoryCard(entry) }
                    }
                    val remaining = entries.size - visibleCount
                    if (remaining > 0) {
                        TextActionButton(
                            text = stringResource(R.string.revision_history_more, remaining),
                            onClick = { visibleCount += HISTORY_PAGE_SIZE },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RevisionHistoryCard(entry: LoopRevisionEntry) {
    val revision = entry.revision
    DetailCard {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = if (entry.isInitial) stringResource(R.string.revision_history_initial)
                        else entry.fields.map { fieldLabel(it) }.joinToString(" · "),
                    style = AppTypography.titleSmall,
                    color = AppColor.onSurface,
                    modifier = Modifier.semantics { heading() },
                )
                HistoryExplanation(revision.recordedAt.toLocalDateTime().format(RevisionTimeFormat))
                if (entry.isInitial) {
                    // A migrated baseline is a snapshot, never proof of when earlier edits happened.
                    HistoryExplanation(stringResource(R.string.revision_history_initial_note))
                } else {
                    if (entry.fields.any { it != LoopRevisionField.WEEKLY_GOAL }) {
                        HistoryExplanation(stringResource(R.string.revision_history_effective,
                            LocalDate.ofEpochDay(revision.effectiveFrom).format(RevisionDateFormat)))
                    }
                    if (LoopRevisionField.WEEKLY_GOAL in entry.fields) {
                        HistoryExplanation(stringResource(R.string.revision_history_goal_effective,
                            LocalDate.ofEpochDay(revision.goalEffectiveFrom).format(RevisionDateFormat)))
                    }
                }
            }
            entry.fields.forEach { field ->
                HairlineDivider()
                Column(
                    verticalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier.fillMaxWidth().semantics(mergeDescendants = true) {},
                ) {
                    Text(fieldLabel(field), style = AppTypography.labelMedium, color = AppColor.onSurfaceVariant)
                    entry.previous?.let { previous -> RevisionFieldValue(field, previous, isPrevious = true) }
                    RevisionFieldValue(field, revision.settings, isPrevious = false, initial = entry.isInitial)
                }
            }
        }
    }
}

@Composable
private fun HistoryExplanation(text: String) {
    Text(text, style = AppTypography.bodySmall, color = AppColor.onSurfaceVariant)
}

@Composable
private fun RevisionFieldValue(
    field: LoopRevisionField,
    settings: LoopSettings,
    isPrevious: Boolean,
    initial: Boolean = false,
) {
    val value = fieldValue(field, settings)
    val text = when {
        initial -> value
        isPrevious -> stringResource(R.string.revision_history_before, value)
        else -> stringResource(R.string.revision_history_after, value)
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        if (field == LoopRevisionField.COLOR) {
            Box(Modifier.size(16.dp).background(Color(settings.color), CircleShape)
                .border(1.dp, AppColor.onSurface.copy(alpha = 0.2f), CircleShape))
        }
        Text(text, style = AppTypography.bodyMedium,
            color = if (isPrevious) AppColor.onSurfaceVariant else AppColor.onSurface)
    }
}

@Composable
private fun fieldLabel(field: LoopRevisionField): String = stringResource(when (field) {
    LoopRevisionField.NAME -> R.string.revision_field_name
    LoopRevisionField.COLOR -> R.string.revision_field_color
    LoopRevisionField.DAYS -> R.string.revision_field_days
    LoopRevisionField.TIME -> R.string.revision_field_time
    LoopRevisionField.WEEKLY_GOAL -> R.string.revision_field_goal
    LoopRevisionField.ENABLED -> R.string.revision_field_enabled
})

@Composable
private fun fieldValue(field: LoopRevisionField, settings: LoopSettings): String = when (field) {
    LoopRevisionField.NAME -> settings.title
    LoopRevisionField.COLOR -> String.format(Locale.ROOT, "#%08X", settings.color)
    LoopRevisionField.DAYS -> repeatDaysText(settings.activeDays)
    LoopRevisionField.TIME -> when {
        settings.isAnyTime -> stringResource(R.string.revision_anytime)
        settings.startInDay !in 0 until 86_400_000L || settings.endInDay !in 0 until 86_400_000L ->
            stringResource(R.string.revision_unknown_time)
        else -> {
            val range = "${settings.startInDay.formatHourMinute()} – ${settings.endInDay.formatHourMinute()}"
            if (settings.endInDay < settings.startInDay) stringResource(R.string.revision_overnight, range) else range
        }
    }
    LoopRevisionField.WEEKLY_GOAL -> if (settings.weeklyGoal > 0) {
        stringResource(R.string.revision_goal_times, settings.weeklyGoal)
    } else stringResource(R.string.revision_goal_none)
    LoopRevisionField.ENABLED -> stringResource(if (settings.enabled) R.string.revision_enabled else R.string.revision_disabled)
}

@Composable
private fun repeatDaysText(activeDays: Int): String {
    when (activeDays) {
        LoopDay.EVERYDAY -> return stringResource(R.string.everyday)
        LoopDay.WEEKDAYS -> return stringResource(R.string.weekdays)
        LoopDay.WEEKENDS -> return stringResource(R.string.weekends)
        0 -> return stringResource(R.string.revision_no_days)
    }
    val labels = (1..7).mapNotNull { day ->
        val mask = if (day == 7) LoopDay.SUNDAY else 1 shl day
        if (activeDays and mask != 0) stringResource(DAYS_WITH_3CHARS[day - 1]) else null
    }
    return labels.joinToString(" · ")
}
