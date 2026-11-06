package cl.smartvolt.asistencia.util

import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.modelo.MonthlyReport
import cl.smartvolt.asistencia.modelo.MonthlyReportCalculator
import cl.smartvolt.asistencia.modelo.SolicitudHoraExtra
import cl.smartvolt.asistencia.modelo.WorkerRole
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.util.UUID
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

// acá probamos los casos raros del reporte: día sin salida, extra, varios trabajadores
class ReporteMensualTest {

    @Test
    fun `un dia completo suma las horas trabajadas`() {
        val fecha = LocalDate.of(2026, 11, 3)
        val reporte = calcular(
            listOf(
                marca("OP-001", Asistencia.TYPE_CHECK_IN, fecha, 8),
                marca("OP-001", Asistencia.TYPE_CHECK_OUT, fecha, 17)
            )
        )

        val trabajador = reporte.workers.single()
        assertEquals(540, trabajador.attendanceMinutes)
        assertEquals(540, trabajador.totalMinutes)
        assertEquals(1, trabajador.daysWorked)
        assertEquals("Camila Rojas", trabajador.workerName)
        assertEquals(540, reporte.totalMinutes)
    }

    @Test
    fun `una entrada sin salida no suma horas`() {
        val fecha = LocalDate.of(2026, 11, 3)
        val reporte = calcular(
            listOf(marca("OP-001", Asistencia.TYPE_CHECK_IN, fecha, 8))
        )

        val trabajador = reporte.workers.single()
        val dia = trabajador.days.single()
        assertTrue(dia.withoutCheckOut)
        assertEquals(0, dia.attendanceMinutes)
        assertEquals(1, trabajador.daysWithoutCheckOut)
        assertEquals(1, reporte.workersWithoutCheckOut)
    }

    @Test
    fun `dos pares el mismo dia suman ambos tramos`() {
        val fecha = LocalDate.of(2026, 11, 3)
        val reporte = calcular(
            listOf(
                marca("OP-001", Asistencia.TYPE_CHECK_IN, fecha, 8),
                marca("OP-001", Asistencia.TYPE_CHECK_OUT, fecha, 12),
                marca("OP-001", Asistencia.TYPE_CHECK_IN, fecha, 13),
                marca("OP-001", Asistencia.TYPE_CHECK_OUT, fecha, 17)
            )
        )

        val dia = reporte.workers.single().days.single()
        assertEquals(480, dia.attendanceMinutes)
        assertEquals(480, dia.totalMinutes)
    }

    @Test
    fun `las horas extra aprobadas se suman al total`() {
        val fecha = LocalDate.of(2026, 11, 3)
        val reporte = calcular(
            eventos = listOf(
                marca("OP-001", Asistencia.TYPE_CHECK_IN, fecha, 8),
                marca("OP-001", Asistencia.TYPE_CHECK_OUT, fecha, 17)
            ),
            aprobaciones = listOf(aprobacion("OP-001", fecha, 18, 20))
        )

        val trabajador = reporte.workers.single()
        assertEquals(540, trabajador.attendanceMinutes)
        assertEquals(120, trabajador.overtimeMinutes)
        assertEquals(660, trabajador.totalMinutes)
        assertEquals(660, reporte.totalMinutes)
    }

    @Test
    fun `las horas extra pendientes no suman horas`() {
        val fecha = LocalDate.of(2026, 11, 3)
        val reporte = calcular(
            aprobaciones = listOf(
                aprobacion(
                    "OP-001",
                    fecha,
                    18,
                    20,
                    estado = SolicitudHoraExtra.PENDIENTE
                )
            )
        )

        val trabajador = reporte.workers.single()
        assertEquals(0, trabajador.overtimeMinutes)
        assertEquals(1, trabajador.pendingRequests)
        assertEquals(1, reporte.totalPending)
    }

    @Test
    fun `el reporte solo incluye el mes consultado`() {
        val reporte = calcular(
            listOf(
                marca("OP-001", Asistencia.TYPE_CHECK_IN, LocalDate.of(2026, 10, 30), 8),
                marca("OP-001", Asistencia.TYPE_CHECK_OUT, LocalDate.of(2026, 10, 30), 17),
                marca("OP-001", Asistencia.TYPE_CHECK_IN, LocalDate.of(2026, 11, 3), 8),
                marca("OP-001", Asistencia.TYPE_CHECK_OUT, LocalDate.of(2026, 11, 3), 17)
            ),
            mes = YearMonth.of(2026, 11)
        )

        assertEquals(1, reporte.totalDays)
        assertEquals(540, reporte.totalMinutes)
    }

    @Test
    fun `el csv incluye encabezado filas y total`() {
        val fecha = LocalDate.of(2026, 11, 3)
        val reporte = calcular(
            eventos = listOf(
                marca("OP-001", Asistencia.TYPE_CHECK_IN, fecha, 8),
                marca("OP-001", Asistencia.TYPE_CHECK_OUT, fecha, 17)
            ),
            aprobaciones = listOf(aprobacion("OP-001", fecha, 18, 20))
        )

        val csv = ReporteCsv.contenido(reporte)

        assertTrue(csv.startsWith("\uFEFF"))
        assertTrue(csv.contains("Fecha;Trabajador;Identificador"))
        assertTrue(csv.contains("Horas totales"))
        assertTrue(csv.contains("2026-11-03;Camila Rojas;OP-001;08:00;17:00"))
        assertTrue(csv.contains("TOTAL;;;;;9,00;;2,00;11,00"))
        val columnas = csv.lineSequence().first().removePrefix("\uFEFF").split(';')
        assertEquals(9, columnas.size)
        assertNotNull(ReporteCsv.nombreArchivo(reporte.month))
        assertEquals("registro-smartvolt-2026-11.csv", ReporteCsv.nombreArchivo(reporte.month))
    }

    private fun calcular(
        eventos: List<Asistencia> = emptyList(),
        aprobaciones: List<SolicitudHoraExtra> = emptyList(),
        mes: YearMonth = YearMonth.of(2026, 11)
    ): MonthlyReport = MonthlyReportCalculator.build(
        events = eventos,
        approvals = aprobaciones,
        month = mes,
        zone = ZONA,
        nameOf = { "Camila Rojas" }
    )

    private fun marca(
        workerId: String,
        tipo: String,
        fecha: LocalDate,
        hora: Int
    ): Asistencia {
        val millis = fecha.atTime(hora, 0).atZone(ZONA).toInstant().toEpochMilli()
        return Asistencia(
            id = UUID.randomUUID().toString(),
            type = tipo,
            timestampMillis = millis,
            workerId = workerId,
            siteId = "FAENA-DEMO-01"
        )
    }

    private fun aprobacion(
        workerId: String,
        fecha: LocalDate,
        desde: Int,
        hasta: Int,
        estado: String = SolicitudHoraExtra.APROBADA
    ): SolicitudHoraExtra {
        val inicio = fecha.atTime(desde, 0).atZone(ZONA).toInstant().toEpochMilli()
        val fin = fecha.atTime(hasta, 0).atZone(ZONA).toInstant().toEpochMilli()
        return SolicitudHoraExtra(
            approvalId = UUID.randomUUID().toString(),
            workerId = workerId,
            siteId = "FAENA-DEMO-01",
            dateMillis = java.time.LocalDate.ofEpochDay(fecha.toEpochDay())
                .atStartOfDay(java.time.ZoneOffset.UTC).toInstant().toEpochMilli(),
            authorizedStartMillis = inicio,
            authorizedEndMillis = fin,
            approverRole = WorkerRole.ADMIN.name,
            approverId = "ADM-003",
            status = estado
        )
    }

    private companion object {
        val ZONA: ZoneId = ZoneId.of("America/Santiago")
    }
}
