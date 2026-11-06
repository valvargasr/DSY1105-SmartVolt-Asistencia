package cl.smartvolt.asistencia.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorksiteValidatorTest {

    @Test
    fun `un formulario completo pasa la validacion`() {
        val resultado = WorksiteValidator.validate(formulario())

        assertTrue(resultado is WorksiteValidation.Valid)
        val datos = (resultado as WorksiteValidation.Valid).data
        assertEquals("Faena Andes", datos.name)
        assertEquals("Camino a Farellones km 12", datos.address)
        assertEquals(300, datos.radiusMeters)
        assertEquals(-33.35, datos.latitude, 0.0001)
        assertEquals(-70.52, datos.longitude, 0.0001)
    }

    @Test
    fun `el nombre vacio se rechaza`() {
        val resultado = WorksiteValidator.validate(formulario(name = ""))

        assertTrue(resultado is WorksiteValidation.Invalid)
        val errores = (resultado as WorksiteValidation.Invalid).errors
        assertFalse(errores.name.isBlank())
        assertFalse(errores.hayErrores.not())
    }

    @Test
    fun `la direccion demasiado corta se rechaza`() {
        val resultado = WorksiteValidator.validate(formulario(address = "abc"))

        assertTrue(resultado is WorksiteValidation.Invalid)
        val errores = (resultado as WorksiteValidation.Invalid).errors
        assertFalse(errores.address.isBlank())
    }

    @Test
    fun `el radio fuera de rango se rechaza`() {
        val pequeno = WorksiteValidator.validate(formulario(radiusMeters = "10"))
        val grande = WorksiteValidator.validate(formulario(radiusMeters = "1200"))

        assertTrue(pequeno is WorksiteValidation.Invalid)
        assertTrue(grande is WorksiteValidation.Invalid)
    }

    @Test
    fun `el radio no numerico se rechaza`() {
        val resultado = WorksiteValidator.validate(formulario(radiusMeters = "trescientos"))

        assertTrue(resultado is WorksiteValidation.Invalid)
        val errores = (resultado as WorksiteValidation.Invalid).errors
        assertFalse(errores.radiusMeters.isBlank())
    }

    @Test
    fun `la latitud fuera de rango se rechaza`() {
        val resultado = WorksiteValidator.validate(formulario(latitude = "95"))

        assertTrue(resultado is WorksiteValidation.Invalid)
        val errores = (resultado as WorksiteValidation.Invalid).errors
        assertFalse(errores.latitude.isBlank())
    }

    @Test
    fun `la longitud fuera de rango se rechaza`() {
        val resultado = WorksiteValidator.validate(formulario(longitude = "-181"))

        assertTrue(resultado is WorksiteValidation.Invalid)
        val errores = (resultado as WorksiteValidation.Invalid).errors
        assertFalse(errores.longitude.isBlank())
    }

    @Test
    fun `acepta coma decimal en las coordenadas`() {
        val resultado = WorksiteValidator.validate(
            formulario(latitude = "-33,35", longitude = "-70,52")
        )

        assertTrue(resultado is WorksiteValidation.Valid)
        val datos = (resultado as WorksiteValidation.Valid).data
        assertEquals(-33.35, datos.latitude, 0.0001)
        assertEquals(-70.52, datos.longitude, 0.0001)
    }

    private fun formulario(
        name: String = "Faena Andes",
        address: String = "Camino a Farellones km 12",
        radiusMeters: String = "300",
        latitude: String = "-33.35",
        longitude: String = "-70.52"
    ) = WorksiteInput(
        name = name,
        address = address,
        radiusMeters = radiusMeters,
        latitude = latitude,
        longitude = longitude
    )
}
