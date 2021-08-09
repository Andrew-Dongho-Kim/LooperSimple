package com.pnd.android.loop.common

import android.util.Log
import com.pnd.android.loop.BuildConfig

const val PREFIX_TAG = "LooperSimple"
const val APP_VERSION = BuildConfig.VERSION_NAME

fun log(tag: String) = Logger(tag)

fun test(message:()->String) = Logger("TEST-DH").d(message)

class Logger(val tag: String) {
    inline fun d(crossinline message: () -> String) =
        Log.d(tag, "[$PREFIX_TAG] ${message()} - AppVersion: $APP_VERSION")

    inline fun e(throwable: Throwable? = null, crossinline message: () -> String) =
        Log.e(tag, "[$PREFIX_TAG] ${message()} - AppVersion: $APP_VERSION", throwable)
}
