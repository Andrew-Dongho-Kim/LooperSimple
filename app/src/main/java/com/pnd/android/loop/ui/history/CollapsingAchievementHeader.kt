package com.pnd.android.loop.ui.history

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material.icons.outlined.Today
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.common.FloatingHeaderShape
import com.pnd.android.loop.ui.common.FloatingPillHeight
import com.pnd.android.loop.ui.common.FloatingSurface
import com.pnd.android.loop.ui.common.floatingHeaderPadding
import com.pnd.android.loop.ui.common.surfaceReveal
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary

// --- 헤더 치수 -------------------------------------------------------------------------------

/** 액션바 행 높이 (Material3 small TopAppBar와 동일). */
val AchievementHeaderActionBarHeight = 64.dp

/** 헤더가 쉬는(펼침) 상태에서 차지하는 전체 높이. 리스트는 이 높이 아래에서 시작한다. */
fun achievementHeaderExpandedHeight(topInset: Dp) = topInset + AchievementHeaderActionBarHeight

/** 헤더가 완전히 접히기까지의 스크롤 거리. 이 거리를 지나면 타이틀이 사라지고 아이콘이 플로팅된다. */
val AchievementHeaderCollapseDistance = AchievementHeaderActionBarHeight


val TitleTranslationX = (-24).dp

/**
 * 기록 화면의 접히는 헤더. 스크롤되는 리스트 위에 오버레이로 그려지며 배경은 투명하다.
 *
 * [progress]는 리스트 스크롤로 계산된 접힘 정도(`0f..1f`):
 * - `0f`(펼침) — 뒤로가기 + 타이틀이 왼쪽에, 액션 아이콘이 오른쪽에 그대로 보인다.
 * - `1f`(접힘) — 뒤로가기 + 타이틀은 서서히 사라지고, 액션 아이콘은 제자리에서 플로팅 배경을 얻는다.
 *
 * 홈 헤더와 동일한 플로팅/페이딩 규칙을 공유해 두 화면의 상단 동작이 일관되게 느껴지도록 한다.
 */
@Composable
fun CollapsingAchievementHeader(
    progress: Float,
    title: String,
    isDescriptionMode: Boolean,
    onNavigateUp: () -> Unit,
    onToggleViewMode: () -> Unit,
    onMoveToToday: () -> Unit,
    backdrop: BackdropState?,
    modifier: Modifier = Modifier,
) {
    val surfaceProgress = surfaceReveal(progress)
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    Box(
        modifier = modifier
            .floatingHeaderPadding()
            .statusBarsPadding()
            .fillMaxWidth()
            .height(achievementHeaderExpandedHeight(topInset)),
    ) {
        // 뒤로가기 + 타이틀: 액션바 행에 고정된 채, 스크롤할수록 alpha가 0으로 사라진다.
        Row(
            modifier = Modifier
                .align(Alignment.TopStart)
                .height(AchievementHeaderActionBarHeight),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            AppBarIcon(
                modifier = Modifier.alpha(1f - progress),
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                color = AppColor.onSurface,
                descriptionResId = R.string.navi_up,
                onClick = onNavigateUp,
            )

            FloatingSurface(
                modifier = Modifier.offset(x = TitleTranslationX * progress, y = 0.dp),
                progress = surfaceProgress,
                shape = FloatingHeaderShape,
                backdrop = backdrop,
                contentPadding = PaddingValues(horizontal = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .height(FloatingPillHeight),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = title,
                        textAlign = TextAlign.Center,
                        style = AppTypography.headlineSmall.copy(
                            color = AppColor.onSurface,
                            fontWeight = FontWeight.Normal,
                        ),
                    )
                }
            }
        }

        // 액션 아이콘: 오른쪽 제자리를 지키며 스크롤 끝에서 플로팅 배경만 나타난다(홈과 동일).
        Box(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .height(AchievementHeaderActionBarHeight),
            contentAlignment = Alignment.CenterEnd,
        ) {
            FloatingSurface(
                progress = surfaceProgress,
                shape = FloatingHeaderShape,
                backdrop = backdrop,
            ) {
                Row(
                    modifier = Modifier
                        .height(FloatingPillHeight),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // 홈 액션 아이콘과 동일한 룩: onSurface 70% 색 + 좌우 8dp 여백의 심플한 아웃라인 아이콘.
                    // 뷰 모드 토글은 설명 모드일 때만 primary로 강조해 현재 상태를 나타낸다.
                    AppBarIcon(
                        imageVector = Icons.Outlined.Checklist,
                        color = if (isDescriptionMode) {
                            AppColor.primary
                        } else {
                            AppColor.onSurface.copy(alpha = 0.7f)
                        },
                        descriptionResId = R.string.daily_record,
                        onClick = onToggleViewMode,
                    )
                    // 오늘로 이동: 기존 날짜 숫자 배지 대신 홈과 같은 심플한 아웃라인 아이콘으로 통일한다.
                    AppBarIcon(
                        imageVector = Icons.Outlined.Today,
                        color = AppColor.onSurface.copy(alpha = 0.7f),
                        descriptionResId = R.string.navi_today,
                        onClick = onMoveToToday,
                    )
                }
            }
        }
    }
}
