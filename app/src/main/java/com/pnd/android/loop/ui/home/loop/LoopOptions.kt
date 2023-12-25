package com.pnd.android.loop.ui.home.loop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.alarm.AlarmController
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.home.LoopViewModel
import com.pnd.android.loop.ui.theme.PADDING_HZ_ITEM
import kotlinx.coroutines.launch

@ExperimentalMaterialApi
@Composable
fun LoopOptions(
    alarmHelper: AlarmController,
    loop: LoopVo,
    editedLoop: MutableState<LoopVo?>,
    swipeState: SwipeableState<Int>,
    modifier: Modifier = Modifier
) {
    val viewModel = viewModel<LoopViewModel>()
    val showDeleteDialog = remember { mutableStateOf(false) }

    if (showDeleteDialog.value) {
        DeleteDialog(
            viewModel = viewModel,
            alarmHelper = alarmHelper,
            loop = loop,
            showDeleteDialog = showDeleteDialog
        )
    }

    Row(
        modifier = modifier
            .height(36.dp)
            .padding(start = 12.dp)
            .background(MaterialTheme.colors.primary, MaterialTheme.shapes.medium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val scope = rememberCoroutineScope()

        LoopToolIcon(
            imageVector = Icons.Outlined.ModeEdit,
            onClick = {
                editedLoop.value = loop
                scope.launch { swipeState.animateTo(targetValue = 0) }
            }
        )

        LoopToolIcon(
            imageVector = Icons.Outlined.Delete,
            onClick = {
                showDeleteDialog.value = true
            }
        )
    }
}

@Composable
private fun LoopToolIcon(
    imageVector: ImageVector,
    onClick: (() -> Unit) = {}
) = Icon(
    modifier = Modifier
        .clickable(
            interactionSource = remember { MutableInteractionSource() },
            onClick = onClick,
            indication = rememberRipple(bounded = false, color = Color.White)
        )
        .padding(
            start = PADDING_HZ_ITEM,
            end = PADDING_HZ_ITEM
        )
        .size(24.dp),
    painter = rememberVectorPainter(image = imageVector),
    tint = Color.White,
    contentDescription = null
)


@Composable
fun DeleteDialog(
    viewModel: LoopViewModel,
    alarmHelper: AlarmController,
    loop: LoopVo,
    showDeleteDialog: MutableState<Boolean>
) {
    AlertDialog(
        onDismissRequest = {
            showDeleteDialog.value = false
        },
        text = {
            Text(
                text = stringResource(id = R.string.delete_dialog_message),
                style = MaterialTheme.typography.body1
            )
        },
        dismissButton = {
            TextButton(onClick = {
                showDeleteDialog.value = false
            }) {
                Text(text = stringResource(id = R.string.cancel))
            }
        },
        confirmButton = {
            TextButton(onClick = {
                viewModel.removeLoop(loop)
            }) {
                Text(text = stringResource(id = R.string.ok))
            }
        }
    )
}