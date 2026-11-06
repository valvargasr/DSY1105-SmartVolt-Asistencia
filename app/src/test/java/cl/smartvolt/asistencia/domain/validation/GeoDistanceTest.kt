package cl.smartvolt.asistencia.domain.validation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class GeoDistanceTest {

    @Test
    fun `el mismo punto da cero metros`() {
        val distancia = GeoDistance.metersBetween(-33.4489, -70.6693, -33.4489, -70.6693)

        assertEquals(0.0, distancia, 0.001)
    }

    @Test
    fun `santiago y valparaiso quedan a menos de cien kilometros`() {
        val distancia = GeoDistance.metersBetween(-33.4489, -70.6693, -33.0472, -71.6127)

        assertTrue("Distancia inesperada: $distancia", distancia in 95_000.0..102_000.0)
    }

    @Test
    fun `la distancia es simetrica`() {
        val ida = GeoDistance.metersBetween(-33.4489, -70.6693, -33.0472, -71.6127)
        val vuelta = GeoDistance.metersBetween(-33.0472, -71.6127, -33.4489, -70.6693)

        assertEquals(ida, vuelta, 0.5)
    }

    @Test
    fun `un grado de latitud ronda los ciento once kilometros`() {
        val distancia = GeoDistance.metersBetween(0.0, 0.0, 1.0, 0.0)

        assertTrue("Distancia inesperada: $distancia", distancia in 111_000.0..111_300.0)
    }

    @Test
    fun `mil metros al sur se distinguen del centro`() {
        val distancia = GeoDistance.metersBetween(-33.4489, -70.6693, -33.4579, -70.6693)

        assertTrue("Distancia inesperada: $distancia", distancia in 990.0..1_010.0)
    }
}
