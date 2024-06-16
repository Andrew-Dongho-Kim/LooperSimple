package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface

@Composable
fun DeleteLoopDialog(
    modifier: Modifier = Modifier,
    loopTitle: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = modifier.padding(horizontal = 32.dp),
        shape = RoundShapes.medium,
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = "\"$loopTitle\"\n\n${stringResource(id = R.string.delete_confirm_message)}",
                style = AppTypography.titleMedium.copy(
                    color = AppColor.onSurface
                ),
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    style = AppTypography.titleMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismiss()
            }) {
                Text(
                    text = stringResource(id = R.string.ok),
                    style = AppTypography.titleMedium,
                )
            }
        },
        containerColor = AppColor.surface,
        tonalElevation = 0.dp,
    )
}
