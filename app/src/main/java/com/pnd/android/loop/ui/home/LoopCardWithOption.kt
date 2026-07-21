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
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.util.isActive

@Immutable
data class LoopCardValues(
    val syncWithTime: Boolean = true,
    val isActive: Boolean = false,
    val isHighlighted: Boolean = false,
    val showAddToGroup: Boolean = true,
    // 옵션 메뉴에 "완료로 기록 / 건너뜀으로 기록"을 노출할지. 전체 탭처럼 응답이 목적이 아닌
    // 화면에서는 false 로 두어 숨긴다.
    val showRecordActions: Boolean = true,
    // 지금 이 카드가 하단 입력 패널에서 편집 중인 대상인지. 강조 테두리 + "수정 중" 배지로 스포트라이트한다.
    val isEditing: Boolean = false,
    // 편집 중인 다른 카드가 있어 이 카드는 배경으로 물러나야 하는지(디밍 대상).
    val isEditDimmed: Boolean = false,
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
            loopColor = loop.color,
            onDismiss = {
                showDeleteDialog = false
                blurState.off()
            },
            onDelete = { onDelete(loop) },
        )
    }

    // '완료로 기록' 다이얼로그는 배경 블러가 필요하므로, blurState 를 가진 이 자리에서
    // 관리한다(삭제 다이얼로그와 동일한 패턴). LoopCard 는 트리거 콜백만 올려 준다.
    var showRecordDoneDialog by rememberSaveable { mutableStateOf(false) }
    if (showRecordDoneDialog) {
        RecordDoneDialog(
            loop = loop,
            onConfirm = { startInDay, endInDay ->
                onStateChanged(
                    loop.copyAs(startInDay = startInDay, endInDay = endInDay),
                    LoopDoneVo.DoneState.DONE,
                )
            },
            onDismiss = {
                showRecordDoneDialog = false
                blurState.off()
            },
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
        onRecordDone = {
            showRecordDoneDialog = true
            blurState.on()
        },
        onEdit = onEdit,
        onDelete = {
            showDeleteDialog = true
            blurState.on()
        },
        onNavigateToGroupPicker = onNavigateToGroupPicker,
        onNavigateToDetailPage = onNavigateToDetailPage
    )
}
