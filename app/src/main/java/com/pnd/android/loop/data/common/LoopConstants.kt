package com.pnd.android.loop.data.common


import com.pnd.android.loop.data.LoopDay
import java.time.LocalTime
import java.util.concurrent.TimeUnit

const val NO_REPEAT = 0L
const val DEFAULT_CREATED = 0L
const val DEFAULT_TITLE = ""
const val DEFAULT_COLOR = 0xFF9EBBEE.toInt() // 홈 톤과 어울리는 잔잔한 파스텔 블루
const val DEFAULT_ACTIVE_DAYS = LoopDay.EVERYDAY
const val DEFAULT_INTERVAL = NO_REPEAT
const val DEFAULT_ENABLED = true
const val DEFAULT_IS_ANY_TIME = false
const val DEFAULT_IS_MOCK = false

val defaultStartInDay
    get() =
        TimeUnit.NANOSECONDS.toMillis(
            LocalTime
                .now()
                .withMinute(0)
                .withSecond(0)
                .toNanoOfDay()
        )

val defaultEndInDay
    get() = TimeUnit.NANOSECONDS.toMillis(
        LocalTime
            .now()
            .withMinute(0)
            .withSecond(0)
            .withNano(0)
            .plusHours(1)
            .toNanoOfDay()
    )


/**
 * 루프 색상 팔레트.
 *
 * 홈 화면의 잔잔한 톤에 맞춰 채도가 낮은 파스텔 계열로만 구성했다. 색은 색상환 순서
 * (빨강 → 노랑 → 초록 → 파랑 → 보라 → 분홍)로 배열해 선택기에서 부드러운 그라데이션처럼
 * 읽히고, 마지막 두 색은 어떤 색과도 어울리는 중립 톤이다.
 *
 * 각 색은 라이트/다크 모드 모두에서 잉크 색과 살짝 섞여 표시되므로(compositeOverOnSurface)
 * 밝은 파스텔이라도 양쪽 테마에서 무난하게 보인다. 6열 그리드에 맞도록 항상 6의 배수를 유지한다.
 */
val SUPPORTED_COLORS = listOf(
    // 웜톤: 빨강 → 노랑
    0xFFF29B9B.toInt(), 0xFFF2A98F.toInt(), 0xFFF3B98C.toInt(),
    0xFFF6CE90.toInt(), 0xFFF3DE93.toInt(), 0xFFEDE895.toInt(),
    // 그린: 라임 → 에메랄드
    0xFFD9E795.toInt(), 0xFFBFE197.toInt(), 0xFFA6D99C.toInt(),
    0xFF97D9AC.toInt(), 0xFF8ED8BE.toInt(), 0xFF8CD6CE.toInt(),
    // 쿨톤: 시안 → 인디고 (DEFAULT_COLOR = 파스텔 블루)
    0xFF8FD1DE.toInt(), 0xFF95C8EC.toInt(), DEFAULT_COLOR,
    0xFFAAB1EA.toInt(), 0xFFB8A9E6.toInt(), 0xFFC7A5E2.toInt(),
    // 보라 → 분홍, 그리고 중립 톤
    0xFFD7A4DE.toInt(), 0xFFE6A4D5.toInt(), 0xFFF0A5C3.toInt(),
    0xFFF29FAE.toInt(), 0xFFCBB6A6.toInt(), 0xFFB7BEC9.toInt(),
)


const val MAX_LOOPS_TOGETHER = 3