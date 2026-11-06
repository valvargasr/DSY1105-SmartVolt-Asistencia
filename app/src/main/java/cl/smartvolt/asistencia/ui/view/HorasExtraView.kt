package cl.smartvolt.asistencia.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.modelo.DemoAccount
import cl.smartvolt.asistencia.modelo.EstadoSolicitud
import cl.smartvolt.asistencia.modelo.SolicitudHoraExtra
import cl.smartvolt.asistencia.modelo.WorkerRole
import cl.smartvolt.asistencia.ui.components.EmptyState
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import cl.smartvolt.asistencia.util.Fechas.hora
import cl.smartvolt.asistencia.viewmodel.HorasExtraViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun HorasExtraView(
    cuenta: DemoAccount,
    onVolver: () -> Unit,
    viewModel: HorasExtraViewModel = viewModel()
) {
    val solicitudes by viewModel.solicitudes.collectAsState()
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(cuenta.workerId) { viewModel.limpiarMensaje() }

    if (cuenta.role == WorkerRole.WORKER) {
        FormularioObrero(
            cuenta = cuenta,
            solicitudes = solicitudes,
            mensaje = estado.mensaje,
            guardando = estado.guardando,
            errores = estado.errores,
            onSolicitar = viewModel::solicitar,
            onVolver = onVolver
        )
    } else {
        AdministrarSolicitudes(
            solicitudes = solicitudes,
            mensaje = estado.mensaje,
            onAprobar = viewModel::aprobar,
            onRechazar = viewModel::rechazar,
            onVolver = onVolver
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FormularioObrero(
    cuenta: DemoAccount,
    solicitudes: List<SolicitudHoraExtra>,
    mensaje: String?,
    guardando: Boolean,
    errores: cl.smartvolt.asistencia.util.Validaciones.ErroresHoraExtra,
    onSolicitar: (LocalDate?, String, String, String) -> Unit,
    onVolver: () -> Unit
) {
    val hoy = remember { LocalDate.now() }
    var fechaMillis by rememberSaveable {
        mutableStateOf(hoy.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    }
    var desde by rememberSaveable { mutableStateOf("18:00") }
    var hasta by rememberSaveable { mutableStateOf("20:00") }
    var motivo by rememberSaveable { mutableStateOf("") }
    var mostrarCalendario by remember { mutableStateOf(false) }

    val fecha = Instant.ofEpochMilli(fechaMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    LaunchedEffect(mensaje) {
        if (mensaje?.startsWith("Solicitud enviada") == true) motivo = ""
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SmartVoltTopBar(
                    title = "Solicitar horas extra",
                    subtitle = "Trabajador - ${cuenta.siteName}",
                    onBack = onVolver
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Completa los datos. El administrador revisará tu solicitud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = fecha.format(formatter),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Fecha") },
                            trailingIcon = {
                                IconButton(onClick = { mostrarCalendario = true }) {
                                    Icon(
                                        imageVector = Icons.Outlined.CalendarMonth,
                                        contentDescription = "Elegir fecha"
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = desde,
                                onValueChange = { desde = it },
                                label = { Text("Desde (HH:mm)") },
                                singleLine = true,
                                isError = errores.desde.isNotEmpty(),
                                supportingText = {
                                    if (errores.desde.isNotEmpty()) Text(errores.desde)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = hasta,
                                onValueChange = { hasta = it },
                                label = { Text("Hasta (HH:mm)") },
                                singleLine = true,
                                isError = errores.hasta.isNotEmpty(),
                                supportingText = {
                                    if (errores.hasta.isNotEmpty()) Text(errores.hasta)
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = motivo,
                            onValueChange = { motivo = it },
                            label = { Text("Motivo") },
                            minLines = 2,
                            maxLines = 4,
                            isError = errores.motivo.isNotEmpty(),
                            supportingText = {
                                if (errores.motivo.isNotEmpty()) Text(errores.motivo)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { onSolicitar(fecha, desde, hasta, motivo) },
                            enabled = !guardando,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                        ) {
                            Icon(Icons.Outlined.Timer, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (guardando) "Guardando..." else "Enviar solicitud")
                        }
                        if (!mensaje.isNullOrBlank()) {
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = mensaje,
                                color = if (mensaje.startsWith("No se pudo")) {
                                    MaterialTheme.colorScheme.error
                                } else {
                                    MaterialTheme.colorScheme.primary
                                },
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Mis solicitudes")
                    Spacer(Modifier.weight(1f))
                    StatusPill(
                        text = solicitudes.size.toString(),
                        tone = if (solicitudes.isEmpty()) StatusTone.NEUTRAL else StatusTone.OK
                    )
                }
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = solicitudes.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    EmptyState(
                        icon = Icons.Outlined.Schedule,
                        title = "Todavía no tienes solicitudes",
                        description = "Cuando envíes una solicitud, aquí podrás revisar su estado."
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    solicitudes.forEach { TarjetaSolicitud(it, mostrarTrabajador = false) }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Volver")
                }
            }
        }
    }

    if (mostrarCalendario) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = fechaMillis)
        DatePickerDialog(
            onDismissRequest = { mostrarCalendario = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { fechaMillis = it }
                    mostrarCalendario = false
                }) { Text("Aceptar") }
            },
            dismissButton = {
                TextButton(onClick = { mostrarCalendario = false }) { Text("Cancelar") }
            }
        ) { DatePicker(state = pickerState) }
    }
}

@Composable
private fun AdministrarSolicitudes(
    solicitudes: List<SolicitudHoraExtra>,
    mensaje: String?,
    onAprobar: (String) -> Unit,
    onRechazar: (String, String) -> Unit,
    onVolver: () -> Unit
) {
    var rechazoDe by remember { mutableStateOf<SolicitudHoraExtra?>(null) }
    val pendientes = solicitudes.filter { it.estado == EstadoSolicitud.PENDIENTE }
    val decididas = solicitudes.filter { it.estado != EstadoSolicitud.PENDIENTE }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SmartVoltTopBar(
                    title = "Horas extra",
                    subtitle = "Administración",
                    onBack = onVolver
                )
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Outlined.WarningAmber,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = "Autorizar horas extra no significa pagarlas. " +
                            "Aquí solo se registra la decisión operativa.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (!mensaje.isNullOrBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(mensaje, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(18.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Solicitudes pendientes")
                    Spacer(Modifier.weight(1f))
                    StatusPill(
                        text = pendientes.size.toString(),
                        tone = if (pendientes.isEmpty()) StatusTone.NEUTRAL else StatusTone.WARNING
                    )
                }
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = pendientes.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    EmptyState(
                        icon = Icons.Outlined.Schedule,
                        title = "Sin solicitudes pendientes",
                        description = "Las solicitudes de los trabajadores aparecerán aquí."
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pendientes.forEach { solicitud ->
                        TarjetaSolicitud(
                            solicitud = solicitud,
                            mostrarTrabajador = true,
                            acciones = {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = { onAprobar(solicitud.approvalId) },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.secondary,
                                            contentColor = MaterialTheme.colorScheme.onSecondary
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.CheckCircle,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text("Aprobar")
                                    }
                                    OutlinedButton(
                                        onClick = { rechazoDe = solicitud },
                                        colors = ButtonDefaults.outlinedButtonColors(
                                            contentColor = MaterialTheme.colorScheme.error
                                        ),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(50.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Outlined.ErrorOutline,
                                            contentDescription = null,
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(Modifier.width(5.dp))
                                        Text("Rechazar")
                                    }
                                }
                            }
                        )
                    }
                }

                if (decididas.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    SectionLabel("Solicitudes resueltas")
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        decididas.forEach { TarjetaSolicitud(it, mostrarTrabajador = true) }
                    }
                }

                Spacer(Modifier.height(20.dp))
                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                ) {
                    Text("Volver")
                }
            }
        }
    }

    rechazoDe?.let { solicitud ->
        DialogoRechazo(
            solicitud = solicitud,
            onCancelar = { rechazoDe = null },
            onConfirmar = { motivo ->
                onRechazar(solicitud.approvalId, motivo)
                rechazoDe = null
            }
        )
    }
}

@Composable
private fun TarjetaSolicitud(
    solicitud: SolicitudHoraExtra,
    mostrarTrabajador: Boolean,
    acciones: (@Composable () -> Unit)? = null
) {
    val tone = when (solicitud.estado) {
        EstadoSolicitud.PENDIENTE -> StatusTone.WARNING
        EstadoSolicitud.APROBADA -> StatusTone.OK
        EstadoSolicitud.RECHAZADA -> StatusTone.DANGER
        EstadoSolicitud.CANCELADA -> StatusTone.NEUTRAL
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (mostrarTrabajador) {
                        Text(solicitud.workerId, style = MaterialTheme.typography.titleSmall)
                    }
                    Text(
                        text = solicitud.siteId,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                StatusPill(solicitud.estado.etiqueta, tone)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Text("Fecha: ${solicitud.fecha}", style = MaterialTheme.typography.bodyMedium)
            Text(
                text = "Horario: ${hora(solicitud.authorizedStartMillis)} - " +
                    hora(solicitud.authorizedEndMillis),
                style = MaterialTheme.typography.bodyMedium
            )
            solicitud.reason?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Motivo: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            solicitud.decisionNote?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Nota del administrador: $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (acciones != null) {
                Spacer(Modifier.height(12.dp))
                acciones()
            }
        }
    }
}

@Composable
private fun DialogoRechazo(
    solicitud: SolicitudHoraExtra,
    onCancelar: () -> Unit,
    onConfirmar: (String) -> Unit
) {
    var motivo by remember { mutableStateOf("") }
    var error by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onCancelar,
        icon = { Icon(Icons.Outlined.ErrorOutline, contentDescription = null) },
        title = { Text("Rechazar solicitud") },
        text = {
            Column {
                Text("Solicitud de ${solicitud.workerId}")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it; error = false },
                    label = { Text("Motivo del rechazo") },
                    isError = error,
                    supportingText = { if (error) Text("El motivo es obligatorio.") },
                    minLines = 2
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (motivo.isBlank()) error = true else onConfirmar(motivo.trim())
            }) { Text("Confirmar rechazo") }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        }
    )
}
