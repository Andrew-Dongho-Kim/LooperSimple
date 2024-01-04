package com.pnd.android.loop.ui.home.loop.input.selector

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.theme.compositeOverSurface

@Composable
fun TextSelectorItem(
    modifier: Modifier = Modifier,
    text: AnnotatedString,
    selected: Boolean = false,
    onClick: () -> Unit = {}
) {
    val colorOnSurface = MaterialTheme.colors.onSurface
    val colors = ButtonDefaults.buttonColors(
        backgroundColor = if (selected) {
            colorOnSurface.copy(alpha = 0.08f)
        } else {
            compositeOverSurface()
        },
        contentColor = if (selected) {
            colorOnSurface
        } else {
            colorOnSurface.copy(alpha = 0.74f)
        }
    )
    val border = if (selected) {
        BorderStroke(width = 0.5.dp, color = colorOnSurface.copy(alpha = 0.3f))
    } else {
        null
    }

    TextButton(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .height(30.dp),
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        border = border,
        enabled = true,
        colors = colors,
        contentPadding = PaddingValues(0.dp)
    ) {
        Text(
            text = text,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.body1.copy(
                shadow = Shadow(blurRadius = 0.5f),
                fontWeight = FontWeight.Medium
            )
        )
    }
}