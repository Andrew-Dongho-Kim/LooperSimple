package com.pnd.android.loop.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.util.isActive

@Immutable
data class LoopCardValues(
    val syncWithTime: Boolean = true,
    val isActive: Boolean = false,
    val isHighlighted: Boolean = false,
    val showAddToGroup: Boolean = true,
)

@Composable
fun LoopCardWithOption(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    cardValues: LoopCardValues,
    onEdit: (LoopBase) -> Unit,
    onDelete: (LoopBase) -> Unit,
    onStateChanged: (LoopBase, Int) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    var isActive by remember { mutableStateOf(false) }
    LaunchedEffect(loop, loopViewModel) {
        loopViewModel.localDateTime.collect { currTime ->
            isActive = loop.isActive(currTime)
        }
    }

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    if (showDeleteDialog) {
        DeleteLoopDialog(
            loopTitle = loop.title,
            onDismiss = {
                showDeleteDialog = false
                blurState.off()
            },
            onDelete = { onDelete(loop) },
        )
    }

    LoopCard(
        modifier = modifier,
        loop = loop,
        cardValues = cardValues.copy(isActive = isActive),
        onEnabled = { enabled ->
            val updated = loop.copyAs(enabled = enabled).asLoopVo()
            loopViewModel.addOrUpdateLoop(updated)
        },
        onStateChanged = onStateChanged,
        onEdit = onEdit,
        onDelete = {
            showDeleteDialog = true
            blurState.on()
        },
        onNavigateToGroupPicker = onNavigateToGroupPicker,
        onNavigateToDetailPage = onNavigateToDetailPage
    )
}
