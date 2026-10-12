package com.pnd.android.loop.state

import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.util.MS_1DAY

/**
 * 시간제 루프 카드의 ✓ 한 번으로 남길 완료 기록. 고르지 않고 바로 저장하므로, 지금 상태에서
 * 가장 그럴듯한 시각을 정해 둔다. 틀렸으면 사용자가 스낵바의 '시간 수정'으로 고친다.
 *
 * anytime 루프는 계획 시각이 없어 추정할 근거가 없으므로 ✓ 를 두지 않고 시각을 직접 입력받는다.
 */
sealed interface QuickDone {
    /** [com.pnd.android.loop.data.history.LoopMutationStore.setResponse] 에 넘길 시각. null 이면 계획 시각 그대로. */
    val times: Pair<Long, Long>?

    /** 시간창 안에서 일찍 끝냈다: 계획 시작부터 지금까지. */
    data class EarlyFinish(override val times: Pair<Long, Long>) : QuickDone

    /** 시간창이 끝난(또는 아직 오지 않은) 루프: 계획 시각 그대로 완료한다. */
    data object AsPlanned : QuickDone {
        override val times: Pair<Long, Long>? get() = null
    }
}

/**
 * 시간제 [loop] 를 지금([nowInDay]) ✓ 했을 때 남길 기록을 정한다.
 *
 * @param isInWindow 지금 시간창 안에 있는지.
 */
fun quickDoneFor(
    loop: LoopBase,
    nowInDay: Long,
    isInWindow: Boolean,
): QuickDone {
    require(!loop.isAnyTime) { "Anytime loops are recorded with entered times" }
    require(nowInDay in 0 until MS_1DAY)
    return if (isInWindow && loop.startInDay in 0 until MS_1DAY) {
        QuickDone.EarlyFinish(loop.startInDay to nowInDay)
    } else {
        QuickDone.AsPlanned
    }
}
