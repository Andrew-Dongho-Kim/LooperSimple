package com.pnd.android.loop.ui.common

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

/**
 * 앱 전역 아이콘 버튼. 48dp 터치 타깃(Material 최소) 안에 14dp 패딩으로 아이콘을 배치해,
 * 모든 화면의 앱바/헤더 아이콘이 같은 크기·히트영역을 갖도록 한다.
 */
@Composable
fun AppBarIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    color: Color,
    @StringRes descriptionResId: Int,
    onClick: () -> Unit = {}
) = Icon(
    modifier = modifier
        .clip(CircleShape)
        .clickable(onClick = onClick)
        .size(48.dp)
        .padding(14.dp),
    imageVector = imageVector,
    tint = color,
    contentDescription = stringResource(descriptionResId)
)
