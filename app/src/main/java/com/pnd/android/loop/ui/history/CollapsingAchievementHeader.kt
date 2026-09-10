package com.pnd.android.loop.ui.history

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Today
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.AppPageHeader
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.onSurfaceVariant

val AchievementHeaderActionBarHeight = Dimens.appBarHeight
val AchievementHeaderCollapseDistance = AchievementHeaderActionBarHeight

fun achievementHeaderExpandedHeight(topInset: Dp) = topInset + AchievementHeaderActionBarHeight

/** 기록 화면의 날짜와 오늘 이동 동작만 정의하고, 헤더 배치는 공통 컴포넌트에 맡긴다. */
@Composable
fun CollapsingAchievementHeader(
    progress: Float,
    title: String,
    onNavigateUp: () -> Unit,
    onMoveToToday: () -> Unit,
    backdrop: BackdropState?,
    modifier: Modifier = Modifier,
) {
    AppPageHeader(
        modifier = modifier,
        title = title,
        onNavigateUp = onNavigateUp,
        progress = progress,
        backdrop = backdrop,
        actions = {
            AppBarIcon(
                imageVector = Icons.Outlined.Today,
                color = AppColor.onSurfaceVariant,
                descriptionResId = R.string.navi_today,
                onClick = onMoveToToday,
            )
        },
    )
}
