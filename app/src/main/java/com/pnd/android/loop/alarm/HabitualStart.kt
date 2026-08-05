package com.pnd.android.loop.alarm

import com.pnd.android.loop.alarm.notification.NotificationSettings
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1HOUR
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToLong

/*
 * anytime 루프에 "보통 이 시각에 시작하셨어요" 하고 권할 시각을 과거 기록에서 추정한다.
 *
 * 시간제 루프는 사용자가 정한 시작 시각이 있어 그때 알리면 되지만, anytime 루프에는 그런 시각이
 * 아예 없다. 그래서 실제로 시작한 날들의 시각을 모아 "이 사람의 습관 시각"을 만들어 쓴다.
 *
 * 진입점은 두 개다.
 *  - estimateHabitualStart(samples, today) : 순수 계산. 표본만 주면 결과가 정해진다.
 *  - HabitualStartEstimator                : DB에서 표본을 모아 위 함수를 돌린다.
 */

/**
 * 하루의 경계를 자정이 아니라 새벽 4시로 본다.
 *
 * 시각은 원형이라 23:50 과 00:20 은 사실 20분 차이인데, 자정 기준으로 늘어놓으면 하루만큼
 * 떨어진 값이 된다. 그대로 평균을 내면 23:30·23:50·00:20 의 평균이 07:53 이 나와 엉뚱한 시각에
 * 알리게 된다. 밤에 하는 anytime 루프(스트레칭·일기·정리)가 많아 흔한 경우다.
 *
 * 새벽 4시를 0 으로 옮겨 계산하면 자정 전후가 인접해져 이 문제가 사라진다. 사람의 "하루"도
 * 자정에 끊기지 않으므로 이 축이 오히려 직관과 맞고, 새벽 4시에 습관을 갖는 경우는 드물다.
 */
private val DAY_START_OFFSET = 4 * MS_1HOUR

/**
 * 알림 시각을 표본 분포의 어디에 둘지. 사용자가 설정에서 고른다.
 *
 * 중앙값(0.5)에 알리면 정의상 절반의 날은 이미 시작한 뒤에 알림 시각이 온다. 그 날은 알림이
 * 안 뜨지만(발화 직전에 다시 확인한다) 문제는 나머지다. 평소보다 이른 알림은 "아직 할 시간
 * 아닌데"로 읽히고, 그게 반복되면 무시하는 습관이 든다.
 *
 * 평소 시각대를 거의 다 지난 뒤에 알리면 "맞다, 오늘 안 했네"가 된다. 같은 1회 알림으로
 * 실효성이 훨씬 높다. 그래서 기본값을 중앙값보다 늦은 [USUAL] 로 둔다.
 */
enum class HabitualStartShift(val percentile: Double) {
    /** 평소 시작하는 시각대의 한가운데. 알림을 조금 더 일찍 받고 싶을 때. */
    EARLIER(0.5),

    /** 평소 시각대를 거의 다 지난 지점(기본값). */
    USUAL(0.65),

    /** 평소 시각대를 확실히 지난 지점. 알림을 늦게 받고 싶을 때. */
    LATER(0.8),
    ;

    companion object {
        /** 저장된 ordinal 로 복원한다. 값이 깨졌거나 범위를 벗어나면 기본값으로 돌아간다. */
        fun ofOrdinal(ordinal: Int) = entries.getOrNull(ordinal) ?: USUAL
    }
}

/** 평일/휴일 그룹만으로 추정하기 위한 최소 표본 수. 미달이면 전체 표본으로 물러난다. */
private const val MIN_GROUP_SAMPLES = 3

/** 분위수를 낼 최소 표본 수. 미달이면 분위수가 의미 없어 마지막 시작 시각을 그대로 쓴다. */
private const val MIN_TOTAL_SAMPLES = 2

/**
 * 표본으로 볼 최근 시작 기록의 개수.
 *
 * "최근 N일"이 아니라 "최근 N개"로 세는 이유: 주 2~3회만 하는 루프도 표본을 채울 수 있다.
 * 대신 아무리 개수가 모자라도 [SAMPLE_MAX_DAYS] 보다 오래된 기록은 지금의 습관이 아니라 보고 버린다.
 */
private const val SAMPLE_LIMIT = 12
private const val SAMPLE_MAX_DAYS = 60L

/** 추정에 쓰는 과거 시작 기록 하나. [startInDay] 는 하루 안의 시각(ms). */
data class StartSample(
    val dayOfWeek: DayOfWeek,
    val startInDay: Long,
)

/** 추정이 무엇을 근거로 삼았는지. 알림 문구를 어떻게 쓸지 정하는 데 쓴다. */
enum class HabitualStartBasis {
    /** 평일 기록만으로 추정했다. */
    WEEKDAY,

    /** 휴일 기록만으로 추정했다. */
    HOLIDAY,

    /** 평일/휴일 표본이 모자라 요일을 가리지 않고 추정했다. */
    ALL_DAYS,

    /** 표본이 하나뿐이라 마지막 시작 시각을 그대로 썼다. */
    LAST_START,
}

/** 추정 결과. [startInDay] 시각에 "아직 시작하지 않았으면" 알린다. */
data class HabitualStart(
    val startInDay: Long,
    val basis: HabitualStartBasis,
    val sampleCount: Int,
)

/**
 * 과거 시작 기록에서 오늘 알릴 시각을 추정한다. 표본이 하나도 없으면 null — 근거가 없을 때
 * 시각을 발명하지 않는다(사용자가 한 번 직접 시작하면 그때부터 기준이 생긴다).
 *
 * 추정은 세 가지를 신경 쓴다.
 *  - 평일과 휴일을 나눈다. 기상·식사 시각이 달라지면 습관 시각도 갈린다.
 *  - 자정을 넘는 습관도 한 덩어리로 본다([DAY_START_OFFSET]).
 *  - 중앙값이 아니라 조금 늦은 분위수를 쓴다([HabitualStartShift]).
 *
 * 표본이 모자라면 단계적으로 물러난다([HabitualStartBasis] 순서대로).
 *  평일/휴일 그룹 → 요일 무시한 전체 → 마지막 시작 시각 → null
 *
 * @param samples 최신순으로 정렬된 과거 시작 기록. 오늘 기록은 포함하지 않는다.
 * @param today 오늘 요일. 평일/휴일 중 어느 표본을 볼지 가른다.
 * @param shift 알림 시각을 평소보다 이르게/늦게 당기는 사용자 설정.
 */
fun estimateHabitualStart(
    samples: List<StartSample>,
    today: DayOfWeek,
    shift: HabitualStartShift = HabitualStartShift.USUAL,
): HabitualStart? {
    if (samples.isEmpty()) return null

    val sameKindOfDay = samples.filter { sample -> sample.dayOfWeek.isHoliday == today.isHoliday }

    return when {
        sameKindOfDay.size >= MIN_GROUP_SAMPLES -> HabitualStart(
            startInDay = notifyTimeOf(samples = sameKindOfDay, shift = shift),
            basis = if (today.isHoliday) HabitualStartBasis.HOLIDAY else HabitualStartBasis.WEEKDAY,
            sampleCount = sameKindOfDay.size,
        )

        samples.size >= MIN_TOTAL_SAMPLES -> HabitualStart(
            startInDay = notifyTimeOf(samples = samples, shift = shift),
            basis = HabitualStartBasis.ALL_DAYS,
            sampleCount = samples.size,
        )

        else -> HabitualStart(
            startInDay = samples.first().startInDay,
            basis = HabitualStartBasis.LAST_START,
            sampleCount = samples.size,
        )
    }
}

/**
 * 표본들의 분위수를 알림 시각으로 삼는다. 자정을 넘는 습관도 한 덩어리로 모이도록 새벽 4시
 * 기준 축에서 계산한 뒤 원래 시각으로 되돌린다.
 *
 * 분위수는 순서 통계량이라 이상치에 거의 흔들리지 않는다. 평소 09시에 하는 루프에 23시 기록
 * 하나가 섞여도 0.65 지점은 그대로다. 그래서 별도의 이상치 제외 단계를 두지 않는다.
 */
private fun notifyTimeOf(samples: List<StartSample>, shift: HabitualStartShift): Long {
    val onDayAxis = samples.map { sample -> sample.startInDay.toDayAxis() }.sorted()
    return percentile(sorted = onDayAxis, ratio = shift.percentile).fromDayAxis()
}

/**
 * 오름차순 [sorted] 의 [ratio] 분위수. 인접한 두 표본 사이를 선형 보간해, 표본이 적어도 값이
 * 뚝뚝 끊기지 않게 한다. 예) 5개의 0.65 분위 → 3번째와 4번째 사이의 60% 지점.
 */
private fun percentile(sorted: List<Long>, ratio: Double): Long {
    val position = ratio * (sorted.size - 1)
    val lower = floor(position).toInt()
    val upper = ceil(position).toInt()
    if (lower == upper) return sorted[lower]

    val weight = position - lower
    return sorted[lower] + ((sorted[upper] - sorted[lower]) * weight).roundToLong()
}

/** 자정 기준 시각을 "새벽 4시 = 0" 인 축으로 옮긴다. [DAY_START_OFFSET] 참고. */
private fun Long.toDayAxis() = (this - DAY_START_OFFSET + MS_1DAY) % MS_1DAY

/** [toDayAxis] 의 역변환. */
private fun Long.fromDayAxis() = (this + DAY_START_OFFSET) % MS_1DAY

/** 토·일을 휴일로 본다. 공휴일은 앱이 알 수 없으므로 포함하지 않는다. */
private val DayOfWeek.isHoliday: Boolean
    get() = this == DayOfWeek.SATURDAY || this == DayOfWeek.SUNDAY

/**
 * DB에서 표본을 모아 [estimateHabitualStart] 를 돌린다.
 *
 * 알람을 예약하는 쪽([LoopScheduler])과 알림 문구를 만드는 쪽([AnyTimeStartPrompter])이 같은
 * 값을 봐야 하므로 계산 진입점을 여기 하나로 둔다. 알람이 걸린 시각과 알림에 적힌 "보통 09:30"
 * 이 서로 다르면 사용자는 앱이 엉뚱한 말을 한다고 느낀다.
 */
@Singleton
class HabitualStartEstimator @Inject constructor(
    appDb: AppDatabase,
    private val notificationSettings: NotificationSettings,
) {

    private val loopDoneDao = appDb.loopDoneDao()

    suspend fun estimate(loopId: Int, today: LocalDate = LocalDate.now()): HabitualStart? {
        val samples = loopDoneDao.getRecentStarts(
            loopId = loopId,
            since = today.minusDays(SAMPLE_MAX_DAYS).toMs(),
            today = today.toMs(),
            limit = SAMPLE_LIMIT,
        ).map { done ->
            StartSample(
                dayOfWeek = done.date.toLocalDate().dayOfWeek,
                startInDay = done.startInDay,
            )
        }

        return estimateHabitualStart(
            samples = samples,
            today = today.dayOfWeek,
            shift = notificationSettings.current.habitualStartShift,
        )
    }
}
