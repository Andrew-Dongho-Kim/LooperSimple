package com.pnd.android.loop.ui.detail

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.ModeEdit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.pnd.android.loop.BuildConfig
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.ui.common.AppBarIcon
import com.pnd.android.loop.ui.common.SimpleAd
import com.pnd.android.loop.ui.home.DeleteLoopDialog
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.AppTypography
import com.pnd.android.loop.ui.theme.Dimens
import com.pnd.android.loop.ui.theme.background
import com.pnd.android.loop.ui.theme.compositeOverOnSurface
import com.pnd.android.loop.ui.theme.error
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface
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

    // 삭제한 직후의 되살리기 창. 값이 있는 동안은 본문 대신 실행 취소 안내가 보인다.
    var deleted by remember { mutableStateOf<LoopDetailViewModel.DeletedLoop?>(null) }
    var isConfirmingDelete by rememberSaveable { mutableStateOf(false) }

    if (isConfirmingDelete) {
        // 삭제는 기록과 메모까지 함께 지운다. 홈과 같은 확인 다이얼로그를 한 번 거친다.
        DeleteLoopDialog(
            loopTitle = loop.title,
            loopColor = loop.color,
            onDismiss = { isConfirmingDelete = false },
            onDelete = {
                isConfirmingDelete = false
                scope.launch { deleted = detailViewModel.deleteLoop(loop) }
            },
        )
    }

    val snapshot = deleted

    Scaffold(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .background(color = AppColor.background),
        topBar = {
            // 활성화 토글만은 앱바에 두지 않는다. 예전에 앱바에서 가장 누르기 쉬운 자리에 있던
            // 탓에, 기록을 멈추는 되돌리기 어려운 동작이 실수로 눌리기 쉬웠다. 지금은 본문 최상단
            // 요약 헤더에서 "지금 어느 쪽인지"를 글자로 보여주며 고른다.
            DetailAppBar(
                modifier = Modifier.statusBarsPadding(),
                title = loop.title,
                color = loop.color,
                // 지운 뒤 되살리기를 기다리는 동안은 이미 없는 루프다. 고칠 수도 없고 다시 지울
                // 수도 없으므로 두 버튼을 거둔다. 남은 선택지는 실행 취소와 닫기뿐이다.
                showActions = snapshot == null,
                onNavigateUp = onNavigateUp,
                onEdit = onEdit,
                onDelete = { isConfirmingDelete = true },
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
    ) { contentPadding ->
        Box(modifier = Modifier.padding(contentPadding)) {
            if (snapshot != null) {
                DeletedNotice(
                    loopTitle = snapshot.loop.title,
                    onUndo = {
                        detailViewModel.restoreLoop(snapshot)
                        deleted = null
                    },
                    onExpire = onNavigateUp,
                )
            } else {
                DetailPageContent(
                    detailViewModel = detailViewModel,
                    loop = loop,
                    feedback = feedback,
                    scrollState = scrollState,
                )
            }
        }
    }
}

/**
 * 상세 화면의 액션 바. 이 루프가 무엇인지(색 · 이름)와 이 루프에 할 수 있는 두 가지 일
 * (수정 · 삭제)이 한 줄에 모인다.
 *
 * 예전에는 이름과 색이 본문 맨 위 요약 헤더에 있었고, 고치는 길은 섹션마다 흩어져 있었다
 * (이름 옆 연필 · 색 점 · 스케줄 섹션의 '시간 수정'). 지금은 스크롤 위치와 무관하게 늘 같은
 * 자리에서 같은 두 버튼을 누른다. 이름과 색도 함께 앱바로 올라와, 본문을 어디까지 내려도
 * "무슨 루프를 보고 있는지"가 화면에서 사라지지 않는다.
 *
 * 삭제만 error 색을 입혀 되돌릴 수 없는 동작임을 색으로도 구분한다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DetailAppBar(
    modifier: Modifier = Modifier,
    title: String,
    color: Int,
    showActions: Boolean,
    onNavigateUp: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    TopAppBar(
        modifier = modifier.background(color = AppColor.surface),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(LoopColorDotSize)
                        .clip(CircleShape)
                        .background(color.compositeOverOnSurface()),
                )
                Text(
                    modifier = Modifier.padding(start = 10.dp),
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = AppTypography.titleLarge.copy(
                        color = AppColor.onSurface,
                        fontWeight = FontWeight.Bold,
                    ),
                )
            }
        },
        navigationIcon = {
            AppBarIcon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                color = AppColor.onSurface,
                descriptionResId = R.string.navi_up,
                onClick = onNavigateUp,
            )
        },
        actions = {
            if (showActions) {
                AppBarIcon(
                    imageVector = Icons.Outlined.ModeEdit,
                    color = AppColor.onSurface,
                    descriptionResId = R.string.detail_edit_loop,
                    onClick = onEdit,
                )
                AppBarIcon(
                    imageVector = Icons.Outlined.Delete,
                    color = AppColor.error,
                    descriptionResId = R.string.delete_loop_title,
                    onClick = onDelete,
                )
            }
        },
    )
}

/**
 * 상세 화면 본문. 화면은 두 층으로만 나뉜다.
 *
 * 1. [SummaryHeader] — 카드 테두리 없이 배경 위에 바로 놓이는 요약. 활성화 상태와 핵심
 *    지표 둘(완료율·연속), 이번 주 목표 진행까지가 여기에 들어가 진입 즉시 스크롤 없이 읽힌다.
 * 2. [SectionList] — 스케줄 / 통계 / 기록 / 내보내기를 한 장의 카드 안에 담는다.
 *    접힌 행에도 오른쪽에 값이 남아 있어 펼치지 않아도 정보가 사라지지 않는다.
 *
 * 화면이 그리는 수치는 전부 [LoopDetailViewModel.stats] 하나에서 온다. 예전에는 요약 헤더와
 * 기록 섹션이 같은 응답 목록으로 같은 날짜 인덱스를 각자 만들고, 60일 롤링 완료율까지 컴포지션
 * 도중에 계산했다.
 */
@Composable
private fun DetailPageContent(
    modifier: Modifier = Modifier,
    detailViewModel: LoopDetailViewModel,
    loop: LoopBase,
    feedback: DetailFeedback,
    scrollState: ScrollState,
) {
    val stats by detailViewModel.stats.collectAsState()
    val memos by detailViewModel.memos.collectAsState()

    Column(
        modifier = modifier
            .padding(horizontal = Dimens.screenHorizontalPadding)
            .fillMaxWidth()
            .verticalScroll(state = scrollState),
        verticalArrangement = Arrangement.spacedBy(Dimens.contentPadding),
    ) {
        SummaryHeader(
            loop = loop,
            stats = stats,
            onEnabledChange = { enabled -> detailViewModel.enableLoop(loop, enabled) },
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

/**
 * 스케줄 / 통계 / 기록 / 내보내기를 담는 한 장의 카드.
 *
 * 순서는 "이 루프가 무엇인지 → 잘 지키고 있는지 → 되돌아보기 → 꺼내기" 다. 스케줄이 맨 위인
 * 이유는 루프의 정체성이기 때문이고, 내보내기가 맨 아래인 이유는 가장 드물게 쓰기 때문이다.
 *
 * 기본으로 펼쳐 두는 것은 통계 하나뿐이다. 나머지는 접힌 채로도 오른쪽 요약에 값이 남는다.
 */
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

    var scheduleExpanded by rememberSaveable(loop.loopId) { mutableStateOf(false) }
    var statsExpanded by rememberSaveable(loop.loopId) { mutableStateOf(true) }
    var journalExpanded by rememberSaveable(loop.loopId) { mutableStateOf(false) }

    // 같은 시간대에 몇 개가 몰려 있는지. 시간대가 바뀔 때만 다시 센다.
    var overlappingCount by remember(loop.loopId) { mutableStateOf(0) }
    LaunchedEffect(loop.loopId, loop.startInDay, loop.endInDay, loop.activeDays, loop.isAnyTime) {
        overlappingCount = detailViewModel.overlappingLoopCount(loop)
    }

    DetailCard(
        modifier = modifier,
        contentPadding = PaddingValues(vertical = 4.dp),
    ) {
        ScheduleSection(
            loop = loop,
            today = stats.today,
            createdDate = stats.createdDate,
            overlappingCount = overlappingCount,
            expanded = scheduleExpanded,
            onExpandedChange = { scheduleExpanded = it },
        )

        SectionSeparator()

        StatsSection(
            stats = stats,
            accent = accent,
            expanded = statsExpanded,
            onExpandedChange = { statsExpanded = it },
        )

        SectionSeparator()

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
                detailViewModel.setDoneState(loop = loop, localDate = date, doneState = state)
            },
        )

        SectionSeparator()

        ExportRow(
            detailViewModel = detailViewModel,
            loopTitle = loop.title,
            recordCount = stats.totalCount,
            feedback = feedback,
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
    onUndo: () -> Unit,
    onExpire: () -> Unit,
) {
    LaunchedEffect(loopTitle) {
        delay(UNDO_WINDOW_MS)
        onExpire()
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
