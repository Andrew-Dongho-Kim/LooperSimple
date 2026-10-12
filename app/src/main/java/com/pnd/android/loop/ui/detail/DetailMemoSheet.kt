package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.formatMonthDateDay
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

/** 메모 작성·수정 전용 편집창. 기록의 상태와 시각을 변경하는 기능은 제공하지 않는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun JournalMemoSheet(
    date: LocalDate,
    editable: Boolean,
    memo: String,
    memoLoaded: Boolean,
    memoLoadFailed: Boolean,
    memoDirty: Boolean,
    onRetryLoad: () -> Unit,
    onMemoChange: (String) -> Unit,
    onSaveMemo: suspend () -> Unit,
    onDismiss: () -> Unit,
) {
    var editingMemo by rememberSaveable(date) { mutableStateOf(true) }
    var savingMemo by remember { mutableStateOf(false) }
    var messageRes by remember(date) { mutableStateOf<Int?>(null) }
    var saveFailed by remember(date) { mutableStateOf(false) }
    val busy = savingMemo
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = true,
        // 저장 도중에는 닫지 않는다. 일반적인 닫기에서는 부모가 작성 중인 메모를 보존한다.
        confirmValueChange = { !savingMemo },
    )

    LaunchedEffect(editingMemo, memoLoaded, editable) {
        if (editingMemo && memoLoaded && editable) {
            focusRequester.requestFocus()
            keyboard?.show()
        }
    }

    ModalBottomSheet(
        sheetState = sheetState,
        onDismissRequest = { if (!busy) onDismiss() },
        containerColor = AppColor.surfaceElevated,
        contentColor = AppColor.onSurface,
        tonalElevation = 0.dp,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Dimens.screenHorizontalPadding)
                .padding(bottom = 24.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    modifier = Modifier.weight(1f),
                    text = date.formatMonthDateDay(),
                    style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
                )
                IconButton(
                    enabled = !busy,
                    onClick = onDismiss,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.detail_memo_close),
                        tint = AppColor.onSurface,
                    )
                }
            }

            when {
                !memoLoaded -> {
                    Text(
                        modifier = Modifier.padding(top = DetailSpacing.group),
                        text = stringResource(
                            if (memoLoadFailed) R.string.detail_journal_load_failed
                            else R.string.detail_journal_loading,
                        ),
                        style = AppTypography.bodyMedium.copy(color = AppColor.onSurface.copy(alpha = 0.65f)),
                    )
                    if (memoLoadFailed) {
                        TextActionButton(
                            text = stringResource(R.string.detail_journal_retry),
                            onClick = onRetryLoad,
                        )
                    }
                }
                editingMemo && editable -> {
                    JournalMemoField(
                        modifier = Modifier.padding(top = DetailSpacing.group).focusRequester(focusRequester),
                        value = memo,
                        onValueChange = {
                            onMemoChange(it)
                            messageRes = null
                            saveFailed = false
                        },
                        enabled = !savingMemo,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = DetailSpacing.headerToContent),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Text(
                            modifier = Modifier.weight(1f),
                            text = if (savingMemo) stringResource(R.string.detail_journal_saving)
                            else if (memoDirty) stringResource(R.string.detail_journal_draft) else "",
                            style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
                        )
                        PrimaryPillButton(
                            enabled = memoDirty && !busy,
                            text = stringResource(R.string.save),
                            onClick = {
                                savingMemo = true
                                messageRes = null
                                saveFailed = false
                                scope.launch {
                                    try {
                                        onSaveMemo()
                                        focusManager.clearFocus()
                                        keyboard?.hide()
                                        editingMemo = false
                                        messageRes = R.string.detail_saved_memo
                                    } catch (cancelled: CancellationException) {
                                        throw cancelled
                                    } catch (_: Exception) {
                                        saveFailed = true
                                        messageRes = R.string.detail_journal_save_failed
                                    } finally {
                                        savingMemo = false
                                    }
                                }
                            },
                        )
                    }
                }
                else -> {
                    if (memo.isNotBlank()) {
                        Text(
                            modifier = Modifier.fillMaxWidth().padding(top = DetailSpacing.group, bottom = DetailSpacing.related),
                            text = memo,
                            style = AppTypography.bodyMedium.copy(color = AppColor.onSurface, lineHeight = 22.sp),
                        )
                    }
                    if (editable) {
                        TextActionButton(
                            modifier = Modifier.padding(top = if (memo.isBlank()) DetailSpacing.group else 0.dp),
                            enabled = !busy,
                            text = stringResource(
                                if (memo.isBlank()) R.string.detail_journal_add_memo
                                else R.string.detail_journal_edit_memo,
                            ),
                            onClick = { editingMemo = true },
                        )
                    }
                }
            }

            messageRes?.let { message ->
                Text(
                    modifier = Modifier.padding(top = 12.dp)
                        .semantics { liveRegion = LiveRegionMode.Polite },
                    text = stringResource(message),
                    style = AppTypography.bodySmall.copy(
                        color = if (saveFailed) AppColor.error else AppColor.onSurface.copy(alpha = 0.65f),
                    ),
                )
            }
        }
    }
}

/** 사용자가 메모 수정을 선택했을 때만 나타나는 입력창. 저장 중에는 입력을 잠근다. */
@Composable
private fun JournalMemoField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
    enabled: Boolean,
) {
    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 112.dp, max = 240.dp)
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.12f),
                shape = RoundShapes.medium,
            )
            .padding(all = 14.dp),
        enabled = enabled,
        value = value,
        onValueChange = onValueChange,
        cursorBrush = SolidColor(AppColor.primary),
        textStyle = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface,
            lineHeight = 20.sp,
        ),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.retrospect_hint),
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.4f),
                    ),
                )
            }
            innerTextField()
        },
    )
}
