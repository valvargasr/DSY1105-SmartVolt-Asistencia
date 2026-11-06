package cl.smartvolt.asistencia.ui.view

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.ui.components.EmptyState
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import cl.smartvolt.asistencia.util.Fechas.fechaHora
import cl.smartvolt.asistencia.viewmodel.HistorialViewModel
import java.util.Locale

private enum class FiltroHistorial(val etiqueta: String) {
    TODAS("Todas"),
    ENTRADAS("Entradas"),
    SALIDAS("Salidas")
}

@Composable
fun HistorialView(viewModel: HistorialViewModel = viewModel()) {
    val marcas by viewModel.marcas.collectAsState()
    val encabezado by viewModel.encabezado.collectAsState()
    var filtro by rememberSaveable { mutableStateOf(FiltroHistorial.TODAS) }

    // el filtro es solo visual, no vuelve a consultar la base
    val filtradas = when (filtro) {
        FiltroHistorial.TODAS -> marcas
        FiltroHistorial.ENTRADAS -> marcas.filter { it.type == Asistencia.TYPE_CHECK_IN }
        FiltroHistorial.SALIDAS -> marcas.filter { it.type == Asistencia.TYPE_CHECK_OUT }
    }
    val pendientes = marcas.count { it.syncState == Asistencia.PENDING_SYNC }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp)
    ) {
        SmartVoltTopBar(
            title = "Historial",
            subtitle = encabezado
        )

        Spacer(Modifier.height(14.dp))
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
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = "${marcas.size} ${if (marcas.size == 1) "marca" else "marcas"}",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                StatusPill(
                    text = if (pendientes == 0) "Al día" else "$pendientes pendientes",
                    tone = if (pendientes == 0) StatusTone.OK else StatusTone.WARNING
                )
            }
        }

        Spacer(Modifier.height(12.dp))
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

        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            if (filtradas.isEmpty()) {
                EmptyState(
                    icon = Icons.Outlined.History,
                    title = if (marcas.isEmpty()) "Aún no hay marcas" else "Sin resultados",
                    description = if (marcas.isEmpty()) {
                        "Cuando registres una entrada o salida, aparecerá aquí."
                    } else {
                        "Prueba con otro filtro para ver las marcas guardadas."
                    }
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    items(filtradas, key = { it.id }) { marca ->
                        TarjetaMarca(marca)
                    }
                }
            }
        }
    }
}

@Composable
private fun TarjetaMarca(marca: Asistencia) {
    // cada tarjeta muestra dónde y con qué baliza se marcó, por si hay que revisar
    val esEntrada = marca.type == Asistencia.TYPE_CHECK_IN
    val sincronizada = marca.syncState != Asistencia.PENDING_SYNC
    val icono = if (esEntrada) Icons.Outlined.Login else Icons.Outlined.Logout
    val tinte = if (esEntrada) {
        MaterialTheme.colorScheme.secondary
    } else {
        MaterialTheme.colorScheme.tertiary
    }
    val fondo = if (esEntrada) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.tertiaryContainer
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(fondo, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icono,
                        contentDescription = null,
                        tint = tinte,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (esEntrada) "Entrada" else "Salida",
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = fechaHora(marca.timestampMillis),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = if (sincronizada) "Registrada" else "Pendiente",
                    tone = if (sincronizada) StatusTone.OK else StatusTone.WARNING
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(9.dp))

            if (marca.latitude != null && marca.longitude != null) {
                FilaDetalle(
                    icon = Icons.Outlined.LocationOn,
                    label = "GPS",
                    value = String.format(
                        Locale.ROOT,
                        "%.5f, %.5f",
                        marca.latitude,
                        marca.longitude
                    )
                )
            }
            if (marca.beaconId != null) {
                FilaDetalle(
                    icon = Icons.Outlined.Bluetooth,
                    label = "Baliza",
                    value = "${marca.beaconId} - RSSI ${marca.rssi}"
                )
            }
            if (marca.mockSignal) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "Ubicación simulada informada.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = "${marca.workerId} - ${marca.siteId}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FilaDetalle(icon: ImageVector, label: String, value: String) {
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
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
