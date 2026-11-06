package cl.smartvolt.asistencia.domain.validation

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Distancia entre dos coordenadas sobre la superficie terrestre.
 *
 * Usa la fórmula de haversine con el radio medio de la Tierra. Es suficiente
 * para decidir si una marca cae dentro de una faena de pocos cientos de metros
 * y no depende de servicios externos.
 */
object GeoDistance {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun metersBetween(
        latitudeA: Double,
        longitudeA: Double,
        latitudeB: Double,
        longitudeB: Double
    ): Double {
        val deltaLatitude = Math.toRadians(latitudeB - latitudeA)
        val deltaLongitude = Math.toRadians(longitudeB - longitudeA)
        val a = sin(deltaLatitude / 2).pow(2) +
            cos(Math.toRadians(latitudeA)) *
            cos(Math.toRadians(latitudeB)) *
            sin(deltaLongitude / 2).pow(2)
        return 2 * EARTH_RADIUS_METERS * asin(sqrt(a))
    }
}
