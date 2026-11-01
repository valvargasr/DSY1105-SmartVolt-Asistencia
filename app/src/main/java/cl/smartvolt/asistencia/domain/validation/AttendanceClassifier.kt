package cl.smartvolt.asistencia.domain.validation

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.domain.model.DailyAttendanceStatus
import java.time.Instant
import java.time.ZoneId

/**
 * Clasificador de puntualidad y ausencia.
 *
 * Los parámetros (hora de inicio del turno, tolerancia, momento de cierre)
 * NO están definidos en las fuentes (DP-05). Por eso se reciben como
 * argumentos configurables en lugar de constantes codificadas.
 *
 * Esta clase es Kotlin puro para poder probarla con tests unitarios.
 */
object AttendanceClassifier {

    /**
     * Configuración de un turno para clasificar asistencia.
     */
    data class ShiftConfig(
        val shiftStartMillis: Long,
        val toleranceMillis: Long,
        val absenceCutoffMillis: Long
    )

    /**
     * Clasifica la asistencia de un trabajador dado su check-in y la config del turno.
     *
     * @param checkIn evento de entrada (null si no hay marca)
     * @param config configuración del turno (horarios, tolerancia, corte)
     * @param hasScheduledShift si el trabajador tenía turno programado ese día
     * @param isHoliday si la fecha es feriado
     * @return estado de asistencia clasificado
     */
    fun classify(
        checkIn: LocalAttendanceEvent?,
        config: ShiftConfig?,
        hasScheduledShift: Boolean,
        isHoliday: Boolean
    ): DailyAttendanceStatus {
        // Sin turno programado, no se clasifica como ausente (RN-06).
        if (!hasScheduledShift) {
            return DailyAttendanceStatus.PENDING
        }

        // Feriado sin jornada especial: no se genera ausencia (HU-11).
        if (isHoliday) {
            return DailyAttendanceStatus.PENDING
        }

        if (checkIn == null) {
            // Sin marca: pendiente hasta que venza la ventana de llegada.
            return DailyAttendanceStatus.PENDING
        }

        if (config == null) {
            // Sin configuración de turno aprobada: no se puede clasificar.
            return DailyAttendanceStatus.PENDING
        }

        val eventTime = checkIn.timestampMillis

        return when {
            eventTime <= config.shiftStartMillis + config.toleranceMillis ->
                DailyAttendanceStatus.PRESENT_ON_TIME
            eventTime < config.absenceCutoffMillis ->
                DailyAttendanceStatus.PRESENT_LATE
            else ->
                DailyAttendanceStatus.ABSENT
        }
    }

    /**
     * Determina si una fecha/hora ya superó el corte para declarar ausencia.
     */
    fun isPastAbsenceCutoff(nowMillis: Long, config: ShiftConfig?): Boolean {
        if (config == null) return false
        return nowMillis >= config.absenceCutoffMillis
    }

    /**
     * Formatea un timestamp a hora legible (HH:mm) en zona local.
     */
    fun formatTime(millis: Long, zoneId: ZoneId = ZoneId.systemDefault()): String {
        val instant = Instant.ofEpochMilli(millis)
        val time = instant.atZone(zoneId).toLocalTime()
        return String.format("%02d:%02d", time.hour, time.minute)
    }
}
