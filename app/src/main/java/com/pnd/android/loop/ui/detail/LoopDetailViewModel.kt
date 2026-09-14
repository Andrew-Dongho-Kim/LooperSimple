package com.pnd.android.loop.ui.detail

import android.app.Application
import android.os.SystemClock
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.data.history.LoopHistory
import com.pnd.android.loop.data.history.localDate
import com.pnd.android.loop.ui.home.viewmodel.LoopRepository
import com.pnd.android.loop.util.todayFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DELETE_UNDO_WINDOW_MS = 6_000L

@HiltViewModel
class LoopDetailViewModel @Inject constructor(
    private val app: Application,
    appDb: AppDatabase,
    savedStateHandle: SavedStateHandle,
    private val loopRepository: LoopRepository,
) : ViewModel() {

    // 쓰기 작업 전용 스코프. 화면을 벗어난 뒤에도 저장이 끝까지 가도록 ViewModel 수명과 분리한다.
    private val coroutineScope = CoroutineScope(SupervisorJob())

    private val loopId: Int = savedStateHandle[NavigatePage.ARGS_ID] ?: -1

    private val loopDao = appDb.loopDao()
    private val loopRetrospectDao = appDb.loopRetrospectDao()

    /**
     * 삭제 직후에만 유지하는 복구 정보. 화면의 remember 에 두면 회전 등 구성 변경 때
     * 실행 취소 기회가 사라지므로 ViewModel 수명에 둔다.
     */
    data class PendingDeletion(
        val deleted: LoopHistory,
        val undoDeadlineElapsedMs: Long,
    )

    private val _pendingDeletion = MutableStateFlow<PendingDeletion?>(null)
    val pendingDeletion = _pendingDeletion
    private val restoreMutex = Mutex()

    // 루프를 삭제하면 Room 이 이 자리에 null 을 흘려보낸다. 화면이 닫히는 몇 프레임 동안
    // 마지막으로 유효했던 값을 그대로 쓰도록 걸러 내, 삭제 직후 NPE 가 나지 않게 한다.
    val loop = loopDao.getLoopFlow(loopId).mapNotNull { it }

    /**
     * "오늘". 자정에 한 번 갱신된다. 화면에서 `LocalDate.now()` 를 직접 부르면 그 값이
     * 컴포지션 시점에 박혀, 앱을 켜 둔 채 자정을 넘겼을 때 달력의 오늘 표시와 주간 스트립이
     * 하루 밀린 채 남는다.
     */
    val today = todayFlow().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = LocalDate.now(),
    )

    // 이 루프에 남긴 회고 메모 전체(본문이 있는 것만, 최신 날짜 순).
    val memos = loopRetrospectDao.getRetrospectsFlow(loopId)
        .map { retrospects ->
            retrospects
                .filter { !it.text.isNullOrBlank() }
                .sortedByDescending { it.localDate() }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )


    /**
     * 상단 요약과 최근 활동 통계를 같은 기록으로 계산한다.
     * 집계는 UI 스레드 밖에서 한 번 수행하고, 화면은 계산 결과만 표시한다.
     */
    internal val stats = combine(loopRepository.historyRepository.snapshots, today) { snapshot, date ->
        val timeline = snapshot.byId[loopId]
        if (timeline == null) DetailStats.empty(date) else computeDetailStats(timeline, date)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DetailStats.empty())

    /** Null means loading; an empty list means no stored history. */
    internal val revisionHistory = loopRepository.historyRepository.snapshots
        .map { snapshot -> buildLoopRevisionEntries(snapshot.byId[loopId]?.history?.revisions.orEmpty()) }
        .distinctUntilChanged()
        .flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** 선택한 날짜에 남긴 회고 메모 본문. 없으면 null. */
    suspend fun retrospectOf(date: LocalDate): String? =
        loopRepository.getMemo(loopId, date)?.text

    /**
     * 선택한 날짜의 회고 메모를 저장한다. 내용이 비어 있으면 null 로 지워, 달력의 메모 마커도
     * 함께 사라지게 한다. 저장이 실제로 끝난 뒤에 돌아오므로, 화면은 그때 확인 메시지를 띄운다.
     */
    suspend fun saveRetrospect(date: LocalDate, text: String) {
        loopRepository.saveMemo(loopId, date, text)
    }

    /**
     * [saveRetrospect] 와 같은 저장이되, 결과를 기다리지 않는다.
     *
     * 다른 날짜로 옮기거나 화면을 벗어나면서 초안을 지킬 때 쓴다. 그 시점에는 화면의 코루틴이
     * 곧 취소되므로, ViewModel 수명과 분리된 스코프에서 끝까지 저장한다.
     */
    fun saveRetrospectInBackground(date: LocalDate, text: String) {
        coroutineScope.launch { saveRetrospect(date, text) }
    }

    /**
     * 특정 날짜의 완료/건너뜀 상태를 고친다. 달력에서 지난 기록을 바로잡는 데 쓴다.
     *
     * '언제든지' 루프는 그날 실제로 시작·정지한 시각이 응답 행에만 남는다(루프 자체의 시각은
     * ANY_TIME 이다). 지난 기록의 상태만 바꾸려다 그 값을 잃지 않도록, 이미 행이 있으면 그
     * 시각을 그대로 실어 보낸다. '기록 없음'으로 되돌릴 때는 원래 규칙대로 시각도 함께 비운다.
     *
     * 저장은 저장소를 거친다 — 오늘 몫을 고치면 상시 알림과 대기 중인 알림도 함께 정리돼야 한다.
     */
    suspend fun setDoneState(
        localDate: LocalDate,
        @LoopDoneVo.DoneState doneState: Int,
    ) {
        loopRepository.setRecordedState(loopId, localDate, doneState)
        AppWidgetUpdateWorker.updateWidget(app)
    }

    suspend fun setLoopEnabled(
        loop: LoopBase,
        enabled: Boolean
    ) {
        loopRepository.setEnabled(loop.loopId, enabled)
        AppWidgetUpdateWorker.updateWidget(app)
    }

    fun enableLoop(loop: LoopBase, enabled: Boolean) {
        coroutineScope.launch { setLoopEnabled(loop, enabled) }
    }

    /** 상세 화면에서 인라인으로 고친 이름·시간·색·반복·목표를 저장한다. */
    suspend fun updateLoop(loop: LoopBase) {
        loopRepository.addOrUpdateLoop(loop.asLoopVo())
        AppWidgetUpdateWorker.updateWidget(app)
    }

    /** 시간대를 바꿔 저장하기 전, 같은 시간에 겹치는 루프가 몇 개인지 확인한다. */
    suspend fun numberOfLoopsAtTheSameTime(loop: LoopBase) =
        loopRepository.numberOfLoopsAtTheSameTime(loop = loop)

    /**
     * 이 루프와 시간대가 겹치는 **다른** 루프의 수. 루프 정보에서 "이 시간대에 N개 더"로 알려
     * 시간을 옮길지 판단하게 돕는다. [numberOfLoopsAtTheSameTime] 은 자기 자신을 포함한다.
     */
    suspend fun overlappingLoopCount(loop: LoopBase) =
        (numberOfLoopsAtTheSameTime(loop) - 1).coerceAtLeast(0)

    /**
     * 삭제하기 전의 루프·응답 기록·회고 메모 전부. 삭제는 되돌릴 수 없는 동작이라,
     * 실행 취소를 눌렀을 때 그대로 되살릴 수 있도록 통째로 들고 있는다.
     */
    suspend fun deleteLoop(loop: LoopBase): Boolean {
        val snapshot = loopRepository.deleteLoop(loop) ?: return false
        AppWidgetUpdateWorker.updateWidget(app)
        _pendingDeletion.value = PendingDeletion(snapshot, SystemClock.elapsedRealtime() + DELETE_UNDO_WINDOW_MS)
        return true
    }

    /** Restore the original identities and every child row in one transaction. */
    suspend fun restorePendingDeletion(): Boolean = restoreMutex.withLock {
        val pending = _pendingDeletion.value ?: return@withLock false
        if (SystemClock.elapsedRealtime() > pending.undoDeadlineElapsedMs) {
            _pendingDeletion.value = null
            return@withLock false
        }
        loopRepository.restoreLoop(pending.deleted)
        AppWidgetUpdateWorker.updateWidget(app)
        _pendingDeletion.value = null
        true
    }

    /** 사용자가 복구 창을 닫거나 시간이 만료되면, 더 이상 되돌릴 수 없음을 확정한다. */
    fun clearPendingDeletion() {
        _pendingDeletion.value = null
    }

    /**
     * 이 루프의 전체 기록을 CSV 한 장으로 만든다(날짜 · 상태 · 회고).
     * 백업과 다른 도구로의 반출을 겸하므로 날짜는 로캘과 무관한 ISO 형식으로 적는다.
     */
    suspend fun buildCsv(loopTitle: String): String {
        val timeline = loopRepository.historyRepository.snapshot().byId[loopId]
            ?: return ""
        val days = timeline.days(timeline.createdDate, LocalDate.now())
        return buildString {
            append("# ").append(csvCell(loopTitle)).append('\n')
            append("date,state,memo,title,color,planned_start_ms,planned_end_ms,anytime,estimated,revision_id,weekly_goal\n")
            days.forEach { day ->
                val state = when (day.response.done) {
                    LoopDoneVo.DoneState.DONE -> "done"
                    LoopDoneVo.DoneState.SKIP -> "skip"
                    LoopDoneVo.DoneState.IN_PROGRESS -> "in_progress"
                    else -> if (day.hasOccurrence) "no_response" else "not_scheduled"
                }
                val columns = listOf(day.date.toString(), state, day.note, day.loop.title,
                    day.loop.color.toString(), day.loop.startInDay.toString(), day.loop.endInDay.toString(),
                    day.loop.isAnyTime.toString(), day.estimated.toString(),
                    day.response.revisionId?.toString().orEmpty(), timeline.goalOn(day.date).toString())
                append(columns.joinToString(",", transform = ::csvCell)).append('\n')
            }
        }
    }

    /** 쉼표·따옴표·줄바꿈이 든 메모가 열을 깨뜨리지 않도록 RFC 4180 방식으로 감싼다. */
    private fun csvCell(raw: String): String = "\"" + raw.replace("\"", "\"\"") + "\""
}
