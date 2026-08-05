package com.pnd.android.loop.ui.settings

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pnd.android.loop.alarm.notification.CHANNEL_ID_LOOP_STARTED
import com.pnd.android.loop.alarm.notification.isNotificationPermissionRequested
import com.pnd.android.loop.common.log
import com.pnd.android.loop.ui.findActivity

private val logger = log("NotificationStatus")

/**
 * 알림이 제대로 동작하지 못하게 막고 있는 문제. 가장 치명적인 것 하나만 사용자에게 보여준다
 * (경고를 여러 개 쌓아 두면 무엇부터 해야 하는지 알 수 없다).
 */
enum class NotificationIssue {
    /** 앱 알림 자체가 꺼져 있다(권한 거부 또는 시스템 설정에서 끔). 알림이 하나도 오지 않는다. */
    NOTIFICATION_DISABLED,

    /**
     * 앱 알림은 허용됐지만 개별 채널이 꺼져 있다.
     *
     * 사용자가 알림을 길게 눌러 "이 알림 끄기"를 하면 이 상태가 된다. 앱 권한은 그대로 허용이라
     * areNotificationsEnabled() 는 true 를 돌려주므로, 따로 보지 않으면 놓치는 경우다.
     */
    CHANNEL_DISABLED,

    /** 정확 알람이 막혀 있다. 알림은 오지만 몇 분 늦을 수 있다. */
    EXACT_ALARM_DENIED,
}

/**
 * [NotificationIssue.NOTIFICATION_DISABLED] 를 어떻게 되돌릴 수 있는지.
 *
 * shouldShowRequestPermissionRationale 만으로는 "아직 안 물어봄"과 "영구 거부"를 구분할 수 없어
 * (둘 다 false), 요청한 적이 있는지를 함께 봐야 한다. 이 구분이 없으면 영구 거부한 사용자는
 * 아무리 눌러도 시스템 다이얼로그가 뜨지 않아 앱 안에서 되돌릴 방법이 없다.
 */
enum class NotificationFixAction {
    /** 시스템 권한 다이얼로그를 띄울 수 있다. */
    REQUEST_PERMISSION,

    /** 다이얼로그가 더 뜨지 않는다. 시스템 설정 화면으로 보내야 한다. */
    OPEN_SETTINGS,
}

/** 지금 알림 상태에 문제가 있는지, 있다면 어떻게 되돌릴 수 있는지. */
data class NotificationStatus(
    val issue: NotificationIssue?,
    val fixAction: NotificationFixAction,
) {
    val hasIssue get() = issue != null
}

/**
 * 화면이 다시 보일 때마다 알림 상태를 다시 판정한다.
 *
 * 사용자가 시스템 설정에 갔다 돌아오면 즉시 반영돼야 하므로 ON_RESUME 마다 갱신한다. 그래서
 * 문제가 해결되는 순간 배너가 스스로 사라지고, 상태와 화면이 어긋나지 않는다.
 */
@Composable
fun rememberNotificationStatus(): State<NotificationStatus> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val status = remember { mutableStateOf(context.notificationStatus()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                status.value = context.notificationStatus()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    return status
}

/**
 * 지금 이 순간의 알림 상태.
 *
 * 문제를 심각한 순서로 본다. 알림이 아예 안 오는 것 > 일부 알림만 안 오는 것 > 늦게 오는 것.
 */
fun Context.notificationStatus(): NotificationStatus {
    val issue = when {
        !areNotificationsAllowed() -> NotificationIssue.NOTIFICATION_DISABLED
        isAnnouncementChannelMuted() -> NotificationIssue.CHANNEL_DISABLED
        !canScheduleExactAlarms() -> NotificationIssue.EXACT_ALARM_DENIED
        else -> null
    }
    return NotificationStatus(issue = issue, fixAction = notificationFixAction())
}

/** 앱 알림이 허용된 상태인지. 권한(API 33+)과 시스템 설정의 앱 알림 스위치를 함께 본다. */
private fun Context.areNotificationsAllowed(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val granted = ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted) return false
    }
    return notificationManager().areNotificationsEnabled()
}

/**
 * 시작 안내 채널이 꺼져 있는지. 상시 알림 채널은 포그라운드 서비스가 소유해 꺼도 표시되므로
 * 사용자가 체감하는 문제는 heads-up 채널에서 생긴다.
 */
private fun Context.isAnnouncementChannelMuted(): Boolean {
    val channel = notificationManager().getNotificationChannel(CHANNEL_ID_LOOP_STARTED)
    // 채널이 아직 없으면(앱 첫 실행 직전) 꺼진 것이 아니다.
    return channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE
}

private fun Context.canScheduleExactAlarms(): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
    return getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
}

/** 알림 권한을 되돌릴 방법. 판정 근거는 [NotificationFixAction] 설명 참고. */
private fun Context.notificationFixAction(): NotificationFixAction {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
        // 런타임 권한이 없던 시절의 기기. 앱 알림 스위치는 시스템 설정에서만 켤 수 있다.
        return NotificationFixAction.OPEN_SETTINGS
    }

    val activity = findActivity() ?: return NotificationFixAction.OPEN_SETTINGS
    return when {
        activity.shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS) ->
            NotificationFixAction.REQUEST_PERMISSION

        // rationale=false 는 "아직 안 물어봄"과 "영구 거부" 둘 다다. 물어본 적이 없다면 아직
        // 다이얼로그를 띄울 수 있고, 물어봤는데도 거부 상태라면 다이얼로그는 더 뜨지 않는다.
        isNotificationPermissionRequested() -> NotificationFixAction.OPEN_SETTINGS

        else -> NotificationFixAction.REQUEST_PERMISSION
    }
}

/** 알림 상태를 되돌릴 수 있는 시스템 설정 화면을 연다. */
fun Context.openNotificationSettings(issue: NotificationIssue?) {
    val intent = when (issue) {
        NotificationIssue.CHANNEL_DISABLED -> Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            .putExtra(Settings.EXTRA_CHANNEL_ID, CHANNEL_ID_LOOP_STARTED)

        NotificationIssue.EXACT_ALARM_DENIED -> exactAlarmSettingsIntent()

        // 권한 거부·앱 알림 꺼짐, 그리고 설정 화면에서 직접 열었을 때(issue == null).
        else -> Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
    }
    startSettings(intent)
}

/** 앱 알림 설정 화면(채널 목록)을 연다. 설정 화면에서 "시스템 알림 설정 열기"에 쓴다. */
fun Context.openAppNotificationSettings() = openNotificationSettings(issue = null)

private fun Context.exactAlarmSettingsIntent(): Intent =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM)
            .setData(Uri.fromParts("package", packageName, null))
    } else {
        appDetailsSettingsIntent()
    }

private fun Context.appDetailsSettingsIntent(): Intent =
    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        .setData(Uri.fromParts("package", packageName, null))

/**
 * 설정 화면으로 이동한다. 기기·OEM 에 따라 특정 설정 화면이 없을 수 있어, 실패하면 앱 정보
 * 화면으로 물러난다. 그마저 없으면 조용히 포기한다(사용자를 크래시로 벌줄 이유가 없다).
 */
private fun Context.startSettings(intent: Intent) {
    val activity = findActivity()
    // 액티비티 컨텍스트가 아니면 새 태스크로 띄워야 한다.
    if (activity == null) intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

    try {
        startActivity(intent)
    } catch (e: Exception) {
        logger.e { "failed to open settings: ${e.message}" }
        try {
            startActivity(appDetailsSettingsIntent().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        } catch (e: Exception) {
            logger.e { "failed to open app details: ${e.message}" }
        }
    }
}

private fun Context.notificationManager() = getSystemService(NotificationManager::class.java)
