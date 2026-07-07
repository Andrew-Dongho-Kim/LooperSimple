package com.pnd.android.loop.ui.home.timeline

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.Undo
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.actualEndInDay
import com.pnd.android.loop.data.actualStartInDay
import com.pnd.android.loop.data.doneState
import com.pnd.android.loop.data.isInProgress
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.ui.home.AnyTimeLoopStartOrStop
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.compositedOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.MS_1DAY
import com.pnd.android.loop.util.MS_1MIN
import com.pnd.android.loop.util.formatHourMinute
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.launch
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * 시안 A — 24시간 원형 다이얼.
 *
 * 하루(0~24시)를 한 바퀴로 펼쳐, 각 루프를 시각에 비례한 호(arc)로 그린다.
 * - 겹치는 루프: 안쪽 레인으로 분리해 서로 가리지 않게 한다.
 * - 현재 시각: WineRed 바늘 + 중앙의 큰 시각 텍스트로 명확히 표시한다.
 * - 진행 중 루프: 더 굵게 + 현재 위치에 맥박(pulse) 점으로 한눈에 띈다.
 * - 겹침: 레인(동심원)으로 분리하되 최대 3줄까지만 그리고, 넘치면 "+N" 배지로 접는다.
 * - 시작/종료·잔여 시간: 호를 탭하면 뜨는 말풍선 팝업에서 확인하고 완료/스킵한다.
 * - AnyTime(시간 미지정) 루프: 24시간 링 바깥의 점선 궤도에 노드로 얹고(대기=속 빈 링,
 *   실행 중=채움+헤일로), 노드를 탭하면 시작/정지 팝업이 뜬다.
 *
 * 색은 모두 [AppColor](테마 토큰) 기반이라 다크/라이트 모두에서 동일한 강도로 읽힌다.
 */
@Composable
fun LoopCircularDial(
    modifier: Modifier = Modifier,
    loops: List<LoopBase>,
    onStateChanged: (LoopBase, Int) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
) {
    // 현재 시각(분 단위로 갱신)을 하루 기준 ms 로 환산해 둔다.
    val localTime by rememberLocalTime()
    val nowMs = localTime.toMs()

    // 루프를 "시간 레인에 그릴 것"과 "바깥 궤도 노드로 둘 것"으로 나눈다.
    // - 시간이 정해진 루프: 그대로 레인에.
    // - 실행 중 AnyTime: 시작 시각~현재까지 "자라는 시간 루프"로 변환 → 다른 루프와 동일한 진행 이펙트.
    // - 정지(완료/스킵)된 AnyTime: 시작~정지 구간의 완료 조각으로 고정.
    // - 대기 중 AnyTime: 바깥 궤도 노드로.
    // nowMs 를 키에 포함해 실행 중 호가 분 단위로 자라도록 한다.
    val (idleAnyTimeLoops, timedLoops) = remember(loops, nowMs) {
        val idle = ArrayList<LoopBase>()
        val timed = ArrayList<LoopBase>()
        loops.forEach { loop ->
            when {
                !loop.isAnyTime -> timed += loop
                // isAnyTime=true 를 유지해, 그릴 때 "시작→안쪽 이동" 진입 애니메이션 대상인지 식별한다.
                loop.isInProgress -> timed += loop.copyAs(
                    startInDay = loop.actualStartInDay,
                    endInDay = nowMs,
                    isAnyTime = true,
                )
                loop.isRespond -> timed += loop.copyAs(
                    startInDay = loop.actualStartInDay,
                    endInDay = loop.actualEndInDay,
                    isAnyTime = true,
                )
                else -> idle += loop
            }
        }
        idle to timed
    }

    Box(modifier = modifier.fillMaxWidth()) {
        DialFace(
            timedLoops = timedLoops,
            anyTimeLoops = idleAnyTimeLoops,
            nowMs = nowMs,
            currentTimeText = localTime.formatHourMinute(withAmPm = false),
            onLoopClick = onNavigateToDetailPage,
            onStateChanged = onStateChanged,
        )
    }
}

// region ─────────────────────────── 상태 분류 & 레인 배치 ───────────────────────────

/** 다이얼에서 한 루프가 그려지는 상태. 완료/스킵/진행 중/예정/지남(미응답)을 구분한다. */
private enum class DialState { DONE, SKIP, ACTIVE, UPCOMING, PAST }

/** 하루 기준 종료 시각(ms). 자정을 넘기는 루프는 하루를 더해 단조 증가하도록 보정한다. */
private fun LoopBase.endMsInDay(): Long =
    if (endInDay < startInDay) endInDay + MS_1DAY else endInDay

private fun LoopBase.dialStateAt(nowMs: Long): DialState = when {
    doneState == LoopDoneVo.DoneState.DONE -> DialState.DONE
    doneState == LoopDoneVo.DoneState.SKIP -> DialState.SKIP
    isInProgress -> DialState.ACTIVE
    nowMs in startInDay until endMsInDay() -> DialState.ACTIVE
    endMsInDay() <= nowMs && !isRespond -> DialState.PAST
    else -> DialState.UPCOMING
}

/** 다이얼에 그릴 하나의 호. 어느 레인(동심원)에 놓일지와 상태를 함께 담는다. */
private data class DialArc(
    val loop: LoopBase,
    val lane: Int,
    val state: DialState,
)

/**
 * 겹치는 루프를 서로 다른 레인으로 배치한다(그리디 인터벌 배치).
 * 시작 시각 순으로 훑으며, 아직 끝나지 않은 레인이 없으면 새 레인을 연다.
 * @return (배치된 호 목록, 사용된 레인 수)
 */
private fun layoutArcs(loops: List<LoopBase>, nowMs: Long): Pair<List<DialArc>, Int> {
    val laneEndMs = ArrayList<Long>() // 레인별 현재까지 채워진 끝 시각
    val arcs = loops
        .sortedBy { loop -> loop.startInDay }
        .map { loop ->
            val start = loop.startInDay
            var lane = laneEndMs.indexOfFirst { end -> end <= start }
            if (lane == -1) {
                lane = laneEndMs.size
                laneEndMs.add(loop.endMsInDay())
            } else {
                laneEndMs[lane] = loop.endMsInDay()
            }
            DialArc(loop = loop, lane = lane, state = loop.dialStateAt(nowMs))
        }
    return arcs to maxOf(1, laneEndMs.size)
}

/** 3레인을 넘겨 접힌(숨긴) 루프 묶음을 나타내는 "+N" 배지. [atMs]=배지를 놓을 대표 시각. */
private data class OverflowBadge(val atMs: Long, val count: Int)

/**
 * 표시 레인([maxVisibleLanes])을 넘어간 호들을 시간대로 묶어 "+N" 배지로 접는다.
 * 시간이 이어지는(겹치는) 숨은 호끼리 한 묶음으로 합쳐, 그 구간 가운데에 배지를 하나 둔다.
 */
private fun foldOverflow(arcs: List<DialArc>, maxVisibleLanes: Int): List<OverflowBadge> {
    val hidden = arcs.filter { it.lane >= maxVisibleLanes }.sortedBy { it.loop.startInDay }
    if (hidden.isEmpty()) return emptyList()

    val badges = ArrayList<OverflowBadge>()
    var clusterStart = hidden.first().loop.startInDay
    var clusterEnd = hidden.first().loop.endMsInDay()
    var count = 0
    fun flush() {
        badges.add(OverflowBadge(atMs = (clusterStart + clusterEnd) / 2, count = count))
    }
    hidden.forEach { arc ->
        val s = arc.loop.startInDay
        val e = arc.loop.endMsInDay()
        if (count == 0 || s <= clusterEnd) {
            // 겹치거나 맞닿으면 같은 묶음으로 확장
            clusterEnd = maxOf(clusterEnd, e)
            if (count == 0) clusterStart = s
            count++
        } else {
            flush()
            clusterStart = s; clusterEnd = e; count = 1
        }
    }
    flush()
    return badges
}

// endregion

// region ─────────────────────────── 다이얼 페이스(Canvas) ───────────────────────────

/** 레인은 3개까지만 시각적으로 분리하고, 그 이상 겹치면 가장 안쪽 레인에 겹쳐 그린다. */
private const val MAX_VISIBLE_LANES = 3

/** 다이얼의 크기 정보(중심·반지름·레인·바깥 궤도). 그리기와 탭 히트 판정이 같은 값을 쓰도록 한곳에서 계산한다. */
private class DialGeometry(
    val cx: Float,
    val cy: Float,
    val stroke: Float,
    val laneStep: Float,
    val outer: Float,
    val orbitRadius: Float, // AnyTime 노드가 놓이는 바깥 점선 궤도의 반지름
) {
    fun laneRadius(lane: Int): Float = outer - stroke / 2f - lane * laneStep
}

/**
 * 캔버스 픽셀 크기로부터 다이얼 지오메트리를 만든다. DrawScope·PointerInputScope 모두 Density 라 재사용 가능.
 * [reserveOrbit] 가 true 면 24시간 링을 안쪽으로 줄여, 시각 라벨 바깥에 AnyTime 궤도 자리를 확보한다.
 */
private fun Density.dialGeometry(
    widthPx: Float,
    heightPx: Float,
    reserveOrbit: Boolean
): DialGeometry {
    val stroke = 13.dp.toPx()
    // 바깥쪽 여백: 시각 라벨(기본) + AnyTime 궤도(있을 때).
    val labelInset = (if (reserveOrbit) 46.dp else 26.dp).toPx()
    val outer = min(widthPx, heightPx) / 2f - labelInset
    return DialGeometry(
        cx = widthPx / 2f,
        cy = heightPx / 2f,
        stroke = stroke,
        laneStep = stroke + 3.dp.toPx(),
        outer = outer,
        orbitRadius = outer + 30.dp.toPx(),
    )
}

/** AnyTime 노드를 놓을 각도(도, 12시=0). 시각과 무관함을 드러내려 상단에 24° 간격으로 모아 배치한다. */
private fun anyTimeNodeAngle(index: Int, count: Int): Double {
    val spacing = 24.0
    return (index - (count - 1) / 2.0) * spacing
}

/** 탭 결과: 어떤 호를, 그 호 위 어느 지점(px)에서 눌렀는지. 말풍선 앵커로 쓴다. */
private data class DialHit(val arc: DialArc, val anchor: Offset)

/**
 * 탭 좌표가 어떤 루프 호 위인지 판정한다.
 * 중심에서의 거리로 레인(반지름)을, 각도로 시각(하루 ms)을 구해 호의 시간 범위와 대조한다.
 * 앵커는 눌린 각도의 해당 호 위 지점에 두어, 말풍선 꼬리가 그 호를 가리키게 한다.
 */
private fun DialGeometry.hitTest(
    tap: Offset,
    arcs: List<DialArc>,
    visibleLanes: Int,
    slop: Float,
): DialHit? {
    val dx = tap.x - cx
    val dy = tap.y - cy
    val dist = hypot(dx, dy)

    // 12시 방향을 0으로 두고 시계방향으로 증가하는 각도(0..360).
    var deg = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())) + 90.0
    if (deg < 0) deg += 360.0
    val tappedMs = (deg / 360.0 * MS_1DAY).toLong()

    // 그린 최소 스윕(약 4°)과 좌우 여유(3°)를 히트 범위에도 반영한다.
    // 방금 시작한 AnyTime 처럼 지속시간이 0 에 가까운 호도 탭할 수 있게 하기 위함.
    val minSpanMs = MS_1DAY * 4 / 360
    val angleSlopMs = MS_1DAY * 3 / 360
    val arc = arcs.firstOrNull { arc ->
        if (arc.lane >= visibleLanes) return@firstOrNull false // 접힌(숨긴) 호는 탭 대상에서 제외
        val radius = laneRadius(arc.lane)
        val half = stroke / 2f + slop
        if (dist < radius - half || dist > radius + half) return@firstOrNull false
        val s = arc.loop.startInDay.coerceAtLeast(0L)
        val e = maxOf(arc.loop.endMsInDay(), s + minSpanMs)
        tappedMs in (s - angleSlopMs)..(e + angleSlopMs)
    } ?: return null

    val radius = laneRadius(arc.lane)
    val a = Math.toRadians(-90.0 + tappedMs.toDouble() / MS_1DAY * 360.0)
    val anchor = Offset(cx + radius * cos(a).toFloat(), cy + radius * sin(a).toFloat())
    return DialHit(arc = arc, anchor = anchor)
}

/** AnyTime 노드 탭 결과. [anchor]=노드 중심(팝업 꼬리가 노드를 가리키도록). */
private data class AnyTimeHit(val loop: LoopBase, val anchor: Offset)

/** 바깥 궤도의 AnyTime 노드 중 탭 지점과 겹치는 것을 찾는다. */
private fun DialGeometry.hitTestAnyTime(
    tap: Offset,
    loops: List<LoopBase>,
    nodeRadius: Float,
    slop: Float
): AnyTimeHit? {
    loops.forEachIndexed { i, loop ->
        val a = Math.toRadians(-90.0 + anyTimeNodeAngle(i, loops.size))
        val center =
            Offset(cx + orbitRadius * cos(a).toFloat(), cy + orbitRadius * sin(a).toFloat())
        if (hypot(tap.x - center.x, tap.y - center.y) <= nodeRadius + slop) {
            return AnyTimeHit(loop = loop, anchor = center)
        }
    }
    return null
}

@Composable
private fun DialFace(
    timedLoops: List<LoopBase>,
    anyTimeLoops: List<LoopBase>,
    nowMs: Long,
    currentTimeText: String,
    onLoopClick: (LoopBase) -> Unit,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {
    val (arcs, laneCount) = remember(timedLoops, nowMs) { layoutArcs(timedLoops, nowMs) }
    val visibleLanes = min(laneCount, MAX_VISIBLE_LANES)
    // 3레인을 넘긴 호들은 그리지 않고 "+N" 배지로 접는다.
    val overflowBadges = remember(arcs, visibleLanes) { foldOverflow(arcs, visibleLanes) }
    val hasAnyTime = anyTimeLoops.isNotEmpty()

    // "?" 도움말 팝업 표시 여부.
    var showHelp by remember { mutableStateOf(false) }

    // 탭으로 선택된 호. 앵커(누른 지점)는 그대로 두되 arc 정보는 매 프레임 현재 arcs 에서 다시 찾아,
    // 분 단위 갱신으로 목록이 바뀌어도 팝업이 유지되도록 한다(루프가 사라지면 자동으로 닫힘).
    var selection by remember { mutableStateOf<DialHit?>(null) }
    val selectedHit = selection?.let { sel ->
        arcs.firstOrNull { it.loop.loopId == sel.arc.loop.loopId }
            ?.let { DialHit(arc = it, anchor = sel.anchor) }
    }
    val selectedId = selectedHit?.arc?.loop?.loopId

    // 탭으로 선택된 AnyTime 노드. 마찬가지로 loopId 로 현재 목록에서 다시 찾아 유지한다.
    var anyTimeSelection by remember { mutableStateOf<AnyTimeHit?>(null) }
    val selectedAnyTime = anyTimeSelection?.let { sel ->
        anyTimeLoops.firstOrNull { it.loopId == sel.loop.loopId }
            ?.let { AnyTimeHit(loop = it, anchor = sel.anchor) }
    }

    // 진행 중 루프의 맥박 애니메이션(반지름·투명도). 하나의 트랜지션을 모든 진행 호가 공유한다.
    val pulse = rememberInfiniteTransition(label = "DialPulse")
    val pulseRadius by pulse.animateFloat(
        initialValue = 4f,
        targetValue = 9f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseRadius",
    )
    val pulseAlpha by pulse.animateFloat(
        initialValue = 0.45f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "pulseAlpha",
    )
    // 진행 중 호 뒤에서 숨쉬는(breathing) 글로우. 폭·투명도가 함께 커졌다 작아진다.
    val glowWidth by pulse.animateFloat(
        initialValue = 5f,
        targetValue = 16f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glowWidth",
    )
    val glowAlpha by pulse.animateFloat(
        initialValue = 0.32f,
        targetValue = 0.04f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "glowAlpha",
    )
    // 진행 중 밴드 위로 흐르는 점선(마칭 앤츠)의 위상. 0→1 을 반복해 종료 방향으로 계속 흐른다.
    val dashPhase by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(700, easing = LinearEasing), RepeatMode.Restart),
        label = "dashPhase",
    )

    // 방금 시작한 AnyTime 이 "바깥 궤도 → 안쪽 레인"으로 들어오는 진입 애니메이션 진행값(loopId -> 0..1).
    // 값이 없거나 1 이면 완전히 자리잡은 상태.
    val enterProgress = remember { mutableStateMapOf<Int, Float>() }
    val runningAnyTimeIds = remember(arcs) {
        arcs.filter { it.loop.isAnyTime && it.state == DialState.ACTIVE }
            .map { it.loop.loopId }
            .toSet()
    }
    // null = 최초 구성(이때는 애니메이션 없이 기준선만 잡는다). 이후 새로 등장한 id 만 진입 애니메이션.
    var knownRunningIds by remember { mutableStateOf<Set<Int>?>(null) }
    LaunchedEffect(runningAnyTimeIds) {
        val prev = knownRunningIds
        knownRunningIds = runningAnyTimeIds
        enterProgress.keys.retainAll(runningAnyTimeIds) // 종료된 것은 진입값 제거
        if (prev == null) return@LaunchedEffect
        (runningAnyTimeIds - prev).forEach { id ->
            launch {
                animate(initialValue = 0f, targetValue = 1f, animationSpec = tween(450)) { v, _ ->
                    enterProgress[id] = v
                }
                enterProgress[id] = 1f
            }
        }
    }

    // 테마 토큰을 미리 읽어 Canvas 람다에서 재사용한다(다크/라이트 자동 대응).
    val trackColor = AppColor.onSurface.copy(alpha = 0.08f)
    val tickMajorColor = AppColor.onSurface.copy(alpha = 0.55f)
    val tickMinorColor = AppColor.onSurface.copy(alpha = 0.22f)
    // 지난 미응답(PAST) 호는 "색 외곽선 + 빈 속" 으로 그린다. 속을 트랙과 같은 불투명색으로
    // 덧칠해 테두리만 남기는 방식이라, 색 밴드 아래 배경(트랙)과 자연스럽게 이어진다.
    val hollowFillColor = compositedOnSurface(alpha = 0.08f)
    val highlightColor = AppColor.onSurface.copy(alpha = 0.18f) // 선택된 호 뒤에 까는 후광
    val needleColor = AppColor.onSurface
    val primaryColor = AppColor.primary
    val badgeFillColor = primaryColor.copy(alpha = 0.16f) // "+N" 배지 배경(primary 틴트)
    val orbitColor = AppColor.onSurface.copy(alpha = 0.3f) // AnyTime 바깥 점선 궤도
    val labelStyle =
        TextStyle(color = tickMajorColor, fontSize = 11.sp, fontWeight = FontWeight.Medium)
    val badgeTextStyle =
        TextStyle(color = primaryColor, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    val textMeasurer = rememberTextMeasurer()

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .sizeIn(maxWidth = 320.dp)
                .aspectRatio(1f),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(arcs, visibleLanes, anyTimeLoops, hasAnyTime) {
                        // 탭하면 히트 판정. 바깥 AnyTime 노드를 먼저 보고, 없으면 시간 호를 본다.
                        // 둘 다 아니면(빈 곳) 두 팝업 모두 닫는다.
                        detectTapGestures { tap ->
                            val geometry = dialGeometry(
                                size.width.toFloat(),
                                size.height.toFloat(),
                                hasAnyTime
                            )
                            val anyHit = if (hasAnyTime) {
                                geometry.hitTestAnyTime(
                                    tap = tap,
                                    loops = anyTimeLoops,
                                    nodeRadius = 6.dp.toPx(),
                                    slop = 8.dp.toPx(),
                                )
                            } else null
                            if (anyHit != null) {
                                anyTimeSelection = anyHit
                                selection = null
                            } else {
                                selection = geometry.hitTest(
                                    tap = tap,
                                    arcs = arcs,
                                    visibleLanes = visibleLanes,
                                    slop = 8.dp.toPx(),
                                )
                                anyTimeSelection = null
                            }
                        }
                    },
            ) {
                val geo = dialGeometry(size.width, size.height, hasAnyTime)
                val cx = geo.cx
                val cy = geo.cy
                val stroke = geo.stroke

                // 1) 사용 중인 레인 수만큼 옅은 배경 트랙 원을 깐다.
                for (lane in 0 until visibleLanes) {
                    drawCircle(
                        color = trackColor,
                        radius = geo.laneRadius(lane),
                        center = Offset(cx, cy),
                        style = Stroke(width = stroke),
                    )
                }

                // 2) 시(hour) 눈금과 3시간마다의 숫자 라벨.
                for (hour in 0 until 24) {
                    val major = hour % 3 == 0
                    val angle = Math.toRadians(-90.0 + hour / 24.0 * 360.0)
                    val cosA = cos(angle).toFloat()
                    val sinA = sin(angle).toFloat()
                    val inner = geo.outer + 2.dp.toPx()
                    val tick = geo.outer + (if (major) 8.dp.toPx() else 4.dp.toPx())
                    drawLine(
                        color = if (major) tickMajorColor else tickMinorColor,
                        start = Offset(cx + inner * cosA, cy + inner * sinA),
                        end = Offset(cx + tick * cosA, cy + tick * sinA),
                        strokeWidth = if (major) 1.5.dp.toPx() else 1.dp.toPx(),
                    )
                    if (major) {
                        val lr = geo.outer + 16.dp.toPx()
                        val layout = textMeasurer.measure(hour.toString(), labelStyle)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                x = cx + lr * cosA - layout.size.width / 2f,
                                y = cy + lr * sinA - layout.size.height / 2f,
                            ),
                        )
                    }
                }

                // 3) 루프 호. 겹침은 안쪽 레인으로. 표시 레인을 넘긴 호는 그리지 않고 아래 "+N" 배지로 접는다.
                arcs.forEach { arc ->
                    if (arc.lane >= visibleLanes) return@forEach
                    val radius = geo.laneRadius(arc.lane)
                    val loopColor = Color(arc.loop.color)
                    val isActive = arc.state == DialState.ACTIVE
                    val isPast = arc.state == DialState.PAST
                    val isSkip = arc.state == DialState.SKIP
                    // 진행 중은 가장 굵게, 나머지는 기본 두께(스킵은 아래에서 취소선으로 별도 처리).
                    val width = if (isActive) stroke + 4.dp.toPx() else stroke

                    val startMs = arc.loop.startInDay.coerceAtLeast(0L)
                    val sweep = ((arc.loop.endMsInDay() - startMs).toFloat() / MS_1DAY * 360f)
                        .coerceAtLeast(2.5f) // 아주 짧은 루프도 최소한 보이게
                    val startAngle = -90f + startMs.toFloat() / MS_1DAY * 360f
                    val topLeft = Offset(cx - radius, cy - radius)
                    val arcSize = Size(radius * 2f, radius * 2f)

                    // 선택된 호는 뒤에 후광을 깔아 어떤 호를 눌렀는지 드러낸다.
                    if (arc.loop.loopId == selectedId) {
                        drawArc(
                            color = highlightColor,
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(width = width + 8.dp.toPx(), cap = StrokeCap.Round),
                        )
                    }

                    // 진행 중 호는 뒤에서 숨쉬는 글로우가 커졌다 작아지며 "지금 진행 중"을 확실히 알린다.
                    if (isActive) {
                        drawArc(
                            color = loopColor.copy(alpha = glowAlpha),
                            startAngle = startAngle,
                            sweepAngle = sweep,
                            useCenter = false,
                            topLeft = topLeft,
                            size = arcSize,
                            style = Stroke(
                                width = width + glowWidth.dp.toPx(),
                                cap = StrokeCap.Round
                            ),
                        )
                    }

                    when {
                        isPast -> {
                            // 지난 미응답: 색 밴드를 그린 뒤 속을 트랙색으로 덧칠 → 색 외곽선만 남는 빈 형태.
                            // 겹침은 레인(반지름)이 달라 서로 침범하지 않으므로 이 방식이 안전하다.
                            val rim = 2.2.dp.toPx()
                            drawArc(
                                color = loopColor,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = width, cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = hollowFillColor,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = (width - rim * 2f).coerceAtLeast(1f),
                                    cap = StrokeCap.Round
                                ),
                            )
                        }

                        isActive -> {
                            // 진행 중: 경과분은 진한 색, 잔여분은 옅은 색으로 나눠 진행률을 보여주고(추천 A),
                            // 밴드 위로 흐르는 점선을 옅게 얹어 "지금 흐르고 있다"는 모션을 준다(추천 B).
                            val endMs = arc.loop.endMsInDay()
                            val nowClamped = nowMs.coerceIn(startMs, endMs)
                            val elapsedSweep = ((nowClamped - startMs).toFloat() / MS_1DAY * 360f)
                            val nowAngle = -90f + nowClamped.toFloat() / MS_1DAY * 360f
                            val remainSweep = ((endMs - nowClamped).toFloat() / MS_1DAY * 360f)

                            // 잔여분(옅게)
                            drawArc(
                                color = loopColor.copy(alpha = 0.33f),
                                startAngle = nowAngle,
                                sweepAngle = remainSweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = width, cap = StrokeCap.Round),
                            )
                            // 경과분(진하게)
                            drawArc(
                                color = loopColor,
                                startAngle = startAngle,
                                sweepAngle = elapsedSweep.coerceAtLeast(0.5f),
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = width, cap = StrokeCap.Round),
                            )
                            // 흐르는 점선(마칭 앤츠) — 종료 방향으로 계속 흐른다.
                            val period = 2.dp.toPx() + 11.dp.toPx()
                            drawArc(
                                color = Color.White.copy(alpha = 0.5f),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(
                                    width = width * 0.28f,
                                    cap = StrokeCap.Round,
                                    pathEffect = PathEffect.dashPathEffect(
                                        intervals = floatArrayOf(2.dp.toPx(), 11.dp.toPx()),
                                        phase = dashPhase * period,
                                    ),
                                ),
                            )
                        }

                        isSkip -> {
                            // 스킵: 옅은 색 밴드 위에 가운데 관통선(취소선) → "줄 그어 건너뜀".
                            drawArc(
                                color = loopColor.copy(alpha = 0.32f),
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = width, cap = StrokeCap.Round),
                            )
                            drawArc(
                                color = loopColor,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = 2.6.dp.toPx(), cap = StrokeCap.Round),
                            )
                        }

                        else -> {
                            // 시안 3: 완료=꽉 찬 색, 예정=옅은 색.
                            val arcColor = when (arc.state) {
                                DialState.UPCOMING -> loopColor.copy(alpha = 0.4f)
                                else -> loopColor
                            }
                            drawArc(
                                color = arcColor,
                                startAngle = startAngle,
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = width, cap = StrokeCap.Round),
                            )
                        }
                    }

                    // 진행 중이면 현재 위치에 맥박 점을 얹어 "지금 이거"를 즉시 인지시킨다.
                    if (isActive) {
                        val a = Math.toRadians(-90.0 + nowMs.toDouble() / MS_1DAY * 360.0)
                        val cosA2 = cos(a).toFloat()
                        val sinA2 = sin(a).toFloat()
                        val pos = Offset(cx + radius * cosA2, cy + radius * sinA2)

                        // 방금 시작한 AnyTime 은 바깥 궤도에서 현재 위치(리딩 엣지)로 점이 날아 들어온다.
                        val enter = if (arc.loop.isAnyTime) (enterProgress[arc.loop.loopId] ?: 1f) else 1f
                        if (enter < 1f) {
                            val fromR = geo.orbitRadius
                            val curR = fromR + (radius - fromR) * enter
                            val fromPos = Offset(cx + fromR * cosA2, cy + fromR * sinA2)
                            val curPos = Offset(cx + curR * cosA2, cy + curR * sinA2)
                            drawLine(
                                color = loopColor.copy(alpha = (1f - enter) * 0.5f),
                                start = fromPos,
                                end = curPos,
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round,
                            )
                            drawCircle(loopColor, 5.dp.toPx(), curPos)
                        }

                        drawCircle(loopColor.copy(alpha = pulseAlpha), pulseRadius.dp.toPx(), pos)
                        drawCircle(loopColor, 3.5.dp.toPx(), pos)
                    }
                }

                // 3-1) 접힌 겹침을 "+N" 배지로 표시. 겹친 구간 가운데, 가장 안쪽 레인보다 살짝 안쪽에 둔다.
                if (overflowBadges.isNotEmpty()) {
                    val badgeRadius = 9.dp.toPx()
                    val ringRadius =
                        (geo.laneRadius(visibleLanes - 1) - stroke).coerceAtLeast(badgeRadius)
                    overflowBadges.forEach { badge ->
                        val a = Math.toRadians(-90.0 + badge.atMs.toDouble() / MS_1DAY * 360.0)
                        val center = Offset(
                            cx + ringRadius * cos(a).toFloat(),
                            cy + ringRadius * sin(a).toFloat()
                        )
                        drawCircle(badgeFillColor, badgeRadius, center)
                        drawCircle(
                            primaryColor,
                            badgeRadius,
                            center,
                            style = Stroke(width = 1.dp.toPx())
                        )
                        val layout = textMeasurer.measure("+${badge.count}", badgeTextStyle)
                        drawText(
                            textLayoutResult = layout,
                            topLeft = Offset(
                                x = center.x - layout.size.width / 2f,
                                y = center.y - layout.size.height / 2f,
                            ),
                        )
                    }
                }

                // 4) 현재 시각 바늘. 중앙 텍스트를 가리지 않도록 안쪽 반지름에서 출발한다.
                val na = Math.toRadians(-90.0 + nowMs.toDouble() / MS_1DAY * 360.0)
                val cosN = cos(na).toFloat()
                val sinN = sin(na).toFloat()
                val needleInner = geo.outer * 0.36f
                val needleOuter = geo.outer + 6.dp.toPx()
                drawLine(
                    color = needleColor,
                    start = Offset(cx + needleInner * cosN, cy + needleInner * sinN),
                    end = Offset(cx + needleOuter * cosN, cy + needleOuter * sinN),
                    strokeWidth = 2.5.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawCircle(needleColor, 4.dp.toPx(), Offset(cx, cy))

                // 5) AnyTime(시간 미지정) 루프: 바깥 점선 궤도 + 노드.
                //    대기 = 속 빈 색 링, 실행 중 = 채움 + 맥박 헤일로.
                if (hasAnyTime) {
                    drawCircle(
                        color = orbitColor,
                        radius = geo.orbitRadius,
                        center = Offset(cx, cy),
                        style = Stroke(
                            width = 1.2.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(
                                    3.dp.toPx(),
                                    4.dp.toPx()
                                )
                            ),
                        ),
                    )
                    val nodeRadius = 6.dp.toPx()
                    anyTimeLoops.forEachIndexed { i, loop ->
                        val a = Math.toRadians(-90.0 + anyTimeNodeAngle(i, anyTimeLoops.size))
                        val center = Offset(
                            cx + geo.orbitRadius * cos(a).toFloat(),
                            cy + geo.orbitRadius * sin(a).toFloat(),
                        )
                        val loopColor = Color(loop.color)
                        if (loop.isInProgress) {
                            drawCircle(
                                loopColor.copy(alpha = pulseAlpha),
                                nodeRadius + pulseRadius.dp.toPx(),
                                center
                            )
                            drawCircle(loopColor, nodeRadius, center)
                        } else {
                            drawCircle(hollowFillColor, nodeRadius, center)
                            drawCircle(
                                loopColor,
                                nodeRadius,
                                center,
                                style = Stroke(width = 2.4.dp.toPx())
                            )
                        }
                    }
                }
            }

            // 중앙 오버레이: 현재 시각.
            DialCenterLabel(
                modifier = Modifier.padding(bottom = 32.dp),
                currentTimeText = currentTimeText
            )

            // 탭한 호를 가리키는 말풍선 팝업(시안 1). 별도 윈도우(Popup)라 부모에 잘리지 않고
            // 항상 최상단에 뜨며, 위치는 화면 밖으로 넘치지 않게 보정된다.
            selectedHit?.let { hit ->
                DialLoopTooltip(
                    arc = hit.arc,
                    nowMs = nowMs,
                    anchor = hit.anchor,
                    onDismiss = { selection = null },
                    onClick = {
                        onLoopClick(hit.arc.loop)
                        selection = null
                    },
                    onStateChanged = { loop, doneState ->
                        onStateChanged(loop, doneState)
                        selection = null // 처리 후 팝업을 닫는다
                    },
                )
            }

            // 탭한 AnyTime 노드를 가리키는 팝업. 제목·상태 + 시작/정지 버튼.
            selectedAnyTime?.let { hit ->
                AnyTimeTooltip(
                    loop = hit.loop,
                    anchor = hit.anchor,
                    onDismiss = { anyTimeSelection = null },
                    onClick = {
                        onLoopClick(hit.loop)
                        anyTimeSelection = null
                    },
                    onStateChanged = { loop, doneState ->
                        onStateChanged(loop, doneState)
                        anyTimeSelection = null
                    },
                )
            }

            // 사각형 다이얼의 빈 우상단 모서리에 "?" 도움말 버튼. 탭하면 상태 설명 팝업을 연다.
            IconButton(
                onClick = { showHelp = true },
                modifier = Modifier.align(Alignment.TopEnd),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                    tint = AppColor.onSurface.copy(alpha = 0.5f),
                    contentDescription = stringResource(id = R.string.dial_help_title),
                )
            }
        }
    }

    if (showHelp) {
        DialStateHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
private fun DialCenterLabel(
    modifier: Modifier = Modifier,
    currentTimeText: String,
) {
    Text(
        modifier = modifier,
        text = currentTimeText,
        style = AppTypography.headlineSmall.copy(
            color = AppColor.onSurface,
            fontWeight = FontWeight.SemiBold,
        ),
    )
}

// endregion

// region ─────────────────────────── 상태 설명 팝업("?") ───────────────────────────

/** 다이얼에서 쓰는 상태 표기(시안 3)의 의미를 미니 견본과 함께 설명하는 도움말 다이얼로그. */
@Composable
private fun DialStateHelpDialog(onDismiss: () -> Unit) {
    // usePlatformDefaultWidth=false: 기본 다이얼로그 폭(넓게 고정)을 끄고 콘텐츠 크기에 맞춘다.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        val shape = RoundedCornerShape(16.dp)
        Column(
            modifier = Modifier
                .widthIn(max = 340.dp) // 태블릿 등에서 과도하게 넓어지지 않도록만 제한
                .clip(shape)
                .background(color = AppColor.surfaceElevated)
                .border(
                    width = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.14f),
                    shape = shape
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(id = R.string.dial_help_title),
                style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
            )

            // (견본 종류, 상태 이름, 설명) — 다이얼의 렌더링과 같은 스타일로 보여준다.
            HelpRow(StateSwatchKind.SOLID, R.string.done, R.string.dial_desc_done)
            HelpRow(StateSwatchKind.STRIKE, R.string.skip, R.string.dial_desc_skip)
            HelpRow(StateSwatchKind.HOLLOW, R.string.no_response, R.string.dial_desc_noresp)
            HelpRow(StateSwatchKind.THICK, R.string.dial_running, R.string.dial_desc_active)
            HelpRow(
                StateSwatchKind.LIGHT,
                R.string.dial_status_upcoming,
                R.string.dial_desc_upcoming
            )

            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RowShape)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 14.dp, vertical = 8.dp),
                text = stringResource(id = R.string.ok),
                style = AppTypography.labelLarge.copy(color = AppColor.primary),
            )
        }
    }
}

@Composable
private fun HelpRow(
    kind: StateSwatchKind,
    nameRes: Int,
    descRes: Int,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StateSwatch(kind = kind)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = stringResource(id = nameRes),
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Text(
                text = stringResource(id = descRes),
                style = AppTypography.labelMedium.copy(color = AppColor.onSurface.copy(alpha = 0.6f)),
            )
        }
    }
}

/** 도움말의 상태 견본 종류. 다이얼 호 스타일을 축소해 보여준다. */
private enum class StateSwatchKind { SOLID, LIGHT, THICK, HOLLOW, STRIKE }

/** 상태 견본. 색은 다이얼 호처럼 임의 색을 대표하도록 primary 로 그린다. */
@Composable
private fun StateSwatch(kind: StateSwatchKind) {
    val color = AppColor.primary
    val surfaceColor = AppColor.surfaceElevated // 빈 형태(HOLLOW)에서 속을 지울 배경색(다이얼로그 면)
    Canvas(modifier = Modifier.size(width = 30.dp, height = 16.dp)) {
        val cy = size.height / 2f
        val w = if (kind == StateSwatchKind.THICK) 12.dp.toPx() else 8.dp.toPx()
        val cap = StrokeCap.Round
        val start = Offset(w / 2f, cy)
        val end = Offset(size.width - w / 2f, cy)
        when (kind) {
            StateSwatchKind.SOLID, StateSwatchKind.THICK ->
                drawLine(color, start, end, strokeWidth = w, cap = cap)

            StateSwatchKind.LIGHT ->
                drawLine(color.copy(alpha = 0.4f), start, end, strokeWidth = w, cap = cap)

            StateSwatchKind.STRIKE -> {
                // 취소선: 옅은 밴드 위에 가운데 관통선.
                drawLine(color.copy(alpha = 0.32f), start, end, strokeWidth = w, cap = cap)
                drawLine(color, start, end, strokeWidth = 2.4.dp.toPx(), cap = cap)
            }

            StateSwatchKind.HOLLOW -> {
                // 색 외곽선(빈 형태): 색 선 위에 배경색을 덧칠해 테두리만 남긴다.
                drawLine(color, start, end, strokeWidth = w, cap = cap)
                drawLine(surfaceColor, start, end, strokeWidth = w - 4.4.dp.toPx(), cap = cap)
            }
        }
    }
}

// endregion

// region ─────────────────────────── 말풍선 팝업(시안 1) ───────────────────────────

/**
 * 앵커(다이얼 박스 좌표계의 한 지점)를 가리키는 말풍선 팝업의 공통 껍데기.
 *
 * [Popup] 으로 띄워 부모(리스트 아이템/다이얼 박스) 경계에 잘리지 않고 항상 최상단에 그려지며,
 * [DialTooltipPositionProvider] 가 실제 화면 크기를 보고 화면 밖으로 넘치지 않게 위치를 보정한다.
 * 최종 위치에 맞춘 꼬리 방향(pointingUp)·꼬리 x(tailXPx)를 [content] 로 넘겨 앵커를 정확히 가리킨다.
 */
@Composable
private fun AnchoredTooltip(
    anchor: Offset,
    onDismiss: () -> Unit,
    content: @Composable (pointingUp: Boolean, tailXPx: Float) -> Unit,
) {
    val density = LocalDensity.current
    var pointingUp by remember { mutableStateOf(false) }
    var tailXPx by remember { mutableFloatStateOf(0f) }

    val positionProvider = remember(anchor, density) {
        DialTooltipPositionProvider(
            anchorInParent = anchor,
            gapPx = with(density) { 6.dp.roundToPx() },
            marginPx = with(density) { 8.dp.roundToPx() },
            tailInsetPx = with(density) { 18.dp.roundToPx() },
            onPlaced = { up, tailX ->
                pointingUp = up
                tailXPx = tailX
            },
        )
    }

    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismiss,
        properties = PopupProperties(focusable = false), // 다이얼을 계속 탭할 수 있게 비포커스
    ) {
        content(pointingUp, tailXPx)
    }
}

/** 탭한 시간 호를 가리키는 말풍선(정보 + 완료/스킵/되돌리기). */
@Composable
private fun DialLoopTooltip(
    arc: DialArc,
    nowMs: Long,
    anchor: Offset,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {
    AnchoredTooltip(anchor = anchor, onDismiss = onDismiss) { pointingUp, tailXPx ->
        DialTooltipCard(
            arc = arc,
            nowMs = nowMs,
            pointingUp = pointingUp,
            tailXPx = tailXPx,
            onClick = onClick,
            onStateChanged = onStateChanged,
        )
    }
}

/** 탭한 AnyTime 노드를 가리키는 말풍선(제목·상태 + 시작/정지). */
@Composable
private fun AnyTimeTooltip(
    loop: LoopBase,
    anchor: Offset,
    onDismiss: () -> Unit,
    onClick: () -> Unit,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {
    AnchoredTooltip(anchor = anchor, onDismiss = onDismiss) { pointingUp, tailXPx ->
        AnyTimeTooltipCard(
            loop = loop,
            pointingUp = pointingUp,
            tailXPx = tailXPx,
            onClick = onClick,
            onStateChanged = onStateChanged,
        )
    }
}

/**
 * 말풍선 팝업의 위치를 정하는 제공자.
 * - 앵커(부모 좌표) → 화면 좌표로 옮긴 뒤, 위쪽이면 아래로 펼쳐 위로 넘치지 않게 한다.
 * - 좌우/상하 모두 화면 안으로 clamp 해 팝업이 잘리지 않는다.
 * - 최종 위치가 정해지면 꼬리 방향/오프셋을 [onPlaced] 로 돌려준다.
 */
private class DialTooltipPositionProvider(
    private val anchorInParent: Offset,
    private val gapPx: Int,
    private val marginPx: Int,
    private val tailInsetPx: Int,
    private val onPlaced: (pointingUp: Boolean, tailXPx: Float) -> Unit,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // 앵커(호 위 지점)를 화면 좌표로.
        val anchorX = anchorBounds.left + anchorInParent.x
        val anchorY = anchorBounds.top + anchorInParent.y

        // 화면 위쪽(40% 위)에 앵커가 있으면 아래로 펼친다.
        val pointingUp = anchorY < windowSize.height * 0.4f
        val rawX = anchorX - popupContentSize.width / 2f
        val rawY = if (pointingUp) anchorY + gapPx else anchorY - gapPx - popupContentSize.height

        val maxX = (windowSize.width - popupContentSize.width - marginPx).coerceAtLeast(marginPx)
        val maxY = (windowSize.height - popupContentSize.height - marginPx).coerceAtLeast(marginPx)
        val x = rawX.roundToInt().coerceIn(marginPx, maxX)
        val y = rawY.roundToInt().coerceIn(marginPx, maxY)

        // 꼬리는 앵커의 실제 x 를 향하되 카드 모서리에 붙지 않도록 안쪽으로 제한.
        val maxTail = (popupContentSize.width - tailInsetPx).coerceAtLeast(tailInsetPx).toFloat()
        val tailX = (anchorX - x).coerceIn(tailInsetPx.toFloat(), maxTail)
        onPlaced(pointingUp, tailX)

        return IntOffset(x, y)
    }
}

/**
 * 말풍선 카드 공통 껍데기: 떠 있는 표면 + 테두리 + 앵커를 가리키는 꼬리. 내용은 [content] 로 채운다.
 */
@Composable
private fun TooltipShell(
    pointingUp: Boolean,
    tailXPx: Float,
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    // 팝업 면은 라이트에선 흰색, 다크에선 한 단계 밝은 표면(surfaceElevated)이라 배경과 분리된다.
    val surface = AppColor.surfaceElevated
    val borderColor = AppColor.onSurface.copy(alpha = 0.14f)
    val tailHalf = 8.dp
    val tailHeight = 8.dp

    Box {
        Column(
            modifier = Modifier
                .clip(TooltipShape)
                .background(color = surface)
                .border(width = 0.5.dp, color = borderColor, shape = TooltipShape)
                .clickable(onClick = onClick)
                .widthIn(min = 160.dp, max = 240.dp)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            content = content,
        )

        // 카드 위/아래 모서리에서 앵커를 향해 튀어나오는 삼각 꼬리.
        TooltipTail(
            modifier = Modifier
                .align(if (pointingUp) Alignment.TopStart else Alignment.BottomStart)
                .offset {
                    IntOffset(
                        x = (tailXPx - tailHalf.toPx()).roundToInt(),
                        y = (if (pointingUp) -tailHeight.toPx() else tailHeight.toPx()).roundToInt(),
                    )
                },
            pointingUp = pointingUp,
            color = surface,
        )
    }
}

@Composable
private fun DialTooltipCard(
    arc: DialArc,
    nowMs: Long,
    pointingUp: Boolean,
    tailXPx: Float,
    onClick: () -> Unit,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {
    TooltipShell(pointingUp = pointingUp, tailXPx = tailXPx, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color = Color(arc.loop.color)),
            )
            Text(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                text = arc.loop.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = Modifier.size(8.dp))
            DialStatusPill(state = arc.state)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                modifier = Modifier.size(15.dp),
                imageVector = Icons.Rounded.Schedule,
                tint = AppColor.onSurface.copy(alpha = 0.6f),
                contentDescription = null,
            )
            Text(
                modifier = Modifier.padding(start = 6.dp),
                text = "${arc.loop.startInDay.formatHourMinute(withAmPm = false)} – " +
                        arc.loop.endInDay.formatHourMinute(withAmPm = false),
                style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
            )
        }

        // 진행 중일 때만 잔여 시간을 덧붙인다.
        if (arc.state == DialState.ACTIVE) {
            val remainingMin = ((arc.loop.endMsInDay() - nowMs) / MS_1MIN).toInt().coerceAtLeast(0)
            Text(
                text = stringResource(id = R.string.dial_remaining_minutes, remainingMin),
                style = AppTypography.labelMedium.copy(color = AppColor.primary),
            )
        }

        // 상태별 동작 버튼: 아직 응답 전이면 완료/스킵, 이미 완료·스킵했다면 되돌리기.
        TooltipActions(
            loop = arc.loop,
            state = arc.state,
            onStateChanged = onStateChanged,
        )
    }
}

/** AnyTime 노드 팝업 내용: 제목·상태(실행 중/아무때나) + 시작/정지 버튼. */
@Composable
private fun AnyTimeTooltipCard(
    loop: LoopBase,
    pointingUp: Boolean,
    tailXPx: Float,
    onClick: () -> Unit,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {
    TooltipShell(pointingUp = pointingUp, tailXPx = tailXPx, onClick = onClick) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(color = Color(loop.color)),
            )
            Text(
                modifier = Modifier
                    .padding(start = 8.dp)
                    .weight(1f),
                text = loop.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = AppTypography.bodyMedium.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
                text = stringResource(
                    id = if (loop.isInProgress) R.string.dial_running else R.string.anytime
                ),
                style = AppTypography.labelSmall.copy(
                    color = if (loop.isInProgress) AppColor.primary
                    else AppColor.onSurface.copy(alpha = 0.6f),
                ),
            )
        }

        // 시간 미지정 루프는 시작/정지로 진행한다(기존 카드에서 쓰던 버튼 재사용).
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            AnyTimeLoopStartOrStop(
                loop = loop,
                onStateChanged = onStateChanged,
            )
        }
    }
}

/** 상태 배지. 진행 중은 primary 틴트로 눈에 띄게, 나머지는 중립 톤으로 조용히 표시한다. */
@Composable
private fun DialStatusPill(state: DialState) {
    val label = stringResource(
        id = when (state) {
            DialState.ACTIVE -> R.string.dial_running
            DialState.DONE -> R.string.done
            DialState.SKIP -> R.string.skip
            DialState.UPCOMING -> R.string.dial_status_upcoming
            DialState.PAST -> R.string.dial_status_past
        }
    )
    val isActive = state == DialState.ACTIVE
    val contentColor = if (isActive) AppColor.primary else AppColor.onSurface.copy(alpha = 0.6f)
    Text(
        modifier = Modifier
            .clip(CircleShape)
            .background(
                color = if (isActive) AppColor.primary.copy(alpha = 0.14f)
                else AppColor.onSurface.copy(alpha = 0.08f),
            )
            .padding(horizontal = 8.dp, vertical = 2.dp),
        text = label,
        style = AppTypography.labelSmall.copy(color = contentColor),
    )
}

/** 말풍선 꼬리(삼각형). [pointingUp] 이면 위로, 아니면 아래로 향한다. */
@Composable
private fun TooltipTail(
    modifier: Modifier = Modifier,
    pointingUp: Boolean,
    color: Color,
) {
    Canvas(modifier = modifier.size(width = 16.dp, height = 8.dp)) {
        val w = size.width
        val h = size.height
        val path = Path().apply {
            if (pointingUp) {
                moveTo(0f, h); lineTo(w / 2f, 0f); lineTo(w, h)
            } else {
                moveTo(0f, 0f); lineTo(w / 2f, h); lineTo(w, 0f)
            }
            close()
        }
        drawPath(path = path, color = color)
    }
}

/**
 * 팝업 하단 동작 버튼.
 * - 완료/스킵 전(진행 중·예정·지남): [완료] [스킵]
 * - 이미 완료/스킵: [되돌리기](미응답 상태로 리셋)
 */
@Composable
private fun TooltipActions(
    loop: LoopBase,
    state: DialState,
    onStateChanged: (loop: LoopBase, doneState: @LoopDoneVo.DoneState Int) -> Unit,
) {
    val responded = state == DialState.DONE || state == DialState.SKIP
    Row(
        modifier = Modifier.padding(top = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (responded) {
            TooltipActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.AutoMirrored.Rounded.Undo,
                label = stringResource(id = R.string.dial_undo),
                emphasized = false,
                onClick = { onStateChanged(loop, LoopDoneVo.DoneState.NO_RESPONSE) },
            )
        } else {
            TooltipActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Check,
                label = stringResource(id = R.string.done),
                emphasized = true,
                onClick = { onStateChanged(loop, LoopDoneVo.DoneState.DONE) },
            )
            TooltipActionButton(
                modifier = Modifier.weight(1f),
                icon = Icons.Rounded.Close,
                label = stringResource(id = R.string.skip),
                emphasized = false,
                onClick = { onStateChanged(loop, LoopDoneVo.DoneState.SKIP) },
            )
        }
    }
}

/** 팝업/목록 공용 소형 동작 버튼. [emphasized] 면 primary 틴트, 아니면 중립 톤. */
@Composable
private fun TooltipActionButton(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    label: String,
    emphasized: Boolean,
    onClick: () -> Unit,
) {
    val contentColor = if (emphasized) AppColor.primary else AppColor.onSurface.copy(alpha = 0.7f)
    Row(
        modifier = modifier
            .clip(RowShape)
            .background(
                color = if (emphasized) AppColor.primary.copy(alpha = 0.12f)
                else AppColor.onSurface.copy(alpha = 0.06f),
            )
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            modifier = Modifier.size(16.dp),
            imageVector = icon,
            tint = contentColor,
            contentDescription = null,
        )
        Text(
            modifier = Modifier.padding(start = 5.dp),
            text = label,
            style = AppTypography.labelMedium.copy(color = contentColor),
        )
    }
}

// endregion

private val RowShape = RoundedCornerShape(10.dp)
private val TooltipShape = RoundedCornerShape(12.dp)
