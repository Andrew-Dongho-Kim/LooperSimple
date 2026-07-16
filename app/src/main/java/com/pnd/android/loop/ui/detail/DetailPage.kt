package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingDown
import androidx.compose.material.icons.automirrored.outlined.TrendingFlat
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material.icons.outlined.Repeat
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.common.NO_REPEAT
import com.pnd.android.loop.ui.common.SimpleAd
import com.pnd.android.loop.ui.common.SimpleAppBar
import com.pnd.android.loop.ui.home.LoopOnOffSwitch
import com.pnd.android.loop.ui.statisctics.computeStreak
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onPrimary
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS_SUNDAY_FIRST
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.formatYearMonthDateDays
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.toLocalDate
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import kotlin.math.ceil
import kotlin.math.roundToInt

private val adId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-2341430172816266/5981213088"
}

/** Inner padding shared by every card on the detail screen. */
private val CardPadding = 20.dp

/** Vertical gap between rows inside a card (info rows, header → content). */
private val CardInnerSpacing = 16.dp


@Composable
fun DetailPage(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val loop by detailViewModel.loop.collectAsState(LoopVo.create())
    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.background),
        topBar = {
            SimpleAppBar(
                modifier = Modifier
                    .statusBarsPadding(),
                title = loop.title,
                onNavigateUp = onNavigateUp,
                actions = {
                    LoopOnOffSwitch(
                        modifier = Modifier.padding(end = Dimens.screenHorizontalPadding),
                        enabled = loop.enabled,
                        onEnabled = { enabled -> detailViewModel.enableLoop(loop, enabled) }
                    )
                }
            )
        },
    )
    { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            DetailPageContent(
                detailViewModel = detailViewModel,
                loop = loop,
            )
        }
    }
}

@Composable
private fun DetailPageContent(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    // 성취 요약(A)과 달력(C)이 같은 응답 기록을 공유하므로 한 번만 구독해 아래로 내려준다.
    val responses by detailViewModel.allResponses.collectAsState(initial = emptyList())

    Column(
        modifier = modifier
            .padding(horizontal = Dimens.screenHorizontalPadding)
            .padding(top = Dimens.contentPadding, bottom = 48.dp)
            .fillMaxWidth()
            .verticalScroll(state = rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(Dimens.sectionSpacing),
    ) {
        // 1. 성취 요약(A: 코치형) — 연속 달성 + 이번 주 피드백 + 베스트 요일
        AchievementCard(
            responses = responses,
            loop = loop,
        )

        // 2. 응답 요약(A안: 구성 비율 막대) — 아래 추세 차트의 도입부
        LoopRateSummary(
            detailViewModel = detailViewModel,
            loop = loop,
        )

        // 3. 추세 차트(C안: 인사이트 요약형) — 일별 추세 / 월별 / 요일별
        DailyDoneRateChart(
            responses = responses,
            loop = loop,
        )

        MonthlyDoneRateChart(
            responses = responses,
            loop = loop,
        )

        DayOfWeekDoneRateChart(
            responses = responses,
            loop = loop,
        )

        // 4. 기록(C: 저널형) — 달력에서 날짜를 골라 상태 확인 + 회고 메모 (시간 설정 바로 위)
        JournalCard(
            detailViewModel = detailViewModel,
            responses = responses,
            loop = loop,
        )

        // 5. 스케줄(시간 설정)
        ScheduleCard(loop = loop)

        // 6. 광고는 콘텐츠 흐름을 끊지 않도록 맨 아래로
        SimpleAd(adId = adId)

        DetailPageDebug(
            detailViewModel = detailViewModel,
            loop = loop,
        )
    }
}

/**
 * Shared container for every section on the detail screen: a lifted surface with soft
 * rounding and a hairline border so cards read as a distinct layer over the background in
 * both light and dark themes (mirrors the card styling used on the home screen).
 */
@Composable
private fun DetailCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundShapes.large,
        color = AppColor.surfaceElevated,
        border = BorderStroke(
            width = 0.5.dp,
            color = AppColor.onSurface.copy(alpha = 0.08f),
        ),
    ) {
        Column(
            modifier = Modifier.padding(all = CardPadding),
            content = content,
        )
    }
}

/** Bold title shown at the top of a card to label the section below it. */
@Composable
private fun SectionHeader(
    modifier: Modifier = Modifier,
    title: String,
) {
    Text(
        modifier = modifier,
        text = title,
        style = AppTypography.titleMedium.copy(
            color = AppColor.onSurface,
            fontWeight = FontWeight.Bold,
        ),
    )
}

/**
 * 시간 설정 카드(A안: 하루 타임라인).
 * 하루 중 활동 구간을 24시간 막대 위에 강조해 "언제 하는 습관인지"를 공간적으로 보여준다.
 * 그 아래 활동 요일 칩과 반복·시작일 메타를 답는다.
 * '언제든지' 루프는 고정 구간이 없으므로 막대 대신 "언제든지" 요약만 보여준다.
 */
@Composable
private fun ScheduleCard(
    modifier: Modifier = Modifier,
    loop: LoopBase,
) {
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
    val dayCount = LocalDate.now().toEpochDay() - createdDate.toEpochDay() + 1
    val accent = Color(loop.color).compositeOverOnSurface()

    DetailCard(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.detail_schedule))

        if (loop.isAnyTime) {
            Text(
                modifier = Modifier.padding(top = CardInnerSpacing),
                text = stringResource(id = R.string.anytime),
                style = AppTypography.bodyLarge.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
        } else {
            // 활동 구간 요약: "오전 7:00 – 오전 7:30 · 30분"
            val durationMinutes = ((loop.endInDay - loop.startInDay) / MS_1MIN).toInt().coerceAtLeast(0)
            val durationText = if (durationMinutes >= 60) {
                stringResource(id = R.string.stat_duration_hm, durationMinutes / 60, durationMinutes % 60)
            } else {
                stringResource(id = R.string.stat_duration_m, durationMinutes)
            }
            Text(
                modifier = Modifier.padding(top = CardInnerSpacing),
                text = "${loop.startInDay.formatHourMinute()} – ${loop.endInDay.formatHourMinute()} · $durationText",
                style = AppTypography.bodyLarge.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )

            DayTimeline(
                modifier = Modifier
                    .padding(top = 14.dp)
                    .fillMaxWidth(),
                startFraction = loop.startInDay.toFloat() / MS_1DAY,
                endFraction = loop.endInDay.toFloat() / MS_1DAY,
                accent = accent,
            )
            HourTicks(modifier = Modifier.padding(top = 6.dp))
        }

        ActiveDaysRow(
            modifier = Modifier.padding(top = 18.dp),
            activeDays = loop.activeDays,
        )

        Column(
            modifier = Modifier.padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            if (loop.interval != NO_REPEAT) {
                InfoRow(
                    icon = Icons.Outlined.Repeat,
                    label = stringResource(id = R.string.detail_repeat),
                    value = intervalString(loop.interval),
                )
            }
            InfoRow(
                icon = Icons.Outlined.DateRange,
                label = stringResource(id = R.string.created_date),
                value = createdDate.formatYearMonthDateDays(),
                trailing = stringResource(id = R.string.n_days, dayCount),
            )
        }
    }
}

/**
 * 하루(24시간)를 가로 막대로 놓고 활동 구간([startFraction]~[endFraction], 0f..1f)만 강조색으로 칠한다.
 * 30분 같은 짧은 구간도 사라지지 않도록 최소 폭을 보장하며, 시작 위치는 실제 시각에 맞춘다.
 */
@Composable
private fun DayTimeline(
    modifier: Modifier = Modifier,
    startFraction: Float,
    endFraction: Float,
    accent: Color,
) {
    val before = startFraction.coerceIn(0f, 1f)
    val window = (endFraction - startFraction).coerceIn(0.03f, 1f - before)
    val after = (1f - before - window).coerceAtLeast(0f)

    Row(
        modifier = modifier
            .height(12.dp)
            .clip(CircleShape)
            .background(AppColor.surfaceContainer),
    ) {
        if (before > 0f) {
            Box(modifier = Modifier.weight(before).fillMaxHeight())
        }
        Box(
            modifier = Modifier
                .weight(window)
                .fillMaxHeight()
                .background(accent),
        )
        if (after > 0f) {
            Box(modifier = Modifier.weight(after).fillMaxHeight())
        }
    }
}

/** 타임라인 아래 0·6·12·18·24시 눈금 라벨. */
@Composable
private fun HourTicks(
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        listOf(0, 6, 12, 18, 24).forEach { hour ->
            Text(
                text = "$hour",
                style = AppTypography.labelSmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** A single "icon · label … value" line used inside [ScheduleCard]. */
@Composable
private fun InfoRow(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    value: String,
    trailing: String? = null,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = AppColor.onSurface.copy(alpha = 0.5f),
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 12.dp),
            text = label,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.6f),
            ),
        )

        Spacer(modifier = Modifier.weight(1f))

        Text(
            text = value,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        if (trailing != null) {
            Text(
                modifier = Modifier.padding(start = 8.dp),
                text = trailing,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
        }
    }
}

/** The seven weekdays as compact chips; active days are filled and tinted with the accent. */
@Composable
private fun ActiveDaysRow(
    modifier: Modifier = Modifier,
    activeDays: Int,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        ABB_DAYS.forEachIndexed { index, dayResId ->
            val selected = activeDays.isOn(LoopDay.fromIndex(index))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(30.dp)
                    .clip(CircleShape)
                    .background(
                        color = if (selected) {
                            AppColor.primary.copy(alpha = 0.14f)
                        } else {
                            AppColor.surfaceContainer
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(id = dayResId),
                    style = AppTypography.labelMedium.copy(
                        color = if (selected) {
                            AppColor.primary
                        } else {
                            AppColor.onSurface.copy(alpha = 0.4f)
                        },
                    ),
                )
            }
        }
    }
}

/**
 * 응답 구성 요약(A안: 구성 비율 막대).
 * 완료 · 건너뜀 · 미응답의 비중을 하나의 가로 막대로 보여주고, 아래 범례에 개수와 비율을 답는다.
 * 세 값의 합이 전체(활성 응답 대상)와 같아 "전체가 어떻게 나뉘는가"가 한눈에 읽히며,
 * 아래 추세 차트 3종의 도입부 역할을 한다.
 */
@Composable
private fun LoopRateSummary(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
) {
    val total by detailViewModel.allEnabledCount.collectAsState(initial = 0)
    val doneCount by detailViewModel.doneCount.collectAsState(initial = 0)
    val skipCount by detailViewModel.skipCount.collectAsState(initial = 0)
    val respondCount by detailViewModel.respondCount.collectAsState(initial = 0)

    // 미응답 = 전체 − 응답(완료+건너뜀). 데이터 경합으로 음수가 되지 않도록 방어한다.
    val noResponseCount = (total - respondCount).coerceAtLeast(0)

    val doneColor = Color(loop.color).compositeOverOnSurface()
    val skipColor = AppColor.onSurface.copy(alpha = 0.35f)

    DetailCard(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.detail_response_summary))

        CompositionBar(
            modifier = Modifier.padding(top = CardInnerSpacing),
            doneCount = doneCount,
            skipCount = skipCount,
            noResponseCount = noResponseCount,
            doneColor = doneColor,
            skipColor = skipColor,
        )

        Column(
            modifier = Modifier.padding(top = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            RateLegendRow(
                label = stringResource(id = R.string.detail_rate_done),
                count = doneCount,
                total = total,
                swatchColor = doneColor,
                outlined = false,
            )
            RateLegendRow(
                label = stringResource(id = R.string.detail_rate_skip),
                count = skipCount,
                total = total,
                swatchColor = skipColor,
                outlined = false,
            )
            RateLegendRow(
                label = stringResource(id = R.string.detail_rate_no_response),
                count = noResponseCount,
                total = total,
                // 미응답은 채우지 않고 테두리만 있는 표식으로, 막대의 빈 트랙과 대응시킨다.
                swatchColor = AppColor.onSurface.copy(alpha = 0.3f),
                outlined = true,
            )
        }
    }
}

/**
 * 완료 · 건너뜀 · 미응답을 이어 붙인 하나의 알약형 막대. 각 구간 너비는 개수에 비례한다.
 * 개수가 0인 구간은 그리지 않아(가중치 0 방지) 크래시를 막고, 미응답 구간은 색을 칠하지 않아
 * 트랙(빈 배경)이 그대로 드러나게 표현한다.
 */
@Composable
private fun CompositionBar(
    modifier: Modifier = Modifier,
    doneCount: Int,
    skipCount: Int,
    noResponseCount: Int,
    doneColor: Color,
    skipColor: Color,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(14.dp)
            .clip(CircleShape)
            .background(AppColor.surfaceContainer),
    ) {
        if (doneCount > 0) {
            Box(
                modifier = Modifier
                    .weight(doneCount.toFloat())
                    .fillMaxHeight()
                    .background(doneColor),
            )
        }
        if (skipCount > 0) {
            Box(
                modifier = Modifier
                    .weight(skipCount.toFloat())
                    .fillMaxHeight()
                    .background(skipColor),
            )
        }
        if (noResponseCount > 0) {
            Box(modifier = Modifier.weight(noResponseCount.toFloat()))
        }
    }
}

/** 막대 색과 짝을 이루는 범례 한 줄: 색 표식 · 라벨 · 개수 · 비율. */
@Composable
private fun RateLegendRow(
    label: String,
    count: Int,
    total: Int,
    swatchColor: Color,
    outlined: Boolean,
) {
    val percent = if (total == 0) 0 else (count.toFloat() / total * 100).roundToInt()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(CircleShape)
                .then(
                    if (outlined) {
                        Modifier.border(width = 0.5.dp, color = swatchColor, shape = CircleShape)
                    } else {
                        Modifier.background(swatchColor)
                    }
                ),
        )
        Text(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            text = label,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.7f),
            ),
        )
        Text(
            text = "$count",
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.45f),
            ),
        )
        Text(
            modifier = Modifier.widthIn(min = 44.dp),
            text = "$percent%",
            textAlign = TextAlign.End,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// A. 코치형 성취 요약
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 숫자를 나열하는 대신 "지금 잘하고 있는가"를 먼저 보여주는 히어로 카드.
 * 연속 달성(스트릭)을 크게 강조하고, 최근 7일의 흐름을 점으로, 이번 주 성과를 한 줄
 * 피드백으로 요약한다. (요일별 인사이트는 아래 '요일별 완료율' 카드가 담당한다.)
 */
@Composable
private fun AchievementCard(
    modifier: Modifier = Modifier,
    responses: List<LoopDoneVo>,
    loop: LoopBase,
) {
    val accent = Color(loop.color).compositeOverOnSurface()
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }

    // 완료 상태를 날짜로 인덱싱해 두면 스트릭·주간·달력이 모두 빠르게 조회할 수 있다.
    val doneStateByDate = remember(responses) {
        responses.associate { it.date.toLocalDate() to it.done }
    }
    val streak = remember(responses) {
        computeStreak(doneDates = responses.filter { it.isDone() }.map { it.date.toLocalDate() })
    }
    val weekly = remember(doneStateByDate, loop.activeDays, createdDate) {
        computeWeeklyProgress(
            doneStateByDate = doneStateByDate,
            activeDays = loop.activeDays,
            createdDate = createdDate,
        )
    }

    DetailCard(modifier = modifier) {
        // 연속 달성(히어로): 불꽃 아이콘 + 큰 숫자 + 최고 기록
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = Icons.Outlined.LocalFireDepartment,
                tint = accent,
                contentDescription = null,
            )
            Column(modifier = Modifier.padding(start = 14.dp)) {
                Text(
                    text = stringResource(id = R.string.detail_streak_current_days, streak.current),
                    style = AppTypography.headlineMedium.copy(color = AppColor.onSurface),
                )
                Text(
                    modifier = Modifier.padding(top = 2.dp),
                    text = stringResource(id = R.string.detail_streak_best, streak.longest),
                    style = AppTypography.bodySmall.copy(
                        color = AppColor.onSurface.copy(alpha = 0.5f),
                    ),
                )
            }
        }

        // 최근 7일 흐름(왼→오: 6일 전 → 오늘)
        WeekDotsRow(
            modifier = Modifier.padding(top = 18.dp),
            doneStateByDate = doneStateByDate,
            createdDate = createdDate,
            activeDays = loop.activeDays,
            accent = accent,
        )

        // 이번 주 한 줄 피드백
        WeeklyFeedbackRow(
            modifier = Modifier.padding(top = 16.dp),
            weekly = weekly,
        )
    }
}

/** 최근 7일을 작은 알약으로 늘어놓아, 완료(강조색)·건너뜀(옅음)·그 외를 색으로 구분한다. */
@Composable
private fun WeekDotsRow(
    modifier: Modifier = Modifier,
    doneStateByDate: Map<LocalDate, Int>,
    createdDate: LocalDate,
    activeDays: Int,
    accent: Color,
) {
    val today = LocalDate.now()
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        (6 downTo 0).forEach { offset ->
            val date = today.minusDays(offset.toLong())
            val fill = when (doneStateByDate[date]) {
                DoneState.DONE -> accent
                DoneState.SKIP -> AppColor.onSurface.copy(alpha = 0.25f)
                else -> AppColor.surfaceContainer
            }
            // 생성 이전이거나 비활성 요일은 흐리게 처리해 "해당 없음"을 구분한다.
            val isActive = !date.isBefore(createdDate) && activeDays.isOn(dayForLoop(date))
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(fill)
                    .alpha(if (isActive) 1f else 0.4f),
            )
        }
    }
}

/** 이번 주 완료 성과 + 지난주 대비 추세를 한 줄로 요약하는, 옅게 채운 배너. */
@Composable
private fun WeeklyFeedbackRow(
    modifier: Modifier = Modifier,
    weekly: WeeklyProgress,
) {
    val icon: ImageVector
    val tint: Color
    val trendRes: Int
    when {
        weekly.trend > 0 -> {
            icon = Icons.AutoMirrored.Outlined.TrendingUp
            tint = AppColor.primary
            trendRes = R.string.detail_week_trend_up
        }

        weekly.trend < 0 -> {
            icon = Icons.AutoMirrored.Outlined.TrendingDown
            tint = AppColor.error
            trendRes = R.string.detail_week_trend_down
        }

        else -> {
            icon = Icons.AutoMirrored.Outlined.TrendingFlat
            tint = AppColor.onSurface.copy(alpha = 0.6f)
            trendRes = R.string.detail_week_trend_same
        }
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            .padding(all = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(20.dp),
            imageVector = icon,
            tint = tint,
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 10.dp)) {
            Text(
                text = stringResource(
                    id = R.string.detail_week_progress,
                    weekly.doneThisWeek,
                    weekly.activeThisWeek,
                ),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                modifier = Modifier.padding(top = 2.dp),
                text = stringResource(id = trendRes),
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.55f),
                ),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// C. 저널형 달력 · 회고
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 한 달 달력을 중심에 두고, 날짜를 누르면 그날의 상태와 회고 메모를 보고 남길 수 있는 카드.
 * 완료한 날은 잔디처럼 옅은 강조색으로 칠하고, 메모가 있는 날은 우상단에 점 마커를 얹는다.
 */
@Composable
private fun JournalCard(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    responses: List<LoopDoneVo>,
    loop: LoopBase,
) {
    val today = LocalDate.now()
    val accent = Color(loop.color).compositeOverOnSurface()
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
    val doneStateByDate = remember(responses) {
        responses.associate { it.date.toLocalDate() to it.done }
    }

    // 메모가 있는 날짜 집합. 회고가 추가/삭제되면 즉시 마커가 갱신된다.
    val retrospects by detailViewModel.retrospects.collectAsState(initial = emptyList())
    val memoDates = remember(retrospects) {
        retrospects.filter { !it.text.isNullOrBlank() }.map { it.date.toLocalDate() }.toSet()
    }

    var selectedDate by remember { mutableStateOf(today) }
    var visibleMonth by remember { mutableStateOf(YearMonth.from(today)) }

    DetailCard(modifier = modifier) {
        SectionHeader(title = stringResource(id = R.string.daily_record))
        Text(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(id = R.string.detail_journal_hint),
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.45f),
            ),
        )

        MonthNavigator(
            modifier = Modifier.padding(top = CardInnerSpacing),
            visibleMonth = visibleMonth,
            canGoPrev = visibleMonth.isAfter(YearMonth.from(createdDate)),
            canGoNext = visibleMonth.isBefore(YearMonth.from(today)),
            onPrev = { visibleMonth = visibleMonth.minusMonths(1) },
            onNext = { visibleMonth = visibleMonth.plusMonths(1) },
        )

        MonthCalendar(
            modifier = Modifier.padding(top = 12.dp),
            visibleMonth = visibleMonth,
            doneStateByDate = doneStateByDate,
            memoDates = memoDates,
            createdDate = createdDate,
            today = today,
            selectedDate = selectedDate,
            accent = accent,
            onSelect = { date -> selectedDate = date },
        )

        SelectedDayPanel(
            modifier = Modifier.padding(top = 18.dp),
            detailViewModel = detailViewModel,
            selectedDate = selectedDate,
            doneState = doneStateByDate[selectedDate],
            accent = accent,
        )
    }
}

/** 이전/다음 달로 이동하는 헤더. 범위(생성월 ~ 이번 달)를 벗어나는 화살표는 흐리게 비활성화한다. */
@Composable
private fun MonthNavigator(
    modifier: Modifier = Modifier,
    visibleMonth: YearMonth,
    canGoPrev: Boolean,
    canGoNext: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NavArrow(icon = Icons.Outlined.ChevronLeft, enabled = canGoPrev, onClick = onPrev)
        Text(
            modifier = Modifier.weight(1f),
            text = visibleMonth.atDay(1).formatYearMonth(),
            textAlign = TextAlign.Center,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )
        NavArrow(icon = Icons.Outlined.ChevronRight, enabled = canGoNext, onClick = onNext)
    }
}

@Composable
private fun NavArrow(
    icon: ImageVector,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Icon(
        modifier = Modifier
            .size(30.dp)
            .clip(CircleShape)
            .clickable(enabled = enabled, onClick = onClick)
            .padding(4.dp)
            .alpha(if (enabled) 1f else 0.25f),
        imageVector = icon,
        tint = AppColor.onSurface.copy(alpha = 0.7f),
        contentDescription = null,
    )
}

/** 일요일 시작 한 달 달력 그리드. 완료 히트 배경·오늘/선택 강조·메모 마커를 셀마다 그린다. */
@Composable
private fun MonthCalendar(
    modifier: Modifier = Modifier,
    visibleMonth: YearMonth,
    doneStateByDate: Map<LocalDate, Int>,
    memoDates: Set<LocalDate>,
    createdDate: LocalDate,
    today: LocalDate,
    selectedDate: LocalDate,
    accent: Color,
    onSelect: (LocalDate) -> Unit,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        // 요일 헤더(일 ~ 토). 주말은 요일 색을 옅게 입힌다.
        Row(modifier = Modifier.fillMaxWidth()) {
            DAYS_WITH_3CHARS_SUNDAY_FIRST.forEachIndexed { index, dayResId ->
                val dayOfWeek = DayOfWeek.of(if (index == 0) 7 else index)
                Text(
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    text = stringResource(id = dayResId),
                    style = AppTypography.labelSmall.copy(
                        color = dayOfWeek.color().copy(alpha = 0.7f),
                    ),
                )
            }
        }

        // 1일이 놓일 위치까지의 빈 칸 수(일요일 시작 기준)와 필요한 주(행) 수를 계산한다.
        val leadingBlanks = visibleMonth.atDay(1).dayOfWeek.value % 7
        val lengthOfMonth = visibleMonth.lengthOfMonth()
        val rows = ceil((leadingBlanks + lengthOfMonth) / 7f).toInt()

        var cellIndex = 0
        Column(modifier = Modifier.padding(top = 8.dp)) {
            repeat(rows) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp),
                ) {
                    repeat(7) {
                        val dayOfMonth = cellIndex - leadingBlanks + 1
                        if (dayOfMonth in 1..lengthOfMonth) {
                            val date = visibleMonth.atDay(dayOfMonth)
                            CalendarDayCell(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                date = date,
                                state = doneStateByDate[date],
                                hasMemo = date in memoDates,
                                isToday = date == today,
                                isSelected = date == selectedDate,
                                // 미래·생성 이전 날짜는 선택할 수 없다.
                                selectable = !date.isAfter(today) && !date.isBefore(createdDate),
                                accent = accent,
                                onSelect = onSelect,
                            )
                        } else {
                            Box(modifier = Modifier.weight(1f))
                        }
                        cellIndex++
                    }
                }
            }
        }
    }
}

/** 달력의 하루 칸: 완료 히트 배경 + 날짜 숫자 + (선택 시)외곽 링 + (메모 시)점 마커. */
@Composable
private fun CalendarDayCell(
    modifier: Modifier = Modifier,
    date: LocalDate,
    state: Int?,
    hasMemo: Boolean,
    isToday: Boolean,
    isSelected: Boolean,
    selectable: Boolean,
    accent: Color,
    onSelect: (LocalDate) -> Unit,
) {
    val isDark = isSystemInDarkTheme()
    // 완료일은 잔디처럼 옅은 강조색으로 칠하고, 건너뛴 날은 아주 옅은 회색으로 표시한다.
    // 다크 모드는 배경이 어두워 같은 알파라도 옅게 보이므로 조금 더 진하게 칠한다.
    val background = when (state) {
        DoneState.DONE -> accent.copy(alpha = if (isDark) 0.40f else 0.22f)
        DoneState.SKIP -> AppColor.onSurface.copy(alpha = 0.10f)
        else -> Color.Transparent
    }
    val textColor = when {
        !selectable -> AppColor.onSurface.copy(alpha = 0.3f)
        state == DoneState.DONE -> accent
        else -> AppColor.onSurface
    }

    Box(
        modifier = modifier
            .padding(2.dp)
            .clip(CircleShape)
            .background(background)
            .then(
                // 선택된 날은 면이 아니라 외곽 링으로 표시해 히트 배경과 겹치지 않게 한다.
                if (isSelected) {
                    Modifier.border(width = 1.5.dp, color = accent, shape = CircleShape)
                } else {
                    Modifier
                }
            )
            .clickable(enabled = selectable) { onSelect(date) },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "${date.dayOfMonth}",
            style = AppTypography.bodySmall.copy(
                color = textColor,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
            ),
        )

        if (hasMemo) {
            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 4.dp, end = 5.dp)
                    .size(5.dp)
                    .clip(CircleShape)
                    .background(AppColor.onSurface.copy(alpha = 0.55f)),
            )
        }
    }
}

/**
 * 달력에서 고른 날짜의 상태(완료/건너뜀/기록 없음)와 그날의 회고 메모를 편집하는 패널.
 * 날짜가 바뀌면 해당 날짜의 메모를 다시 불러온다.
 */
@Composable
private fun SelectedDayPanel(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    selectedDate: LocalDate,
    doneState: Int?,
    accent: Color,
) {
    // 선택 날짜가 바뀌면 입력값을 초기화하고, 저장된 메모를 비동기로 불러온다.
    var memo by remember(selectedDate) { mutableStateOf("") }
    var loaded by remember(selectedDate) { mutableStateOf(false) }
    LaunchedEffect(selectedDate) {
        memo = detailViewModel.retrospectOf(selectedDate) ?: ""
        loaded = true
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                modifier = Modifier.weight(1f),
                text = selectedDate.formatMonthDateDay(),
                style = AppTypography.titleSmall.copy(color = AppColor.onSurface),
            )
            DayStatusChip(doneState = doneState, accent = accent)
        }

        JournalMemoField(
            modifier = Modifier.padding(top = 12.dp),
            value = memo,
            onValueChange = { memo = it },
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = R.string.retrospect_char_count, memo.length),
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.4f),
                ),
            )
            Spacer(modifier = Modifier.weight(1f))
            SaveMemoButton(
                enabled = loaded,
                onClick = { detailViewModel.saveRetrospect(selectedDate, memo) },
            )
        }
    }
}

/** 선택한 날짜의 상태를 알약으로 보여준다(완료=강조색 / 건너뜀 / 기록 없음). */
@Composable
private fun DayStatusChip(
    doneState: Int?,
    accent: Color,
) {
    val labelRes: Int
    val color: Color
    when (doneState) {
        DoneState.DONE -> {
            labelRes = R.string.done
            color = accent
        }

        DoneState.SKIP -> {
            labelRes = R.string.skip
            color = AppColor.onSurface.copy(alpha = 0.6f)
        }

        else -> {
            labelRes = R.string.detail_day_no_record
            color = AppColor.onSurface.copy(alpha = 0.4f)
        }
    }
    Text(
        modifier = Modifier
            .clip(CircleShape)
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        text = stringResource(id = labelRes),
        style = AppTypography.labelMedium.copy(color = color),
    )
}

/** 회고 입력창. 홈 화면의 회고 입력과 같은 톤이되, 스크롤 중 키보드가 튀지 않도록 자동 포커스는 두지 않는다. */
@Composable
private fun JournalMemoField(
    modifier: Modifier = Modifier,
    value: String,
    onValueChange: (String) -> Unit,
) {
    BasicTextField(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp)
            .clip(RoundShapes.medium)
            .background(AppColor.surfaceContainer)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.12f),
                shape = RoundShapes.medium,
            )
            .padding(all = 14.dp),
        value = value,
        onValueChange = onValueChange,
        cursorBrush = SolidColor(AppColor.primary),
        textStyle = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface,
            lineHeight = 20.sp,
        ),
        decorationBox = { innerTextField ->
            if (value.isEmpty()) {
                Text(
                    text = stringResource(id = R.string.retrospect_hint),
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.4f),
                    ),
                )
            }
            innerTextField()
        },
    )
}

/** 회고 메모 저장 버튼(강조색 알약). */
@Composable
private fun SaveMemoButton(
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Text(
        modifier = Modifier
            .clip(RoundShapes.medium)
            .background(AppColor.primary.copy(alpha = if (enabled) 1f else 0.4f))
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 8.dp),
        text = stringResource(id = R.string.save),
        style = AppTypography.labelLarge.copy(color = AppColor.onPrimary),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 성취 요약 계산용 순수 함수 (Compose 와 무관하게 테스트 가능)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 이번 주(최근 7일) 성과 요약.
 * @param doneThisWeek 최근 7일 중 완료한 날 수
 * @param activeThisWeek 최근 7일 중 이 루프가 활동해야 하는(활성 요일·생성 이후) 날 수
 * @param trend 지난 7일 대비 완료 수 변화(양수=개선, 0=유지, 음수=주춤)
 */
private data class WeeklyProgress(
    val doneThisWeek: Int,
    val activeThisWeek: Int,
    val trend: Int,
)

private fun computeWeeklyProgress(
    doneStateByDate: Map<LocalDate, Int>,
    activeDays: Int,
    createdDate: LocalDate,
    today: LocalDate = LocalDate.now(),
): WeeklyProgress {
    val last7 = (0..6).map { today.minusDays(it.toLong()) }
    val prev7 = (7..13).map { today.minusDays(it.toLong()) }

    val doneThisWeek = last7.count { doneStateByDate[it] == DoneState.DONE }
    val donePrevWeek = prev7.count { doneStateByDate[it] == DoneState.DONE }
    val activeThisWeek = last7.count {
        !it.isBefore(createdDate) && activeDays.isOn(dayForLoop(it))
    }

    return WeeklyProgress(
        doneThisWeek = doneThisWeek,
        activeThisWeek = activeThisWeek,
        trend = doneThisWeek.compareTo(donePrevWeek),
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// 추세 차트 3종 (C안: 인사이트 요약형)
// 무거운 차트 라이브러리 대신, 각 카드가 "한 줄 결론 + 가벼운 네이티브 시각화"로 구성된다.
// 모든 지표는 이미 구독 중인 응답 기록(responses)에서 메모리 내에서 계산해, DB 왕복이 없다.
// ─────────────────────────────────────────────────────────────────────────────

/** 최근 완료율 추세(7일 롤링)를 스파크라인 + "오르고/주춤" 한 줄로 요약한다. */
@Composable
private fun DailyDoneRateChart(
    modifier: Modifier = Modifier,
    responses: List<LoopDoneVo>,
    loop: LoopBase,
) {
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
    val trend = remember(responses, createdDate) {
        computeDailyTrend(responses = responses, createdDate = createdDate)
    } ?: return

    val accent = Color(loop.color).compositeOverOnSurface()
    val icon: ImageVector
    val tint: Color
    val headlineRes: Int
    when {
        trend.deltaPercent > 0 -> {
            icon = Icons.AutoMirrored.Outlined.TrendingUp
            tint = AppColor.primary
            headlineRes = R.string.detail_trend_daily_up
        }

        trend.deltaPercent < 0 -> {
            icon = Icons.AutoMirrored.Outlined.TrendingDown
            tint = AppColor.error
            headlineRes = R.string.detail_trend_daily_down
        }

        else -> {
            icon = Icons.AutoMirrored.Outlined.TrendingFlat
            tint = AppColor.onSurface.copy(alpha = 0.6f)
            headlineRes = R.string.detail_trend_daily_steady
        }
    }

    DetailCard(modifier = modifier) {
        TrendHeaderRow(
            icon = icon,
            iconTint = tint,
            text = stringResource(id = headlineRes),
            trailing = formatDeltaPercent(trend.deltaPercent),
            trailingColor = tint,
        )
        Sparkline(
            modifier = Modifier
                .padding(top = 16.dp)
                .fillMaxWidth()
                .height(56.dp),
            values = trend.rates,
            color = accent,
        )
    }
}

/** 이번 달 완료율과 지난달 대비 변화를, 최근 몇 달의 막대와 함께 보여준다. */
@Composable
private fun MonthlyDoneRateChart(
    modifier: Modifier = Modifier,
    responses: List<LoopDoneVo>,
    loop: LoopBase,
) {
    val months = remember(responses) { computeMonthlyRates(responses) }
    if (months.isEmpty()) return

    val accent = Color(loop.color).compositeOverOnSurface()
    val current = months.last()
    val thisMonthPercent = (current.second * 100).roundToInt()
    val delta = months.getOrNull(months.size - 2)?.let { prev ->
        ((current.second - prev.second) * 100).roundToInt()
    }

    val icon: ImageVector
    val tint: Color
    val headline: String
    when {
        delta == null -> {
            icon = Icons.AutoMirrored.Outlined.TrendingFlat
            tint = AppColor.onSurface.copy(alpha = 0.6f)
            headline = stringResource(id = R.string.detail_trend_this_month, thisMonthPercent)
        }

        delta > 0 -> {
            icon = Icons.AutoMirrored.Outlined.TrendingUp
            tint = AppColor.primary
            headline = stringResource(id = R.string.detail_trend_monthly_up, thisMonthPercent)
        }

        delta < 0 -> {
            icon = Icons.AutoMirrored.Outlined.TrendingDown
            tint = AppColor.error
            headline = stringResource(id = R.string.detail_trend_monthly_down, thisMonthPercent)
        }

        else -> {
            icon = Icons.AutoMirrored.Outlined.TrendingFlat
            tint = AppColor.onSurface.copy(alpha = 0.6f)
            headline = stringResource(id = R.string.detail_trend_monthly_steady, thisMonthPercent)
        }
    }

    val entries = months.map { (yearMonth, rate) ->
        BarEntry(
            label = stringResource(id = ABB_MONTHS[yearMonth.monthValue - 1]),
            fraction = rate,
            highlighted = yearMonth == current.first,
        )
    }

    DetailCard(modifier = modifier) {
        TrendHeaderRow(
            icon = icon,
            iconTint = tint,
            text = headline,
            trailing = if (delta == null) "$thisMonthPercent%" else formatDeltaPercent(delta),
            trailingColor = if (delta == null) accent else tint,
        )
        MiniBarChart(
            modifier = Modifier.padding(top = 16.dp),
            entries = entries,
            accent = accent,
        )
    }
}

/** 요일별 완료율을 7개 막대로 보여주고, 가장 잘 지키는 요일을 강조 + 한 줄로 요약한다. */
@Composable
private fun DayOfWeekDoneRateChart(
    modifier: Modifier = Modifier,
    responses: List<LoopDoneVo>,
    loop: LoopBase,
) {
    val rates = remember(responses) { computeWeekdayRates(responses) }
    val bestIndex = rates
        .withIndex()
        .filter { it.value != null }
        .maxByOrNull { it.value!! }
        ?.index ?: return

    val accent = Color(loop.color).compositeOverOnSurface()
    val bestRate = rates[bestIndex] ?: 0f
    val bestDayName = stringResource(id = DAYS_WITH_3CHARS_SUNDAY_FIRST[bestIndex])

    val entries = rates.mapIndexed { index, rate ->
        BarEntry(
            label = stringResource(id = DAYS_WITH_3CHARS_SUNDAY_FIRST[index]),
            fraction = rate ?: 0f,
            highlighted = index == bestIndex,
        )
    }

    DetailCard(modifier = modifier) {
        TrendHeaderRow(
            icon = Icons.Outlined.EmojiEvents,
            iconTint = AppColor.onSurface.copy(alpha = 0.6f),
            text = stringResource(id = R.string.detail_best_weekday, bestDayName),
            trailing = "${(bestRate * 100).roundToInt()}%",
            trailingColor = accent,
        )
        MiniBarChart(
            modifier = Modifier.padding(top = 16.dp),
            entries = entries,
            accent = accent,
        )
    }
}

/** 추세 카드 상단의 "아이콘 · 한 줄 결론 · 우측 수치" 헤더. */
@Composable
private fun TrendHeaderRow(
    icon: ImageVector,
    iconTint: Color,
    text: String,
    trailing: String,
    trailingColor: Color,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            modifier = Modifier.size(18.dp),
            imageVector = icon,
            tint = iconTint,
            contentDescription = null,
        )
        Text(
            modifier = Modifier
                .padding(start = 10.dp)
                .weight(1f),
            text = text,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Medium,
            ),
        )
        Text(
            text = trailing,
            style = AppTypography.bodyMedium.copy(
                color = trailingColor,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/** 완료율(0f..1f) 시계열을 선으로 잇는 가벼운 스파크라인. 위로 갈수록 완료율이 높다. */
@Composable
private fun Sparkline(
    modifier: Modifier = Modifier,
    values: List<Float>,
    color: Color,
) {
    Canvas(modifier = modifier) {
        if (values.size < 2) return@Canvas

        val stepX = size.width / (values.size - 1)
        val path = Path()
        values.forEachIndexed { index, value ->
            val x = stepX * index
            val y = size.height * (1f - value.coerceIn(0f, 1f))
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path = path,
            color = color,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round,
                join = StrokeJoin.Round,
            ),
        )
    }
}

/** 미니 막대 하나의 데이터: 라벨 · 채움 비율(0f..1f) · 강조 여부. */
private data class BarEntry(
    val label: String,
    val fraction: Float,
    val highlighted: Boolean,
)

/** 월별·요일별 완료율을 그리는 공용 막대 차트. 강조 막대만 진하게, 나머지는 옅게 칠한다. */
@Composable
private fun MiniBarChart(
    modifier: Modifier = Modifier,
    entries: List<BarEntry>,
    accent: Color,
    barAreaHeight: Dp = 72.dp,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(barAreaHeight),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.Bottom,
        ) {
            entries.forEach { entry ->
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    verticalArrangement = Arrangement.Bottom,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            // 값이 0이어도 바닥에 아주 얇은 자국을 남겨 "칸"이 있음을 알린다.
                            .fillMaxHeight(entry.fraction.coerceIn(0.02f, 1f))
                            .clip(RoundShapes.small)
                            .background(
                                if (entry.highlighted) accent else accent.copy(alpha = 0.25f),
                            ),
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            entries.forEach { entry ->
                Text(
                    modifier = Modifier.weight(1f),
                    text = entry.label,
                    textAlign = TextAlign.Center,
                    style = AppTypography.labelSmall.copy(
                        color = if (entry.highlighted) {
                            accent
                        } else {
                            AppColor.onSurface.copy(alpha = 0.45f)
                        },
                    ),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// 추세 계산용 순수 함수 (Compose 와 무관하게 테스트 가능)
// ─────────────────────────────────────────────────────────────────────────────

/**
 * 일별 완료율 추세.
 * @param rates 각 날짜의 최근 [computeDailyTrend]의 windowDays 일 롤링 완료율(0f..1f), 과거→오늘 순
 * @param deltaPercent 약 2주 전 대비 완료율 변화(%p, 양수=상승)
 */
private data class DailyTrend(
    val rates: List<Float>,
    val deltaPercent: Int,
)

/**
 * 최근 [maxPoints]일에 대해 [windowDays]일 롤링 완료율을 계산한다.
 * 각 날짜의 값 = (창 안의 완료 수) / (창 안의 응답 대상 수). 응답 대상은 비활성(DISABLED)이 아닌 기록.
 * 기록이 전혀 없거나 구간이 너무 짧으면 null 을 돌려 카드를 숨긴다.
 */
private fun computeDailyTrend(
    responses: List<LoopDoneVo>,
    createdDate: LocalDate,
    today: LocalDate = LocalDate.now(),
    windowDays: Int = 7,
    maxPoints: Int = 60,
): DailyTrend? {
    val doneByDate = responses
        .filter { !it.isDisabled() }
        .associate { it.date.toLocalDate() to it.isDone() }
    if (doneByDate.isEmpty()) return null

    val start = maxOf(createdDate, today.minusDays((maxPoints - 1).toLong()))
    val totalDays = (today.toEpochDay() - start.toEpochDay()).toInt() + 1
    if (totalDays < 2) return null

    val rates = (0 until totalDays).map { offset ->
        val day = start.plusDays(offset.toLong())
        var enabled = 0
        var done = 0
        var cursor = day.minusDays((windowDays - 1).toLong())
        while (!cursor.isAfter(day)) {
            doneByDate[cursor]?.let { isDone ->
                enabled++
                if (isDone) done++
            }
            cursor = cursor.plusDays(1)
        }
        if (enabled == 0) 0f else done.toFloat() / enabled
    }

    // 약 2주 전 지점과 비교해 최근 추세를 %p 로 낸다(데이터가 짧으면 첫 지점과 비교).
    val referenceIndex = (rates.lastIndex - 14).coerceAtLeast(0)
    val deltaPercent = ((rates.last() - rates[referenceIndex]) * 100).roundToInt()
    return DailyTrend(rates = rates, deltaPercent = deltaPercent)
}

/**
 * 최근 [monthsBack]개월의 월별 완료율(0f..1f). 데이터가 있는 달만, 오래된 달→최신 달 순으로 담는다.
 * 완료율 = 그 달의 완료 수 / 응답 대상(비활성 제외) 수.
 */
private fun computeMonthlyRates(
    responses: List<LoopDoneVo>,
    monthsBack: Int = 6,
): List<Pair<YearMonth, Float>> {
    val enabled = responses.filter { !it.isDisabled() }
    if (enabled.isEmpty()) return emptyList()

    return enabled
        .groupBy { YearMonth.from(it.date.toLocalDate()) }
        .map { (yearMonth, records) ->
            yearMonth to records.count { it.isDone() }.toFloat() / records.size
        }
        .sortedBy { it.first }
        .takeLast(monthsBack)
}

/**
 * 요일별 완료율(0f..1f). 인덱스 0=일요일 … 6=토요일(달력 헤더와 같은 순서).
 * 그 요일에 응답 대상 기록이 하나도 없으면 해당 칸은 null.
 */
private fun computeWeekdayRates(
    responses: List<LoopDoneVo>,
): List<Float?> {
    val byDayOfWeek = responses
        .filter { !it.isDisabled() }
        .groupBy { it.date.toLocalDate().dayOfWeek }

    return (0..6).map { index ->
        val dayOfWeek = DayOfWeek.of(if (index == 0) 7 else index)
        val records = byDayOfWeek[dayOfWeek]
        if (records.isNullOrEmpty()) null
        else records.count { it.isDone() }.toFloat() / records.size
    }
}

/** 변화량을 "+12%p" / "-5%p" / "0%p" 형태의 짧은 배지 문자열로 만든다. */
private fun formatDeltaPercent(delta: Int): String =
    (if (delta > 0) "+$delta" else "$delta") + "%p"

@Composable
private fun DetailPageDebug(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase
) {
    if (!BuildConfig.DEBUG) return

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(
                color = AppColor.onSurface.copy(alpha = 0.1f),
                shape = RoundShapes.medium
            )
            .padding(all = 12.dp)
    ) {
        Text(
            text = "[DEBUG]",
            style = AppTypography.titleMedium
        )

        Text(
            modifier = Modifier.padding(top = 18.dp),
            text = "Loop id: ${loop.loopId}",
            style = AppTypography.titleMedium
        )

        val responses by detailViewModel.allResponses.collectAsState(initial = emptyList())
        LazyColumn(
            modifier = Modifier
                .padding(top = 24.dp)
                .fillMaxWidth()
                .height(100.dp)
        ) {
            items(
                items = responses,
                key = { it.date },
            ) { item ->

                Row {
                    Text(
                        text = "${item.date.toLocalDate()}"
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = when (item.done) {
                            LoopDoneVo.DoneState.DONE -> "O"
                            LoopDoneVo.DoneState.SKIP -> "."
                            LoopDoneVo.DoneState.NO_RESPONSE -> " "
                            LoopDoneVo.DoneState.DISABLED -> "D"
                            else -> "?"
                        }
                    )
                }
            }
        }
    }
}