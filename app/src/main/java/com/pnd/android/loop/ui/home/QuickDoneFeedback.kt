package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarVisuals
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.state.QuickDone
import com.pnd.android.loop.ui.home.viewmodel.QuickDoneReceipt

/**
 * ✓ 한 번 완료 뒤에 띄우는 스낵바. 기본 스낵바는 동작을 하나만 담을 수 있어, '실행취소'는
 * 스낵바의 기본 동작(performAction)으로 두고 '시간 수정'은 [onEdit] 으로 따로 싣는다.
 */
class QuickDoneSnackbarVisuals(
    override val message: String,
    override val actionLabel: String,
    val editLabel: String,
    val onEdit: () -> Unit,
) : SnackbarVisuals {
    override val withDismissAction: Boolean get() = false
    override val duration: SnackbarDuration get() = SnackbarDuration.Long
}

/** 홈 스낵바 호스트의 그리기. ✓ 스낵바만 두 동작으로 그리고 나머지는 기본 모양 그대로다. */
@Composable
fun HomeSnackbar(data: SnackbarData) {
    val visuals = data.visuals as? QuickDoneSnackbarVisuals
    if (visuals == null) {
        Snackbar(snackbarData = data)
        return
    }
    Snackbar(
        modifier = Modifier.padding(12.dp),
        action = {
            // 기본 스낵바의 동작 버튼과 같은 색을 써서 어두운 스낵바 위에서도 또렷하게 읽히게 한다.
            val colors = ButtonDefaults.textButtonColors(contentColor = SnackbarDefaults.actionColor)
            Row {
                TextButton(
                    colors = colors,
                    onClick = {
                        visuals.onEdit()
                        data.dismiss()
                    },
                ) {
                    Text(text = visuals.editLabel)
                }
                TextButton(colors = colors, onClick = { data.performAction() }) {
                    Text(text = visuals.actionLabel)
                }
            }
        },
    ) {
        Text(text = visuals.message)
    }
}

/** ✓ 로 무엇을 남겼는지 한 줄로. */
fun Context.quickDoneMessage(receipt: QuickDoneReceipt): String {
    val title = receipt.loop.title
    return when (val result = receipt.result) {
        is QuickDone.EarlyFinish -> getString(R.string.quick_done_snack_early, title, result.times.rangeText())
        QuickDone.AsPlanned -> getString(R.string.quick_done_snack_planned, title)
    }
}
