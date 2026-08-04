package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.common.MAX_LOOPS_TOGETHER
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.common.NavigationBarFadingEdge
import com.pnd.android.loop.ui.common.StatusBarFadingEdge
import com.pnd.android.loop.ui.common.backdropSource
import com.pnd.android.loop.ui.common.isPortrait
import com.pnd.android.loop.ui.common.rememberBackdropState
import com.pnd.android.loop.ui.common.supportsBackdropBlur
import com.pnd.android.loop.ui.home.input.UserInput
import com.pnd.android.loop.ui.home.input.UserInputState
import com.pnd.android.loop.ui.home.input.selector.MIN_DIFF_MINUTES
import com.pnd.android.loop.ui.home.input.selector.isLoopDurationTooShort
import com.pnd.android.loop.ui.home.input.loopInputPanelBackgroundColor
import com.pnd.android.loop.ui.home.input.rememberUserInputState
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.isActive
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toMs
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDateTime

@Composable
fun Home(
    modifier: Modifier = Modifier,
    loopViewModel: LoopViewModel,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
) {
    val context = LocalContext.current
    val inputState = rememberUserInputState(context = context)
    val snackBarHostState = remember { SnackbarHostState() }
    val blurState = rememberBlurState()

    // Today / All selection is owned here so both the pinned tab in the app bar and the
    // scrolling content (header stats + loop list) stay in sync from a single source.
    var selectedTab by rememberSaveable { mutableIntStateOf(HomeTab.TODAY) }
    // Opening the input reveals the new / edited loop, which only the All list shows.
    LaunchedEffect(inputState.isOpen) {
        if (inputState.isOpen) selectedTab = HomeTab.ALL
    }

    // Selecting 오늘 also closes the input so the list doesn't jump back to 전체.
    val onTabSelected: (Int) -> Unit = { tab ->
        selectedTab = tab
        if (tab == HomeTab.TODAY) inputState.close(context)
    }


    val contentWindowInsets = if (LocalConfiguration.current.isPortrait()) {
        ScaffoldDefaults.contentWindowInsets
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.statusBars)
            .exclude(WindowInsets.ime)
    } else {
        ScaffoldDefaults.contentWindowInsets
            .add(WindowInsets.displayCutout)
            .exclude(WindowInsets.statusBars)
            .exclude(WindowInsets.ime)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColor.background)
            .blur(radius = blurState.radius),
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    // 스낵바가 떠 있는 추가 버튼 위에 걸리도록, 버튼 크기/여백에서 파생한 여유를 둔다.
                    .padding(
                        bottom = Dimens.floatingInputButtonMargin +
                            Dimens.floatingInputButtonSize + 16.dp
                    )
                    .padding(
                        bottom = if (inputState.isSelectorOpened) {
                            dimensionResource(id = R.dimen.user_input_selector_content_height)
                        } else {
                            0.dp
                        }
                    ),
                hostState = snackBarHostState
            )
        },
        containerColor = Color.Transparent,
        contentColor = Color.Transparent,
        // The floating app bar and pinned tabs handle the status-bar area themselves, so the
        // scrolling content is allowed to draw all the way up behind them.
        contentWindowInsets = contentWindowInsets,
    )
    { contentPadding ->
        HomeScaffoldContent(
            modifier = Modifier.padding(contentPadding),
            blurState = blurState,
            inputState = inputState,
            snackBarHostState = snackBarHostState,
            loopViewModel = loopViewModel,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToGroupPage = onNavigateToGroupPage,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )
    }
}


@Composable
private fun HomeScaffoldContent(
    modifier: Modifier,
    blurState: BlurState,
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToGroupPage: () -> Unit,
    onNavigateToStatisticsPage: () -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    Box(modifier = modifier) {
        val lazyListState = rememberLazyListState()
        // A real backdrop blur is only available on API 31+; elsewhere the floating surfaces fall
        // back to translucent white, so we only bother capturing the content where it's used.
        val backdrop = rememberBackdropState()
        val headerBackdrop = if (supportsBackdropBlur) backdrop else null

        HomeBody(
            // 내비게이션 바 영역까지 콘텐츠가 그려지도록 navigationBarsPadding을 두지 않는다.
            modifier = Modifier
                .fillMaxHeight()
                .imePadding(),
            blurState = blurState,
            inputState = inputState,
            snackBarHostState = snackBarHostState,
            lazyListState = lazyListState,
            backdrop = headerBackdrop,
            loopViewModel = loopViewModel,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onNavigateToGroupPicker = onNavigateToGroupPicker,
            onNavigateToDetailPage = onNavigateToDetailPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
        )

        // 상태바 영역의 페이딩 엣지: 스크롤되는 리스트 위, 떠 있는 헤더 알약(pill) 아래에 그려서
        // 콘텐츠가 상태바 밑으로 자연스럽게 사라지도록 한다.
        StatusBarFadingEdge(modifier = Modifier.align(Alignment.TopCenter))

        // 내비게이션 바 영역 처리:
        // - 평소에는 콘텐츠가 하단으로 자연스럽게 사라지도록 페이딩 엣지를 그린다.
        // - 루프 추가 UX가 활성화되면 페이딩 엣지 대신, 입력 패널과 동일한 배경/알파로 채워
        //   패널이 화면 하단까지 이어져 보이도록 한다.
        if (inputState.isVisible) {
            NavigationBarInputScrim(
                modifier = Modifier.align(Alignment.BottomCenter),
                isSelectorOpened = inputState.isSelectorOpened,
            )
        } else {
            NavigationBarFadingEdge(modifier = Modifier.align(Alignment.BottomCenter))
        }

        // 루프가 하나도 없고 입력도 닫혀 있으면(= OOBE 빈 화면) 오늘/전체 탭은 의미가 없으므로
        // 숨긴다. 첫 루프가 추가되거나 입력이 열리면 다시 나타난다.
        val allLoops by loopViewModel.allLoopsWithDoneStates.collectAsState(initial = null)
        val showTabs = !allLoops.isNullOrEmpty() || !inputState.isModeNone

        // The collapsing action bar: a plain app bar at rest that, as the list scrolls, fades out
        // the title and floats the icons (in place) and the 오늘/전체 tabs (slid up to the left).
        val collapseProgress by rememberHomeHeaderCollapseProgress(lazyListState)
        CollapsingHomeHeader(
            modifier = Modifier.align(Alignment.TopCenter),
            progress = collapseProgress,
            loopViewModel = loopViewModel,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            showTabs = showTabs,
            onNavigateToGroupPage = onNavigateToGroupPage,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            backdrop = headerBackdrop,
        )

        val context = LocalContext.current
        val scope = rememberCoroutineScope()
        UserInput(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .imePadding()
                .then(if (LocalConfiguration.current.isPortrait()) Modifier.navigationBarsPadding() else Modifier),
            inputState = inputState,
            snackBarHostState = snackBarHostState,
            lazyListState = lazyListState,
            backdrop = headerBackdrop,
            onEnsureLoop = { loop ->
                ensureLoop(
                    context = context,
                    loopViewModel = loopViewModel,
                    loop = loop,
                    hostState = snackBarHostState
                )
            },
            onLoopSubmitted = { newLoop ->
                // created == 0L 은 아직 저장된 적 없는 새 루프. 이 경우에만 추가 확인 + 실행취소를
                // 제공해, 빠른 시작(템플릿) 추가와 동일한 피드백으로 통일한다. 기존 루프 편집은
                // 되돌릴 값이 다르므로 그대로 갱신만 한다.
                val isNewLoop = newLoop.created == 0L
                if (isNewLoop) {
                    scope.launch {
                        val added = loopViewModel.addLoopReturning(
                            newLoop.asLoopVo(created = LocalDateTime.now().toMs())
                        )
                        val result = snackBarHostState.showSnackbar(
                            message = context.getString(R.string.oobe_loop_added, added.title),
                            actionLabel = context.getString(R.string.action_undo),
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            loopViewModel.deleteLoop(added)
                        }
                    }
                } else {
                    loopViewModel.addOrUpdateLoop(newLoop.asLoopVo(created = newLoop.created))
                }
            }
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .background(
                    color = if (blurState.isOn) {
                        AppColor.onSurface.copy(
                            alpha = if (isSystemInDarkTheme()) 0.1f else 0.2f
                        )
                    } else {
                        Color.Transparent
                    }
                )
        )
    }
}

/**
 * 루프 추가 UX가 활성화됐을 때 내비게이션 바 영역을 채우는 배경. 입력 패널이 화면 하단까지
 * 자연스럽게 이어져 보이도록 한다.
 * - Selector가 열려 있으면 Selector(surfaceElevated, 불투명)와 동일한 색으로 채운다.
 * - 그 외에는 입력 패널과 동일한 반투명 배경([loopInputPanelBackgroundColor])을 사용한다.
 */
@Composable
private fun NavigationBarInputScrim(
    isSelectorOpened: Boolean,
    modifier: Modifier = Modifier,
) {
    val navigationBarHeight = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val color = if (isSelectorOpened) AppColor.surfaceElevated else loopInputPanelBackgroundColor()
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(navigationBarHeight)
            .background(color = color),
    )
}

@Composable
private fun HomeBody(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    inputState: UserInputState,
    snackBarHostState: SnackbarHostState,
    lazyListState: LazyListState,
    backdrop: BackdropState?,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    val sections by loopViewModel.observeSectionsAsState(
        inputState = inputState,
        selectedTab = selectedTab,
    )
    val onEdit = remember { { loop: LoopBase -> inputState.edit(loop) } }
    val onDelete = remember { { loop: LoopBase -> loopViewModel.deleteLoop(loop) } }
    val onStateChanged: (LoopBase, Int) -> Unit = remember {
        { loop, doneState ->
            loopViewModel.changeLoopState(loop = loop, doneState = doneState)
        }
    }

    // The collapsing header (title + icons + tabs) is drawn as an overlay, so the list simply
    // starts below its expanded height and then scrolls up underneath it. This content is also
    // the source the floating surfaces sample for their backdrop blur (API 31+ only).
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    // 루프가 없는 빈 상태에서는 헤더에 탭 행이 없으므로 그만큼 높이를 줄여, 콘텐츠가
    // 불필요한 공백 없이 위로 올라오게 한다. (탭 노출 조건은 헤더의 showTabs와 동일)
    val headerHeight = homeHeaderExpandedHeight(topInset, includeTabs = !sections.isNullOrEmpty())
    val backdropModifier = backdrop?.let { Modifier.backdropSource(it) } ?: Modifier

    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 하단 패널에서 수정 중인 루프 id. 목록의 해당 카드를 스포트라이트(강조 테두리 + "수정 중" 배지)
    // 하고 나머지 카드는 흐리게 물러나게 하는 데 쓴다.
    val editingLoopId = if (inputState.mode == UserInputState.Mode.Edit) {
        inputState.value.loopId
    } else {
        null
    }

    // 새 루프(New 모드) 진입 시 목록을 맨 위로 보내 새 카드가 보이게 하는 스크롤은 UserInput의
    // SideEffect가 이미 담당한다. 여기서 mode를 key로 또 스크롤하면 빈 제목에서 일어나는
    // None↔New 토글마다 재실행되어 그 스크롤과 경쟁하고, 결국 텍스트 입력 포커스가 흔들린다.
    // 그래서 New 스크롤은 여기서 다루지 않고 Edit 케이스만 처리한다.

    // 편집을 시작하면 대상 카드가 떠 있는 헤더/입력 패널에 가리지 않도록 목록에서 보이게 스크롤한다.
    // 편집 진입은 오늘→전체 탭 전환과 목록 재구성을 함께 유발하므로, 레이아웃을 한 번만 읽지 않고
    // snapshotFlow로 대상 카드가 실제 배치에 나타날 때까지 기다렸다가 스크롤한다. (editingLoopId가
    // 바뀌면 LaunchedEffect가 취소되므로 카드가 끝내 나타나지 않아도 매달려 있을 뿐 부작용은 없다.)
    // contentPadding(top = headerHeight) 덕분에 animateScrollToItem은 카드를 헤더 아래에 정렬하고,
    // 목록의 imePadding으로 뷰포트가 줄어 키보드에도 가리지 않는다.
    LaunchedEffect(editingLoopId) {
        if (editingLoopId == null) return@LaunchedEffect
        val index = snapshotFlow {
            lazyListState.layoutInfo.visibleItemsInfo
                .firstOrNull { it.key == editingLoopId }
                ?.index
        }.filterNotNull().first()
        lazyListState.animateScrollToItem(index)
    }

    // backdropSource wraps the background so the captured layer (sampled by the blur) includes it.
    Box(
        modifier = modifier
            .then(backdropModifier)
            .background(AppColor.background)
    ) {
        val currentSections = sections
        if (currentSections == null) {
            // 아직 DB 로딩 전 — 빈 상태(EmptyLoops)를 그리면 깜빡이므로 그리지 않는다.
            // 다만 로딩이 길어질 때는 멈춘 화면처럼 보이지 않도록 잠깐 뒤 스피너를 띄운다.
            LoadingLoops(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeight)
            )
        } else if (currentSections.isEmpty()) {
            EmptyLoops(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeight),
                // 빠른 시작 템플릿 선택 시, 입력창 제출과 동일하게 생성 시각을 채워 바로 추가한다.
                onSelectTemplate = { template ->
                    scope.launch {
                        val added = loopViewModel.addLoopReturning(
                            template.asLoopVo(created = LocalDateTime.now().toMs())
                        )
                        // 추가한 루프가 오늘 예정이 아니어도 곧바로 보이도록 전체 탭으로 전환한다.
                        // (오늘 탭만 보고 "오늘 완료" 상태로 오인하는 것을 방지)
                        onTabSelected(HomeTab.ALL)
                        // 추가 확인 + 실행취소: 오탭으로 들어간 루프를 즉시 되돌릴 수 있게 한다.
                        val result = snackBarHostState.showSnackbar(
                            message = context.getString(R.string.oobe_loop_added, added.title),
                            actionLabel = context.getString(R.string.action_undo),
                            duration = SnackbarDuration.Long,
                        )
                        if (result == SnackbarResult.ActionPerformed) {
                            loopViewModel.deleteLoop(added)
                        }
                    }
                },
                // '직접 추가'는 하단 입력 편집기를 열어 사용자가 원하는 루프를 직접 만들게 한다.
                onCreateManually = { inputState.open(context) },
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(top = headerHeight),
            ) {
                currentSections.forEach { section ->
                    section(
                        section = section,
                        blurState = blurState,
                        loopViewModel = loopViewModel,
                        snackBarHostState = snackBarHostState,
                        selectedTab = selectedTab,
                        editingLoopId = editingLoopId,
                        onEdit = onEdit,
                        onDelete = onDelete,
                        onStateChanged = onStateChanged,
                        onNavigateToGroupPicker = onNavigateToGroupPicker,
                        onNavigateToDetailPage = onNavigateToDetailPage,
                        onNavigateToHistoryPage = onNavigateToHistoryPage,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(Dimens.bottomContentClearance))
                }
            }
        }
    }
}

/**
 * DB 로딩 중 표시하는 지연 인디케이터. 대부분의 로딩은 한 프레임 안에 끝나므로, 바로 스피너를
 * 그리면 깜빡임만 남긴다. 그래서 [DELAY_MS]만큼 지난 뒤에도 여전히 로딩 중일 때만 스피너를 띄워,
 * 짧은 로딩은 조용히 지나가고 길어질 때만 "동작 중" 신호를 준다.
 */
@Composable
private fun LoadingLoops(modifier: Modifier = Modifier) {
    var showSpinner by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(LOADING_SPINNER_DELAY_MS)
        showSpinner = true
    }
    if (showSpinner) {
        Box(modifier = modifier, contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                modifier = Modifier.size(28.dp),
                color = AppColor.primary,
                strokeWidth = 2.dp,
            )
        }
    }
}

private const val LOADING_SPINNER_DELAY_MS = 300L

@Composable
private fun LoopViewModel.observeSectionsAsState(
    inputState: UserInputState,
    @HomeTab.Type selectedTab: Int,
): State<List<Section>?> {
    // null = 아직 DB 로딩 전. 이 동안에는 빈 상태(EmptyLoops)를 그리지 않고 sections도 null로 둔다.
    val loops by allLoopsWithDoneStates.collectAsState(initial = null)
    val yesterdayLoops by loopsNoResponseYesterday.collectAsState(initial = emptyList())

    val loadedLoops = loops
    return if (loadedLoops == null) {
        remember { mutableStateOf<List<Section>?>(null) }
    } else if (loadedLoops.isEmpty() && inputState.mode == UserInputState.Mode.None) {
        remember { mutableStateOf(emptyList()) }
    } else {
        val resultLoops = ArrayList<LoopBase>(
            // TODAY 탭: 오늘 활성인 루프에 더해, 자정을 넘겨 지금도 진행 중인(어제 시작한) 루프도 포함한다.
            if (selectedTab == HomeTab.TODAY) loadedLoops.filter { loop -> loop.enabled && (loop.isActiveDay() || loop.isActive()) } else loadedLoops)
        if (inputState.mode == UserInputState.Mode.Edit) {
            val edited = inputState.value
            val index = resultLoops.indexOfFirst { it.loopId == edited.loopId }
            // The edited loop may have disappeared from the latest DB emission
            // (deleted or filtered out) — only replace it when it still exists.
            if (index != -1) resultLoops[index] = edited
        }


        val isAllTab = selectedTab == HomeTab.ALL
        val todayOrAllSection = if (isAllTab) {
            rememberAllSection(resultLoops, inputState)
        } else {
            rememberTodaySection(resultLoops)
        }
        val sections = buildList {
            add(rememberHeaderSection(resultLoops))
            add(todayOrAllSection)
            // 전체 탭에서는 어제 미응답 루프 카드를 노출하지 않는다.
            if (!isAllTab) add(rememberYesterdaySection(yesterdayLoops))
            add(rememberAdSection())
            // 전체 탭에서는 오늘 Done/Skip한 루프 정보를 노출하지 않는다.
            // 타임라인 뷰는 자체적으로 완료/스킵 상태를 표시하므로 카드를 생략하고,
            // 리스트·다이얼 뷰에서는 완료/스킵 루프를 별도 Done/Skip 카드로 모아 보여준다.
            // (다이얼은 완료/스킵 루프를 다이얼에서 제외하고 이 카드로만 표시한다.)
            val todayViewMode = (todayOrAllSection as? Section.Today)?.viewMode?.value
            val showDoneSkipCard =
                todayViewMode == TodayViewMode.LIST || todayViewMode == TodayViewMode.DIAL
            if (!isAllTab && showDoneSkipCard) add(rememberDoneSection(resultLoops))
            // 전체 탭 하단에는 전체 루프의 done/skip 이력을 매트릭스 그리드로 덧붙인다.
            if (isAllTab) add(rememberAllHistoryGridSection(resultLoops))
        }.filter { it.size > 0 }

        remember(sections) { mutableStateOf(sections) }
    }
}


@Composable
private fun rememberHeaderSection(
    loops: List<LoopBase>,
): Section = remember {
    Section.HeaderCard()
}.apply {
    items.value = loops
}


@Composable
private fun rememberYesterdaySection(
    loops: List<LoopBase>,
): Section {
    return rememberSaveable(saver = Section.Yesterday.Saver) {
        Section.Yesterday()
    }.apply {
        items.value = loops
    }
}

@Composable
private fun rememberTodaySection(
    loops: List<LoopBase>,
): Section {
    val context = LocalContext.current
    return rememberSaveable(saver = Section.Today.Saver) {
        Section.Today().apply { load(context) }
    }.apply {
        items.value = loops
    }
}


@Composable
private fun rememberAllSection(
    loops: List<LoopBase>,
    inputState: UserInputState,
): Section {

    val resultLoops = remember(loops, inputState.mode, inputState.value) {
        loops.toMutableList().apply {
            if (inputState.mode == UserInputState.Mode.New) {
                add(0, inputState.value)
            }
        }
    }

    // 비활성 루프 그룹의 펼침 상태가 회전 등 구성 변경에도 유지되도록 rememberSaveable로 복원한다.
    return rememberSaveable(saver = Section.All.Saver) {
        Section.All()
    }.apply {
        items.value = resultLoops
    }
}

@Composable
private fun rememberAdSection() = remember { Section.Ad() }

/**
 * 전체 탭 하단 기록 그리드 섹션. 전달받은 전체 루프 목록을 그대로 그리드 데이터로 쓴다.
 * (임시 mock 루프 필터링은 그리드 컴포저블 내부에서 처리한다.)
 */
@Composable
private fun rememberAllHistoryGridSection(
    loops: List<LoopBase>,
): Section = remember {
    Section.AllHistoryGrid()
}.apply {
    items.value = loops
}

@Composable
private fun rememberDoneSection(loops: List<LoopBase>): Section {
    val resultLoops = remember(loops) {
        loops.filter { it.isActiveDay() && it.isRespond }
    }
    return remember {
        Section.DoneSkip()
    }.apply {
        items.value = resultLoops
    }
}


private suspend fun ensureLoop(
    context: Context,
    loopViewModel: LoopViewModel,
    loop: LoopBase,
    hostState: SnackbarHostState
): Boolean {

    // Empty title check
    if (loop.title.trim().isEmpty()) {
        hostState.showSnackbar(
            message = context.getString(R.string.warning_enter_characters_other_than_spaces)
        )
        return false
    }

    // The steppers let the user pass through a too-short span (so an overnight loop is still
    // reachable); the span is only rejected here, at submit time, where it can't get stuck.
    if (!loop.isAnyTime && isLoopDurationTooShort(loop.startInDay, loop.endInDay)) {
        hostState.showSnackbar(
            message = context.getString(
                R.string.warning_end_time_should_be_after_start_time,
                MIN_DIFF_MINUTES
            )
        )
        return false
    }

    if (loopViewModel.numberOfLoopsAtTheSameTime(loop) > MAX_LOOPS_TOGETHER) {
        hostState.showSnackbar(
            message = context.getString(
                R.string.warning_up_to_max_loops,
                MAX_LOOPS_TOGETHER
            )
        )
        return false
    }

    return true
}
