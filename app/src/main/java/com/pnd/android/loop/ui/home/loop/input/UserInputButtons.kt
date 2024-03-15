package com.pnd.android.loop.ui.home.loop.input

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.Timelapse
import androidx.compose.material.icons.twotone.HourglassBottom
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface

@Composable
fun UserInputButtons(
    modifier: Modifier = Modifier,
    inputState: UserInputState,
    isOpen: Boolean,
    onInputOpenToggle: (Boolean) -> Unit,
    onSubmitted: () -> Unit,
) {
    val overrideModifier = if (isOpen) {
        modifier.clickable(enabled = false, onClick = {})
    } else {
        modifier
    }

    Row(
        modifier = overrideModifier
            .background(color = Color.Transparent)
            .height(56.dp)
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (isOpen) {
            UserInputSelectorButtons(
                inputState = inputState,
            )
        }
        Spacer(modifier = Modifier.weight(1f))

        if (isOpen && inputState.mode != UserInputState.Mode.None) {
            UserInputSubmitButton(
                enabled = !inputState.isTitleEmpty,
                onSubmitted = onSubmitted,
                isEditing = inputState.mode == UserInputState.Mode.Edit
            )
        }

        if (inputState.mode == UserInputState.Mode.None) {
            UserInputOpenButton(
                isOpen = isOpen,
                onInputOpenToggle = onInputOpenToggle,
            )
        }
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

    val backgroundColor = AppColor.surface.compositeOverOnSurface(alpha = 0.9f)
    val borderColor = AppColor.onSurface.copy(alpha = 0.3f)
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
            tint = AppColor.onSurface.copy(alpha = 0.6f),
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
            disabledContainerColor = AppColor.surface,
            disabledContentColor = AppColor.onSurface.copy(alpha = 0.2f),
            contentColor = Color.White
        ),
        border = BorderStroke(
            width = 1.dp,
            color = AppColor.onSurface.copy(alpha = 0.12f)
        ),
        contentPadding = PaddingValues(0.dp)
    ) {
        Icon(
            imageVector = if (isEditing) Icons.Outlined.ModeEdit else Icons.Filled.Add,
            contentDescription = stringResource(if (isEditing) R.string.edit else R.string.add),
        )
    }
}


@Composable
private fun UserInputOpenButton(
    modifier: Modifier = Modifier,
    isOpen: Boolean,
    onInputOpenToggle: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .width(90.dp)
            .height(110.dp)
    ) {
        if (!isOpen) {
            Box(
                modifier = Modifier
                    .clip(CircleShape)
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(color = AppColor.surface.copy(alpha = 0.4f))
                    .graphicsLayer {
                        this.renderEffect = BlurEffect(
                            radiusX = 100f,
                            radiusY = 100f,
                            edgeTreatment = TileMode.Clamp
                        )
                    })
        }
        Button(
            modifier = modifier.align(if (isOpen) Alignment.BottomEnd else Alignment.Center),
            onClick = { onInputOpenToggle(!isOpen) },
            shape = CircleShape,
            colors = ButtonDefaults.buttonColors(
                containerColor = AppColor.surface,
            ),
            border = BorderStroke(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.3f)
            ),
            contentPadding = PaddingValues(0.dp)
        ) {
            Icon(
                imageVector = if (isOpen) {
                    Icons.AutoMirrored.Outlined.ArrowForward
                } else {
                    Icons.AutoMirrored.Outlined.ArrowBack
                },
                tint = AppColor.primary.copy(alpha = 0.8f),
                contentDescription = ""
            )
        }
    }
}