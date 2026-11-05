package cl.smartvolt.asistencia.feature.overtime

import java.time.LocalDate
import java.time.LocalTime

/** Errores por campo del formulario de solicitud de horas extra. */
data class OvertimeFormErrors(
    val date: String = "",
    val start: String = "",
    val end: String = "",
    val reason: String = ""
) {
    val isValid: Boolean
        get() = date.isEmpty() && start.isEmpty() && end.isEmpty() && reason.isEmpty()
}

/**
 * Validaciones puras del formulario para mantener las reglas fuera de Compose.
 * Los horarios se ingresan en formato de 24 horas HH:mm.
 */
object OvertimeFormValidator {

    private val timePattern = Regex("^(?:[01]\\d|2[0-3]):[0-5]\\d$")

    fun validate(
        date: LocalDate?,
        startText: String,
        endText: String,
        reasonText: String
    ): OvertimeFormErrors {
        val dateError = if (date == null) "Selecciona una fecha." else ""

        val start = startText.trim()
        val end = endText.trim()

        val startError = when {
            start.isEmpty() -> "Ingresa la hora de inicio."
            !timePattern.matches(start) -> "Usa el formato HH:mm, por ejemplo 18:00."
            else -> ""
        }

        val endError = when {
            end.isEmpty() -> "Ingresa la hora de término."
            !timePattern.matches(end) -> "Usa el formato HH:mm, por ejemplo 20:00."
            startError.isEmpty() && LocalTime.parse(end).isNotAfter(LocalTime.parse(start)) ->
                "La hora de término debe ser posterior al inicio."
            else -> ""
        }

        val reasonError = if (reasonText.isBlank()) {
            "Describe el motivo de la solicitud."
        } else {
            ""
        }

        return OvertimeFormErrors(
            date = dateError,
            start = startError,
            end = endError,
            reason = reasonError
        )
    }

    private fun LocalTime.isNotAfter(other: LocalTime): Boolean = !isAfter(other)
}
