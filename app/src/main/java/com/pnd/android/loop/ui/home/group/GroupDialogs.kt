package com.pnd.android.loop.ui.home.group

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface

@Composable
fun CreateGroupDialog(
    modifier: Modifier = Modifier,
    onCreate: (CharSequence) -> Unit,
    onDismiss: () -> Unit,
) {
    val textFiled = rememberTextFieldState()
    AlertDialog(
        modifier = modifier.padding(horizontal = 32.dp),
        shape = RoundShapes.medium,
        onDismissRequest = onDismiss,
        text = {
            if (textFiled.text.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.enter_group_title),
                    style = AppTypography.titleMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.3f)
                    ),
                )
            }
            BasicTextField(
                state = textFiled,
                textStyle = AppTypography.titleMedium.copy(
                    color = AppColor.onSurface
                ),
                cursorBrush = SolidColor(value = AppColor.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
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
                onCreate(textFiled.text)
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

@Composable
fun DeleteDialog(
    modifier: Modifier = Modifier,
    title: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = modifier.padding(horizontal = 32.dp),
        shape = RoundShapes.medium,
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = title,
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