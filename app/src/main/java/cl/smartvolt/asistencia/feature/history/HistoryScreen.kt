package cl.smartvolt.asistencia.feature.history

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.location.MockSignal
import cl.smartvolt.asistencia.ui.components.EmptyState
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class FiltroHistorial(val etiqueta: String) {
    TODAS("Todas"),
    ENTRADAS("Entradas"),
    SALIDAS("Salidas")
}

/**
 * Pantalla de historial de marcas.
 *
 * Muestra las marcas guardadas localmente con fecha, tipo y estado de
 * sincronización. No presenta marcas offline como validadas por el servidor.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    workerLabel: String,
    onVolver: () -> Unit
) {
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var filtro by rememberSaveable { mutableStateOf(FiltroHistorial.TODAS) }

    val filtrados = when (filtro) {
        FiltroHistorial.TODAS -> events
        FiltroHistorial.ENTRADAS -> events.filter {
            it.type == LocalAttendanceEvent.TYPE_CHECK_IN
        }
        FiltroHistorial.SALIDAS -> events.filter {
            it.type == LocalAttendanceEvent.TYPE_CHECK_OUT
        }
    }
    val pendientes = events.count {
        it.syncState == LocalAttendanceEvent.PENDING_SYNC
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SmartVoltTopBar(
            title = "Historial de marcas",
            subtitle = workerLabel,
            onBack = onVolver,
            trailing = {
                IconButton(onClick = viewModel::refrescar) {
                    Icon(
                        imageVector = Icons.Outlined.Refresh,
                        contentDescription = "Actualizar historial"
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(14.dp))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel(text = "Registros locales")
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "${events.size} ${if (events.size == 1) "marca" else "marcas"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                StatusPill(
                    text = if (pendientes == 0) "Al día" else "$pendientes pendientes",
                    tone = if (pendientes == 0) StatusTone.OK else StatusTone.WARNING
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FiltroHistorial.entries.forEach { opcion ->
                FilterChip(
                    selected = filtro == opcion,
                    onClick = { filtro = opcion },
                    label = { Text(opcion.etiqueta) },
                    leadingIcon = if (filtro == opcion) {
                        {
                            Icon(
                                imageVector = Icons.Outlined.Check,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    } else {
                        null
                    }
                )
            }
        }

        if (isLoading) {
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            androidx.compose.animation.AnimatedVisibility(
                visible = filtrados.isEmpty() && !isLoading,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = if (events.isEmpty()) {
                        "Aún no hay marcas"
                    } else {
                        "Sin resultados para este filtro"
                    },
                    description = if (events.isEmpty()) {
                        "Cuando registres una entrada o salida, aparecerá aquí con su " +
                            "estado de sincronización."
                    } else {
                        "Prueba con otro filtro para ver las marcas guardadas."
                    },
                    action = if (events.isEmpty()) {
                        {
                            Button(onClick = onVolver) {
                                Text("Ir a registrar")
                            }
                        }
                    } else {
                        null
                    }
                )
            }

            androidx.compose.animation.AnimatedVisibility(
                visible = filtrados.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtrados, key = { it.id }) { event ->
                        EventCard(event)
                    }
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: LocalAttendanceEvent) {
    val esEntrada = event.type == LocalAttendanceEvent.TYPE_CHECK_IN
    val sincronizada = event.syncState != LocalAttendanceEvent.PENDING_SYNC
    val icono = if (esEntrada) {
        Icons.AutoMirrored.Outlined.Login
    } else {
        Icons.AutoMirrored.Outlined.Logout
    }
    val iconoTinte = if (esEntrada) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val iconoFondo = if (esEntrada) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(iconoFondo, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = iconoTinte,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (esEntrada) "Entrada" else "Salida",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = formatoFecha(event.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                StatusPill(
                    text = if (sincronizada) "Registrada" else "Pendiente",
                    tone = if (sincronizada) StatusTone.OK else StatusTone.WARNING
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(9.dp))

            if (event.latitude != null && event.longitude != null) {
                DetailRow(
                    icon = Icons.Outlined.LocationOn,
                    label = "GPS",
                    value = String.format(
                        Locale.ROOT,
                        "%.5f, %.5f",
                        event.latitude,
                        event.longitude
                    )
                )
            }

            if (event.beaconId != null) {
                DetailRow(
                    icon = Icons.Outlined.Bluetooth,
                    label = "Baliza",
                    value = "${event.beaconId} · RSSI ${event.rssi}"
                )
            }

            if (event.mockSignal == MockSignal.DETECTED) {
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Ubicación simulada informada: marca en revisión.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "${event.workerId} · ${event.siteId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun DetailRow(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatoFecha(millis: Long): String =
    SimpleDateFormat("dd MMM yyyy · HH:mm", Locale.forLanguageTag("es-CL")).format(Date(millis))
