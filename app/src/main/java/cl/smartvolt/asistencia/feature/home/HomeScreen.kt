package cl.smartvolt.asistencia.feature.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cl.smartvolt.asistencia.domain.model.WorkerRole
import cl.smartvolt.asistencia.session.DemoAccount
import cl.smartvolt.asistencia.ui.components.ResourceStatusCard
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltMark
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Pantalla de inicio por rol.
 *
 * El obrero ve su jornada y accede a la marcación; el capataz y el
 * administrador ven además el resumen de la faena (RF-13).
 */
@Composable
fun HomeScreen(
    account: DemoAccount,
    viewModel: HomeViewModel,
    onRegistrarAsistencia: () -> Unit,
    onVerHistorial: () -> Unit,
    onAbrirPanel: () -> Unit,
    onAbrirHorasExtra: () -> Unit,
    onCerrarSesion: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        visible = true
        // Al volver de marcar asistencia el resumen debe reflejar los cambios.
        viewModel.refrescar()
    }

    val esObrero = account.role == WorkerRole.WORKER
    val esAdmin = account.role == WorkerRole.ADMIN

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
                    .widthIn(max = 540.dp)
            ) {
                EncabezadoInicio(
                    account = account,
                    onCerrarSesion = onCerrarSesion
                )

                Spacer(modifier = Modifier.height(18.dp))

                AnimatedVisibility(
                    visible = visible,
                    enter = fadeIn(tween(360)) + slideInVertically(tween(420)) { it / 10 }
                ) {
                    Column {
                        Saludo(account = account)

                        Spacer(modifier = Modifier.height(16.dp))

                        TarjetaJornada(
                            estado = estado,
                            esObrero = esObrero,
                            onRegistrarAsistencia = onRegistrarAsistencia,
                            onVerHistorial = onVerHistorial
                        )
                    }
                }

                if (!esObrero) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(500)) + slideInVertically(tween(560)) { it / 10 }
                    ) {
                        TarjetaFaena(
                            estado = estado,
                            onAbrirPanel = onAbrirPanel
                        )
                    }
                }

                if (esObrero || esAdmin) {
                    Spacer(modifier = Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = visible,
                        enter = fadeIn(tween(620)) + slideInVertically(tween(680)) { it / 10 }
                    ) {
                        TarjetaHorasExtra(
                            descripcion = if (esObrero) {
                                "Envía solicitudes y revisa su estado."
                            } else {
                                "Revisa, aprueba o rechaza solicitudes de la faena."
                            },
                            onAbrirHorasExtra = onAbrirHorasExtra
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                ResourceStatusCard(
                    icon = Icons.Outlined.CloudOff,
                    eyebrow = "Sincronización",
                    title = if (estado.pendingSync == 0) {
                        "Todo al día"
                    } else {
                        "${estado.pendingSync} marcas pendientes"
                    },
                    detail = if (estado.pendingSync == 0) {
                        "No hay marcas esperando envío al servidor."
                    } else {
                        "Guardadas en este dispositivo. Se enviarán cuando exista backend."
                    },
                    tone = if (estado.pendingSync == 0) StatusTone.OK else StatusTone.WARNING,
                    pillText = if (estado.pendingSync == 0) "Al día" else "Pendiente"
                )

                Spacer(modifier = Modifier.height(16.dp))

                DatosSesion(account = account)

                Spacer(modifier = Modifier.height(14.dp))

                OutlinedButton(
                    onClick = onCerrarSesion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Logout,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión demo")
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun EncabezadoInicio(
    account: DemoAccount,
    onCerrarSesion: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SmartVoltMark(size = 40.dp)

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "SmartVolt",
                style = MaterialTheme.typography.titleMedium
            )
            SectionLabel(text = account.roleLabel)
        }

        IconButton(onClick = onCerrarSesion) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Logout,
                contentDescription = "Cerrar sesión",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun Saludo(account: DemoAccount) {
    val nombre = account.displayName.substringBefore(" ")
    Column {
        Text(
            text = "Hola, $nombre",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = fechaLarga(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun TarjetaJornada(
    estado: HomeUiState,
    esObrero: Boolean,
    onRegistrarAsistencia: () -> Unit,
    onVerHistorial: () -> Unit
) {
    val completa = estado.dayState.hasCheckIn && estado.dayState.hasCheckOut
    val abierta = estado.dayState.hasOpenCheckIn
    val progreso by animateFloatAsState(
        targetValue = when {
            completa -> 1f
            estado.dayState.hasCheckIn -> 0.5f
            else -> 0f
        },
        animationSpec = tween(700),
        label = "progresoJornada"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionLabel(
                    text = "Jornada de hoy",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                StatusPill(
                    text = when {
                        completa -> "Completa"
                        abierta -> "En curso"
                        else -> "Sin marcas"
                    },
                    tone = when {
                        completa -> StatusTone.OK
                        abierta -> StatusTone.WARNING
                        else -> StatusTone.NEUTRAL
                    }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            AnimatedContent(
                targetState = when {
                    completa -> "Jornada cerrada"
                    abierta -> "Estás dentro de la faena"
                    else -> "Aún no marcas tu entrada"
                },
                transitionSpec = {
                    (fadeIn(tween(220)) + slideInVertically(tween(260)) { it / 2 })
                        .togetherWith(fadeOut(tween(160)) + slideOutVertically(tween(200)) { -it / 2 })
                },
                label = "estadoJornada"
            ) { titulo ->
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.headlineSmall
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = if (esObrero) {
                    "Registra tu entrada y salida con GPS y baliza Bluetooth."
                } else {
                    "Puedes marcar tu propia asistencia igual que el equipo."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            LinearProgressIndicator(
                progress = { progreso },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(50)),
                color = if (completa) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.primary
                },
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Spacer(modifier = Modifier.height(14.dp))

            Row(modifier = Modifier.fillMaxWidth()) {
                MarcaDelDia(
                    icon = Icons.AutoMirrored.Outlined.Login,
                    label = "Entrada",
                    millis = estado.entryMillis,
                    modifier = Modifier.weight(1f)
                )
                MarcaDelDia(
                    icon = Icons.AutoMirrored.Outlined.Logout,
                    label = "Salida",
                    millis = estado.exitMillis,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = onRegistrarAsistencia,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
            ) {
                Icon(
                    imageVector = if (abierta) Icons.AutoMirrored.Outlined.Logout else Icons.AutoMirrored.Outlined.Login,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = when {
                        abierta -> "Registrar salida"
                        completa -> "Ver marcas de hoy"
                        else -> "Registrar entrada"
                    },
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = onVerHistorial,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.History,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Ver historial")
            }
        }
    }
}

@Composable
private fun MarcaDelDia(
    icon: ImageVector,
    label: String,
    millis: Long?,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
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
        Spacer(modifier = Modifier.width(8.dp))
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
private fun TarjetaFaena(
    estado: HomeUiState,
    onAbrirPanel: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Groups,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Faena en vivo",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricaFaena(
                    valor = estado.teamPresent,
                    etiqueta = "Presentes",
                    modifier = Modifier.weight(1f)
                )
                MetricaFaena(
                    valor = estado.teamCheckedOut,
                    etiqueta = "Con salida",
                    modifier = Modifier.weight(1f)
                )
                MetricaFaena(
                    valor = estado.teamMarks,
                    etiqueta = "Marcas",
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            Button(
                onClick = onAbrirPanel,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(
                    imageVector = Icons.Outlined.Badge,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Abrir panel de faena")
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null
                )
            }
        }
    }
}

@Composable
private fun MetricaFaena(
    valor: Int,
    etiqueta: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
                RoundedCornerShape(12.dp)
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = valor.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = etiqueta,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun TarjetaHorasExtra(
    descripcion: String,
    onAbrirHorasExtra: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Horas extra",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = descripcion,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onAbrirHorasExtra) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = "Abrir horas extra"
                )
            }
        }
    }
}

@Composable
private fun DatosSesion(account: DemoAccount) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            SectionLabel(text = "Sesión de demostración")
            Spacer(modifier = Modifier.height(10.dp))
            FilaDato(icon = Icons.Outlined.Person, texto = account.email)
            FilaDato(icon = Icons.Outlined.Badge, texto = account.workerId)
            FilaDato(icon = Icons.Outlined.Work, texto = account.siteName)
            FilaDato(icon = Icons.Outlined.Schedule, texto = account.shiftLabel)
            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.outlineVariant
            )
            Text(
                text = "Datos ficticios para la defensa. Sin backend ni datos personales reales.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
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
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = texto,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun hora(millis: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(millis))

private fun fechaLarga(): String =
    SimpleDateFormat("EEEE d 'de' MMMM", Locale.forLanguageTag("es-CL"))
        .format(Date())
        .replaceFirstChar { it.uppercase() }
