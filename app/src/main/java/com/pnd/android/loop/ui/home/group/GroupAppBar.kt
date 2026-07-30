package com.pnd.android.loop.ui.home.group

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.pnd.android.loop.R
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.onSurface

/**
 * 그룹 화면(GroupPage)과 그룹 선택 화면(GroupPicker)이 공유하는 상단 앱바.
 *
 * 예전에는 두 파일에 완전히 동일한 앱바가 각각 정의돼 있었다. "그룹 추가(+)" 버튼과 생성
 * 다이얼로그까지 똑같으므로 한 곳으로 모아 두 화면의 상단 동작이 항상 일치하도록 한다.
 */
@Composable
internal fun GroupTopAppBar(
    onCreateGroup: (CharSequence) -> Unit,
    onNavigateUp: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showCreateGroupDialog by remember { mutableStateOf(false) }

    SimpleAppBar(
        modifier = modifier,
        title = stringResource(id = R.string.group),
        onNavigateUp = onNavigateUp,
        actions = {
            AppBarIcon(
                imageVector = Icons.Outlined.Add,
                color = AppColor.onSurface.copy(alpha = 0.8f),
                descriptionResId = R.string.add_group,
                onClick = { showCreateGroupDialog = true }
            )
        }
    )

    if (showCreateGroupDialog) {
        CreateGroupDialog(
            onCreate = onCreateGroup,
            onDismiss = { showCreateGroupDialog = false }
        )
    }
}
