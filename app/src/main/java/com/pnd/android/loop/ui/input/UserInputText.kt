package com.pnd.android.loop.ui.input

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.SemanticsPropertyKey
import androidx.compose.ui.semantics.SemanticsPropertyReceiver
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R

val KeyboardShownKey = SemanticsPropertyKey<Boolean>("KeyboardShownKey")
var SemanticsPropertyReceiver.keyboardShownProperty by KeyboardShownKey

@Composable
fun UserInputText(
    onTextChanged: (TextFieldValue) -> Unit,
    onTextFieldFocused: (Boolean) -> Unit,
    focusState: Boolean,
    textFieldValue: TextFieldValue,
    keyboardType: KeyboardType = KeyboardType.Text,
    keyboardShown: Boolean,
) {
    val descText = stringResource(R.string.desc_enter_loop_title)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .semantics {
                contentDescription = descText
                keyboardShownProperty = keyboardShown
            },
        horizontalArrangement = Arrangement.End
    ) {
        Surface {
            Box(
                modifier = Modifier
                    .height(48.dp)
                    .weight(1f)
                    .align(Alignment.Bottom)
            ) {
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { onTextChanged(it) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp)
                        .align(Alignment.CenterStart)
                        .onFocusChanged { state -> onTextFieldFocused(state.isFocused) },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = keyboardType,
                        imeAction = ImeAction.Send
                    ),
                    maxLines = 1,
                    cursorBrush = SolidColor(LocalContentColor.current),
                    textStyle = LocalTextStyle.current.copy(color = LocalContentColor.current)
                )

                if (textFieldValue.text.isEmpty() && !focusState) {
                    val disableContentColor =
                        MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
                    Text(
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 16.dp),
                        text = descText,
                        style = MaterialTheme.typography.body1.copy(color = disableContentColor)
                    )
                }
            }

        }
    }
}