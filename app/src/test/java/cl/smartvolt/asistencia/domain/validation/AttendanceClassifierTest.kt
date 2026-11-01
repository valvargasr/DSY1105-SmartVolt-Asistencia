package cl.smartvolt.asistencia.domain.validation

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.domain.model.DailyAttendanceStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AttendanceClassifierTest {

    private val config = AttendanceClassifier.ShiftConfig(
        shiftStartMillis = 1_000_000L,
        toleranceMillis = 15 * 60 * 1000L, // 15 min
        absenceCutoffMillis = 1_000_000L + 60 * 60 * 1000L // 1 hora
    )

    private fun checkInAt(millis: Long): LocalAttendanceEvent =
        LocalAttendanceEvent(
            id = "evt-1",
            type = LocalAttendanceEvent.TYPE_CHECK_IN,
            timestampMillis = millis
        )

    @Test
    fun `sin turno programado retorna pendiente`() {
        val result = AttendanceClassifier.classify(
            checkIn = checkInAt(1_000_000L),
            config = config,
            hasScheduledShift = false,
            isHoliday = false
        )
        assertEquals(DailyAttendanceStatus.PENDING, result)
    }

    @Test
    fun `feriado retorna pendiente`() {
        val result = AttendanceClassifier.classify(
            checkIn = checkInAt(1_000_000L),
            config = config,
            hasScheduledShift = true,
            isHoliday = true
        )
        assertEquals(DailyAttendanceStatus.PENDING, result)
    }

    @Test
    fun `sin marca retorna pendiente`() {
        val result = AttendanceClassifier.classify(
            checkIn = null,
            config = config,
            hasScheduledShift = true,
            isHoliday = false
        )
        assertEquals(DailyAttendanceStatus.PENDING, result)
    }

    @Test
    fun `sin config retorna pendiente`() {
        val result = AttendanceClassifier.classify(
            checkIn = checkInAt(1_000_000L),
            config = null,
            hasScheduledShift = true,
            isHoliday = false
        )
        assertEquals(DailyAttendanceStatus.PENDING, result)
    }

    @Test
    fun `llegada a tiempo`() {
        val result = AttendanceClassifier.classify(
            checkIn = checkInAt(1_000_000L), // exactamente a la hora
            config = config,
            hasScheduledShift = true,
            isHoliday = false
        )
        assertEquals(DailyAttendanceStatus.PRESENT_ON_TIME, result)
    }

    @Test
    fun `llegada dentro de tolerancia es a tiempo`() {
        val result = AttendanceClassifier.classify(
            checkIn = checkInAt(1_000_000L + 10 * 60 * 1000L), // 10 min después
            config = config,
            hasScheduledShift = true,
            isHoliday = false
        )
        assertEquals(DailyAttendanceStatus.PRESENT_ON_TIME, result)
    }

    @Test
    fun `llegada después de tolerancia es atraso`() {
        val result = AttendanceClassifier.classify(
            checkIn = checkInAt(1_000_000L + 20 * 60 * 1000L), // 20 min después
            config = config,
            hasScheduledShift = true,
            isHoliday = false
        )
        assertEquals(DailyAttendanceStatus.PRESENT_LATE, result)
    }

    @Test
    fun `llegada después del corte es ausente`() {
        val result = AttendanceClassifier.classify(
            checkIn = checkInAt(1_000_000L + 90 * 60 * 1000L), // 90 min después
            config = config,
            hasScheduledShift = true,
            isHoliday = false
        )
        assertEquals(DailyAttendanceStatus.ABSENT, result)
    }

    @Test
    fun `isPastAbsenceCutoff con config`() {
        assertTrue(
            AttendanceClassifier.isPastAbsenceCutoff(
                nowMillis = 1_000_000L + 90 * 60 * 1000L,
                config = config
            )
        )
    }

    @Test
    fun `isPastAbsenceCutoff sin config retorna false`() {
        assertFalse(
            AttendanceClassifier.isPastAbsenceCutoff(
                nowMillis = 1_000_000L + 90 * 60 * 1000L,
                config = null
            )
        )
    }

    @Test
    fun `formatTime produce HH mm`() {
        val formatted = AttendanceClassifier.formatTime(1_700_000_000_000L)
        assertTrue(formatted.matches(Regex("\\d{2}:\\d{2}")))
    }
}
