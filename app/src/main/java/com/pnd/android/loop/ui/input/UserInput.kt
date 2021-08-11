package com.pnd.android.loop.ui.input

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Divider
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.input.selector.InputSelector
import com.pnd.android.loop.ui.input.selector.Selectors
import com.pnd.android.loop.ui.input.selector.UserInputSelector
import com.pnd.android.loop.util.BackPressHandler
import kotlinx.coroutines.launch

private val logger = log("UserInput")


@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    scrollStateLoops: ScrollState,
    onLoopSubmitted: (LoopVo) -> Unit,
    loopInEdit: MutableState<LoopVo?>
) {
    val isEditing = loopInEdit.value != null

    val loop = remember { mutableStateOf(LoopVo()) }
    val loopTitle = remember { mutableStateOf(TextFieldValue()) }
    FillLoopInEdit(
        loopInEdit = loopInEdit.value,
        currLoop = loop,
        currLoopTitle = loopTitle
    )

    val currSelector = rememberSaveable { mutableStateOf(InputSelector.NONE) }
    val onDismiss = onDismissSelectorOrEditMode(
        currSelector = currSelector,
        loopInEdit = loopInEdit,
        currLoop = loop,
        currLoopTitle = loopTitle
    )
    OverrideBackPress(currSelector, isEditing, onDismiss)


    val onScrollToBottomOfLoops = onScrollToBottomOfLoops(scrollStateLoops)

    Column(modifier) {
        Divider()

        // Used to decide if the keyboard should be shown
        var isLoopTitleFocused by remember { mutableStateOf(false) }
        UserInputText(
            onTextChanged = {
                loopTitle.value = it
                loop.value.title = it.text
            },
            onTextFieldFocused = { focused ->
                logger.d { "onTextFieldFocused : $focused" }
                if (focused) {
                    currSelector.value = InputSelector.NONE
                    onScrollToBottomOfLoops()
                }
                isLoopTitleFocused = focused
            },
            focusState = isLoopTitleFocused,
            textFieldValue = loopTitle.value,
            keyboardShown = currSelector.value == InputSelector.NONE && isLoopTitleFocused
        )
        UserInputSelector(
            onSelectorChange = onUserInputSelectorChanged(currSelector),
            selectedInterval = loop.value.interval,
            canSubmitLoop = loopTitle.value.text.isNotBlank(),
            onLoopSubmitted = onLoopSubmitted(
                currLoop = loop,
                currLoopTitle = loopTitle,
                onDone = { loopVo ->
                    onLoopSubmitted(loopVo)
                    onScrollToBottomOfLoops()
                    onDismiss()
                },
            ),
            currentInputSelector = currSelector.value,
            isEditing = isEditing
        )

        Selectors(currentSelector = currSelector.value, loop = loop.value)
    }
}

@Composable
private fun OverrideBackPress(
    currentInputSelector: MutableState<InputSelector>,
    isEditing: Boolean,
    onDismiss: () -> Unit
) {
    // Intercept back navigation if there's a InputSelector visible
    val isSelectorOpened = currentInputSelector.value != InputSelector.NONE

    if (isSelectorOpened || isEditing) {
        BackPressHandler(onBackPressed = onDismiss)
    }
}


@Composable
private fun FillLoopInEdit(
    loopInEdit: LoopVo?,
    currLoop: MutableState<LoopVo>,
    currLoopTitle: MutableState<TextFieldValue>
) {
    if (loopInEdit != null && loopInEdit != currLoop.value) {
        currLoop.value = loopInEdit
        currLoopTitle.value = TextFieldValue(loopInEdit.title)
    }
}


@Composable
private fun onLoopSubmitted(
    currLoop: MutableState<LoopVo>,
    currLoopTitle: MutableState<TextFieldValue>,
    onDone: (LoopVo) -> Unit,
): (Boolean) -> Unit {
    return { isEditing ->
        with(currLoop.value) {
            if (!isEditing) {
                id = 0
                tickStart = 0
            }
            onDone(this)
        }
        currLoopTitle.value = TextFieldValue()
    }
}

@Composable
private fun onUserInputSelectorChanged(currentInputSelector: MutableState<InputSelector>): (InputSelector) -> Unit =
    { selector ->
        with(currentInputSelector) {
            value = if (selector == value) InputSelector.NONE else selector
        }
    }

private fun onDismissSelectorOrEditMode(
    currSelector: MutableState<InputSelector>,
    loopInEdit: MutableState<LoopVo?>,
    currLoop: MutableState<LoopVo>,
    currLoopTitle: MutableState<TextFieldValue>
): () -> Unit = {
    if (currSelector.value == InputSelector.NONE) {
        // Dismiss edit mode
        loopInEdit.value = null
        currLoop.value = LoopVo()
        currLoopTitle.value = TextFieldValue()
    } else {
        // Dismiss selector
        currSelector.value = InputSelector.NONE
    }
}


@Composable
private fun onScrollToBottomOfLoops(scrollStateLoops: ScrollState): () -> Unit {
    val scope = rememberCoroutineScope()
    return {
        scope.launch { scrollStateLoops.animateScrollTo(0) }
    }
}



