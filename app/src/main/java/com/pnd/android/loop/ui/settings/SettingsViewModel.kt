package com.pnd.android.loop.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pnd.android.loop.alarm.HabitualStartShift
import com.pnd.android.loop.alarm.LoopScheduler
import com.pnd.android.loop.alarm.notification.NotificationPreferences
import com.pnd.android.loop.alarm.notification.NotificationSettings
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 알림 설정 화면의 상태.
 *
 * 설정을 바꾸면 곧바로 [LoopScheduler.syncLoops] 를 다시 돌린다. 습관 알림 사용 여부나 시각
 * 보정은 이미 예약된 알람의 시각을 바꿔야 하는데, 그 재계산은 syncLoops 가 담당하기 때문이다.
 * 다시 돌리지 않으면 오늘 하루는 예전 설정대로 알림이 온다.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val notificationSettings: NotificationSettings,
    private val loopScheduler: LoopScheduler,
) : ViewModel() {

    val preferences: StateFlow<NotificationPreferences> = notificationSettings.preferences

    /** 디바운스 중인 재예약 작업. 새 변경이 들어오면 취소하고 다시 기다린다. */
    private var syncJob: Job? = null

    fun setAnyTimeDueEnabled(enabled: Boolean) = update { it.copy(anyTimeDueEnabled = enabled) }

    fun setHabitualStartShift(shift: HabitualStartShift) =
        update { it.copy(habitualStartShift = shift) }

    fun setQuietHoursEnabled(enabled: Boolean) = update { it.copy(quietHoursEnabled = enabled) }

    fun setQuietHours(startHour: Int, endHour: Int) =
        update { it.copy(quietHoursStartHour = startHour, quietHoursEndHour = endHour) }

    fun setInProgressRemindIntervalHours(hours: Int) =
        update { it.copy(inProgressRemindIntervalHours = hours) }

    /**
     * 설정을 저장하고 알람을 다시 예약한다.
     *
     * 저장은 즉시 하되 재예약은 조금 모은다. 시간 스테퍼는 한 번 맞추는 데 여러 번 눌리므로,
     * 탭마다 전체 루프를 재예약하면 같은 일이 병렬로 여러 번 돈다. 디바운스가 끝나기 전에
     * 화면을 떠났다면 [onCleared] 에서 흘려보내, 마지막 변경이 반영되지 않는 일은 없게 한다.
     */
    private fun update(transform: (NotificationPreferences) -> NotificationPreferences) {
        notificationSettings.update(transform)

        syncJob?.cancel()
        syncJob = viewModelScope.launch {
            delay(SYNC_DEBOUNCE_MS)
            loopScheduler.syncLoops()
            syncJob = null
        }
    }

    override fun onCleared() {
        // 아직 기다리는 중이었다면 지금 돌린다. syncLoops 는 자체 스코프에서 동작하므로
        // viewModelScope 가 정리된 뒤에도 끝까지 진행된다.
        if (syncJob?.isActive == true) {
            syncJob?.cancel()
            loopScheduler.syncLoops()
        }
        super.onCleared()
    }
}

/** 설정 변경이 연달아 들어올 때 재예약을 모으는 시간(ms). */
private const val SYNC_DEBOUNCE_MS = 400L
