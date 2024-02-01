package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material.ContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppBar
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.home.loop.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.util.annotatedString
import com.pnd.android.loop.util.formatYearMonthDateDays
import java.time.LocalDate

@Composable
fun HomeAppBar(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
) {
    val totalLoops = loopViewModel.total.collectAsState(initial = 0)
    val countInProgress = loopViewModel.countInActive.collectAsState(initial = 0)
    AppBar(
        modifier = modifier,
        title = {
            Column(modifier = Modifier.weight(1f)) {
                val localDate by loopViewModel.localDate.collectAsState(initial = LocalDate.now())
                Text(
                    text = localDate.formatYearMonthDateDays(),
                    style = MaterialTheme.typography.subtitle1
                )

                Text(
                    text = annotatedString(
                        "#${countInProgress.value}/${
                            stringResource(
                                R.string.loops,
                                totalLoops.value
                            )
                        }"
                    ),
                    style = AppTypography.caption.copy(
                        color = AppColor.onSurface.copy(alpha = ContentAlpha.medium)
                    )
                )
            }
        },
        actions = {
            // Info icon
            AppBarIcon(
                imageVector = Icons.Outlined.Info,
                color = AppColor.onSurface.copy(alpha = ContentAlpha.medium),
                descriptionResId = R.string.about_app,
            )
        }
    )
}
