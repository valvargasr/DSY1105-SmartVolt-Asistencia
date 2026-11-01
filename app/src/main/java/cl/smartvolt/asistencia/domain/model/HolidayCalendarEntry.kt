package cl.smartvolt.asistencia.domain.model

import java.time.LocalDate

/**
 * Entrada de calendario laboral (feriados y jornadas especiales).
 *
 * La fuente y el territorio deben confirmarse con la contraparte (DP-06).
 */
data class HolidayCalendarEntry(
    val date: LocalDate,
    val territory: String,
    val type: HolidayType,
    val sourceVersion: String = "demo-2026"
)

enum class HolidayType(val label: String) {
    NATIONAL_HOLIDAY("Feriado nacional"),
    REGIONAL_HOLIDAY("Feriado regional"),
    SPECIAL_WORKDAY("Jornada especial"),
    REGULAR_WORKDAY("Día laboral")
}
