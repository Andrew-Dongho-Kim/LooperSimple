package com.pnd.android.loop.ui.input.selector

import androidx.compose.material.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.alarm.AlarmCategory
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.input.*
import com.pnd.android.loop.util.h2m2


private val logger = log("Selectors")

enum class InputSelector {
    NONE,
    ICONS,
    ALARM_INTERVAL,
    START_END_TIME,
    ALARMS,
}

@Composable
fun Selectors(
    currentSelector: InputSelector,
    loop: LoopVo
) {
    var icon by remember { mutableStateOf(loop.icon) }
    var loopStart by remember { mutableStateOf(loop.loopStart) }
    var loopEnd by remember { mutableStateOf(loop.loopEnd) }
    var enabledDays by remember { mutableStateOf(loop.loopEnableDays) }
    var interval by remember { mutableStateOf(loop.interval) }
    var alarms by remember { mutableStateOf(loop.alarms) }

    Selectors(
        selectedInterval = interval,
        onIntervalSelected = {
            loop.interval = it
            interval = it
        },
        selectedIcon = icon,
        onIconSelected = { _, index ->
            loop.icon = index
            icon = index
        },
        selectedStartTime = loopStart,
        onStartTimeSelected = {
            loop.loopStart = it
            loopStart = it
            logger.d { "Loop start time : ${h2m2(it)}" }
        },
        selectedEndTime = loopEnd,
        onEndTimeSelected = {
            loop.loopEnd = it
            loopEnd = it
            logger.d { "Loop end time : ${h2m2(it)}" }
        },
        currentSelector = currentSelector,
        selectedDays = enabledDays,
        onSelectedDayChanged = {
            loop.loopEnableDays = it
            enabledDays = it
        },
        selectedAlarms = alarms,
        onAlarmSelected = {
            loop.alarms = it
            alarms = it
            logger.d { "selected alarms - sound:${AlarmCategory.Sounds.nameOf(alarms)}" }
        }
    )
}


@Composable
fun Selectors(
    currentSelector: InputSelector,
    // Related intervals
    selectedInterval: Long,
    onIntervalSelected: (Long) -> Unit,

    // Related icons
    selectedIcon: Int,
    onIconSelected: (ImageVector, Int) -> Unit,

    //Related start & end times, dates
    selectedStartTime: Long,
    onStartTimeSelected: (Long) -> Unit,
    selectedEndTime: Long,
    onEndTimeSelected: (Long) -> Unit,
    selectedDays: Int,
    onSelectedDayChanged: (Int) -> Unit,

    //Related alarms
    selectedAlarms: Int,
    onAlarmSelected: (Int) -> Unit,
) {
    if (currentSelector == InputSelector.NONE) return


    // Request focus to force the TextField to lose it
    // If the selector is shown, always request focus to trigger a TextField.onFocusChange.
    val focusRequester = FocusRequester()

    SideEffect {
        if (currentSelector != InputSelector.NONE) {
            focusRequester.requestFocus()
        }
    }
    val selectorExpandedColor = getSelectorExpandedColor()

    Surface(color = selectorExpandedColor, elevation = 3.dp) {
        when (currentSelector) {
            InputSelector.ICONS -> IconSelector(
                selectedIcon = selectedIcon,
                onIconSelected = onIconSelected,
                focusRequester = focusRequester,
            )
            InputSelector.ALARM_INTERVAL -> IntervalSelector(
                focusRequester = focusRequester,
                selectedInterval = selectedInterval,
                onIntervalSelected = onIntervalSelected,
            )
            InputSelector.START_END_TIME -> StartEndTimeSelector(
                focusRequester = focusRequester,
                selectedStartTime = selectedStartTime,
                onStartTimeSelected = onStartTimeSelected,
                selectedEndTime = selectedEndTime,
                onEndTimeSelected = onEndTimeSelected,
                selectedDays = selectedDays,
                onSelectedDayChanged = onSelectedDayChanged
            )
            InputSelector.ALARMS -> AlarmSelector(
                focusRequester = focusRequester,
                selectedAlarm = selectedAlarms,
                onAlarmSelected = onAlarmSelected
            )
            else -> {
                // do nothing
            }
        }
    }
}
