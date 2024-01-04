package com.pnd.android.loop.ui.home.loop.input

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.loop.input.selector.InputSelector
import com.pnd.android.loop.ui.home.loop.input.selector.Selectors
import com.pnd.android.loop.util.BackPressHandler
import com.pnd.android.loop.util.rememberImeOpenState


@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    lazyListState: LazyListState,
    onLoopSubmitted: (LoopBase) -> Unit,
    isEditing: Boolean = false,
) {
    val keyboardShown by rememberImeOpenState()
    val focusRequester = FocusRequester()

    SideEffect {
        if (!keyboardShown && inputState.prevSelector == InputSelector.NONE) {
            focusRequester.requestFocus()
        }
    }

    OverrideBackPress(
        inputSelector = inputState.currSelector
    ) { inputSelector ->
        inputState.setSelector(inputSelector)
    }


    Column(modifier) {
        Divider()
        UserInputText(
            textField = inputState.textFieldValue,
            hasFocus = keyboardShown,
            onTextChanged = { textFiledValue -> inputState.update(title = textFiledValue) },
            onTextFieldFocused = { focused ->
                if (focused) {
                    inputState.setSelector(InputSelector.NONE)
                }
            }
        )

        UserInputButtons(
            inputState = inputState,
            onSubmitted = {
                onLoopSubmitted(inputState.value)
                inputState.reset()
            },
            isEditing = isEditing
        )

        Selectors(
            inputState = inputState,
            focusRequester = focusRequester,
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

