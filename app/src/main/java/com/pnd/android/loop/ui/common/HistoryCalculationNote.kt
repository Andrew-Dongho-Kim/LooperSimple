package com.pnd.android.loop.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.outlineVariant

@Composable
fun HistoryCalculationNote(
    estimated: Boolean,
    modifier: Modifier = Modifier,
    @StringRes titleResId: Int = R.string.history_calculation_help,
    @StringRes explanationResId: Int = R.string.history_rate_basis,
    additionalNotes: List<String> = emptyList(),
) {
    var showingHelp by rememberSaveable { mutableStateOf(false) }
    val title = stringResource(titleResId)

    AppBarIcon(
        modifier = modifier
            .background(AppColor.surfaceContainer, CircleShape)
            .border(1.dp, AppColor.outlineVariant, CircleShape)
            .semantics { role = Role.Button },
        imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
        color = AppColor.primary,
        descriptionResId = titleResId,
        onClick = { showingHelp = true },
    )

    if (showingHelp) {
        AppDialog(onDismiss = { showingHelp = false }) {
            Text(title, style = AppTypography.headlineSmall, color = AppColor.onSurface)
            Column(
                modifier = Modifier.weight(1f, fill = false).padding(top = 14.dp).heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    stringResource(explanationResId),
                    style = AppTypography.bodyMedium,
                    color = AppColor.onSurfaceVariant,
                )
                if (estimated) {
                    Text(
                        stringResource(R.string.history_estimated),
                        style = AppTypography.bodyMedium,
                        color = AppColor.onSurfaceVariant,
                    )
                }
                additionalNotes.forEach { note ->
                    Text(note, style = AppTypography.bodyMedium, color = AppColor.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 24.dp),
                horizontalArrangement = Arrangement.End,
            ) {
                TextButton(onClick = { showingHelp = false }) {
                    Text(
                        stringResource(R.string.ok),
                        style = AppTypography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = AppColor.primary,
                    )
                }
            }
        }
    }
}
