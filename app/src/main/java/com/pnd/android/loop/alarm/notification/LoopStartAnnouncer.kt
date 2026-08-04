package com.pnd.android.loop.alarm.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.PowerManager
import com.pnd.android.loop.common.log
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.util.currentOccurrence
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.toMs
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers

/**
 * 같은 시각에 시작하는 루프들의 알람이 각각 도착하는 것을 모으는 시간(ms). 안내 알림은
 * ID 하나를 공유하므로 도착하는 대로 띄우면 마지막 것만 남는다. 사람이 못 느낄 만큼만 늦춰
 * 한 번에 묶는다.
 */
private const val ANNOUNCE_BATCH_WINDOW_MS = 700L

/**
 * 루프가 막 시작되었을 때 화면 상단에 "○○ 시작" 안내(heads-up)를 띄운다.
 *
 * - 화면이 켜져 있으면: 짧게 모아(같은 시각 시작 루프들을 한 알림으로) 곧바로 띄운다.
 * - 화면이 꺼져 있으면: 대기 큐에 담아 두었다가 화면이 켜지는 순간(ACTION_SCREEN_ON)
 *   모아서 한 번에 띄운다. heads-up 은 화면이 꺼진 채로 post 하면 peek 되지 않으므로,
 *   화면이 켜지는 시점에 새로 post 해야 사용자가 볼 수 있다.
 *
 * 진행 중 루프가 있는 동안에는 [LoopForegroundService] 가 프로세스를 살려 두므로,
 * 여기서 런타임 등록한 화면 리시버와 대기 큐도 그 사이 유지된다. 다만 진행 중 루프가 모두
 * 끝나 서비스가 종료된 뒤 프로세스가 회수되면 대기 큐는 사라진다. ACTION_SCREEN_ON 은
 * 매니페스트로 등록할 수 없어 근본적인 회피가 불가하고, 그때는 알릴 루프도 이미 끝난
 * 상태이므로 안내를 놓쳐도 실질적인 손실은 없다.
 */
@Singleton
class LoopStartAnnouncer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val powerManager: PowerManager,
    private val notificationHelper: NotificationHelper,
    private val appDb: AppDatabase,
) {
    private val logger = log("LoopStartAnnouncer")

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    // 화면이 꺼진 사이 시작된 루프들의 id. 화면이 켜질 때 DB 를 다시 읽어 판정하므로
    // 루프 스냅샷 대신 id 만 들고 있는다. 시작 순서를 유지하려 LinkedHashSet 을 쓴다.
    private val deferredLoopIds = LinkedHashSet<Int>()
    private var screenReceiver: BroadcastReceiver? = null

    // 화면이 켜져 있을 때 짧게 모아 한 번에 띄우기 위한 버퍼.
    private val batchLoopIds = LinkedHashSet<Int>()
    private var batchJob: Job? = null

    @Synchronized
    fun announce(loop: LoopBase) {
        if (powerManager.isInteractive) {
            logger.i { "screen on -> announce (batched): ${loop.title}" }
            batchLoopIds += loop.loopId
            batchJob?.cancel()
            batchJob = scope.launch {
                delay(ANNOUNCE_BATCH_WINDOW_MS)
                flushBatch()
            }
        } else {
            logger.i { "screen off -> defer: ${loop.title}" }
            deferredLoopIds += loop.loopId
            ensureScreenReceiver()
        }
    }

    /** 짧은 시간 안에 시작한 루프들을 하나의 안내 알림으로 띄운다. */
    @Synchronized
    private fun flushBatch() {
        val loopIds = batchLoopIds.toSet()
        batchLoopIds.clear()
        if (loopIds.isEmpty()) return

        scope.launch {
            // 알람 인텐트로 넘어온 루프 스냅샷에는 done 기록이 없다. 그 값만 믿으면 사용자가
            // 미리 완료해 둔 루프에도 "시작할 시간이에요" 를 띄우게 되므로, DB 를 다시 읽어
            // 지금도 응답이 필요한 루프만 알린다.
            val loops = queryStillRunning(loopIds)
            logger.i { "announce ${loops.size}/${loopIds.size} started loop(s)" }
            notificationHelper.notifyLoopsStarted(loops)
        }
    }

    private fun ensureScreenReceiver() {
        if (screenReceiver != null) return

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_SCREEN_ON) flushDeferred()
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_SCREEN_ON))
        screenReceiver = receiver
    }

    @Synchronized
    private fun flushDeferred() {
        val loopIds = deferredLoopIds.toSet()
        deferredLoopIds.clear()
        unregisterScreenReceiver()
        if (loopIds.isEmpty()) return

        scope.launch {
            // 화면이 꺼진 사이 이미 종료됐거나 위젯/알림에서 완료·비활성 처리된 루프도 있다.
            // 알람 인텐트에서 복원한 스냅샷은 그 변화를 모르므로 DB 를 다시 읽어 판정한다.
            val loops = queryStillRunning(loopIds)
            logger.i { "screen turned on -> ${loops.size}/${loopIds.size} still running" }
            if (loops.isNotEmpty()) notificationHelper.notifyLoopsInProgress(loops)
        }
    }

    /** 지금도 진행 중인(미응답) 루프만 남긴다. 자정을 넘긴 occurrence 도 올바르게 판정한다. */
    private suspend fun queryStillRunning(loopIds: Set<Int>): List<LoopBase> {
        val dao = appDb.fullLoopDao()
        val today = LocalDate.now()
        val yesterdayLoops = dao.getAllLoops(today.minusDays(1).toMs()).associateBy { it.loopId }

        return dao.getAllLoops(today.toMs())
            .filter { loop -> loop.loopId in loopIds }
            .map { loop -> currentOccurrence(today = loop, yesterday = yesterdayLoops[loop.loopId]) }
            .filter { loop -> loop.isActive() && !loop.isRespond && !loop.isDisabled }
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
