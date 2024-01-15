package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridItemScope
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyHorizontalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.LazyPagingItems
import androidx.paging.compose.collectAsLazyPagingItems
import com.pnd.android.loop.R
import com.pnd.android.loop.data.Day
import com.pnd.android.loop.data.Day.Companion.isOn
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.day
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.rememberDayColor
import com.pnd.android.loop.util.toLocalDate
import java.time.LocalDate

@Composable
fun DetailPage(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel = hiltViewModel()
) {
    val loop by detailViewModel.loop.collectAsState(LoopBase.default())
    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        topBar = {
            DetailAppBar(
                modifier = Modifier
                    .background(
                        color = AppColor.surface
                    )
                    .statusBarsPadding(),
                loop = loop,
            )
        }
    )
    { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            DetailPageContent(
                detailViewModel = detailViewModel,
                loop = loop,
            )
        }
    }
}

@Composable
private fun DetailPageContent(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .fillMaxWidth()
    ) {
        LoopCreatedDate(
            modifier = Modifier.padding(top = 8.dp),
            created = loop.created
        )
        DoneHistoryGrid(
            modifier = modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
                .height(224.dp),
            detailViewModel = detailViewModel,
            loop = loop,
        )
    }
}

@Composable
private fun LoopCreatedDate(
    modifier: Modifier = Modifier,
    created: Long,
) {
    val createdDate = created.toLocalDate()
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            modifier = Modifier,
            text = stringResource(id = R.string.created_date) + " : ",
            style = AppTypography.subtitle1.copy(
                color = AppColor.onSurface
            )
        )

        Text(
            modifier = Modifier.weight(1f),
            text = createdDate.formatYearMonthDateDays(),
            style = AppTypography.body1.copy(
                color = AppColor.onSurface
            )
        )

        Text(
            modifier = Modifier,
            text = stringResource(id = R.string.n_days, createdDate.until(LocalDate.now()).days),
            style = AppTypography.body1.copy(
                color = AppColor.onSurface
            )
        )
    }
}

@Composable
private fun DoneHistoryGrid(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    val doneHistory = detailViewModel.donePager.collectAsLazyPagingItems()

    val lazyGridState = rememberLazyGridState()
    Row(modifier = modifier) {
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
                    doneVo = item,
                    color = Color(loop.color).copy(alpha = 0.7f),
                    activeDays = loop.loopActiveDays
                )
            }
        }
        DoneHistoryDayHeader(
            modifier = Modifier.padding(start = 6.dp)
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
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val day = Day.fromIndex(index)
            Text(
                modifier = Modifier
                    .weight(1f)
                    .wrapContentHeight(align = Alignment.CenterVertically),
                text = stringResource(id = dayResId),
                style = AppTypography.body1.copy(
                    color = rememberDayColor(day = day)
                )
            )
        }
    }
}

@Composable
private fun DoneHistoryItem(
    modifier: Modifier = Modifier,
    doneVo: LoopDoneVo,
    color: Color,
    activeDays: Int
) {
    val localDate = remember(doneVo.date) { doneVo.date.toLocalDate() }
    val firstDateOfMonth = localDate.dayOfMonth == 1
    val isActive = activeDays.isOn(day(doneVo.date.toLocalDate()))

    val shape = RoundShapes.small
    Box(
        modifier = modifier
            .padding(1.dp)
            .clip(shape = shape)
            .background(
                color = if (firstDateOfMonth) AppColor.onSurface.copy(alpha = 0.1f) else Color.Transparent
            )
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.2f),
                shape = shape
            )
            .alpha(if (isActive) 1.0f else 0.3f)
    ) {

        if (doneVo.done != LoopDoneVo.DoneState.NO_RESPONSE) {
            Image(
                modifier = Modifier.align(Alignment.Center),
                imageVector = when {
                    doneVo.isDone() -> Icons.Outlined.Circle
                    else -> Icons.Outlined.Close
                },
                colorFilter = ColorFilter.tint(
                    if (doneVo.isDone()) {
                        color
                    } else {
                        AppColor.onSurface.copy(alpha = ContentAlpha.disabled)
                    }
                ),
                contentDescription = null
            )
        }

        Text(
            modifier = Modifier.align(Alignment.Center),
            text = if (firstDateOfMonth) "${localDate.month.value}/1" else "${localDate.dayOfMonth}",
            style = AppTypography.caption.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Normal
            )
        )
    }
}

fun <T : Any> LazyGridScope.items(
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
