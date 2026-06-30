package com.pnd.android.loop.ui.home.input.selector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.outline
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer

@Composable
fun TextSelectorItem(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val colors = ButtonDefaults.buttonColors(
        containerColor = if (selected) {
            AppColor.primary
        } else {
            AppColor.surfaceContainer
        },
        contentColor = if (selected) {
            AppColor.onPrimary
        } else {
            AppColor.onSurface.copy(alpha = 0.74f)
        }
    )
    val border = if (selected) {
        null
    } else {
        BorderStroke(width = 0.5.dp, color = AppColor.outline.copy(alpha = 0.5f))
    }

    TextButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                horizontal = Dimens.contentPadding,
                vertical = Dimens.itemSpacing
            )
            .height(36.dp),
        onClick = onClick,
        shape = RoundShapes.medium,
        border = border,
        enabled = true,
        colors = colors,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = AppTypography.bodyMedium.copy(
                fontWeight = FontWeight.Medium
            )
        )
    }
}