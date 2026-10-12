package com.pnd.android.loop.ui.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.pnd.android.loop.R
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopRetrospectVo
import com.pnd.android.loop.data.history.localDate
import java.time.LocalDate
import kotlinx.coroutines.CancellationException

private val JournalDateSaver = Saver<LocalDate, Long>(
    save = { it.toEpochDay() }, restore = { LocalDate.ofEpochDay(it) },
)

/** Records are read-only; only notes open an editor and preserve their draft lifecycle. */
@Composable
internal fun TimelineJournalSection(
    stats: DetailStats,
    memos: List<LoopRetrospectVo>,
    responses: Map<LocalDate, LoopDoneVo>,
    accent: Color,
    onLoadMemo: suspend (LocalDate) -> String?,
    onSaveMemo: suspend (LocalDate, String) -> Unit,
    onSaveMemoInBackground: (LocalDate, String) -> Unit,
) {
    var selectedDate by rememberSaveable(stateSaver = JournalDateSaver) { mutableStateOf(stats.today) }
    var visibleDate by rememberSaveable(stateSaver = JournalDateSaver) { mutableStateOf(stats.today) }
    var monthly by rememberSaveable { mutableStateOf(false) }
    var showMemoSheet by rememberSaveable { mutableStateOf(false) }
    var showAllMemos by rememberSaveable { mutableStateOf(false) }
    var memoLoadFailed by rememberSaveable(selectedDate) { mutableStateOf(false) }
    var memoLoadAttempt by rememberSaveable { mutableIntStateOf(0) }
    var memo by rememberSaveable(selectedDate) { mutableStateOf("") }
    var savedMemo by rememberSaveable(selectedDate) { mutableStateOf<String?>(null) }
    val dirty = savedMemo != null && memo != savedMemo

    LaunchedEffect(selectedDate, showMemoSheet, memoLoadAttempt) {
        if (showMemoSheet && savedMemo == null) {
            memoLoadFailed = false
            try {
                val loaded = onLoadMemo(selectedDate).orEmpty()
                savedMemo = loaded
                memo = loaded
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                memoLoadFailed = true
            }
        }
    }
    fun flushDraft() {
        if (dirty) {
            onSaveMemoInBackground(selectedDate, memo)
            savedMemo = memo
        }
    }
    fun selectDay(date: LocalDate) {
        if (date < stats.createdDate || date > stats.today) return
        if (date != selectedDate) flushDraft()
        selectedDate = date
        visibleDate = date
    }
    fun editMemo(date: LocalDate) {
        if (date < stats.createdDate || date > stats.today) return
        selectDay(date)
        // Reload on each fresh editor opening so external saves never leave a stale draft.
        savedMemo = null
        showMemoSheet = true
    }
    val latestDirty by rememberUpdatedState(dirty)
    val latestDate by rememberUpdatedState(selectedDate)
    val latestMemo by rememberUpdatedState(memo)
    val latestSave by rememberUpdatedState(onSaveMemoInBackground)
    DisposableEffect(Unit) {
        onDispose { if (latestDirty) latestSave(latestDate, latestMemo) }
    }

    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
        JournalCalendarCard(
            stats = stats, accent = accent, selectedDate = selectedDate,
            visibleDate = visibleDate, monthly = monthly,
            onMonthlyChange = { monthly = it; visibleDate = selectedDate },
            onNavigate = { date -> selectDay(date.coerceIn(stats.createdDate, stats.today)) },
            onSelect = ::selectDay,
            onToday = { selectDay(stats.today) },
        )
        RecordedDayCard(
            date = selectedDate, response = responses[selectedDate],
            state = stats.doneStateByDate[selectedDate], accent = accent,
        )
        JournalMemoCard(
            date = selectedDate,
            text = memos.firstOrNull { it.localDate() == selectedDate }?.text.orEmpty(),
            onEdit = { editMemo(selectedDate) },
        )
        if (memos.isNotEmpty()) {
            TextActionButton(
                text = androidx.compose.ui.res.stringResource(
                    if (showAllMemos) R.string.detail_hide_all_memos else R.string.detail_show_all_memos,
                    memos.size,
                ),
                onClick = { showAllMemos = !showAllMemos },
            )
            if (showAllMemos) {
                memos.forEach { entry ->
                    JournalMemoCard(
                        date = entry.localDate(), text = entry.text.orEmpty(),
                        onEdit = { editMemo(entry.localDate()) },
                    )
                }
            }
        }
    }
    if (showMemoSheet) {
        JournalMemoSheet(
            date = selectedDate,
            editable = selectedDate in stats.createdDate..stats.today,
            memo = memo, memoLoaded = savedMemo != null,
            memoLoadFailed = memoLoadFailed, memoDirty = dirty,
            onRetryLoad = { memoLoadAttempt++ }, onMemoChange = { memo = it },
            onSaveMemo = {
                val date = selectedDate
                val text = memo
                onSaveMemo(date, text)
                if (selectedDate == date) savedMemo = text
            },
            onDismiss = { flushDraft(); showMemoSheet = false },
        )
    }
}
