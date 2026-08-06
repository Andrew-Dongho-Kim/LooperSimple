package com.pnd.android.loop.data

import com.pnd.android.loop.util.currentOccurrence
import com.pnd.android.loop.util.currentOccurrenceDate
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.isOvernight
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * 오늘 화면(홈 오늘 탭·앱 위젯)에 걸치는 occurrence 하나.
 *
 * 보통은 루프 하나에 occurrence 하나지만, 자정을 넘기는 루프는 하루에 둘이 걸친다.
 * 예를 들어 22:00~06:30 루프를 오늘 07:00 에 보면
 *  - 어젯밤 시작해 오늘 아침에 끝난 몫 → 아직 답하지 않았다면 "응답 대기"
 *  - 오늘 밤 22:00 에 다시 시작할 몫 → "다음 예정"
 * 두 가지가 함께 있어야 한다. 둘은 done 기록이 저장되는 날짜가 다르므로([date]) 화면의 한 줄은
 * 루프가 아니라 이 occurrence 를 단위로 삼는다.
 */
data class TodayOccurrence(
    /** [date] 행의 done 기록과 조인된 루프. 진행/완료 여부는 이 값에서 읽어야 한다. */
    val loop: LoopBase,
    /** occurrence 가 시작한 날. 응답(완료/건너뜀)은 반드시 이 날짜 행에 기록한다. */
    val date: LocalDate,
    /**
     * 어젯밤에 시작해 오늘 아침에 끝났는데 아직 답하지 않은 몫인지.
     * 시계상으로는 다음 시작 전이지만 실제로는 이미 끝난 occurrence 라, 화면에서는 응답 대기로 다룬다.
     */
    val isCarriedOver: Boolean,
) {
    /**
     * 화면에서 이 몫을 가리키는 키(목록 항목 키, 다이얼 선택 상태).
     * 같은 루프가 두 몫으로 나올 수 있어 loopId 만으로는 서로 구분되지 않는다.
     */
    val key: String get() = "${loop.loopId}-$date"
}

/**
 * 오늘 화면에 올릴 occurrence 목록을 만든다. 홈 오늘 탭과 앱 위젯이 같은 규칙을 쓰도록 한곳에 둔다.
 *
 * @param todayLoops     오늘 날짜 행과 조인한 루프 전체
 * @param yesterdayLoops 어제 날짜 행과 조인한 루프(loopId → 루프). 자정을 넘기는 루프의 어젯밤
 *                       몫을 판단하는 데 쓴다.
 */
fun buildTodayOccurrences(
    todayLoops: List<LoopBase>,
    yesterdayLoops: Map<Int, LoopBase>,
    now: LocalDateTime = LocalDateTime.now(),
): List<TodayOccurrence> {
    val today = now.toLocalDate()
    val yesterday = today.minusDays(1)

    return buildList {
        todayLoops.forEach { loop ->
            val yesterdayRow = yesterdayLoops[loop.loopId]

            // 1) 어젯밤 몫. 오늘 밤 다시 시작하기 전까지 응답 대기로 남는다.
            if (yesterdayRow != null && yesterdayRow.isCarriedOverInto(now)) {
                add(
                    TodayOccurrence(
                        loop = yesterdayRow,
                        date = yesterday,
                        isCarriedOver = true,
                    )
                )
            }

            // 2) 지금 몫. 오늘 활성이거나 자정을 넘겨 지금도 진행 중인 루프만 오늘 화면에 올린다.
            if (!loop.enabled) return@forEach
            if (!loop.isActiveDay(today) && !loop.isActive(now)) return@forEach
            add(
                TodayOccurrence(
                    // 자정을 넘겨 이어지는 중이면 done 기록이 어제 행에 있으므로 그 행으로 바꿔 든다.
                    loop = currentOccurrence(today = loop, yesterday = yesterdayRow, now = now),
                    date = currentOccurrenceDate(today = loop, yesterday = yesterdayRow, now = now),
                    isCarriedOver = false,
                )
            )
        }
    }
}

/**
 * 어제 행([this])이 "어젯밤에 시작해 오늘 아침에 끝났고, 아직 답하지 않은" 몫인지.
 *
 * 자정을 넘기는 시간제 루프만 해당한다. 시간이 없는 anytime 루프는 종료 시각이 없어 어느
 * 시점에 끝났는지 알 수 없고, 하루 안에서 끝나는 루프는 어젯밤 몫이 오늘로 넘어오지 않는다.
 */
private fun LoopBase.isCarriedOverInto(now: LocalDateTime): Boolean {
    if (!enabled || isDisabled || isAnyTime || !isOvernight) return false
    // 어제가 활성 요일이 아니었다면 어젯밤에 시작한 occurrence 자체가 없다.
    if (!isActiveDay(now.toLocalDate().minusDays(1))) return false
    // 어제보다 나중에 만든 루프라면 어젯밤에는 아직 존재하지 않았다.
    if (!created.toLocalDate().isBefore(now.toLocalDate())) return false
    // 아직 오늘 아침 종료 시각 전이면 지금도 진행 중이다. 이월이 아니라 "지금 몫"이다.
    if (now.toLocalTime().toMs() < endInDay) return false
    return isNotRespond
}
