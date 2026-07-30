package com.pnd.android.loop.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceElevated

// --- 공통 다이얼로그 치수 ---------------------------------------------------------------------
// 예전에는 세 다이얼로그가 모서리(28/12/8dp)·좌우 여백(24/32dp)을 제각각 썼다. 여기서 한 곳으로 모아
// 어느 다이얼로그를 열어도 같은 크기·모양·위계로 보이도록 한다.

/** 화면 좌우와 다이얼로그 사이 여백. */
private val DialogHorizontalMargin = 24.dp

/** 다이얼로그 내부 콘텐츠 여백. */
private val DialogContentPadding = 24.dp

/** 콘텐츠와 하단 버튼 행 사이 간격. */
private val DialogButtonRowTopPadding = 24.dp

/** 취소와 확인 버튼 사이 간격. */
private val DialogButtonSpacing = 8.dp

/**
 * 앱 공통 다이얼로그 컨테이너.
 *
 * 모든 다이얼로그가 같은 모양([RoundShapes.large])·배경([AppColor.surfaceElevated])·헤어라인
 * 외곽선·여백을 공유하도록 하는 유일한 진입점이다. 화면 코드는 [content] 슬롯에 본문과
 * 하단의 [DialogButtons]만 채워 넣으면 된다.
 *
 * 헤어라인 외곽선은 배경 블러 위에서도 다이얼로그 경계를 또렷하게 잡아준다.
 */
@Composable
fun AppDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    properties: DialogProperties = DialogProperties(),
    content: @Composable ColumnScope.() -> Unit,
) {
    Dialog(onDismissRequest = onDismiss, properties = properties) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = DialogHorizontalMargin),
            shape = RoundShapes.large,
            color = AppColor.surfaceElevated,
            border = BorderStroke(width = 1.dp, color = AppColor.outlineVariant),
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(DialogContentPadding),
                content = content,
            )
        }
    }
}

/**
 * 다이얼로그 하단의 취소/확인 액션 행.
 *
 * 모든 다이얼로그가 동일한 위치(오른쪽 정렬)·순서(취소 → 확인)·스타일을 쓰도록 강제한다.
 * 확인 버튼은 항상 Bold이며, 색만 용도에 따라 달라진다:
 * - 기본: [AppColor.primary] (일반 확인)
 * - [destructive] = true: [AppColor.error] (삭제 등 되돌릴 수 없는 동작)
 *
 * 색으로 심각도만 구분하고 굵기·위치·모양은 항상 같으므로, 사용자는 "확인 버튼은 이렇게 생겼다"를
 * 한 번만 학습하면 된다.
 */
@Composable
fun DialogButtons(
    confirmText: String,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    cancelText: String = stringResource(id = R.string.cancel),
    confirmEnabled: Boolean = true,
    destructive: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = DialogButtonRowTopPadding),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DialogTextButton(
            text = cancelText,
            onClick = onCancel,
            color = AppColor.onSurface.copy(alpha = 0.6f),
        )
        Spacer(modifier = Modifier.size(DialogButtonSpacing))
        DialogTextButton(
            text = confirmText,
            onClick = onConfirm,
            enabled = confirmEnabled,
            color = if (destructive) AppColor.error else AppColor.primary,
            fontWeight = FontWeight.Bold,
        )
    }
}

/** [DialogButtons] 내부의 단일 텍스트 버튼. 비활성 시에는 색을 흐리게 낮춰 사용 불가를 알린다. */
@Composable
private fun DialogTextButton(
    text: String,
    onClick: () -> Unit,
    color: Color,
    enabled: Boolean = true,
    fontWeight: FontWeight? = null,
) {
    TextButton(onClick = onClick, enabled = enabled) {
        Text(
            text = text,
            style = AppTypography.titleMedium.copy(
                color = if (enabled) color else AppColor.onSurface.copy(alpha = 0.3f),
                fontWeight = fontWeight ?: AppTypography.titleMedium.fontWeight,
            ),
        )
    }
}
