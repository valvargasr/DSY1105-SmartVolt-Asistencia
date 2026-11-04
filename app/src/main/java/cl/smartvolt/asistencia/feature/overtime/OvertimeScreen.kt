package cl.smartvolt.asistencia.feature.overtime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cl.smartvolt.asistencia.domain.model.OvertimeApproval
import cl.smartvolt.asistencia.domain.model.OvertimeStatus
import cl.smartvolt.asistencia.session.DemoAccount
import cl.smartvolt.asistencia.ui.components.EmptyState
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date
import java.util.Locale

/**
 * Administración de horas extra (RF-16 / RF-17).
 *
 * El administrador autoriza o rechaza con motivo. Una salida tardía por sí sola
 * no autoriza horas extra: la decisión queda registrada en Room.
 */
@Composable
fun OvertimeScreen(
    viewModel: OvertimeViewModel,
    account: DemoAccount,
    onVolver: () -> Unit
) {
    val solicitudes by viewModel.requests.collectAsState()
    var rechazoDe by remember { mutableStateOf<OvertimeApproval?>(null) }

    val pendientes = solicitudes.filter { it.status == OvertimeStatus.PENDING }
    val decididas = solicitudes.filter { it.status != OvertimeStatus.PENDING }
        .sortedByDescending { it.createdAtMillis }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
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
                    title = "Horas extra",
                    subtitle = "Autorización · ${account.roleLabel}",
                    onBack = onVolver
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.WarningAmber,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.tertiary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Autorizar horas extra no significa pagarlas: la app solo " +
                                "registra el estado operativo (RN-10).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel(text = "Pendientes")
                    Spacer(modifier = Modifier.weight(1f))
                    StatusPill(
                        text = pendientes.size.toString(),
                        tone = if (pendientes.isEmpty()) StatusTone.NEUTRAL else StatusTone.WARNING
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                AnimatedVisibility(
                    visible = pendientes.isEmpty(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    EmptyState(
                        icon = Icons.Outlined.Schedule,
                        title = "Sin solicitudes pendientes",
                        description = "Genera una solicitud de prueba para revisar el flujo de autorización.",
                        action = {
                            Button(
                                onClick = {
                                    val hoy = LocalDate.now()
                                    val base = hoy.atStartOfDay(ZoneId.systemDefault())
                                        .toInstant().toEpochMilli()
                                    viewModel.solicitar(
                                        workerId = "OP-001",
                                        siteId = account.siteId,
                                        date = hoy,
                                        startMillis = base + 17 * 60 * 60 * 1000L,
                                        endMillis = base + 19 * 60 * 60 * 1000L,
                                        reason = "Cierre de faena programado"
                                    )
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Add,
                                    contentDescription = null
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Crear solicitud demo")
                            }
                        }
                    )
                }

                Column(
                    modifier = Modifier.animateContentSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    pendientes.forEach { solicitud ->
                        TarjetaSolicitud(
                            solicitud = solicitud,
                            onAprobar = {
                                viewModel.aprobar(solicitud.approvalId, account.workerId)
                            },
                            onRechazar = { rechazoDe = solicitud }
                        )
                    }
                }

                if (decididas.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(20.dp))
                    SectionLabel(text = "Decisiones registradas")
                    Spacer(modifier = Modifier.height(8.dp))

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        decididas.forEach { solicitud ->
                            TarjetaSolicitud(
                                solicitud = solicitud,
                                onAprobar = {},
                                onRechazar = {}
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Volver al inicio")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    rechazoDe?.let { solicitud ->
        DialogoRechazo(
            solicitud = solicitud,
            onCancelar = { rechazoDe = null },
            onConfirmar = { motivo ->
                viewModel.rechazar(solicitud.approvalId, account.workerId, motivo)
                rechazoDe = null
            }
        )
    }
}

@Composable
private fun TarjetaSolicitud(
    solicitud: OvertimeApproval,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    val tono = when (solicitud.status) {
        OvertimeStatus.PENDING -> StatusTone.WARNING
        OvertimeStatus.APPROVED -> StatusTone.OK
        OvertimeStatus.REJECTED -> StatusTone.DANGER
        OvertimeStatus.CANCELLED -> StatusTone.NEUTRAL
    }
    val etiqueta = when (solicitud.status) {
        OvertimeStatus.PENDING -> "Pendiente"
        OvertimeStatus.APPROVED -> "Autorizada"
        OvertimeStatus.REJECTED -> "Rechazada"
        OvertimeStatus.CANCELLED -> "Cancelada"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = solicitud.workerId,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = solicitud.siteId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(text = etiqueta, tone = tono)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(modifier = Modifier.height(9.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "${solicitud.date} · " +
                        "${hora(solicitud.authorizedStartMillis)}–" +
                        "${hora(solicitud.authorizedEndMillis)}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            if (!solicitud.reason.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Motivo: ${solicitud.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (solicitud.status == OvertimeStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onAprobar,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Autorizar")
                    }
                    OutlinedButton(
                        onClick = onRechazar,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ErrorOutline,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rechazar")
                    }
                }
            }
        }
    }
}

@Composable
private fun DialogoRechazo(
    solicitud: OvertimeApproval,
    onCancelar: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error
            )
        },
        title = { Text("Rechazar solicitud") },
        text = {
            Column {
                Text(
                    text = "Indica el motivo del rechazo para ${solicitud.workerId}. " +
                        "Queda registrado en el historial local.",
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = {
                        motivo = it
                        error = ""
                    },
                    label = { Text("Motivo del rechazo") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Schedule,
                            contentDescription = null
                        )
                    },
                    trailingIcon = {
                        if (error.isNotEmpty()) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = "Error en el motivo",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    },
                    isError = error.isNotEmpty(),
                    supportingText = {
                        if (error.isNotEmpty()) {
                            Text(error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (motivo.isBlank()) {
                        error = "Escribe un motivo para rechazar."
                    } else {
                        onConfirmar(motivo.trim())
                    }
                }
            ) {
                Text("Confirmar rechazo")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        }
    )
}

private fun hora(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
