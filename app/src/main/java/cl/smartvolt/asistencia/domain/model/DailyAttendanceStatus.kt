package cl.smartvolt.asistencia.domain.model

/**
 * Estado de asistencia diaria de un trabajador.
 *
 * NO se declara ausente hasta que venza la ventana de llegada aprobada (RN-06).
 */
enum class DailyAttendanceStatus(val label: String) {
    PENDING("Pendiente"),
    PRESENT_ON_TIME("Presente a tiempo"),
    PRESENT_LATE("Presente con atraso"),
    ABSENT("Ausente"),
    CHECK_OUT_REGISTERED("Salida registrada");

    companion object {
        fun fromCode(code: String): DailyAttendanceStatus =
            entries.firstOrNull { it.name == code } ?: PENDING
    }
}
