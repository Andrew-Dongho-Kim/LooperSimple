package com.pnd.android.loop.ui.home.loop.input

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.twotone.HourglassBottom
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.loop.input.selector.InputSelector
import com.pnd.android.loop.ui.theme.elevatedSurface

@Composable
fun UserInputButtons(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    onSubmitted: () -> Unit,
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
            inputState = inputState,
        )

        Spacer(modifier = Modifier.weight(1f))
        UserInputSubmitButton(
            enabled = !inputState.isTitleEmpty,
            onSubmitted = onSubmitted,
            isEditing = isEditing
        )
    }
}

@Composable
private fun UserInputSelectorButtons(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
) {
    Box(modifier = modifier) {
        val buttonSize = dimensionResource(id = R.dimen.user_input_selector_button_size)
        UserInputButtonsIndicator(
            inputState = inputState,
            size = buttonSize,
        )
        Row {
            SelectorButton(
                modifier = Modifier.size(buttonSize),
                icon = Icons.Filled.FiberManualRecord,
                contentDescription = stringResource(id = R.string.desc_color_selector)
            ) { inputState.setSelector(InputSelector.COLOR) }

            SelectorButton(
                modifier = Modifier.size(buttonSize),
                icon = Icons.Outlined.Timelapse,
                contentDescription = stringResource(id = R.string.desc_time_selector)
            ) { inputState.setSelector(InputSelector.START_END_TIME) }

            SelectorButton(
                modifier = Modifier.size(buttonSize),
                icon = Icons.TwoTone.HourglassBottom,
                contentDescription = stringResource(id = R.string.desc_interval_selector)
            ) { inputState.setSelector(InputSelector.ALARM_INTERVAL) }
        }
    }
}

@Composable
private fun UserInputButtonsIndicator(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    size: Dp,
) {
    val currSelector = inputState.currSelector
    val prevSelector = inputState.prevSelector

    if (currSelector == InputSelector.NONE) return

    val offsetX by animateDpAsState(
        targetValue = size * currSelector.ordinal,
        animationSpec = tween(
            durationMillis = if (prevSelector == InputSelector.NONE) 0 else 300,
            easing = LinearOutSlowInEasing,
        ),
        label = ""
    )

    val backgroundColor = MaterialTheme.colors.elevatedSurface(3.dp)
    val borderColor = MaterialTheme.colors.onSurface.copy(alpha = 0.3f)
    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToPx(), 0) }
            .size(size)
            .drawBehind {
                drawRoundRect(
                    color = backgroundColor,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
                )
                drawRoundRect(
                    color = borderColor,
                    cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx()),
                    style = Stroke(width = 0.5.dp.toPx())
                )
            }
    )
}

@Composable
private fun SelectorButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Icon(
            imageVector = icon,
            tint = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
            contentDescription = contentDescription
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
        colors = ButtonDefaults.buttonColors(
            disabledBackgroundColor = MaterialTheme.colors.surface,
            disabledContentColor = MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium),
            contentColor = Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.12f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = if (isEditing) Icons.Outlined.ModeEdit else Icons.Filled.Add,
            contentDescription = stringResource(if (isEditing) R.string.edit else R.string.add),
        )
    }
}