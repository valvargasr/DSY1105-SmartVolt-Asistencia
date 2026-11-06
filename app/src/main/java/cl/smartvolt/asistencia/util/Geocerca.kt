package cl.smartvolt.asistencia.util

import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

// fórmula de haversine, sirve para saber si el trabajador está dentro del radio
object Geocerca {

    private const val RADIO_TIERRA_METROS = 6_371_000.0

    // Calcula la distancia en metros entre dos coordenadas
    fun distanciaMetros(
        latitudA: Double,
        longitudA: Double,
        latitudB: Double,
        longitudB: Double
    ): Double {
        val deltaLatitud = Math.toRadians(latitudB - latitudA)
        val deltaLongitud = Math.toRadians(longitudB - longitudA)
        val a = sin(deltaLatitud / 2).pow(2) +
            cos(Math.toRadians(latitudA)) *
            cos(Math.toRadians(latitudB)) *
            sin(deltaLongitud / 2).pow(2)
        return 2 * RADIO_TIERRA_METROS * asin(sqrt(a))
    }
}
