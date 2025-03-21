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
import com.pnd.android.loop.ui.theme.onSurface


@Composable
fun UserInputText(
    modifier: Modifier = Modifier,
    textField: TextFieldValue,
    keyboardType: KeyboardType = KeyboardType.Text,
    hasFocus: Boolean,
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

        if (textField.text.isEmpty() && !hasFocus) {
            EmptyTextField(modifier = Modifier.align(Alignment.CenterStart))
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
            .padding(start = 16.dp)
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
        modifier = modifier.padding(start = 16.dp),
        text = stringResource(R.string.desc_enter_loop_title),
        style = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.3f)
        )
    )
}
