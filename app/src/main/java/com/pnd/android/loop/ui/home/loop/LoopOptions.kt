package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface

@Composable
fun LoopOptions(
    modifier: Modifier = Modifier,
    color: Color,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    var showDeleteDialog by remember { mutableStateOf(false) }
    Row(
        modifier = modifier
            .background(
                color = color.copy(alpha = 0.3f),
                shape = RoundShapes.large
            )
            .clip(
                shape = RoundShapes.large
            ),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        OptionIcon(
            imageVector = Icons.Outlined.ModeEdit,
            text = stringResource(R.string.edit),
            onClick = onEdit
        )

        OptionIcon(
            imageVector = Icons.Outlined.Delete,
            text = stringResource(R.string.delete),
            onClick = {
                showDeleteDialog = true
            }
        )
    }
    if (showDeleteDialog) {
        DeleteDialog(
            onDismiss = { showDeleteDialog = false },
            onDelete = onDelete
        )
    }
}

@Composable
private fun OptionIcon(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    text: String,
    onClick: (() -> Unit) = {}
) {
    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        val color = AppColor.onSurface.copy(alpha = ContentAlpha.medium)
        Icon(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .size(18.dp),
            painter = rememberVectorPainter(image = imageVector),
            tint = color,
            contentDescription = text
        )

        Text(
            text = text,
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface
            )
        )
    }
}
