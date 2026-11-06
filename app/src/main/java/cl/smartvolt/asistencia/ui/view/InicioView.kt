package cl.smartvolt.asistencia.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.modelo.DemoAccount
import cl.smartvolt.asistencia.modelo.WorkerRole
import cl.smartvolt.asistencia.ui.components.ResourceStatusCard
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltMark
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import cl.smartvolt.asistencia.util.Fechas
import cl.smartvolt.asistencia.util.Fechas.hora
import cl.smartvolt.asistencia.viewmodel.InicioUiState
import cl.smartvolt.asistencia.viewmodel.InicioViewModel

@Composable
// Pantalla de inicio con el resumen del dia y los accesos
fun InicioView(
    cuenta: DemoAccount,
    onIrAsistencia: () -> Unit,
    onIrHistorial: () -> Unit,
    onAbrirPanel: () -> Unit,
    onAbrirFaena: () -> Unit,
    onAbrirReporte: () -> Unit,
    onAbrirHorasExtra: () -> Unit,
    viewModel: InicioViewModel = viewModel()
) {
    // Estado que viene del ViewModel
    val estado by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { visible = true }

    // Roles para decidir que accesos mostrar
    val esObrero = cuenta.role == WorkerRole.WORKER
    val esAdmin = cuenta.role == WorkerRole.ADMIN

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
                    .widthIn(max = 540.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // ENCABEZADO
                    SmartVoltMark(size = 40.dp)
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SmartVolt", style = MaterialTheme.typography.titleMedium)
                        SectionLabel(text = cuenta.roleLabel)
                    }
                }

                Spacer(Modifier.height(18.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn() + slideInVertically { it / 10 }
                ) {
                    // SALUDO Y FECHA
                    Column {
                        Text(
                            text = "Hola, ${cuenta.displayName.substringBefore(" ")}",
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = Fechas.fechaLarga(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(16.dp))
                        // TARJETA DE JORNADA
                        TarjetaJornada(
                            estado = estado,
                            esObrero = esObrero,
                            siteName = estado.faena.name,
                            puedeEditarFaena = esAdmin,
                            onEditarFaena = onAbrirFaena,
                            onRegistrarAsistencia = onIrAsistencia,
                            onVerHistorial = onIrHistorial
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                // TARJETA DE SINCRONIZACION
                ResourceStatusCard(
                    icon = Icons.Outlined.CloudOff,
                    eyebrow = "Sincronización",
                    title = if (estado.pendientes == 0) {
                        "Todo al día"
                    } else {
                        "${estado.pendientes} marcas pendientes"
                    },
                    detail = if (estado.pendientes == 0) {
                        "No hay marcas esperando envío al servidor."
                    } else {
                        "Guardadas en este dispositivo. Se enviarán cuando exista backend."
                    },
                    tone = if (estado.pendientes == 0) StatusTone.OK else StatusTone.WARNING,
                    pillText = if (estado.pendientes == 0) "Al día" else "Pendiente"
                )

                Spacer(Modifier.height(16.dp))
                // LISTA DE ACCESOS
                SectionLabel(text = "Accesos")
                Spacer(Modifier.height(8.dp))

                AccesoCard(
                    icon = Icons.Outlined.LocationOn,
                    titulo = "Registrar asistencia",
                    detalle = "GPS, baliza y geocerca de la faena.",
                    onClick = onIrAsistencia
                )
                AccesoCard(
                    icon = Icons.Outlined.History,
                    titulo = "Historial de marcas",
                    detalle = "Revisa tus registros y su estado.",
                    onClick = onIrHistorial
                )
                if (cuenta.role == WorkerRole.SUPERVISOR || esAdmin) {
                    AccesoCard(
                        icon = Icons.Outlined.Groups,
                        titulo = "Panel de faena",
                        detalle = "Presentes, salidas y marcas del día.",
                        onClick = onAbrirPanel
                    )
                }
                if (esAdmin) {
                    AccesoCard(
                        icon = Icons.Outlined.Edit,
                        titulo = "Editar faena",
                        detalle = "Nombre, centro y radio.",
                        onClick = onAbrirFaena
                    )
                    AccesoCard(
                        icon = Icons.Outlined.Description,
                        titulo = "Registro del mes",
                        detalle = "Horas, CSV y PDF.",
                        onClick = onAbrirReporte
                    )
                }
                if (esObrero || esAdmin) {
                    AccesoCard(
                        icon = Icons.Outlined.Timer,
                        titulo = "Horas extra",
                        detalle = if (esObrero) {
                            "Envía solicitudes y revisa su estado."
                        } else {
                            "Revisa, aprueba o rechaza solicitudes."
                        },
                        onClick = onAbrirHorasExtra
                    )
                }

                Spacer(Modifier.height(12.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(17.dp)) {
                        // DATOS DE LA SESION
                        SectionLabel(text = "Sesión de demostración")
                        Spacer(Modifier.height(10.dp))
                        FilaDato(Icons.Outlined.Badge, cuenta.email)
                        FilaDato(Icons.Outlined.Badge, cuenta.workerId)
                        FilaDato(Icons.Outlined.LocationOn, cuenta.siteName)
                        FilaDato(Icons.Outlined.AccessTime, cuenta.shiftLabel)
                    }
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
// Tarjeta con el estado de la jornada y los botones de marca
private fun TarjetaJornada(
    estado: InicioUiState,
    esObrero: Boolean,
    siteName: String,
    puedeEditarFaena: Boolean,
    onEditarFaena: () -> Unit,
    onRegistrarAsistencia: () -> Unit,
    onVerHistorial: () -> Unit
) {
    val jornada = estado.jornada
    val progreso = when {
        jornada.completa -> 1f
        jornada.tieneEntrada -> 0.5f
        else -> 0f
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(text = "Jornada de hoy")
                Spacer(Modifier.weight(1f))
                StatusPill(
                    text = when {
                        jornada.completa -> "Completa"
                        jornada.entradaAbierta -> "En curso"
                        else -> "Sin marcas"
                    },
                    tone = when {
                        jornada.completa -> StatusTone.OK
                        jornada.entradaAbierta -> StatusTone.WARNING
                        else -> StatusTone.NEUTRAL
                    }
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = when {
                    jornada.completa -> "Jornada cerrada"
                    jornada.entradaAbierta -> "Estás dentro de la faena"
                    else -> "Aún no marcas tu entrada"
                },
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = if (esObrero) {
                    "Registra tu entrada y salida de forma segura."
                } else {
                    "Puedes marcar tu propia asistencia igual que el equipo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(12.dp))
            // FAENA ACTUAL
            Surface(
                shape = RoundedCornerShape(13.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(9.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Faena actual",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = siteName,
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                    if (puedeEditarFaena) {
                        androidx.compose.material3.IconButton(onClick = onEditarFaena) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar faena",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(15.dp))
            // PROGRESO DE LA JORNADA
            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (jornada.completa) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(Modifier.height(14.dp))
            // MARCAS DE ENTRADA Y SALIDA
            Row(modifier = Modifier.fillMaxWidth()) {
                MarcaDelDia(
                    icon = Icons.Outlined.Login,
                    label = "Entrada",
                    millis = jornada.entrada,
                    modifier = Modifier.weight(1f)
                )
                MarcaDelDia(
                    icon = Icons.Outlined.Logout,
                    label = "Salida",
                    millis = jornada.salida,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(16.dp))
            // BOTON PRINCIPAL
            Button(
                onClick = onRegistrarAsistencia,
                colors = if (esObrero) {
                    ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    )
                } else {
                    ButtonDefaults.buttonColors()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
            ) {
                Icon(
                    imageVector = if (jornada.entradaAbierta) Icons.Outlined.Logout
                    else Icons.Outlined.Login,
                    contentDescription = null
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = when {
                        jornada.entradaAbierta -> "Registrar salida"
                        jornada.completa -> "Ver marcas de hoy"
                        else -> "Registrar entrada"
                    },
                    style = MaterialTheme.typography.titleMedium
                )
            }

            Spacer(Modifier.height(8.dp))
            // BOTON DE HISTORIAL
            OutlinedButton(
                onClick = onVerHistorial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(Icons.Outlined.History, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ver historial")
            }
        }
    }
}

@Composable
// Muestra una marca del dia con su hora
private fun MarcaDelDia(
    icon: ImageVector,
    label: String,
    millis: Long?,
    modifier: Modifier = Modifier
) {
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Column {
            SectionLabel(text = label)
            Text(
                text = millis?.let { hora(it) } ?: "--:--",
                style = MaterialTheme.typography.titleMedium
            )
        }
    }
}

@Composable
// Tarjeta de acceso a otra seccion
private fun AccesoCard(
    icon: ImageVector,
    titulo: String,
    detalle: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        onClick = onClick
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        MaterialTheme.colorScheme.primaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = detalle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
// Fila con icono y un dato corto
private fun FilaDato(icon: ImageVector, texto: String) {
    Row(
        modifier = Modifier.padding(vertical = 3.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
