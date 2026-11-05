package cl.smartvolt.asistencia.feature.overtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OvertimeFormValidatorTest {

    @Test
    fun `formulario vacio muestra error en todos los campos`() {
        val result = OvertimeFormValidator.validate(null, "", "", "")
        assertTrue(result.date.isNotEmpty())
        assertTrue(result.start.isNotEmpty())
        assertTrue(result.end.isNotEmpty())
        assertTrue(result.reason.isNotEmpty())
        assertTrue(!result.isValid)
    }

    @Test
    fun `hora con formato incorrecto se rechaza`() {
        val result = OvertimeFormValidator.validate(
            LocalDate.of(2026, 11, 4), "6pm", "20:00", "Cierre de faena"
        )
        assertTrue(result.start.isNotEmpty())
    }

    @Test
    fun `hora final igual o anterior al inicio se rechaza`() {
        val result = OvertimeFormValidator.validate(
            LocalDate.of(2026, 11, 4), "20:00", "19:00", "Cierre de faena"
        )
        assertTrue(result.end.isNotEmpty())
    }

    @Test
    fun `solicitud completa y con horarios validos se acepta`() {
        val result = OvertimeFormValidator.validate(
            LocalDate.of(2026, 11, 4), "18:00", "20:00", "Trabajo adicional en obra"
        )
        assertEquals(OvertimeFormErrors(), result)
        assertTrue(result.isValid)
    }
}
