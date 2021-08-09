package com.pnd.android.loop.ui.home

import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FilterList
import androidx.compose.material.icons.outlined.Info
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pnd.android.loop.R
import com.pnd.android.loop.common.test
import com.pnd.android.loop.data.LoopFilter
import com.pnd.android.loop.ui.AppBar
import com.pnd.android.loop.util.textFormatter


@Composable
fun HomeAppBar(
    onNavIconPressed: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: HomeViewModel = viewModel()

    val totalLoops = viewModel.total.observeAsState()

    val countInProgress = viewModel.countInProgress.observeAsState()

    AppBar(
        modifier = modifier,
        onNavIconPressed = onNavIconPressed,
        title = {
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Channel name
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.subtitle1
                )
                // Number of members
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
        }, actions = {
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
    val viewModel = viewModel<HomeViewModel>()
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
        Row {
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