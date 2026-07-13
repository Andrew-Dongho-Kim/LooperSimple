package com.pnd.android.loop.ui.home.input.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
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
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTheme
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.rememberDayColor
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalTime

/** Minimum gap the loop must span; the end time has to sit at least this far after the start. */
private const val MIN_DIFF_MINUTES = 15

@Composable
fun StartEndTimeSelector(
    modifier: Modifier = Modifier,
    isAnyTimeChecked: Boolean,
    onIsAnyTimeCheckChanged: (Boolean) -> Unit,
    selectedStartTime: Long,
    onStartTimeSelected: (Long) -> Unit,
    selectedEndTime: Long,
    onEndTimeSelected: (Long) -> Unit,
    selectedDays: Int,
    onSelectedDayChanged: (Int) -> Unit
) {
    val startTime = (if (selectedStartTime < 0) defaultStartInDay else selectedStartTime).toLocalTime()
    val endTime = (if (selectedEndTime < 0) defaultEndInDay else selectedEndTime).toLocalTime()

    Column(
        modifier = modifier.padding(horizontal = Dimens.contentPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // A compact "anytime" checkbox sits above the cards, taking only as much room as it needs.
        AnyTimeCheckbox(
            modifier = Modifier.align(Alignment.End),
            checked = isAnyTimeChecked,
            onCheckedChange = onIsAnyTimeCheckChanged
        )

        // Start / End steppers. They dim and lock when the loop runs all day.
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .alpha(if (isAnyTimeChecked) 0.4f else 1f),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            TimeStepperCard(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.start),
                localTime = startTime,
                enabled = !isAnyTimeChecked,
                onTimeChanged = { onStartTimeSelected(it.toMs()) }
            )
            TimeStepperCard(
                modifier = Modifier.weight(1f),
                label = stringResource(id = R.string.end),
                localTime = endTime,
                enabled = !isAnyTimeChecked,
                onTimeChanged = { onEndTimeSelected(it.toMs()) }
            )
        }

        DaySelector(
            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
            selectedDays = selectedDays,
            onSelectedDayChanged = onSelectedDayChanged
        )
    }
}

/** Small checkbox + label toggling the all-day ("anytime") mode; filled when the loop runs all day. */
@Composable
private fun AnyTimeCheckbox(
    modifier: Modifier = Modifier,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    val accent = if (checked) AppColor.primary else AppColor.onSurface.copy(alpha = 0.6f)
    Row(
        modifier = modifier
            .clip(RoundShapes.medium)
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 4.dp, horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(RoundShapes.small)
                .background(if (checked) AppColor.primary else Color.Transparent)
                .then(
                    if (checked) Modifier
                    else Modifier.border(1.5.dp, AppColor.outlineVariant, RoundShapes.small)
                ),
            contentAlignment = Alignment.Center
        ) {
            if (checked) {
                Icon(
                    modifier = Modifier.size(12.dp),
                    imageVector = Icons.Filled.Check,
                    contentDescription = null,
                    tint = AppColor.onPrimary
                )
            }
        }
        Text(
            text = stringResource(id = R.string.anytime),
            style = AppTypography.labelLarge.copy(color = accent)
        )
    }
}

/**
 * A single edge of the span. Hour and minute are nudged with up/down steppers, and a tap on the
 * AM/PM label flips the period. Values wrap (12→1, 59→00) and never flip the period on their own,
 * matching how the old wheels behaved.
 */
@Composable
private fun TimeStepperCard(
    modifier: Modifier = Modifier,
    label: String,
    localTime: LocalTime,
    enabled: Boolean,
    onTimeChanged: (LocalTime) -> Unit,
) {
    val hour12 = (localTime.hour % 12).let { if (it == 0) 12 else it }
    val minute = localTime.minute
    val isPm = localTime.hour >= 12

    Column(
        modifier = modifier
            .clip(RoundShapes.large)
            .border(1.dp, AppColor.outlineVariant, RoundShapes.large)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = label,
            style = AppTypography.labelMedium.copy(color = AppColor.onSurface.copy(alpha = 0.6f))
        )
        Spacer(modifier = Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Stepper(
                value = hour12.toString(),
                enabled = enabled,
                onIncrease = { onTimeChanged(buildTime(hour12 % 12 + 1, minute, isPm)) },
                onDecrease = { onTimeChanged(buildTime(if (hour12 == 1) 12 else hour12 - 1, minute, isPm)) }
            )
            Text(
                text = ":",
                style = AppTypography.headlineMedium.copy(color = AppColor.onSurface.copy(alpha = 0.4f))
            )
            Stepper(
                value = "%02d".format(minute),
                enabled = enabled,
                onIncrease = { onTimeChanged(buildTime(hour12, (minute + 1) % 60, isPm)) },
                onDecrease = { onTimeChanged(buildTime(hour12, (minute + 59) % 60, isPm)) }
            )
            AmPmToggle(
                isPm = isPm,
                enabled = enabled,
                onClick = { onTimeChanged(buildTime(hour12, minute, !isPm)) }
            )
        }
    }
}

/** An up-arrow / value / down-arrow column used for one field (hour or minute). */
@Composable
private fun Stepper(
    value: String,
    enabled: Boolean,
    onIncrease: () -> Unit,
    onDecrease: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        StepperArrow(imageVector = Icons.Filled.KeyboardArrowUp, enabled = enabled, onClick = onIncrease)
        Text(
            modifier = Modifier.width(44.dp),
            text = value,
            textAlign = TextAlign.Center,
            style = AppTypography.headlineMedium.copy(color = AppColor.onSurface)
        )
        StepperArrow(imageVector = Icons.Filled.KeyboardArrowDown, enabled = enabled, onClick = onDecrease)
    }
}

@Composable
private fun StepperArrow(
    imageVector: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        modifier = Modifier
            .size(24.dp)
            .clip(RoundShapes.small)
            .repeatingClickable(enabled = enabled, onClick = onClick),
        imageVector = imageVector,
        contentDescription = null,
        tint = AppColor.onSurface.copy(alpha = 0.4f)
    )
}

/**
 * Like [clickable] but keeps firing [onClick] while the finger stays down, so holding a stepper
 * arrow keeps nudging the value. A single tap fires once (immediate); a hold repeats after a short
 * initial delay and then accelerates down to [minDelayMillis] between steps.
 */
private fun Modifier.repeatingClickable(
    enabled: Boolean,
    onClick: () -> Unit,
    initialDelayMillis: Long = 350,
    minDelayMillis: Long = 50,
    delayDecayFactor: Float = 0.85f,
): Modifier = composed {
    val currentOnClick by rememberUpdatedState(onClick)
    val scope = rememberCoroutineScope()
    pointerInput(enabled) {
        if (!enabled) return@pointerInput
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            val repeatJob = scope.launch {
                currentOnClick()
                delay(initialDelayMillis)
                var currentDelay = initialDelayMillis
                while (true) {
                    currentOnClick()
                    currentDelay = (currentDelay * delayDecayFactor)
                        .toLong()
                        .coerceAtLeast(minDelayMillis)
                    delay(currentDelay)
                }
            }
            waitForUpOrCancellation()
            repeatJob.cancel()
        }
    }
}

/** A tap-to-flip AM·PM label sharing the card row. */
@Composable
private fun AmPmToggle(
    isPm: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        modifier = Modifier
            .padding(start = 4.dp)
            .clip(RoundShapes.medium)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        text = stringResource(id = if (isPm) R.string.pm else R.string.am),
        style = AppTypography.labelLarge.copy(color = AppColor.primary)
    )
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
                .fillMaxWidth()
                .clip(RoundShapes.large)
                .background(color = AppColor.outlineVariant.copy(alpha = 0.4f))
                .padding(4.dp)
        ) {
            ABB_DAYS.forEachIndexed { index, dayResId ->
                DaySegment(
                    modifier = Modifier.weight(1f),
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
private fun DaySegment(
    modifier: Modifier = Modifier,
    day: Int,
    dayResId: Int,
    selectedDays: Int,
    onSelectedDayChanged: (Int) -> Unit = {}
) {
    val selected = selectedDays.isOn(day)

    Text(
        modifier = modifier
            .padding(horizontal = 2.dp)
            .clip(RoundShapes.medium)
            .clickable { onSelectedDayChanged(selectedDays.toggle(day)) }
            .background(
                color = if (selected) AppColor.primary else Color.Transparent,
                shape = RoundShapes.medium
            )
            .padding(vertical = 8.dp),
        text = stringResource(dayResId),
        textAlign = TextAlign.Center,
        color = if (selected) {
            AppColor.onPrimary
        } else {
            rememberDayColor(day = day)
        },
        style = AppTypography.titleMedium.copy(
            textAlign = TextAlign.Center
        )
    )
}

/** Rebuilds a [LocalTime] from the 12-hour value, minute and AM/PM state. */
private fun buildTime(hour12: Int, minute: Int, isPm: Boolean): LocalTime {
    val hour24 = (hour12 % 12) + if (isPm) 12 else 0
    return LocalTime.of(hour24, minute)
}

/**
 * True when a span from [startInDay] to [endInDay] (both millis-of-day) is shorter than the
 * minimum a loop may cover. Callers use this to warn when the end sits too close to the start.
 */
fun isLoopDurationTooShort(startInDay: Long, endInDay: Long): Boolean {
    return endInDay - startInDay < MIN_DIFF_MINUTES * 60_000L
}

@Preview(
    backgroundColor = 0xfffafafa,
    showBackground = true
)
@Composable
private fun StartEndTimeSelectorPreview() {
    AppTheme {
        StartEndTimeSelector(
            isAnyTimeChecked = false,
            onIsAnyTimeCheckChanged = {},
            selectedStartTime = LocalTime.of(7, 0).toMs(),
            onStartTimeSelected = {},
            selectedEndTime = LocalTime.of(8, 0).toMs(),
            onEndTimeSelected = {},
            selectedDays = 0b0101010,
            onSelectedDayChanged = {}
        )
    }
}

@Preview(
    backgroundColor = 0xfffafafa,
    showBackground = true
)
@Composable
private fun AnyTimePreview() {
    AppTheme {
        StartEndTimeSelector(
            isAnyTimeChecked = true,
            onIsAnyTimeCheckChanged = {},
            selectedStartTime = LocalTime.of(7, 0).toMs(),
            onStartTimeSelected = {},
            selectedEndTime = LocalTime.of(8, 0).toMs(),
            onEndTimeSelected = {},
            selectedDays = 0b0101010,
            onSelectedDayChanged = {}
        )
    }
}
