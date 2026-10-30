package cl.smartvolt.asistencia.native.location

/**
 * Fuente de ubicación del dispositivo.
 *
 * La interfaz permite reemplazar la implementación real por una de prueba en
 * los tests unitarios, sin arrastrar dependencias de Android.
 */
interface LocationProvider {

    /** Indica si la app ya tiene permiso de ubicación. */
    fun hasPermission(): Boolean

    /** Indica si el servicio de ubicación del teléfono está encendido. */
    fun isLocationServiceEnabled(): Boolean

    /**
     * Intenta obtener una lectura de ubicación.
     *
     * Nunca lanza excepciones: todos los fallos se devuelven como un
     * [LocationResult] con el [LocationResult.status] correspondiente.
     */
    suspend fun getCurrentLocation(): LocationResult
}
