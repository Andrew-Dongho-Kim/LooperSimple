package com.pnd.android.loop.alarm

import androidx.annotation.StringDef


const val ACTION_LOOP_START = "com.pnd.android.app.ACTION_LOOP_ALARM"
const val ACTION_LOOP_REPEAT = "com.pnd.android.app.ACTION_LOOP_REPEAT"
const val ACTION_LOOP_END = "com.pnd.android.app.ACTION_LOOP_ENDED"
const val ACTION_LOOP_SYNC = "com.pnd.android.app.ACTION_LOOP_SYNC"

@StringDef(
    ACTION_LOOP_START,
    ACTION_LOOP_REPEAT,
    ACTION_LOOP_END,
    ACTION_LOOP_SYNC
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