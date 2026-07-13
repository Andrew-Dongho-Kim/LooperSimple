package com.pnd.android.loop.data

/**
 * 통계 화면 전용 Room 조회 결과(DTO)들을 모아 둔다.
 * (순위용 [LoopWithStatistics] · 월별 투자시간 [MonthlyLoopDuration] 은 LoopWithDone.kt 참고)
 *
 * 화면에 바로 뿌리는 UI 모델이 아니라, DAO 쿼리 결과를 담는 "원자재"에 해당한다.
 * 실제 지표 계산은 ui/statisctics 의 순수 함수들이 이 DTO들을 가공해서 수행한다.
 */

/**
 * 기간 내 하나의 루프 응답 기록(완료/스킵/무응답 등, DISABLED 제외).
 *
 * 시간대 히트맵·완벽한 날·회고율·계획대비 실제 등 기간 기반 지표 대부분을
 * 이 한 종류의 레코드에서 계산하기 위해 필요한 컬럼을 폭넓게 담는다.
 *
 * @param loopId 루프 id
 * @param title 루프 제목
 * @param color 루프 색상(ARGB)
 * @param date 응답 날짜(에폭 ms, 자정 기준)
 * @param done 응답 상태([LoopDoneVo.DoneState])
 * @param startInDay 실제 시작 시각(자정부터의 ms). 기록이 없으면 음수(-1)일 수 있다.
 * @param endInDay 실제 종료 시각(자정부터의 ms).
 * @param plannedStartInDay 루프에 설정된 계획 시작 시각(자정부터의 ms). '언제든지' 루프는 -1.
 * @param isAnyTime '언제든지' 루프 여부(계획 시각이 없어 계획대비 실제 분석에서 제외).
 * @param retrospect 해당 날짜에 남긴 회고 메모(없으면 null).
 */
data class LoopResponseRecord(
    val loopId: Int,
    val title: String,
    val color: Int,
    val date: Long,
    val done: Int,
    val startInDay: Long,
    val endInDay: Long,
    val plannedStartInDay: Long,
    val isAnyTime: Boolean,
    val retrospect: String?,
)

/**
 * 월(연/월) 단위 완료 응답 집계. 완료율 추세 계산에 사용한다.
 *
 * @param year 연도
 * @param month 월(1~12)
 * @param doneCount 완료(DONE) 횟수
 * @param respondedCount 응답한 전체 횟수(DISABLED 제외 = 완료+스킵+무응답 등)
 */
data class MonthlyCompletionCount(
    val year: Int,
    val month: Int,
    val doneCount: Int,
    val respondedCount: Int,
)

/**
 * 최근에 생성된 루프의 누적 성과. 신규 루프 정착률 계산에 사용한다.
 *
 * @param loopId 루프 id
 * @param title 루프 제목
 * @param color 루프 색상(ARGB)
 * @param created 생성 시각(에폭 ms)
 * @param respondedCount 생성 이후 응답한 전체 횟수(DISABLED 제외)
 * @param doneCount 생성 이후 완료(DONE) 횟수
 */
data class NewLoopRecord(
    val loopId: Int,
    val title: String,
    val color: Int,
    val created: Long,
    val respondedCount: Int,
    val doneCount: Int,
)
