package com.pnd.android.loop.ui.home.input

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface


@Composable
fun UserInputText(
    modifier: Modifier = Modifier,
    textField: TextFieldValue,
    keyboardType: KeyboardType = KeyboardType.Text,
    hasFocus: Boolean,
    isError: Boolean = false,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
    ) {
        // Block to click under UserInputText
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .clickable(enabled = false, onClick = {})
        )
        UserInputTextField(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxWidth(),
            text = textField,
            keyboardType = keyboardType,
            onTextChanged = onTextChanged,
            onTextFieldFocused = onTextFieldFocused
        )

        if (textField.text.isEmpty()) {
            // 제출을 시도했는데 제목이 비어 있으면(오류) 포커스 여부와 무관하게 오류 안내를 띄운다.
            // 그 외에는 포커스가 없을 때만 기본 플레이스홀더를 보여 준다.
            if (isError) {
                ErrorTextField(modifier = Modifier.align(Alignment.CenterStart))
            } else if (!hasFocus) {
                EmptyTextField(modifier = Modifier.align(Alignment.CenterStart))
            }
        }
    }

}

@Composable
private fun UserInputTextField(
    modifier: Modifier = Modifier,
    text: TextFieldValue,
    keyboardType: KeyboardType,
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
) {
    BasicTextField(
        value = text,
        onValueChange = { onTextChanged(it) },
        modifier = modifier
            .fillMaxWidth()
            .padding(start = Dimens.contentPadding)
            .onFocusChanged { state -> onTextFieldFocused(state.isFocused) },
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            imeAction = ImeAction.Done
        ),
        maxLines = 1,
        cursorBrush = SolidColor(AppColor.onSurface),
        textStyle = AppTypography.titleMedium.copy(color = AppColor.onSurface)
    )
}

@Composable
private fun EmptyTextField(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(start = Dimens.contentPadding),
        text = stringResource(R.string.desc_enter_loop_title),
        style = AppTypography.titleMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.4f)
        )
    )
}

/**
 * 빈 제목으로 제출을 시도했을 때 필드 자리에 보여 주는 인라인 오류 안내. 하단 스낵바 대신
 * 입력 지점에서 바로 알려, 시선 이동 없이 무엇을 고쳐야 하는지 즉시 전달한다.
 */
@Composable
private fun ErrorTextField(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier.padding(start = Dimens.contentPadding),
        text = stringResource(R.string.warning_enter_characters_other_than_spaces),
        style = AppTypography.titleMedium.copy(
            color = AppColor.error
        )
    )
}
