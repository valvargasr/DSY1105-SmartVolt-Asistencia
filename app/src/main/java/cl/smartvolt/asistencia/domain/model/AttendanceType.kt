package cl.smartvolt.asistencia.domain.model

/**
 * Tipo de marca de asistencia.
 */
enum class AttendanceType(val code: String) {
    CHECK_IN("CHECK_IN"),
    CHECK_OUT("CHECK_OUT");

    companion object {
        fun fromCode(code: String): AttendanceType? =
            entries.firstOrNull { it.code == code }
    }
}
