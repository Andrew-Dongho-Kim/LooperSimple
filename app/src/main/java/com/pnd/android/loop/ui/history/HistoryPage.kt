package com.pnd.android.loop.ui.history

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.toLocalDate

@Composable
fun HistoryPage(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel = hiltViewModel()
) {

    HistoryPager(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(),
        historyViewModel = historyViewModel
    )
}

@Composable
private fun HistoryPager(
    modifier: Modifier = Modifier,
    historyViewModel: HistoryViewModel,
) {
    val items = historyViewModel.historyPager.collectAsLazyPagingItems()
    LazyColumn(
        modifier = modifier,
        state = rememberLazyListState()
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
        Text(
            modifier = Modifier.padding(bottom = 8.dp),
            text = item[0].date.toLocalDate().formatYearMonthDateDays(),
            style = AppTypography.subtitle1.copy(
                color = AppColor.onSurface
            )
        )
        item.forEach { loop ->
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