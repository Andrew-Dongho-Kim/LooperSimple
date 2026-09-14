package com.pnd.android.loop.data

/** Room projection with one unambiguous constructor (FullLoopVo has JVM overloads). */
data class AchievementDayRecord(
    val loopId: Int,
    val color: Int,
    val title: String,
    val created: Long,
    val startInDay: Long,
    val endInDay: Long,
    val activeDays: Int,
    val enabled: Boolean,
    val actualStartInDay: Long,
    val actualEndInDay: Long,
    val date: Long,
    val retrospect: String,
    val done: Int,
    val isAnyTime: Boolean,
    val weeklyGoal: Int,
) {
    fun asFullLoop() = FullLoopVo(
        loopId = loopId, color = color, title = title, created = created,
        startInDay = startInDay, endInDay = endInDay, activeDays = activeDays,
        enabled = enabled, actualStartInDay = actualStartInDay, actualEndInDay = actualEndInDay,
        date = date, retrospect = retrospect, done = done,
        isAnyTime = isAnyTime, weeklyGoal = weeklyGoal,
    )
}
