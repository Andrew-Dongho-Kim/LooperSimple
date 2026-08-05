package com.pnd.android.loop.alarm.notification

import com.pnd.android.loop.alarm.HabitualStartEstimator
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isInProgress
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.util.currentOccurrence
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 습관 시각이 됐는데도 아직 시작하지 않은 anytime 루프에 "시작할까요?" 를 물어본다.
 *
 * anytime 루프는 시작하기 전까지 어떤 알림에도 등장하지 않는다. 상시 알림은 "진행 중"인 루프만
 * 보여주고(anytime 루프는 시작해야 진행 중이 된다), 위젯을 쓰지 않는 사용자는 앱을 직접 열어야
 * 비로소 오늘 할 일을 본다. 그래서 하루 한 번, 평소 하던 시각에 [시작] 버튼과 함께 알린다.
 *
 * 시작을 누르면 진행 중으로 기록되고 → 상시 알림에 등록되고 → 오래 방치되면 1시간마다 다시
 * 알리는 기존 흐름으로 자연히 이어진다.
 */
@Singleton
class AnyTimeStartPrompter @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val habitualStartEstimator: HabitualStartEstimator,
    private val notificationSettings: NotificationSettings,
    private val appDb: AppDatabase,
) {
    private val logger = log("AnyTimeStartPrompter")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 습관 시각 알람이 도착한 루프. 오늘 아직 시작하지 않았다면 안내 알림을 띄운다. */
    fun prompt(loopId: Int) {
        // 설정에서 끈 뒤에도 이미 걸려 있던 알람은 도착할 수 있다(앱 업데이트 전에 예약된 것 등).
        // 예약을 취소하는 것과 별개로, 발화 시점에도 한 번 더 확인해 설정이 항상 이기게 한다.
        if (!notificationSettings.current.anyTimeDueEnabled) {
            logger.i { "anytime due notification is turned off" }
            return
        }

        scope.launch {
            val loop = queryNotStartedYet(loopId)
            if (loop == null) {
                logger.i { "loop:$loopId needs no start prompt" }
                return@launch
            }

            logger.i { "anytime loop not started yet -> ask: ${loop.title}" }
            notificationHelper.notifyAnyTimeLoopDue(
                loop = loop,
                // 알람을 예약할 때와 같은 계산을 다시 돌려, 알림에 적는 "보통 09:30" 이 실제로
                // 알람이 걸린 시각과 어긋나지 않게 한다.
                habitualStart = habitualStartEstimator.estimate(loopId),
            )
        }
    }

    /**
     * 오늘 아직 시작하지 않은 anytime 루프면 그 루프를, 아니면 null.
     *
     * 알람 인텐트로 넘어온 루프 스냅샷에는 done 기록이 없어 시작 여부를 알 수 없으므로 DB를 다시
     * 읽는다. 어제 시작해 자정을 넘겨 아직 진행 중인 루프는 done 기록이 "어제" 행에 있으므로
     * 두 날짜를 함께 읽어 occurrence 기준 행으로 판정한다([currentOccurrence] 참고).
     */
    private suspend fun queryNotStartedYet(loopId: Int): LoopBase? {
        val dao = appDb.fullLoopDao()
        val today = LocalDate.now()
        val yesterdayLoops = dao.getAllLoops(today.minusDays(1).toMs()).associateBy { it.loopId }

        val loop = dao.getAllLoops(today.toMs())
            .firstOrNull { loop -> loop.loopId == loopId }
            ?.let { loop -> currentOccurrence(today = loop, yesterday = yesterdayLoops[loop.loopId]) }
            ?: return null

        // 시간제 루프에는 사용자가 정한 시작 시각이 있어 시작 알람이 따로 있다(방어적으로 제외).
        if (!loop.isAnyTime) return null
        if (!loop.enabled) return null
        // 오늘이 그 루프의 요일이 아니면 애초에 할 일이 없다.
        if (!loop.isActiveDay()) return null
        // 이미 시작해 진행 중이거나(어제 시작해 이어지는 것도 포함), 이미 답한 루프는 묻지 않는다.
        if (loop.isInProgress) return null
        if (loop.isRespond || loop.isDisabled) return null

        return loop
    }
}
