package com.pnd.android.loop.ui.detail

import android.net.Uri
import android.os.SystemClock
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.exclude
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScaffoldDefaults
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.common.AppPageHeader
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.BackdropState
import com.pnd.android.loop.ui.common.NavigationBarFadingEdge
import com.pnd.android.loop.ui.common.StatusBarFadingEdge
import com.pnd.android.loop.ui.common.backdropSource
import com.pnd.android.loop.ui.common.rememberBackdropState
import com.pnd.android.loop.ui.common.rememberScrollCollapseProgress
import com.pnd.android.loop.ui.common.supportsBackdropBlur
import com.pnd.android.loop.ui.common.SimpleAd
import com.pnd.android.loop.ui.home.DeleteLoopDialog
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.RoundShapes
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surfaceElevated
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val adId = if (BuildConfig.DEBUG) {
    "ca-app-pub-3940256099942544/6300978111"
} else {
    "ca-app-pub-2341430172816266/5981213088"
}

/** 삭제한 뒤 실행 취소를 기다리는 시간. 지나면 화면을 닫는다. */
private const val UNDO_WINDOW_MS = 6_000L

/** 상세 상단 바가 펼쳐진 상태에서 차지하는 액션 행 높이. */
private val DetailAppBarHeight = Dimens.appBarHeight

/**
 * 루프 상세 화면. 두 모습만 갖는다 — 읽는 화면([DetailScreen])과 고치는 화면([LoopEditor]).
 *
 * 스낵바 창구는 두 모습보다 위에 둔다. 편집기에서 저장하고 상세 화면으로 돌아온 직후에
 * "저장했어요"가 보여야 하는데, 그 확인 메시지의 주인은 어느 한 쪽 화면이 아니다.
 */
@Composable
fun DetailPage(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel = hiltViewModel(),
    onNavigateUp: () -> Unit,
) {
    val loop by detailViewModel.loop.collectAsState(LoopVo.create())
    val snackBarHostState = remember { SnackbarHostState() }
    val feedback = rememberDetailFeedback(hostState = snackBarHostState)

    // 본문의 스크롤 위치도 두 모습보다 위에 둔다. 아래쪽 섹션을 보다가 수정하고 돌아왔을 때
    // 화면이 맨 위로 튀어 오르면, 방금 보던 자리를 다시 찾아 내려가야 한다.
    val scrollState = rememberScrollState()

    var isEditing by rememberSaveable { mutableStateOf(false) }

    if (isEditing) {
        LoopEditor(
            modifier = modifier,
            loop = loop,
            onCountLoopsAtSameTime = { draft -> detailViewModel.numberOfLoopsAtTheSameTime(draft) },
            onSave = { edited ->
                detailViewModel.updateLoop(edited)
                isEditing = false
                feedback.show(R.string.detail_saved_loop)
            },
            onClose = { isEditing = false },
        )
    } else {
        DetailScreen(
            modifier = modifier,
            detailViewModel = detailViewModel,
            loop = loop,
            feedback = feedback,
            snackBarHostState = snackBarHostState,
            scrollState = scrollState,
            onEdit = { isEditing = true },
            onNavigateUp = onNavigateUp,
        )
    }
}

/**
 * 읽는 화면. 이 루프가 무엇인지(색 · 이름)와 무엇을 할 수 있는지(수정 · 삭제)는 액션 바가 맡고,
 * 본문은 오로지 "얼마나 잘 지키고 있는가"만 말한다.
 */
@Composable
private fun DetailScreen(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    feedback: DetailFeedback,
    snackBarHostState: SnackbarHostState,
    scrollState: ScrollState,
    onEdit: () -> Unit,
    onNavigateUp: () -> Unit,
) {
    val scope = rememberCoroutineScope()

    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }
    var showingLoopInfo by rememberSaveable(loop.loopId) { mutableStateOf(false) }
    val pendingDeletion by detailViewModel.pendingDeletion.collectAsState()

    if (isConfirmingDelete) {
        // 삭제는 기록과 메모까지 함께 지운다. 홈과 같은 확인 다이얼로그를 한 번 거친다.
        DeleteLoopDialog(
            loopTitle = loop.title,
            loopColor = loop.color,
            onDismiss = { isConfirmingDelete = false },
            onDelete = {
                isConfirmingDelete = false
                scope.launch {
                    detailViewModel.deleteLoop(loop)
                }
            },
        )
    }

    val pending = pendingDeletion

    if (showingLoopInfo && pending == null) {
        val today by detailViewModel.today.collectAsState()
        LoopInformationDialog(
            loop = loop,
            today = today,
            onLoadOverlapCount = { detailViewModel.overlappingLoopCount(it) },
            onDismiss = { showingLoopInfo = false },
        )
    }

    val backdrop = rememberBackdropState()
    val headerProgress by rememberScrollCollapseProgress(
        scrollState = scrollState,
        collapseDistance = DetailAppBarHeight,
    )
    val topInset = WindowInsets.statusBars.asPaddingValues().calculateTopPadding()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val contentPadding = PaddingValues(
        top = topInset + DetailAppBarHeight + DetailSpacing.screenTop,
        bottom = bottomInset + DetailSpacing.sectionBottom,
    )

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(color = AppColor.background),
        snackbarHost = {
            SnackbarHost(
                modifier = Modifier.navigationBarsPadding(),
                hostState = snackBarHostState,
            )
        },
        containerColor = Color.Transparent,
        contentColor = AppColor.onSurface,
        // 본문을 상태·내비게이션 바까지 확장하고, 각 시스템 바는 fading edge가 마감한다.
        contentWindowInsets = ScaffoldDefaults.contentWindowInsets
            .exclude(WindowInsets.statusBars)
            .exclude(WindowInsets.navigationBars),
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding),
        ) {
            if (pending != null) {
                DeletedNotice(
                    loopTitle = pending.deleted.loop.title,
                    undoDeadlineElapsedMs = pending.undoDeadlineElapsedMs,
                    onUndo = { scope.launch { detailViewModel.restorePendingDeletion() } },
                    onExpire = {
                        detailViewModel.clearPendingDeletion()
                        onNavigateUp()
                    },
                )
            } else {
                DetailPageContent(
                    modifier = Modifier
                        .fillMaxSize()
                        .backdropSource(backdrop),
                    detailViewModel = detailViewModel,
                    loop = loop,
                    feedback = feedback,
                    scrollState = scrollState,
                    contentPadding = contentPadding,
                )
            }

            // 시스템 바 위로도 본문이 이어지되, 경계에서는 자연스럽게 사라진다.
            StatusBarFadingEdge(modifier = Modifier.align(Alignment.TopCenter))
            NavigationBarFadingEdge(modifier = Modifier.align(Alignment.BottomCenter))

            DetailAppBar(
                modifier = Modifier.align(Alignment.TopCenter),
                title = loop.title,
                color = loop.color,
                progress = headerProgress,
                backdrop = if (supportsBackdropBlur) backdrop else null,
                showActions = pending == null,
                enabled = loop.enabled,
                onNavigateUp = onNavigateUp,
                onEdit = onEdit,
                onEnabledChange = { detailViewModel.enableLoop(loop, it) },
                onShowInfo = { showingLoopInfo = true },
                onDelete = { isConfirmingDelete = true },
            )
        }
    }
}

/** 이름·색·수정은 항상 보이고, 스크롤 시에는 플로팅 표면 위에 남는다. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailAppBar(
    modifier: Modifier = Modifier,
    title: String,
    color: Int,
    progress: Float,
    backdrop: BackdropState?,
    showActions: Boolean,
    onNavigateUp: () -> Unit,
    onEdit: () -> Unit,
    enabled: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onShowInfo: () -> Unit,
    onDelete: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    AppPageHeader(
        modifier = modifier,
        title = title,
        onNavigateUp = onNavigateUp,
        progress = progress,
        backdrop = backdrop,
        titleLeading = {
            Box(
                modifier = Modifier
                    .padding(end = 10.dp)
                    .size(LoopColorDotSize)
                    .clip(CircleShape)
                    .background(color.compositeOverOnSurface()),
            )
        },
        // 삭제 안내 중에는 액션이 하나도 없다. 빈 람다를 넘기면 내용 없는 알약만 떠 보이므로
        // 이때는 액션 슬롯 자체를 비운다(null).
        actions = if (!showActions) null else {
            {
                AppBarIcon(
                    imageVector = Icons.Outlined.ModeEdit,
                    color = AppColor.onSurface,
                    descriptionResId = R.string.detail_edit_loop,
                    onClick = onEdit,
                )
                Box {
                    AppBarIcon(
                        imageVector = Icons.Outlined.MoreVert,
                        color = AppColor.onSurface,
                        descriptionResId = R.string.detail_more_actions,
                        onClick = { menuExpanded = true },
                    )
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false },
                        shape = RoundShapes.large,
                        containerColor = AppColor.surfaceElevated,
                        tonalElevation = 0.dp,
                    ) {
                        DropdownMenuItem(
                            modifier = Modifier.semantics {
                                role = Role.Switch
                                toggleableState = ToggleableState(enabled)
                            },
                            text = {
                                Text(
                                    stringResource(R.string.detail_schedule_enabled),
                                    color = AppColor.onSurface,
                                )
                            },
                            trailingIcon = {
                                Switch(
                                    checked = enabled,
                                    // The menu row handles taps and exposes one accessible switch.
                                    onCheckedChange = null,
                                )
                            },
                            onClick = { onEnabledChange(!enabled) },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.detail_loop_info), color = AppColor.onSurface) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Info, contentDescription = null, tint = AppColor.onSurface)
                            },
                            onClick = {
                                menuExpanded = false
                                onShowInfo()
                            },
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.delete_loop_title), color = AppColor.error) },
                            leadingIcon = {
                                Icon(Icons.Outlined.Delete, contentDescription = null, tint = AppColor.error)
                            },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            },
                        )
                    }
                }
            }
        },
    )
}

/** 활성 상태와 주간 목표 아래에 기록·통계·수정 이력을 배치한다. */
@Composable
private fun DetailPageContent(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    feedback: DetailFeedback,
    scrollState: ScrollState,
    contentPadding: PaddingValues,
) {
    val stats by detailViewModel.stats.collectAsState()
    val memos by detailViewModel.memos.collectAsState()

    Column(
        modifier = modifier
            .padding(horizontal = Dimens.screenHorizontalPadding)
            .fillMaxWidth()
            .verticalScroll(state = scrollState)
            .padding(contentPadding),
        verticalArrangement = Arrangement.spacedBy(DetailSpacing.group),
    ) {
        SummaryHeader(
            loop = loop,
            stats = stats,
        )

        SectionList(
            detailViewModel = detailViewModel,
            loop = loop,
            stats = stats,
            memos = memos,
            feedback = feedback,
        )

        SimpleAd(adId = adId)
    }

}

/** 기록과 메모 → 자세한 통계 → 수정 이력. 각 섹션은 접힌 상태로 시작한다. */
@Composable
private fun SectionList(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    stats: DetailStats,
    memos: List<LoopRetrospectVo>,
    feedback: DetailFeedback,
) {
    val accent = Color(loop.color).compositeOverOnSurface()

    var statsExpanded by rememberSaveable(loop.loopId) { mutableStateOf(false) }
    var journalExpanded by rememberSaveable(loop.loopId) { mutableStateOf(false) }
    var historyExpanded by rememberSaveable(loop.loopId) { mutableStateOf(false) }
    val revisionHistory by detailViewModel.revisionHistory.collectAsState()

    Column(modifier = modifier.fillMaxWidth()) {
        HairlineDivider()
        JournalSection(
            stats = stats,
            memos = memos,
            accent = accent,
            feedback = feedback,
            expanded = journalExpanded,
            onExpandedChange = { journalExpanded = it },
            onLoadMemo = { date -> detailViewModel.retrospectOf(date) },
            onSaveMemo = { date, text -> detailViewModel.saveRetrospect(date, text) },
            onSaveMemoInBackground = { date, text ->
                // 화면이 사라지는 중일 수 있으므로, 컴포지션과 함께 취소되지 않는 곳에서 저장한다.
                detailViewModel.saveRetrospectInBackground(date, text)
            },
            onSetDoneState = { date, state ->
                detailViewModel.setDoneState(localDate = date, doneState = state)
            },
        )

        HairlineDivider()
        StatsSection(
            stats = stats,
            accent = accent,
            expanded = statsExpanded,
            onExpandedChange = { statsExpanded = it },
        )

        HairlineDivider()
        RevisionHistorySection(
            loopId = loop.loopId,
            entries = revisionHistory,
            expanded = historyExpanded,
            onExpandedChange = { historyExpanded = it },
        )

    }
}

/**
 * 기록 전체를 CSV 한 장으로 꺼내는 행.
 *
 * 저장 위치는 시스템 파일 선택기에 맡긴다. 앱이 스스로 파일을 쓰려면 공유용 provider 와 저장소
 * 권한이 따라붙는데, 사용자가 위치를 직접 고르는 편이 권한도 필요 없고 어디에 저장됐는지도 분명하다.
 */
@Composable
private fun ExportRow(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loopTitle: String,
    recordCount: Int,
    feedback: DetailFeedback,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv"),
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = runCatching {
                val csv = detailViewModel.buildCsv(loopTitle)
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri)?.use { out ->
                        out.write(csv.toByteArray(Charsets.UTF_8))
                    } ?: error("cannot open $uri")
                }
            }.isSuccess
            feedback.show(
                if (ok) R.string.detail_export_done else R.string.detail_export_failed
            )
        }
    }

    SectionActionRow(
        modifier = modifier,
        icon = Icons.Outlined.FileDownload,
        title = stringResource(id = R.string.detail_export),
        summary = stringResource(id = R.string.detail_export_count, recordCount),
        onClick = { launcher.launch(csvFileName(loopTitle)) },
    )
}

/** 파일 이름에 쓸 수 없는 글자를 걸러 낸 CSV 파일 이름. */
private fun csvFileName(loopTitle: String): String {
    val safe = loopTitle.replace(Regex("[\\\\/:*?\"<>|]"), "_").trim().ifBlank { "loop" }
    return "$safe.csv"
}

/**
 * 삭제 직후의 되살리기 창.
 *
 * 확인 다이얼로그를 거쳐도 삭제는 기록과 메모까지 함께 지운다. 지운 직후 잠깐은 되돌릴 수 있게
 * 두고, 그 사이 아무 것도 하지 않으면 화면을 닫는다. 목록으로 곧장 돌아가 버리면 실행 취소를
 * 띄울 자리가 없기 때문에, 이 안내를 이 화면 안에 둔다.
 */
@Composable
private fun DeletedNotice(
    modifier: Modifier = Modifier,
    loopTitle: String,
    undoDeadlineElapsedMs: Long,
    onUndo: () -> Unit,
    onExpire: () -> Unit,
) {
    var remainingSeconds by remember(loopTitle, undoDeadlineElapsedMs) {
        mutableIntStateOf(remainingSecondsUntil(undoDeadlineElapsedMs))
    }

    LaunchedEffect(undoDeadlineElapsedMs) {
        while (true) {
            remainingSeconds = remainingSecondsUntil(undoDeadlineElapsedMs)
            if (remainingSeconds <= 0) {
                onExpire()
                break
            }
            delay(250)
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(horizontal = Dimens.screenHorizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = stringResource(id = R.string.detail_deleted_title, loopTitle),
            textAlign = TextAlign.Center,
            style = AppTypography.titleMedium.copy(color = AppColor.onSurface),
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = stringResource(id = R.string.detail_deleted_message),
            textAlign = TextAlign.Center,
            style = AppTypography.bodySmall.copy(
                color = AppColor.onSurface.copy(alpha = 0.5f),
            ),
        )
        Text(
            modifier = Modifier.padding(top = 8.dp),
            text = "$remainingSeconds",
            textAlign = TextAlign.Center,
            style = AppTypography.bodySmall.copy(color = AppColor.onSurface.copy(alpha = 0.5f)),
        )
        PrimaryPillButton(
            modifier = Modifier.padding(top = 20.dp),
            text = stringResource(id = R.string.detail_undo),
            onClick = onUndo,
        )
        TextActionButton(
            modifier = Modifier.padding(top = 4.dp),
            text = stringResource(id = R.string.detail_deleted_close),
            onClick = onExpire,
        )
    }
}

private fun remainingSecondsUntil(deadlineElapsedMs: Long): Int =
    ((deadlineElapsedMs - SystemClock.elapsedRealtime() + 999L) / 1_000L)
        .coerceAtLeast(0L)
        .toInt()
