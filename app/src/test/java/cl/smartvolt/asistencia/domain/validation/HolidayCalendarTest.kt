package cl.smartvolt.asistencia.domain.validation

import cl.smartvolt.asistencia.domain.model.HolidayCalendar
import cl.smartvolt.asistencia.domain.model.HolidayType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class HolidayCalendarTest {

    @Test
    fun `feriado nacional se detecta`() {
        val navidad = LocalDate.of(2026, 12, 25)
        assertTrue(HolidayCalendar.isHoliday(navidad))
    }

    @Test
    fun `dia laboral no es feriado`() {
        val diaLaboral = LocalDate.of(2026, 10, 29)
        assertFalse(HolidayCalendar.isHoliday(diaLaboral))
    }

    @Test
    fun `entrada de feriado tiene tipo correcto`() {
        val entry = HolidayCalendar.getEntry(LocalDate.of(2026, 9, 18))
        assertNotNull(entry)
        assertEquals(HolidayType.NATIONAL_HOLIDAY, entry!!.type)
    }

    @Test
    fun `dia sin feriado retorna null`() {
        val entry = HolidayCalendar.getEntry(LocalDate.of(2026, 10, 29))
        assertNull(entry)
    }

    @Test
    fun `sin turno programado no se genera ausencia`() {
        val fecha = LocalDate.of(2026, 10, 29)
        assertFalse(HolidayCalendar.shouldGenerateAbsence(fecha, hasScheduledShift = false))
    }

    @Test
    fun `feriado sin jornada especial no genera ausencia`() {
        val navidad = LocalDate.of(2026, 12, 25)
        assertFalse(HolidayCalendar.shouldGenerateAbsence(navidad, hasScheduledShift = true))
    }

    @Test
    fun `dia laboral con turno genera ausencia`() {
        val diaLaboral = LocalDate.of(2026, 10, 29)
        assertTrue(HolidayCalendar.shouldGenerateAbsence(diaLaboral, hasScheduledShift = true))
    }
}
