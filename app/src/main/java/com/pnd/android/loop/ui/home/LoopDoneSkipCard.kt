package com.pnd.android.loop.ui.home

import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopWithDone
import com.pnd.android.loop.ui.common.isLargeScreen
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.formatMonthDateDay
import java.time.LocalDate

@Composable
fun LoopDoneSkipCard(
    modifier: Modifier = Modifier,
    section: Section,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    val loops by section.items

    Column(modifier = modifier) {
        if (loops.isNotEmpty()) {
            LoopDoneSkipCardContent(
                modifier = Modifier.padding(top = 12.dp),
                blurState = blurState,
                loops = loops,
                loopViewModel = loopViewModel,
                onNavigateToDetailPage = onNavigateToDetailPage,
                onNavigateToHistoryPage = onNavigateToHistoryPage
            )
        }
    }
}

@Composable
private fun LoopDoneSkipCardContent(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loops: List<LoopBase>,
    loopViewModel: LoopViewModel,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    val loopGroup = loops.groupBy { (it as LoopWithDone).done }

    val onUndoDoneState = remember {
        { loop: LoopBase ->
            loopViewModel.doneLoop(
                loop = loop,
                doneState = DoneState.NO_RESPONSE
            )
        }
    }

    Column(modifier = modifier) {
        DoneSkipCard(
            blurState = blurState,
            loopViewModel = loopViewModel,
            loops = loopGroup[DoneState.DONE] ?: emptyList(),
            title = stringResource(id = R.string.done),
            icon = Icons.Filled.Done,
            iconColor = AppColor.primary,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            onUndoDoneState = onUndoDoneState,
        )

        DoneSkipCard(
            blurState = blurState,
            modifier = Modifier.padding(top = 12.dp),
            loopViewModel = loopViewModel,
            loops = loopGroup[DoneState.SKIP] ?: emptyList(),
            title = stringResource(id = R.string.skip),
            icon = Icons.Filled.Clear,
            iconColor = AppColor.onSurface,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            onUndoDoneState = onUndoDoneState,
        )
    }
}

@Composable
private fun DoneSkipCard(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loopViewModel: LoopViewModel,
    loops: List<LoopBase>,
    title: String,
    icon: ImageVector,
    iconColor: Color,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onUndoDoneState: (loop: LoopBase) -> Unit,
) {
    if (loops.isEmpty()) return

    Column(
        modifier = modifier.padding(
            horizontal = 12.dp,
            vertical = 8.dp
        )
    ) {
        DoneSkipHeader(
            modifier = Modifier.align(Alignment.CenterHorizontally),
            title = title,
            itemCount = loops.size,
            icon = icon,
            iconColor = iconColor,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )
        Column(
            modifier = Modifier.padding(top = 4.dp)
        ) {
            loops.forEach { loop ->
                key(loop.loopId) {
                    DoneSkipItem(
                        blurState = blurState,
                        loop = loop,
                        onGetRetrospect = {
                            loopViewModel.getMemo(
                                loopId = loop.loopId,
                                localDate = LocalDate.now()
                            )?.text ?: ""
                        },
                        onSaveRetrospect = { text ->
                            loopViewModel.saveMemo(
                                loopId = loop.loopId,
                                localDate = LocalDate.now(),
                                text = text,
                            )
                        },
                        onNavigateToDetailPage = onNavigateToDetailPage,
                        onUndoDoneState = onUndoDoneState,
                    )
                }
            }
        }
    }

}

@Composable
private fun DoneSkipHeader(
    modifier: Modifier = Modifier,
    title: String,
    itemCount: Int,
    icon: ImageVector,
    iconColor: Color,
    onNavigateToHistoryPage: () -> Unit
) {
    Row(
        modifier = modifier
            .padding(bottom = 8.dp)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.1f),
                shape = RoundShapes.medium
            )
            .clip(RoundShapes.medium)
            .clickable(onClick = onNavigateToHistoryPage)
            .padding(vertical = 6.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "($itemCount) $title",
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
        Image(
            modifier = Modifier
                .padding(start = 8.dp)
                .size(12.dp),
            imageVector = icon,
            colorFilter = ColorFilter.tint(color = iconColor),
            contentDescription = title
        )

    }
}

@Composable
private fun DoneSkipItem(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    loop: LoopBase,
    onGetRetrospect: suspend () -> String,
    onSaveRetrospect: (String) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onUndoDoneState: (loop: LoopBase) -> Unit,
) {
    var isRetrospectDialogOpened by rememberSaveable { mutableStateOf(false) }

    var retrospect by remember { mutableStateOf("") }
    LaunchedEffect(key1 = loop.loopId) {
        retrospect = onGetRetrospect()
    }

    Row(
        modifier = modifier
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .clickable { onNavigateToDetailPage(loop) },
        verticalAlignment = Alignment.CenterVertically,

        ) {
        LoopCardColor(
            modifier = Modifier
                .padding(horizontal = 8.dp)
                .padding(end = 12.dp)
                .size(8.dp),
            color = loop.color
        )

        DoneSkipItemTitle(
            modifier = Modifier.weight(1f),
            title = loop.title
        )

        if (LocalConfiguration.current.isLargeScreen()) {
            DoneSkipItemStartAndEndTime(
                modifier = Modifier.padding(horizontal = 8.dp),
                loopStart = loop.startInDay,
                loopEnd = loop.endInDay
            )
        }

        DoneSkipCardButton(
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = stringResource(id = R.string.memo),
            tintColor = if (retrospect.isNotEmpty()) AppColor.primary else AppColor.onSurface,
            tintColorAlpha = 0.7f,
            onClick = {
                isRetrospectDialogOpened = true
                blurState.on()
            },
        )

        DoneSkipCardButton(
            modifier = Modifier.padding(start = 12.dp),
            imageVector = Icons.Filled.Refresh,
            contentDescription = stringResource(id = R.string.restore),
            onClick = { onUndoDoneState(loop) },
        )
    }

    if (isRetrospectDialogOpened) {
        RetrospectDialog(
            loop = loop,
            retrospect = retrospect,
            onRetrospectChanged = { retrospect = it },
            onSaveRetrospect = onSaveRetrospect,
            onDismiss = {
                isRetrospectDialogOpened = false
                blurState.off()
            },
        )
    }
}

@Composable
private fun DoneSkipItemTitle(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        style = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface,
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun DoneSkipItemStartAndEndTime(
    modifier: Modifier = Modifier,
    loopStart: Long,
    loopEnd: Long
) {
    Text(
        modifier = modifier,
        text = "(${loopStart.formatHourMinute(withAmPm = true)} ~ ${
            loopEnd.formatHourMinute(
                withAmPm = true
            )
        })",
        style = AppTypography.bodySmall.copy(
            color = AppColor.onSurface.copy(
                alpha = 0.3f
            ),
        )
    )
}

@Composable
private fun DoneSkipCardButton(
    modifier: Modifier = Modifier,
    imageVector: ImageVector,
    contentDescription: String,
    tintColor: Color = AppColor.onSurface,
    tintColorAlpha: Float = 0.7f,
    onClick: () -> Unit,
) {
    Image(
        modifier = modifier
            .clip(shape = RoundShapes.small)
            .clickable(onClick = onClick)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.1f),
                shape = RoundShapes.small
            )
            .padding(horizontal = 10.dp, vertical = 4.dp)
            .size(16.dp),
        imageVector = imageVector,
        colorFilter = ColorFilter.tint(
            color = tintColor.copy(alpha = tintColorAlpha)
        ),
        contentDescription = contentDescription
    )
}

@Composable
private fun RetrospectDialog(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    retrospect: String,
    onRetrospectChanged: (String) -> Unit,
    onSaveRetrospect: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        modifier = modifier.padding(horizontal = 32.dp),
        shape = RoundShapes.medium,
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(id = R.string.retrospect),
                style = AppTypography.titleLarge.copy(
                    color = AppColor.onSurface
                ),
            )
        },
        text = {
            RetrospectDialogContent(
                loop = loop,
                retrospect = retrospect,
                onRetrospectChanged = onRetrospectChanged
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(id = R.string.cancel),
                    style = AppTypography.titleMedium,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onSaveRetrospect(retrospect)
                onDismiss()
            }) {
                Text(
                    text = stringResource(id = R.string.save),
                    style = AppTypography.titleMedium,
                )
            }
        },
        textContentColor = AppColor.surface,
        containerColor = AppColor.surface,
        tonalElevation = 0.dp,
        properties = DialogProperties(dismissOnClickOutside = false)
    )
}

@Composable
private fun RetrospectDialogContent(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    retrospect: String,
    onRetrospectChanged: (String) -> Unit,
) {
    Column(modifier = modifier) {
        Row {
            Text(
                modifier = Modifier.weight(1f),
                text = loop.title,
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface
                )
            )

            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = LocalDate.now().formatMonthDateDay(),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface
                )
            )
        }

        BasicTextField(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .border(
                    width = 1.dp,
                    color = AppColor.onSurface.copy(alpha = 0.2f),
                    shape = RoundShapes.small
                )
                .padding(all = 8.dp),
            value = retrospect,
            onValueChange = { onRetrospectChanged(it) },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Default
            ),
            cursorBrush = SolidColor(AppColor.onSurface),
            textStyle = AppTypography.bodyMedium.copy(color = AppColor.onSurface)
        )
    }
}