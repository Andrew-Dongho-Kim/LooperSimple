package com.pnd.android.loop.ui.input

import android.util.Log
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.home.loop.LoopCardActiveDays
import com.pnd.android.loop.ui.input.selector.InputSelector
import com.pnd.android.loop.ui.input.selector.Selectors
import com.pnd.android.loop.util.BackPressHandler
import com.pnd.android.loop.util.h2m2
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.textFormatter
import kotlinx.coroutines.launch

private val logger = log("UserInput")


@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    scrollState: ScrollState,
    loop: LoopVo,
    onLoopUpdated: (LoopVo) -> Unit,
    onLoopSubmitted: (LoopVo) -> Unit,
    isEditing: Boolean = false,
) {
    var currSelector by rememberSaveable { mutableStateOf(InputSelector.NONE) }
    var titleTextField by remember { mutableStateOf(TextFieldValue()) }

    var hasInputFocus by remember { mutableStateOf(false) }
    // Request focus to force the TextField to lose it
    // If the selector is shown, always request focus to trigger a TextField.onFocusChange.
    val focusRequester = FocusRequester()
    OverrideBackPress(
        inputSelector = currSelector
    ) { inputSelector ->
        currSelector = inputSelector
    }


    val coroutineScope = rememberCoroutineScope()
    val scrollToBottom = remember {
        { coroutineScope.launch { scrollState.animateScrollTo(0) } }
    }
    Column(modifier) {
        Divider()
        // Used to decide if the keyboard should be shown
        UserInputText(
            textField = titleTextField,
            hasFocus = hasInputFocus,
            onTextChanged = { textFiledValue -> titleTextField = textFiledValue }
        ) { focused ->
            if (focused) {
                currSelector = InputSelector.NONE
                scrollToBottom()
            }
            hasInputFocus = focused
        }
        if (hasInputFocus || currSelector != InputSelector.NONE) {
            UserInputSummary(modifier = Modifier, loopVo = loop)
        }

        UserInputButtons(
            submitEnabled = titleTextField.text.isNotBlank(),
            onSubmitted = {
                onLoopSubmitted(loop.copy(title = titleTextField.text))

                titleTextField = TextFieldValue()
                scrollToBottom()
                onLoopUpdated(LoopVo.default())
            },
            inputSelector = currSelector,
            onInputSelectorChanged = { selector ->
                currSelector = if (selector == currSelector) {
                    InputSelector.NONE
                } else {
                    selector
                }
            },
            isEditing = isEditing
        )

        Selectors(
            focusRequester = focusRequester,
            currentSelector = currSelector,
            loop = loop,
            onLoopUpdated = onLoopUpdated
        )
    }
}


@Composable
private fun UserInputSummary(
    modifier: Modifier,
    loopVo: LoopVo
) {
    Row(modifier.padding(start = 16.dp)) {
        Text(
            modifier = Modifier.width(110.dp),
            text = "${h2m2(loopVo.loopStart)} ~ ${h2m2(loopVo.loopEnd)}",
            style = MaterialTheme.typography.caption.copy(
                color = MaterialTheme.colors.onSurface
            ),
        )
        Text(
            text = textFormatter(
                intervalString(loopVo.interval)
            ),
            style = MaterialTheme.typography.caption.copy(
                color = MaterialTheme.colors.onSurface
            ),
            modifier = Modifier.width(70.dp)
        )

        LoopCardActiveDays(loop = loopVo)
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

