package com.pnd.android.loop.ui.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp


@Composable
fun SelectorButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    selected: Boolean,
    text: AnnotatedString = AnnotatedString(""),
    contentDescription: String?,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .clickable(
                onClick = onClick,
                enabled = true,
                role = Role.Button,
                interactionSource = remember { MutableInteractionSource() },
                indication = rememberRipple(bounded = false, radius = 24.dp)
            )
    ) {
        val tint = if (selected) MaterialTheme.colors.primary else LocalContentColor.current
        CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
            Icon(
                modifier = Modifier
                    .padding(top = 12.dp, start = 12.dp, bottom = 12.dp)
                    .size(20.dp),
                imageVector = icon,
                tint = tint,
                contentDescription = contentDescription
            )
            Text(
                modifier = Modifier.padding(end = 12.dp),
                text = text,
                style = MaterialTheme.typography.caption
            )
        }
    }

}