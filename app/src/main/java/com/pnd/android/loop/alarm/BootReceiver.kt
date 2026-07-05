package com.pnd.android.loop.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.pnd.android.loop.common.log
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * 재부팅 시 알람은 모두 초기화되므로, 부팅이 끝나면 루프들을 다시 예약하고
 * 진행 중인 루프가 있으면 알림을 복구한다. 이 리시버가 없으면 사용자가 앱을
 * 직접 열기 전까지 알림이 뜨지 않는다.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    private val logger = log("BootReceiver")

    @Inject
    lateinit var loopScheduler: LoopScheduler

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON" -> {
                logger.i { "reboot detected, re-syncing loops" }
                loopScheduler.syncLoops()
            }
        }
    }
}
