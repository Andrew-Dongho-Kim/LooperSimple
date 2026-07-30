package com.pnd.android.loop.alarm.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.util.isActive
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 루프가 막 시작되었을 때 화면 상단에 "○○ 시작" 안내(heads-up)를 띄운다.
 *
 * - 화면이 켜져 있으면: 즉시 안내 알림을 띄운다.
 * - 화면이 꺼져 있으면: 대기 큐에 담아 두었다가 화면이 켜지는 순간(ACTION_SCREEN_ON)
 *   모아서 한 번에 띄운다. heads-up 은 화면이 꺼진 채로 post 하면 peek 되지 않으므로,
 *   화면이 켜지는 시점에 새로 post 해야 사용자가 볼 수 있다.
 *
 * 진행 중 루프가 있는 동안에는 [LoopForegroundService] 가 프로세스를 살려 두므로,
 * 여기서 런타임 등록한 화면 리시버와 대기 큐도 그 사이 유지된다.
 */
@Singleton
class LoopStartAnnouncer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val powerManager: PowerManager,
    private val notificationHelper: NotificationHelper,
) {
    private val logger = log("LoopStartAnnouncer")

    // 화면이 꺼진 사이 시작된 루프들. 같은 루프가 중복되지 않도록 loopId 로 관리하고,
    // 시작 순서를 유지하기 위해 LinkedHashMap 을 쓴다.
    private val pending = LinkedHashMap<Int, LoopBase>()
    private var screenReceiver: BroadcastReceiver? = null

    @Synchronized
    fun announce(loop: LoopBase) {
        if (powerManager.isInteractive) {
            logger.i { "screen on -> announce now: ${loop.title}" }
            notificationHelper.notifyLoopStarted(loop)
        } else {
            logger.i { "screen off -> defer: ${loop.title}" }
            pending[loop.loopId] = loop
            ensureScreenReceiver()
        }
    }

    private fun ensureScreenReceiver() {
        if (screenReceiver != null) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_ON) flushPending()
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        screenReceiver = receiver
    }

    @Synchronized
    private fun flushPending() {
        if (pending.isNotEmpty()) {
            // 화면이 꺼진 사이 이미 종료된(시간 창을 벗어난) 루프는 빼고, 지금도 진행 중인
            // 루프만 "진행 중" + 시간 정보로 알린다. isActive() 는 현재 시각 기준이라
            // 스냅샷만으로도 종료 여부를 정확히 판정한다(시작 알람은 시간제 루프에만 발생).
            val stillRunning = pending.values.filter { it.isActive() }
            logger.i { "screen turned on -> ${stillRunning.size}/${pending.size} still running" }
            if (stillRunning.isNotEmpty()) {
                notificationHelper.notifyLoopsInProgress(stillRunning)
            }
            pending.clear()
        }
        unregisterScreenReceiver()
    }

    private fun unregisterScreenReceiver() {
        screenReceiver?.let { receiver ->
            try {
                context.unregisterReceiver(receiver)
            } catch (e: IllegalArgumentException) {
                // 이미 해제된 경우. 무시한다.
            }
        }
        screenReceiver = null
    }
}
