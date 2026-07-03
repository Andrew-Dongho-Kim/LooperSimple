package com.pnd.android.loop.ui.home.input.selector

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.common.SUPPORTED_COLORS
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.onSurfaceLight
import com.pnd.android.loop.ui.theme.primary

private const val COLOR_COLUMNS = 6

/** 스와치가 차지하는 정사각 영역 — 선택 링과 여백까지 포함한 클릭 타깃. */
private val SwatchSlotSize = 44.dp

/** 실제로 칠해지는 색 원의 지름. 선택 여부와 무관하게 고정되어 레이아웃이 흔들리지 않는다. */
private val SwatchSize = 30.dp

/** 선택된 색을 감싸는 링의 두께. */
private val SelectionRingWidth = 2.dp

@Composable
fun ColorSelector(
    modifier: Modifier = Modifier,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
) {
    val description = stringResource(id = R.string.desc_color_selector)
    Column(
        modifier = modifier
            .padding(top = Dimens.contentPadding)
            .semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(Dimens.cardSpacing),
    ) {
        // 팔레트는 항상 6의 배수라 모든 행이 꽉 차므로 단순히 6개씩 잘라 배치한다.
        SUPPORTED_COLORS.chunked(COLOR_COLUMNS).forEach { rowColors ->
            ColorRow(
                colors = rowColors,
                selectedColor = selectedColor,
                onColorSelected = onColorSelected,
            )
        }
    }
}

@Composable
private fun ColorRow(
    colors: List<Int>,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
    ) {
        colors.forEach { color ->
            ColorSwatch(
                color = color,
                isSelected = color == selectedColor,
                onColorSelected = onColorSelected,
            )
        }
    }
}

/**
 * 색 하나를 나타내는 원형 스와치.
 *
 * 색은 카드의 색점과 동일하게 [compositeOverOnSurface]로 표시해 선택기와 실제 화면이
 * 같은 톤으로 보이게 한다. 밝은 파스텔이 라이트 모드 배경에 묻히지 않도록 옅은 테두리를
 * 항상 두르고, 선택되면 primary 색 링과 체크 표시로 강조한다.
 */
@Composable
private fun ColorSwatch(
    color: Int,
    isSelected: Boolean,
    onColorSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val swatchColor = color.compositeOverOnSurface()
    Box(
        modifier = modifier
            .size(SwatchSlotSize)
            .clip(CircleShape)
            .clickable { onColorSelected(color) },
        contentAlignment = Alignment.Center,
    ) {
        if (isSelected) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .border(
                        width = SelectionRingWidth,
                        color = AppColor.primary,
                        shape = CircleShape,
                    ),
            )
        }

        Box(
            modifier = Modifier
                .size(SwatchSize)
                .clip(CircleShape)
                .background(color = swatchColor)
                .border(
                    width = 1.dp,
                    color = AppColor.onSurface.copy(alpha = 0.12f),
                    shape = CircleShape,
                ),
            contentAlignment = Alignment.Center,
        ) {
            if (isSelected) {
                // 스와치는 항상 밝은 파스텔이라 어두운 체크가 양쪽 테마에서 잘 읽힌다.
                Icon(
                    modifier = Modifier.size(16.dp),
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = AppColor.onSurfaceLight.copy(alpha = 0.7f),
                )
            }
        }
    }
}
