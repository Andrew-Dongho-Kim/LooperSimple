package com.pnd.android.loop.alarm.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pnd.android.loop.appwidget.AppWidgetRefresher
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.currentOccurrence
import com.pnd.android.loop.util.elapsedMinutesSinceStart
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import javax.inject.Inject
import kotlin.time.Duration.Companion.milliseconds

private val logger = log("LoopForegroundService")

/**
 * 진행 중인 루프를 알림창에 상시 표시하는 포그라운드 서비스.
 *
 * - 앱을 실행하지 않아도(백그라운드/프로세스 종료 후 알람으로 깨어나도) 통합 알림을 유지한다.
 * - 포그라운드 서비스가 소유한 알림이라 사용자가 스와이프로 지울 수 없다.
 * - 1분마다 스스로 DB를 다시 읽어 남은 시간을 갱신하고, 진행 중인 루프가 하나도
 *   없어지면 알림을 내리고 서비스를 종료한다. 즉 별도의 진행 알람 틱이 필요 없다.
 */
@AndroidEntryPoint
class LoopForegroundService : Service() {

    @Inject
    lateinit var appDb: AppDatabase

    @Inject
    lateinit var notificationHelper: NotificationHelper

    @Inject
    lateinit var notificationSettings: NotificationSettings

    @Inject
    lateinit var appWidgetRefresher: AppWidgetRefresher

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    /**
     * 이미 포그라운드로 승격돼 있는지. refresh() 는 서비스가 실행 중이어도 onStartCommand 를
     * 다시 태우는데, 그때마다 placeholder 로 승격하면 알림이 "앱 이름만" 있는 상태로 깜빡인다.
     */
    private var isForeground = false

    /** 마지막 start 요청의 id. 종료 직전에 새 요청이 도착했는지 판별하는 데 쓴다. */
    private var latestStartId = 0

    /**
     * anytime 루프별로 마지막 "진행 중" 안내를 띄운 경과 시간(시). 1시간 단위로 한 번만
     * 알리기 위한 표시다. 서비스가 사는 동안만 유지하면 되고(진행 중 루프가 없으면 서비스도
     * 없다), 프로세스가 재시작되면 다음 틱에 한 번 더 알릴 수 있으나 무해하다.
     */
    private val remindedHours = mutableMapOf<Int, Int>()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        latestStartId = startId
        // 반드시 시작 후 5초 이내에 startForeground를 호출해야 한다. 최초에는 DB를 읽기 전이라
        // placeholder 알림으로 즉시 승격하고, 곧이어 실제 내용으로 갱신한다.
        if (!promoteToForeground()) return START_NOT_STICKY
        restartTicks()
        return START_STICKY
    }

    /** 포그라운드 승격에 성공했는지(또는 이미 승격돼 있는지) 반환한다. */
    private fun promoteToForeground(): Boolean {
        // 이미 알림을 소유하고 있으면 placeholder 로 덮어쓰지 않는다. 갱신은 틱이 맡는다.
        if (isForeground) return true

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        return try {
            logger.i { "Start the foreground service withType:$type" }
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notificationHelper.buildOngoingNotification(emptyList()),
                type,
            )
            isForeground = true
            true
        } catch (e: Exception) {
            // startForeground 를 끝내 호출하지 못하면, 시스템이 5초 뒤 "did not then call
            // Service.startForeground()" 로 프로세스를 죽인다. 삼키지 말고 스스로 물러난다.
            logger.e { "failed to start foreground: ${e.message}" }
            stopSelf()
            false
        }
    }

    private fun restartTicks() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val loops = queryActiveLoops()
                if (loops.isEmpty()) {
                    // 종료 판단은 메인 스레드에서 확정한다. onStartCommand 와 같은 스레드라
                    // "종료를 정한 직후 도착한 refresh" 와 뒤섞이지 않는다. 그 사이 이 잡이
                    // 취소됐다면 withContext 가 취소로 끝나 종료 자체를 건너뛴다.
                    withContext(Dispatchers.Main) { stopSelfAndForeground() }
                    break
                }
                loops.forEach { loop ->
                    logger.i { " - Active tickets: $loop" }
                }
                notificationHelper.updateOngoing(loops)
                remindLongRunningAnyTimeLoops(loops)
                refreshAppWidget()
                delay(delayToNextMinute().milliseconds)
            }
        }
    }

    /**
     * 위젯도 알림과 같은 시각 기준의 문구("32분 남음")를 쓴다. 알림만 갱신하면 위젯은 루프가 끝날
     * 때까지 시작 시점의 문구로 멈춰 있으므로, 1분마다 깨어 있는 이 자리에서 함께 다시 그린다.
     * 워커를 새로 띄우지 않고 바로 부르고, 위젯이 하나도 없으면 refresh 가 곧바로 빠져나온다.
     *
     * 실패하더라도 알림 틱은 계속 돌아야 하므로 예외는 여기서 삼킨다. 그대로 새어 나가면 이 잡이
     * 끝나 알림이 다음 트리거까지 멈추고, 처리되지 않은 예외로 앱이 죽는다.
     */
    private suspend fun refreshAppWidget() {
        try {
            appWidgetRefresher.refresh()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.e { "failed to refresh the app widget: ${e.message}" }
        }
    }

    /**
     * 지금 이 순간 알림에 보여줄 루프: 활성화 + 오늘 요일 + 진행 시간 + 아직 미응답.
     *
     * 자정을 넘겨 이어지는 루프는 done 기록이 "어제" 행에 있어서, 오늘 날짜로만 조인하면
     * 이미 완료한 루프를 계속 미응답으로 보게 된다([currentOccurrence] 참고). anytime 루프도
     * 어제의 IN_PROGRESS 기록이 잡히지 않아 자정에 알림이 사라져 버린다. 두 날짜를 함께 읽어
     * occurrence 기준 행으로 판정한다.
     */
    private suspend fun queryActiveLoops(): List<LoopBase> {
        val dao = appDb.fullLoopDao()
        val today = LocalDate.now()
        val yesterdayLoops = dao.getAllLoops(today.minusDays(1).toMs()).associateBy { it.loopId }

        return dao.getAllLoops(today.toMs())
            .map { loop -> currentOccurrence(today = loop, yesterday = yesterdayLoops[loop.loopId]) }
            .filter { loop -> loop.isActive() && !loop.isRespond && !loop.isDisabled }
    }

    /**
     * anytime 루프는 종료 시각이 없어 사용자가 정지하지 않으면 며칠이고 진행 중으로 남는다.
     * 무음 상시 알림만으로는 눈에 띄지 않으므로, 설정한 간격마다 heads-up 으로 다시 알린다.
     */
    private fun remindLongRunningAnyTimeLoops(loops: List<LoopBase>) {
        remindedHours.keys.retainAll(loops.map { it.loopId }.toSet())

        val intervalHours = notificationSettings.current.inProgressRemindIntervalHours
        if (intervalHours == IN_PROGRESS_REMIND_OFF) return

        val due = loops.filter { loop ->
            if (!loop.isAnyTime) return@filter false
            val hours = (loop.elapsedMinutesSinceStart() ?: return@filter false) / 60
            // 간격의 배수가 되는 시점에만 알린다. 예) 2시간 간격이면 2·4·6시간째.
            if (hours < intervalHours || hours % intervalHours != 0) return@filter false
            remindedHours[loop.loopId] != hours
        }
        if (due.isEmpty()) return

        due.forEach { loop ->
            remindedHours[loop.loopId] = (loop.elapsedMinutesSinceStart() ?: 0) / 60
        }
        logger.i { "remind ${due.size} long-running anytime loop(s)" }
        notificationHelper.notifyLoopsInProgress(due)
    }

    private fun stopSelfAndForeground() {
        // startId 를 넘겨, 종료를 정한 뒤 새 refresh 가 도착했다면 종료를 건너뛴다. 그대로
        // stopSelf() 하면 방금 시작된 루프의 알림이 사라진 채 다음 트리거까지 돌아오지 않는다.
        if (!stopSelfResult(latestStartId)) {
            logger.i { "a newer start request arrived - keep the ongoing notification" }
            return
        }
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        isForeground = false
    }

    /** 벽시계 분 경계에 맞춰 갱신되도록 다음 분까지 남은 시간을 반환한다. */
    private fun delayToNextMinute(): Long {
        val remainder = System.currentTimeMillis() % MS_1MIN
        return if (remainder == 0L) MS_1MIN else MS_1MIN - remainder
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
        isForeground = false
        super.onDestroy()
    }

    companion object {
        /**
         * 진행 중 루프 알림을 최신 상태로 만든다. 서비스가 실행 중이 아니면 시작하고,
         * 이미 실행 중이면 즉시 다시 DB를 읽어 내용을 갱신한다. 진행 중인 루프가 없으면
         * 서비스가 스스로 알림을 내리고 종료한다.
         */
        fun refresh(context: Context) {
            val intent = Intent(context, LoopForegroundService::class.java)
            try {
                logger.i { "Request to start the loop foreground service" }
                ContextCompat.startForegroundService(context, intent)
            } catch (e: Exception) {
                // Android 12+ 백그라운드 시작 제한 등으로 실패할 수 있다. 이 경우 다음
                // 알람(시작/동기화)이나 재부팅 시 다시 시도된다.
                logger.e { "failed to start service: ${e.message}" }
            }
        }
    }
}
