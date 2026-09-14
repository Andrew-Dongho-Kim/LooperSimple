package com.pnd.android.loop.data.history

import androidx.room.withTransaction
import com.pnd.android.loop.data.*
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.util.dayForLoop
import com.pnd.android.loop.util.toLocalDate
import com.pnd.android.loop.util.toMs
import java.time.Clock
import java.time.LocalDate
import java.time.LocalDateTime
import javax.inject.Inject
import javax.inject.Singleton

/** Database commands only. Alarms and widgets run after these transactions commit. */
@Singleton
class LoopMutationStore @Inject constructor(private val db: AppDatabase) {
    private val loops get() = db.loopDao()
    private val records get() = db.loopDoneDao()
    private val history get() = db.loopHistoryDao()

    suspend fun saveSettings(values: List<LoopVo>, clock: Clock = Clock.systemDefaultZone()): List<LoopVo> =
        db.withTransaction {
            val now = LocalDateTime.now(clock)
            values.map { saveSettings(it, now, clock.millis()) }
        }

    private suspend fun saveSettings(requested: LoopVo, now: LocalDateTime, recordedAt: Long): LoopVo {
        if (requested.loopId == 0) {
            val saved = requested.copy(loopId = loops.insert(requested).single().toInt())
            val created = saved.created.toLocalDate().toEpochDay()
            val revisionId = history.insertRevision(LoopRevisionVo(
                loopId = saved.loopId, recordedAt = recordedAt,
                effectiveFrom = created, goalEffectiveFrom = created, knownFrom = created,
                settings = LoopSettings.from(saved),
            ))
            reconcileUnstartedDay(saved, revisionId, now.toLocalDate(), existing = null)
            return saved
        }

        val previous = history.get(requested.loopId) ?: error("This loop was deleted")
        val before = LoopSettings.from(previous.loop)
        val after = LoopSettings.from(requested)
        if (before == after) return previous.loop
        val saved = after.applyTo(previous.loop) // Keep identity and creation time from the DB.
        val timeline = LoopTimeline(previous)
        val today = now.toLocalDate()
        val existing = timeline.responseOn(today)
        val oldDay = timeline.day(today)
        val preserve = shouldPreserveOccurrence(oldDay, now)
        if (preserve && existing == null && oldDay != null) {
            records.addOrUpdate(plannedRecord(oldDay.loop, today, timeline.revisionOn(today)?.revisionId))
        }
        val last = previous.revisions.maxByOrNull { it.revisionId }
        loops.update(saved)
        val goalFrom = if (before.weeklyGoal != after.weeklyGoal) {
            nextWeekStart(today).toEpochDay()
        } else last?.goalEffectiveFrom ?: timeline.createdDate.toEpochDay()
        val revisionId = history.insertRevision(LoopRevisionVo(
            loopId = saved.loopId, recordedAt = recordedAt, effectiveFrom = today.toEpochDay(),
            goalEffectiveFrom = goalFrom, knownFrom = last?.knownFrom ?: today.toEpochDay(),
            settings = after,
        ))
        if (!preserve) reconcileUnstartedDay(saved, revisionId, today, existing)
        return saved
    }

    private suspend fun reconcileUnstartedDay(loop: LoopVo, revisionId: Long, date: LocalDate, existing: LoopDoneVo?) {
        if (loop.enabled && (loop.activeDays and dayForLoop(date)) != 0) {
            records.addOrUpdate(plannedRecord(loop, date, revisionId).copy(date = existing?.date ?: date.toMs()))
        } else if (existing != null) {
            records.delete(loop.loopId, existing.date)
        }
    }

    /** Fill only operational days; statistics reconstruct older missing days without writing them. */
    suspend fun ensureOperationalDays(today: LocalDate = LocalDate.now()) = db.withTransaction {
        history.getAll().forEach { saved ->
            val timeline = LoopTimeline(saved)
            listOf(today.minusDays(1), today).forEach { date ->
                val day = timeline.day(date)
                if (day?.hasOccurrence == true && timeline.responseOn(date) == null) {
                    records.addIfAbsent(plannedRecord(day.loop, date, timeline.revisionOn(date)?.revisionId))
                }
            }
        }
    }

    suspend fun setResponse(
        loopId: Int,
        date: LocalDate,
        state: Int,
        suppliedTimes: Pair<Long, Long>? = null,
        clock: Clock = Clock.systemDefaultZone(),
    ) = db.withTransaction {
        require(state in listOf(DoneState.NO_RESPONSE, DoneState.DONE, DoneState.SKIP, DoneState.IN_PROGRESS))
        val saved = history.get(loopId) ?: return@withTransaction
        val timeline = LoopTimeline(saved)
        require(date >= timeline.createdDate && date <= LocalDate.now(clock))
        val existing = timeline.responseOn(date)
        val plan = timeline.day(date)?.loop ?: timeline.settingsOn(date)
        // Undoing an explicit off-schedule entry restores an empty day, not a scheduled miss.
        if (state == DoneState.NO_RESPONSE && (!plan.enabled || (plan.activeDays and dayForLoop(date)) == 0)) {
            existing?.let { records.delete(loopId, it.date) }
            return@withTransaction
        }
        val revisionId = existing?.revisionId ?: timeline.revisionOn(date)?.revisionId
        val initial = existing ?: plannedRecord(plan, date, revisionId)
        records.addOrUpdate(updatedResponse(initial, plan, state, suppliedTimes, clock))
    }

    suspend fun respondIfUnanswered(
        loopId: Int,
        requestedDate: LocalDate?,
        state: Int,
        clock: Clock = Clock.systemDefaultZone(),
    ) = db.withTransaction {
        require(state == DoneState.DONE || state == DoneState.SKIP)
        val saved = history.get(loopId) ?: return@withTransaction
        if (!saved.loop.enabled) return@withTransaction
        val timeline = LoopTimeline(saved)
        val now = LocalDateTime.now(clock)
        val today = now.toLocalDate()
        val date = requestedDate ?: com.pnd.android.loop.util.currentOccurrenceDate(
            timeline.liveLoop(today), timeline.liveLoop(today.minusDays(1)), now,
        )
        if (date < timeline.createdDate || date > today) return@withTransaction
        val day = timeline.day(date) ?: return@withTransaction
        if (!day.hasOccurrence || day.response.isRespond()) return@withTransaction
        setResponse(loopId, date, state, clock = clock)
    }

    suspend fun start(
        loopId: Int,
        requestedDate: LocalDate? = null,
        clock: Clock = Clock.systemDefaultZone(),
    ) = db.withTransaction {
        val saved = history.get(loopId) ?: return@withTransaction
        val timeline = LoopTimeline(saved)
        val today = LocalDate.now(clock)
        if (requestedDate != null && requestedDate != today) return@withTransaction
        val day = timeline.day(today) ?: return@withTransaction
        if (!saved.loop.enabled || !day.loop.isAnyTime || !day.hasOccurrence) return@withTransaction
        // A repeated/stale start must not erase a running or already answered occurrence.
        if (listOf(today, today.minusDays(1)).any { date ->
            timeline.responseOn(date)?.done == DoneState.IN_PROGRESS
        }) return@withTransaction
        if (timeline.responseOn(today)?.done in listOf(DoneState.DONE, DoneState.SKIP)) return@withTransaction
        setResponse(loopId, today, DoneState.IN_PROGRESS, clock = clock)
    }

    suspend fun stop(loopId: Int, requestedDate: LocalDate? = null, clock: Clock = Clock.systemDefaultZone()) = db.withTransaction {
        val saved = history.get(loopId) ?: return@withTransaction
        val timeline = LoopTimeline(saved)
        val today = LocalDate.now(clock)
        val date = requestedDate ?: listOf(today, today.minusDays(1)).firstOrNull {
            timeline.responseOn(it)?.done == DoneState.IN_PROGRESS
        } ?: return@withTransaction
        if (timeline.responseOn(date)?.done != DoneState.IN_PROGRESS) return@withTransaction
        setResponse(loopId, date, DoneState.DONE, clock = clock)
    }

    suspend fun setEnabled(loopId: Int, enabled: Boolean) = db.withTransaction {
        val current = history.get(loopId)?.loop ?: return@withTransaction
        saveSettings(current.copy(enabled = enabled), LocalDateTime.now(), System.currentTimeMillis())
    }

    suspend fun saveNote(loopId: Int, date: LocalDate, text: String) = db.withTransaction {
        val saved = history.get(loopId) ?: return@withTransaction
        require(date >= LoopTimeline(saved).createdDate && date <= LocalDate.now())
        val previous = saved.notes.firstOrNull { it.localDate() == date }
        db.loopRetrospectDao().insert(LoopRetrospectVo(
            loopId = loopId, date = previous?.date ?: date.toMs(),
            text = text.ifBlank { null }, localEpochDay = date.toEpochDay(),
        ))
    }

    suspend fun delete(loopId: Int): LoopHistory? = db.withTransaction {
        val saved = history.get(loopId) ?: return@withTransaction null
        loops.delete(loopId)
        saved
    }

    suspend fun restore(saved: LoopHistory) = db.withTransaction {
        check(loops.getLoop(saved.loop.loopId) == null) { "Cannot restore over an existing loop" }
        loops.insert(saved.loop)
        history.restoreRevisions(saved.revisions)
        saved.responses.forEach { records.addOrUpdate(it) }
        saved.notes.forEach { db.loopRetrospectDao().insert(it) }
    }
}
