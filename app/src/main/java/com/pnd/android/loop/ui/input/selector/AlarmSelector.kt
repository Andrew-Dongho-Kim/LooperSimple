package com.pnd.android.loop.ui.input.selector

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusTarget
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.alarm.ALL_ALARM_CATEGORIES
import com.pnd.android.loop.alarm.AlarmCategory
import com.pnd.android.loop.alarm.AlarmPlayer
import com.pnd.android.loop.ui.input.common.InnerTabButton
import com.pnd.android.loop.ui.input.common.TextSelectorItem
import kotlinx.coroutines.launch


private data class Item(
    val key: Int,
    val name: String,
    val resource: Int,
    val isSelected: Boolean
)

@Composable
fun AlarmSelector(
    modifier: Modifier = Modifier,
    selectedAlarm: Int,
    onAlarmSelected: (Int) -> Unit = {},
) {
    var selectedCategory by remember { mutableStateOf(AlarmCategory.Sounds) }

    val description = stringResource(id = R.string.desc_alarm_selector)
    Column(
        modifier = modifier
            .semantics { contentDescription = description }
    ) {

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
                .horizontalScroll(rememberScrollState())
        ) {
            ALL_ALARM_CATEGORIES.forEach { category ->
                InnerTabButton(
                    text = stringResource(id = category.titleResId),
                    onClick = { selectedCategory = category },
                    selected = selectedCategory == category
                )
            }
        }

        val scope = rememberCoroutineScope()
        val listState = rememberLazyListState()
        val context = LocalContext.current
        val alarmPlayer = remember { AlarmPlayer(context) }

        scope.launch {
            listState.scrollToItem(
                (selectedCategory.indexOf(selectedAlarm) - 1).coerceAtLeast(0), 0
            )
        }


        val list = selectedCategory.items.entries.mapIndexed { index, entry ->
            Item(
                key = selectedCategory.key(index),
                name = entry.key,
                resource = entry.value,
                isSelected = selectedCategory.indexOf(selectedAlarm) == index
            )
        }
        LazyColumn(state = listState) {
            items(
                items = list,
                key = { item -> item.name }) { item ->
                TextSelectorItem(
                    text = AnnotatedString(item.name),
                    selected = item.isSelected,
                    onClick = {
                        alarmPlayer.play(item.resource)
                        onAlarmSelected(item.key)
                    }
                )
            }
        }
    }
}
