package com.pnd.android.loop.ui.home

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.zIndex
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDay.Companion.isOn
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.util.ABB_DAYS
import com.pnd.android.loop.util.color
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.delay
import java.time.LocalDate

// 그리드 치수. 셀·행·헤더 높이를 상수로 묶어 왼쪽 이름 열과 날짜 열의 높이가 항상 정확히 맞도록 한다.
private val CellSize = 28.dp          // 실제 ✓/✕가 그려지는 정사각 셀
private val CellGap = 2.dp            // 셀 사이 여백
private val ColumnWidth = CellSize + CellGap * 2   // 날짜 한 열의 폭
private val RowHeight = CellSize + CellGap * 2     // 루프 한 행의 높이
private val NameColumnWidth = 96.dp   // 왼쪽 고정 루프 이름 열 폭
private val NameColumnCollapsedWidth = 14.dp // 완전히 접혔을 때 남겨 두는 폭(루프 색상 점은 항상 보이도록)
private val CollapseScrollDistance = 120.dp  // 이만큼 스크롤하면 이름 열이 완전히 접힌다(스크롤 양에 비례해 접힘)

private val YearBandHeight = 15.dp    // 헤더: 연도 표시 줄
private val WeekdayBandHeight = 16.dp // 헤더: 요일 줄
private val DayBandHeight = 18.dp     // 헤더: 일자(또는 월 전환) 줄
private val HeaderHeight = YearBandHeight + WeekdayBandHeight + DayBandHeight

// 완료(DONE) 채움색(뮤트 인디고). 앱 primary 파랑의 색조는 유지하되 채도를 낮춰 눈이 편하게 다듬은 값.
// 라이트/다크 각각 배경 대비를 고려해 다르게 둔다.
@Composable
private fun doneFillColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF808EF5) else Color(0xFF5567D6)

// 건너뜀(SKIP) 채움색(중립 슬레이트). 완료의 따뜻한 앰버와 뚜렷이 구분되는 무채색 계열.
@Composable
private fun skipFillColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFF565049) else Color(0xFFCFC7B6)

// 비활성(DISABLED) 셀의 바탕색. 그 위에 빗금([diagonalHatch])을 덧그린다.
@Composable
private fun disabledBgColor(): Color = AppColor.onSurface.copy(alpha = 0.04f)

// 비활성 셀의 빗금 색.
@Composable
private fun hatchColor(): Color = AppColor.onSurface.copy(alpha = 0.22f)

// 텍스트·강조·생성일 마커용 accent(블루). 완료 채움과 같은 파랑 계열이되,
// 완료 셀(인디고 채움) 위에 겹치는 생성일 마커까지 잘 보이도록 명도를 반대로 둔다:
// 라이트는 진한 블루(밝은 배경/채움 위에서 도드라짐), 다크는 옅은 블루(어두운 배경/채움 위에서 도드라짐).
@Composable
private fun accentColor(): Color =
    if (isSystemInDarkTheme()) Color(0xFFC6CDFF) else Color(0xFF123CC9)

/**
 * 전체 탭 하단 기록 그리드(색상 채움 방식의 매트릭스).
 *
 * - 행 = 루프, 열 = 가장 오래된 루프의 생성일부터 오늘까지 하루 단위.
 * - 각 셀은 그 날의 상태를 아이콘 없이 색/패턴으로 구분한다:
 *   완료=앰버 채움, 건너뜀=슬레이트 채움, 비활성=빗금, 미응답=빈 아웃라인,
 *   생성일=좌상단 앰버 삼각형 마커(그날 상태 위에 겹쳐 표시).
 * - 왼쪽 루프 이름 열은 고정하고 날짜 열만 가로로 스크롤해, 기간이 길어도 UI가 잘리지 않는다.
 * - 색은 전부 테마([AppColor])에서 가져와 라이트/다크 모드에 함께 대응한다.
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
    // 입력 중인 임시(mock) 루프는 기록이 없으므로 그리드에서 제외한다.
    val gridLoops = remember(loops) { loops.filter { !it.isMock } }
    if (gridLoops.isEmpty()) {
        AllHistoryEmpty(modifier = modifier)
        return
    }

    // 가장 오래된 생성일 ~ 오늘까지의 날짜 열을 미리 계산한다.
    // 연/월 전환 여부를 순차적으로 판단해 헤더 라벨 노출에 쓴다.
    val today = LocalDate.now()
    val columns = remember(gridLoops, today) {
        val start = gridLoops.minOf { it.created }.toLocalDate()
        buildHistoryColumns(start = start, end = today)
    }

    Column(modifier = modifier) {
        AllHistoryHeader()

        // 오른쪽 스크롤 열의 상태. 스크롤 여부에 따라 왼쪽 이름 열 노출을 제어한다.
        val listState = rememberLazyListState()
        // 최초 진입 시 가장 최근(오늘)이 보이도록 끝으로 스크롤한다.
        LaunchedEffect(columns.size) {
            if (columns.isNotEmpty()) listState.scrollToItem(columns.lastIndex)
        }

        // 현재 왼쪽 가장자리에 걸쳐 잘리지 않고 '완전히' 보이는 첫 열의 인덱스.
        // 이 열의 요일 줄에 "연도.월" 배지를 표시해, 스크롤 위치와 무관하게 항상 기준 년/월이 보이도록 한다.(스크롤 시 자동 갱신)
        val leadingIndex by remember {
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
        LaunchedEffect(listState.isScrollInProgress) {
            if (!listState.isScrollInProgress && collapseProgress.floatValue > 0f) {
                delay(1000)
                animate(
                    initialValue = collapseProgress.floatValue,
                    targetValue = 0f,
                    animationSpec = tween(durationMillis = 300),
                ) { value, _ -> collapseProgress.floatValue = value }
            }
        }

        // 접혀도 색상 점은 보이도록 최소 폭([NameColumnCollapsedWidth])까지만 줄인다.
        val nameColumnWidth = lerp(
            start = NameColumnWidth,
            stop = NameColumnCollapsedWidth,
            fraction = collapseProgress.floatValue,
        )

        Row(modifier = Modifier.padding(top = 12.dp)) {
            // 왼쪽 고정 열: 헤더 높이만큼 비운 뒤 루프 이름을 세로로 나열한다.
            // 내부 콘텐츠는 고정 폭을 유지한 채 바깥 폭만 줄여, 접힐 때 텍스트가 재배치되지 않도록 clip 한다.
            Box(
                modifier = Modifier
                    .width(nameColumnWidth)
                    .clipToBounds(),
            ) {
                HistoryNameColumn(loops = gridLoops)
            }

            LazyRow(
                modifier = Modifier
                    .weight(1f)
                    .nestedScroll(nestedScrollConnection),
                state = listState,
                contentPadding = PaddingValues(horizontal = 4.dp),
            ) {
                itemsIndexed(
                    items = columns,
                    key = { _, column -> column.dateMs },
                ) { index, column ->
                    HistoryDateColumn(
                        column = column,
                        loops = gridLoops,
                        doneHistory = doneHistory,
                        isLeading = index == leadingIndex,
                    )
                }
            }
        }
    }
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

/** 날짜 한 열: 상단 헤더(연/요일/일) + 그 아래 각 루프의 상태 셀. */
@Composable
private fun HistoryDateColumn(
    column: HistoryColumn,
    loops: List<LoopBase>,
    doneHistory: Map<Int, Map<Long, Int>>,
    isLeading: Boolean,
) {
    Column(
        modifier = Modifier
            // 배지가 좁은 열 폭을 넘어 옆 열 위로 겹칠 수 있으므로, 리딩 열을 항상 위에 그린다.
            .then(if (isLeading) Modifier.zIndex(1f) else Modifier)
            .width(ColumnWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        HistoryColumnHeader(column = column, isLeading = isLeading)
        loops.forEach { loop ->
            HistoryCell(
                loop = loop,
                column = column,
                doneState = doneHistory[loop.loopId]?.get(column.dateMs),
            )
        }
    }
}

/**
 * 날짜 열 헤더. 위에서부터 연도 / 요일 / 일자 3단이며,
 * - 연도는 [HistoryColumn.isYearChanged]일 때만(= 해가 바뀌는 지점) 표시한다.
 * - 단, 가장 왼쪽에 보이는 열([isLeading])이면 스크롤 기준점이 되도록 "연도.월" 배지를 항상 표시한다.
 * - 일자 줄은 매월 1일이면 "월/1", 그 외에는 일(day) 숫자를 보여준다.
 * - 오늘은 강조색으로 표시한다.
 */
@Composable
private fun HistoryColumnHeader(column: HistoryColumn, isLeading: Boolean) {
    val date = column.date
    val isToday = date == LocalDate.now()

    // 연도 줄(요일 위): 폭이 좁아도 잘리지 않도록 열 너비에 맞춰 가운데 정렬한다.
    // - 가장 왼쪽에 '완전히' 보이는 열([isLeading])이면 스크롤 기준점이 되도록 "연도.월" 배지를 항상 표시한다.
    //   좁은 열 폭을 넘어가는 배지는 왼쪽 정렬 + 오른쪽(대부분 비어 있는 옆 열의 연도 칸)으로 확장한다.
    // - 그 외에는 해가 바뀌는 지점에서만 연도를 표시한다.
    Box(
        modifier = Modifier
            .width(ColumnWidth)
            .height(YearBandHeight),
        contentAlignment = if (isLeading) Alignment.CenterStart else Alignment.Center,
    ) {
        when {
            isLeading -> {
                Text(
                    modifier = Modifier
                        .wrapContentWidth(align = Alignment.Start, unbounded = true)
                        .clip(RoundShapes.small)
                        .background(color = accentColor().copy(alpha = 0.12f))
                        .padding(horizontal = 4.dp, vertical = 1.dp),
                    text = "${date.year}.${date.monthValue}",
                    maxLines = 1,
                    style = AppTypography.labelSmall.copy(
                        color = accentColor(),
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }

            column.isYearChanged -> {
                Text(
                    text = "${date.year}",
                    maxLines = 1,
                    style = AppTypography.labelSmall.copy(
                        color = AppColor.onSurface,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
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
            text = stringResource(id = ABB_DAYS[date.dayOfWeek.value % 7]),
            style = AppTypography.labelSmall.copy(color = date.dayOfWeek.color()),
        )
    }

    // 일자 줄: 매월 1일은 "월/1"로 월 전환을 알린다.
    Box(
        modifier = Modifier.height(DayBandHeight),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = if (column.isFirstOfMonth) "${date.monthValue}/1" else "${date.dayOfMonth}",
            style = AppTypography.labelSmall.copy(
                color = if (isToday) accentColor() else AppColor.onSurface,
                fontWeight = if (isToday || column.isFirstOfMonth) FontWeight.Medium else FontWeight.Normal,
            ),
        )
    }
}

/**
 * 상태 셀 하나. 색/패턴만으로 상태를 구분한다.
 * - 완료=앰버 채움, 건너뜀=슬레이트 채움, 비활성(DISABLED)=빗금, 미응답=빈 아웃라인.
 * - 생성일이면 그날 상태 위에 좌상단 삼각형 마커를 겹쳐 "시작 지점"을 표시한다.
 * - 생성일 이전이거나 비활성 요일이면서 아무 기록도 없으면 흐리게 처리해 "해당 없음"으로 둔다.
 */
@Composable
private fun HistoryCell(
    loop: LoopBase,
    column: HistoryColumn,
    doneState: Int?,
) {
    val date = column.date
    val isActiveDay = remember(loop.activeDays, date) { loop.activeDays.isOn(dayForLoop(date)) }
    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
    val isBeforeCreated = remember(createdDate, date) { date.isBefore(createdDate) }
    val isCreatedDay = date == createdDate

    // 기록이 없는 칸 중, 생성일 이전이거나 비활성 요일인 날은 "해당 없음"으로 흐리게 둔다.
    // (생성일 당일은 마커를 보여야 하므로 흐리게 처리하지 않는다.)
    val isDimmed = doneState == null && !isCreatedDay && (isBeforeCreated || !isActiveDay)

    Box(
        modifier = Modifier
            .size(ColumnWidth, RowHeight)
            .padding(CellGap),
        contentAlignment = Alignment.Center,
    ) {
        StateSwatch(
            size = CellSize,
            state = doneState,
            isCreatedDay = isCreatedDay,
            dimmed = isDimmed,
        )
    }
}

/**
 * 상태 하나를 색/패턴으로 그린 정사각 셀. 그리드 셀과 범례가 동일한 모양을 공유하도록 분리했다.
 *
 * @param state DONE/SKIP/DISABLED 또는 null(미응답).
 * @param isCreatedDay 생성일이면 좌상단에 삼각형 마커를 덧그린다.
 * @param dimmed "해당 없음" 칸을 흐리게 처리할지 여부.
 */
@Composable
private fun StateSwatch(
    size: Dp,
    state: Int?,
    isCreatedDay: Boolean = false,
    dimmed: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val fillColor = when (state) {
        DoneState.DONE -> doneFillColor()
        DoneState.SKIP -> skipFillColor()
        DoneState.DISABLED -> disabledBgColor()
        else -> Color.Transparent
    }
    val hatch = hatchColor()

    Box(
        modifier = modifier
            .size(size)
            .clip(RoundShapes.small)
            .background(color = fillColor)
            .then(if (state == DoneState.DISABLED) Modifier.diagonalHatch(hatch) else Modifier)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.15f),
                shape = RoundShapes.small,
            )
            .alpha(if (dimmed) 0.3f else 1f),
    ) {
        if (isCreatedDay) {
            // 좌상단 삼각형 마커: 그날 상태 채움 위에 겹쳐 "시작 지점"임을 표시한다.
            val marker = accentColor()
            Canvas(modifier = Modifier.size(size * 0.4f)) {
                val s = this.size.minDimension
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(s, 0f)
                    lineTo(0f, s)
                    close()
                }
                drawPath(path = path, color = marker)
            }
        }
    }
}

/** 셀 배경에 좌상→우하 대각선 빗금을 그린다(비활성 상태 표시). 앞선 clip 안에서 호출돼 모서리가 둥글게 잘린다. */
private fun Modifier.diagonalHatch(
    color: Color,
    strokeWidth: Dp = 1.dp,
    gap: Dp = 4.dp,
): Modifier = drawBehind {
    val sw = strokeWidth.toPx()
    val g = gap.toPx()
    val w = size.width
    val h = size.height
    var x = 0f
    while (x <= w + h) {
        drawLine(
            color = color,
            start = Offset(x, 0f),
            end = Offset(x - h, h),
            strokeWidth = sw,
        )
        x += g
    }
}

/**
 * 도움말 팝업. 5개 상태(완료·건너뜀·응답없음·비활성화·생성일)를 그리드 셀과 똑같은 미니 셀 +
 * 이름 + 한 줄 설명으로 안내한다. 색은 전부 테마에서 가져와 라이트/다크 모두 대응한다.
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
                state = DoneState.DONE,
                name = stringResource(id = R.string.done),
                desc = stringResource(id = R.string.all_history_help_done),
            )
            HistoryHelpRow(
                state = DoneState.SKIP,
                name = stringResource(id = R.string.skip),
                desc = stringResource(id = R.string.all_history_help_skip),
            )
            HistoryHelpRow(
                state = null,
                name = stringResource(id = R.string.no_response),
                desc = stringResource(id = R.string.all_history_help_no_response),
            )
            HistoryHelpRow(
                state = DoneState.DISABLED,
                name = stringResource(id = R.string.loop_disable),
                desc = stringResource(id = R.string.all_history_help_disabled),
            )
            HistoryHelpRow(
                state = null,
                isCreatedDay = true,
                name = stringResource(id = R.string.all_history_created),
                desc = stringResource(id = R.string.all_history_help_created),
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

/** 도움말 한 줄: 상태 미니 셀 + 이름 + 설명. 셀은 그리드와 동일한 [StateSwatch]를 재사용한다. */
@Composable
private fun HistoryHelpRow(
    state: Int?,
    name: String,
    desc: String,
    isCreatedDay: Boolean = false,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        StateSwatch(size = 22.dp, state = state, isCreatedDay = isCreatedDay)
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
 * 그리드의 날짜 열 하나를 나타내는 값 객체.
 *
 * @param date 해당 열의 날짜
 * @param dateMs 자정 기준 epoch ms(= [doneHistory] 조회 키)
 * @param isFirstOfMonth 매월 1일 여부(월 전환 라벨/배경에 사용)
 * @param isYearChanged 직전 열 대비 연도가 바뀌었는지(연도 라벨 노출에 사용)
 */
private data class HistoryColumn(
    val date: LocalDate,
    val dateMs: Long,
    val isFirstOfMonth: Boolean,
    val isYearChanged: Boolean,
)

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
                date = date,
                dateMs = date.toMs(),
                isFirstOfMonth = date.dayOfMonth == 1,
                isYearChanged = prevYear == null || date.year != prevYear,
            )
        )
        prevYear = date.year
        date = date.plusDays(1)
    }
    return columns
}
