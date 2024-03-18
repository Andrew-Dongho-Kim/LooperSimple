package com.pnd.android.loop.ui.home.loop.input.selector

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.data.Day.Companion.fromIndex
import com.pnd.android.loop.data.Day.Companion.isOn
import com.pnd.android.loop.data.Day.Companion.toggle
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceDark
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.rememberDayColor
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

@Composable
fun StartEndTimeSelector(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    selectedStartTime: Long,
    onStartTimeSelected: (Long) -> Unit,
    selectedEndTime: Long,
    onEndTimeSelected: (Long) -> Unit,
    selectedDays: Int,
    onSelectedDayChanged: (Int) -> Unit
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        StartAndEndTimeSelector(
            blurState = blurState,
            selectedStartTime = selectedStartTime,
            onStartTimeSelected = onStartTimeSelected,
            selectedEndTime = selectedEndTime,
            onEndTimeSelected = onEndTimeSelected
        )
        DaySelector(
            modifier = Modifier.padding(bottom = 8.dp),
            selectedDays = selectedDays,
            onSelectedDayChanged = onSelectedDayChanged
        )
    }
}

@Composable
private fun StartAndEndTimeSelector(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    selectedStartTime: Long,
    onStartTimeSelected: (Long) -> Unit,
    selectedEndTime: Long,
    onEndTimeSelected: (Long) -> Unit,
) {

    Row(
        modifier = modifier.padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeDisplay(
            modifier = Modifier.weight(1f),
            blurState = blurState,
            title = stringResource(id = R.string.start),
            selectedStartTime = selectedStartTime,
            onStartTimeSelected = onStartTimeSelected,
            selectedEndTime = selectedEndTime,
            onEndTimeSelected = onEndTimeSelected,
            isStart = true
        )
        Image(
            imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
            colorFilter = ColorFilter.tint(
                color = AppColor.primary
            ),
            contentDescription = ""
        )
        TimeDisplay(
            modifier = Modifier.weight(1f),
            blurState = blurState,
            title = stringResource(id = R.string.end),
            selectedStartTime = selectedStartTime,
            onStartTimeSelected = onStartTimeSelected,
            selectedEndTime = selectedEndTime,
            onEndTimeSelected = onEndTimeSelected,
            isStart = false,
        )
    }
}

@Composable
private fun TimeDisplay(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    title: String,
    selectedStartTime: Long,
    onStartTimeSelected: (Long) -> Unit,
    selectedEndTime: Long,
    onEndTimeSelected: (Long) -> Unit,
    isStart: Boolean,
) {
    var isOpened by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface
            )
        )
        val time = if (isStart) selectedStartTime else selectedEndTime
        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .wrapContentWidth(align = Alignment.CenterHorizontally)
                .clip(RoundShapes.large)
                .clickable {
                    isOpened = true
                    blurState.on()
                }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            text = time.formatHourMinute(withAmPm = true),
            style = AppTypography.titleLarge.copy(
                color = AppColor.onSurface.copy(
                    alpha = 0.6f
                )
            )
        )
    }

    if (isOpened) {
        TimePickerDialog(
            localTimeStart = selectedStartTime.toLocalTime(),
            onStartTimeSelected = onStartTimeSelected,
            localTimeEnd = selectedEndTime.toLocalTime(),
            onEndTimeSelected = onEndTimeSelected,
            isStart = isStart,
            onDismiss = {
                isOpened = false
                blurState.off()

            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
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

    Log.d("DDEBUG2", "test")
    val timePickerState = if (isStartTime) startTimePickerState else endTimePickerState
    UpdateStartEndTime(
        timePickerState = timePickerState,
        selectedTime = if (isStartTime) localTimeStart else localTimeEnd,
        onTimeSelected = if (isStartTime) onStartTimeSelected else onEndTimeSelected,
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
        TimePickerDialogContent(
            timePickerState = timePickerState,
            localTimeStart = localTimeStart,
            localTimeEnd = localTimeEnd,
            isStart = isStartTime,
            onTimeTypeSelected = { isStart -> isStartTime = isStart }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialogContent(
    modifier: Modifier = Modifier,
    timePickerState: TimePickerState,
    localTimeStart: LocalTime,
    localTimeEnd: LocalTime,
    isStart: Boolean,
    onTimeTypeSelected: (Boolean) -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
    ) {
        TimePickerStartEndTime(
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 24.dp, top = 24.dp),
            localTimeStart = localTimeStart,
            localTimeEnd = localTimeEnd,
            isStart = isStart,
            onTimeTypeSelected = onTimeTypeSelected,
        )

        TimePicker(
            modifier = Modifier.align(Alignment.Center),
            state = timePickerState,
            colors = TimePickerDefaults.colors(
                clockDialColor = AppColor.surface,
                clockDialSelectedContentColor = AppColor.onSurfaceDark,
                clockDialUnselectedContentColor = AppColor.onSurface,
                selectorColor = AppColor.primary,
                containerColor = AppColor.background,
                periodSelectorSelectedContainerColor = AppColor.primary.compositeOverSurface(
                    alpha = if (isSystemInDarkTheme()) 0.2f else 0.1f
                ),
                periodSelectorUnselectedContainerColor = AppColor.surface,
                periodSelectorSelectedContentColor = AppColor.onSurface,
                periodSelectorUnselectedContentColor = AppColor.onSurface,
                timeSelectorSelectedContainerColor = AppColor.primary.compositeOverSurface(
                    alpha = if (isSystemInDarkTheme()) 0.2f else 0.1f
                ),
                timeSelectorUnselectedContainerColor = AppColor.surface,
                timeSelectorSelectedContentColor = AppColor.onSurface,
                timeSelectorUnselectedContentColor = AppColor.onSurface,
            )
        )

        val snackBarHostState = remember { SnackbarHostState() }
        SnackbarHost(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 24.dp),
            hostState = snackBarHostState
        )
    }
}

@Composable
private fun TimePickerStartEndTime(
    modifier: Modifier = Modifier,
    localTimeStart: LocalTime,
    localTimeEnd: LocalTime,
    isStart: Boolean,
    onTimeTypeSelected: (Boolean) -> Unit,
) {
    Column(modifier = modifier) {
        val selectedColor = AppColor.primary.compositeOverSurface(
            alpha = if (isSystemInDarkTheme()) 0.2f else 0.1f
        )
        val normalColor = AppColor.surface

        TimePickerTimeText(
            title = stringResource(id = R.string.start),
            localTime = localTimeStart,
            backgroundColor = if (isStart) selectedColor else normalColor,
            onClick = { onTimeTypeSelected(true) }
        )

        TimePickerTimeText(
            title = stringResource(id = R.string.end),
            localTime = localTimeEnd,
            backgroundColor = if (isStart) normalColor else selectedColor,
            onClick = { onTimeTypeSelected(false) }
        )

    }
}

@Composable
private fun TimePickerTimeText(
    modifier: Modifier = Modifier,
    title: String,
    localTime: LocalTime,
    backgroundColor: Color,
    onClick: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier
                .background(backgroundColor)
                .border(
                    width = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.2f)
                )
                .clickable(onClick = onClick)
                .padding(all = 12.dp),
            text = title,
            style = AppTypography.titleLarge.copy(color = AppColor.onSurface)
        )
        Text(
            modifier = Modifier
                .padding(start = 8.dp)
                .background(AppColor.surface)
                .border(
                    width = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.2f)
                )
                .padding(
                    vertical = 12.dp,
                    horizontal = 18.dp
                ),
            text = localTime.formatHourMinute(),
            style = AppTypography.titleLarge.copy(color = AppColor.onSurface)
        )
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UpdateStartEndTime(
    timePickerState: TimePickerState,
    selectedTime: LocalTime,
    onTimeSelected: (Long) -> Unit,
) {
    if (selectedTime.hour == timePickerState.hour &&
        selectedTime.minute == timePickerState.minute
    ) {
        return
    }

    onTimeSelected(LocalTime.of(timePickerState.hour, timePickerState.minute).toMs())
}

@Composable
fun DaySelector(
    modifier: Modifier = Modifier,
    selectedDays: Int,
    onSelectedDayChanged: (Int) -> Unit = {}
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(id = R.string.select_day),
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface
            )
        )

        Row(
            modifier = Modifier
                .padding(top = 16.dp)
                .horizontalScroll(state = rememberScrollState())
        ) {
            ABB_DAYS.forEachIndexed { index, dayResId ->
                DateItemText(
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(34.dp),
                    day = fromIndex(index),
                    dayResId = dayResId,
                    selectedDays = selectedDays,
                    onSelectedDayChanged = onSelectedDayChanged
                )
            }
        }
    }

}

@Composable
private fun DateItemText(
    modifier: Modifier = Modifier,
    day: Int,
    dayResId: Int,
    selectedDays: Int,
    onSelectedDayChanged: (Int) -> Unit = {}
) {
    val selectedColor = AppColor.primary
    val selected = selectedDays.isOn(day)

    Text(
        modifier = modifier
            .clip(CircleShape)
            .clickable { onSelectedDayChanged(selectedDays.toggle(day)) }
            .border(
                width = 0.5.dp,
                color = if (selected) selectedColor else Color.Transparent,
                shape = CircleShape
            )
            .wrapContentHeight(Alignment.CenterVertically),
        text = stringResource(dayResId),
        textAlign = TextAlign.Center,
        color = if (selected) {
            selectedColor
        } else {
            rememberDayColor(day = day)
        },
        style = AppTypography.titleMedium.copy(
            textAlign = TextAlign.Center
        )
    )
}
