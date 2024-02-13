package com.pnd.android.loop.ui.history

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.toLocalDate
import java.time.LocalDate

@Composable
fun HistoryPage(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {

    HistoryPager(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(horizontal = 24.dp),
        historyViewModel = historyViewModel
    )
}

@Composable
private fun HistoryPager(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel,
) {
    val items = historyViewModel.historyPager.collectAsLazyPagingItems()
    val lazyListState = rememberLazyListState()
    LaunchedEffect(key1 = Unit) { lazyListState.scrollToItem(index = Int.MAX_VALUE) }
    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        reverseLayout = true,
    ) {
        items(
            count = items.itemCount,
            key = { index -> items[index]!![0].date }
        ) { index ->
            val item = items[index]!!

            HistoryItem(
                modifier = Modifier.padding(top = 12.dp),
                item = item
            )
        }
    }
}

@Composable
private fun HistoryItem(
    modifier: Modifier = Modifier,
    item: List<LoopWithDone>
) {
    Column(modifier = modifier) {
        YearMonthDateHeader(
            modifier = Modifier
                .padding(top = 22.dp)
                .padding(bottom = 12.dp),
            localDate = item[0].date.toLocalDate()
        )

        val doneList = item.filter { it.done == LoopDoneVo.DoneState.DONE }
        if (doneList.isNotEmpty()) {
            HistoryItemSection(
                loops = doneList,
                title = stringResource(id = R.string.done),
                icon = Icons.Filled.Done,
                iconColor = AppColor.primary,
            )
        }

        val skipList = item.filter { it.done == LoopDoneVo.DoneState.SKIP }
        if (skipList.isNotEmpty()) {
            HistoryItemSection(
                modifier = Modifier.padding(top = 12.dp),
                loops = skipList,
                title = stringResource(id = R.string.skip),
                icon = Icons.Filled.Clear,
                iconColor = AppColor.onSurface,
            )
        }
    }
}

@Composable
private fun YearMonthDateHeader(
    modifier: Modifier = Modifier,
    localDate: LocalDate
) {
    Text(
        modifier = modifier.fillMaxWidth(),
        text = localDate.formatYearMonthDateDays(),
        style = AppTypography.h6.copy(
            color = AppColor.onSurface,
            textAlign = TextAlign.Center,
        )
    )
}


@Composable
private fun HistoryItemSection(
    modifier: Modifier = Modifier,
    loops: List<LoopWithDone>,
    title: String,
    icon: ImageVector,
    iconColor: Color,
) {
    Column(modifier = modifier) {
        HistoryItemSectionHeader(
            title = title,
            itemCount = loops.size,
            icon = icon,
            iconColor = iconColor
        )
        loops.forEach { loop ->
            key(loop.id) {
                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = loop.title,
                    style = AppTypography.body1.copy(
                        color = AppColor.onSurface
                    )
                )
            }
        }
    }
}

@Composable
private fun HistoryItemSectionHeader(
    modifier: Modifier = Modifier,
    title: String,
    itemCount: Int,
    icon: ImageVector,
    iconColor: Color,
) {
    Row(
        modifier = modifier
            .padding(bottom = 8.dp)
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "($itemCount) $title",
            style = MaterialTheme.typography.subtitle1.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(12.dp),
            imageVector = icon,
            colorFilter = ColorFilter.tint(color = iconColor),
            contentDescription = title
        )

    }
}