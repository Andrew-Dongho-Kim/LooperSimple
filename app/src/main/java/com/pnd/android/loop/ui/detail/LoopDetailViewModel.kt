package com.pnd.android.loop.ui.detail

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pnd.android.loop.appwidget.AppWidgetUpdateWorker
import com.pnd.android.loop.common.NavigatePage
import com.pnd.android.loop.data.AppDatabase
import com.pnd.android.loop.data.LoopBase
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.asLoopVo
import com.pnd.android.loop.ui.home.viewmodel.LoopRepository
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import com.pnd.android.loop.util.todayFlow
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import javax.inject.Inject

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
    private val loopDoneDao = appDb.loopDoneDao()
    private val loopRetrospectDao = appDb.loopRetrospectDao()

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
                .sortedByDescending { it.date }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val allResponses = loopDoneDao.getAllFlow(loopId)

    /**
     * 화면이 그리는 모든 수치. 예전에는 완료율·스트릭·추세·요일별·월별을 각 컴포저블이
     * 컴포지션 중에 계산했고(최대 60일 × 7일 창을 도는 롤링 계산 포함), 개수 넷은 DAO flow 를
     * 따로 구독했다. 지금은 한 번만, 그것도 UI 스레드 밖에서 계산한다.
     */
    internal val stats = combine(loop, allResponses, memos, today) { loop, responses, memos, today ->
        computeDetailStats(
            responses = responses,
            memoDates = memos.map { it.date.toLocalDate() }.toSet(),
            activeDays = loop.activeDays,
            weeklyGoal = loop.weeklyGoal,
            createdDate = loop.created.toLocalDate(),
            today = today,
        )
    }
        .flowOn(Dispatchers.Default)
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = DetailStats.empty(),
        )

    /** 선택한 날짜에 남긴 회고 메모 본문. 없으면 null. */
    suspend fun retrospectOf(date: LocalDate): String? =
        loopRetrospectDao.getRetrospect(loopId = loopId, localDate = date.toMs())?.text

    /**
     * 선택한 날짜의 회고 메모를 저장한다. 내용이 비어 있으면 null 로 지워, 달력의 메모 마커도
     * 함께 사라지게 한다. 저장이 실제로 끝난 뒤에 돌아오므로, 화면은 그때 확인 메시지를 띄운다.
     */
    suspend fun saveRetrospect(date: LocalDate, text: String) {
        loopRetrospectDao.insert(
            LoopRetrospectVo(
                loopId = loopId,
                date = date.toMs(),
                text = text.ifBlank { null },
            )
        )
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
        loop: LoopBase,
        localDate: LocalDate,
        @LoopDoneVo.DoneState doneState: Int,
    ) {
        val existing = loopDoneDao.getDoneState(loopId = loopId, date = localDate.toMs())
        val loopForWrite = if (
            loop.isAnyTime &&
            existing != null &&
            doneState != LoopDoneVo.DoneState.NO_RESPONSE
        ) {
            loop.copyAs(startInDay = existing.startInDay, endInDay = existing.endInDay)
        } else {
            loop
        }

        loopRepository.changeLoopState(
            loop = loopForWrite,
            localDate = localDate,
            doneState = doneState,
        )
        AppWidgetUpdateWorker.updateWidget(app)
    }

    fun enableLoop(
        loop: LoopBase,
        enabled: Boolean
    ) {
        coroutineScope.launch {
            loopRepository.addOrUpdateLoop(loop.copyAs(enabled = enabled).asLoopVo())
            AppWidgetUpdateWorker.updateWidget(app)
        }
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
     * 이 루프와 시간대가 겹치는 **다른** 루프의 수. 스케줄 섹션에서 "이 시간대에 N개 더"로 알려
     * 시간을 옮길지 판단하게 돕는다. [numberOfLoopsAtTheSameTime] 은 자기 자신을 포함한다.
     */
    suspend fun overlappingLoopCount(loop: LoopBase) =
        (numberOfLoopsAtTheSameTime(loop) - 1).coerceAtLeast(0)

    /**
     * 삭제하기 전의 루프·응답 기록·회고 메모 전부. 삭제는 되돌릴 수 없는 동작이라,
     * 실행 취소를 눌렀을 때 그대로 되살릴 수 있도록 통째로 들고 있는다.
     */
    data class DeletedLoop(
        val loop: LoopVo,
        val responses: List<LoopDoneVo>,
        val retrospects: List<LoopRetrospectVo>,
    )

    /**
     * 루프를 지우면서 되살리기용 스냅샷을 돌려준다. 스냅샷을 못 만들면(이미 지워졌다면) null.
     * 화면은 이 값을 들고 실행 취소 스낵바를 띄운다.
     */
    suspend fun deleteLoop(loop: LoopBase): DeletedLoop? {
        val snapshot = loopDao.getLoop(loopId)?.let { vo ->
            DeletedLoop(
                loop = vo,
                responses = loopDoneDao.getAllFlow(loopId).first(),
                retrospects = loopRetrospectDao.getRetrospectsFlow(loopId).first(),
            )
        }
        loopRepository.deleteLoop(loop)
        AppWidgetUpdateWorker.updateWidget(app)
        return snapshot
    }

    /**
     * [deleteLoop] 로 지운 루프를 기록·메모까지 되돌린다.
     *
     * 루프를 먼저 되살려야 loop_done / loop_memo 의 외래 키가 걸리지 않는다. 되살리는 과정에서
     * 저장소가 오늘 몫의 빈 응답 행을 하나 만들어 두므로, 스냅샷을 나중에 덮어써 원래 상태로 돌린다.
     */
    fun restoreLoop(deleted: DeletedLoop) {
        coroutineScope.launch {
            loopRepository.addOrUpdateLoop(deleted.loop)
            deleted.responses.forEach { loopDoneDao.addOrUpdate(it) }
            deleted.retrospects.forEach { loopRetrospectDao.insert(it) }
            AppWidgetUpdateWorker.updateWidget(app)
        }
    }

    /**
     * 이 루프의 전체 기록을 CSV 한 장으로 만든다(날짜 · 상태 · 회고).
     * 백업과 다른 도구로의 반출을 겸하므로 날짜는 로캘과 무관한 ISO 형식으로 적는다.
     */
    suspend fun buildCsv(loopTitle: String): String {
        val responses = allResponses.first().sortedBy { it.date }
        val memoByDate = loopRetrospectDao.getRetrospectsFlow(loopId).first()
            .associate { it.date to (it.text ?: "") }

        return buildString {
            append("# ").append(csvCell(loopTitle)).append('\n')
            append("date,state,memo\n")
            responses.forEach { response ->
                append(response.date.toLocalDate().toString()).append(',')
                append(
                    when (response.done) {
                        LoopDoneVo.DoneState.DONE -> "done"
                        LoopDoneVo.DoneState.SKIP -> "skip"
                        LoopDoneVo.DoneState.DISABLED -> "disabled"
                        LoopDoneVo.DoneState.IN_PROGRESS -> "in_progress"
                        else -> "no_response"
                    }
                ).append(',')
                append(csvCell(memoByDate[response.date] ?: ""))
                append('\n')
            }
        }
    }

    /** 쉼표·따옴표·줄바꿈이 든 메모가 열을 깨뜨리지 않도록 RFC 4180 방식으로 감싼다. */
    private fun csvCell(raw: String): String = "\"" + raw.replace("\"", "\"\"") + "\""
}
