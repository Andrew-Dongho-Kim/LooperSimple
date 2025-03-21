package com.pnd.android.loop.ui.home.input.selector

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopDay.Companion.fromIndex
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDay.Companion.toggle
import com.pnd.android.loop.data.common.defaultEndInDay
import com.pnd.android.loop.data.common.defaultStartInDay
import com.pnd.android.loop.ui.home.BlurState
import com.pnd.android.loop.ui.home.rememberBlurState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTheme
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
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
    isAnyTimeChecked: Boolean,
    onIsAnyTimeCheckChanged: (Boolean) -> Unit,
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
        Spacer(modifier = Modifier.height(20.dp))

        StartAndEndTimeSelector(
            blurState = blurState,
            isAnyTime = isAnyTimeChecked,
            onIsAnyTimeCheckChanged = onIsAnyTimeCheckChanged,
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
    isAnyTime: Boolean,
    onIsAnyTimeCheckChanged: (Boolean) -> Unit,
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
            isAnyTime = isAnyTime,
            onIsAnyTimeCheckChanged = onIsAnyTimeCheckChanged,
            isStart = true,
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
            isAnyTime = isAnyTime,
            onIsAnyTimeCheckChanged = onIsAnyTimeCheckChanged,
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
    isAnyTime: Boolean,
    onIsAnyTimeCheckChanged: (Boolean) -> Unit,
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
                .padding(top = 12.dp)
                .padding(horizontal = 24.dp),
            text = if (isAnyTime) {
                stringResource(id = R.string.anytime)
            } else {
                time.formatHourMinute(withAmPm = true)
            },
            style = AppTypography.titleLarge.copy(
                color = AppColor.onSurface.copy(
                    alpha = 0.6f
                )
            )
        )
    }

    if (isOpened) {
        TimePickerDialog(
            localTimeStart = (if (selectedStartTime < 0) defaultStartInDay else selectedStartTime).toLocalTime(),
            onStartTimeSelected = onStartTimeSelected,
            localTimeEnd = (if (selectedEndTime < 0) defaultEndInDay else selectedEndTime).toLocalTime(),
            onEndTimeSelected = onEndTimeSelected,
            isAnyTime = isAnyTime,
            onIsAnyTimeCheckChanged = onIsAnyTimeCheckChanged,
            isStart = isStart,
            onDismiss = {
                isOpened = false
                blurState.off()
            }
        )
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

@Preview(
    backgroundColor = 0xfffafafa,
    showBackground = true
)
@Composable
private fun AnyTimePreview() {
    AppTheme {
        StartAndEndTimeSelector(
            blurState = rememberBlurState(),
            isAnyTime = true,
            onIsAnyTimeCheckChanged = {},
            selectedStartTime = LocalTime.now().toMs(),
            onStartTimeSelected = {},
            selectedEndTime = LocalTime.now().plusHours(1).toMs(),
            onEndTimeSelected = {}
        )
    }
}


@Preview(
    backgroundColor = 0xfffafafa,
    showBackground = true
)
@Composable
private fun StartAndEndTimeSelectorPreview() {
    AppTheme {
        StartAndEndTimeSelector(
            blurState = rememberBlurState(),
            isAnyTime = false,
            onIsAnyTimeCheckChanged = {},
            selectedStartTime = LocalTime.now().toMs(),
            onStartTimeSelected = {},
            selectedEndTime = LocalTime.now().plusHours(1).toMs(),
            onEndTimeSelected = {}
        )
    }
}
