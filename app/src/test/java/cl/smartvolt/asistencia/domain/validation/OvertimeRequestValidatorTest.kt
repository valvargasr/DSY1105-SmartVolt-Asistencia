package cl.smartvolt.asistencia.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class OvertimeRequestValidatorTest {

    private fun entrada(
        fecha: String = "4/11/2026",
        desde: String = "18:00",
        hasta: String = "20:30",
        motivo: String = "Cierre de faena programado"
    ) = OvertimeRequestInput(fecha, desde, hasta, motivo)

    @Test
    fun `solicitud completa y coherente es valida`() {
        val resultado = OvertimeRequestValidator.validate(entrada())

        assertTrue(resultado is OvertimeRequestResult.Valid)
        val datos = (resultado as OvertimeRequestResult.Valid).data
        assertEquals(LocalDate.of(2026, 11, 4), datos.date)
        assertTrue(datos.endMillis > datos.startMillis)
        assertEquals("Cierre de faena programado", datos.reason)
    }

    @Test
    fun `campos vacios generan error por campo`() {
        val resultado = OvertimeRequestValidator.validate(
            entrada(fecha = "", desde = "", hasta = "", motivo = "")
        )

        val errores = (resultado as OvertimeRequestResult.Invalid).errors
        assertTrue(errores.fecha.isNotEmpty())
        assertTrue(errores.desde.isNotEmpty())
        assertTrue(errores.hasta.isNotEmpty())
        assertTrue(errores.motivo.isNotEmpty())
    }

    @Test
    fun `fecha con formato incorrecto se rechaza`() {
        val resultado = OvertimeRequestValidator.validate(
            entrada(fecha = "2026-11-04")
        )

        val errores = (resultado as OvertimeRequestResult.Invalid).errors
        assertTrue(errores.fecha.isNotEmpty())
        assertTrue(errores.desde.isEmpty())
    }

    @Test
    fun `hora de termino debe ser posterior a la de inicio`() {
        val resultado = OvertimeRequestValidator.validate(
            entrada(desde = "20:00", hasta = "18:00")
        )

        val errores = (resultado as OvertimeRequestResult.Invalid).errors
        assertTrue(errores.hasta.contains("posterior"))
    }

    @Test
    fun `motivo demasiado corto se rechaza`() {
        val resultado = OvertimeRequestValidator.validate(
            entrada(motivo = "ok")
        )

        val errores = (resultado as OvertimeRequestResult.Invalid).errors
        assertTrue(errores.motivo.isNotEmpty())
    }

    @Test
    fun `acepta horas sin cero inicial`() {
        val resultado = OvertimeRequestValidator.validate(
            entrada(desde = "8:00", hasta = "9:30")
        )

        assertTrue(resultado is OvertimeRequestResult.Valid)
    }
}
