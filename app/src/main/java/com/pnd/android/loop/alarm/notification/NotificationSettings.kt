package com.pnd.android.loop.alarm.notification

import android.content.Context
import android.content.SharedPreferences
import com.pnd.android.loop.alarm.HabitualStartShift
import com.pnd.android.loop.util.MS_1HOUR
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val PREF_NAME = "notification_settings"

private const val KEY_ANYTIME_DUE_ENABLED = "anytime_due_enabled"
private const val KEY_HABITUAL_SHIFT = "habitual_shift"
private const val KEY_QUIET_HOURS_ENABLED = "quiet_hours_enabled"
private const val KEY_QUIET_HOURS_START = "quiet_hours_start"
private const val KEY_QUIET_HOURS_END = "quiet_hours_end"
private const val KEY_IN_PROGRESS_REMIND_HOURS = "in_progress_remind_hours"
private const val KEY_NOTIFICATION_PERMISSION_REQUESTED = "notification_permission_requested"

/** 진행 중 리마인드를 끈 상태를 나타내는 간격 값. */
const val IN_PROGRESS_REMIND_OFF = 0

/**
 * 알림 권한을 한 번이라도 요청한 적이 있는지. 사용자 설정이 아니라 앱의 내부 상태다.
 *
 * shouldShowRequestPermissionRationale 은 "아직 안 물어봄"과 "영구 거부"에 똑같이 false 를
 * 돌려주므로, 이 플래그가 없으면 두 경우를 구분할 수 없다(전자는 다시 물어야 하고, 후자는
 * 시스템 설정으로 보내야 한다).
 *
 * 권한 상태를 판정하는 쪽은 Hilt 주입 없이 Context 만 들고 있어서, [NotificationSettings] 의
 * 멤버가 아니라 top-level 로 둔다. 저장 파일과 키는 같으므로 값은 하나다.
 */
fun Context.isNotificationPermissionRequested(): Boolean =
    notificationPrefs().getBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, false)

/** 알림 권한을 요청했다고 기록한다. 시스템 다이얼로그를 띄운 직후에 호출한다. */
fun Context.markNotificationPermissionRequested() {
    notificationPrefs().edit().putBoolean(KEY_NOTIFICATION_PERMISSION_REQUESTED, true).apply()
}

private fun Context.notificationPrefs(): SharedPreferences =
    getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

/**
 * 사용자가 고를 수 있는 진행 중 리마인드 간격(시간). 0은 "끔".
 * anytime 루프는 종료 시각이 없어 정지하지 않으면 며칠이고 진행 중으로 남으므로 이 알림이 필요하다.
 */
val IN_PROGRESS_REMIND_CHOICES = listOf(IN_PROGRESS_REMIND_OFF, 1, 2, 3)

/**
 * 알림 관련 사용자 설정의 스냅샷.
 *
 * 값을 하나씩 읽는 대신 이 묶음을 한 번에 넘겨, 알람을 예약하는 시점과 알림을 만드는 시점이
 * 서로 다른 설정을 보는 일이 없게 한다.
 */
data class NotificationPreferences(
    /** anytime 루프에 "시작할까요?" 습관 알림을 보낼지. */
    val anyTimeDueEnabled: Boolean,
    /** 습관 알림 시각을 평소보다 이르게/늦게 당길지. */
    val habitualStartShift: HabitualStartShift,
    /** 방해 금지 시간대를 쓸지. */
    val quietHoursEnabled: Boolean,
    /** 방해 금지 시작/종료 시(0~23). 시작이 종료보다 크면 자정을 넘는 구간으로 본다. */
    val quietHoursStartHour: Int,
    val quietHoursEndHour: Int,
    /** 진행 중인 anytime 루프를 다시 알리는 간격(시간). [IN_PROGRESS_REMIND_OFF] 면 알리지 않는다. */
    val inProgressRemindIntervalHours: Int,
) {

    /**
     * 하루 안의 시각 [msInDay] 가 방해 금지 구간에 들어가는지. 자정을 넘는 구간(예: 23시~6시)도
     * 처리한다. 구간의 끝 시각은 포함하지 않는다(6시는 이미 깨어 있는 시각이다).
     *
     * 이 판정은 사용자가 직접 정하지 않은 알림, 즉 습관 시각을 추정해 보내는 알림에만 쓴다.
     * 루프 시작·종료처럼 사용자가 스스로 그 시각을 고른 알림까지 막으면 앱이 약속을 어기는 셈이다.
     */
    fun isInQuietHours(msInDay: Long): Boolean {
        if (!quietHoursEnabled) return false
        if (quietHoursStartHour == quietHoursEndHour) return false

        val start = quietHoursStartHour * MS_1HOUR
        val end = quietHoursEndHour * MS_1HOUR
        return if (start < end) {
            msInDay in start until end
        } else {
            msInDay >= start || msInDay < end
        }
    }
}

/**
 * 알림 관련 사용자 설정.
 *
 * 안드로이드 알림 채널 설정으로 할 수 있는 것(켜기/끄기, 소리, 진동, 중요도, 방해금지 예외)은
 * 여기 두지 않는다. 시스템 설정 화면이 이미 그 역할을 하고, 앱이 흉내내면 두 곳이 어긋난다.
 * 그래서 채널을 성격별로 쪼개 두고([CHANNEL_ID_ONGOING] 등), 여기에는 시스템이 대신할 수 없는
 * 값만 남긴다.
 *
 * 저장소는 SharedPreferences 다. 이 값들을 읽는 쪽이 알람 예약(LoopScheduler)과 알림 생성
 * (NotificationHelper)처럼 동기 컨텍스트라, suspend 로 읽어야 하는 DataStore 는 맞지 않는다.
 * 앱의 다른 설정들도 이미 SharedPreferences 를 쓰고 있어 패턴도 일치한다.
 */
@Singleton
class NotificationSettings @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    private val _preferences = MutableStateFlow(load())

    /** 설정 화면이 구독하는 값. */
    val preferences: StateFlow<NotificationPreferences> = _preferences.asStateFlow()

    /** 알람·알림 쪽에서 동기로 읽는 현재 값. */
    val current: NotificationPreferences get() = _preferences.value

    /** 설정 하나를 바꾼다. 저장과 구독자 통지를 함께 처리한다. */
    fun update(transform: (NotificationPreferences) -> NotificationPreferences) {
        val updated = transform(current)
        save(updated)
        _preferences.value = updated
    }

    private fun load() = NotificationPreferences(
        anyTimeDueEnabled = prefs.getBoolean(KEY_ANYTIME_DUE_ENABLED, true),
        habitualStartShift = HabitualStartShift.ofOrdinal(
            prefs.getInt(KEY_HABITUAL_SHIFT, HabitualStartShift.USUAL.ordinal)
        ),
        quietHoursEnabled = prefs.getBoolean(KEY_QUIET_HOURS_ENABLED, true),
        quietHoursStartHour = prefs.getInt(KEY_QUIET_HOURS_START, DEFAULT_QUIET_START_HOUR),
        quietHoursEndHour = prefs.getInt(KEY_QUIET_HOURS_END, DEFAULT_QUIET_END_HOUR),
        inProgressRemindIntervalHours = prefs.getInt(KEY_IN_PROGRESS_REMIND_HOURS, 1),
    )

    private fun save(preferences: NotificationPreferences) {
        prefs.edit()
            .putBoolean(KEY_ANYTIME_DUE_ENABLED, preferences.anyTimeDueEnabled)
            .putInt(KEY_HABITUAL_SHIFT, preferences.habitualStartShift.ordinal)
            .putBoolean(KEY_QUIET_HOURS_ENABLED, preferences.quietHoursEnabled)
            .putInt(KEY_QUIET_HOURS_START, preferences.quietHoursStartHour)
            .putInt(KEY_QUIET_HOURS_END, preferences.quietHoursEndHour)
            .putInt(KEY_IN_PROGRESS_REMIND_HOURS, preferences.inProgressRemindIntervalHours)
            .apply()
    }

    companion object {
        /** 기본 방해 금지 구간: 새벽 1시~5시. 대부분 자고 있고, 습관 알림이 필요할 시각도 아니다. */
        const val DEFAULT_QUIET_START_HOUR = 1
        const val DEFAULT_QUIET_END_HOUR = 5
    }
}
