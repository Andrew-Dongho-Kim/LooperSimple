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
import com.pnd.android.loop.appwidget.WidgetLoop
import com.pnd.android.loop.appwidget.awaitsResponse
import com.pnd.android.loop.appwidget.canRespond
import com.pnd.android.loop.appwidget.isRunning
import com.pnd.android.loop.appwidget.needsStartStop
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.TimeStat
import com.pnd.android.loop.data.common.NO_REPEAT
import com.pnd.android.loop.ui.home.TodayGroup
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.DAY_STRING_MAP
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.intervalString
import com.pnd.android.loop.util.toLocalTime
import com.pnd.android.loop.util.toMs
import java.time.LocalTime

private val WIDGET_MEDIUM_PADDING_HORIZONTAL = 16.dp

/** 좌우 6.dp 여백을 두어 카드가 위젯 가장자리에 붙지 않게 한다(헤더 정렬과 맞춤). */
private val WIDGET_ROW_PADDING_HORIZONTAL = 6.dp

/** 카드 사이 세로 간격(투명 Spacer 로 만든다). */
private val WIDGET_ROW_GAP = 8.dp

/** 그룹 헤더 위 간격. 카드 사이 간격보다 넉넉히 둬 그룹 경계가 보이게 한다. */
private val WIDGET_GROUP_GAP = 14.dp

/** 헤더 텍스트를 카드 안쪽 내용과 같은 선에 맞추기 위한 들여쓰기. */
private val WIDGET_GROUP_HEADER_INDENT = WIDGET_ROW_PADDING_HORIZONTAL + 12.dp

/** 목록 맨 아래 여백 항목의 id. 루프 항목은 0 이상을 쓰므로([WidgetLoop.itemId]) 음수 영역을 쓴다. */
private const val ITEM_ID_BOTTOM_SPACER = -1L

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
    loops: List<WidgetLoop>,
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
 * 히어로 카드 하나 + 그룹별 컴팩트 행 목록. 전체를 하나의 LazyColumn 에 담아 위젯 높이가
 * 낮아도 함께 스크롤되게 한다.
 *
 * 히어로는 "지금(또는 가장 먼저) 할 일" 하나를 세우는 자리라 헤더 밖에 둔다. 나머지는 홈
 * "오늘" 탭과 같은 그룹(다음 예정 / 응답 대기) 헤더 아래로 모아, 좁은 위젯에서도 아직 남은
 * 일과 답을 기다리는 일의 경계가 보이게 한다.
 */
@Composable
private fun LoopWidgetBody(
    modifier: GlanceModifier = GlanceModifier,
    loops: List<WidgetLoop>,
) {
    val hero = pickHeroLoop(loops)
    // 같은 루프가 두 몫으로 올 수 있으므로 loopId 가 아니라 항목 자체로 제외한다.
    val groups = buildWidgetGroups(loops.filter { it != hero })

    LazyColumn(modifier = modifier) {
        item(itemId = hero.itemId()) {
            LoopWidgetHero(
                modifier = GlanceModifier.padding(horizontal = WIDGET_ROW_PADDING_HORIZONTAL),
                widgetLoop = hero,
            )
        }

        groups.forEach { group ->
            item(itemId = group.group.headerItemId()) {
                // 헤더도 카드와 같은 이유로 Column + Spacer 로 위 간격을 만든다(아래 주의점 참고).
                Column {
                    Spacer(modifier = GlanceModifier.height(WIDGET_GROUP_GAP))
                    LoopGroupHeader(
                        modifier = GlanceModifier.padding(
                            start = WIDGET_GROUP_HEADER_INDENT,
                            bottom = 2.dp,
                        ),
                        group = group.group,
                        count = group.loops.size,
                    )
                }
            }
            items(
                items = group.loops,
                itemId = { widgetLoop -> widgetLoop.itemId() },
            ) { widgetLoop ->
                // 카드 사이 간격 만들기(Glance 주의점 2가지):
                //  1) item 람다가 자식을 여러 개 emit 하면 세로로 쌓지 않고 Box 로 겹친다.
                //  2) background 는 modifier 순서와 무관하게 패딩 영역까지 칠하므로 top 패딩으로는
                //     마진이 생기지 않는다.
                // 그래서 명시적 Column 으로 세로로 쌓고, 배경 없는 Spacer 로 투명 간격을 만든다.
                Column {
                    Spacer(modifier = GlanceModifier.height(WIDGET_ROW_GAP))
                    LoopWidgetMiniRow(
                        modifier = GlanceModifier.padding(
                            horizontal = WIDGET_ROW_PADDING_HORIZONTAL,
                        ),
                        widgetLoop = widgetLoop,
                    )
                }
            }
        }

        item(itemId = ITEM_ID_BOTTOM_SPACER) {
            Spacer(modifier = GlanceModifier.height(8.dp))
        }
    }
}

/**
 * 히어로로 띄울 항목: 지금 진행 중인 몫을 최우선으로, 없으면 가장 이른(곧 시작할) 몫을 고른다.
 * loops 는 비어있지 않다(호출부에서 보장).
 *
 * 어젯밤에서 넘어온 몫은 히어로 후보에서 뺀다. 히어로는 그룹 목록 밖에 따로 서므로
 * ([LoopWidgetBody] 가 히어로를 제외하고 그룹을 만든다), 올려 버리면 "응답 대기" 헤더 아래에서
 * 사라져 답을 기다리는 몫이 없는 것처럼 읽힌다. 다른 후보가 하나도 없을 때만 히어로로 세운다.
 */
private fun pickHeroLoop(loops: List<WidgetLoop>): WidgetLoop =
    loops.firstOrNull { it.isRunning() }
        ?: loops.filterNot { it.isCarriedOver }.minByOrNull { it.loop.startInDay }
        ?: loops.first()

// ---------------------------------------------------------------------------
// 그룹 나누기 — 홈 "오늘" 탭과 같은 분류를 위젯 데이터로 다시 판정한다.
// ---------------------------------------------------------------------------

/** 헤더 한 줄과 그 아래에 쌓일 몫들. 빈 그룹은 [buildWidgetGroups] 가 미리 걸러낸다. */
private data class WidgetLoopGroup(
    val group: TodayGroup,
    val loops: List<WidgetLoop>,
)

/**
 * 몫들을 [TodayGroup] 으로 나눈다. 그룹 순서는 enum 선언 순서(진행 중 → 다음 예정 → 응답 대기)를
 * 그대로 따르고, 그룹 안은 시작 시각이 이른 순으로 세운다.
 */
private fun buildWidgetGroups(loops: List<WidgetLoop>): List<WidgetLoopGroup> {
    val byGroup = loops.groupBy { widgetLoop -> widgetLoop.widgetGroup() }

    return TodayGroup.entries.mapNotNull { group ->
        val groupLoops = byGroup[group] ?: return@mapNotNull null
        WidgetLoopGroup(
            group = group,
            loops = groupLoops.sortedWith(
                compareBy<WidgetLoop>(
                    // 아직 시작하지 않은 시간 미지정 루프는 시작 시각이 없다(-1). 시각으로 줄을
                    // 세울 수 없으니 시각이 있는 몫들 뒤로 보낸다.
                    { it.loop.startInDay < 0 },
                    { it.loop.startInDay },
                )
            ),
        )
    }
}

/**
 * 이 몫이 어느 그룹에 속하는지. 분기 순서와 기준은 홈 오늘 탭의 그룹 판정과 맞춰, 같은 루프가
 * 앱과 위젯에서 다른 그룹으로 읽히지 않게 한다.
 */
private fun WidgetLoop.widgetGroup(): TodayGroup {
    // 어젯밤 몫은 이미 끝난 occurrence 다. 시계는 다음 시작 전을 가리키지만 답을 기다리는 상태다.
    if (isCarriedOver) return TodayGroup.AWAITING

    return when {
        isRunning() -> TodayGroup.NOW
        // 시간 미지정 루프는 시작 시각이 없어 시각으로 비교할 수 없다. 시작 전이면 예정으로 둔다.
        loop.isAnyTime -> TodayGroup.UPCOMING
        // 오늘의 시작 시각이 아직 오지 않았다. 자정을 넘기는 루프도 여기서 "오늘 밤 시작"으로 잡힌다.
        LocalTime.now().toMs() < loop.startInDay -> TodayGroup.UPCOMING
        // 시간창이 끝났는데 완료/건너뜀 응답이 아직 없다.
        else -> TodayGroup.AWAITING
    }
}

/** 헤더 항목의 LazyColumn id. 루프 항목(0 이상)과 겹치지 않게 음수 영역에서 그룹마다 하나씩 쓴다. */
private fun TodayGroup.headerItemId(): Long = ITEM_ID_BOTTOM_SPACER - 1 - ordinal

/**
 * 히어로 카드 — 상태 배지 + 색/제목 + 시간·남은시간 + 요일·반복 + 핵심 액션까지
 * 루프의 모든 기능을 한 카드에 펼친다.
 */
@Composable
private fun LoopWidgetHero(
    modifier: GlanceModifier = GlanceModifier,
    widgetLoop: WidgetLoop,
) {
    val context = LocalContext.current
    val loop = widgetLoop.loop
    val isRunning = widgetLoop.isRunning()

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
                HeroTimeLine(widgetLoop = widgetLoop, emphasize = isRunning)
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

        HeroActions(widgetLoop = widgetLoop)
    }
}

/**
 * 컴팩트 행 — 색/제목 + 부가정보(남은시간·요일)와 우측 상태별 액션 하나.
 * 진행 중이면 은은한 강조 톤으로 히어로와 시각적으로 이어지게 한다.
 */
@Composable
private fun LoopWidgetMiniRow(
    modifier: GlanceModifier = GlanceModifier,
    widgetLoop: WidgetLoop,
) {
    val context = LocalContext.current
    val loop = widgetLoop.loop
    val isRunning = widgetLoop.isRunning()

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
            val progress = widgetLoop.progressText()
            if (progress.isNotEmpty()) {
                Spacer(modifier = GlanceModifier.height(3.dp))
                Text(
                    text = progress,
                    style = TextStyle(fontSize = 12.sp, color = textSecondary()),
                )
            }
        }
        MiniRowAction(widgetLoop = widgetLoop, isRunning = isRunning)
    }
}

// ---------------------------------------------------------------------------
// 액션 — 상태에 맞는 컨트롤만 노출한다.
//   · anytime 시작/정지 필요        → 시작·정지 버튼
//   · 이미 시작한 시간제 루프        → 완료·건너뛰기 버튼
//   · 아직 시작 전                    → (히어로) 없음 / (행) 시작 시각 표시
// ---------------------------------------------------------------------------

@Composable
private fun HeroActions(widgetLoop: WidgetLoop) {
    when {
        widgetLoop.needsStartStop() -> {
            Spacer(modifier = GlanceModifier.height(14.dp))
            AnyTimeLoopStartOrStop(loop = widgetLoop.loop)
        }

        widgetLoop.canRespond() -> {
            Spacer(modifier = GlanceModifier.height(14.dp))
            LoopDoneOrSkipMedium(
                loopId = widgetLoop.loop.loopId,
                dateMs = widgetLoop.dateMs,
            )
        }
        // 시작 전 시간제 루프: 정보만 보여주고 액션은 두지 않는다.
    }
}

@Composable
private fun MiniRowAction(widgetLoop: WidgetLoop, isRunning: Boolean) {
    when {
        widgetLoop.needsStartStop() -> AnyTimeLoopStartOrStop(loop = widgetLoop.loop)
        widgetLoop.canRespond() -> LoopDoneOrSkipCompact(
            loopId = widgetLoop.loop.loopId,
            dateMs = widgetLoop.dateMs,
        )

        else -> LoopStartEndTime(loop = widgetLoop.loop, emphasize = isRunning)
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
private fun HeroTimeLine(widgetLoop: WidgetLoop, emphasize: Boolean) {
    val time = widgetLoop.loop.toStartOrEndTime()
    val progress = widgetLoop.progressText()
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

/**
 * 남은/경과 시간 문구(예: "32분 남음", "2시간 후 시작"). 기존 TimeStat 문자열을 그대로
 * 재사용하되, 위젯 Text 가 강조 마커('#')를 렌더링하지 못하므로 제거한다.
 *
 * 시각 차이는 모두 하루 둘레(mod 24h)로 계산한다. 자정을 넘기는 루프는 단순 뺄셈이 음수가 되거나
 * (새벽 구간의 남은 시간) 엉뚱하게 커져(낮 시간대의 시작까지) 문구가 틀어지기 때문이다.
 */
@Composable
private fun WidgetLoop.progressText(): String {
    val context = LocalContext.current
    // 어젯밤 몫은 오늘 밤 몫과 시간대가 같아 남은 시간으로는 구분되지 않는다. 어느 날 몫인지를 밝힌다.
    if (isCarriedOver) {
        return context.getString(
            R.string.loop_time_yesterday,
            context.getString(R.string.finished),
        )
    }

    val nowMs = LocalTime.now().toMs()
    val stat: TimeStat? = when {
        loop.isAnyTime ->
            if (isRunning() && loop.startInDay in 0..nowMs) {
                TimeStat.InProgress((nowMs - loop.startInDay).toLocalTime(), isAnyTime = true)
            } else {
                null
            }

        isRunning() -> TimeStat.InProgress(
            msUntil(from = nowMs, to = loop.endInDay).toLocalTime(),
            isAnyTime = false,
        )

        // 이미 끝나 답만 남은 몫에는 남은 시간 문구가 없다(응답 버튼이 대신 붙는다).
        awaitsResponse() -> null

        else -> TimeStat.BeforeStart(
            msUntil(from = nowMs, to = loop.startInDay).toLocalTime(),
            isAnyTime = false,
        )
    }
    return stat?.asString(context, isAbb = true)?.replace("#", "").orEmpty()
}

/** [from] 에서 [to] 까지 남은 시간(ms). 하루 둘레로 감아 항상 0 이상 24h 미만이 되게 한다. */
private fun msUntil(from: Long, to: Long): Long = ((to - from) % MS_1DAY + MS_1DAY) % MS_1DAY

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
