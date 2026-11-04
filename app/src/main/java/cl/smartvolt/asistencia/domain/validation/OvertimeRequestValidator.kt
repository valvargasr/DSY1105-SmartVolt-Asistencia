package cl.smartvolt.asistencia.domain.validation

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.time.format.ResolverStyle

/**
 * Entrada del formulario de solicitud de horas extra del trabajador.
 *
 * Los valores llegan como texto porque provienen de campos de la interfaz. La
 * validación y la conversión viven aquí, no en el composable (IE 2.2.1).
 */
data class OvertimeRequestInput(
    val fecha: String = "",
    val desde: String = "",
    val hasta: String = "",
    val motivo: String = ""
)

/** Errores por campo, listos para mostrarse junto a cada control. */
data class OvertimeRequestErrors(
    val fecha: String = "",
    val desde: String = "",
    val hasta: String = "",
    val motivo: String = ""
) {
    val hayErrores: Boolean
        get() = fecha.isNotEmpty() || desde.isNotEmpty() ||
            hasta.isNotEmpty() || motivo.isNotEmpty()
}

/** Resultado de una validación correcta, ya convertido a instantes. */
data class OvertimeRequestValidated(
    val date: LocalDate,
    val startMillis: Long,
    val endMillis: Long,
    val reason: String
)

sealed interface OvertimeRequestResult {
    data class Valid(val data: OvertimeRequestValidated) : OvertimeRequestResult
    data class Invalid(val errors: OvertimeRequestErrors) : OvertimeRequestResult
}

/**
 * Reglas del formulario de horas extra.
 *
 * Solo se validan reglas objetivas del propio formulario: formato de fecha y
 * hora, que la hora de término sea posterior a la de inicio y que el motivo
 * explique la solicitud. No se inventan topes de duración ni recargos: esas
 * reglas dependen de la empresa y siguen pendientes de acuerdo.
 */
object OvertimeRequestValidator {

    const val MOTIVO_MINIMO = 5

    private val FORMATO_FECHA: DateTimeFormatter = DateTimeFormatter
        .ofPattern("d/M/uuuu")
        .withResolverStyle(ResolverStyle.STRICT)

    private val FORMATO_HORA: DateTimeFormatter = DateTimeFormatter
        .ofPattern("H:mm")
        .withResolverStyle(ResolverStyle.STRICT)

    fun validate(input: OvertimeRequestInput): OvertimeRequestResult {
        val fecha = parseFecha(input.fecha)
        val desde = parseHora(input.desde)
        val hasta = parseHora(input.hasta)
        val motivo = input.motivo.trim()

        val errorFecha = when {
            input.fecha.isBlank() -> "Ingresa la fecha de las horas extra."
            fecha == null -> "Usa el formato dd/mm/aaaa."
            else -> ""
        }
        val errorDesde = when {
            input.desde.isBlank() -> "Ingresa la hora de inicio."
            desde == null -> "Usa el formato hh:mm."
            else -> ""
        }
        val errorHasta = when {
            input.hasta.isBlank() -> "Ingresa la hora de término."
            hasta == null -> "Usa el formato hh:mm."
            else -> ""
        }
        val errorMotivo = when {
            motivo.isBlank() -> "Explica el motivo de las horas extra."
            motivo.length < MOTIVO_MINIMO ->
                "El motivo debe tener al menos $MOTIVO_MINIMO caracteres."
            else -> ""
        }

        val horarioInvalido = fecha != null && desde != null && hasta != null &&
            !hasta.isAfter(desde)

        if (errorFecha.isNotEmpty() || errorDesde.isNotEmpty() ||
            errorHasta.isNotEmpty() || errorMotivo.isNotEmpty() || horarioInvalido
        ) {
            return OvertimeRequestResult.Invalid(
                OvertimeRequestErrors(
                    fecha = errorFecha,
                    desde = errorDesde,
                    hasta = if (horarioInvalido) {
                        "La hora de término debe ser posterior a la de inicio."
                    } else {
                        errorHasta
                    },
                    motivo = errorMotivo
                )
            )
        }

        val zona = ZoneId.systemDefault()
        val fechaValida = fecha ?: return OvertimeRequestResult.Invalid(
            OvertimeRequestErrors(fecha = "Ingresa la fecha de las horas extra.")
        )
        val desdeValida = desde ?: return OvertimeRequestResult.Invalid(
            OvertimeRequestErrors(desde = "Ingresa la hora de inicio.")
        )
        val hastaValida = hasta ?: return OvertimeRequestResult.Invalid(
            OvertimeRequestErrors(hasta = "Ingresa la hora de término.")
        )

        return OvertimeRequestResult.Valid(
            OvertimeRequestValidated(
                date = fechaValida,
                startMillis = fechaValida.atTime(desdeValida).atZone(zona).toInstant().toEpochMilli(),
                endMillis = fechaValida.atTime(hastaValida).atZone(zona).toInstant().toEpochMilli(),
                reason = motivo
            )
        )
    }

    private fun parseFecha(texto: String): LocalDate? = try {
        LocalDate.parse(texto.trim(), FORMATO_FECHA)
    } catch (_: DateTimeParseException) {
        null
    }

    private fun parseHora(texto: String): LocalTime? = try {
        LocalTime.parse(texto.trim(), FORMATO_HORA)
    } catch (_: DateTimeParseException) {
        null
    }
}
