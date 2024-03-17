package com.pnd.android.loop.ui.home.loop.input

import android.content.Context
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.loop.input.selector.Selectors
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

    val context = LocalContext.current
    val pref = remember {
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }
    var isOpen by rememberSaveable {
        mutableStateOf(pref.getBoolean(KEY_IS_USER_INPUT_OPEN, true))
    }

    Column(
        modifier
            .animateContentSize()
            .background(color = if (isOpen) AppColor.surface else Color.Transparent)

    ) {
        if (isOpen) {
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
            isOpen = isOpen,
            onInputOpenToggle = { open ->
                isOpen = open
                pref.edit { putBoolean(KEY_IS_USER_INPUT_OPEN, isOpen) }
            },
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

private const val PREF_NAME = "user_input_pref"
private const val KEY_IS_USER_INPUT_OPEN = "is_user_input_open"