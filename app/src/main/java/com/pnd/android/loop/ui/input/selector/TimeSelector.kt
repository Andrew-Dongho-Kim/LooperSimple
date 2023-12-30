package com.pnd.android.loop.ui.input.selector

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowRightAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopVo.Day.Companion.fromIndex
import com.pnd.android.loop.data.LoopVo.Day.Companion.isOn
import com.pnd.android.loop.data.LoopVo.Day.Companion.toggle
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.rememberDayColor
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

private val logger = log("TimeSelector")


@Composable
fun StartEndTimeSelector(
    modifier: Modifier = Modifier,
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
            title = stringResource(id = R.string.start),
            selectedTime = selectedStartTime,
            onTimeSelected = onStartTimeSelected
        )
        Image(
            imageVector = Icons.Filled.ArrowRightAlt,
            colorFilter = ColorFilter.tint(
                color = MaterialTheme.colors.primary
            ),
            contentDescription = ""
        )
        TimeDisplay(
            modifier = Modifier.weight(1f),
            title = stringResource(id = R.string.end),
            selectedTime = selectedEndTime,
            onTimeSelected = onEndTimeSelected
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimeDisplay(
    modifier: Modifier = Modifier,
    title: String,
    selectedTime: Long,
    onTimeSelected: (Long) -> Unit,
) {
    var isOpened by remember { mutableStateOf(false) }

    val localTime = selectedTime.toLocalTime()
    val state = rememberTimePickerState(
        initialHour = localTime.hour,
        initialMinute = localTime.minute,
        is24Hour = false
    )

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.onSurface
            )
        )

        Text(
            modifier = Modifier
                .padding(top = 8.dp)
                .wrapContentWidth(align = Alignment.CenterHorizontally)
                .clip(RoundShapes.large)
                .clickable { isOpened = true }
                .padding(horizontal = 24.dp, vertical = 12.dp),
            text = selectedTime.formatHourMinute(withAmPm = true),
            style = MaterialTheme.typography.h6.copy(
                color = MaterialTheme.colors.onSurface.copy(
                    alpha = ContentAlpha.medium
                )
            )
        )
    }

    if (isOpened) {
        TimePickerDialog(
            state = state,
            onDismiss = {
                isOpened = false
                onTimeSelected(LocalTime.of(state.hour, state.minute).toMs())
            }
        )
    }

}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    state: TimePickerState,
    onDismiss: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismiss) {
        TimePicker(state = state)
    }
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
            style = MaterialTheme.typography.subtitle1.copy(
                color = MaterialTheme.colors.onSurface
            )
        )

        Row(modifier = Modifier.padding(top = 16.dp)) {
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
    val selectedColor = MaterialTheme.colors.primary
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
        style = MaterialTheme.typography.subtitle1.copy(
            textAlign = TextAlign.Center
        )
    )
}
