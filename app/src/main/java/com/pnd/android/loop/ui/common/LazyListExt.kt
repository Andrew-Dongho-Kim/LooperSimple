package com.pnd.android.loop.ui.common

import android.util.Log
import androidx.compose.foundation.lazy.LazyListState

fun LazyListState.findFirstFullyVisibleItemIndex(): Int = findFullyVisibleItemIndex(false)

fun LazyListState.findLastFullyVisibleItemIndex(): Int = findFullyVisibleItemIndex(true)

private fun LazyListState.findFullyVisibleItemIndex(reversed: Boolean): Int {
    val visibleItems = layoutInfo.visibleItemsInfo
    visibleItems
        .run { if (reversed) reversed() else this }
        .forEach { itemInfo ->
            val itemStartOffset = itemInfo.offset
            val itemEndOffset = itemInfo.offset + itemInfo.size
            val viewportStartOffset = layoutInfo.viewportStartOffset
            val viewportEndOffset = layoutInfo.viewportEndOffset
            if (itemStartOffset >= viewportStartOffset && itemEndOffset <= viewportEndOffset) {
                return itemInfo.index
            }
        }
    return if (visibleItems.isEmpty()) {
        -1
    } else if (reversed) {
        visibleItems.last().index
    } else {
        visibleItems.first().index
    }
}