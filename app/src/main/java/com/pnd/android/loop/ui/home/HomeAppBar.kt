package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
    val totalLoops by loopViewModel.countInTodayRemain.collectAsState(initial = 0)
    val countInProgress by loopViewModel.countInActive.collectAsState(initial = 0)


    AppBar(
        modifier = modifier,
        title = {
            Column(modifier = Modifier.weight(1f)) {
                val localDate by loopViewModel.localDate.collectAsState(initial = LocalDate.now())
                Text(
                    text = localDate.formatYearMonthDateDays(),
                    style = AppTypography.titleMedium.copy(
                        color = AppColor.onSurface
                    )
                )

                Text(
                    text = annotatedString(
                        "#${countInProgress}/${
                            stringResource(
                                R.string.loops,
                                totalLoops
                            )
                        }"
                    ),
                    style = AppTypography.labelMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.8f)
                    )
                )
            }
        },
        actions = {
            // Filter icon
            var isMenuOpened by rememberSaveable { mutableStateOf(false) }
            AppBarIcon(

                imageVector = Icons.Outlined.FilterList,
                color = AppColor.onSurface.copy(alpha = 0.8f),
                descriptionResId = R.string.filter,
                onClick = { isMenuOpened = true }
            )

            DropdownMenu(
                expanded = isMenuOpened,
                onDismissRequest = { isMenuOpened = false }
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "전체 루프 보기",
                            style = AppTypography.bodyMedium.copy(
                                color = AppColor.onSurface
                            )
                        )
                    },
                    onClick = {
                        isMenuOpened = false
                    }
                )
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "섹션 별 보기",
                            style = AppTypography.bodyMedium.copy(
                                color = AppColor.onSurface
                            )
                        )
                    },
                    onClick = {
                        isMenuOpened = false
                    }
                )
            }

            // Info icon
            AppBarIcon(
                imageVector = Icons.Outlined.Info,
                color = AppColor.onSurface.copy(alpha = 0.8f),
                descriptionResId = R.string.about_app,
            )
        }
    )
}
