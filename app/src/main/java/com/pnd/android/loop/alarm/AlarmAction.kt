package com.pnd.android.loop.alarm

import androidx.annotation.StringDef


const val ACTION_LOOP_START = "com.pnd.android.app.ACTION_LOOP_ALARM"
const val ACTION_LOOP_REPEAT = "com.pnd.android.app.ACTION_LOOP_REPEAT"
const val ACTION_LOOP_END = "com.pnd.android.app.ACTION_LOOP_ENDED"
const val ACTION_LOOP_SYNC = "com.pnd.android.app.ACTION_LOOP_SYNC"

/** 진행 중인 루프의 알림(남은 시간·진행률)을 1분마다 갱신하기 위한 내부 틱 액션 */
const val ACTION_LOOP_PROGRESS = "com.pnd.android.app.ACTION_LOOP_PROGRESS"

@StringDef(
    ACTION_LOOP_START,
    ACTION_LOOP_REPEAT,
    ACTION_LOOP_END,
    ACTION_LOOP_SYNC,
    ACTION_LOOP_PROGRESS,
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