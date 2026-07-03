package com.pnd.android.loop.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.paging.compose.collectAsLazyPagingItems
import com.pnd.android.loop.R
import com.pnd.android.loop.data.FullLoopVo
import com.pnd.android.loop.data.isDone
import com.pnd.android.loop.data.isSkip
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.common.StatusBarFadingEdge
import com.pnd.android.loop.ui.common.backdropSource
import com.pnd.android.loop.ui.common.findLastFullyVisibleItemIndex
import com.pnd.android.loop.ui.common.rememberBackdropState
import com.pnd.android.loop.ui.common.rememberListCollapseProgress
import com.pnd.android.loop.ui.common.supportsBackdropBlur
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceContainer
import com.pnd.android.loop.util.formatMonthDateDay
import com.pnd.android.loop.util.formatStartEndTime
import com.pnd.android.loop.util.formatYearMonth
import com.pnd.android.loop.util.toLocalDate
import kotlinx.coroutines.launch
import kotlin.math.roundToInt
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun DailyAchievementPage(
    modifier: Modifier = Modifier,
    achievementViewModel: DailyAchievementViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val minDate by achievementViewModel.flowMinCreatedDate
        .collectAsState(initial = LocalDate.now())

    var selectedDate by remember { mutableStateOf(LocalDate.now()) }

    val lazyListState = rememberLazyListState()
    val pagerState = rememberPagerState {
        ChronoUnit.MONTHS.between(
            minDate.withDayOfMonth(1),
            LocalDate.now().withDayOfMonth(1),
        ).toInt() + 1
    }

    val coroutineScope = rememberCoroutineScope()
    val onDateSelected = remember {
        func@{ date: LocalDate ->
            if (date.isBefore(minDate) || date.isAfter(LocalDate.now())) {
                return@func
            }
            selectedDate = date
            coroutineScope.launch {
                // Scroll to the tapped date, not the previously selected one.
                val pos = calculateListItemPosition(
                    itemCount = lazyListState.layoutInfo.totalItemsCount,
                    selectedDate = date,
                )
                if (pos < 0) return@launch
                lazyListState.scrollToItem(pos)
                pagerState.scrollToPage(calculateTargetCalendarPage(date))
            }
        }
    }

    val viewMode by achievementViewModel.flowViewMode.collectAsState(
        initial = DailyAchievementPageViewMode.COLOR_DOT
    )

    // 상태바 높이. 접히는 헤더 뒤로 리스트가 스크롤되도록 리스트 상단 여백을 계산하는 데 쓴다.
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()

    // API 31+에서는 플로팅 액션 아이콘 배경에 실제 배경 블러를 적용한다(그 외에는 반투명으로 대체).
    val backdrop = rememberBackdropState()
    val headerBackdrop = if (supportsBackdropBlur) backdrop else null

    // 리스트 스크롤에 따른 헤더 접힘 정도. 리스트는 최신이 하단에 오는 역방향이라 reverseLayout=true.
    val collapseProgress by rememberListCollapseProgress(
        lazyListState = lazyListState,
        collapseDistance = AchievementHeaderCollapseDistance,
        reverseLayout = true,
    )

    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.background),
        // 상태바 영역까지 콘텐츠가 그려지도록 상태바 인셋을 제외한다(접히는 헤더 + 상태바 페이딩 엣지가 처리).
        // 내비게이션 바 인셋도 제외해, 아래 달력 패널이 내비게이션 바 영역까지 같은 배경색으로 이어지게 한다.
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .exclude(WindowInsets.statusBars)
            .exclude(WindowInsets.navigationBars),
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            DailyAchievementPageContent(
                pagerState = pagerState,
                lazyListState = lazyListState,
                achievementViewModel = achievementViewModel,
                viewMode = viewMode,
                minDate = minDate,
                selectedDate = selectedDate,
                onDateSelected = onDateSelected,
                onUpdateSelectedDate = { localDate -> selectedDate = localDate },
                backdrop = headerBackdrop,
                // 상단 리스트가 접히는 헤더 뒤로 스크롤되도록, 리스트 상단에 헤더 높이만큼 여백을 준다.
                listTopPadding = achievementHeaderExpandedHeight(topInset),
            )

            // 상태바 영역: 스크롤되는 리스트가 상태바 뒤로 부드럽게 사라지도록 페이딩 엣지를 얹는다.
            StatusBarFadingEdge(modifier = Modifier.align(Alignment.TopCenter))

            // 접히는 헤더(투명 배경): 뒤로가기+타이틀은 스크롤하면 서서히 사라지고, 액션 아이콘은 플로팅된다.
            CollapsingAchievementHeader(
                modifier = Modifier.align(Alignment.TopCenter),
                progress = collapseProgress,
                title = selectedDate.formatYearMonth(),
                isDescriptionMode = viewMode == DailyAchievementPageViewMode.DESCRIPTION_TEXT,
                onNavigateUp = onNavigateUp,
                onToggleViewMode = { achievementViewModel.toggleViewMode() },
                onMoveToToday = { onDateSelected(LocalDate.now()) },
                backdrop = headerBackdrop,
            )
        }
    }
}

/** 아래쪽 달력 패널의 위 모서리 모양. 리스트와의 경계를 명확히 하기 위해 위쪽만 둥글린다. */
private val CalendarPanelShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)

/** 달력 패널이 펼쳐졌을 때 화면에서 차지하는 세로 비율. */
private const val CalendarExpandedHeightFraction = 0.46f

/** 그래버(손잡이) 영역 높이. 접히면 이 높이(+내비게이션 바)만 남는다. */
private val CalendarGrabHandleHeight = 28.dp

/** 그래버 손잡이 알약(pill)의 크기. */
private val CalendarGrabHandleBarWidth = 36.dp
private val CalendarGrabHandleBarHeight = 4.dp

/** 손을 뗐을 때 이 속도(px/s)를 넘으면 위치와 상관없이 그 방향(위=펼침/아래=접힘)으로 스냅한다. */
private const val CalendarSettleVelocityThreshold = 500f

@Composable
private fun DailyAchievementPageContent(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    lazyListState: LazyListState,
    achievementViewModel: DailyAchievementViewModel,
    viewMode: DailyAchievementPageViewMode,
    minDate: LocalDate,
    selectedDate: LocalDate,
    onDateSelected: (LocalDate) -> Unit,
    onUpdateSelectedDate: (LocalDate) -> Unit,
    backdrop: BackdropState?,
    listTopPadding: Dp,
) {
    // 플로팅 액션 아이콘이 뒤 콘텐츠를 블러로 샘플링할 수 있도록, 배경과 함께 이 영역을 캡처한다.
    val backdropModifier = backdrop?.let { Modifier.backdropSource(it) } ?: Modifier

    // 달력 패널이 내비게이션 바 영역까지 배경을 채우도록, 그 높이만큼 패널 콘텐츠 하단에 여백을 준다.
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    // 펼침/접힘 높이를 화면 크기 기준으로 계산하기 위해 BoxWithConstraints로 감싼다.
    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .then(backdropModifier)
            .background(color = AppColor.background),
    ) {
        val density = LocalDensity.current
        // 펼침: 화면의 일정 비율. 접힘: 그래버 + 내비게이션 바만 남긴다.
        val expandedHeight = maxHeight * CalendarExpandedHeightFraction
        val collapsedHeight = CalendarGrabHandleHeight + navigationBarHeight
        // 화면(펼침/접힘 높이)이 바뀌면 접힘 상태를 새 값으로 다시 만든다.
        val collapseState = remember(density, expandedHeight, collapsedHeight) {
            with(density) {
                CalendarCollapseState(
                    collapsedPx = collapsedHeight.toPx(),
                    expandedPx = expandedHeight.toPx(),
                )
            }
        }
        val coroutineScope = rememberCoroutineScope()
        // 드래그/애니메이션에 따라 매 프레임 갱신되는 현재 패널 높이.
        val panelHeight = with(density) { collapseState.heightPx.toDp() }

        Column(modifier = Modifier.fillMaxSize()) {

            LaunchedEffect(key1 = Unit) {
                lazyListState.scrollToItem(Int.MAX_VALUE)
            }

            UpdateSelectedDate(
                pagerState = pagerState,
                lazyListState = lazyListState,
                minDate = minDate,
                onUpdateSelectedDate = onUpdateSelectedDate
            )

            // 위쪽 기록 리스트. 달력이 접힐수록 남는 공간을 그대로 채운다.
            DailyAchievementsRecords(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = Dimens.screenHorizontalPadding),
                lazyListState = lazyListState,
                achievementViewModel = achievementViewModel,
                // 리스트가 접히는 헤더 뒤로 스크롤되도록 상단 여백을 콘텐츠 패딩으로 준다.
                contentTopPadding = listTopPadding,
            )

            // 아래쪽 달력 패널. 맨 위 그래버를 위·아래로 드래그(또는 탭)해 접거나 펼 수 있다.
            CalendarPanel(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(panelHeight),
                navigationBarHeight = navigationBarHeight,
                onDrag = { deltaPx -> coroutineScope.launch { collapseState.dragBy(deltaPx) } },
                onDragStopped = { velocityPx -> coroutineScope.launch { collapseState.settle(velocityPx) } },
                onToggle = { coroutineScope.launch { collapseState.toggle() } },
            ) {
                DailyAchievementCalendar(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(top = Dimens.itemSpacing),
                    viewMode = viewMode,
                    pagerState = pagerState,
                    achievementViewModel = achievementViewModel,
                    minDate = minDate,
                    selectedDate = selectedDate,
                    onDateSelected = onDateSelected
                )
            }
        }
    }
}

/**
 * 아래쪽 달력 패널의 접힘 상태.
 *
 * 그래버를 위로 드래그하면 펼쳐지고([expandedPx]) 아래로 드래그하면 접힌다([collapsedPx]).
 * 드래그 중에는 높이가 손가락을 그대로 따라오고(snapTo), 손을 떼면 가까운 쪽으로 부드럽게
 * 스냅한다(animateTo). 높이는 픽셀 단위로 다루며, 화면 크기가 바뀌면 새로 만들어진다.
 */
@Stable
private class CalendarCollapseState(
    private val collapsedPx: Float,
    private val expandedPx: Float,
) {
    private val heightAnim = Animatable(expandedPx)

    /** 현재 패널 높이(px). 드래그·애니메이션에 따라 매 프레임 갱신된다. */
    val heightPx: Float get() = heightAnim.value

    private val midPx get() = (collapsedPx + expandedPx) / 2f

    /** 드래그 델타(px)만큼 높이를 조절한다. 아래로 끌면(delta>0) 접히는 방향이라 높이가 줄어든다. */
    suspend fun dragBy(deltaPx: Float) {
        heightAnim.snapTo((heightAnim.value - deltaPx).coerceIn(collapsedPx, expandedPx))
    }

    /** 손을 뗐을 때 속도와 위치를 함께 보고 펼침/접힘 중 가까운 쪽으로 스냅한다. */
    suspend fun settle(velocityPx: Float) {
        val target = when {
            velocityPx > CalendarSettleVelocityThreshold -> collapsedPx
            velocityPx < -CalendarSettleVelocityThreshold -> expandedPx
            heightAnim.value < midPx -> collapsedPx
            else -> expandedPx
        }
        heightAnim.animateTo(target, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    }

    /** 탭으로 접힘/펼침을 토글한다. 절반보다 펼쳐져 있으면 접고, 아니면 편다. */
    suspend fun toggle() {
        val target = if (heightAnim.value > midPx) collapsedPx else expandedPx
        heightAnim.animateTo(target, animationSpec = spring(stiffness = Spring.StiffnessMediumLow))
    }
}

/**
 * 달력을 감싸는, 살짝 떠 있는 하단 패널. 맨 위 그래버(손잡이)를 위·아래로 드래그하면
 * [onDrag]/[onDragStopped]로 패널이 접히거나 펼쳐지고, 손잡이를 탭하면 [onToggle]로 반전된다.
 *
 * 배경·테두리·그림자는 [AppColor] 토큰만 사용해 라이트/다크 모두에서 경계가 뚜렷하다.
 * 패널을 [CalendarPanelShape]로 클립해, 접히는 동안 내부 달력이 밖으로 삐져나오지 않게 한다.
 * 배경은 내비게이션 바 영역까지 이어지고, 콘텐츠만 그 높이만큼 위로 띄운다.
 */
@Composable
private fun CalendarPanel(
    modifier: Modifier = Modifier,
    navigationBarHeight: Dp,
    onDrag: (deltaPx: Float) -> Unit,
    onDragStopped: (velocityPx: Float) -> Unit,
    onToggle: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .shadow(elevation = 12.dp, shape = CalendarPanelShape, clip = false)
            .clip(CalendarPanelShape)
            .background(color = AppColor.surfaceContainer)
            .border(
                width = 1.dp,
                color = AppColor.onSurface.copy(alpha = 0.12f),
                shape = CalendarPanelShape,
            )
            .padding(bottom = navigationBarHeight),
    ) {
        CalendarGrabHandle(
            onDrag = onDrag,
            onDragStopped = onDragStopped,
            onToggle = onToggle,
        )
        content()
    }
}

/**
 * 패널 상단의 그래버(손잡이). 얇은 알약 모양 바 하나로, 위·아래 드래그로 패널을 접고 편다.
 * 탭으로도 토글되어 드래그를 모르는 사용자도 접거나 펼 수 있다.
 * 색은 onSurface 기반 반투명이라 라이트/다크 어디서나 자연스럽게 얹힌다.
 */
@Composable
private fun CalendarGrabHandle(
    modifier: Modifier = Modifier,
    onDrag: (deltaPx: Float) -> Unit,
    onDragStopped: (velocityPx: Float) -> Unit,
    onToggle: () -> Unit,
) {
    val dragState = rememberDraggableState { delta -> onDrag(delta) }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(CalendarGrabHandleHeight)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStopped = { velocity -> onDragStopped(velocity) },
            )
            // 손잡이 영역을 탭하면 접힘/펼침 토글. 리플 없이 조용히 동작하게 indication은 끈다.
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onToggle,
            ),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(width = CalendarGrabHandleBarWidth, height = CalendarGrabHandleBarHeight)
                .clip(CircleShape)
                .background(color = AppColor.onSurface.copy(alpha = 0.3f)),
        )
    }
}

/** 리스트↔달력 동기화의 주체. 사용자가 실제로 만지기 시작한 쪽만 상대를 움직인다. */
private enum class SyncDriver { NONE, LIST, PAGER }

/**
 * 리스트와 달력(pager)의 위치를 서로 동기화한다.
 *
 * 두 뷰는 양방향으로 연결돼 있다(리스트 스크롤→달력 페이지 이동, 달력 스와이프→리스트 이동).
 * 그런데 한쪽을 움직이면 상대를 '프로그램적으로' 스크롤하고, `isScrollInProgress`만 보고 판단하면
 * 그 프로그램적 스크롤이 다시 원래 쪽을 되돌리는 피드백 루프가 생긴다. 특히 리스트를 위로 튕기면,
 * 리스트가 유발한 달력 애니메이션이 리스트 fling보다 늦게 끝나서, 그 짧은 틈에 달력→리스트 동기화가
 * `scrollToItem`으로 리스트를 '해당 월 1일' 위치로 끌어당겨 최상단까지 가지 못하고 임의 지점에 멈춘다.
 *
 * 그래서 `isScrollInProgress`(프로그램적 스크롤 포함) 대신 `interactionSource`(사용자 드래그)만으로
 * 동기화의 주체를 정한다. 사용자가 드래그를 시작한 쪽을 주체로 latch하고, 양쪽이 완전히 멈추면 푼다.
 * 프로그램적 애니메이션은 드래그가 아니므로 주체를 빼앗지 못해 피드백 루프가 끊긴다.
 */
@Composable
private fun UpdateSelectedDate(
    pagerState: PagerState,
    lazyListState: LazyListState,
    minDate: LocalDate,
    onUpdateSelectedDate: (LocalDate) -> Unit,
) {
    val listDragged by lazyListState.interactionSource.collectIsDraggedAsState()
    val pagerDragged by pagerState.interactionSource.collectIsDraggedAsState()

    var driver by remember { mutableStateOf(SyncDriver.NONE) }

    // 사용자가 드래그를 시작한 쪽을 동기화 주체로 latch한다(프로그램적 스크롤로는 바뀌지 않는다).
    LaunchedEffect(listDragged, pagerDragged) {
        when {
            listDragged -> driver = SyncDriver.LIST
            pagerDragged -> driver = SyncDriver.PAGER
        }
    }
    // 리스트와 달력이 모두 완전히 멈추면(트리거된 애니메이션까지 끝) 주체를 해제한다.
    val idle = !lazyListState.isScrollInProgress && !pagerState.isScrollInProgress
    LaunchedEffect(idle) {
        if (idle) driver = SyncDriver.NONE
    }

    when (driver) {
        SyncDriver.LIST -> OnListScrolled(
            pagerState = pagerState,
            lazyListState = lazyListState,
            onUpdateSelectedDate = onUpdateSelectedDate
        )

        SyncDriver.PAGER -> OnPagerScrolled(
            pagerState = pagerState,
            lazyListState = lazyListState,
            minDate = minDate,
            onUpdateSelectedDate = onUpdateSelectedDate
        )

        SyncDriver.NONE -> Unit
    }
}

@Composable
private fun OnListScrolled(
    pagerState: PagerState,
    lazyListState: LazyListState,
    onUpdateSelectedDate: (LocalDate) -> Unit,
) {
    val total by remember {
        derivedStateOf { lazyListState.layoutInfo.totalItemsCount }
    }
    val index by remember {
        derivedStateOf { lazyListState.findLastFullyVisibleItemIndex() }
    }

    val selectedDate = LocalDate.now().minusDays((total - 1).toLong()).plusDays(index.toLong())
    onUpdateSelectedDate(selectedDate)

    LaunchedEffect(key1 = selectedDate.withDayOfMonth(1)) {
        pagerState.animateScrollToPage(calculateTargetCalendarPage(selectedDate))
    }
}

@Composable
private fun OnPagerScrolled(
    pagerState: PagerState,
    lazyListState: LazyListState,
    minDate: LocalDate,
    onUpdateSelectedDate: (LocalDate) -> Unit,
) {
    var selectedDate = LocalDate.now()
        .minusMonths(pagerState.targetPage.toLong())
        .withDayOfMonth(1)
    selectedDate = if (selectedDate.isBefore(minDate)) {
        minDate
    } else if (selectedDate.isAfter(LocalDate.now())) {
        LocalDate.now()
    } else {
        selectedDate
    }

    onUpdateSelectedDate(selectedDate)
    LaunchedEffect(key1 = selectedDate) {
        val pos = calculateListItemPosition(
            itemCount = lazyListState.layoutInfo.totalItemsCount,
            selectedDate = selectedDate,
        )
        if (pos < 0) return@LaunchedEffect
        lazyListState.scrollToItem(pos)
    }
}

fun calculateTargetCalendarPage(selectedDate: LocalDate) = ChronoUnit.MONTHS.between(
    selectedDate.withDayOfMonth(1),
    LocalDate.now().withDayOfMonth(1),
).toInt()

fun calculateListItemPosition(
    itemCount: Int,
    selectedDate: LocalDate,
) =
    (selectedDate.toEpochDay() -
            LocalDate.now().minusDays((itemCount - 1).toLong()).toEpochDay()
            ).toInt()


@Composable
private fun DailyAchievementsRecords(
    modifier: Modifier = Modifier,
    lazyListState: LazyListState,
    achievementViewModel: DailyAchievementViewModel,
    contentTopPadding: Dp = 0.dp,
) {
    val items = achievementViewModel.achievementPager.collectAsLazyPagingItems()

    LazyColumn(
        modifier = modifier,
        state = lazyListState,
        reverseLayout = true,
        // 리스트가 상단의 떠 있는 앱바 뒤로 스크롤되도록 상단에 콘텐츠 패딩을 준다.
        contentPadding = PaddingValues(top = contentTopPadding),
    ) {
        items(
            count = items.itemCount,
            key = { index -> items[index]!![0].date }
        ) { index ->
            val item = items[index]!!

            AchievementItem(
                modifier = Modifier.padding(vertical = Dimens.contentPadding),
                item = item
            )
        }
    }
}

@Composable
private fun AchievementItem(
    modifier: Modifier = Modifier,
    item: List<FullLoopVo>
) {
    val itemDate = item[0].date.toLocalDate()
    val doneList = item.filter { it.done.isDone() }
    val skipList = item.filter { it.done.isSkip() }
    // 타임라인은 완료한 루프를 먼저, 건너뛴 루프를 뒤에 이어 하나의 세로 레일로 보여준다.
    val timelineLoops = remember(item) { doneList + skipList }

    Column(modifier = modifier) {
        // 하루치 기록을 하나의 카드로 묶고, 맨 위에 달성 요약(진행바)을 얹는다.
        DayCard(modifier = Modifier.fillMaxWidth()) {
            DaySummaryHeader(
                itemDate = itemDate,
                doneCount = doneList.size,
                totalCount = timelineLoops.size,
            )

            if (timelineLoops.isEmpty()) {
                EmptyDayContent(modifier = Modifier.padding(top = Dimens.contentPadding))
                return@DayCard
            }

            Column(modifier = Modifier.padding(top = Dimens.contentPadding)) {
                timelineLoops.forEachIndexed { index, loop ->
                    key(loop.loopId) {
                        TimelineLoopRow(
                            loop = loop,
                            itemDate = itemDate,
                            isDone = loop.done.isDone(),
                            isLast = index == timelineLoops.lastIndex,
                        )
                    }
                }
            }
        }
    }
}

/**
 * 하루치 기록을 담는 카드. 라이트/다크 모두에서 배경과 자연스럽게 분리되도록
 * 옅은 컨테이너 배경 위에 머리카락 굵기의 테두리를 얹는다.
 */
@Composable
private fun DayCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(RoundShapes.large)
            .background(AppColor.surfaceContainer)
            .border(
                width = 0.5.dp,
                color = AppColor.onSurface.copy(alpha = 0.1f),
                shape = RoundShapes.large,
            )
            .padding(vertical = Dimens.contentPadding),
        content = content,
    )
}

/**
 * 하루 요약 헤더. 날짜와 함께 달성률을 색이 있는 퍼센트 알약으로 보여주고, 아래에 아주 얇은
 * 헤어라인으로 진행 정도를 은은하게 얹는다. 알약·헤어라인 색은 달성 단계([progressTierOf])에 따라
 * 회색→앰버→파랑→초록으로 바뀌어, 강렬한 진행바 대신 색으로 성취 정도를 전한다.
 * 오늘이면 날짜를 primary 색으로 강조한다.
 */
@Composable
private fun DaySummaryHeader(
    modifier: Modifier = Modifier,
    itemDate: LocalDate,
    doneCount: Int,
    totalCount: Int,
) {
    val isToday = itemDate == LocalDate.now()
    // 기록이 없는 날(totalCount == 0)은 0f로 두고 알약·헤어라인을 그리지 않는다.
    val fraction = if (totalCount > 0) doneCount.toFloat() / totalCount else 0f
    val tier = progressTierOf(fraction)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.contentPadding),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier.weight(1f),
                text = itemDate.formatMonthDateDay(),
                style = AppTypography.titleSmall.copy(
                    color = if (isToday) AppColor.primary else AppColor.onSurface.copy(alpha = 0.9f),
                    fontWeight = FontWeight.SemiBold,
                ),
            )
            // 기록이 있는 날에만 달성률 알약을 보여준다. 단계 색의 옅은 배경 위에 진한 같은 계열 텍스트.
            if (totalCount > 0) {
                Text(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(tier.container)
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    text = "${(fraction * 100).roundToInt()}%",
                    style = AppTypography.labelMedium.copy(
                        color = tier.accent,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
            }
        }
    }
}

/**
 * 달성 비율에 따른 진행 색상 단계. 강렬한 단색 진행바 대신, 담담한 회색(저조)→앰버(중간)→
 * 파랑(높음)→초록(완료)으로 바뀌며 성취 정도를 색으로 전한다.
 * [accent]는 알약 텍스트·헤어라인 채움 색, [container]는 알약의 옅은 배경색이다.
 */
@Immutable
private data class ProgressTier(val accent: Color, val container: Color)

/** [fraction](0~1)을 4단계로 나눠 색을 고른다. 라이트/다크 각각 대비가 유지되도록 색을 따로 지정한다. */
@Composable
private fun progressTierOf(fraction: Float): ProgressTier {
    val isDark = isSystemInDarkTheme()
    return when {
        // 완료: 성취를 강조하는 초록. 파랑(primary)과 구분돼 "다 했다"가 한눈에 보인다.
        fraction >= 1f -> if (isDark) {
            ProgressTier(accent = Color(0xFF7DD3A8), container = Color(0xFF7DD3A8).copy(alpha = 0.16f))
        } else {
            ProgressTier(accent = Color(0xFF1E7A46), container = Color(0xFF1E7A46).copy(alpha = 0.12f))
        }
        // 높음(67~99%): 앱의 기본 강조색인 파랑.
        fraction >= 0.67f -> ProgressTier(
            accent = AppColor.primary,
            container = AppColor.primary.copy(alpha = if (isDark) 0.20f else 0.12f),
        )
        // 중간(34~66%): 진행 중임을 따뜻하게 알리는 앰버.
        fraction >= 0.34f -> if (isDark) {
            ProgressTier(accent = Color(0xFFE0A93C), container = Color(0xFFE0A93C).copy(alpha = 0.18f))
        } else {
            ProgressTier(accent = Color(0xFF8A5A00), container = Color(0xFF8A5A00).copy(alpha = 0.12f))
        }
        // 저조(0~33%): 판단·경고 느낌 없이 담담한 중립 회색.
        else -> ProgressTier(
            accent = AppColor.onSurface.copy(alpha = if (isDark) 0.60f else 0.50f),
            container = AppColor.onSurface.copy(alpha = if (isDark) 0.14f else 0.08f),
        )
    }
}

/** 기록이 하나도 없는 날 카드 안에 보여주는 안내 문구. */
@Composable
private fun EmptyDayContent(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.contentPadding),
        text = stringResource(id = R.string.no_achievements),
        textAlign = TextAlign.Center,
        style = AppTypography.bodyMedium.copy(
            color = AppColor.onSurface.copy(alpha = 0.5f)
        )
    )
}

/**
 * 타임라인의 한 줄. 왼쪽 레일(점 + 연결선)과 오른쪽 내용(제목·시간·회고)으로 이뤄진다.
 * 완료한 루프는 루프 색으로 채운 점, 건너뛴 루프는 빈 회색 링 + 취소선 제목으로 구분한다.
 * 마지막 줄([isLast])은 아래 연결선을 그리지 않는다.
 */
@Composable
private fun TimelineLoopRow(
    modifier: Modifier = Modifier,
    loop: FullLoopVo,
    itemDate: LocalDate,
    isDone: Boolean,
    isLast: Boolean,
) {
    val loopColor = loop.color.compositeOverOnSurface()
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = Dimens.contentPadding)
            // 레일의 연결선이 다음 점까지 닿도록, 행 높이를 내용 높이에 맞춘다.
            .height(IntrinsicSize.Min),
    ) {
        TimelineRail(color = loopColor, isDone = isDone, isLast = isLast)

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = Dimens.contentPadding)
                // 마지막 줄이 아니면 다음 점과의 간격만큼 아래 여백을 준다.
                .padding(bottom = if (isLast) 0.dp else Dimens.contentPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // 제목(+생성 배지)은 왼쪽에서 필요한 만큼 차지하고, 시간은 같은 줄 오른쪽 끝으로 민다.
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        // 제목이 길면 줄여서 시간이 밀리지 않게 한다.
                        modifier = Modifier.weight(1f, fill = false),
                        text = loop.title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        style = AppTypography.bodyLarge.copy(
                            color = if (isDone) {
                                AppColor.onSurface
                            } else {
                                AppColor.onSurface.copy(alpha = 0.5f)
                            },
                            fontWeight = FontWeight.Medium,
                            textDecoration = if (isDone) null else TextDecoration.LineThrough,
                        ),
                    )

                    val createdDate = remember(loop.created) { loop.created.toLocalDate() }
                    if (itemDate == createdDate) {
                        CreatedBadge(modifier = Modifier.padding(start = Dimens.itemSpacing))
                    }
                }

                Text(
                    modifier = Modifier.padding(start = Dimens.itemSpacing),
                    text = loop.formatStartEndTime(),
                    maxLines = 1,
                    style = AppTypography.labelMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.5f),
                    ),
                )
            }

            if (loop.retrospect.isNotEmpty()) {
                Text(
                    modifier = Modifier.padding(top = Dimens.itemSpacing),
                    text = "“${loop.retrospect}”",
                    style = AppTypography.bodyMedium.copy(
                        color = AppColor.onSurface.copy(alpha = 0.55f),
                        lineHeight = 18.sp,
                    ),
                )
            }
        }
    }
}

/** 타임라인 왼쪽 레일. 위쪽 상태 점과, 다음 줄로 이어지는 세로 연결선으로 구성된다. */
@Composable
private fun TimelineRail(
    modifier: Modifier = Modifier,
    color: Color,
    isDone: Boolean,
    isLast: Boolean,
) {
    Column(
        modifier = modifier.fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // 제목 첫 줄의 세로 중심에 점이 오도록 살짝 내려서 그린다.
        Box(
            modifier = Modifier
                .padding(top = 5.dp)
                .size(11.dp)
                .then(
                    if (isDone) {
                        Modifier.background(color = color, shape = CircleShape)
                    } else {
                        Modifier.border(
                            width = 1.5.dp,
                            color = AppColor.onSurface.copy(alpha = 0.4f),
                            shape = CircleShape,
                        )
                    }
                ),
        )
        if (!isLast) {
            Box(
                modifier = Modifier
                    .padding(vertical = 3.dp)
                    .width(1.5.dp)
                    .weight(1f)
                    .background(AppColor.onSurface.copy(alpha = 0.12f)),
            )
        }
    }
}

/** 해당 날짜에 처음 만들어진 루프임을 알리는 알약형 배지. */
@Composable
private fun CreatedBadge(modifier: Modifier = Modifier) {
    Text(
        modifier = modifier
            .clip(CircleShape)
            .background(AppColor.error.copy(alpha = 0.12f))
            .padding(horizontal = 6.dp, vertical = 1.dp),
        text = stringResource(id = R.string.created),
        style = AppTypography.labelSmall.copy(
            color = AppColor.error.copy(alpha = 0.9f)
        )
    )
}

