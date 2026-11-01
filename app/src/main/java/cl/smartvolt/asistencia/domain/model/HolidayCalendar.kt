package cl.smartvolt.asistencia.domain.model

import java.time.LocalDate

/**
 * Proveedor de calendario de feriados para Chile.
 *
 * Es una lista en memoria para la demostración. La fuente oficial y el
 * territorio deben confirmarse con la contraparte (DP-06). La lista debe
 * actualizarse por año y no quedar limitada a 2026.
 *
 * No se codifica como política empresarial aprobada: es configuración de
 * prueba claramente etiquetada.
 */
object HolidayCalendar {

    private val holidays2026: Map<LocalDate, HolidayCalendarEntry> = mapOf(
        LocalDate.of(2026, 1, 1) to HolidayCalendarEntry(
            LocalDate.of(2026, 1, 1), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 4, 3) to HolidayCalendarEntry(
            LocalDate.of(2026, 4, 3), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 4, 4) to HolidayCalendarEntry(
            LocalDate.of(2026, 4, 4), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 5, 1) to HolidayCalendarEntry(
            LocalDate.of(2026, 5, 1), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 5, 21) to HolidayCalendarEntry(
            LocalDate.of(2026, 5, 21), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 6, 29) to HolidayCalendarEntry(
            LocalDate.of(2026, 6, 29), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 7, 16) to HolidayCalendarEntry(
            LocalDate.of(2026, 7, 16), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 8, 15) to HolidayCalendarEntry(
            LocalDate.of(2026, 8, 15), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 9, 18) to HolidayCalendarEntry(
            LocalDate.of(2026, 9, 18), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 9, 19) to HolidayCalendarEntry(
            LocalDate.of(2026, 9, 19), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 10, 12) to HolidayCalendarEntry(
            LocalDate.of(2026, 10, 12), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 10, 31) to HolidayCalendarEntry(
            LocalDate.of(2026, 10, 31), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 11, 1) to HolidayCalendarEntry(
            LocalDate.of(2026, 11, 1), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 12, 8) to HolidayCalendarEntry(
            LocalDate.of(2026, 12, 8), "CL", HolidayType.NATIONAL_HOLIDAY
        ),
        LocalDate.of(2026, 12, 25) to HolidayCalendarEntry(
            LocalDate.of(2026, 12, 25), "CL", HolidayType.NATIONAL_HOLIDAY
        )
    )

    /**
     * Devuelve la entrada de calendario para la fecha dada, o null si no hay
     * feriado registrado (se asume día laboral).
     */
    fun getEntry(date: LocalDate): HolidayCalendarEntry? = holidays2026[date]

    /**
     * Indica si la fecha es feriado (no laborable) según la lista de prueba.
     */
    fun isHoliday(date: LocalDate): Boolean =
        holidays2026[date]?.type == HolidayType.NATIONAL_HOLIDAY ||
            holidays2026[date]?.type == HolidayType.REGIONAL_HOLIDAY

    /**
     * Determina si se debe generar ausencia: solo si hay una jornada programada
     * y la fecha no es feriado sin jornada especial.
     */
    fun shouldGenerateAbsence(date: LocalDate, hasScheduledShift: Boolean): Boolean {
        if (!hasScheduledShift) return false
        if (isHoliday(date)) {
            // Si es feriado, solo se genera ausencia si hay jornada especial programada.
            // En esta demo, asumimos que no hay jornada especial salvo configuración.
            return false
        }
        return true
    }
}
