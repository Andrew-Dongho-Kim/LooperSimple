package com.pnd.android.loop.ui.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Autorenew
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.common.MAX_LOOPS_TOGETHER
import com.pnd.android.loop.data.isDisabled
import com.pnd.android.loop.data.isInProgressState
import com.pnd.android.loop.data.isNotRespond
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.ui.home.input.UserInput
import com.pnd.android.loop.ui.home.input.UserInputState
import com.pnd.android.loop.ui.home.input.loopInputPanelBackgroundColor
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.common.NavigationBarFadingEdge
import com.pnd.android.loop.ui.common.StatusBarFadingEdge
import com.pnd.android.loop.ui.common.backdropSource
import com.pnd.android.loop.ui.common.rememberBackdropState
import com.pnd.android.loop.ui.common.supportsBackdropBlur
import com.pnd.android.loop.ui.home.input.rememberUserInputState
import com.pnd.android.loop.ui.home.viewmodel.LoopViewModel
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.primary
import com.pnd.android.loop.ui.theme.surface
import com.pnd.android.loop.ui.theme.surfaceElevated
import com.pnd.android.loop.util.isActiveDay
import com.pnd.android.loop.util.toMs
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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .blur(radius = blurState.radius),
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier
                    .navigationBarsPadding()
                    .imePadding()
                    .padding(bottom = 48.dp + 56.dp)
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
        contentWindowInsets = ScaffoldDefaults
            .contentWindowInsets
            .exclude(WindowInsets.navigationBars)
            .exclude(WindowInsets.statusBars)
            .exclude(WindowInsets.ime),
    )
    { contentPadding ->
        HomeContent(
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
private fun HomeContent(
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
    Box(modifier = modifier.background(color = AppColor.background)) {
        val lazyListState = rememberLazyListState()
        // A real backdrop blur is only available on API 31+; elsewhere the floating surfaces fall
        // back to translucent white, so we only bother capturing the content where it's used.
        val backdrop = rememberBackdropState()
        val headerBackdrop = if (supportsBackdropBlur) backdrop else null

        HomeContent(
            // 내비게이션 바 영역까지 콘텐츠가 그려지도록 navigationBarsPadding을 두지 않는다.
            modifier = Modifier
                .fillMaxHeight()
                .imePadding(),
            blurState = blurState,
            inputState = inputState,
            lazyListState = lazyListState,
            backdrop = headerBackdrop,
            loopViewModel = loopViewModel,
            selectedTab = selectedTab,
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

        // The collapsing action bar: a plain app bar at rest that, as the list scrolls, fades out
        // the title and floats the icons (in place) and the 오늘/전체 tabs (slid up to the left).
        val collapseProgress by rememberHomeHeaderCollapseProgress(lazyListState)
        CollapsingHomeHeader(
            modifier = Modifier.align(Alignment.TopCenter),
            progress = collapseProgress,
            loopViewModel = loopViewModel,
            selectedTab = selectedTab,
            onTabSelected = onTabSelected,
            onNavigateToGroupPage = onNavigateToGroupPage,
            onNavigateToStatisticsPage = onNavigateToStatisticsPage,
            onNavigateToHistoryPage = onNavigateToHistoryPage,
            backdrop = headerBackdrop,
        )

        val context = LocalContext.current
        UserInput(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .navigationBarsPadding()
                .imePadding(),
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
                loopViewModel.addOrUpdateLoop(
                    newLoop.asLoopVo(
                        created = if (newLoop.created == 0L) {
                            LocalDateTime.now().toMs()
                        } else {
                            newLoop.created
                        }
                    )
                )
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
private fun HomeContent(
    modifier: Modifier = Modifier,
    blurState: BlurState,
    inputState: UserInputState,
    lazyListState: LazyListState,
    backdrop: BackdropState?,
    loopViewModel: LoopViewModel,
    @HomeTab.Type selectedTab: Int,
    onNavigateToGroupPicker: (LoopBase) -> Unit,
    onNavigateToDetailPage: (LoopBase) -> Unit,
    onNavigateToHistoryPage: () -> Unit,
) {
    val sections by loopViewModel.observeSectionsAsState(
        inputState = inputState,
        selectedTab = selectedTab,
    )
    val onEdit = remember { { loop: LoopBase -> inputState.edit(loop) } }

    // The collapsing header (title + icons + tabs) is drawn as an overlay, so the list simply
    // starts below its expanded height and then scrolls up underneath it. This content is also
    // the source the floating surfaces sample for their backdrop blur (API 31+ only).
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val headerHeight = homeHeaderExpandedHeight(topInset)
    val backdropModifier = backdrop?.let { Modifier.backdropSource(it) } ?: Modifier

    // backdropSource wraps the background so the captured layer (sampled by the blur) includes it.
    Box(modifier = modifier.then(backdropModifier).background(AppColor.background)) {
        if (sections.isEmpty()) {
            EmptyLoops(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeight)
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                state = lazyListState,
                contentPadding = PaddingValues(top = headerHeight),
            ) {
                sections.forEach { section ->
                    section(
                        section = section,
                        blurState = blurState,
                        loopViewModel = loopViewModel,
                        selectedTab = selectedTab,
                        onEdit = onEdit,
                        onNavigateToGroupPicker = onNavigateToGroupPicker,
                        onNavigateToDetailPage = onNavigateToDetailPage,
                        onNavigateToHistoryPage = onNavigateToHistoryPage,
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(150.dp))
                }
            }
        }
    }
}

@Composable
fun EmptyLoops(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier.padding(horizontal = 48.dp),
        contentAlignment = Alignment.Center,
    ) {
        HomeEmptyState(
            icon = Icons.Outlined.Autorenew,
            title = stringResource(R.string.desc_no_loops),
            hint = stringResource(R.string.desc_no_loops_hint),
        )
    }
}

/**
 * 홈에서 쓰는 공용 빈 상태 — 틴트 원 안의 아이콘 + 제목 + 힌트. 루프가 하나도 없을 때와
 * 오늘 할 일을 모두 끝냈을 때가 같은 문법으로 읽히도록 한 곳에서 스타일을 관리한다.
 * 색은 모두 테마에서 가져와 라이트/다크 모드에 함께 대응한다.
 */
@Composable
fun HomeEmptyState(
    modifier: Modifier = Modifier,
    icon: ImageVector,
    title: String,
    hint: String,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(color = AppColor.primary.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                modifier = Modifier.size(36.dp),
                imageVector = icon,
                tint = AppColor.primary,
                contentDescription = null,
            )
        }

        Text(
            modifier = Modifier.padding(top = 20.dp),
            text = title,
            textAlign = TextAlign.Center,
            style = AppTypography.titleMedium.copy(
                color = AppColor.onSurface,
                fontWeight = FontWeight.Bold,
            ),
        )

        Text(
            modifier = Modifier.padding(top = 6.dp),
            text = hint,
            textAlign = TextAlign.Center,
            style = AppTypography.bodyMedium.copy(
                color = AppColor.onSurface.copy(alpha = 0.55f),
            ),
        )
    }
}


@Composable
private fun LoopViewModel.observeSectionsAsState(
    inputState: UserInputState,
    @HomeTab.Type selectedTab: Int,
): State<List<Section>> {
    val loops by allLoopsWithDoneStates.collectAsState(emptyList())
    val yesterdayLoops by loopsNoResponseYesterday.collectAsState(initial = emptyList())

    return if (loops.isEmpty() && inputState.mode == UserInputState.Mode.None) {
        remember { mutableStateOf(emptyList()) }
    } else {
        val resultLoops = ArrayList<LoopBase>(loops)
        if (inputState.mode == UserInputState.Mode.Edit) {
            val edited = inputState.value
            val index = resultLoops.indexOfFirst { it.loopId == edited.loopId }
            // The edited loop may have disappeared from the latest DB emission
            // (deleted or filtered out) — only replace it when it still exists.
            if (index != -1) resultLoops[index] = edited
        }

        // The 오늘 / 전체 tab is pinned in the app bar; here we only pick which loop list
        // its selection maps to.
        val doneSection = rememberDoneSection(resultLoops)
        val sections = mutableListOf(
            rememberHeaderSection(resultLoops),
            if (selectedTab == HomeTab.ALL) {
                rememberAllSection(resultLoops, inputState)
            } else {
                rememberTodaySection(resultLoops)
            },
            rememberYesterdaySection(yesterdayLoops),
            rememberAdSection(),
            // 전체 탭에서는 오늘 Done/Skip한 루프 정보를 노출하지 않는다.
            if (selectedTab == HomeTab.ALL) null else doneSection,
        ).filterNotNull().filter { it.size > 0 }

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
    val resultLoops = remember(loops) {
        loops.filter {
            (it.isActiveDay() && (it.isNotRespond || it.isDisabled || it.isInProgressState))
        }
    }

    val context = LocalContext.current
    return rememberSaveable(saver = Section.Today.Saver) {
        Section.Today().apply { load(context) }
    }.apply {
        items.value = resultLoops
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

    return remember {
        Section.All()
    }.apply {
        items.value = resultLoops
    }
}

@Composable
private fun rememberAdSection() = remember { Section.Ad() }

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
