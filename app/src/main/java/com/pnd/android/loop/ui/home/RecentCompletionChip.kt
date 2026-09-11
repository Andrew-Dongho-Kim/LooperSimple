package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppDialog
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.surfaceContainer
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@Composable
internal fun RecentCompletionChip(loopTitle: String, completion: RecentLoopCompletion) {
    var showExplanation by rememberSaveable { mutableStateOf(false) }
    val explanationLabel = stringResource(R.string.recent_completion_explain)
    val accessibleLabel = stringResource(
        R.string.recent_completion_accessibility, loopTitle, completion.percent,
    )
    // A neutral label, not a success/warning badge. clickable keeps its own click semantics
    // and consumes the tap, so opening the explanation does not open the parent loop detail.
    Text(
        text = stringResource(R.string.recent_completion_chip, completion.percent),
        style = AppTypography.labelMedium.copy(color = AppColor.onSurfaceVariant),
        modifier = Modifier
            .clip(RoundShapes.small)
            .background(AppColor.surfaceContainer)
            .clickable(role = Role.Button, onClickLabel = explanationLabel) { showExplanation = true }
            .semantics { contentDescription = accessibleLabel }
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
    if (showExplanation) {
        RecentCompletionDialog(loopTitle, completion, onDismiss = { showExplanation = false })
    }
}

@Composable
private fun RecentCompletionDialog(
    loopTitle: String,
    completion: RecentLoopCompletion,
    onDismiss: () -> Unit,
) {
    val configuration = LocalConfiguration.current
    val dateFormat = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(configuration.locales[0])
    AppDialog(
        onDismiss = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
        modifier = Modifier.widthIn(max = 520.dp)
            .heightIn(max = (configuration.screenHeightDp * 0.85f).dp),
    ) {
        // All text and the close action remain reachable in landscape and at large font sizes.
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(R.string.recent_completion_title),
                style = AppTypography.titleLarge.copy(color = AppColor.onSurface),
            )
            Text(loopTitle, style = AppTypography.titleMedium.copy(color = AppColor.onSurface))
            Text(
                stringResource(
                    R.string.recent_completion_period,
                    completion.start.format(dateFormat), completion.end.format(dateFormat),
                ),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant),
            )
            Column(
                modifier = Modifier.fillMaxWidth().clip(RoundShapes.medium)
                    .background(AppColor.surfaceContainer).padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(
                    stringResource(R.string.recent_completion_percent, completion.percent),
                    style = AppTypography.headlineMedium.copy(color = AppColor.onSurface),
                )
                Text(
                    stringResource(R.string.recent_completion_formula, completion.doneCount, completion.totalCount),
                    style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
                )
                Text(
                    stringResource(R.string.recent_completion_rounding),
                    style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant),
                )
            }
            CompletionCountRow(stringResource(R.string.recent_completion_done), completion.doneCount)
            CompletionCountRow(stringResource(R.string.recent_completion_skipped), completion.skipCount)
            CompletionCountRow(stringResource(R.string.recent_completion_unanswered), completion.unansweredCount)
            HorizontalDivider(color = AppColor.outlineVariant)
            CompletionCountRow(stringResource(R.string.recent_completion_total), completion.totalCount)
            Text(
                stringResource(R.string.recent_completion_rules),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant),
            )
            Text(
                stringResource(R.string.recent_completion_schedule_note),
                style = AppTypography.bodySmall.copy(color = AppColor.onSurfaceVariant),
            )
            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text(stringResource(R.string.recent_completion_close))
            }
        }
    }
}

@Composable
private fun CompletionCountRow(label: String, count: Int) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(label, modifier = Modifier.weight(1f), style = AppTypography.bodyMedium.copy(color = AppColor.onSurface))
        Text(
            stringResource(R.string.recent_completion_count, count),
            style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
        )
    }
}
