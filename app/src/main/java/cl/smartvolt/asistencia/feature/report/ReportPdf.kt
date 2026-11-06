package cl.smartvolt.asistencia.feature.report

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import cl.smartvolt.asistencia.domain.report.MonthlyReport
import cl.smartvolt.asistencia.domain.report.formatClp
import cl.smartvolt.asistencia.domain.report.formatDecimalHours
import cl.smartvolt.asistencia.domain.report.formatDuration
import java.io.ByteArrayOutputStream
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Genera el registro mensual en PDF.
 *
 * Usa PdfDocument de Android, sin dependencias nuevas. La hoja es A4 vertical
 * y la tabla se reparte en varias páginas cuando no cabe.
 */
object ReportPdf {

    private const val ANCHO = 595
    private const val ALTO = 842
    private const val MARGEN = 40f
    private const val LIMITE_INFERIOR = 790f

    fun fileName(month: YearMonth): String = "registro-smartvolt-$month.pdf"

    fun build(
        report: MonthlyReport,
        siteName: String,
        zone: ZoneId = ZoneId.systemDefault(),
        generatedAt: LocalDate = LocalDate.now()
    ): ByteArray {
        val documento = PdfDocument()
        val titulo = pincel(18f, Color.rgb(24, 32, 44), negrita = true)
        val subtitulo = pincel(10f, Color.rgb(90, 100, 112))
        val seccion = pincel(11f, Color.rgb(24, 32, 44), negrita = true)
        val cuerpo = pincel(9f, Color.rgb(45, 55, 68))
        val pequeno = pincel(8f, Color.rgb(110, 118, 128))
        val encabezado = pincel(8.5f, Color.rgb(70, 78, 90), negrita = true)
        val negrita = pincel(9f, Color.rgb(24, 32, 44), negrita = true)
        val linea = Paint().apply {
            color = Color.rgb(214, 218, 224)
            strokeWidth = 0.6f
        }
        val montoDerecha = pincel(9f, Color.rgb(24, 32, 44), negrita = true).apply {
            textAlign = Paint.Align.RIGHT
        }

        val formatoMes = DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("es-CL"))
        val formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy")
        val formatoHora = DateTimeFormatter.ofPattern("HH:mm")
        val etiquetaMes = report.month.atDay(1).format(formatoMes)
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.forLanguageTag("es-CL")) else it.toString() }

        var pagina = documento.startPage(
            PdfDocument.PageInfo.Builder(ANCHO, ALTO, 1).create()
        )
        var lienzo = pagina.canvas
        var numeroPagina = 1
        var y = encabezadoPagina(
            lienzo = lienzo,
            titulo = titulo,
            subtitulo = subtitulo,
            siteName = siteName,
            etiquetaMes = etiquetaMes,
            generado = generatedAt.format(formatoFecha)
        )

        fun cerrarPagina() {
            lienzo.drawLine(MARGEN, 810f, ANCHO - MARGEN, 810f, linea)
            lienzo.drawText("SmartVolt · Registro del mes", MARGEN, 824f, pequeno)
            val pie = pincel(8f, Color.rgb(110, 118, 128)).apply { textAlign = Paint.Align.RIGHT }
            lienzo.drawText("Página $numeroPagina", ANCHO - MARGEN, 824f, pie)
            documento.finishPage(pagina)
        }

        fun nuevaPagina() {
            cerrarPagina()
            numeroPagina += 1
            pagina = documento.startPage(
                PdfDocument.PageInfo.Builder(ANCHO, ALTO, numeroPagina).create()
            )
            lienzo = pagina.canvas
            y = encabezadoPagina(
                lienzo = lienzo,
                titulo = titulo,
                subtitulo = subtitulo,
                siteName = siteName,
                etiquetaMes = etiquetaMes,
                generado = generatedAt.format(formatoFecha)
            )
            y = filaEncabezado(lienzo, encabezado, linea, y)
        }

        fun asegurarEspacio(alto: Float) {
            if (y + alto > LIMITE_INFERIOR) {
                nuevaPagina()
            }
        }

        lienzo.drawText("Resumen del mes", MARGEN, y, seccion)
        y += 16f
        lienzo.drawText("Trabajadores: ${report.totalWorkers}", MARGEN, y, cuerpo)
        lienzo.drawText(
            "Días con marcas: ${report.totalDays}",
            MARGEN + 150f,
            y,
            cuerpo
        )
        lienzo.drawText(
            "Solicitudes pendientes: ${report.totalPending}",
            MARGEN + 300f,
            y,
            cuerpo
        )
        y += 15f
        lienzo.drawText(
            "Horas de asistencia: ${formatDuration(report.totalAttendanceMinutes)}",
            MARGEN,
            y,
            cuerpo
        )
        lienzo.drawText(
            "Monto asistencia: ${formatClp(report.totalAttendanceAmountClp)}",
            MARGEN + 250f,
            y,
            cuerpo
        )
        y += 15f
        lienzo.drawText(
            "Horas extra aprobadas: ${formatDuration(report.totalOvertimeMinutes)}",
            MARGEN,
            y,
            cuerpo
        )
        lienzo.drawText(
            "Monto extra: ${formatClp(report.totalOvertimeAmountClp)}",
            MARGEN + 250f,
            y,
            cuerpo
        )
        y += 18f
        lienzo.drawText("Monto total: ${formatClp(report.totalAmountClp)}", MARGEN, y, negrita)
        y += 12f
        lienzo.drawText(
            "Estimación con la tarifa de la faena. No incluye impuestos, bonos ni descuentos.",
            MARGEN,
            y,
            pequeno
        )
        y += 22f

        y = filaEncabezado(lienzo, encabezado, linea, y)

        report.workers.forEach { trabajador ->
            asegurarEspacio(34f)
            lienzo.drawText("${trabajador.workerName} (${trabajador.workerId})", MARGEN, y, negrita)
            val resumen = "${formatDuration(trabajador.totalMinutes)} · " +
                formatClp(trabajador.totalAmountClp)
            lienzo.drawText(resumen, ANCHO - MARGEN, y, montoDerecha)
            y += 12f

            trabajador.days.sortedBy { it.date }.forEach { dia ->
                asegurarEspacio(16f)
                lienzo.drawText(dia.date.format(formatoFecha), MARGEN, y, cuerpo)
                lienzo.drawText(
                    dia.firstCheckInMillis?.let { hora(it, zone, formatoHora) } ?: "",
                    MARGEN + 86f,
                    y,
                    cuerpo
                )
                lienzo.drawText(
                    dia.lastCheckOutMillis?.let { hora(it, zone, formatoHora) } ?: "",
                    MARGEN + 136f,
                    y,
                    cuerpo
                )
                lienzo.drawText(formatDecimalHours(dia.attendanceMinutes), MARGEN + 190f, y, cuerpo)
                lienzo.drawText(formatDecimalHours(dia.overtimeMinutes), MARGEN + 250f, y, cuerpo)
                lienzo.drawText(formatClp(dia.totalAmountClp), ANCHO - MARGEN, y, cuerpo)
                if (dia.withoutCheckOut) {
                    lienzo.drawText("Sin salida", MARGEN + 310f, y, pequeno)
                }
                y += 14f
            }
            y += 8f
        }

        asegurarEspacio(30f)
        lienzo.drawLine(MARGEN, y - 4f, ANCHO - MARGEN, y - 4f, linea)
        y += 12f
        lienzo.drawText("TOTAL DEL MES", MARGEN, y, negrita)
        lienzo.drawText(formatDecimalHours(report.totalAttendanceMinutes), MARGEN + 190f, y, negrita)
        lienzo.drawText(formatDecimalHours(report.totalOvertimeMinutes), MARGEN + 250f, y, negrita)
        lienzo.drawText(formatClp(report.totalAmountClp), ANCHO - MARGEN, y, montoDerecha)

        cerrarPagina()

        return ByteArrayOutputStream().use { salida ->
            documento.writeTo(salida)
            documento.close()
            salida.toByteArray()
        }
    }

    private fun encabezadoPagina(
        lienzo: Canvas,
        titulo: Paint,
        subtitulo: Paint,
        siteName: String,
        etiquetaMes: String,
        generado: String
    ): Float {
        lienzo.drawText("Registro del mes", MARGEN, 58f, titulo)
        lienzo.drawText("$siteName · $etiquetaMes", MARGEN, 76f, subtitulo)
        lienzo.drawText("Generado el $generado", MARGEN, 90f, subtitulo)
        lienzo.drawLine(MARGEN, 100f, ANCHO - MARGEN, 100f, Paint().apply {
            color = Color.rgb(214, 218, 224)
            strokeWidth = 0.6f
        })
        return 124f
    }

    private fun filaEncabezado(
        lienzo: Canvas,
        encabezado: Paint,
        linea: Paint,
        y: Float
    ): Float {
        lienzo.drawText("Fecha", MARGEN, y, encabezado)
        lienzo.drawText("Entrada", MARGEN + 86f, y, encabezado)
        lienzo.drawText("Salida", MARGEN + 136f, y, encabezado)
        lienzo.drawText("Horas", MARGEN + 190f, y, encabezado)
        lienzo.drawText("Extra", MARGEN + 250f, y, encabezado)
        val monto = pincel(8.5f, Color.rgb(70, 78, 90), negrita = true).apply {
            textAlign = Paint.Align.RIGHT
        }
        lienzo.drawText("Monto", ANCHO - MARGEN, y, monto)
        lienzo.drawLine(MARGEN, y + 4f, ANCHO - MARGEN, y + 4f, linea)
        return y + 18f
    }

    private fun hora(millis: Long, zone: ZoneId, formato: DateTimeFormatter): String =
        Instant.ofEpochMilli(millis).atZone(zone).toLocalTime().format(formato)

    private fun pincel(tamano: Float, color: Int, negrita: Boolean = false): Paint =
        Paint().apply {
            this.color = color
            textSize = tamano
            isAntiAlias = true
            typeface = if (negrita) Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            else Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
}
