package cl.smartvolt.asistencia.domain.model

/**
 * Perfil de trabajador (datos anonimizados, sin RUT/nombre real).
 */
data class WorkerProfile(
    val workerId: String,
    val role: WorkerRole,
    val assignedSiteId: String,
    val deviceBindingId: String?,
    val active: Boolean = true
)

enum class WorkerRole(val label: String) {
    WORKER("Operario"),
    SUPERVISOR("Supervisor"),
    ADMIN("Administrador"),
    HR("Administración/RR.HH."),
    MANAGEMENT("Gerencia");

    companion object {
        fun fromCode(code: String): WorkerRole =
            entries.firstOrNull { it.name == code } ?: WORKER
    }
}
