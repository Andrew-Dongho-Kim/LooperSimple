package com.pnd.android.loop.ui.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.InsertEmoticon
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.twotone.HourglassBottom
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
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
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.common.SelectorButton
import com.pnd.android.loop.ui.input.selector.InputSelector
import com.pnd.android.loop.ui.input.selector.Selectors
import com.pnd.android.loop.ui.theme.compositedOnSurface
import com.pnd.android.loop.ui.theme.elevatedSurface
import com.pnd.android.loop.util.BackPressHandler
import kotlinx.coroutines.launch

private val logger = log("UserInput")


@Composable
fun UserInput(
    defaultLoop: LoopVo = LoopVo(),
    onInputEntered: (LoopVo) -> Unit,
    scrollState: ScrollState,
    editedLoop: MutableState<LoopVo?>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var currentInputSelector by rememberSaveable { mutableStateOf(InputSelector.NONE) }
    val dismissKeyboard = { currentInputSelector = InputSelector.NONE }

    // Intercept back navigation if there's a InputSelector visible
    if (currentInputSelector != InputSelector.NONE) {
        BackPressHandler(onBackPressed = dismissKeyboard)
    }

    val loop by remember { mutableStateOf(defaultLoop) }
    var textState by remember { mutableStateOf(TextFieldValue()) }
    textState = TextFieldValue(text = editedLoop.value?.title ?: "")

    logger.d { "TEST-DH, USER INPUT REBIND : ${editedLoop.value?.title}" }

    // Used to decide if the keyboard should be shown
    var textFieldFocusState by remember { mutableStateOf(false) }

    Column(modifier) {
        Divider()
        UserInputText(
            onTextChanged = {
                textState = it
                loop.title = textState.text
            },
            onTextFieldFocused = { focused ->
                logger.d { "onTextFieldFocused : $focused" }
                if (focused) {
                    currentInputSelector = InputSelector.NONE
                    scope.launch { scrollState.animateScrollTo(0) }
                }
                textFieldFocusState = focused
            },
            focusState = textFieldFocusState,
            textFieldValue = textState,
            keyboardShown = currentInputSelector == InputSelector.NONE && textFieldFocusState
        )
        UserInputSelector(
            onSelectorChange = { selector ->
                currentInputSelector = if (currentInputSelector == selector) {
                    InputSelector.NONE
                } else {
                    selector
                }
            },
            selectedInterval = loop.interval,
            sendMessageEnabled = textState.text.isNotBlank(),
            onMessageSent = {
                loop.id = 0
                loop.tickStart = 0

                onInputEntered(loop)

                // Reset text field and close keyboard
                textState = TextFieldValue()
                // Move scroll to bottom
                scope.launch { scrollState.animateScrollTo(0) }
                dismissKeyboard()
            },
            currentInputSelector = currentInputSelector
        )

        Selectors(currentSelector = currentInputSelector, loop = loop)
    }
}


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

@Composable
private fun UserInputSelector(
    onSelectorChange: (InputSelector) -> Unit,
    selectedInterval: Long,
    sendMessageEnabled: Boolean,
    onMessageSent: () -> Unit,
    currentInputSelector: InputSelector,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .wrapContentHeight()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SelectorButton(
            onClick = { onSelectorChange(InputSelector.ICONS) },
            icon = Icons.Outlined.InsertEmoticon,
            selected = currentInputSelector == InputSelector.ICONS,
            contentDescription = stringResource(id = R.string.desc_icon_selector)
        )
        SelectorButton(
            onClick = { onSelectorChange(InputSelector.ALARM_INTERVAL) },
            icon = Icons.TwoTone.HourglassBottom,
            selected = currentInputSelector == InputSelector.ALARM_INTERVAL,
            contentDescription = stringResource(id = R.string.desc_alarm_interval)
        )
        SelectorButton(
            onClick = { onSelectorChange(InputSelector.START_END_TIME) },
            icon = Icons.Outlined.Timelapse,
            selected = currentInputSelector == InputSelector.START_END_TIME,
            contentDescription = stringResource(id = R.string.desc_allowed_time)
        )
        SelectorButton(
            onClick = { onSelectorChange(InputSelector.ALARMS) },
            icon = Icons.Outlined.VolumeUp,
            selected = currentInputSelector == InputSelector.ALARMS,
            contentDescription = stringResource(id = R.string.desc_notification)
        )

        val border = if (!sendMessageEnabled) {
            BorderStroke(
                width = 1.dp,
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
            )
        } else {
            null
        }
        Spacer(modifier = Modifier.weight(1f))

        val disabledContentColor =
            MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)

        val buttonColors = ButtonDefaults.buttonColors(
            disabledBackgroundColor = MaterialTheme.colors.surface,
            disabledContentColor = disabledContentColor
        )

        Button(
            enabled = sendMessageEnabled,
            onClick = onMessageSent,
            shape = CircleShape,
            colors = buttonColors,
            border = border,
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                Icons.Filled.Add,
                contentDescription = stringResource(R.string.add)
            )
        }
    }
}

@Composable
fun getSelectorExpandedColor(): Color {
    return if (MaterialTheme.colors.isLight) {
        MaterialTheme.colors.compositedOnSurface(0.04f)
    } else {
        MaterialTheme.colors.elevatedSurface(8.dp)
    }
}