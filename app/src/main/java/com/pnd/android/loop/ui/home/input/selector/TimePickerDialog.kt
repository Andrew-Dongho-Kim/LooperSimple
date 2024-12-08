package com.pnd.android.loop.ui.home.input.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.BasicAlertDialog
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.divider
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceDark
import com.pnd.android.loop.ui.theme.primary
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
    )

    BasicAlertDialog(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        properties = DialogProperties(
            usePlatformDefaultWidth = false
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
    isStart: Boolean,
    onTimeTypeSelected: (Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.surface)
    ) {
        TimePickerStartEndTime(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    end = 24.dp,
                    top = 12.dp
                ),
            errorState = errorState,
            startTimePickerState = startTimePickerState,
            endTimePickerState = endTimePickerState,
            isStart = isStart,
            onTimeTypeSelected = onTimeTypeSelected,
        )

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
            )
        )

        Spacer(modifier = Modifier.weight(1f))

        TimePickerOkCancelButtons(
            modifier = Modifier
                .align(Alignment.End)
                .padding(bottom = 32.dp, end = 32.dp),
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
    startTimePickerState: TimePickerState,
    endTimePickerState: TimePickerState,
    errorState: ErrorState,
    isStart: Boolean,
    onTimeTypeSelected: (Boolean) -> Unit,
) {
    Column(
        modifier = modifier
            .padding(top = 24.dp)
            .border(
                width = 0.5.dp,
                color = AppColor.divider,
                shape = RoundedCornerShape(size = 4.dp)
            )
    ) {

        TimePickerTimeText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.start),
            localTime = LocalTime.of(startTimePickerState.hour, startTimePickerState.minute),
            isSelected = isStart,
            isError = errorState == ErrorState.StartError,
            onClick = { onTimeTypeSelected(true) },
        )

        TimePickerTimeText(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(id = R.string.end),
            localTime = LocalTime.of(endTimePickerState.hour, endTimePickerState.minute),
            isSelected = !isStart,
            isError = errorState == ErrorState.EndError,
            onClick = { onTimeTypeSelected(false) },
        )

        if (errorState != ErrorState.Ok) {
            Text(
                modifier = Modifier.padding(
                    top = 12.dp,
                    start = 8.dp,
                    bottom = 12.dp,
                ),
                text = stringResource(
                    id = if (errorState == ErrorState.StartError) {
                        R.string.warning_start_time_should_be_before_end_time
                    } else {
                        R.string.warning_end_time_should_be_after_start_time
                    }
                ),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.error
                )
            )
        }
    }
}

@Composable
private fun TimePickerTimeText(
    modifier: Modifier = Modifier,
    title: String,
    localTime: LocalTime,
    isSelected: Boolean,
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
                .padding(
                    start = 8.dp,
                )
                .size(18.dp),
            imageVector = Icons.Outlined.Check,
            tint = if (isSelected) AppColor.primary else Color.Transparent,
            contentDescription = null
        )
        Text(
            modifier = Modifier
                .fillMaxHeight()
                .wrapContentHeight(Alignment.CenterVertically)
                .widthIn(min = 80.dp)
                .padding(
                    horizontal = 12.dp,
                    vertical = 8.dp
                ),
            text = title,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
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
                ),
            text = localTime.formatHourMinute(),
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
) = remember(isStart) {
    derivedStateOf {
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
    @Composable get() = AppColor.primary.compositeOverSurface(
        alpha = if (isSystemInDarkTheme()) 0.2f else 0.1f
    )

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
        isStart = true,
        onDismiss = {},
    )
}