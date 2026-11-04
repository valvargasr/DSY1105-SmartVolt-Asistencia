package cl.smartvolt.asistencia.feature.overtime

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import cl.smartvolt.asistencia.domain.model.OvertimeApproval
import cl.smartvolt.asistencia.domain.model.OvertimeStatus
import cl.smartvolt.asistencia.domain.model.WorkerRole
import cl.smartvolt.asistencia.session.DemoAccount
import cl.smartvolt.asistencia.ui.components.EmptyState
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Date
import java.text.SimpleDateFormat
import java.util.Locale

/** Muestra las funciones de horas extra según el rol activo. */
@Composable
fun OvertimeScreen(
    viewModel: OvertimeViewModel,
    account: DemoAccount,
    onVolver: () -> Unit
) {
    val solicitudes by viewModel.requests.collectAsState()
    val mensaje by viewModel.operationMessage.collectAsState()
    val guardando by viewModel.isSaving.collectAsState()

    LaunchedEffect(account.workerId) { viewModel.limpiarMensaje() }

    when (account.role) {
        WorkerRole.WORKER -> SolicitudObreroScreen(
            viewModel = viewModel,
            account = account,
            solicitudes = solicitudes.filter { it.workerId == account.workerId }
                .sortedByDescending { it.createdAtMillis },
            mensaje = mensaje,
            guardando = guardando,
            onVolver = onVolver
        )
        WorkerRole.ADMIN -> AdministrarHorasExtraScreen(
            viewModel = viewModel,
            account = account,
            solicitudes = solicitudes.filter { it.siteId == account.siteId },
            mensaje = mensaje,
            onVolver = onVolver
        )
        else -> AccesoHorasExtraDenegado(onVolver)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SolicitudObreroScreen(
    viewModel: OvertimeViewModel,
    account: DemoAccount,
    solicitudes: List<OvertimeApproval>,
    mensaje: String?,
    guardando: Boolean,
    onVolver: () -> Unit
) {
    val hoy = remember { LocalDate.now() }
    var fechaMillis by rememberSaveable {
        mutableStateOf(hoy.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli())
    }
    var desde by rememberSaveable { mutableStateOf("18:00") }
    var hasta by rememberSaveable { mutableStateOf("20:00") }
    var motivo by rememberSaveable { mutableStateOf("") }
    var errores by remember { mutableStateOf(OvertimeFormErrors()) }
    var mostrarCalendario by remember { mutableStateOf(false) }

    val fecha = Instant.ofEpochMilli(fechaMillis).atZone(ZoneOffset.UTC).toLocalDate()
    val formatter = remember { DateTimeFormatter.ofPattern("dd/MM/yyyy") }

    LaunchedEffect(mensaje) {
        if (mensaje?.startsWith("Solicitud enviada") == true) {
            motivo = ""
        }
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
                    subtitle = "Trabajador · ${account.siteName}",
                    onBack = onVolver
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Completa los datos de las horas adicionales. El administrador revisará tu solicitud.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = fecha.format(formatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Fecha") },
                    isError = errores.date.isNotEmpty(),
                    supportingText = { if (errores.date.isNotEmpty()) Text(errores.date) },
                    trailingIcon = {
                        IconButton(onClick = { mostrarCalendario = true }) {
                            Icon(Icons.Outlined.CalendarMonth, contentDescription = "Elegir fecha")
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = desde,
                        onValueChange = { desde = it; errores = errores.copy(start = "", end = "") },
                        label = { Text("Desde (HH:mm)") },
                        placeholder = { Text("18:00") },
                        singleLine = true,
                        isError = errores.start.isNotEmpty(),
                        supportingText = { if (errores.start.isNotEmpty()) Text(errores.start) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = hasta,
                        onValueChange = { hasta = it; errores = errores.copy(end = "") },
                        label = { Text("Hasta (HH:mm)") },
                        placeholder = { Text("20:00") },
                        singleLine = true,
                        isError = errores.end.isNotEmpty(),
                        supportingText = { if (errores.end.isNotEmpty()) Text(errores.end) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii),
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = motivo,
                    onValueChange = { motivo = it; errores = errores.copy(reason = "") },
                    label = { Text("Motivo") },
                    placeholder = { Text("Ej.: trabajo adicional en obra") },
                    minLines = 2,
                    maxLines = 4,
                    isError = errores.reason.isNotEmpty(),
                    supportingText = { if (errores.reason.isNotEmpty()) Text(errores.reason) },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        val validacion = OvertimeFormValidator.validate(fecha, desde, hasta, motivo)
                        errores = validacion
                        if (validacion.isValid) {
                            val zone = ZoneId.systemDefault()
                            val inicio = fecha.atTime(LocalTime.parse(desde.trim()))
                                .atZone(zone).toInstant().toEpochMilli()
                            val termino = fecha.atTime(LocalTime.parse(hasta.trim()))
                                .atZone(zone).toInstant().toEpochMilli()
                            viewModel.solicitar(
                                requesterRole = account.role,
                                workerId = account.workerId,
                                siteId = account.siteId,
                                date = fecha,
                                startMillis = inicio,
                                endMillis = termino,
                                reason = motivo
                            )
                            errores = OvertimeFormErrors()
                        }
                    },
                    enabled = !guardando,
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Icon(Icons.Outlined.Timer, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(if (guardando) "Guardando..." else "Enviar solicitud")
                }

                if (!mensaje.isNullOrBlank()) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = mensaje,
                        color = if (mensaje.startsWith("No se pudo")) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }

                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SectionLabel("Mis solicitudes")
                    Spacer(Modifier.weight(1f))
                    StatusPill(solicitudes.size.toString(), if (solicitudes.isEmpty()) StatusTone.NEUTRAL else StatusTone.OK)
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
                    solicitudes.forEach { solicitud -> TarjetaEstadoSolicitud(solicitud) }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Volver al inicio")
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
                    errores = errores.copy(date = "")
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
private fun AdministrarHorasExtraScreen(
    viewModel: OvertimeViewModel,
    account: DemoAccount,
    solicitudes: List<OvertimeApproval>,
    mensaje: String?,
    onVolver: () -> Unit
) {
    var rechazoDe by remember { mutableStateOf<OvertimeApproval?>(null) }
    val pendientes = solicitudes.filter { it.status == OvertimeStatus.PENDING }
    val decididas = solicitudes.filter { it.status != OvertimeStatus.PENDING }
        .sortedByDescending { it.createdAtMillis }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                SmartVoltTopBar(
                    title = "Horas extra",
                    subtitle = "Administración · ${account.siteName}",
                    onBack = onVolver
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.WarningAmber, contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "Autorizar horas extra no significa pagarlas. Aquí solo se registra la decisión operativa.",
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
                    StatusPill(pendientes.size.toString(), if (pendientes.isEmpty()) StatusTone.NEUTRAL else StatusTone.WARNING)
                }
                Spacer(Modifier.height(8.dp))
                AnimatedVisibility(visible = pendientes.isEmpty(), enter = fadeIn(), exit = fadeOut()) {
                    EmptyState(
                        icon = Icons.Outlined.Schedule,
                        title = "Sin solicitudes pendientes",
                        description = "Las solicitudes enviadas por los trabajadores aparecerán aquí."
                    )
                }
                Column(modifier = Modifier.animateContentSize(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    pendientes.forEach { solicitud ->
                        TarjetaSolicitudAdmin(
                            solicitud = solicitud,
                            onAprobar = { viewModel.aprobar(solicitud.approvalId, account.workerId, account.role) },
                            onRechazar = { rechazoDe = solicitud }
                        )
                    }
                }
                if (decididas.isNotEmpty()) {
                    Spacer(Modifier.height(20.dp))
                    SectionLabel("Solicitudes resueltas")
                    Spacer(Modifier.height(8.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        decididas.forEach { TarjetaSolicitudAdmin(it, {}, {}) }
                    }
                }
                Spacer(Modifier.height(20.dp))
                OutlinedButton(onClick = onVolver, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Text("Volver al inicio")
                }
            }
        }
    }

    rechazoDe?.let { solicitud ->
        DialogoRechazo(
            solicitud = solicitud,
            onCancelar = { rechazoDe = null },
            onConfirmar = { motivo ->
                viewModel.rechazar(solicitud.approvalId, account.workerId, account.role, motivo)
                rechazoDe = null
            }
        )
    }
}

@Composable
private fun TarjetaEstadoSolicitud(solicitud: OvertimeApproval) {
    TarjetaBaseSolicitud(solicitud = solicitud, mostrarTrabajador = false)
}

@Composable
private fun TarjetaSolicitudAdmin(
    solicitud: OvertimeApproval,
    onAprobar: () -> Unit,
    onRechazar: () -> Unit
) {
    TarjetaBaseSolicitud(
        solicitud = solicitud,
        mostrarTrabajador = true,
        acciones = if (solicitud.status == OvertimeStatus.PENDING) {
            {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onAprobar, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.CheckCircle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Aprobar")
                    }
                    OutlinedButton(onClick = onRechazar, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(5.dp))
                        Text("Rechazar")
                    }
                }
            }
        } else null
    )
}

@Composable
private fun TarjetaBaseSolicitud(
    solicitud: OvertimeApproval,
    mostrarTrabajador: Boolean,
    acciones: (@Composable () -> Unit)? = null
) {
    val tone = when (solicitud.status) {
        OvertimeStatus.PENDING -> StatusTone.WARNING
        OvertimeStatus.APPROVED -> StatusTone.OK
        OvertimeStatus.REJECTED -> StatusTone.DANGER
        OvertimeStatus.CANCELLED -> StatusTone.NEUTRAL
    }
    val label = when (solicitud.status) {
        OvertimeStatus.PENDING -> "Pendiente"
        OvertimeStatus.APPROVED -> "Aprobada"
        OvertimeStatus.REJECTED -> "Rechazada"
        OvertimeStatus.CANCELLED -> "Cancelada"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    if (mostrarTrabajador) Text(solicitud.workerId, style = MaterialTheme.typography.titleSmall)
                    Text(solicitud.siteId, style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                StatusPill(label, tone)
            }
            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(8.dp))
            Text("Fecha: ${solicitud.date}", style = MaterialTheme.typography.bodyMedium)
            Text(
                "Horario: ${hora(solicitud.authorizedStartMillis)}–${hora(solicitud.authorizedEndMillis)}",
                style = MaterialTheme.typography.bodyMedium
            )
            solicitud.reason?.takeIf { it.isNotBlank() }?.let {
                Spacer(Modifier.height(4.dp))
                Text("Motivo / comentario: $it", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (acciones != null) {
                Spacer(Modifier.height(12.dp))
                acciones()
            }
        }
    }
}

@Composable
private fun AccesoHorasExtraDenegado(onVolver: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Outlined.ErrorOutline, contentDescription = null,
                tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(42.dp))
            Spacer(Modifier.height(12.dp))
            Text("Acceso no disponible", style = MaterialTheme.typography.titleLarge)
            Text("Esta sección está disponible para obreros y administradores según sus funciones.",
                style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onVolver) { Text("Volver") }
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
        dismissButton = { TextButton(onClick = onCancelar) { Text("Cancelar") } }
    )
}

private fun hora(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))
