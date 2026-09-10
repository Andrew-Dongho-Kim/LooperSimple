package com.pnd.android.loop.appwidget

import com.fasterxml.jackson.databind.ObjectMapper
import com.pnd.android.loop.data.LoopDay
import com.pnd.android.loop.data.LoopDoneVo
import com.pnd.android.loop.data.LoopDoneVo.DoneState
import com.pnd.android.loop.data.LoopVo
import com.pnd.android.loop.data.TodayOccurrence
import com.pnd.android.loop.data.buildTodayOccurrences
import com.pnd.android.loop.data.isRespond
import com.pnd.android.loop.data.toLoopWithDone
import com.pnd.android.loop.util.toMs
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class WidgetStateTest {
    private val friday = LocalDate.of(2026, 9, 11)
    private val saturday = friday.plusDays(1)
    private val now = saturday.atTime(1, 0)
    private val mapper = ObjectMapper()
    private fun time(hour: Int) = LocalTime.of(hour, 0).toMs()
    private fun anytime() = LoopVo.anytime(
        id = 1, title = "Read", color = 0, created = friday.toMs(),
        activeDays = LoopDay.FRIDAY,
    )
    private fun row(date: LocalDate, state: Int, start: Long = 0L, end: Long = 0L) =
        anytime().toLoopWithDone(LoopDoneVo(1, date.toMs(), start, end, state))

    private fun roundTrip(widget: WidgetLoop): WidgetLoop {
        val map = mutableMapOf<String, Any?>()
        widget.putTo(map)
        val json = mapper.writeValueAsString(mapOf("loops" to listOf(map), "total" to 1, "registered" to 2))
        val restored = mapper.readValue(json, AppWidget.AppWidgetData::class.java)
        assertEquals(1, restored.total)
        assertEquals(2, restored.registered)
        return restored.loops.single().asWidgetLoop()
    }

    @Test fun `running anytime survives midnight on an unscheduled day`() {
        val yesterday = row(friday, DoneState.IN_PROGRESS, time(23), -1L)
        val today = row(saturday, DoneState.NO_RESPONSE)
        val occurrence = buildTodayOccurrences(listOf(today), mapOf(1 to yesterday), now).single()
        assertEquals(friday, occurrence.date)
        assertFalse(occurrence.isCarriedOver)
        val widget = roundTrip(occurrence.toWidgetLoop())
        assertTrue(widget.isRunning())
        assertTrue(widget.needsStartStop())
        assertEquals(friday.toMs(), widget.dateMs)
        assertEquals(time(23), widget.loop.startInDay)
    }

    @Test fun `stopped anytime does not spill into an unscheduled day`() {
        val yesterday = row(friday, DoneState.DONE, time(23), time(1))
        assertTrue(buildTodayOccurrences(
            listOf(row(saturday, DoneState.NO_RESPONSE)), mapOf(1 to yesterday), now,
        ).isEmpty())
    }

    @Test fun `missing done row with zero times shows start action`() {
        val widget = roundTrip(TodayOccurrence(row(friday, DoneState.NO_RESPONSE), friday, false).toWidgetLoop())
        assertFalse(widget.isRunning())
        assertTrue(widget.needsStartStop())
        assertEquals(-1L, widget.loop.startInDay)
    }

    @Test fun `undo with retained running times shows start action`() {
        val undone = row(friday, DoneState.NO_RESPONSE, time(23), -1L)
        val widget = roundTrip(TodayOccurrence(undone, friday, false).toWidgetLoop())
        assertFalse(widget.isRunning())
        assertTrue(widget.needsStartStop())
    }

    @Test fun `new start takes precedence over yesterday running record`() {
        val today = row(saturday, DoneState.IN_PROGRESS, time(0), -1L)
        val yesterday = row(friday, DoneState.IN_PROGRESS, time(23), -1L)
        val occurrence = buildTodayOccurrences(listOf(today), mapOf(1 to yesterday), now).single()
        assertEquals(saturday, occurrence.date)
        assertEquals(time(0), roundTrip(occurrence.toWidgetLoop()).loop.startInDay)
    }

    @Test fun `disabled running loop is not displayed`() {
        val today = row(saturday, DoneState.NO_RESPONSE).copy(enabled = false)
        val yesterday = row(friday, DoneState.IN_PROGRESS, time(23), -1L).copy(enabled = false)
        assertTrue(buildTodayOccurrences(listOf(today), mapOf(1 to yesterday), now).isEmpty())
    }

    @Test fun `overnight completed occurrence keeps its response after midnight`() {
        val loop = LoopVo.create(
            id = 2, title = "Sleep", color = 0, created = friday.toMs(),
            startInDay = time(22), endInDay = time(6), activeDays = LoopDay.FRIDAY,
        )
        val yesterday = loop.toLoopWithDone(LoopDoneVo(2, friday.toMs(), time(22), time(6), DoneState.DONE))
        val today = loop.toLoopWithDone(LoopDoneVo(2, saturday.toMs()))
        val occurrence = buildTodayOccurrences(listOf(today), mapOf(2 to yesterday), now).single()
        assertEquals(friday, occurrence.date)
        assertTrue(occurrence.loop.isRespond)
    }

    @Test fun `widget empty state counts survive JSON restoration`() {
        val restored = mapper.readValue(
            """{"loops":[],"total":3,"registered":4}""", AppWidget.AppWidgetData::class.java,
        )
        assertTrue(restored.loops.isEmpty())
        assertEquals(3, restored.total)
        assertEquals(4, restored.registered)
    }
}
