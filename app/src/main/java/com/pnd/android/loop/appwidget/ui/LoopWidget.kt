package com.pnd.android.loop.appwidget.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.ColorFilter
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.color.ColorProvider as dayNightColorProvider
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.width
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pnd.android.loop.HomeActivity
import com.pnd.android.loop.R
import com.pnd.android.loop.appwidget.doneAction
import com.pnd.android.loop.appwidget.skipAction
import com.pnd.android.loop.appwidget.startAction
import com.pnd.android.loop.appwidget.stopAction
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.ui.home.TodayGroup
import com.pnd.android.loop.ui.home.labelResId
import com.pnd.android.loop.ui.theme.AppWidgetPalette
import com.pnd.android.loop.ui.theme.DayNightColor
import com.pnd.android.loop.util.ABB_MONTHS
import com.pnd.android.loop.util.DAYS_WITH_3CHARS
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.isTimeInLoop
import com.pnd.android.loop.util.toMs
import java.time.LocalDate
import java.time.LocalTime


// ---------------------------------------------------------------------------
// Apple-inspired widget design tokens
// A calm, layered light surface with soft rounded "wells", clear typographic
// hierarchy and tactile circular controls.
// ---------------------------------------------------------------------------
internal val WIDGET_CARD_RADIUS = 28.dp
internal val WIDGET_ROW_RADIUS = 22.dp

// ---------------------------------------------------------------------------
// 색 토큰
//
// 위젯의 색은 언제나 낮/밤 두 벌을 함께 실어 보낸다([ColorProvider] 의 day/night). 한 벌로
// 확정해 보내면 화면을 만든 시점의 테마로 색이 굳어, 사용자가 다크 모드를 바꿔도 다음 갱신이
// 올 때까지 이전 색이 그대로 남는다. 두 벌을 실으면 Android 12+ 에서는 시스템이 바꿔 끼고,
// 그 아래에서는 지금처럼 만들 때의 테마로 칠해진다(더 나빠지지 않는다).
//
// 원본 색은 앱 UI 와 같은 스킴에서 온다([AppWidgetPalette]). 두 벌짜리 색을 만드는 팩토리는
// 타입 이름과 겹쳐(둘 다 ColorProvider) dayNightColorProvider 로 들여온다.
// ---------------------------------------------------------------------------

/** 낮/밤 두 벌에 같은 투명도를 입혀 색 하나를 만든다. */
private fun DayNightColor.provider(alpha: Float = 1f): ColorProvider = dayNightColorProvider(
    day = day.copy(alpha = alpha),
    night = night.copy(alpha = alpha),
)

/** 위젯 바탕. */
internal fun widgetSurface(): ColorProvider = AppWidgetPalette.surface.provider()

/** 강조색(진행 중 표시, 완료 버튼, 날짜 밑줄). */
internal fun accent(alpha: Float = 1f): ColorProvider = AppWidgetPalette.primary.provider(alpha)

/** 글자·바탕 위에 얹는 중성 톤. */
internal fun onSurfaceTint(alpha: Float): ColorProvider = AppWidgetPalette.onSurface.provider(alpha)

/** 카드·행의 바닥 톤. 진행 중인 몫은 강조색을 옅게 깔아 시선이 먼저 닿게 한다. */
internal fun widgetWell(active: Boolean = false): ColorProvider =
    if (active) accent(alpha = 0.11f) else onSurfaceTint(alpha = 0.045f)

internal fun textPrimary(): ColorProvider = onSurfaceTint(alpha = 0.92f)

internal fun textSecondary(): ColorProvider = onSurfaceTint(alpha = 0.5f)

internal fun textTertiary(): ColorProvider = onSurfaceTint(alpha = 0.38f)

/** 루프 고유색을 onSurface 위에 합성할 때의 투명도. 앱 카드와 같은 값을 쓴다. */
private const val LOOP_COLOR_ALPHA = 0.8f

/**
 * 루프 고유색을 위젯에 올릴 톤으로 만든다.
 *
 * 앱 카드와 같은 방식(onSurface 위에 [LOOP_COLOR_ALPHA] 로 합성)이되, 낮/밤 각각의 onSurface
 * 위에서 합성해 두 벌을 만든다. [tint] 는 합성이 끝난 색에 다시 입히는 투명도로, 점 둘레의
 * 옅은 후광에 쓴다.
 */
private fun loopColorProvider(argb: Int, tint: Float = 1f): ColorProvider {
    val color = Color(argb).copy(alpha = LOOP_COLOR_ALPHA)
    return dayNightColorProvider(
        day = color.compositeOver(AppWidgetPalette.onSurface.day).copy(alpha = tint),
        night = color.compositeOver(AppWidgetPalette.onSurface.night).copy(alpha = tint),
    )
}


@Composable
fun LoopTitle(
    modifier: GlanceModifier = GlanceModifier,
    title: String,
    isActive: Boolean = false,
) {
    Text(
        modifier = modifier,
        text = title,
        maxLines = 2,
        style = TextStyle(
            fontSize = 15.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
            color = textPrimary(),
        )
    )
}

/**
 * Loop accent indicator: a solid colored dot wrapped in a soft same-color halo,
 * echoing the iOS Reminders / Calendar dot treatment.
 *
 * @param loopColor 루프에 지정된 고유색(ARGB). 위젯 톤으로 옮기는 일은 [loopColorProvider] 가 맡는다.
 */
@Composable
fun LoopColor(
    modifier: GlanceModifier = GlanceModifier,
    loopColor: Int,
    active: Boolean = false,
) {
    val outer = if (active) 18.dp else 14.dp
    val inner = if (active) 9.dp else 7.dp
    Box(
        modifier = modifier
            .size(outer)
            .cornerRadius(outer / 2)
            .background(loopColorProvider(argb = loopColor, tint = if (active) 0.22f else 0.16f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = GlanceModifier
                .size(inner)
                .cornerRadius(inner / 2)
                .background(loopColorProvider(argb = loopColor))
        ) {}
    }
}

@Composable
fun LoopStartEndTime(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
    emphasize: Boolean = false,
) {
    Text(
        modifier = modifier,
        text = loop.toStartOrEndTime(),
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasize) accent() else textSecondary(),
        )
    )
}

@Composable
fun LoopBase.toStartOrEndTime(): String {
    return if (isAnyTime) startOrEndTimeForAnyLoop() else startOrEndTimeForNormalLoop()
}

@Composable
private fun LoopBase.startOrEndTimeForAnyLoop(): String {
    return if (startInDay < 0) {
        stringResourceGlance(id = R.string.anytime)
    } else {
        stringResourceGlance(
            id = R.string.started_at,
            startInDay.formatHourMinute(context = LocalContext.current)
        )
    }
}

@Composable
private fun LoopBase.startOrEndTimeForNormalLoop(): String {
    val now = LocalTime.now().toMs()
    // 아직 시작 전이면 시작 시각을, 진행 중이거나 끝났으면 종료 시각을 보여준다.
    // 자정을 넘기는 루프(예: 22:00~06:00)는 새벽에도 now < start 라, 시각 비교만으로는 진행 중인
    // 구간을 "시작 전"으로 잘못 읽는다. 시간창 안인지를 함께 봐서 그 구간을 걸러낸다.
    return if (now < startInDay && !isTimeInLoop(now)) {
        stringResourceGlance(
            id = R.string.start_at,
            startInDay.formatHourMinute(context = LocalContext.current)
        )
    } else {
        stringResourceGlance(
            id = R.string.end_at,
            endInDay.formatHourMinute(context = LocalContext.current)
        )
    }
}

/**
 * A circular, tappable control used for the loop quick-actions. Tinted-filled to
 * read as a soft button rather than a bare icon.
 */
@Composable
private fun WidgetIconButton(
    onClick: Action,
    resId: Int,
    contentDescription: String,
    tint: ColorProvider,
    background: ColorProvider,
    modifier: GlanceModifier = GlanceModifier,
    size: Dp = 34.dp,
) {
    Box(
        modifier = modifier
            .size(size)
            .cornerRadius(size / 2)
            .background(background)
            .clickable(onClick),
        contentAlignment = Alignment.Center
    ) {
        Image(
            modifier = GlanceModifier.size(size * 0.5f),
            provider = ImageProvider(resId = resId),
            colorFilter = ColorFilter.tint(tint),
            contentDescription = contentDescription
        )
    }
}

@Composable
fun LoopDoneOrSkip(
    modifier: GlanceModifier = GlanceModifier,
    loopId: Int,
    dateMs: Long = 0L,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetIconButton(
            onClick = doneAction(loopId = loopId, dateMs = dateMs),
            resId = R.drawable.done,
            contentDescription = stringResourceGlance(id = R.string.done),
            tint = accent(),
            background = accent(alpha = 0.14f),
        )
        Spacer(modifier = GlanceModifier.defaultWeight())
        WidgetIconButton(
            onClick = skipAction(loopId = loopId, dateMs = dateMs),
            resId = R.drawable.skip,
            contentDescription = stringResourceGlance(id = R.string.skip),
            tint = onSurfaceTint(alpha = 0.55f),
            background = onSurfaceTint(alpha = 0.06f),
        )
    }
}


@Composable
fun LoopDoneOrSkipMedium(
    modifier: GlanceModifier = GlanceModifier,
    loopId: Int,
    dateMs: Long = 0L,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetIconButton(
            onClick = doneAction(loopId = loopId, dateMs = dateMs),
            resId = R.drawable.done,
            contentDescription = stringResourceGlance(id = R.string.done),
            tint = accent(),
            background = accent(alpha = 0.14f),
        )
        Spacer(modifier = GlanceModifier.width(12.dp))
        WidgetIconButton(
            onClick = skipAction(loopId = loopId, dateMs = dateMs),
            resId = R.drawable.skip,
            contentDescription = stringResourceGlance(id = R.string.skip),
            tint = onSurfaceTint(alpha = 0.55f),
            background = onSurfaceTint(alpha = 0.06f),
        )
    }
}

/**
 * Compact done / skip pair sized for a list row's trailing slot. Unlike
 * [LoopDoneOrSkipMedium] it takes only its intrinsic width (no fillMaxWidth) and uses
 * slightly smaller buttons, so it sits neatly next to the title without stretching the row.
 */
@Composable
fun LoopDoneOrSkipCompact(
    modifier: GlanceModifier = GlanceModifier,
    loopId: Int,
    dateMs: Long = 0L,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        WidgetIconButton(
            onClick = doneAction(loopId = loopId, dateMs = dateMs),
            resId = R.drawable.done,
            contentDescription = stringResourceGlance(id = R.string.done),
            tint = accent(),
            background = accent(alpha = 0.14f),
            size = 30.dp,
        )
        Spacer(modifier = GlanceModifier.width(8.dp))
        WidgetIconButton(
            onClick = skipAction(loopId = loopId, dateMs = dateMs),
            resId = R.drawable.skip,
            contentDescription = stringResourceGlance(id = R.string.skip),
            tint = onSurfaceTint(alpha = 0.55f),
            background = onSurfaceTint(alpha = 0.06f),
            size = 30.dp,
        )
    }
}

@Composable
fun AnyTimeLoopStartOrStop(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    val isStart = loop.startInDay < 0
    val loopId = loop.loopId
    WidgetIconButton(
        modifier = modifier,
        onClick = if (isStart) startAction(loopId = loopId) else stopAction(loopId = loopId),
        resId = if (isStart) R.drawable.start else R.drawable.stop,
        contentDescription = stringResourceGlance(
            id = if (isStart) R.string.start else R.string.stop
        ),
        tint = if (isStart) accent() else onSurfaceTint(alpha = 0.55f),
        background = if (isStart) accent(alpha = 0.14f) else onSurfaceTint(alpha = 0.06f),
    )
}

@Composable
fun LocalDateHeader(
    modifier: GlanceModifier = GlanceModifier,
    localDate: LocalDate = LocalDate.now(),
) {
    Column(modifier = modifier.clickable(actionStartActivity<HomeActivity>())) {
        Text(
            text = localDate.formatYearMonthDateDaysGlance(),
            style = TextStyle(
                color = textPrimary(),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
            )
        )
        Spacer(modifier = GlanceModifier.size(6.dp))
        // Short accent underline — a quiet iOS-style emphasis cue beneath the date.
        Box(
            modifier = GlanceModifier
                .width(22.dp)
                .height(3.dp)
                .cornerRadius(2.dp)
                .background(accent())
        ) {}
    }
}

/**
 * 위젯 목록의 그룹 헤더. 홈 "오늘" 탭의 그룹 캡션과 같은 라벨·같은 문법("다음 예정 · 2")으로
 * 적어, 앱과 위젯이 같은 분류를 같은 말로 읽게 한다. "지금 진행 중"만 강조색으로 세워
 * 목록을 훑을 때 시선이 먼저 닿게 하는 것도 앱과 같다.
 */
@Composable
fun LoopGroupHeader(
    modifier: GlanceModifier = GlanceModifier,
    group: TodayGroup,
    count: Int,
) {
    val isNow = group == TodayGroup.NOW
    Text(
        modifier = modifier,
        text = stringResourceGlance(
            id = R.string.today_group_caption,
            stringResourceGlance(id = group.labelResId),
            count,
        ),
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = if (isNow) FontWeight.Bold else FontWeight.Medium,
            color = if (isNow) accent() else textSecondary(),
        )
    )
}

/**
 * 위젯에 띄울 몫이 하나도 없는 이유.
 *
 * 같은 빈 화면이라도 "다 끝냈다"와 "오늘은 예정이 없다"와 "루프가 아직 없다"는 서로 다른 상태이고
 * 안내 문구도 달라야 한다. 셋을 개수 비교로 그때그때 다시 판정하면 어긋나기 쉬워, 한 번만 판정해
 * 이 값으로 넘긴다. 문구는 홈 "오늘" 탭과 같은 것을 쓴다.
 */
enum class WidgetEmptyReason {
    /** 등록된 루프가 아직 하나도 없다. */
    NO_LOOPS,

    /** 루프는 있지만 오늘 몫이 없다(오늘이 활성 요일이 아니거나, 루프가 모두 꺼져 있다). */
    NOTHING_SCHEDULED,

    /** 오늘 몫을 모두 완료하거나 건너뛰었다. */
    ALL_DONE,
}

@Composable
fun LoopWidgetEmpty(
    modifier: GlanceModifier = GlanceModifier,
    reason: WidgetEmptyReason,
) {
    val (titleResId, hintResId) = when (reason) {
        WidgetEmptyReason.NO_LOOPS ->
            R.string.desc_no_loops to R.string.desc_no_loops_hint

        WidgetEmptyReason.NOTHING_SCHEDULED ->
            R.string.today_no_scheduled_loops to R.string.today_no_scheduled_loops_hint

        WidgetEmptyReason.ALL_DONE ->
            R.string.today_loops_finished to R.string.today_loops_finished_hint
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
            .clickable(actionStartActivity<HomeActivity>()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.Horizontal.CenterHorizontally
        ) {
            Text(
                text = stringResourceGlance(id = titleResId),
                style = TextStyle(
                    color = textPrimary(),
                    fontWeight = FontWeight.Medium,
                    fontSize = 15.sp
                )
            )
            Spacer(modifier = GlanceModifier.size(4.dp))
            Text(
                text = stringResourceGlance(id = hintResId),
                style = TextStyle(
                    color = textTertiary(),
                    fontSize = 12.sp
                )
            )
        }
    }
}

@Composable
fun LocalDate.formatYearMonthDateDaysGlance(): String {
    return stringResourceGlance(
        id = R.string.format_year_month_date_day,
        "$year",
        stringResourceGlance(id = ABB_MONTHS[monthValue - 1]),
        "$dayOfMonth",
        stringResourceGlance(id = DAYS_WITH_3CHARS[dayOfWeek.value - 1])
    )
}
