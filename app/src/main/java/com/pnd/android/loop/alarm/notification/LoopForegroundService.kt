package com.pnd.android.loop.alarm.notification

import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.isActive
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
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

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var tickJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // 반드시 시작 후 5초 이내에 startForeground를 호출해야 한다. 최초에는 DB를 읽기 전이라
        // placeholder 알림으로 즉시 승격하고, 곧이어 실제 내용으로 갱신한다.
        promoteToForeground()
        restartTicks()
        return START_STICKY
    }

    private fun promoteToForeground() {
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        } else {
            0
        }
        try {
            logger.i { "Start the foreground service withType:$type" }
            ServiceCompat.startForeground(
                this,
                FOREGROUND_NOTIFICATION_ID,
                notificationHelper.buildOngoingNotification(emptyList()),
                type,
            )
        } catch (e: Exception) {
            logger.e { "failed to start foreground: ${e.message}" }
        }
    }

    private fun restartTicks() {
        tickJob?.cancel()
        tickJob = scope.launch {
            while (isActive) {
                val loops = queryActiveLoops()
                if (loops.isEmpty()) {
                    stopSelfAndForeground()
                    break
                }
                loops.forEach { loop ->
                    logger.i { " - Active tickets: $loop" }
                }
                notificationHelper.updateOngoing(loops)
                delay(delayToNextMinute().milliseconds)
            }
        }
    }

    /** 지금 이 순간 알림에 보여줄 루프: 활성화 + 오늘 요일 + 진행 시간 + 아직 미응답 */
    private suspend fun queryActiveLoops(): List<LoopBase> {
        return appDb.fullLoopDao().getAllLoops().filter { loop ->
            loop.isActive()
        }
    }

    private fun stopSelfAndForeground() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** 벽시계 분 경계에 맞춰 갱신되도록 다음 분까지 남은 시간을 반환한다. */
    private fun delayToNextMinute(): Long {
        val remainder = System.currentTimeMillis() % MS_1MIN
        return if (remainder == 0L) MS_1MIN else MS_1MIN - remainder
    }

    override fun onDestroy() {
        tickJob?.cancel()
        scope.cancel()
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
