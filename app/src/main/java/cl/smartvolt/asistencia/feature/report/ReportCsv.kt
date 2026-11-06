package cl.smartvolt.asistencia.feature.report

import cl.smartvolt.asistencia.domain.report.MonthlyReport
import cl.smartvolt.asistencia.domain.report.formatDecimalHours
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Genera el CSV del registro mensual.
 *
 * Se usa punto y coma como separador y coma decimal para que el archivo se
 * abra bien en Excel en español. El BOM inicial evita que las tildes salgan
 * rotas al abrirlo con ese programa.
 */
object ReportCsv {

    fun fileName(month: YearMonth): String = "registro-smartvolt-$month.csv"

    fun content(report: MonthlyReport, zone: ZoneId = ZoneId.systemDefault()): String {
        val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
        val texto = StringBuilder()

        texto.append('\uFEFF')
        texto.append(
            "Fecha;Trabajador;Identificador;Entrada;Salida;" +
                "Horas asistencia;Sin salida;Horas extra aprobadas;Horas totales;" +
                "Monto asistencia CLP;Monto extra CLP;Monto total CLP\n"
        )

        for (trabajador in report.workers) {
            val dias = trabajador.days.sortedBy { it.date }
            for (dia in dias) {
                texto.append(dia.date.toString()).append(';')
                texto.append(limpiar(trabajador.workerName)).append(';')
                texto.append(trabajador.workerId).append(';')
                texto.append(dia.firstCheckInMillis?.let { hora(it, zone, formatoHora) } ?: "").append(';')
                texto.append(dia.lastCheckOutMillis?.let { hora(it, zone, formatoHora) } ?: "").append(';')
                texto.append(formatDecimalHours(dia.attendanceMinutes)).append(';')
                texto.append(if (dia.withoutCheckOut) "Sí" else "No").append(';')
                texto.append(formatDecimalHours(dia.overtimeMinutes)).append(';')
                texto.append(formatDecimalHours(dia.totalMinutes)).append(';')
                texto.append(dia.attendanceAmountClp).append(';')
                texto.append(dia.overtimeAmountClp).append(';')
                texto.append(dia.totalAmountClp).append('\n')
            }
        }

        texto.append("TOTAL;;;;;")
        texto.append(formatDecimalHours(report.totalAttendanceMinutes)).append(';')
        texto.append("").append(';')
        texto.append(formatDecimalHours(report.totalOvertimeMinutes)).append(';')
        texto.append(formatDecimalHours(report.totalMinutes)).append(';')
        texto.append(report.totalAttendanceAmountClp).append(';')
        texto.append(report.totalOvertimeAmountClp).append(';')
        texto.append(report.totalAmountClp).append('\n')

        return texto.toString()
    }

    private fun hora(millis: Long, zone: ZoneId, formato: DateTimeFormatter): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().format(formato)

    private fun limpiar(valor: String): String =
        valor.replace(';', ',').replace('\n', ' ').replace('\r', ' ')
}
