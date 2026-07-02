package com.pnd.android.loop.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
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
import androidx.compose.ui.unit.sp
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
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
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
    // Only LoopWithDone entries carry a done state; ignore any other LoopBase
    // (e.g. the in-progress edit mock) instead of hard-casting and crashing.
    val loopGroup = loops.filterIsInstance<LoopWithDone>().groupBy { it.done }

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

    Surface(
        modifier = modifier.padding(horizontal = 12.dp),
        shape = RoundShapes.large,
        color = AppColor.surfaceElevated,
        border = BorderStroke(
            width = 0.5.dp,
            color = AppColor.onSurface.copy(alpha = 0.08f)
        ),
    ) {
        Column {
            DoneSkipHeader(
                title = title,
                itemCount = loops.size,
                icon = icon,
                iconColor = iconColor,
                onNavigateToHistoryPage = onNavigateToHistoryPage,
            )
            HorizontalDivider(color = AppColor.onSurface.copy(alpha = 0.08f))
            loops.forEachIndexed { index, loop ->
                key(loop.loopId) {
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            color = AppColor.onSurface.copy(alpha = 0.05f)
                        )
                    }
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
            .fillMaxWidth()
            .clickable(onClick = onNavigateToHistoryPage)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DoneSkipHeaderIcon(icon = icon, iconColor = iconColor, title = title)

        Text(
            modifier = Modifier.padding(start = 10.dp),
            text = title,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold
            )
        )
        DoneSkipCountBadge(
            modifier = Modifier.padding(start = 6.dp),
            count = itemCount
        )

        Spacer(modifier = Modifier.weight(1f))

        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            tint = AppColor.onSurface.copy(alpha = 0.4f),
            contentDescription = null
        )
    }
}

@Composable
private fun DoneSkipHeaderIcon(
    icon: ImageVector,
    iconColor: Color,
    title: String,
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .background(
                color = iconColor.copy(alpha = 0.12f),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = Modifier.size(14.dp),
            imageVector = icon,
            colorFilter = ColorFilter.tint(color = iconColor),
            contentDescription = title
        )
    }
}

@Composable
private fun DoneSkipCountBadge(
    modifier: Modifier = Modifier,
    count: Int,
) {
    Box(
        modifier = modifier
            .background(
                color = AppColor.surfaceContainer,
                shape = CircleShape
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = count.toString(),
            style = AppTypography.labelMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
                fontWeight = FontWeight.SemiBold
            )
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
            .clickable { onNavigateToDetailPage(loop) }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LoopCardColor(
            modifier = Modifier
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
            modifier = Modifier.padding(start = 8.dp),
            imageVector = Icons.AutoMirrored.Filled.Chat,
            contentDescription = stringResource(id = R.string.memo),
            tintColor = if (retrospect.isNotEmpty()) AppColor.primary else AppColor.onSurface,
            isHighlighted = retrospect.isNotEmpty(),
            onClick = {
                isRetrospectDialogOpened = true
                blurState.on()
            },
        )

        DoneSkipCardButton(
            modifier = Modifier.padding(start = 8.dp),
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
    isHighlighted: Boolean = false,
    onClick: () -> Unit,
) {
    val backgroundColor = if (isHighlighted) {
        AppColor.primary.copy(alpha = 0.12f)
    } else {
        AppColor.surfaceContainer
    }

    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(color = backgroundColor)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = imageVector,
            tint = tintColor.copy(alpha = if (isHighlighted) 1f else 0.7f),
            contentDescription = contentDescription
        )
    }
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
        shape = RoundShapes.large,
        onDismissRequest = onDismiss,
        title = { RetrospectDialogTitle() },
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
                    style = AppTypography.titleMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.6f)
                    ),
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
                    style = AppTypography.titleMedium.copy(
                        color = AppColor.primary,
                        fontWeight = FontWeight.Bold
                    ),
                )
            }
        },
        textContentColor = AppColor.onSurface,
        containerColor = AppColor.surfaceElevated,
        tonalElevation = 0.dp,
        properties = DialogProperties(dismissOnClickOutside = false)
    )
}

@Composable
private fun RetrospectDialogTitle(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(
                    color = AppColor.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.AutoMirrored.Filled.Chat,
                tint = AppColor.primary,
                contentDescription = null
            )
        }

        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = stringResource(id = R.string.retrospect),
            style = AppTypography.titleLarge.copy(
                color = AppColor.onSurface
            ),
        )
    }
}

@Composable
private fun RetrospectDialogContent(
    modifier: Modifier = Modifier,
    loop: LoopBase,
    retrospect: String,
    onRetrospectChanged: (String) -> Unit,
) {
    Column(modifier = modifier) {
        RetrospectLoopInfo(loop = loop)

        RetrospectTextField(
            modifier = Modifier.padding(top = 16.dp),
            retrospect = retrospect,
            onRetrospectChanged = onRetrospectChanged
        )

        RetrospectFooter(
            modifier = Modifier
                .align(Alignment.End)
                .padding(top = 8.dp),
            charCount = retrospect.length,
            onClear = { onRetrospectChanged("") }
        )
    }
}

@Composable
private fun RetrospectLoopInfo(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(color = AppColor.surfaceContainer)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LoopCardColor(
            modifier = Modifier
                .padding(end = 10.dp)
                .size(8.dp),
            color = loop.color
        )

        Text(
            modifier = Modifier.weight(1f),
            text = loop.title,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.SemiBold
            ),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = LocalDate.now().formatMonthDateDay(),
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f)
            )
        )
    }
}

@Composable
private fun RetrospectTextField(
    modifier: Modifier = Modifier,
    retrospect: String,
    onRetrospectChanged: (String) -> Unit,
) {
    // Open the keyboard right away so the user can start writing without an extra tap.
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 120.dp)
            .clip(RoundShapes.medium)
            .background(color = AppColor.surfaceContainer)
            .border(
                width = 1.dp,
                color = AppColor.primary.copy(alpha = 0.25f),
                shape = RoundShapes.medium
            )
            .padding(all = 14.dp)
            .focusRequester(focusRequester),
        value = retrospect,
        onValueChange = onRetrospectChanged,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Default
        ),
        cursorBrush = SolidColor(AppColor.primary),
        textStyle = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface,
            lineHeight = 20.sp
        ),
        decorationBox = { innerTextField ->
            if (retrospect.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.retrospect_hint),
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.4f)
                    )
                )
            }
            innerTextField()
        }
    )
}

@Composable
private fun RetrospectFooter(
    modifier: Modifier = Modifier,
    charCount: Int,
    onClear: () -> Unit,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (charCount > 0) {
            Text(
                modifier = Modifier
                    .clip(RoundShapes.small)
                    .clickable(onClick = onClear)
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                text = stringResource(id = R.string.retrospect_clear),
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.5f)
                )
            )
        }

        Text(
            modifier = Modifier.padding(start = 8.dp),
            text = stringResource(id = R.string.retrospect_char_count, charCount),
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.4f)
            )
        )
    }
}