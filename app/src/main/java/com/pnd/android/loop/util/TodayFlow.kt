package com.pnd.android.loop.util

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/**
 * "오늘"을 흘려보내는 flow. 값이 바뀌는 순간(자정)에 맞춰 한 번씩만 깨어난다.
 *
 * 화면에서 `LocalDate.now()` 를 직접 부르면 그 값이 컴포지션 시점에 박혀, 앱을 켜 둔 채
 * 자정을 넘기면 "오늘" 표시가 어제에 남는다. 이 앱은 상시 알림과 자정 예약이 있어 화면이
 * 켜진 채 날짜가 바뀌는 일이 드물지 않으므로, 날짜에 기대는 화면은 이 flow 를 구독한다.
 *
 * 자정 직후 몇 밀리초의 오차로 아직 어제가 나오는 일이 없도록 1초를 더 얹어 깨어나고,
 * [distinctUntilChanged] 로 같은 날짜가 두 번 나가지 않게 막는다.
 */
fun todayFlow(): Flow<LocalDate> = flow {
    while (true) {
        val today = LocalDate.now()
        emit(today)

        val nextMidnight = LocalDateTime.of(today.plusDays(1), LocalTime.MIDNIGHT)
        val waitMs = ChronoUnit.MILLIS.between(LocalDateTime.now(), nextMidnight) + 1_000L
        delay(waitMs.coerceAtLeast(1_000L))
    }
}.distinctUntilChanged()
