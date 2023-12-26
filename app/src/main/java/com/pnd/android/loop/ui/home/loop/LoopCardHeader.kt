package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Card
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.ui.common.DashedDivider

@Composable
fun LoopCardHeader(
    modifier: Modifier = Modifier,
    headText: String,
    isExpanded: Boolean,
    onExpandChanged: (isExpanded: Boolean) -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onExpandChanged(!isExpanded) }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {

        Text(
            text = headText,
            style = MaterialTheme.typography.subtitle2.copy(
                color = MaterialTheme.colors.onSurface,
            )
        )
        DashedDivider(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
            dashWidth = 2.dp,
            dashHeight = 1.dp,
            gapWidth = 2.dp,
            color = MaterialTheme.colors.onSurface.copy(alpha = 0.4f)
        )

        Image(
            imageVector = if (!isExpanded) Icons.Rounded.ExpandMore else Icons.Rounded.ExpandLess,
            colorFilter = ColorFilter.tint(color = MaterialTheme.colors.onSurface),
            contentDescription = ""
        )

    }


}