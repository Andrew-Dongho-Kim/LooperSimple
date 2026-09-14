package com.pnd.android.loop.ui.common

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurfaceVariant

@Composable
fun HistoryCalculationNote(estimated: Boolean, modifier: Modifier = Modifier) {
    Column(modifier) {
        Text(stringResource(R.string.history_rate_basis), style = AppTypography.bodySmall,
            color = AppColor.onSurfaceVariant)
        if (estimated) {
            Text(stringResource(R.string.history_estimated), style = AppTypography.bodySmall,
                color = AppColor.onSurfaceVariant)
        }
    }
}
