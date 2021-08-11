package com.pnd.android.loop.ui.input.selector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.InsertEmoticon
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.twotone.HourglassBottom
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.SelectorButton

@Composable
fun UserInputSelector(
    modifier: Modifier = Modifier,
    onSelectorChange: (InputSelector) -> Unit,
    selectedInterval: Long,
    canSubmitLoop: Boolean,
    onLoopSubmitted: (Boolean) -> Unit,
    currentInputSelector: InputSelector,
    isEditing: Boolean
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .wrapContentHeight()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {

        UserInputSelectorFunctionButtons(
            onSelectorChange = onSelectorChange,
            currentInputSelector = currentInputSelector
        )
        Spacer(modifier = Modifier.weight(1f))

        UserInputSubmitButton(
            sendMessageEnabled = canSubmitLoop,
            onLoopSubmitted = onLoopSubmitted,
            isEditing = isEditing
        )
    }
}

@Composable
fun UserInputSelectorFunctionButtons(
    onSelectorChange: (InputSelector) -> Unit,
    currentInputSelector: InputSelector
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
}

@Composable
fun UserInputSubmitButton(
    sendMessageEnabled: Boolean,
    onLoopSubmitted: (Boolean) -> Unit,
    isEditing:Boolean
) {
    Button(
        enabled = sendMessageEnabled,
        onClick = { onLoopSubmitted(isEditing) },
        shape = CircleShape,
        colors = submitButtonColors(),
        border = submitButtonBorder(borderEnabled = sendMessageEnabled),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            if (isEditing) Icons.Outlined.ModeEdit else Icons.Filled.Add,
            contentDescription = stringResource(if (isEditing) R.string.edit else R.string.add)
        )
    }
}

@Composable
private fun submitButtonColors(): ButtonColors {
    return ButtonDefaults.buttonColors(
        disabledBackgroundColor = MaterialTheme.colors.surface,
        disabledContentColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled)
    )
}

@Composable
private fun submitButtonBorder(borderEnabled: Boolean): BorderStroke? {
    return if (!borderEnabled) {
        BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        )
    } else {
        null
    }
}