package com.pnd.android.loop.ui.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.input.selector.InputSelector
import com.pnd.android.loop.ui.input.selector.Selectors
import com.pnd.android.loop.util.BackPressHandler
import com.pnd.android.loop.util.rememberImeOpenState
import kotlinx.coroutines.launch


@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    loop: LoopVo,
    onLoopUpdated: (LoopVo) -> Unit,
    onLoopSubmitted: (LoopVo) -> Unit,
    isEditing: Boolean = false,
) {
    var currInputSelector by rememberSaveable { mutableStateOf(InputSelector.NONE) }
    var prevInputSelector by rememberSaveable { mutableStateOf(InputSelector.NONE) }
    var titleTextField by remember { mutableStateOf(TextFieldValue()) }

    val keyboardShown by rememberImeOpenState()
    val focusRequester = FocusRequester()

    SideEffect {
        if (!keyboardShown && prevInputSelector == InputSelector.NONE) {
            focusRequester.requestFocus()
        }
    }

    OverrideBackPress(
        inputSelector = currInputSelector
    ) { inputSelector ->
        currInputSelector = inputSelector
    }


    Column(modifier) {
        Divider()
        UserInputText(
            textField = titleTextField,
            hasFocus = keyboardShown,
            onTextChanged = { textFiledValue -> titleTextField = textFiledValue }
        ) { focused ->
            if (focused) {
                prevInputSelector = currInputSelector
                currInputSelector = InputSelector.NONE
            }
        }

        UserInputButtons(
            submitEnabled = titleTextField.text.isNotBlank(),
            onSubmitted = {
                onLoopSubmitted(loop.copy(title = titleTextField.text))

                titleTextField = TextFieldValue()
                onLoopUpdated(LoopVo.default())
            },
            prevInputSelector = prevInputSelector,
            currInputSelector = currInputSelector,
            onInputSelectorChanged = { selector ->
                prevInputSelector = currInputSelector
                currInputSelector = if (selector == currInputSelector) {
                    InputSelector.NONE
                } else {
                    selector
                }
            },
            isEditing = isEditing
        )

        Selectors(
            focusRequester = focusRequester,
            currentSelector = currInputSelector,
            loop = loop,
            onLoopUpdated = onLoopUpdated
        )
    }
}

@Composable
private fun OverrideBackPress(
    inputSelector: InputSelector,
    onInputSelectorChanged: (InputSelector) -> Unit,
) {
    if (inputSelector != InputSelector.NONE) {
        BackPressHandler(
            onBackPressed = {
                onInputSelectorChanged(InputSelector.NONE)
            }
        )
    }
}

