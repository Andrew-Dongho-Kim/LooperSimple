package com.pnd.android.loop.ui.home

import androidx.annotation.StringRes
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableFloatState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.vector.VectorPainter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.state.DoneState
import com.pnd.android.loop.state.NOT_SCHEDULED
import com.pnd.android.loop.state.stateLabelRes
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.CORNERS_SMALL
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest

// 그리드 치수. 셀·행·헤더 높이를 상수로 묶어 왼쪽 이름 열과 날짜 열의 높이가 항상 정확히 맞도록 한다.
private val CellSize = 28.dp          // 실제 상태가 그려지는 정사각 셀
private val CellGap = 2.dp            // 셀 사이 여백
private const val MarkSizeRatio = 0.56f // 셀 대비 기호(체크·대시) 크기 비율. 28dp 셀이면 약 16dp.
private val ColumnWidth = CellSize + CellGap * 2   // 날짜 한 열의 폭
private val RowHeight = CellSize + CellGap * 2     // 루프 한 행의 높이
private val InactiveDotSize = 4.dp    // 비활성 요일 칸 가운데에 찍는 점의 지름
private val CellBorderWidth = 0.5.dp  // 셀 아웃라인 두께
private val HatchStrokeWidth = 1.dp   // 비활성 셀 빗금 두께
private val HatchGap = 4.dp           // 비활성 셀 빗금 간격
private val NameColumnWidth = 96.dp   // 왼쪽 고정 루프 이름 열 폭
private val NameColumnCollapsedWidth = 18.dp // 완전히 접혔을 때 남겨 두는 폭(루프 색상 점은 항상 보이도록)
private val CollapseScrollDistance = 120.dp  // 이만큼 스크롤하면 이름 열이 완전히 접힌다(스크롤 양에 비례해 접힘)

private val YearBandHeight = 15.dp    // 헤더: 연도 표시 줄
private val WeekdayBandHeight = 16.dp // 헤더: 요일 줄
private val DayBandHeight = 18.dp     // 헤더: 일자(또는 월 전환) 줄
private val HeaderHeight = YearBandHeight + WeekdayBandHeight + DayBandHeight

/**
 * 그리드 한 칸을 "무엇으로 그릴지" 나타내는 코드.
 *
 * 칸의 모양은 done 상태만으로 정해지지 않는다. 같은 '기록 없음'이라도 그날이 그 루프에 해당하는
 * 날이었는지에 따라 뜻이 달라지므로(내가 안 한 날 vs 애초에 대상이 아닌 날), 판단을 [buildHistoryGrid]
 * 에서 한 번에 끝낸 뒤 그리기([drawHistoryCell])로 넘긴다.
 *
 * 셀 수가 (루프 × 일수)로 늘어나기 때문에 sealed 타입 대신 [Byte] 코드로 둔다. 행 하나의 전체 기간을
 * [ByteArray] 하나에 담으면 셀마다 객체를 만들지 않고 열 인덱스로 바로 꺼내 쓸 수 있다.
 */
private const val LOOK_BEFORE_CREATED: Byte = 0 // 루프 생성 이전. 자리만 두고 아무것도 그리지 않는다.
private const val LOOK_INACTIVE_DAY: Byte = 1   // 판정 대상이 아닌 날. 가운데 점만 찍는다.
private const val LOOK_NO_RESPONSE: Byte = 2    // 미응답. 채움 없이 아웃라인만 남는다.
private const val LOOK_DONE: Byte = 3
private const val LOOK_SKIP: Byte = 4
private const val LOOK_DISABLED: Byte = 5
private const val LOOK_IN_PROGRESS: Byte = 6

/** 공통 상태 코드를 그리드가 쓰는 [LOOK_DONE] 등의 모양 코드로 옮긴다. */
private fun Int.toLookCode(): Byte = when (this) {
    NOT_SCHEDULED -> LOOK_INACTIVE_DAY
    DoneState.DISABLED -> LOOK_DISABLED
    DoneState.DONE -> LOOK_DONE
    DoneState.SKIP -> LOOK_SKIP
    DoneState.IN_PROGRESS -> LOOK_IN_PROGRESS
    // NO_RESPONSE 와 알 수 없는 값은 모두 "채움 없는 아웃라인"으로 같게 둔다.
    else -> LOOK_NO_RESPONSE
}

/**
 * 전체 탭 하단 기록 그리드(색상 채움 방식의 매트릭스).
 *
 * - 행 = 루프, 열 = 가장 오래된 루프의 생성일부터 오늘까지 하루 단위.
 * - 각 셀은 그 날의 상태를 색 채움과 최소한의 기호로 구분한다(판단 규칙은 [buildHistoryGrid] 참고):
 *   완료=인디고 채움+체크, 건너뜀=슬레이트 채움+대시, 비활성화=빗금, 미응답=빈 아웃라인,
 *   비활성 요일=가운데 점, 생성 이전=빈칸(행이 시작되는 칸이 곧 생성일).
 * - 왼쪽 루프 이름 열은 고정하고 날짜 열만 가로로 스크롤해, 기간이 길어도 UI가 잘리지 않는다.
 * - 색은 전부 테마([AppColor])에서 가져와 라이트/다크 모드에 함께 대응한다.
 *
 * 스크롤 성능상 지켜야 하는 두 가지가 있다.
 * 1. 날짜 열 하나는 헤더 몇 개와 [Canvas] 하나로만 그린다. 셀마다 컴포저블을 두면 열이 스크롤로
 *    들어올 때마다 (루프 수 × 4)개의 레이아웃 노드와 그만큼의 벡터 서브컴포지션이 한 프레임 안에
 *    만들어져 프레임을 넘긴다.
 * 2. 스크롤에 따라 바뀌는 값([collapseProgress], 기준 열 인덱스)은 컴포지션에서 읽지 않는다.
 *    여기서 읽으면 이 함수 전체가 매 프레임 재구성되고, [doneHistory]가 unstable 한 [Map]이라
 *    아래의 열들도 전부 따라 재구성된다. 대신 레이아웃/그리기 단계에서 읽는다.
 *
 * @param loops 전체 탭에 보이는 루프 목록(생성일·색·활성 요일 정보를 사용).
 * @param doneHistory loopId -> (날짜(ms) -> done 상태) 맵. 값이 없으면 미응답으로 본다.
 */
@Composable
fun AllDoneHistoryGrid(
    modifier: Modifier = Modifier,
    loops: List<LoopBase>,
    doneHistory: Map<Int, Map<Long, Int>>,
) {
    // 비활성화된 루프와 기록이 없는 입력 중인 임시(mock) 루프는 그리드에서 제외한다.
    val gridLoops = remember(loops) { loops.filter { it.enabled && !it.isMock } }
    if (gridLoops.isEmpty()) {
        AllHistoryEmpty(modifier = modifier)
        return
    }

    // 행·열과 모든 셀의 모양을 한 번에 확정한다. 이후 스크롤 중에는 배열 인덱싱만 남는다.
    val today = LocalDate.now()
    val grid = remember(gridLoops, doneHistory, today) {
        buildHistoryGrid(loops = gridLoops, doneHistory = doneHistory, today = today)
    }
    val palette = rememberGridPalette()
    val metrics = rememberCellMetrics(cellSize = CellSize, gap = CellGap)

    Column(modifier = modifier) {
        AllHistoryHeader()

        // 오른쪽 스크롤 열의 상태. 스크롤 여부에 따라 왼쪽 이름 열 노출을 제어한다.
        val listState = rememberLazyListState()
        // 최초 진입 시 가장 최근(오늘)이 보이도록 끝으로 스크롤한다.
        LaunchedEffect(grid.columns.size) {
            if (grid.columns.isNotEmpty()) listState.scrollToItem(grid.columns.lastIndex)
        }

        // 현재 왼쪽 가장자리에 걸쳐 잘리지 않고 '완전히' 보이는 첫 열의 인덱스.
        // 이 열을 기준으로 "연도.월" 배지를 띄워, 스크롤 위치와 무관하게 항상 기준 년/월이 보이도록 한다.
        //
        // by 로 풀어 읽지 않는다. 여기서 값을 읽으면 기준 열이 바뀔 때마다 그리드 전체가 재구성되므로,
        // State 그대로 내려보내 실제로 쓰는 곳(배지와 연도 라벨)에서만 읽게 한다.
        val leadingIndex = remember(listState) {
            derivedStateOf {
                val info = listState.layoutInfo
                info.visibleItemsInfo.firstOrNull { it.offset >= info.viewportStartOffset }?.index
                    ?: listState.firstVisibleItemIndex
            }
        }

        // 이름 열 접힘 정도(0=완전히 펼침, 1=완전히 접힘). 스크롤 양에 비례해 커진다.
        val collapseProgress = remember { mutableFloatStateOf(0f) }

        // 스크롤 방향과 이동량에 따라 접힘 정도를 갱신한다. 손가락 이동량만큼 천천히 반응하도록
        // [CollapseScrollDistance]만큼 스크롤하면 완전히 접히거나 펼쳐지도록 정규화한다.
        // - 우측으로 스크롤(양수 델타)하면 날짜 그리드가 이름 열 위로 덮이며 천천히 접힌다.
        // - 좌측으로 스크롤(음수 델타)하면 반대로 천천히 다시 펼쳐진다.
        val collapseDistancePx = with(LocalDensity.current) { CollapseScrollDistance.toPx() }
        val nestedScrollConnection = remember(collapseDistancePx) {
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    val next = collapseProgress.floatValue + available.x / collapseDistancePx
                    collapseProgress.floatValue = next.coerceIn(0f, 1f)
                    return Offset.Zero
                }
            }
        }

        // 타이틀이 접혀 있는 상태에서 스크롤이 멈추면 1초 뒤에 이름 열을 애니메이션으로 다시 펼친다.
        // isScrollInProgress 를 컴포저블 본문이 아니라 snapshotFlow 안에서 읽어, 스크롤 시작/종료가
        // 그리드 재구성을 유발하지 않게 한다. collectLatest 가 다시 스크롤될 때 대기와 애니메이션을 끊는다.
        LaunchedEffect(listState, collapseProgress) {
            snapshotFlow { listState.isScrollInProgress }.collectLatest { isScrolling ->
                if (isScrolling || collapseProgress.floatValue <= 0f) return@collectLatest
                delay(1000)
                animate(
                    initialValue = collapseProgress.floatValue,
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 300),
                ) { value, _ -> collapseProgress.floatValue = value }
            }
        }

        Row(modifier = Modifier.padding(top = 12.dp)) {
            // 왼쪽 고정 열: 헤더 높이만큼 비운 뒤 루프 이름을 세로로 나열한다.
            // 내부 콘텐츠는 고정 폭을 유지한 채 바깥 폭만 줄여, 접힐 때 텍스트가 재배치되지 않도록 clip 한다.
            Box(
                modifier = Modifier
                    .clipToBounds()
                    .collapsingWidth(collapseProgress),
            ) {
                HistoryNameColumn(loops = gridLoops)
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyRow(
                    modifier = Modifier
                        .fillMaxWidth()
                        .nestedScroll(nestedScrollConnection),
                    state = listState,
                    contentPadding = PaddingValues(horizontal = 4.dp),
                ) {
                    items(
                        count = grid.columns.size,
                        key = { index -> grid.columns[index].dateMs },
                    ) { index ->
                        HistoryDateColumn(
                            grid = grid,
                            dayIndex = index,
                            palette = palette,
                            metrics = metrics,
                            leadingIndex = leadingIndex,
                        )
                    }
                }

                // 기준 열의 "연도.월" 배지. 예전에는 각 열이 자기가 기준인지 판단해 그렸는데, 그러면
                // 기준이 바뀔 때마다 보이는 열 전부가 재구성됐다. 오버레이 하나로 바꿔, 기준이 바뀌어도
                // 이 배지만 다시 그린다. (열 위에 그려지므로 zIndex 도 필요 없다.)
                LeadingYearMonthBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        // 기준 열이 실제 놓인 x 만큼 따라 움직여, 예전처럼 그 열의 왼쪽 가장자리에
                        // 붙어 보이게 한다. 배치 단계에서 읽으므로 스크롤 중에도 재구성은 없다.
                        .offset {
                            val index = leadingIndex.value
                            val x = listState.layoutInfo.visibleItemsInfo
                                .firstOrNull { it.index == index }?.offset ?: 0
                            IntOffset(x = x, y = 0)
                        },
                    columns = grid.columns,
                    leadingIndex = leadingIndex,
                )
            }
        }
    }
}

/**
 * 접힘 정도에 따라 폭만 줄이는 수정자. [progress] 를 컴포지션이 아니라 measure 단계에서 읽기 때문에,
 * 스크롤로 값이 매 프레임 바뀌어도 재구성 없이 재측정만 일어난다.
 *
 * 콘텐츠는 들어온 제약 그대로 재서 고정 폭을 유지하고, 바깥에는 줄어든 폭만 알린다.
 * 넘치는 부분은 바깥쪽 [Modifier.clipToBounds] 가 잘라낸다.
 */
private fun Modifier.collapsingWidth(progress: MutableFloatState): Modifier = layout { measurable, constraints ->
    val width = lerp(
        start = NameColumnWidth,
        stop = NameColumnCollapsedWidth,
        fraction = progress.floatValue,
    ).roundToPx().coerceIn(constraints.minWidth, constraints.maxWidth)

    val placeable = measurable.measure(constraints)
    layout(width, placeable.height) { placeable.place(0, 0) }
}

/**
 * 섹션 제목("전체 기록") + 우측 도움말(?) 버튼.
 * ? 를 누르면 상태별 색상 의미를 설명하는 팝업([HistoryHelpDialog])을 띄운다.
 */
@Composable
private fun AllHistoryHeader(modifier: Modifier = Modifier) {
    var showHelp by remember { mutableStateOf(false) }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = R.string.all_history_title),
            style = AppTypography.titleSmall.copy(color = AppColor.onSurface),
        )
        Box(
            modifier = Modifier
                .padding(start = 4.dp)
                .size(24.dp)
                .clip(CircleShape)
                .clickable { showHelp = true },
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(16.dp),
                imageVector = Icons.AutoMirrored.Outlined.HelpOutline,
                tint = AppColor.onSurface.copy(alpha = 0.5f),
                contentDescription = stringResource(id = R.string.all_history_help),
            )
        }
    }

    if (showHelp) {
        HistoryHelpDialog(onDismiss = { showHelp = false })
    }
}

/** 표시할 루프가 없을 때의 안내. (전체 탭이 비어 있으면 애초에 노출되지 않지만 방어적으로 둔다.) */
@Composable
private fun AllHistoryEmpty(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier,
        text = stringResource(id = R.string.all_history_empty),
        style = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.55f),
        ),
    )
}

/** 왼쪽 고정 열: 헤더 높이만큼의 여백 + 루프별 (색 점 + 이름) 행. */
@Composable
private fun HistoryNameColumn(
    loops: List<LoopBase>,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(NameColumnWidth)) {
        Spacer(modifier = Modifier.height(HeaderHeight))
        loops.forEach { loop ->
            Row(
                modifier = Modifier
                    .height(RowHeight)
                    .padding(end = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color = loop.color.compositeOverOnSurface()),
                )
                Text(
                    modifier = Modifier.padding(start = 6.dp),
                    text = loop.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.labelMedium.copy(color = AppColor.onSurface),
                )
            }
        }
    }
}

/**
 * 가장 왼쪽에 '완전히' 보이는 열의 "연도.월" 배지. LazyRow 위에 겹쳐 그린다.
 *
 * 기준 열 인덱스를 이 작은 컴포저블 안에서만 읽어, 스크롤로 기준이 바뀌어도 재구성 범위가
 * 여기로 한정되게 한다. 배지는 좁은 열 폭을 넘어가므로 폭 제약 없이 왼쪽 정렬로 둔다.
 */
@Composable
private fun LeadingYearMonthBadge(
    columns: List<HistoryColumn>,
    leadingIndex: State<Int>,
    modifier: Modifier = Modifier,
) {
    val column = columns.getOrNull(leadingIndex.value) ?: return
    val accent = accentColor()
    Box(
        modifier = modifier.height(YearBandHeight),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            modifier = Modifier
                .wrapContentWidth(align = Alignment.Start, unbounded = true)
                .clip(RoundShapes.small)
                .background(color = accent.copy(alpha = 0.12f))
                .padding(horizontal = 4.dp, vertical = 1.dp),
            text = column.yearMonthLabel,
            maxLines = 1,
            style = AppTypography.labelSmall.copy(
                color = accent,
                fontWeight = FontWeight.Medium,
            ),
        )
    }
}

/**
 * 날짜 한 열: 상단 헤더(연/요일/일) + 그 아래 각 루프의 상태 셀.
 *
 * 상태 셀은 전부 [Canvas] 하나에 그린다. 열이 스크롤로 들어올 때 만들어지는 레이아웃 노드가
 * 루프 수에 비례해 늘지 않아야 가로 스크롤이 프레임을 지킨다.
 */
@Composable
private fun HistoryDateColumn(
    grid: HistoryGrid,
    dayIndex: Int,
    palette: GridPalette,
    metrics: CellMetrics,
    leadingIndex: State<Int>,
) {
    Column(
        modifier = Modifier.width(ColumnWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HistoryColumnHeader(
            column = grid.columns[dayIndex],
            dayIndex = dayIndex,
            leadingIndex = leadingIndex,
        )
        Canvas(modifier = Modifier.size(ColumnWidth, RowHeight * grid.rows.size)) {
            grid.rows.forEachIndexed { rowIndex, row ->
                drawHistoryCell(
                    look = row.looks[dayIndex],
                    origin = Offset(0f, rowIndex * metrics.rowHeight),
                    palette = palette,
                    metrics = metrics,
                )
            }
        }
    }
}

/**
 * 날짜 열 헤더. 위에서부터 연도 / 요일 / 일자 3단이며,
 * - 연도는 [HistoryColumn.isYearChanged]일 때만(= 해가 바뀌는 지점) 표시한다.
 * - 일자 줄은 매월 1일이면 "월/1", 그 외에는 일(day) 숫자를 보여준다.
 * - 오늘은 강조색으로 표시한다.
 *
 * 스크롤 기준 열의 "연도.월" 배지는 [LeadingYearMonthBadge]가 오버레이로 따로 그린다.
 */
@Composable
private fun HistoryColumnHeader(
    column: HistoryColumn,
    dayIndex: Int,
    leadingIndex: State<Int>,
) {
    // 연도 줄(요일 위): 폭이 좁아도 잘리지 않도록 열 너비에 맞춰 가운데 정렬한다.
    Box(
        modifier = Modifier
            .width(ColumnWidth)
            .height(YearBandHeight),
        contentAlignment = Alignment.Center,
    ) {
        if (column.isYearChanged) {
            Text(
                // 이 열이 스크롤 기준이면 같은 자리에 배지가 겹치므로 연도 라벨은 비운다.
                // 숨김 판단을 그리기 단계에서 해, 기준이 바뀌어도 재구성이 아니라 재드로우만 일어나게 한다.
                modifier = Modifier.drawWithContent {
                    if (leadingIndex.value != dayIndex) drawContent()
                },
                text = column.yearLabel,
                maxLines = 1,
                style = AppTypography.labelSmall.copy(
                    color = AppColor.onSurface,
                    fontWeight = FontWeight.Medium,
                ),
            )
        }
    }

    // 요일 줄: 항상 표시한다. 일요일/토요일은 기존 규칙대로 색을 달리한다.
    Box(
        modifier = Modifier
            .width(ColumnWidth)
            .height(WeekdayBandHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(id = column.weekdayLabelRes),
            style = AppTypography.labelSmall.copy(color = column.dayOfWeek.color()),
        )
    }

    // 일자 줄: 매월 1일은 "월/1"로 월 전환을 알린다.
    Box(
        modifier = Modifier.height(DayBandHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = column.dayLabel,
            style = AppTypography.labelSmall.copy(
                color = if (column.isToday) accentColor() else AppColor.onSurface,
                fontWeight = if (column.isToday || column.isFirstOfMonth) {
                    FontWeight.Medium
                } else {
                    FontWeight.Normal
                },
            ),
        )
    }
}

/**
 * 셀 한 칸을 그린다. 예전에는 칸마다 Box 3개 + [Icon] 1개를 쌓았는데, 보이는 칸이 (루프 × 열)로
 * 늘어나 스크롤이 버벅였다. 여기서는 레이아웃 노드 없이 그리기 명령만 남긴다.
 *
 * @param origin 이 칸이 차지하는 [ColumnWidth] × [RowHeight] 영역의 좌상단.
 */
private fun DrawScope.drawHistoryCell(
    look: Byte,
    origin: Offset,
    palette: GridPalette,
    metrics: CellMetrics,
) {
    // 생성 이전: 아무것도 그리지 않아 날짜 열의 정렬만 유지한다.
    if (look == LOOK_BEFORE_CREATED) return

    val left = origin.x + metrics.gap
    val top = origin.y + metrics.gap

    // 비활성 요일: 아웃라인과 채움을 모두 비우고 가운데 점만 찍는다.
    // 미응답(빈 아웃라인)과 형태 자체가 달라, 훑어볼 때 "내가 안 한 날"과 섞이지 않는다.
    if (look == LOOK_INACTIVE_DAY) {
        drawCircle(
            color = palette.inactiveDot,
            radius = metrics.dot / 2f,
            center = Offset(left + metrics.cell / 2f, top + metrics.cell / 2f),
        )
        return
    }

    val cellSize = Size(metrics.cell, metrics.cell)
    val corner = CornerRadius(metrics.corner, metrics.corner)

    // 완료/건너뜀/비활성화는 채움색을 깐다. 미응답은 채움이 없어 아웃라인만 남는다.
    val fill = when (look) {
        LOOK_DONE -> palette.doneFill
        LOOK_SKIP -> palette.skipFill
        LOOK_DISABLED -> palette.disabledFill
        else -> null
    }
    if (fill != null) {
        drawRoundRect(
            color = fill,
            topLeft = Offset(left, top),
            size = cellSize,
            cornerRadius = corner,
        )
    }

    // 비활성화 셀의 좌상→우하 대각선 빗금. 둥근 모서리 밖으로 삐져나오지 않도록 셀 경로로 자른다.
    if (look == LOOK_DISABLED) {
        translate(left = left, top = top) {
            clipPath(metrics.cellPath) {
                var x = 0f
                while (x <= metrics.cell * 2) {
                    drawLine(
                        color = palette.hatch,
                        start = Offset(x, 0f),
                        end = Offset(x - metrics.cell, metrics.cell),
                        strokeWidth = metrics.hatchStroke,
                    )
                    x += metrics.hatchGap
                }
            }
        }
    }

    // 아웃라인. 스트로크는 경로 중심에 걸리므로 반 두께만큼 안으로 들여, 기존 Modifier.border 와 맞춘다.
    val inset = metrics.border / 2f
    drawRoundRect(
        color = palette.border,
        topLeft = Offset(left + inset, top + inset),
        size = Size(metrics.cell - metrics.border, metrics.cell - metrics.border),
        cornerRadius = corner,
        style = Stroke(width = metrics.border),
    )

    // 채움 위에 겹치는 기호. 완료와 건너뜀은 채움 색조만 다르기 때문에, 색을 구분하기 어려운
    // 환경에서도 뜻이 남도록 형태를 함께 준다. 비활성화(빗금)와 미응답(빈 아웃라인)은 채움/패턴만으로
    // 이미 형태가 구분되므로 기호를 두지 않는다.
    val painter = when (look) {
        LOOK_DONE -> palette.checkMark
        LOOK_SKIP -> palette.dashMark
        LOOK_IN_PROGRESS -> palette.scheduleMark
        else -> return
    }
    val tint = when (look) {
        LOOK_DONE -> palette.doneMarkColor
        LOOK_SKIP -> palette.skipMarkColor
        else -> palette.inProgressMarkColor
    }
    val markOffset = (metrics.cell - metrics.mark) / 2f
    translate(left = left + markOffset, top = top + markOffset) {
        with(painter) {
            draw(
                size = Size(metrics.mark, metrics.mark),
                colorFilter = ColorFilter.tint(tint),
            )
        }
    }
}

/**
 * 그리드 전체가 공유하는 색과 기호.
 *
 * 색은 예전에 셀마다 @Composable 함수로 꺼냈는데, 그러면 칸 수만큼 [isSystemInDarkTheme] 와
 * CompositionLocal 을 읽는다. 기호는 더 비싸서, [Icon] 에 ImageVector 를 넘기면 인스턴스마다
 * 벡터 서브컴포지션이 하나씩 생긴다. 둘 다 그리드당 한 번만 만들어 모든 칸이 돌려 쓴다.
 */
@Stable
private class GridPalette(
    /** 완료(DONE) 채움색(뮤트 인디고). 앱 primary 파랑의 색조는 유지하되 채도를 낮춰 눈이 편하게 다듬은 값. */
    val doneFill: Color,
    /** 건너뜀(SKIP) 채움색(중립 슬레이트). 완료의 인디고와 뚜렷이 구분되는 무채색 계열. */
    val skipFill: Color,
    /** 비활성(DISABLED) 셀의 바탕색. 그 위에 빗금을 덧그린다. */
    val disabledFill: Color,
    val hatch: Color,
    val border: Color,
    /**
     * 비활성 요일(그 루프가 실행되지 않는 요일) 칸에 찍는 점의 색.
     * 셀 아웃라인보다는 진해서 눈에 걸리되, 실제 상태 채움보다는 확실히 옅게 둔다.
     */
    val inactiveDot: Color,
    /**
     * 채움 위에 겹쳐 그리는 기호(체크·대시)의 색. 각 채움색 위에서 대비가 충분하도록 채움과 명도를
     * 반대로 둔다: 라이트는 밝은 채움 위 어두운 기호, 다크는 어두운 채움 위 밝은 기호.
     */
    val doneMarkColor: Color,
    val skipMarkColor: Color,
    val inProgressMarkColor: Color,
    val checkMark: VectorPainter,
    val dashMark: VectorPainter,
    val scheduleMark: VectorPainter,
)

@Composable
private fun rememberGridPalette(): GridPalette {
    val isDark = isSystemInDarkTheme()
    val onSurface = AppColor.onSurface
    val primary = AppColor.primary
    val check = rememberVectorPainter(Icons.Outlined.Check)
    val dash = rememberVectorPainter(Icons.Outlined.Remove)
    val schedule = rememberVectorPainter(Icons.Outlined.Schedule)

    return remember(isDark, onSurface, primary, check, dash, schedule) {
        GridPalette(
            doneFill = if (isDark) Color(0xFF808EF5) else Color(0xFF5567D6),
            skipFill = if (isDark) Color(0xFF565049) else Color(0xFFCFC7B6),
            disabledFill = onSurface.copy(alpha = 0.04f),
            hatch = onSurface.copy(alpha = 0.22f),
            border = onSurface.copy(alpha = 0.15f),
            inactiveDot = onSurface.copy(alpha = 0.3f),
            doneMarkColor = if (isDark) Color(0xFF1E2352) else Color.White,
            skipMarkColor = if (isDark) Color(0xFFE4DDD0) else Color(0xFF4A4438),
            inProgressMarkColor = primary,
            checkMark = check,
            dashMark = dash,
            scheduleMark = schedule,
        )
    }
}

// 헤더 배지·오늘 강조·버튼 텍스트에 쓰는 accent(블루). 완료 채움과 같은 파랑 계열이되,
// 라이트는 진한 블루, 다크는 옅은 블루로 두어 어느 배경에서도 도드라지게 한다.
@Composable
private fun accentColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFC6CDFF) else Color(0xFF123CC9)

/** 셀 하나를 그리는 데 필요한 픽셀 치수. 칸마다 dp→px 변환을 반복하지 않도록 한 번만 계산해 둔다. */
@Stable
private class CellMetrics(
    val cell: Float,
    val gap: Float,
    val corner: Float,
    val border: Float,
    val mark: Float,
    val dot: Float,
    val hatchStroke: Float,
    val hatchGap: Float,
) {
    val rowHeight = cell + gap * 2

    /** 셀 한 칸의 둥근 사각형 경로. 빗금을 자르는 데 쓰며, 칸마다 새로 만들지 않도록 여기 둔다. */
    val cellPath: Path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = Rect(left = 0f, top = 0f, right = cell, bottom = cell),
                cornerRadius = CornerRadius(corner, corner),
            )
        )
    }
}

@Composable
private fun rememberCellMetrics(cellSize: Dp, gap: Dp): CellMetrics {
    val density = LocalDensity.current
    return remember(density, cellSize, gap) { buildCellMetrics(density, cellSize, gap) }
}

private fun buildCellMetrics(density: Density, cellSize: Dp, gap: Dp): CellMetrics = with(density) {
    CellMetrics(
        cell = cellSize.toPx(),
        gap = gap.toPx(),
        corner = CORNERS_SMALL.toPx(),
        border = CellBorderWidth.toPx(),
        mark = cellSize.toPx() * MarkSizeRatio,
        dot = InactiveDotSize.toPx(),
        hatchStroke = HatchStrokeWidth.toPx(),
        hatchGap = HatchGap.toPx(),
    )
}

/**
 * 그리드가 그릴 모든 것을 확정해 담은 모델.
 *
 * 원본 [Map]<Int, Map<Long, Int>> 을 그대로 셀까지 내려보내면 두 가지가 문제가 된다. 첫째로 Compose
 * 에게 [Map] 은 unstable 이라 셀을 받는 컴포저블이 스킵되지 않는다. 둘째로 칸마다 맵을 두 번 조회하고
 * 생성일을 다시 계산한다. 그래서 여기서 한 번에 배열로 펼쳐 두고, 이후에는 인덱스로만 접근한다.
 */
@Stable
private class HistoryGrid(
    val columns: List<HistoryColumn>,
    val rows: List<HistoryRow>,
)

/** 그리드의 한 행(= 루프 하나). [looks]는 [HistoryGrid.columns] 와 같은 순서의 모양 코드다. */
@Stable
private class HistoryRow(val looks: ByteArray)

/**
 * 그리드의 날짜 열 하나. 헤더가 쓰는 라벨까지 미리 만들어 둬, 스크롤 중에 날짜 포맷팅이나
 * [LocalDate.now] 호출이 반복되지 않게 한다.
 *
 * @param dateMs 자정 기준 epoch ms(= doneHistory 조회 키이자 LazyRow 항목 key)
 * @param isFirstOfMonth 매월 1일 여부(월 전환 라벨에 사용)
 * @param isYearChanged 직전 열 대비 연도가 바뀌었는지(연도 라벨 노출에 사용)
 */
@Immutable
private class HistoryColumn(
    val dateMs: Long,
    val dayOfWeek: DayOfWeek,
    @StringRes val weekdayLabelRes: Int,
    val isFirstOfMonth: Boolean,
    val isYearChanged: Boolean,
    val isToday: Boolean,
    val yearLabel: String,
    val yearMonthLabel: String,
    val dayLabel: String,
)

/**
 * 루프 목록과 done 이력에서 그리드 전체를 만든다.
 *
 * 칸의 모양은 위에서부터 우선순위대로 판단한다. 기록이 있으면 요일 설정보다 기록을 우선하는데,
 * 나중에 활성 요일을 바꿔도 그전에 쌓인 기록은 그대로 보여야 하기 때문이다.
 * 생성일에는 따로 마커를 두지 않는다. 생성 이전이 빈칸이므로 한 행에서 처음으로 무언가 그려지는
 * 칸이 곧 생성일이고, 별도 표시는 중복이기 때문이다.
 */
private fun buildHistoryGrid(
    loops: List<LoopBase>,
    doneHistory: Map<Int, Map<Long, Int>>,
    today: LocalDate,
): HistoryGrid {
    val start = loops.minOf { it.created }.toLocalDate()
    val columns = buildHistoryColumns(start = start, end = today)

    // 날짜(ms) -> 열 인덱스. 루프마다 날짜를 처음부터 훑지 않도록 한 번만 만들어 공유한다.
    val indexByDateMs = HashMap<Long, Int>(columns.size * 2)
    columns.forEachIndexed { index, column -> indexByDateMs[column.dateMs] = index }

    val rows = loops.map { loop ->
        val createdIndex = ChronoUnit.DAYS
            .between(start, loop.created.toLocalDate())
            .coerceIn(0L, columns.size.toLong())
            .toInt()

        // 기본값: 생성 이전은 빈칸(ByteArray 의 0), 그 이후로 기록이 없는 날은 "예정 없음".
        val looks = ByteArray(columns.size)
        looks.fill(LOOK_INACTIVE_DAY, fromIndex = createdIndex)

        doneHistory[loop.loopId]?.forEach { (dateMs, state) ->
            val index = indexByDateMs[dateMs] ?: return@forEach
            // 생성 이전에 걸친 기록은 그리지 않는다(행이 시작되는 칸이 곧 생성일이어야 한다).
            if (index < createdIndex) return@forEach
            looks[index] = state.toLookCode()
        }
        HistoryRow(looks = looks)
    }

    return HistoryGrid(columns = columns, rows = rows)
}

/**
 * [start]부터 [end]까지(포함) 하루 간격의 날짜 열을 만든다.
 * 연도 전환은 직전 날짜의 연도와 비교해 판단하며, 첫 열은 항상 연도를 표시한다.
 */
private fun buildHistoryColumns(start: LocalDate, end: LocalDate): List<HistoryColumn> {
    val columns = ArrayList<HistoryColumn>()
    var date = start
    var prevYear: Int? = null
    while (!date.isAfter(end)) {
        columns.add(
            HistoryColumn(
                dateMs = date.toMs(),
                dayOfWeek = date.dayOfWeek,
                weekdayLabelRes = ABB_DAYS[date.dayOfWeek.value % 7],
                isFirstOfMonth = date.dayOfMonth == 1,
                isYearChanged = prevYear == null || date.year != prevYear,
                isToday = date == end,
                yearLabel = "${date.year}",
                yearMonthLabel = "${date.year}.${date.monthValue}",
                dayLabel = if (date.dayOfMonth == 1) {
                    "${date.monthValue}/1"
                } else {
                    "${date.dayOfMonth}"
                },
            )
        )
        prevYear = date.year
        date = date.plusDays(1)
    }
    return columns
}

/**
 * 도움말 팝업. 5개 표시(완료·건너뜀·응답없음·예정 없음·비활성화)를 그리드 셀과 똑같은 미니 셀 +
 * 이름 + 한 줄 설명으로 안내한다. 색은 전부 테마에서 가져와 라이트/다크 모두 대응한다.
 *
 * 헷갈리기 쉬운 "응답없음"과 "예정 없음"을 나란히 두어 두 모양의 차이가 바로 보이도록 배치한다.
 */
@Composable
private fun HistoryHelpDialog(onDismiss: () -> Unit) {
    // usePlatformDefaultWidth=false 로 창의 기본(거의 전체) 폭 제약을 풀고,
    // 카드 자체는 컨텐츠 폭에 맞추되(widthIn 상한) 좌우 여백만 확보한다.
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
                .widthIn(max = 320.dp)
                .clip(RoundShapes.large)
                .background(color = AppColor.surface)
                .border(
                    width = 0.5.dp,
                    color = AppColor.onSurface.copy(alpha = 0.15f),
                    shape = RoundShapes.large,
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text(
                text = stringResource(id = R.string.all_history_help_title),
                style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
            )

            HistoryHelpRow(
                look = LOOK_DONE,
                name = stringResource(DoneState.DONE.stateLabelRes()),
                desc = stringResource(id = R.string.all_history_help_done),
            )
            HistoryHelpRow(
                look = LOOK_SKIP,
                name = stringResource(DoneState.SKIP.stateLabelRes()),
                desc = stringResource(id = R.string.all_history_help_skip),
            )
            HistoryHelpRow(
                look = LOOK_NO_RESPONSE,
                name = stringResource(DoneState.NO_RESPONSE.stateLabelRes()),
                desc = stringResource(id = R.string.all_history_help_no_response),
            )
            HistoryHelpRow(
                look = LOOK_INACTIVE_DAY,
                name = stringResource(NOT_SCHEDULED.stateLabelRes()),
                desc = stringResource(id = R.string.all_history_help_inactive_day),
            )
            HistoryHelpRow(
                look = LOOK_DISABLED,
                name = stringResource(DoneState.DISABLED.stateLabelRes()),
                desc = stringResource(id = R.string.all_history_help_disabled),
            )

            // 확인 버튼: 바깥 영역 탭으로도 닫히지만, 명시적으로 닫을 수 있게 둔다.
            Text(
                modifier = Modifier
                    .align(Alignment.End)
                    .clip(RoundShapes.small)
                    .clickable(onClick = onDismiss)
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                text = stringResource(id = R.string.ok),
                style = AppTypography.labelLarge.copy(color = accentColor()),
            )
        }
    }
}

/** 도움말 한 줄: 미니 셀 + 이름 + 설명. 셀은 그리드와 동일한 [drawHistoryCell]을 재사용한다. */
@Composable
private fun HistoryHelpRow(
    look: Byte,
    name: String,
    desc: String,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StateSwatch(size = 22.dp, look = look)
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = name,
                style = AppTypography.bodyMedium.copy(color = AppColor.onSurface),
            )
            Text(
                text = desc,
                style = AppTypography.bodySmall.copy(
                    color = AppColor.onSurface.copy(alpha = 0.55f),
                ),
            )
        }
    }
}

/**
 * 범례용 미니 셀. 그리드 셀과 똑같이 보이도록 같은 그리기 코드를 쓰되, 여백 없이 셀만 채운다.
 */
@Composable
private fun StateSwatch(
    size: Dp,
    look: Byte,
    modifier: Modifier = Modifier,
) {
    val palette = rememberGridPalette()
    val metrics = rememberCellMetrics(cellSize = size, gap = 0.dp)
    Canvas(modifier = modifier.size(size)) {
        drawHistoryCell(
            look = look,
            origin = Offset.Zero,
            palette = palette,
            metrics = metrics,
        )
    }
}
