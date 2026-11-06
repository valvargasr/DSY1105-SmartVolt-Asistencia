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
 * Los montos son una estimación con la tarifa de la faena, no una liquidación.
 */
data class ReportDay(
    val date: LocalDate,
    val firstCheckInMillis: Long?,
    val lastCheckOutMillis: Long?,
    val attendanceMinutes: Long,
    val overtimeMinutes: Long,
    val openAtEnd: Boolean,
    val hourlyRateClp: Long = DEFAULT_HOURLY_RATE_CLP,
    val overtimeSurchargePercent: Int = DEFAULT_OVERTIME_SURCHARGE
) {
    val withoutCheckOut: Boolean get() = openAtEnd
    val totalMinutes: Long get() = attendanceMinutes + overtimeMinutes
    val attendanceAmountClp: Long
        get() = amountForMinutes(attendanceMinutes, hourlyRateClp)
    val overtimeAmountClp: Long
        get() = amountForMinutes(overtimeMinutes, hourlyRateClp, overtimeSurchargePercent)
    val totalAmountClp: Long get() = attendanceAmountClp + overtimeAmountClp
}

data class ReportWorker(
    val workerId: String,
    val workerName: String,
    val days: List<ReportDay>,
    val pendingRequests: Int,
    val hourlyRateClp: Long = DEFAULT_HOURLY_RATE_CLP,
    val overtimeSurchargePercent: Int = DEFAULT_OVERTIME_SURCHARGE
) {
    val daysWorked: Int get() = days.size
    val attendanceMinutes: Long get() = days.sumOf { it.attendanceMinutes }
    val overtimeMinutes: Long get() = days.sumOf { it.overtimeMinutes }
    val totalMinutes: Long get() = attendanceMinutes + overtimeMinutes
    val daysWithoutCheckOut: Int get() = days.count { it.withoutCheckOut }
    val attendanceAmountClp: Long
        get() = amountForMinutes(attendanceMinutes, hourlyRateClp)
    val overtimeAmountClp: Long
        get() = amountForMinutes(overtimeMinutes, hourlyRateClp, overtimeSurchargePercent)
    val totalAmountClp: Long get() = attendanceAmountClp + overtimeAmountClp
}

data class MonthlyReport(
    val month: YearMonth,
    val workers: List<ReportWorker>,
    val hourlyRateClp: Long = DEFAULT_HOURLY_RATE_CLP,
    val overtimeSurchargePercent: Int = DEFAULT_OVERTIME_SURCHARGE
) {
    val totalWorkers: Int get() = workers.size
    val totalDays: Int get() = workers.sumOf { it.daysWorked }
    val totalAttendanceMinutes: Long get() = workers.sumOf { it.attendanceMinutes }
    val totalOvertimeMinutes: Long get() = workers.sumOf { it.overtimeMinutes }
    val totalMinutes: Long get() = totalAttendanceMinutes + totalOvertimeMinutes
    val totalPending: Int get() = workers.sumOf { it.pendingRequests }
    val workersWithoutCheckOut: Int get() = workers.count { it.daysWithoutCheckOut > 0 }
    val totalAttendanceAmountClp: Long get() = workers.sumOf { it.attendanceAmountClp }
    val totalOvertimeAmountClp: Long get() = workers.sumOf { it.overtimeAmountClp }
    val totalAmountClp: Long get() = workers.sumOf { it.totalAmountClp }
    val isEmpty: Boolean get() = workers.isEmpty()
}

/**
 * Cálculo del registro mensual.
 *
 * Une las marcas de asistencia en pares de entrada y salida, y suma las horas
 * extra aprobadas por el administrador. Las solicitudes pendientes o
 * rechazadas se cuentan pero no suman horas.
 *
 * El monto estimado usa la tarifa por hora de la faena y el recargo de horas
 * extra. La asistencia se paga a la tarifa normal y cada hora extra a la
 * tarifa con el recargo aplicado.
 */
object MonthlyReportCalculator {

    fun build(
        events: List<LocalAttendanceEvent>,
        approvals: List<OvertimeApproval>,
        month: YearMonth,
        zone: ZoneId = ZoneId.systemDefault(),
        hourlyRateClp: Long = DEFAULT_HOURLY_RATE_CLP,
        overtimeSurchargePercent: Int = DEFAULT_OVERTIME_SURCHARGE,
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
                zone = zone,
                hourlyRateClp = hourlyRateClp,
                overtimeSurchargePercent = overtimeSurchargePercent
            )
            ReportWorker(
                workerId = workerId,
                workerName = nameOf(workerId),
                days = dias,
                pendingRequests = pendientesDelMes[workerId].orEmpty().size,
                hourlyRateClp = hourlyRateClp,
                overtimeSurchargePercent = overtimeSurchargePercent
            )
        }.sortedBy { it.workerName }

        return MonthlyReport(
            month = month,
            workers = trabajadores,
            hourlyRateClp = hourlyRateClp,
            overtimeSurchargePercent = overtimeSurchargePercent
        )
    }

    private fun diasDelTrabajador(
        eventos: List<LocalAttendanceEvent>,
        aprobadas: List<OvertimeApproval>,
        zone: ZoneId,
        hourlyRateClp: Long,
        overtimeSurchargePercent: Int
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
                openAtEnd = abierta != null,
                hourlyRateClp = hourlyRateClp,
                overtimeSurchargePercent = overtimeSurchargePercent
            )
        }
    }
}

const val DEFAULT_HOURLY_RATE_CLP: Long = 7000
const val DEFAULT_OVERTIME_SURCHARGE: Int = 50

/**
 * Monto estimado de una cantidad de minutos a la tarifa indicada.
 *
 * Cuando [surchargePercent] es cero se paga la hora normal. La hora extra usa
 * la misma tarifa con el recargo aplicado.
 */
fun amountForMinutes(
    minutes: Long,
    hourlyRateClp: Long,
    surchargePercent: Int = 0
): Long {
    if (minutes <= 0 || hourlyRateClp <= 0) return 0
    val factor = 100.0 + surchargePercent
    return Math.round(minutes * hourlyRateClp * factor / 6000.0)
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

/** Monto en pesos con separador de miles, por ejemplo $63.000. */
fun formatClp(amount: Long): String =
    "$" + String.format(Locale.US, "%,d", amount).replace(',', '.')
