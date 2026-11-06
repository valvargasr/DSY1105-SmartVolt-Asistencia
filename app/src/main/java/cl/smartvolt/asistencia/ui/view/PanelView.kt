package cl.smartvolt.asistencia.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.PersonSearch
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.modelo.Asistencia
import cl.smartvolt.asistencia.ui.components.EmptyState
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltMark
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import cl.smartvolt.asistencia.ui.components.statusColor
import cl.smartvolt.asistencia.ui.components.statusContainer
import cl.smartvolt.asistencia.util.Fechas.fechaHora
import cl.smartvolt.asistencia.util.Fechas.hora
import cl.smartvolt.asistencia.viewmodel.PanelUiState
import cl.smartvolt.asistencia.viewmodel.PanelViewModel

@Composable
fun PanelView(
    onVolver: () -> Unit,
    viewModel: PanelViewModel = viewModel()
) {
    val estado by viewModel.uiState.collectAsState()
    val porTrabajador = estado.marcasHoy
        .groupBy { it.workerId }
        .toList()
        .sortedBy { it.first }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 560.dp)
            ) {
                SmartVoltTopBar(
                    title = "Panel de faena",
                    subtitle = estado.faena.name,
                    onBack = onVolver
                )

                Spacer(Modifier.height(16.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        SmartVoltMark(size = 46.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel(
                                text = "Estado operativo",
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Text(
                                text = if (estado.totalTrabajadores == 0) {
                                    "Sin marcas todavía"
                                } else if (estado.totalTrabajadores == 1) {
                                    "1 trabajador en faena"
                                } else {
                                    "${estado.totalTrabajadores} trabajadores en faena"
                                },
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        StatusPill(
                            text = if (estado.pendientes == 0) "Al día" else "Pendiente",
                            tone = if (estado.pendientes == 0) StatusTone.OK else StatusTone.WARNING
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Metrica(
                        valor = estado.presentes.toString(),
                        etiqueta = "Presentes",
                        detalle = "Entrada sin salida",
                        icon = Icons.Outlined.Groups,
                        tone = StatusTone.OK,
                        modifier = Modifier.weight(1f)
                    )
                    Metrica(
                        valor = estado.conSalida.toString(),
                        etiqueta = "Con salida",
                        detalle = "Salida registrada hoy",
                        icon = Icons.Outlined.Logout,
                        tone = StatusTone.NEUTRAL,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Metrica(
                        valor = estado.marcasHoy.size.toString(),
                        etiqueta = "Marcas",
                        detalle = "Registros del día",
                        icon = Icons.Outlined.Badge,
                        tone = StatusTone.WARNING,
                        modifier = Modifier.weight(1f)
                    )
                    Metrica(
                        valor = estado.pendientes.toString(),
                        etiqueta = "Pendientes",
                        detalle = "Por sincronizar",
                        icon = Icons.Outlined.PersonSearch,
                        tone = StatusTone.DANGER,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(18.dp))
                SectionLabel(text = "Detalle por trabajador")
                Spacer(Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = porTrabajador.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    EmptyState(
                        icon = Icons.Outlined.PersonSearch,
                        title = "Sin marcas en esta faena",
                        description = "Cuando el equipo registre entrada o salida, aparecerá aquí."
                    )
                }

                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    porTrabajador.forEach { (workerId, marcas) ->
                        FilaTrabajador(workerId, marcas)
                    }
                }

                Spacer(Modifier.height(18.dp))
                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Volver")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun Metrica(
    valor: String,
    etiqueta: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    detalle: String? = null,
    tone: StatusTone
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = statusContainer(tone)),
        border = BorderStroke(1.dp, statusColor(tone).copy(alpha = 0.14f))
    ) {
        Column(modifier = Modifier.padding(15.dp)) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = statusColor(tone),
                modifier = Modifier.size(23.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = valor,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            detalle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor(tone)
                )
            }
        }
    }
}

@Composable
private fun FilaTrabajador(workerId: String, marcas: List<Asistencia>) {
    val entrada = marcas.firstOrNull { it.type == Asistencia.TYPE_CHECK_IN }
    val salida = marcas.lastOrNull { it.type == Asistencia.TYPE_CHECK_OUT }
    val pendientes = marcas.count { it.syncState == Asistencia.PENDING_SYNC }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .background(MaterialTheme.colorScheme.secondaryContainer, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Badge,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(workerId, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = when {
                            salida != null -> "Jornada cerrada"
                            entrada != null -> "En faena"
                            else -> "Sin entrada"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(
                    text = if (pendientes == 0) "Sincronizado" else "$pendientes pend.",
                    tone = if (pendientes == 0) StatusTone.OK else StatusTone.WARNING
                )
            }

            Spacer(Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Marca(
                    icon = Icons.Outlined.Login,
                    etiqueta = "Entrada",
                    millis = entrada?.timestampMillis,
                    modifier = Modifier.weight(1f)
                )
                Marca(
                    icon = Icons.Outlined.Logout,
                    etiqueta = "Salida",
                    millis = salida?.timestampMillis,
                    modifier = Modifier.weight(1f)
                )
            }

            val ultima = marcas.lastOrNull()
            if (ultima != null) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "Última marca: ${fechaHora(ultima.timestampMillis)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun Marca(
    icon: ImageVector,
    etiqueta: String,
    millis: Long?,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(15.dp)
        )
        Spacer(Modifier.width(6.dp))
        Column {
            Text(
                text = etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = millis?.let { hora(it) } ?: "--:--",
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
