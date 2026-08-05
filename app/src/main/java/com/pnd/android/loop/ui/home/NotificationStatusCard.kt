package com.pnd.android.loop.ui.home

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.NotificationsOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.alarm.notification.markNotificationPermissionRequested
import com.pnd.android.loop.ui.settings.NotificationFixAction
import com.pnd.android.loop.ui.settings.NotificationIssue
import com.pnd.android.loop.ui.settings.openNotificationSettings
import com.pnd.android.loop.ui.settings.rememberNotificationStatus
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.ui.theme.warning

/**
 * 알림이 동작하지 못하는 상태를 홈 목록 맨 위에 알리는 배너. 문제가 없으면 아무것도 그리지 않는다.
 *
 * 다이얼로그가 아니라 배너인 이유:
 *  - 다이얼로그는 흐름을 끊고, 한 번 닫으면 원인이 다시 감춰진다.
 *  - 배너는 "지금 앱이 반쪽 상태"라는 사실을 계속 보여주고, 권한이 생기면 스스로 사라진다.
 *    상태와 화면이 항상 일치하므로 사용자가 무엇을 고쳤는지 즉시 확인할 수 있다.
 *
 * 카드 모양은 [DisabledLoopsCard] 와 같은 톤(surfaceElevated + 헤어라인)을 따르되, 색만
 * 주의(warning)로 바꿔 목록의 다른 카드와 구별되게 한다.
 */
@Composable
fun NotificationStatusCard(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val status by rememberNotificationStatus()
    val issue = status.issue ?: return

    // 권한 다이얼로그를 띄울 수 있는 경우엔 설정 화면으로 보내지 않고 바로 물어본다. 결과는
    // 화면이 다시 보일 때 rememberNotificationStatus 가 알아서 다시 판정하므로 여기서 쓰지 않는다.
    val requestPermission = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { }

    NotificationStatusCardContent(
        modifier = modifier,
        issue = issue,
        onFix = {
            val canAskDirectly = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    issue == NotificationIssue.NOTIFICATION_DISABLED &&
                    status.fixAction == NotificationFixAction.REQUEST_PERMISSION

            if (canAskDirectly) {
                context.markNotificationPermissionRequested()
                requestPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                context.openNotificationSettings(issue)
            }
        },
    )
}

@Composable
private fun NotificationStatusCardContent(
    issue: NotificationIssue,
    onFix: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(LoopCardShape)
            .background(color = AppColor.surfaceElevated)
            .border(
                width = 1.dp,
                color = AppColor.warning.copy(alpha = 0.35f),
                shape = LoopCardShape,
            )
            .clickable(onClick = onFix)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(color = AppColor.warning.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(20.dp),
                imageVector = Icons.Rounded.NotificationsOff,
                tint = AppColor.warning,
                contentDescription = null,
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(id = issue.titleResId()),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = stringResource(id = issue.descriptionResId()),
                style = AppTypography.labelMedium.copy(
                    color = AppColor.onSurface.copy(alpha = 0.6f),
                ),
            )
        }

        Text(
            text = stringResource(id = R.string.notification_issue_action),
            style = AppTypography.labelLarge.copy(
                color = AppColor.warning,
                fontWeight = FontWeight.SemiBold,
            ),
        )
    }
}

private fun NotificationIssue.titleResId() = when (this) {
    NotificationIssue.NOTIFICATION_DISABLED -> R.string.notification_issue_disabled_title
    NotificationIssue.CHANNEL_DISABLED -> R.string.notification_issue_channel_title
    NotificationIssue.EXACT_ALARM_DENIED -> R.string.notification_issue_exact_alarm_title
}

private fun NotificationIssue.descriptionResId() = when (this) {
    NotificationIssue.NOTIFICATION_DISABLED -> R.string.notification_issue_disabled_desc
    NotificationIssue.CHANNEL_DISABLED -> R.string.notification_issue_channel_desc
    NotificationIssue.EXACT_ALARM_DENIED -> R.string.notification_issue_exact_alarm_desc
}
