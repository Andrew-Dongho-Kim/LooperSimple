package com.pnd.android.loop.alarm

import androidx.annotation.StringDef


const val ACTION_LOOP_START = "com.pnd.android.app.ACTION_LOOP_ALARM"
const val ACTION_LOOP_END = "com.pnd.android.app.ACTION_LOOP_ENDED"
const val ACTION_LOOP_SYNC = "com.pnd.android.app.ACTION_LOOP_SYNC"

/**
 * anytime 루프에 "보통 이 시각에 시작하셨어요" 하고 권하는 알람.
 *
 * 시간창이 없는 anytime 루프는 [ACTION_LOOP_START] 를 걸 시각 자체가 없다. 그래서 과거에 실제로
 * 시작한 기록에서 습관 시각을 추정해(HabitualStart 참고) 그 시각에 이 알람을 예약한다.
 */
const val ACTION_LOOP_ANYTIME_DUE = "com.pnd.android.app.ACTION_LOOP_ANYTIME_DUE"

@StringDef(
    ACTION_LOOP_START,
    ACTION_LOOP_END,
    ACTION_LOOP_SYNC,
    ACTION_LOOP_ANYTIME_DUE,
)
annotation class LoopScheduleAction

/**
 *
 */
const val ACTION_LOOP_DONE = "com.pnd.android.loop.ACTION_LOOP_DONE"

/**
 *
 */
const val ACTION_LOOP_CANCEL = "com.pnd.android.loop.ACTION_CANCEL"