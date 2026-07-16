package com.pnd.android.loop.appwidget.ui

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.action.clickable
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.lazy.LazyColumn
import androidx.glance.appwidget.lazy.items
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.pnd.android.loop.R
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.TimeStat
import com.pnd.android.loop.data.common.NO_REPEAT
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.DAY_STRING_MAP
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

private val WIDGET_MEDIUM_PADDING_HORIZONTAL = 16.dp

/** 좌우 6.dp 여백을 두어 카드가 위젯 가장자리에 붙지 않게 한다(헤더 정렬과 맞춤). */
private val WIDGET_ROW_PADDING_HORIZONTAL = 6.dp

/** 카드 사이 세로 간격(투명 Spacer 로 만든다). */
private val WIDGET_ROW_GAP = 8.dp

// ---------------------------------------------------------------------------
// Medium 위젯 — "진행 히어로 + 리스트" 구성
//  · 지금 할 일 하나를 히어로 카드로 크게 띄워 상태/시간/요일·반복 정보와
//    핵심 액션(완료·건너뛰기 또는 시작·정지)을 모두 노출한다.
//  · 나머지 루프는 컴팩트 행으로 이어 붙여 한눈에 훑고, 상태에 맞는 액션을
//    우측에 인라인으로 제공한다.
// ---------------------------------------------------------------------------

@Composable
fun LoopWidgetMedium(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<LoopBase>,
    todayTotal: Int,
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(widgetSurface())
            .cornerRadius(WIDGET_CARD_RADIUS)
            .padding(
                horizontal = 8.dp,
                vertical = 10.dp,
            ),
    ) {
        LocalDateHeader(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(
                    horizontal = WIDGET_MEDIUM_PADDING_HORIZONTAL,
                    vertical = 6.dp,
                ),
        )
        Spacer(modifier = GlanceModifier.height(8.dp))

        if (loops.isEmpty()) {
            LoopWidgetEmpty(
                modifier = GlanceModifier,
                loopsTotal = todayTotal,
            )
        } else {
            LoopWidgetBody(loops = loops)
        }
    }
}

/**
 * 히어로 카드 하나 + 나머지 컴팩트 행 목록. 전체를 하나의 LazyColumn 에 담아 위젯 높이가
 * 낮아도 함께 스크롤되게 한다.
 */
@Composable
private fun LoopWidgetBody(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<LoopBase>,
) {
    val hero = pickHeroLoop(loops)
    val others = loops.filter { it.loopId != hero.loopId }

    LazyColumn(modifier = modifier) {
        item(itemId = hero.loopId.toLong()) {
            LoopWidgetHero(
                modifier = GlanceModifier.padding(horizontal = WIDGET_ROW_PADDING_HORIZONTAL),
                loop = hero,
            )
        }
        items(
            items = others,
            itemId = { loop -> loop.loopId.toLong() },
        ) { loop ->
            // 카드 사이 간격 만들기(Glance 주의점 2가지):
            //  1) item 람다가 자식을 여러 개 emit 하면 세로로 쌓지 않고 Box 로 겹친다.
            //  2) background 는 modifier 순서와 무관하게 패딩 영역까지 칠하므로 top 패딩으로는
            //     마진이 생기지 않는다.
            // 그래서 명시적 Column 으로 세로로 쌓고, 배경 없는 Spacer 로 투명 간격을 만든다.
            Column {
                Spacer(modifier = GlanceModifier.height(WIDGET_ROW_GAP))
                LoopWidgetMiniRow(
                    modifier = GlanceModifier.padding(horizontal = WIDGET_ROW_PADDING_HORIZONTAL),
                    loop = loop,
                )
            }
        }
        item(itemId = -1L) {
            Spacer(modifier = GlanceModifier.height(8.dp))
        }
    }
}

/**
 * 히어로로 띄울 루프: 지금 진행 중인 루프를 최우선으로, 없으면 가장 이른(곧 시작/응답할)
 * 루프를 고른다. loops 는 비어있지 않다(호출부에서 보장).
 */
private fun pickHeroLoop(loops: List<LoopBase>): LoopBase =
    loops.firstOrNull { it.isRunning() }
        ?: loops.minByOrNull { it.startInDay }
        ?: loops.first()

/**
 * 히어로 카드 — 상태 배지 + 색/제목 + 시간·남은시간 + 요일·반복 + 핵심 액션까지
 * 루프의 모든 기능을 한 카드에 펼친다.
 */
@Composable
private fun LoopWidgetHero(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    val context = LocalContext.current
    val isRunning = loop.isRunning()

    Column(
        modifier = modifier
            .fillMaxWidth()
            .cornerRadius(WIDGET_ROW_RADIUS)
            .background(widgetWell(active = isRunning))
            .clickable { openLoopInApp(context, loop) }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        // 진행 중일 때만 강조 배지를 얹어 "지금 이거"를 가장 강하게 읽히게 한다.
        if (isRunning) {
            HeroRunningBadge()
            Spacer(modifier = GlanceModifier.height(10.dp))
        }

        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            LoopColor(
                color = loop.color.compositeOverOnSurface(),
                active = isRunning,
            )
            Column(
                modifier = GlanceModifier
                    .defaultWeight()
                    .padding(start = 13.dp),
            ) {
                HeroTitle(title = loop.title)
                Spacer(modifier = GlanceModifier.height(5.dp))
                HeroTimeLine(loop = loop, emphasize = isRunning)
            }
        }

        val meta = loop.metaText(context)
        if (meta.isNotEmpty()) {
            Spacer(modifier = GlanceModifier.height(8.dp))
            Text(
                text = meta,
                style = TextStyle(fontSize = 12.sp, color = textTertiary()),
            )
        }

        HeroActions(loop = loop)
    }
}

/**
 * 컴팩트 행 — 색/제목 + 부가정보(남은시간·요일)와 우측 상태별 액션 하나.
 * 진행 중이면 은은한 강조 톤으로 히어로와 시각적으로 이어지게 한다.
 */
@Composable
private fun LoopWidgetMiniRow(
    modifier: GlanceModifier = GlanceModifier,
    loop: LoopBase,
) {
    val context = LocalContext.current
    val isRunning = loop.isRunning()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .cornerRadius(WIDGET_ROW_RADIUS)
            .background(widgetWell(active = isRunning))
            .clickable { openLoopInApp(context, loop) }
            .padding(horizontal = 15.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Vertical.CenterVertically,
    ) {
        LoopColor(
            color = loop.color.compositeOverOnSurface(),
            active = isRunning,
        )
        Column(
            modifier = GlanceModifier
                .defaultWeight()
                .padding(horizontal = 12.dp),
        ) {
            LoopTitle(title = loop.title, isActive = isRunning)
            val progress = loop.progressText()
            if (progress.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = progress,
                    style = TextStyle(fontSize = 12.sp, color = textSecondary()),
                )
            }
        }
        MiniRowAction(loop = loop, isRunning = isRunning)
    }
}

// ---------------------------------------------------------------------------
// 액션 — 상태에 맞는 컨트롤만 노출한다.
//   · anytime 시작/정지 필요        → 시작·정지 버튼
//   · 이미 시작한 시간제 루프        → 완료·건너뛰기 버튼
//   · 아직 시작 전                    → (히어로) 없음 / (행) 시작 시각 표시
// ---------------------------------------------------------------------------

@Composable
private fun HeroActions(loop: LoopBase) {
    when {
        loop.needsStartStop() -> {
            Spacer(modifier = GlanceModifier.height(14.dp))
            AnyTimeLoopStartOrStop(loop = loop)
        }

        loop.canRespond() -> {
            Spacer(modifier = GlanceModifier.height(14.dp))
            LoopDoneOrSkipMedium(loopId = loop.loopId)
        }
        // 시작 전 시간제 루프: 정보만 보여주고 액션은 두지 않는다.
    }
}

@Composable
private fun MiniRowAction(loop: LoopBase, isRunning: Boolean) {
    when {
        loop.needsStartStop() -> AnyTimeLoopStartOrStop(loop = loop)
        loop.canRespond() -> LoopDoneOrSkipCompact(loopId = loop.loopId)
        else -> LoopStartEndTime(loop = loop, emphasize = isRunning)
    }
}

// ---------------------------------------------------------------------------
// 히어로 전용 작은 조각들
// ---------------------------------------------------------------------------

@Composable
private fun HeroRunningBadge() {
    Text(
        text = stringResourceGlance(id = R.string.dial_running),
        modifier = GlanceModifier
            .cornerRadius(8.dp)
            .background(ColorProvider(accentColor().copy(alpha = 0.16f)))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        style = TextStyle(
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = ColorProvider(accentColor()),
        ),
    )
}

@Composable
private fun HeroTitle(title: String) {
    Text(
        text = title,
        maxLines = 2,
        style = TextStyle(
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = textPrimary(),
        ),
    )
}

/** "종료 08:00 · 32분 남음"처럼 시간창과 남은/경과 시간을 한 줄로 합쳐 보여준다. */
@Composable
private fun HeroTimeLine(loop: LoopBase, emphasize: Boolean) {
    val time = loop.toStartOrEndTime()
    val progress = loop.progressText()
    val text = listOf(time, progress).filter { it.isNotEmpty() }.joinToString(separator = " · ")
    if (text.isEmpty()) return

    Text(
        text = text,
        style = TextStyle(
            fontSize = 13.sp,
            fontWeight = if (emphasize) FontWeight.Bold else FontWeight.Medium,
            color = if (emphasize) ColorProvider(accentColor()) else textSecondary(),
        ),
    )
}

// ---------------------------------------------------------------------------
// 위젯 데이터 기준 상태 판별 헬퍼
//  위젯으로 전달되는 루프는 done 상태가 유실되고, anytime 은 실제 시작/종료 시각이
//  start/end 로 옮겨져 온다(AppWidgetUpdateWorker 참고). 그래서 진행/응답 가능 여부는
//  doneState 가 아니라 start/end 시각으로 판단해야 정확하다.
// ---------------------------------------------------------------------------

/** 지금 진행 중인가. anytime 은 "시작됐고 아직 정지 안 됨", 시간제는 "시간창 안". */
private fun LoopBase.isRunning(): Boolean {
    if (isAnyTime) return startInDay >= 0 && endInDay < 0
    val now = LocalTime.now().toMs()
    return now in startInDay until endInDay
}

/** anytime 루프의 시작/정지 버튼이 필요한 상태(아직 시작 전이거나 진행 중). */
private fun LoopBase.needsStartStop(): Boolean =
    isAnyTime && (startInDay < 0 || endInDay < 0)

/** 완료/건너뛰기로 응답할 수 있는 상태(이미 시작한 시간제 루프). */
private fun LoopBase.canRespond(): Boolean =
    !isAnyTime && LocalTime.now().toMs() >= startInDay

/**
 * 남은/경과 시간 문구(예: "32분 남음", "2시간 후 시작"). 기존 TimeStat 문자열을 그대로
 * 재사용하되, 위젯 Text 가 강조 마커('#')를 렌더링하지 못하므로 제거한다.
 */
@Composable
private fun LoopBase.progressText(): String {
    val nowMs = LocalTime.now().toMs()
    val stat: TimeStat? = when {
        isAnyTime ->
            if (isRunning() && startInDay in 0..nowMs) {
                TimeStat.InProgress((nowMs - startInDay).toLocalTime(), isAnyTime = true)
            } else {
                null
            }

        nowMs < startInDay ->
            TimeStat.BeforeStart((startInDay - nowMs).toLocalTime(), isAnyTime = false)

        nowMs < endInDay ->
            TimeStat.InProgress((endInDay - nowMs).toLocalTime(), isAnyTime = false)

        else -> null
    }
    return stat?.asString(LocalContext.current, isAbb = true)?.replace("#", "").orEmpty()
}

/** "매일 · 2시간마다"처럼 활성 요일과 반복 주기를 합친 메타 문구. */
private fun LoopBase.metaText(context: Context): String {
    val repeat = if (interval == NO_REPEAT) "" else intervalString(context, interval)
    return listOf(daysText(context), repeat)
        .filter { it.isNotEmpty() }
        .joinToString(separator = " · ")
}

/** 매일/주중/주말 대명사가 있으면 그것을, 아니면 켜진 요일 약자를 이어 붙인다(예: "월 수 금"). */
private fun LoopBase.daysText(context: Context): String {
    DAY_STRING_MAP[activeDays]?.let { return context.getString(it) }
    return ABB_DAYS.indices
        .filter { index -> activeDays.isOn(LoopDay.fromIndex(index)) }
        .joinToString(separator = " ") { index -> context.getString(ABB_DAYS[index]) }
}

/** 위젯 아이템 탭 → 앱 홈에서 해당 루프를 하이라이트해 연다. */
private fun openLoopInApp(context: Context, loop: LoopBase) {
    context.startActivity(
        Intent(
            Intent.ACTION_VIEW,
            NavigatePage.Home.deepLink(highlightId = loop.loopId).toUri(),
        ).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
    )
}
