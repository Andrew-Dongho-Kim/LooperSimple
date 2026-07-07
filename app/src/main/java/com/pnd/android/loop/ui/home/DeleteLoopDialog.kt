package com.pnd.android.loop.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surfaceElevated

@Composable
fun DeleteLoopDialog(
    modifier: Modifier = Modifier,
    loopTitle: String,
    loopColor: Int,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = AppColor.surfaceElevated,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(id = R.string.delete_loop_title),
                    style = AppTypography.headlineSmall.copy(
                        color = AppColor.onSurface
                    ),
                )

                Spacer(modifier = Modifier.height(14.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(loopColor.compositeOverOnSurface())
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(
                        text = loopTitle,
                        style = AppTypography.titleMedium.copy(
                            color = AppColor.onSurface.copy(alpha = 0.9f)
                        ),
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = stringResource(id = R.string.delete_confirm_message),
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.6f)
                    ),
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = stringResource(id = R.string.cancel),
                            style = AppTypography.titleMedium.copy(
                                color = AppColor.onSurface.copy(alpha = 0.7f)
                            ),
                        )
                    }

                    Spacer(modifier = Modifier.size(8.dp))

                    TextButton(onClick = {
                        onDelete()
                        onDismiss()
                    }) {
                        Text(
                            text = stringResource(id = R.string.delete),
                            style = AppTypography.titleMedium.copy(
                                color = AppColor.error
                            ),
                        )
                    }
                }
            }
        }
    }
}
