package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppBar
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.formatYearMonthDateDays
import java.time.LocalDate

@Composable
fun HomeAppBar(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {

    AppBar(
        modifier = modifier,
        title = {
            Column(modifier = Modifier.weight(1f)) {
                val localDate by loopViewModel.localDate.collectAsState(initial = LocalDate.now())
                Text(
                    text = localDate.formatYearMonthDateDays(),
                    style = AppTypography.titleLarge.copy(
                        color = AppColor.onSurface
                    )
                )
            }
        },
        actions = {
            // Info icon
            AppBarIcon(
                imageVector = Icons.Outlined.Info,
                color = AppColor.onSurface.copy(alpha = 0.8f),
                descriptionResId = R.string.about_app,
            )
        }
    )
}
