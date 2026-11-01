package cl.smartvolt.asistencia.domain.model

/**
 * Estado de una solicitud de horas extra.
 */
enum class OvertimeStatus(val label: String) {
    PENDING("Pendiente"),
    APPROVED("Aprobada"),
    REJECTED("Rechazada"),
    CANCELLED("Cancelada");

    companion object {
        fun fromCode(code: String): OvertimeStatus =
            entries.firstOrNull { it.name == code } ?: PENDING
    }
}
