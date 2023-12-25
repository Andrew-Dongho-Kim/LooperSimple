package com.pnd.android.loop.ui.input.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.input.common.getSelectorExpandedColor


@Composable
fun InnerTabButton(
    text: String,
    onClick: () -> Unit,
    selected: Boolean,
    modifier: Modifier = Modifier
) {
    val colors = ButtonDefaults.buttonColors(
        backgroundColor = if (selected) {
            MaterialTheme.colors.onSurface.copy(alpha = 0.08f)
        } else {
            getSelectorExpandedColor()
        },
        contentColor = if (selected) {
            MaterialTheme.colors.onSurface
        } else {
            MaterialTheme.colors.onSurface.copy(alpha = 0.74f)
        }
    )
    TextButton(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .height(30.dp),
        shape = MaterialTheme.shapes.medium,
        enabled = true,
        colors = colors,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.subtitle2
        )
    }
}