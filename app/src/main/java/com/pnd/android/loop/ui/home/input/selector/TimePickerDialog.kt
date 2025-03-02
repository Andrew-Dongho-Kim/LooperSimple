package com.pnd.android.loop.ui.home.input.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.divider
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceDark
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.secondary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.toMs
import java.time.LocalTime


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerDialog(
    localTimeStart: LocalTime,
    onStartTimeSelected: (Long) -> Unit,
    localTimeEnd: LocalTime,
    onEndTimeSelected: (Long) -> Unit,
    isAnyTime: Boolean,
    onIsAnyTimeCheckChanged: (Boolean) -> Unit,
    isStart: Boolean,
    onDismiss: () -> Unit
) {
    var isStartTime by remember { mutableStateOf(isStart) }
    val startTimePickerState = rememberTimePickerState(
        initialHour = localTimeStart.hour,
        initialMinute = localTimeStart.minute,
        is24Hour = false
    )
    val endTimePickerState = rememberTimePickerState(
        initialHour = localTimeEnd.hour,
        initialMinute = localTimeEnd.minute,
        is24Hour = false
    )

    val errorState by rememberErrorState(
        startTimePickerState = startTimePickerState,
        endTimePickerState = endTimePickerState,
        isStart = isStartTime,
        isAnyTime = isAnyTime,
    )

    BasicAlertDialog(
        modifier = Modifier
            .wrapContentHeight()
            .clip(RoundedCornerShape(size = 8.dp))
            .shadow(
                elevation = 0.5.dp,
                clip = true
            )
            .background(color = AppColor.surface.copy(alpha = 0.9f)),
        properties = DialogProperties(
            usePlatformDefaultWidth = true
        ),
        onDismissRequest = onDismiss
    ) {
        val timePickerState = if (isStartTime) startTimePickerState else endTimePickerState
        TimePickerDialogContent(
            timePickerState = timePickerState,
            errorState = errorState,
            startTimePickerState = startTimePickerState,
            onStartTimeSelected = {
                onStartTimeSelected(
                    LocalTime.of(
                        startTimePickerState.hour,
                        startTimePickerState.minute
                    ).toMs()
                )
            },
            endTimePickerState = endTimePickerState,
            onEndTimeSelected = {
                onEndTimeSelected(
                    LocalTime.of(
                        endTimePickerState.hour,
                        endTimePickerState.minute
                    ).toMs()
                )
            },
            isAnyTime = isAnyTime,
            onIsAnyTimeCheckChanged = onIsAnyTimeCheckChanged,
            isStart = isStartTime,
            onTimeTypeSelected = { isStart -> isStartTime = isStart },
            onDismiss = onDismiss,
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogContent(
    modifier: Modifier = Modifier,
    timePickerState: TimePickerState,
    errorState: ErrorState,
    startTimePickerState: TimePickerState,
    onStartTimeSelected: () -> Unit,
    endTimePickerState: TimePickerState,
    onEndTimeSelected: () -> Unit,
    isAnyTime: Boolean,
    onIsAnyTimeCheckChanged: (Boolean) -> Unit,
    isStart: Boolean,
    onTimeTypeSelected: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = modifier
            .wrapContentHeight()
    ) {
        TimePickerStartEndTime(
            modifier = Modifier
                .wrapContentWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 12.dp
                ),
            errorState = errorState,
            startTimePickerState = startTimePickerState,
            endTimePickerState = endTimePickerState,
            isAnyTime = isAnyTime,
            onIsAnyTimeCheckChanged = onIsAnyTimeCheckChanged,
            onTimeTypeSelected = onTimeTypeSelected,
            isStart = isStart,
        )

        if (errorState != ErrorState.Ok) {
            Text(
                modifier = Modifier.padding(
                    top = 12.dp,
                    start = 24.dp,
                    bottom = 12.dp,
                ),
                text = stringResource(
                    id = if (errorState == ErrorState.StartError) {
                        R.string.warning_start_time_should_be_before_end_time
                    } else {
                        R.string.warning_end_time_should_be_after_start_time
                    }
                ),
                style = AppTypography.labelLarge.copy(
                    color = AppColor.error
                )
            )
        }

        TimePicker(
            modifier = Modifier
                .padding(top = 32.dp)
                .align(Alignment.CenterHorizontally),
            state = timePickerState,
            colors = TimePickerDefaults.colors(
                clockDialColor = AppColor.onSurface.copy(alpha = 0.05f),
                clockDialSelectedContentColor = AppColor.onSurfaceDark,
                clockDialUnselectedContentColor = AppColor.onSurface,
                selectorColor = AppColor.primary,
                containerColor = AppColor.background,
                periodSelectorSelectedContainerColor = selectedBackground,
                periodSelectorUnselectedContainerColor = AppColor.surface,
                periodSelectorSelectedContentColor = AppColor.onSurface,
                periodSelectorUnselectedContentColor = AppColor.onSurface,
                timeSelectorSelectedContainerColor = selectedBackground,
                timeSelectorUnselectedContainerColor = AppColor.surface,
                timeSelectorSelectedContentColor = AppColor.onSurface,
                timeSelectorUnselectedContentColor = AppColor.onSurface,
            ),
        )

        TimePickerOkCancelButtons(
            modifier = Modifier
                .align(Alignment.End)
                .padding(
                    top = 48.dp,
                    bottom = 32.dp,
                    end = 32.dp
                ),
            errorState = errorState,
            onStartTimeSelected = onStartTimeSelected,
            onEndTimeSelected = onEndTimeSelected,
            onDismiss = onDismiss,
        )
    }
}

@Composable
private fun TimePickerOkCancelButtons(
    modifier: Modifier = Modifier,
    errorState: ErrorState,
    onStartTimeSelected: () -> Unit,
    onEndTimeSelected: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .clickable(onClick = onDismiss)
                .padding(12.dp),
            text = stringResource(id = R.string.cancel),
            style = AppTypography.titleLarge.copy(
                color = AppColor.onSurface
            )
        )
        Text(
            modifier = Modifier
                .alpha(if (errorState == ErrorState.Ok) 1f else 0.2f)
                .padding(start = 12.dp)
                .clickable(
                    enabled = errorState == ErrorState.Ok
                ) {
                    onStartTimeSelected()
                    onEndTimeSelected()
                    onDismiss()
                }
                .padding(12.dp),
            text = stringResource(id = R.string.ok),
            style = AppTypography.titleLarge.copy(
                color = AppColor.primary,
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerStartEndTime(
    modifier: Modifier = Modifier,
    errorState: ErrorState,
    startTimePickerState: TimePickerState,
    endTimePickerState: TimePickerState,
    isAnyTime: Boolean,
    onIsAnyTimeCheckChanged: (Boolean) -> Unit,
    onTimeTypeSelected: (Boolean) -> Unit,
    isStart: Boolean,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier
                .border(
                    width = 0.5.dp,
                    color = AppColor.divider,
                    shape = RoundedCornerShape(size = 4.dp)
                )
        ) {
            TimePickerTimeText(
                title = stringResource(id = R.string.start),
                localTime = LocalTime.of(startTimePickerState.hour, startTimePickerState.minute),
                isAnyTime = isAnyTime,
                isSelected = isStart,
                isStart = true,
                isError = errorState == ErrorState.StartError,
                onClick = { onTimeTypeSelected(true) },
            )

            TimePickerTimeText(
                title = stringResource(id = R.string.end),
                localTime = LocalTime.of(endTimePickerState.hour, endTimePickerState.minute),
                isAnyTime = isAnyTime,
                isSelected = !isStart,
                isStart = false,
                isError = errorState == ErrorState.EndError,
                onClick = { onTimeTypeSelected(false) },
            )
        }
        Spacer(modifier = Modifier.weight(weight = 1f))

        AnyTimeCheckBox(
            isAnyTime = isAnyTime,
            onIsAnyTimeCheckChanged = onIsAnyTimeCheckChanged
        )
    }
}

@Composable
private fun AnyTimeCheckBox(
    modifier: Modifier = Modifier,
    isAnyTime: Boolean,
    onIsAnyTimeCheckChanged: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Checkbox(
            checked = isAnyTime,
            onCheckedChange = onIsAnyTimeCheckChanged
        )

        Text(
            text = stringResource(id = R.string.anytime),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )
    }
}

@Composable
private fun TimePickerTimeText(
    modifier: Modifier = Modifier,
    title: String,
    localTime: LocalTime,
    isAnyTime: Boolean,
    isSelected: Boolean,
    isStart: Boolean,
    isError: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {

        Icon(
            modifier = Modifier
                .background(
                    color = if (isSelected) AppColor.primary else Transparent,
                    shape = if (isStart) {
                        RoundedCornerShape(topStart = 4.dp)
                    } else {
                        RoundedCornerShape(bottomStart = 4.dp)
                    }
                )
                .padding(
                    start = 8.dp,
                )
                .width(18.dp)
                .fillMaxHeight(),
            imageVector = Icons.Outlined.Check,
            tint = if (isSelected) Color.White else Transparent,
            contentDescription = null
        )
        Text(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentHeight(Alignment.CenterVertically)
                .background(color = if (isSelected) AppColor.primary else Transparent)
                .widthIn(min = 70.dp)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            text = title,
            style = AppTypography.titleMedium.copy(
                color = if (isSelected) Color.White else AppColor.onSurface,
                fontWeight = FontWeight.Normal
            )
        )

        VerticalDivider()

        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp,
                )
                .widthIn(min = 80.dp),
            text = if (isAnyTime) stringResource(id = R.string.anytime) else localTime.formatHourMinute(),
            style = AppTypography.titleMedium.copy(
                color = if (isError) AppColor.error else AppColor.onSurface,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun rememberErrorState(
    startTimePickerState: TimePickerState,
    endTimePickerState: TimePickerState,
    isStart: Boolean,
    isAnyTime: Boolean,
) = remember(isStart, isAnyTime) {
    derivedStateOf {
        if (isAnyTime) return@derivedStateOf ErrorState.Ok

        val start = startTimePickerState.hour * 60 + startTimePickerState.minute
        val end = endTimePickerState.hour * 60 + endTimePickerState.minute

        if (end - MIN_DIFF_MINUTES < start) {
            if (isStart) {
                ErrorState.StartError
            } else {
                ErrorState.EndError
            }

        } else {
            ErrorState.Ok
        }
    }
}

val selectedBackground
    @Composable get() = AppColor.secondary
//        .compositeOverSurface(
//        alpha = if (isSystemInDarkTheme()) 0.2f else 0.1f
//    )

private enum class ErrorState {
    Ok, StartError, EndError
}

const val MIN_DIFF_MINUTES = 15


@Preview
@Composable
fun TimePickerPreview() {
    TimePickerDialog(
        localTimeStart = LocalTime.now(),
        onStartTimeSelected = {},
        localTimeEnd = LocalTime.now().plusHours(1),
        onEndTimeSelected = {},
        isAnyTime = false,
        onIsAnyTimeCheckChanged = {},
        isStart = true,
        onDismiss = {},
    )
}