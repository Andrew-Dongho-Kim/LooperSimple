package com.pnd.android.loop.ui.home.group

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppDialog
import com.pnd.android.loop.ui.common.DialogButtons
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary

@Composable
fun CreateGroupDialog(
    modifier: Modifier = Modifier,
    onCreate: (CharSequence) -> Unit,
    onDismiss: () -> Unit,
) {
    val textField = rememberTextFieldState()
    AppDialog(modifier = modifier, onDismiss = onDismiss) {
        // 입력 전에는 placeholder를, 입력이 시작되면 그 위에 실제 입력 텍스트를 겹쳐 보여준다.
        Box {
            if (textField.text.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.enter_group_title),
                    style = AppTypography.titleMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.3f)
                    ),
                )
            }
            BasicTextField(
                state = textField,
                textStyle = AppTypography.titleMedium.copy(
                    color = AppColor.onSurface
                ),
                cursorBrush = SolidColor(value = AppColor.primary),
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text,
                    imeAction = ImeAction.Done
                ),
            )
        }

        DialogButtons(
            confirmText = stringResource(id = R.string.ok),
            onConfirm = {
                // 이름이 비어 있으면 무시해, 이름 없는 그룹이 생기지 않게 한다.
                if (textField.text.isNotBlank()) {
                    onCreate(textField.text)
                    onDismiss()
                }
            },
            onCancel = onDismiss,
        )
    }
}

@Composable
fun DeleteDialog(
    modifier: Modifier = Modifier,
    title: String,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(modifier = modifier, onDismiss = onDismiss) {
        Text(
            text = title,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface
            ),
        )

        // 그룹 삭제도 되돌릴 수 없으므로 확인 버튼을 error 색으로 표시(destructive).
        DialogButtons(
            confirmText = stringResource(id = R.string.delete),
            destructive = true,
            onConfirm = {
                onDelete()
                onDismiss()
            },
            onCancel = onDismiss,
        )
    }
}
