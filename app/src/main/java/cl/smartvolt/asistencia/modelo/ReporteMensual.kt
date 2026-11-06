package cl.smartvolt.asistencia.modelo

import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.Locale

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

object MonthlyReportCalculator {

    fun build(
        events: List<Asistencia>,
        approvals: List<SolicitudHoraExtra>,
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
            .filter { YearMonth.from(it.fecha) == month && it.estado == EstadoSolicitud.APROBADA }
            .groupBy { it.workerId }

        val pendientesDelMes = approvals
            .filter { YearMonth.from(it.fecha) == month && it.estado == EstadoSolicitud.PENDIENTE }
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
        eventos: List<Asistencia>,
        aprobadas: List<SolicitudHoraExtra>,
        zone: ZoneId,
        hourlyRateClp: Long,
        overtimeSurchargePercent: Int
    ): List<ReportDay> {
        val eventosPorDia = eventos.groupBy {
            Instant.ofEpochMilli(it.timestampMillis).atZone(zone).toLocalDate()
        }
        val extraPorDia = aprobadas.groupBy { it.fecha }
        val fechas = (eventosPorDia.keys + extraPorDia.keys).toSortedSet()

        return fechas.map { fecha ->
            val delDia = eventosPorDia[fecha].orEmpty().sortedBy { it.timestampMillis }
            var abierta: Long? = null
            var primeraEntrada: Long? = null
            var ultimaSalida: Long? = null
            var minutos = 0L

            for (evento in delDia) {
                when (evento.type) {
                    Asistencia.TYPE_CHECK_IN -> {
                        if (abierta == null) {
                            abierta = evento.timestampMillis
                            if (primeraEntrada == null) primeraEntrada = evento.timestampMillis
                        }
                    }

                    Asistencia.TYPE_CHECK_OUT -> {
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
        horas == 0L && resto == 0L -> "0 h"
        resto == 0L -> "$horas h"
        else -> "$horas h $resto min"
    }
}

fun formatDecimalHours(minutes: Long): String =
    String.format(Locale.US, "%.2f", minutes / 60.0).replace('.', ',')

fun formatClp(amount: Long): String =
    "$" + String.format(Locale.US, "%,d", amount).replace(',', '.')
