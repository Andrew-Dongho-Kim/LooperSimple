package com.pnd.android.loop.alarm.notification

import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.util.currentOccurrence
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.occurrenceStartDate
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/**
 * occurrence 판정을 종료 시각보다 이만큼 앞선 시점에서 한다. 종료 시각이 지난 뒤에 보면 자정을
 * 넘기는 루프(예: 23:00~01:00)의 occurrence 가 오늘로 잡혀([occurrenceStartDate] 는 종료
 * 전까지만 어제로 본다), 정작 판정하고 기록해야 할 어제 몫을 놓친다.
 */
private const val OCCURRENCE_LOOKBACK_MINUTES = 1L

/**
 * 루프의 시간창이 끝났을 때, 아직 응답이 없다면 "완료했나요?" 를 물어본다.
 *
 * 종료 시각이 지나면 루프는 더 이상 진행 중이 아니라 상시 알림에서 조용히 사라지고, 그대로
 * 미응답(NO_RESPONSE)으로 굳는다. 사용자가 알아차리려면 앱을 열어 어제 카드를 찾아 소급
 * 입력해야 했다. 종료 직후 한 번 물어보면 그 자리에서 [완료]/[건너뛰기]로 끝낼 수 있다.
 *
 * 화면이 꺼져 있어도 곧바로 띄운다([LoopStartAnnouncer] 처럼 화면이 켜질 때까지 미루지
 * 않는다). 미루면 "언제 답했는지" 와 "어느 날 몫인지" 가 더 크게 벌어지기만 하고, 알림은
 * 어차피 잠금화면·알림창에 남아 다음에 폰을 볼 때 보이기 때문이다. 기록할 날짜는 지금 정해
 * 알림에 실어 두므로, 잠시 뒤에 답해도 방금 끝난 occurrence 에 남는다.
 */
@Singleton
class LoopEndPrompter @Inject constructor(
    private val notificationHelper: NotificationHelper,
    private val appDb: AppDatabase,
) {
    private val logger = log("LoopEndPrompter")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** 종료 알람이 도착한 루프. 지금도 미응답이면 확인 알림을 띄운다. */
    fun prompt(loopId: Int) {
        scope.launch {
            val endedAt = LocalDateTime.now().minusMinutes(OCCURRENCE_LOOKBACK_MINUTES)
            val loop = queryUnresponded(loopId = loopId, endedAt = endedAt)
            if (loop == null) {
                logger.i { "loop:$loopId needs no end prompt" }
                return@launch
            }
            logger.i { "loop ended with no response -> ask: ${loop.title}" }
            notificationHelper.notifyLoopEnded(
                loop = loop,
                // 사용자가 이 알림에서 답하면 방금 끝난 occurrence 에 기록해야 한다. 누르는
                // 시점은 이미 occurrence 밖이라 그때 다시 계산할 수 없으므로 지금 정해서 넘긴다.
                occurrenceDate = loop.occurrenceStartDate(endedAt),
            )
        }
    }

    /**
     * 방금 끝난 occurrence 가 아직 미응답이면 그 루프를, 아니면 null.
     *
     * 알람 인텐트로 넘어온 루프 스냅샷에는 done 기록이 없어 응답 여부를 알 수 없으므로 DB를
     * 다시 읽는다. 자정을 넘겨 이어지는 루프(예: 23:00~01:00)는 done 기록이 "어제" 행에
     * 있으므로 두 날짜를 함께 읽어 occurrence 기준 행으로 판정한다.
     */
    private suspend fun queryUnresponded(loopId: Int, endedAt: LocalDateTime): LoopBase? {
        val dao = appDb.fullLoopDao()
        val today = LocalDate.now()
        val yesterdayLoops = dao.getAllLoops(today.minusDays(1).toMs()).associateBy { it.loopId }

        val loop = dao.getAllLoops(today.toMs())
            .firstOrNull { loop -> loop.loopId == loopId }
            ?.let { loop ->
                currentOccurrence(
                    today = loop,
                    yesterday = yesterdayLoops[loop.loopId],
                    now = endedAt,
                )
            } ?: return null

        // anytime 루프는 종료 시각이 없어 종료 알람도 예약되지 않는다(방어적으로 제외한다).
        if (loop.isAnyTime) return null
        if (!loop.enabled) return null
        // 종료 알람은 활성 요일과 무관하게 도착한다(예약이 요일을 보지 않는다). 오늘이 그 루프의
        // 요일이 아니면 애초에 할 일이 없었으므로 묻지 않는다.
        if (!loop.isActiveDay(loop.occurrenceStartDate(endedAt))) return null
        // 이미 완료/건너뛰기로 답했거나 비활성 처리된 루프는 물을 필요가 없다.
        if (loop.isRespond || loop.isDisabled) return null
        // 알람이 예상보다 일찍 도착해 아직 진행 중이라면, 상시 알림이 이미 응답 버튼과 함께
        // 보여주고 있다. 끝나지도 않은 루프에 "완료했나요?" 를 묻지 않는다.
        if (loop.isActive()) return null

        return loop
    }
}
