package cl.smartvolt.asistencia.feature.report

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cl.smartvolt.asistencia.domain.report.MonthlyReport
import cl.smartvolt.asistencia.domain.report.ReportDay
import cl.smartvolt.asistencia.domain.report.ReportWorker
import cl.smartvolt.asistencia.domain.report.formatDuration
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 * Registro mensual de la faena.
 *
 * Muestra el total de horas por trabajador y permite guardar el detalle en un
 * archivo CSV. El total suma la asistencia y las horas extra aprobadas; las
 * solicitudes pendientes se informan aparte porque todavía no están aprobadas.
 */
@Composable
fun MonthlyReportScreen(
    viewModel: MonthlyReportViewModel,
    siteName: String,
    onVolver: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var csvPendiente by remember { mutableStateOf<String?>(null) }

    val guardarCsv = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        val texto = csvPendiente
        if (uri != null && texto != null) {
            val guardado = runCatching {
                context.contentResolver.openOutputStream(uri)?.use { salida ->
                    salida.write(texto.toByteArray(Charsets.UTF_8))
                }
            }.isSuccess

            Toast.makeText(
                context,
                if (guardado) "Registro guardado." else "No se pudo guardar el archivo.",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SmartVoltTopBar(
                title = "Registro del mes",
                subtitle = "Administración · $siteName",
                onBack = onVolver
            )
            Spacer(Modifier.height(12.dp))

            SelectorMes(
                month = estado.month,
                onAnterior = viewModel::onMesAnterior,
                onSiguiente = viewModel::onMesSiguiente
            )

            Spacer(Modifier.height(16.dp))

            val reporte = estado.report
            if (estado.cargando && reporte == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (reporte == null || reporte.isEmpty) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "No hay marcas registradas en este mes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                ResumenMes(reporte)

                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = {
                        csvPendiente = ReportCsv.content(reporte)
                        guardarCsv.launch(ReportCsv.fileName(reporte.month))
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(imageVector = Icons.Outlined.Download, contentDescription = null)
                    Text(
                        text = "Guardar registro en CSV",
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    text = "Detalle por trabajador",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(Modifier.height(8.dp))

                reporte.workers.forEach { trabajador ->
                    TarjetaTrabajador(trabajador)
                    Spacer(Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
private fun SelectorMes(
    month: YearMonth,
    onAnterior: () -> Unit,
    onSiguiente: () -> Unit
) {
    val locale = Locale.forLanguageTag("es-CL")
    val formato = remember { DateTimeFormatter.ofPattern("MMMM yyyy", locale) }
    val etiqueta = month.atDay(1).format(formato)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
    val mesActual = YearMonth.now()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onAnterior) {
                Icon(
                    imageVector = Icons.Outlined.ChevronLeft,
                    contentDescription = "Mes anterior"
                )
            }
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onSiguiente, enabled = month < mesActual) {
                Icon(
                    imageVector = Icons.Outlined.ChevronRight,
                    contentDescription = "Mes siguiente"
                )
            }
        }
    }
}

@Composable
private fun ResumenMes(reporte: MonthlyReport) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Total del mes: ${formatDuration(reporte.totalMinutes)}",
                style = MaterialTheme.typography.titleLarge
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Incluye asistencia y horas extra aprobadas.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            FilaResumen("Asistencia", formatDuration(reporte.totalAttendanceMinutes))
            FilaResumen("Horas extra aprobadas", formatDuration(reporte.totalOvertimeMinutes))
            FilaResumen("Trabajadores", reporte.totalWorkers.toString())
            FilaResumen("Días con marcas", reporte.totalDays.toString())
            if (reporte.totalPending > 0) {
                FilaResumen("Solicitudes pendientes", reporte.totalPending.toString())
            }
            if (reporte.workersWithoutCheckOut > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Hay ${reporte.workersWithoutCheckOut} trabajador(es) con una entrada " +
                        "sin salida. Esas horas no se suman hasta completar la jornada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun FilaResumen(etiqueta: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(text = valor, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun TarjetaTrabajador(trabajador: ReportWorker) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = trabajador.workerName,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = trabajador.workerId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Text(
                    text = formatDuration(trabajador.totalMinutes),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Asistencia ${formatDuration(trabajador.attendanceMinutes)} · " +
                    "Extra ${formatDuration(trabajador.overtimeMinutes)} · " +
                    "${trabajador.daysWorked} día(s)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (trabajador.pendingRequests > 0) {
                Text(
                    text = "Solicitudes pendientes: ${trabajador.pendingRequests}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(Modifier.height(10.dp))
            trabajador.days.forEach { dia ->
                FilaDia(dia)
            }
        }
    }
}

@Composable
private fun FilaDia(dia: ReportDay) {
    val locale = Locale.forLanguageTag("es-CL")
    val fecha = remember(dia.date) {
        DateTimeFormatter.ofPattern("EEE dd/MM", locale).format(dia.date)
    }
    val entrada = dia.firstCheckInMillis?.let { hora(it) }
    val salida = dia.lastCheckOutMillis?.let { hora(it) }

    val horario = when {
        entrada != null && salida != null -> "$entrada - $salida"
        entrada != null -> "Entrada $entrada"
        else -> "Sin marcas de asistencia"
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = fecha,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.width(84.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = horario, style = MaterialTheme.typography.bodySmall)
            if (dia.withoutCheckOut) {
                Text(
                    text = "Sin salida",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
        Text(
            text = formatDuration(dia.totalMinutes),
            style = MaterialTheme.typography.bodySmall
        )
    }
}

private fun hora(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
