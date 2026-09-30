package com.pnd.android.loop.state

import com.pnd.android.loop.R
import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class LoopStateTest {
    private val today = LocalDate.of(2026, 9, 23)

    @Test fun `unanswered settles only after its date while responded settles immediately`() {
        assertFalse(DoneState.NO_RESPONSE.isSettledOn(today, today))
        assertTrue(DoneState.NO_RESPONSE.isSettledOn(today.minusDays(1), today))
        for (state in listOf(DoneState.DONE, DoneState.SKIP)) {
            assertTrue(state.isSettledOn(today, today))
            assertTrue(state.isSettledOn(today.minusDays(1), today))
            assertFalse(state.isSettledOn(today.plusDays(1), today))
        }
    }

    @Test fun `disabled unscheduled running and unknown states never enter final rates`() {
        for (state in listOf(DoneState.DISABLED, NOT_SCHEDULED, DoneState.IN_PROGRESS, 99, null)) {
            assertFalse(state.isSettledOn(today.minusDays(1), today))
            assertFalse(state.isSettledOn(today, today))
            assertFalse(state.isRespond())
        }
    }

    @Test fun `missing state is not an unanswered occurrence`() {
        val state: Int? = null
        assertFalse(state.isNoResponse())
        assertFalse(state.isInProgress())
        assertFalse(state.isDisabled())
        assertEquals(R.string.detail_day_no_record, state.stateLabelRes())
        assertEquals(R.string.no_response, DoneState.NO_RESPONSE.stateLabelRes())
    }

    @Test fun `running disabled and unscheduled labels remain distinct from no record`() {
        assertEquals(R.string.history_in_progress, DoneState.IN_PROGRESS.stateLabelRes())
        assertEquals(R.string.detail_loop_state_inactive, DoneState.DISABLED.stateLabelRes())
        assertEquals(R.string.all_history_inactive_day, NOT_SCHEDULED.stateLabelRes())
        assertEquals("disabled", DoneState.DISABLED.stateExportName())
        assertEquals("not_scheduled", NOT_SCHEDULED.stateExportName())
        assertEquals("unknown", 99.stateExportName())
    }

    @Test fun `response writes reject display only and legacy disabled states`() {
        for (state in listOf(DoneState.NO_RESPONSE, DoneState.DONE, DoneState.SKIP, DoneState.IN_PROGRESS)) {
            assertTrue(state.isWritableResponse())
        }
        for (state in listOf(DoneState.DISABLED, NOT_SCHEDULED, 99)) {
            assertFalse(state.isWritableResponse())
        }
    }
}
