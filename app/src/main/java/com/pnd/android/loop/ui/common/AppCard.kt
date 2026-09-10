package com.pnd.android.loop.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.outlineVariant
import com.pnd.android.loop.ui.theme.surfaceElevated

/** 공통 카드 표면. Row나 차트처럼 자체 레이아웃이 있는 콘텐츠에도 같은 테두리를 적용한다. */
@Composable
fun Modifier.appCardSurface(color: Color = AppColor.surfaceElevated): Modifier =
    clip(RoundShapes.large)
        .background(color)
        .border(Dimens.cardBorderWidth, AppColor.outlineVariant, RoundShapes.large)

/** 카드 외곽은 여기서, 카드 안의 항목 간격은 각 화면에서 관리한다. */
@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    color: Color = AppColor.surfaceElevated,
    contentPadding: PaddingValues = PaddingValues(Dimens.contentPadding),
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .appCardSurface(color)
            .padding(contentPadding),
        content = content,
    )
}
