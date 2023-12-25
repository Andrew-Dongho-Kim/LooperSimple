package com.pnd.android.loop.ui.input

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.SelectorButton
import com.pnd.android.loop.ui.input.selector.InputSelector

@Composable
fun UserInputButtons(
    modifier: Modifier = Modifier,
    submitEnabled: Boolean,
    onSubmitted: () -> Unit,
    inputSelector: InputSelector,
    onInputSelectorChanged: (InputSelector) -> Unit,
    isEditing: Boolean = false,
) {
    Row(
        modifier = modifier
            .height(56.dp)
            .wrapContentHeight()
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        UserInputSelectorButtons(
            inputSelector = inputSelector,
            onInputSelectorChanged = onInputSelectorChanged,
        )

        Spacer(modifier = Modifier.weight(1f))
        UserInputSubmitButton(
            enabled = submitEnabled,
            onSubmitted = onSubmitted,
            isEditing = isEditing
        )
    }
}

@Composable
private fun UserInputSelectorButtons(
    modifier: Modifier = Modifier,
    inputSelector: InputSelector,
    onInputSelectorChanged: (InputSelector) -> Unit,
) {
    Log.d("DDEBUG2", "UserInputSelectorButtons:$inputSelector")
    Row(modifier = modifier) {
        SelectorButton(
            onClick = { onInputSelectorChanged(InputSelector.COLOR) },
            icon = Icons.Outlined.InsertEmoticon,
            selected = inputSelector == InputSelector.COLOR,
            contentDescription = stringResource(id = R.string.desc_color_selector)
        )
        SelectorButton(
            onClick = { onInputSelectorChanged(InputSelector.ALARM_INTERVAL) },
            icon = Icons.TwoTone.HourglassBottom,
            selected = inputSelector == InputSelector.ALARM_INTERVAL,
            contentDescription = stringResource(id = R.string.desc_alarm_interval)
        )
        SelectorButton(
            onClick = { onInputSelectorChanged(InputSelector.START_END_TIME) },
            icon = Icons.Outlined.Timelapse,
            selected = inputSelector == InputSelector.START_END_TIME,
            contentDescription = stringResource(id = R.string.desc_allowed_time)
        )
        SelectorButton(
            onClick = { onInputSelectorChanged(InputSelector.ALARMS) },
            icon = Icons.Outlined.VolumeUp,
            selected = inputSelector == InputSelector.ALARMS,
            contentDescription = stringResource(id = R.string.desc_notification)
        )
    }
}

@Composable
private fun UserInputSubmitButton(
    modifier: Modifier = Modifier,
    isEditing: Boolean,
    onSubmitted: () -> Unit,
    enabled: Boolean,
) {
    Button(
        modifier = modifier,
        enabled = enabled,
        onClick = { onSubmitted() },
        shape = CircleShape,
        colors = submitButtonColors(),
        border = submitButtonBorder(borderEnabled = enabled),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = if (isEditing) Icons.Outlined.ModeEdit else Icons.Filled.Add,
            contentDescription = stringResource(if (isEditing) R.string.edit else R.string.add),
        )
    }
}

@Composable
private fun submitButtonColors(): ButtonColors {
    return ButtonDefaults.buttonColors(
        disabledBackgroundColor = MaterialTheme.colors.surface,
        disabledContentColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.disabled),
        contentColor = Color.White
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