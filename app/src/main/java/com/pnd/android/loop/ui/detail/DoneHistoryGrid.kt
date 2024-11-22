package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.toLocalDate
import java.time.LocalDate


@Composable
fun DoneHistoryGrid(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    val doneHistory = detailViewModel.donePager.collectAsLazyPagingItems()

    val lazyGridState = rememberLazyGridState()
    LaunchedEffect(key1 = Unit) { lazyGridState.scrollToItem(index = Int.MAX_VALUE) }

    Column(modifier = modifier) {
        Row(modifier = Modifier.weight(1f)) {
            LazyHorizontalGrid(
                modifier = Modifier.weight(1f),
                state = lazyGridState,
                rows = GridCells.Fixed(7)
            ) {
                items(
                    lazyPagingItems = doneHistory,
                    key = { item -> item.date }
                ) { item ->
                    DoneHistoryItem(
                        modifier = Modifier.size(32.dp),
                        created = loop.created,
                        doneVo = item,
                        color = loop.color.compositeOverOnSurface(),
                        activeDays = loop.activeDays
                    )
                }
            }
            DoneHistoryDayHeader(
                modifier = Modifier.padding(start = 12.dp)
            )
        }
        DoneHistoryGridIconsDescription(
            modifier = Modifier.padding(top = 8.dp),
            doneColor = loop.color
        )
    }
}


@Composable
private fun DoneHistoryGridIconsDescription(
    modifier: Modifier = Modifier,
    doneColor: Int
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        ItemDescriptionItem(
            doneState = DoneState.DONE,
            text = stringResource(id = R.string.done),
            color = doneColor.compositeOverOnSurface(),
        )
        ItemDescriptionItem(
            modifier = Modifier.padding(start = 12.dp),
            doneState = DoneState.SKIP,
            text = stringResource(id = R.string.skip),
            color = AppColor.onSurface,
        )
    }
}

@Composable
private fun ItemDescriptionItem(
    modifier: Modifier = Modifier,
    @DoneState doneState: Int,
    text: String,
    color: Color
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            imageVector = when (doneState) {
                DoneState.DONE -> Icons.Outlined.Circle
                DoneState.SKIP -> Icons.Outlined.Close
                else -> Icons.Outlined.Remove
            },
            colorFilter = ColorFilter.tint(
                color.copy(alpha = 0.4f)
            ),
            contentDescription = null
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface
            )
        )
    }
}


@Composable
private fun DoneHistoryDayHeader(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ABB_DAYS.forEach { dayResId ->
            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(align = Alignment.CenterVertically),
                text = stringResource(id = dayResId),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface
                )
            )
        }
    }
}

@Composable
private fun DoneHistoryItem(
    modifier: Modifier = Modifier,
    created: Long,
    doneVo: LoopDoneVo,
    color: Color,
    activeDays: Int
) {
    val createdDate = remember(created) { created.toLocalDate() }
    val localDate = remember(doneVo.date) { doneVo.date.toLocalDate() }
    val isActive = remember(localDate, activeDays) { activeDays.isOn(dayForLoop(localDate)) }
    val isBeforeCreated = remember(localDate, createdDate) { localDate.isBefore(createdDate) }
    val firstDateOfMonth = localDate.dayOfMonth == 1

    val shape = RoundShapes.small
    Box(
        modifier = modifier
            .padding(1.dp)
            .clip(shape = shape)
            .background(
                color = if (firstDateOfMonth) {
                    AppColor.onSurface.copy(alpha = 0.1f)
                } else {
                    Color.Transparent
                }
            )
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.2f),
                shape = shape
            )
            .alpha(if (!isActive || isBeforeCreated || doneVo.isDisabled()) 0.3f else 1.0f)
    ) {
        if (doneVo.isRespond()) {
            Image(
                modifier = Modifier.align(Alignment.Center),
                imageVector = when {
                    doneVo.isDone() -> Icons.Outlined.Circle
                    doneVo.isSkip() -> Icons.Outlined.Close
                    else -> Icons.Outlined.Remove
                },
                colorFilter = ColorFilter.tint(
                    if (doneVo.isDone()) {
                        color
                    } else {
                        AppColor.onSurface
                    }.copy(alpha = 0.4f)
                ),
                contentDescription = null
            )
        }

        DoneHistoryItemText(
            modifier = Modifier
                .align(Alignment.Center),
            createdDate = createdDate,
            localDate = localDate,
        )
    }
}

@Composable
private fun DoneHistoryItemText(
    modifier: Modifier = Modifier,
    createdDate: LocalDate,
    localDate: LocalDate,
) {
    val now = LocalDate.now()
    val firstDateOfMonth = localDate.dayOfMonth == 1

    val text = when {
        now == localDate -> stringResource(id = R.string.today)
        localDate == createdDate -> stringResource(id = R.string.start)
        firstDateOfMonth -> "${localDate.month.value}/1"
        else -> "${localDate.dayOfMonth}"
    }

    Text(
        modifier = modifier,
        text = text,
        style = AppTypography.labelMedium.copy(
            color = AppColor.onSurface,
            fontWeight = FontWeight.Normal
        )
    )
}


private fun <T : Any> LazyGridScope.items(
    lazyPagingItems: LazyPagingItems<T>,
    key: ((item: T) -> Any),
    itemContent: @Composable LazyGridItemScope.(value: T) -> Unit
) {
    items(
        count = lazyPagingItems.itemCount,
        key = { index -> key(requireNotNull(lazyPagingItems[index])) },
        contentType = { "Item" }
    ) { index ->
        itemContent(requireNotNull(lazyPagingItems[index]))
    }
}