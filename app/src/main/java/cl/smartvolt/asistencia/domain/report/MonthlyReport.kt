package cl.smartvolt.asistencia.domain.report

import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.domain.model.OvertimeApproval
import cl.smartvolt.asistencia.domain.model.OvertimeStatus
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

/**
 * Un día del registro mensual.
 *
 * [openAtEnd] queda en true cuando hubo una entrada sin su salida: esas horas
 * no se suman porque no hay forma de saber cuándo terminó la jornada.
 */
data class ReportDay(
    val date: LocalDate,
    val firstCheckInMillis: Long?,
    val lastCheckOutMillis: Long?,
    val attendanceMinutes: Long,
    val overtimeMinutes: Long,
    val openAtEnd: Boolean
) {
    val withoutCheckOut: Boolean get() = openAtEnd
    val totalMinutes: Long get() = attendanceMinutes + overtimeMinutes
}

data class ReportWorker(
    val workerId: String,
    val workerName: String,
    val days: List<ReportDay>,
    val pendingRequests: Int
) {
    val daysWorked: Int get() = days.size
    val attendanceMinutes: Long get() = days.sumOf { it.attendanceMinutes }
    val overtimeMinutes: Long get() = days.sumOf { it.overtimeMinutes }
    val totalMinutes: Long get() = attendanceMinutes + overtimeMinutes
    val daysWithoutCheckOut: Int get() = days.count { it.withoutCheckOut }
}

data class MonthlyReport(
    val month: YearMonth,
    val workers: List<ReportWorker>
) {
    val totalWorkers: Int get() = workers.size
    val totalDays: Int get() = workers.sumOf { it.daysWorked }
    val totalAttendanceMinutes: Long get() = workers.sumOf { it.attendanceMinutes }
    val totalOvertimeMinutes: Long get() = workers.sumOf { it.overtimeMinutes }
    val totalMinutes: Long get() = totalAttendanceMinutes + totalOvertimeMinutes
    val totalPending: Int get() = workers.sumOf { it.pendingRequests }
    val workersWithoutCheckOut: Int get() = workers.count { it.daysWithoutCheckOut > 0 }
    val isEmpty: Boolean get() = workers.isEmpty()
}

/**
 * Cálculo del registro mensual.
 *
 * Une las marcas de asistencia en pares de entrada y salida, y suma las horas
 * extra aprobadas por el administrador. Las solicitudes pendientes o
 * rechazadas se cuentan pero no suman horas.
 */
object MonthlyReportCalculator {

    fun build(
        events: List<LocalAttendanceEvent>,
        approvals: List<OvertimeApproval>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
        nameOf: (String) -> String
    ): MonthlyReport {
        val eventosDelMes = events
            .filter { YearMonth.from(Instant.ofEpochMilli(it.timestampMillis).atZone(zone)) == month }
            .groupBy { it.workerId }

        val aprobadasDelMes = approvals
            .filter { YearMonth.from(it.date) == month && it.status == OvertimeStatus.APPROVED }
            .groupBy { it.workerId }

        val pendientesDelMes = approvals
            .filter { YearMonth.from(it.date) == month && it.status == OvertimeStatus.PENDING }
            .groupBy { it.workerId }

        val ids = (eventosDelMes.keys + aprobadasDelMes.keys + pendientesDelMes.keys)
            .toSortedSet()

        val trabajadores = ids.map { workerId ->
            val dias = diasDelTrabajador(
                eventos = eventosDelMes[workerId].orEmpty(),
                aprobadas = aprobadasDelMes[workerId].orEmpty(),
                zone = zone
            )
            ReportWorker(
                workerId = workerId,
                workerName = nameOf(workerId),
                days = dias,
                pendingRequests = pendientesDelMes[workerId].orEmpty().size
            )
        }.sortedBy { it.workerName }

        return MonthlyReport(month = month, workers = trabajadores)
    }

    private fun diasDelTrabajador(
        eventos: List<LocalAttendanceEvent>,
        aprobadas: List<OvertimeApproval>,
        zone: ZoneId
    ): List<ReportDay> {
        val eventosPorDia = eventos.groupBy {
            Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate()
        }
        val extraPorDia = aprobadas.groupBy { it.date }

        val fechas = (eventosPorDia.keys + extraPorDia.keys).toSortedSet()

        return fechas.map { fecha ->
            val delDia = eventosPorDia[fecha].orEmpty().sortedBy { it.timestampMillis }
            var abierta: Long? = null
            var primeraEntrada: Long? = null
            var ultimaSalida: Long? = null
            var minutos = 0L

            for (evento in delDia) {
                when (evento.type) {
                    LocalAttendanceEvent.TYPE_CHECK_IN -> {
                        if (abierta == null) {
                            abierta = evento.timestampMillis
                            if (primeraEntrada == null) primeraEntrada = evento.timestampMillis
                        }
                    }

                    LocalAttendanceEvent.TYPE_CHECK_OUT -> {
                        val entrada = abierta
                        if (entrada != null) {
                            minutos += ((evento.timestampMillis - entrada) / 60_000L)
                                .coerceAtLeast(0)
                            abierta = null
                            ultimaSalida = evento.timestampMillis
                        }
                    }
                }
            }

            val extra = extraPorDia[fecha].orEmpty().sumOf {
                ((it.authorizedEndMillis - it.authorizedStartMillis) / 60_000L)
                    .coerceAtLeast(0)
            }

            ReportDay(
                date = fecha,
                firstCheckInMillis = primeraEntrada,
                lastCheckOutMillis = ultimaSalida,
                attendanceMinutes = minutos,
                overtimeMinutes = extra,
                openAtEnd = abierta != null
            )
        }
    }
}

fun formatDuration(minutes: Long): String {
    val horas = minutes / 60
    val resto = minutes % 60
    return when {
        hoursAreZero(horas, resto) -> "0 h"
        resto == 0L -> "$horas h"
        else -> "$horas h $resto min"
    }
}

private fun hoursAreZero(horas: Long, resto: Long): Boolean = horas == 0L && resto == 0L

fun formatDecimalHours(minutes: Long): String =
    String.format(Locale.US, "%.2f", minutes / 60.0).replace('.', ',')
