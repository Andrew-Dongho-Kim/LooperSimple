package com.pnd.android.loop.ui.input

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.ContentAlpha
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusModifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.Loop.Day.Companion.SATURDAY
import com.pnd.android.loop.data.Loop.Day.Companion.SUNDAY
import com.pnd.android.loop.data.Loop.Day.Companion.fromIndex
import com.pnd.android.loop.data.Loop.Day.Companion.isOn
import com.pnd.android.loop.data.Loop.Day.Companion.toggle
import com.pnd.android.loop.ui.common.pager.Pager
import com.pnd.android.loop.ui.common.pager.PagerState
import com.pnd.android.loop.ui.theme.Blue500
import com.pnd.android.loop.ui.theme.PADDING_HZ_ITEM
import com.pnd.android.loop.ui.theme.PADDING_VT_ITEM
import com.pnd.android.loop.ui.theme.Red500
import com.pnd.android.loop.util.*
import kotlinx.coroutines.launch

private val logger = log("TimeSelector")


@Composable
fun StartEndTimeSelector(
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester,
    selectedStartTime: Long,
    onStartTimeSelected: (Long) -> Unit,
    selectedEndTime: Long,
    onEndTimeSelected: (Long) -> Unit,
    selectedDays: Int,
    onSelectedDayChanged: (Int) -> Unit
) {

    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(dimensionResource(id = R.dimen.user_input_selector_content_height))
            .focusRequester(focusRequester)
            .focusTarget()
//            .semantics { contentDescription = description }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = PADDING_HZ_ITEM)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(id = R.string.desc_start_time),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.subtitle2
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(id = R.string.desc_end_time),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.subtitle2
                )
            }
        }

        Row(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = PADDING_VT_ITEM)
        ) {
            TimeSelector(
                modifier = Modifier.weight(1f),
                msSelectedTime = selectedStartTime,
                onTimeSelected = onStartTimeSelected
            )

            TimeSelector(
                modifier = Modifier.weight(1f),
                msSelectedTime = selectedEndTime,
                onTimeSelected = onEndTimeSelected
            )
        }

        DaySelector(
            modifier = Modifier.padding(bottom = 8.dp),
            selectedDays = selectedDays,
            onSelectedDayChanged = onSelectedDayChanged
        )
    }
}

@Composable
fun DaySelector(
    modifier: Modifier = Modifier,
    selectedDays: Int,
    onSelectedDayChanged: (Int) -> Unit = {}
) {
    Row(
        modifier = modifier.padding(horizontal = PADDING_HZ_ITEM)
    ) {
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val day = fromIndex(index)

            val selected = selectedDays.isOn(day)
            val selectedColor = MaterialTheme.colors.primary.copy(alpha = ContentAlpha.medium)

            Box(
                modifier = Modifier
                    .padding(start = 5.dp, end = 5.dp)
                    .size(30.dp)
                    .border(
                        BorderStroke(
                            width = 0.5.dp,
                            color = if (selected) selectedColor else Color.Transparent
                        ), CircleShape
                    )
                    .clickable(
                        onClick = { onSelectedDayChanged(selectedDays.toggle(day)) },
                        enabled = true,
                        role = Role.Button,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false)
                    )
            ) {
                Text(
                    modifier = Modifier.align(Alignment.Center),
                    text = stringResource(dayResId),
                    textAlign = TextAlign.Center,
                    color = if (selected) {
                        selectedColor
                    } else {
                        when (day) {
                            SUNDAY -> Red500
                            SATURDAY -> Blue500
                            else -> MaterialTheme.colors.onSurface.copy(alpha = ContentAlpha.medium)
                        }
                    },
                    style = MaterialTheme.typography.body2
                )
            }
        }
    }
}


@Composable
fun TimeSelector(
    modifier: Modifier = Modifier,
    msSelectedTime: Long,
    onTimeSelected: (Long) -> Unit
) {
    var isAm by remember { mutableStateOf(isAm(msSelectedTime)) }
    var hour by remember { mutableStateOf(hourIn12(msSelectedTime)) }
    var min by remember { mutableStateOf(min(msSelectedTime)) }

    logger.d { "Saved time in TimeSelector: ${if (isAm) "AM" else "PM"} $hour:$min" }

    fun selectTime() = onTimeSelected(
        ((if (hour == 12) 0 else hour) + if (!isAm) 12 else 0) * MS_1HOUR + min * MS_1MIN
    )


    Row(
        modifier = modifier
            .padding(horizontal = 4.dp)
            .background(
                color = MaterialTheme.colors.onSurface.copy(alpha = 0.04f),
                shape = MaterialTheme.shapes.medium
            )
            .padding(horizontal = 5.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        TimeVerticalPager(
            modifier = Modifier.width(50.dp),
            items = AmPm,
            currPage = if (isAm) 0 else 1,
            onPageSelected = { page ->
                isAm = page == 0
                selectTime()
            },
            itemToStr = { item -> stringResource(id = item) }
        )

        TimeVerticalPager(
            modifier = Modifier.width(60.dp),
            items = (1..12).toList(),
            currPage = hour - 1,
            onPageSelected = { page ->
                hour = page + 1
                selectTime()
            },
            itemToStr = { item -> String.format("%02d", item) }
        )

        TimeVerticalPager(
            modifier = Modifier.width(10.dp),
            items = listOf(":"),
            itemToStr = { item -> item }
        )

        TimeVerticalPager(
            modifier = Modifier.width(60.dp),
            items = (0..59).toList(),
            currPage = min,
            onPageSelected = { page ->
                min = page
                selectTime()
            },
            itemToStr = { item -> String.format("%02d", item) }
        )
    }


}

@Composable
fun <T : Any> TimeVerticalPager(
    modifier: Modifier = Modifier,
    items: List<T>,
    currPage: Int = 0,
    onPageSelected: (Int) -> Unit = {},
    itemToStr: @Composable (T) -> String
) {
    var isFirst by remember { mutableStateOf(true) }
    val scope = rememberCoroutineScope()
    val pagerState = remember { PagerState() }.apply {
        this.maxPage = (items.size - 1).coerceAtLeast(0)
        this.onPageSelected = onPageSelected

        if (isFirst) {
            isFirst = false
            scope.launch { selectPage { currentPage = currPage } }
        }
    }

    Pager(
        modifier = modifier,
        state = pagerState,
        orientation = Orientation.Vertical,
        offscreenLimit = items.size,
        velocityFactor = 0.25f
    ) {
        val contentAlpha = if (page != currentPage) 0.2f else 0.9f
        CompositionLocalProvider(LocalContentAlpha provides contentAlpha) {
            PagerTextItem(
                modifier = Modifier
                    .clickable(
                        onClick = {
                            scope.launch {
                                pagerState.selectPage { currentPage = page }
                            }
                        },
                        interactionSource = remember { MutableInteractionSource() },
                        indication = rememberRipple(bounded = false, radius = 24.dp)
                    )
                    .padding(top = 6.dp, bottom = 6.dp),
                text = itemToStr(items[page])
            )
        }
    }
}


@Composable
fun PagerTextItem(
    modifier: Modifier = Modifier,
    text: String
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = text,
        textAlign = TextAlign.Center,
        style = MaterialTheme.typography.h6.copy(
            shadow = Shadow(blurRadius = 1f),
            fontWeight = FontWeight.Medium
        )
    )
}

