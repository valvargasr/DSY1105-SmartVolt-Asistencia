package cl.smartvolt.asistencia.domain.model

/**
 * Faena o frente de trabajo.
 *
 * El centro y el radio definen la zona donde se puede marcar asistencia.
 * El administrador edita estos datos desde la app; el resto de los roles
 * solo los consulta.
 */
data class Worksite(
    val siteId: String,
    val name: String,
    val address: String,
    val latitude: Double,
    val longitude: Double,
    val radiusMeters: Int,
    val perimeterVersion: Int = 1,
    val active: Boolean = true
)
