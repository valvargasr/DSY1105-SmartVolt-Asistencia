package cl.smartvolt.asistencia.domain.model

/**
 * Faena / frente de trabajo (datos ficticios para la demo).
 */
data class Worksite(
    val siteId: String,
    val name: String,
    val perimeterVersion: Int = 1,
    val active: Boolean = true
)
