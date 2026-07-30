package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppDialog
import com.pnd.android.loop.ui.common.DialogButtons
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface

@Composable
fun DeleteLoopDialog(
    modifier: Modifier = Modifier,
    loopTitle: String,
    loopColor: Int,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AppDialog(modifier = modifier, onDismiss = onDismiss) {
        Text(
            text = stringResource(id = R.string.delete_loop_title),
            style = AppTypography.headlineSmall.copy(
                color = AppColor.onSurface
            ),
        )

        Spacer(modifier = Modifier.height(14.dp))

        DeleteTargetLoop(loopTitle = loopTitle, loopColor = loopColor)

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = stringResource(id = R.string.delete_confirm_message),
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f)
            ),
        )

        // 삭제는 되돌릴 수 없으므로 확인 버튼을 error 색으로 표시(destructive).
        DialogButtons(
            confirmText = stringResource(id = R.string.delete),
            destructive = true,
            onConfirm = {
                onDelete()
                onDismiss()
            },
            onCancel = onDismiss,
        )
    }
}

/** 삭제 대상 루프 표시(색 도트 + 제목). 어떤 루프를 지우는지 확인시켜 준다. */
@Composable
private fun DeleteTargetLoop(
    loopTitle: String,
    loopColor: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(loopColor.compositeOverOnSurface())
        )
        Spacer(modifier = Modifier.size(8.dp))
        Text(
            text = loopTitle,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.9f)
            ),
        )
    }
}
