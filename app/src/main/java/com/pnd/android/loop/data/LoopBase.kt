package com.pnd.android.loop.data

interface LoopBase {
    val id: Int
    val title:String
    val color:Int
    val loopStart: Long
    val loopEnd: Long
    val loopActiveDays: Int
    val enabled: Boolean
}