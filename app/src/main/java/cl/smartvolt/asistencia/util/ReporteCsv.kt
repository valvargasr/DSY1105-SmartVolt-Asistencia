package cl.smartvolt.asistencia.util

import cl.smartvolt.asistencia.modelo.MonthlyReport
import cl.smartvolt.asistencia.modelo.formatDecimalHours
import java.time.Instant
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

// Arma el CSV del reporte mensual
object ReporteCsv {

    // Nombre del archivo CSV
    fun nombreArchivo(month: YearMonth): String = "registro-smartvolt-$month.csv"

    // Genera el contenido del CSV fila por fila
    fun contenido(report: MonthlyReport, zone: ZoneId = ZoneId.systemDefault()): String {
        // armamos el csv a mano porque son pocas columnas, el BOM es para que Excel no rompa
        val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
        val texto = StringBuilder()

        texto.append('\uFEFF')
        texto.append(
            "Fecha;Trabajador;Identificador;Entrada;Salida;" +
                "Horas asistencia;Sin salida;Horas extra aprobadas;Horas totales\n"
        )

        for (trabajador in report.workers) {
            for (dia in trabajador.days.sortedBy { it.date }) {
                texto.append(dia.date.toString()).append(';')
                texto.append(limpiar(trabajador.workerName)).append(';')
                texto.append(trabajador.workerId).append(';')
                texto.append(dia.firstCheckInMillis?.let { hora(it, zone, formatoHora) } ?: "")
                    .append(';')
                texto.append(dia.lastCheckOutMillis?.let { hora(it, zone, formatoHora) } ?: "")
                    .append(';')
                texto.append(formatDecimalHours(dia.attendanceMinutes)).append(';')
                texto.append(if (dia.withoutCheckOut) "Sí" else "No").append(';')
                texto.append(formatDecimalHours(dia.overtimeMinutes)).append(';')
                texto.append(formatDecimalHours(dia.totalMinutes)).append('\n')
            }
        }

        texto.append("TOTAL;;;;;")
        texto.append(formatDecimalHours(report.totalAttendanceMinutes)).append(';')
        texto.append("").append(';')
        texto.append(formatDecimalHours(report.totalOvertimeMinutes)).append(';')
        texto.append(formatDecimalHours(report.totalMinutes)).append('\n')

        return texto.toString()
    }

    // Pasa un instante a la hora local
    private fun hora(millis: Long, zone: ZoneId, formato: DateTimeFormatter): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().format(formato)

    // Quita puntos y comas que rompen el CSV
    private fun limpiar(valor: String): String =
        valor.replace(';', ',').replace('\n', ' ').replace('\r', ' ')
}
