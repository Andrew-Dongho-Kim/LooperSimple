package com.pnd.android.loop.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary

/**
 * 앱 전역 공용 빈 상태 — 틴트 원 안의 아이콘 + 제목 + 힌트.
 *
 * 루프가 하나도 없을 때, 오늘 할 일을 모두 끝냈을 때, 통계·기록·상세·그룹 화면에 아직 보여줄 것이
 * 없을 때가 모두 같은 문법으로 읽히도록 한 곳에서 스타일을 관리한다. 색은 모두 테마에서 가져와
 * 라이트/다크 모드에 함께 대응한다.
 */
@Composable
fun AppEmptyState(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    hint: String? = null,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color = AppColor.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(36.dp),
                imageVector = icon,
                tint = AppColor.primary,
                contentDescription = null,
            )
        }

        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = title,
            textAlign = TextAlign.Center,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )

        // 힌트는 선택 사항. 제목만으로 충분한 화면(예: 통계 빈 상태)에서는 생략한다.
        if (hint != null) {
            Text(
                modifier = Modifier.padding(top = 6.dp),
                text = hint,
                textAlign = TextAlign.Center,
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.55f),
                ),
            )
        }
    }
}
