package cl.smartvolt.asistencia.util

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ValidacionesTest {

    @Test
    fun `una faena completa pasa la validacion`() {
        val resultado = Validaciones.validarFaena(
            name = "Faena Andes",
            address = "Camino a Farellones km 12",
            radiusMeters = "300",
            latitude = "-33.35",
            longitude = "-70.52"
        )

        assertTrue(resultado is Validaciones.ResultadoFaena.Valida)
        val datos = (resultado as Validaciones.ResultadoFaena.Valida).datos
        assertEquals("Faena Andes", datos.name)
        assertEquals(300, datos.radiusMeters)
        assertEquals(-33.35, datos.latitude, 0.0001)
    }

    @Test
    fun `la faena rechaza nombre vacio y radio fuera de rango`() {
        val sinNombre = Validaciones.validarFaena(
            name = "",
            address = "Camino a Farellones km 12",
            radiusMeters = "300",
            latitude = "-33.35",
            longitude = "-70.52"
        )
        val radioMalo = Validaciones.validarFaena(
            name = "Faena Andes",
            address = "Camino a Farellones km 12",
            radiusMeters = "10",
            latitude = "-33.35",
            longitude = "-70.52"
        )

        assertTrue(sinNombre is Validaciones.ResultadoFaena.Invalida)
        assertTrue(radioMalo is Validaciones.ResultadoFaena.Invalida)
        val errores = (radioMalo as Validaciones.ResultadoFaena.Invalida).errores
        assertFalse(errores.radiusMeters.isBlank())
    }

    @Test
    fun `la faena acepta coma decimal en las coordenadas`() {
        val resultado = Validaciones.validarFaena(
            name = "Faena Andes",
            address = "Camino a Farellones km 12",
            radiusMeters = "300",
            latitude = "-33,35",
            longitude = "-70,52"
        )

        assertTrue(resultado is Validaciones.ResultadoFaena.Valida)
        val datos = (resultado as Validaciones.ResultadoFaena.Valida).datos
        assertEquals(-33.35, datos.latitude, 0.0001)
        assertEquals(-70.52, datos.longitude, 0.0001)
    }

    @Test
    fun `una solicitud de horas extra coherente es valida`() {
        val resultado = Validaciones.validarHoraExtra(
            fecha = LocalDate.of(2026, 11, 4),
            desde = "18:00",
            hasta = "20:30",
            motivo = "Cierre de faena programado"
        )

        assertTrue(resultado is Validaciones.ResultadoHoraExtra.Valida)
        val datos = (resultado as Validaciones.ResultadoHoraExtra.Valida).datos
        assertEquals(LocalDate.of(2026, 11, 4), datos.date)
        assertTrue(datos.endMillis > datos.startMillis)
        assertEquals("Cierre de faena programado", datos.reason)
    }

    @Test
    fun `los campos vacios de horas extra generan error`() {
        val resultado = Validaciones.validarHoraExtra(
            fecha = LocalDate.of(2026, 11, 4),
            desde = "",
            hasta = "",
            motivo = ""
        )

        val errores = (resultado as Validaciones.ResultadoHoraExtra.Invalida).errores
        assertTrue(errores.desde.isNotEmpty())
        assertTrue(errores.hasta.isNotEmpty())
        assertTrue(errores.motivo.isNotEmpty())
    }

    @Test
    fun `la hora de termino debe ser posterior a la de inicio`() {
        val resultado = Validaciones.validarHoraExtra(
            fecha = LocalDate.of(2026, 11, 4),
            desde = "20:00",
            hasta = "18:00",
            motivo = "Trabajo adicional en obra"
        )

        val errores = (resultado as Validaciones.ResultadoHoraExtra.Invalida).errores
        assertTrue(errores.hasta.contains("posterior"))
    }

    @Test
    fun `el motivo demasiado corto se rechaza`() {
        val resultado = Validaciones.validarHoraExtra(
            fecha = LocalDate.of(2026, 11, 4),
            desde = "18:00",
            hasta = "20:00",
            motivo = "ok"
        )

        val errores = (resultado as Validaciones.ResultadoHoraExtra.Invalida).errores
        assertTrue(errores.motivo.isNotEmpty())
    }
}
