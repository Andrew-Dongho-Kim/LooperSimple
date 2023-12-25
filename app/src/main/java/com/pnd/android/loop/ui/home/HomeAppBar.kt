package com.pnd.android.loop.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.Checkbox
import androidx.compose.material.ContentAlpha
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentAlpha
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TopAppBar
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.common.test
import com.pnd.android.loop.data.LoopFilter
import com.pnd.android.loop.ui.theme.elevatedSurface
import com.pnd.android.loop.util.textFormatter
import com.pnd.android.loop.util.toYearMonthDateDaysString
import java.time.LocalDate

@Composable
fun HomeAppBar(
    modifier: Modifier = Modifier,
) {
    val viewModel: LoopViewModel = viewModel()
    val totalLoops = viewModel.total.observeAsState()
    val countInProgress = viewModel.countInProgress.observeAsState()

    AppBar(
        modifier = modifier,
        title = {
            Column(modifier = Modifier.weight(1f)) {
                val localDate by viewModel.localTime.collectAsState(initial = LocalDate.now())
                Text(
                    text = localDate.toYearMonthDateDaysString(),
                    style = MaterialTheme.typography.subtitle1
                )

                CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                    Text(
                        text = textFormatter(
                            "#${countInProgress.value ?: 0}/${
                                stringResource(
                                    R.string.loops,
                                    totalLoops.value ?: 0
                                )
                            }"
                        ),
                        style = MaterialTheme.typography.caption
                    )
                }
            }
        },
        actions = {
            CompositionLocalProvider(LocalContentAlpha provides ContentAlpha.medium) {
                // Filter icon
                FilterAppIcon()

                // Info icon
                AppBarIcon(
                    imageVector = Icons.Outlined.Info,
                    descriptionResId = R.string.about_app
                )
            }
        }
    )
}


@Composable
private fun AppBar(
    modifier: Modifier = Modifier,
    title: @Composable RowScope.() -> Unit,
    actions: @Composable RowScope.() -> Unit = {}
) {
    // This bar is translucent but elevation overlays are not applied to translucent colors.
    // Instead we manually calculate the elevated surface color from the opaque color,
    // then apply our alpha.
    //
    // We set the background on the Column rather than the TopAppBar,
    // so that the background is drawn behind any padding set on the app bar (i.e. status bar).
    val backgroundColor = MaterialTheme.colors.elevatedSurface(3.dp)
    Column(
        Modifier.background(backgroundColor.copy(alpha = 0.95f))
    ) {
        TopAppBar(
            modifier = modifier,
            backgroundColor = Color.Transparent,
            elevation = 0.dp, // No shadow needed
            contentColor = MaterialTheme.colors.onSurface,
            actions = actions,
            title = { Row { title() } },
        )
        Divider()
    }
}

@Composable
fun AppBarIcon(
    imageVector: ImageVector,
    @StringRes descriptionResId: Int,
    onClick: () -> Unit = {}
) = Icon(
    imageVector = imageVector,
    modifier = Modifier
        .clickable(onClick = onClick)
        .padding(horizontal = 12.dp, vertical = 16.dp)
        .height(24.dp),
    contentDescription = stringResource(descriptionResId)
)

@Composable
fun FilterAppIcon() {
    val viewModel = viewModel<LoopViewModel>()
    val loopFilter by viewModel.loopFilter.observeAsState(LoopFilter.DEFAULT)

    var isExpanded by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .wrapContentSize(Alignment.TopEnd)
    ) {
        AppBarIcon(
            imageVector = Icons.Outlined.FilterList,
            descriptionResId = R.string.filter,
            onClick = {
                isExpanded = !isExpanded
                test { "isExpanded : $isExpanded" }
            }
        )

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false }) {
            CheckableMenuItem(
                strResId = R.string.filter_enabled,
                checked = loopFilter.onlyEnabled,
                onCheckChanged = { checked -> viewModel.saveFilter(loopFilter.copy(onlyEnabled = checked)) }
            )
            CheckableMenuItem(
                strResId = R.string.filter_progress,
                checked = loopFilter.onlyProgress,
                onCheckChanged = { checked -> viewModel.saveFilter(loopFilter.copy(onlyProgress = checked)) }
            )
            CheckableMenuItem(
                strResId = R.string.filter_today,
                checked = loopFilter.onlyToday,
                onCheckChanged = { checked -> viewModel.saveFilter(loopFilter.copy(onlyToday = checked)) }
            )
        }
    }
}

@Composable
fun CheckableMenuItem(
    @StringRes strResId: Int,
    checked: Boolean,
    onCheckChanged: (Boolean) -> Unit = {}
) {
    DropdownMenuItem(onClick = { onCheckChanged(!checked) }) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = stringResource(strResId),
                style = MaterialTheme.typography.body1
            )
            Spacer(modifier = Modifier.width(8.dp))
            Checkbox(checked = checked, onCheckedChange = onCheckChanged)
        }
    }
}