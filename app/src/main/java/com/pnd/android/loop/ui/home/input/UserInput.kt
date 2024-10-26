package com.pnd.android.loop.ui.home.input

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.input.selector.Selectors
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.BackPressHandler
import com.pnd.android.loop.util.rememberImeOpenState
import kotlinx.coroutines.launch


@Composable
fun UserInput(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
    lazyListState: LazyListState,
    onEnsureLoop: suspend (LoopBase) -> Boolean,
    onLoopSubmitted: (LoopBase) -> Unit,
) {
    val keyboardShown by rememberImeOpenState()
    val focusRequester = FocusRequester()

    val coroutineScope = rememberCoroutineScope()
    SideEffect {
        if (!keyboardShown) focusRequester.requestFocus()

        if (inputState.mode == UserInputState.Mode.New) {
            coroutineScope.launch { lazyListState.animateScrollToItem(0) }
        }
    }

    OverrideBackPress(inputState = inputState)

    Column(
        modifier
            .animateContentSize()
            .background(color = if (inputState.isVisible) AppColor.surface else Color.Transparent)

    ) {
        if (inputState.isVisible) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.3f)
            )
            UserInputText(
                textField = inputState.textFieldValue,
                hasFocus = keyboardShown,
                onTextChanged = { textFiledValue -> inputState.update(title = textFiledValue) },
                onTextFieldFocused = { focused ->
                    inputState.setTextFieldFocused(focused)
                    if (focused) {
                        inputState.setSelector(InputSelector.NONE)
                    }
                }
            )
        }

        UserInputButtons(
            inputState = inputState,
            onSubmitted = {
                coroutineScope.launch {
                    val loop = inputState.value
                    if (onEnsureLoop(loop)) {
                        onLoopSubmitted(loop)
                        inputState.reset()
                    }
                }
            },
        )

        Selectors(
            inputState = inputState,
            blurState = blurState,
            snackBarHostState = snackBarHostState,
            focusRequester = focusRequester,
        )
    }

}


@Composable
private fun OverrideBackPress(
    inputState: UserInputState,
) {
    if (inputState.currSelector != InputSelector.NONE) {
        BackPressHandler {
            inputState.setSelector(InputSelector.NONE)
        }
    } else if (inputState.mode != UserInputState.Mode.None) {
        BackPressHandler {
            inputState.reset()
        }
    }
}