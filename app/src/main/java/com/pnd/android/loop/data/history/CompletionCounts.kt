package com.pnd.android.loop.data.history

import com.pnd.android.loop.state.DoneState
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * 퍼센트를 보여 주기 위한 최소 집계 수.
 *
 * 기록 한두 건으로 "100%"를 띄우면 수치가 실제 습관을 말해 주지 않는다. 표본이 이만큼 모이기
 * 전에는 퍼센트 대신 분모만 보여 준다([CompletionCounts.isReliable]).
 *
 * 단, "오늘" 창에는 적용하지 않는다. 오늘 할 일이 둘뿐인 사용자에게 "2개 중 1개 = 50%"는
 * 완전히 유효한 정보이고, 오늘 탭은 분모를 항상 함께 노출하므로 오해할 여지가 없다.
 */
const val MIN_RELIABLE_SAMPLES = 3

/**
 * "최근" 완료율이 공유하는 창. 전체 탭 헤더와 루프 카드 칩이 같은 값을 쓰므로, 한 화면의 두
 * 수치가 체계적으로 어긋나지 않는다. 오늘은 포함하지 않는다(어제까지).
 */
const val RECENT_COMPLETION_DAYS = 30L

/**
 * 완료율 표기의 단일 기준.
 *
 * 앱의 모든 완료율은 이 타입으로 수렴한다. "어느 기간을 볼지"(창)와 "어떤 루프를 셀지"(후보
 * 선정)는 호출하는 쪽이 정하지만, **무엇을 분모에 넣고 어떻게 반올림하는지는 여기서만 정한다.**
 *
 * - 건너뜀은 의도적인 응답이라 분모에 들지만 완료는 아니다. 그래서 완료율을 낮춘다.
 * - 아직 확정되지 않은 날(오늘의 미응답·진행 중·미래)은 [ResolvedLoopDay.isSettled] 에서 이미
 *   걸러진 뒤 들어온다. 창이 오늘 하루뿐인 경우만 예외다([countTodayProgress]).
 * - 퍼센트는 정수로 반올림한다. 분모가 수백이어도 소수점은 의미 없는 정밀도이고, 화면마다
 *   자리수가 갈리면 같은 수치가 달라 보인다.
 * - 집계 대상이 없으면 비율은 0%가 아니라 null이다. "한 번도 할 일이 없었다"와 "할 일이
 *   있었는데 하나도 못 했다"는 다른 사실이다.
 */
data class CompletionCounts(
    val done: Int = 0,
    val skipped: Int = 0,
    val unanswered: Int = 0,
) {
    val total: Int get() = done + skipped + unanswered

    val isReliable: Boolean get() = total >= MIN_RELIABLE_SAMPLES

    val completionRate: Float? get() = rateOf(done)
    val responseRate: Float? get() = rateOf(done + skipped)
    val skipRate: Float? get() = rateOf(skipped)

    val completionPercent: Int? get() = completionRate?.toPercent()
    val responsePercent: Int? get() = responseRate?.toPercent()
    val skipPercent: Int? get() = skipRate?.toPercent()

    private fun rateOf(count: Int): Float? =
        if (total == 0) null else count.toFloat() / total
}

private fun Float.toPercent(): Int = (this * 100).roundToInt()

/** 날짜별 상태에서 바로 센다. 이미 확정 판정을 통과한 상태만 넘겨야 한다. */
fun countActivity(states: Collection<Int>) = CompletionCounts(
    done = states.count { it == DoneState.DONE },
    skipped = states.count { it == DoneState.SKIP },
    unanswered = states.count { it == DoneState.NO_RESPONSE },
)

/**
 * "오늘" 하루만 보는 창의 분모 규칙. 여기서만 확정 판정([ResolvedLoopDay.isSettled])을 쓰지 않는다.
 *
 * 확정 판정은 "오늘의 미응답은 아직 완료할 수 있으니 분모에서 뺀다"는 규칙이다. 창이 여러 날이면
 * 오늘 하루를 빼도 수치가 거의 흔들리지 않아 타당하지만, 창이 오늘 하루뿐이면 분모에 답한 것만
 * 남아 완료율이 사실상 늘 100%가 된다. 그 결과 아직 답하지 않은 루프는 오늘 수치에 아예 등장하지
 * 못하고, 특히 시작 시각이 없어 하루 내내 미응답으로 남는 anytime 루프는 완료하기 전까지
 * 분모에도 들어오지 않는다.
 *
 * 그래서 오늘 몫이 있는 occurrence 는 아직 답하지 않았어도 모두 분모에 넣는다. 진행 중(IN_PROGRESS)
 * 도 "아직 완료가 아닌 오늘 할 일"이므로 미완료로 센다. 통계 위젯의 오늘 칸과 같은 규칙이라
 * (computeStatisticsWidgetData 의 todayDone/todayTotal) 홈과 위젯의 오늘 수치가 어긋나지 않는다.
 */
fun countTodayProgress(states: Collection<Int>) = CompletionCounts(
    done = states.count { it == DoneState.DONE },
    skipped = states.count { it == DoneState.SKIP },
    unanswered = states.count { it == DoneState.NO_RESPONSE || it == DoneState.IN_PROGRESS },
)

/**
 * occurrence 목록에서 확정된 날만 남겨 센다. 창을 자르는 일은 호출하는 쪽이 한다.
 *
 * 이미 [LoopHistorySnapshot.settled] 로 걸러진 목록을 넘겨도 안전하다(같은 판정이라 멱등).
 */
fun List<ResolvedLoopDay>.completionCounts(today: LocalDate): CompletionCounts =
    countActivity(filter { it.isSettled(today) }.map { it.response.done })
