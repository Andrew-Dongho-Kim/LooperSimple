package com.pnd.android.loop.appwidget

import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoop
import com.pnd.android.loop.data.putTo
import com.pnd.android.loop.util.isOvernight
import com.pnd.android.loop.util.isTimeInLoop
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

private const val EXTRA_OCCURRENCE_DATE = "extra_occurrence_date"
private const val EXTRA_IS_CARRIED_OVER = "extra_is_carried_over"

/**
 * 위젯의 한 줄이 대표하는 occurrence.
 *
 * 위젯으로 넘어가는 루프는 JSON 을 거치며 done 상태를 잃고 순수 LoopVo 로 재구성된다
 * ([AppWidgetUpdateWorker] 참고). 그래서 "어느 날 몫인지"와 "이미 끝난 몫인지"는 루프만 봐서는
 * 알 수 없고, 응답을 올바른 날짜 행에 기록하려면 이 둘을 따로 실어 보내야 한다.
 *
 * 자정을 넘기는 루프는 어젯밤 몫([isCarriedOver])과 오늘 밤 몫이 위젯에 나란히 올 수 있다.
 */
data class WidgetLoop(
    val loop: LoopBase,
    /** 응답(완료/건너뜀)을 기록할 occurrence 날짜(에폭 ms). */
    val dateMs: Long,
    /** 어젯밤에 시작해 오늘 아침에 끝난, 아직 답하지 않은 몫인지. */
    val isCarriedOver: Boolean,
) {
    /**
     * Glance LazyColumn 의 항목 id. 같은 루프가 두 몫으로 올 수 있어 loopId 만으로는 겹치므로,
     * 어젯밤 몫인지까지 섞어 항상 다른 값이 되게 한다.
     */
    fun itemId(): Long = loop.loopId.toLong() * 2 + if (isCarriedOver) 1 else 0
}

// ---------------------------------------------------------------------------
// 위젯 기준 상태 판별
//  위젯으로 전달되는 루프는 done 상태가 유실되고, anytime 은 실제 시작/종료 시각이 start/end 로
//  옮겨져 온다([AppWidgetUpdateWorker] 참고). 그래서 진행/응답 가능 여부는 doneState 가 아니라
//  start/end 시각과 [WidgetLoop.isCarriedOver] 로 판단해야 한다.
//  Small·Medium 두 위젯이 같은 규칙을 쓰도록 여기 한곳에 둔다.
// ---------------------------------------------------------------------------

/**
 * 지금 진행 중인가. anytime 은 "시작됐고 아직 정지 안 됨", 시간제는 시간창 안.
 *
 * 시간창 판정은 [isTimeInLoop] 에 맡긴다. 자정을 넘기는 루프(예: 22:00~06:00)는 창이
 * [start,24h)∪[0,end) 두 조각으로 갈리는데, 단순 범위 비교로는 새벽 구간을 놓친다.
 */
fun WidgetLoop.isRunning(): Boolean {
    // 어젯밤에서 넘어온 몫은 이미 끝났다. 시간창만 보면 오늘 밤 몫과 구분되지 않는다.
    if (isCarriedOver) return false
    if (loop.isAnyTime) return loop.startInDay >= 0 && loop.endInDay < 0
    return loop.isTimeInLoop(LocalTime.now().toMs())
}

/** anytime 루프의 시작/정지 버튼이 필요한 상태(아직 시작 전이거나 진행 중). */
fun WidgetLoop.needsStartStop(): Boolean =
    !isCarriedOver && loop.isAnyTime && (loop.startInDay < 0 || loop.endInDay < 0)

/**
 * 시간창이 끝나 완료/건너뜀 답만 남은 상태.
 *
 * 자정을 넘기는 루프의 오늘 밤 몫은 낮 시간대에 [com.pnd.android.loop.util.isPast] 가 참이 되지만
 * "지난 것"이 아니라 "오늘 밤 예정"이다. 그 몫의 응답 대기는 어젯밤 몫([isCarriedOver])이 맡으므로
 * 여기서는 자정 넘김 루프를 제외한다.
 */
fun WidgetLoop.awaitsResponse(): Boolean {
    if (isCarriedOver) return true
    if (loop.isAnyTime || isRunning() || loop.isOvernight) return false
    return LocalTime.now().toMs() >= loop.endInDay
}

/** 완료/건너뛰기 버튼을 보여줄 상태(이미 시작했거나, 어젯밤에서 넘어온 몫). */
fun WidgetLoop.canRespond(): Boolean {
    if (isCarriedOver) return true
    if (loop.isAnyTime) return false
    // 진행 중이면 미리 완료할 수 있다. 자정을 넘긴 새벽 구간도 진행 중으로 잡히도록,
    // 시작 시각 비교만으로 판단하지 않는다.
    return isRunning() || LocalTime.now().toMs() >= loop.startInDay
}

fun WidgetLoop.putTo(map: MutableMap<String, Any?>) {
    loop.putTo(map)
    map[EXTRA_OCCURRENCE_DATE] = dateMs
    map[EXTRA_IS_CARRIED_OVER] = isCarriedOver
}

fun Map<String, Any?>.asWidgetLoop(): WidgetLoop = WidgetLoop(
    loop = asLoop(),
    dateMs = (getOrDefault(EXTRA_OCCURRENCE_DATE, 0L) as Number).toLong(),
    isCarriedOver = getOrDefault(EXTRA_IS_CARRIED_OVER, false) as Boolean,
)
