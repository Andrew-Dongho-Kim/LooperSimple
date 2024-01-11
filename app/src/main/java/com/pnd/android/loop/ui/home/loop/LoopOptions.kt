package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.AlertDialog
import androidx.compose.material.ContentAlpha
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.SwipeableState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ModeEdit
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
            style = MaterialTheme.typography.caption.copy(
                color = AppColor.onSurface //.copy(alpha = ContentAlpha.medium)
            )
        )
    }
}

@Composable
fun DeleteDialog(
    modifier: Modifier = Modifier,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = modifier,
        onDismissRequest = onDismiss,
        text = {
            Text(
                text = stringResource(id = R.string.delete_confirm_message),
                style = MaterialTheme.typography.body1
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onDelete()
                onDismiss()
            }) {
                Text(text = stringResource(id = R.string.ok))
            }
        }
    )
}