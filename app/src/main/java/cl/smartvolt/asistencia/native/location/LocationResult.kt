package cl.smartvolt.asistencia.native.location

/**
 * Estados posibles de una lectura de ubicación.
 *
 * Cada estado existe para que la interfaz pueda explicar qué falta, en vez de
 * mostrar una ubicación inventada. Ver docs/pruebas-area-c.md.
 */
enum class LocationStatus {
    READY,
    PERMISSION_DENIED,
    LOCATION_DISABLED,
    TIMEOUT,
    INSUFFICIENT_ACCURACY,
    UNAVAILABLE,
    ERROR
}

/**
 * Señal de ubicación simulada (Mock Locations / modo desarrollador).
 *
 * Es una señal de riesgo, no una prueba de fraude: UNKNOWN significa que la
 * versión de Android no permite saberlo.
 */
enum class MockSignal {
    DETECTED,
    NOT_DETECTED,
    UNKNOWN
}

/**
 * Resultado de una lectura de ubicación.
 *
 * Los campos de posición son nulos cuando [status] no es [LocationStatus.READY]:
 * nunca se rellenan con valores de relleno.
 */
data class LocationResult(
    val status: LocationStatus,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val accuracyMeters: Float? = null,
    val capturedAtMillis: Long? = null,
    val mockSignal: MockSignal = MockSignal.UNKNOWN,
    val errorCode: String? = null
)
