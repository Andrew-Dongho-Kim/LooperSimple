package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.home.input.selector.ColorSelector
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer

/** 카드 대신 여백과 구분선으로 묶어 상세 본문과 같은 읽기 흐름을 유지한다. */
@Composable
internal fun EditorForm(
    modifier: Modifier = Modifier,
    draft: LoopEditorDraft,
    enabled: Boolean,
    onChange: (LoopEditorDraft) -> Unit,
) {
    Column(
        modifier = modifier
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Dimens.screenHorizontalPadding)
            .padding(top = DetailSpacing.screenTop, bottom = DetailSpacing.sectionBottom),
        verticalArrangement = Arrangement.spacedBy(DetailSpacing.group),
    ) {
        EditorField(title = stringResource(R.string.detail_loop_name)) {
            NameField(
                draft = draft,
                enabled = enabled,
                onValueChange = { onChange(draft.copy(title = it)) },
            )
        }
        // 공통 색 선택기가 제목 아래의 16dp 여백을 자체 제공한다.
        Column {
            EditorFieldTitle(stringResource(R.string.detail_loop_color))
            ColorSelector(
                selectedColor = draft.color,
                onColorSelected = { if (enabled) onChange(draft.copy(color = it)) },
            )
        }
        HairlineDivider()
        ScheduleFields(draft = draft.schedule, enabled = enabled) {
            onChange(draft.copy(schedule = it))
        }
    }
}

@Composable
private fun EditorField(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(DetailSpacing.headerToContent)) {
        EditorFieldTitle(title)
        content()
    }
}

@Composable
private fun EditorFieldTitle(title: String) {
    Text(title, style = AppTypography.titleSmall.copy(color = AppColor.onSurface))
}

/** 상세 제목의 색 점을 그대로 보여 주되, 입력 영역만 은은하게 강조한다. */
@Composable
private fun NameField(draft: LoopEditorDraft, enabled: Boolean, onValueChange: (String) -> Unit) {
    val label = stringResource(R.string.detail_loop_name)
    val focusManager = LocalFocusManager.current
    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            .sizeIn(minHeight = MinTouchTarget)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(LoopColorDotSize).clip(CircleShape).background(draft.color.compositeOverOnSurface()))
        BasicTextField(
            modifier = Modifier.weight(1f).padding(start = 12.dp)
                .semantics { contentDescription = label },
            value = draft.title,
            onValueChange = onValueChange,
            enabled = enabled,
            singleLine = true,
            cursorBrush = SolidColor(AppColor.primary),
            textStyle = AppTypography.titleMedium.copy(color = AppColor.onSurface),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
            decorationBox = { innerTextField ->
                Box {
                    if (draft.title.isEmpty()) {
                        Text(label, style = AppTypography.titleMedium.copy(color = AppColor.onSurface.copy(alpha = 0.4f)))
                    }
                    innerTextField()
                }
            },
        )
    }
}
