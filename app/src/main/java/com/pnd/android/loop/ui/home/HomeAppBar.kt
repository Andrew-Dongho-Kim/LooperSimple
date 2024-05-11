package com.pnd.android.loop.ui.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
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
    mode: Int,
    onModeChanged: (Int) -> Unit,
) {
    val countInActive by loopViewModel.countInActive.collectAsState(initial = 0)
    val countInTodayRemain by loopViewModel.countInTodayRemain.collectAsState(initial = 0)
    val countInToday by loopViewModel.countInToday.collectAsState(initial = 0)

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

                Text(
                    modifier = Modifier.padding(top = 8.dp),
                    text = annotatedString(
                        stringResource(
                            id = R.string.today_loop_state,
                            countInActive,
                            countInTodayRemain,
                            countInToday
                        )
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

                FilterMenuItem(
                    text = "전체 루프 보기",
                    isSelected = mode == MODE_ALL_LOOPS,
                    onMenuItemClicked = {
                        isMenuOpened = false
                        onModeChanged(MODE_ALL_LOOPS)
                    }
                )
                FilterMenuItem(
                    text = "섹션 별 보기",
                    isSelected = mode == MODE_SECTIONS,
                    onMenuItemClicked = {
                        isMenuOpened = false
                        onModeChanged(MODE_SECTIONS)
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

@Composable
private fun FilterMenuItem(
    text: String,
    isSelected: Boolean,
    onMenuItemClicked: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onMenuItemClicked() }
                )
                Text(
                    text = text,
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface
                    )
                )
            }

        },
        onClick = onMenuItemClicked
    )
}
