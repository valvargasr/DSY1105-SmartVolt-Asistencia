package cl.smartvolt.asistencia.feature.history

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.location.MockSignal

/**
 * Pantalla de historial de marcas.
 *
 * Muestra las marcas guardadas localmente con fecha, tipo y estado de
 * sincronización. No presenta marcas offline como validadas por el servidor.
 */
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onVolver: () -> Unit
) {
    val events by viewModel.events.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Text(
            text = "Historial de marcas",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (isLoading) "Cargando..." else "Marcas guardadas en este dispositivo",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (events.isEmpty() && !isLoading) {
            Text(
                text = "Aún no hay marcas registradas.",
                fontSize = 16.sp
            )
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(events) { event ->
                    EventCard(event)
                }
            }
        }
    }
}

@Composable
private fun EventCard(event: LocalAttendanceEvent) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (event.type == LocalAttendanceEvent.TYPE_CHECK_IN) {
                        "Entrada"
                    } else {
                        "Salida"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = estadoSincronizacion(event.syncState),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = java.text.SimpleDateFormat(
                    "dd/MM/yyyy HH:mm",
                    java.util.Locale.getDefault()
                ).format(java.util.Date(event.timestampMillis)),
                fontSize = 14.sp
            )
            if (event.latitude != null && event.longitude != null) {
                Text(
                    text = "GPS: ${event.latitude}, ${event.longitude}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (event.beaconId != null) {
                Text(
                    text = "Baliza: ${event.beaconId} (RSSI ${event.rssi})",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (event.mockSignal == MockSignal.DETECTED) {
                Text(
                    text = "Ubicación simulada informada: marca en revisión.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "Trabajador: ${event.workerId} | Faena: ${event.siteId}",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun estadoSincronizacion(syncState: String): String = when (syncState) {
    LocalAttendanceEvent.PENDING_SYNC -> "Pendiente de sincronización"
    else -> syncState
}
