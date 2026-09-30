package com.pnd.android.loop.state

import androidx.annotation.IntDef
import java.time.LocalDate

/** Persisted response codes. Keep these values stable so existing Room records remain readable. */
@Target(AnnotationTarget.TYPE, AnnotationTarget.VALUE_PARAMETER, AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY, AnnotationTarget.FIELD)
@Retention(AnnotationRetention.SOURCE)
@IntDef(DoneState.DISABLED, DoneState.NO_RESPONSE, DoneState.DONE, DoneState.SKIP, DoneState.IN_PROGRESS)
annotation class DoneState {
    companion object {
        /** Legacy daily record. New enable/disable changes are stored as settings revisions. */
        const val DISABLED = -1
        const val NO_RESPONSE = 0
        const val DONE = 1
        const val SKIP = 2
        const val IN_PROGRESS = 3
    }
}

/** A day without an occurrence; display only, never written to loop_done. */
const val NOT_SCHEDULED = -2

fun Int?.isDisabled(): Boolean = this == DoneState.DISABLED
fun Int?.isDone(): Boolean = this == DoneState.DONE
fun Int?.isSkip(): Boolean = this == DoneState.SKIP
fun Int?.isRespond(): Boolean = isDone() || isSkip()
fun Int?.isNoResponse(): Boolean = this == DoneState.NO_RESPONSE
fun Int?.isInProgress(): Boolean = this == DoneState.IN_PROGRESS

/** Only these states can be written by a response action. Disabling changes settings instead. */
fun Int.isWritableResponse(): Boolean = isRespond() || isNoResponse() || isInProgress()

/** Today's unanswered occurrence can still be completed; running and future records never settle. */
fun Int?.isSettledOn(date: LocalDate, today: LocalDate): Boolean =
    date <= today && (isRespond() || (isNoResponse() && date < today))

/** Stable, locale-independent names used by CSV exports. */
fun Int.stateExportName(): String = when (this) {
    DoneState.DONE -> "done"
    DoneState.SKIP -> "skip"
    DoneState.IN_PROGRESS -> "in_progress"
    DoneState.NO_RESPONSE -> "no_response"
    DoneState.DISABLED -> "disabled"
    NOT_SCHEDULED -> "not_scheduled"
    else -> "unknown"
}
