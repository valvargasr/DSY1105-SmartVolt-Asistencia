package cl.smartvolt.asistencia.ui.view

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Login
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.ui.components.MapaUbicacion
import cl.smartvolt.asistencia.ui.components.ResourceStatusCard
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import cl.smartvolt.asistencia.util.EstadoBaliza
import cl.smartvolt.asistencia.util.EstadoPermiso
import cl.smartvolt.asistencia.util.EstadoUbicacion
import cl.smartvolt.asistencia.util.Fechas
import cl.smartvolt.asistencia.util.Permisos
import cl.smartvolt.asistencia.util.Sesion
import cl.smartvolt.asistencia.viewmodel.AsistenciaUiState
import cl.smartvolt.asistencia.viewmodel.AsistenciaViewModel
import java.util.Locale

@Composable
// Pantalla para verificar presencia y marcar entrada o salida
fun AsistenciaView(
    viewModel: AsistenciaViewModel = viewModel()
) {
    // Estado de permisos, ubicacion y baliza
    val estado by viewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val actividad = contexto as? Activity

    // Pide permiso de notificaciones
    val lanzadorNotificaciones = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { }

    val solicitarNotificaciones: () -> Unit = {
        Permisos.permisoNotificaciones()?.let { permiso ->
            if (!Permisos.estaConcedido(contexto, permiso)) {
                lanzadorNotificaciones.launch(permiso)
            }
        }
    }

    // Pide permisos de ubicacion y Bluetooth
    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        // si la ubicación quedó ok verificamos altiro, el resto se avisa en pantalla
        val ubicacionOk = Permisos.ubicacionConcedida(contexto)
        val bloqueado = resultados.filterValues { !it }.keys.any { permiso ->
            actividad?.shouldShowRequestPermissionRationale(permiso) == false
        }
        viewModel.actualizarPermiso(ubicacionOk, bloqueado && !ubicacionOk)
        if (ubicacionOk) {
            viewModel.verificar()
            solicitarNotificaciones()
        }
    }

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
                    .widthIn(max = 520.dp)
            ) {
                // ENCABEZADO
                SmartVoltTopBar(
                    title = "Asistencia",
                    subtitle = "${Sesion.cuenta?.roleLabel ?: "Demo"} - ${estado.faena.name}"
                )

                Spacer(Modifier.height(16.dp))
                // RESUMEN DE PRESENCIA
                ResumenPresencia(estado)

                if (estado.verificando) {
                    Spacer(Modifier.height(12.dp))
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                // PERMISOS
                ResourceStatusCard(
                    icon = Icons.Outlined.Security,
                    eyebrow = "Paso 1",
                    title = "Permisos",
                    detail = textoPermiso(estado.permiso),
                    tone = tonoPermiso(estado.permiso),
                    pillText = etiquetaPermiso(estado.permiso)
                )

                Spacer(Modifier.height(10.dp))
                // UBICACION
                ResourceStatusCard(
                    icon = Icons.Outlined.LocationOn,
                    eyebrow = "Paso 2",
                    title = "Ubicación GPS",
                    detail = estado.mensajeUbicacion,
                    tone = tonoUbicacion(estado.ubicacion?.estado),
                    pillText = etiquetaUbicacion(estado.ubicacion?.estado),
                    extra = estado.ubicacion?.let { lectura ->
                        if (lectura.estado == EstadoUbicacion.LISTA) {
                            val lat = String.format(Locale.ROOT, "%.5f", lectura.latitude ?: 0.0)
                            val lon = String.format(Locale.ROOT, "%.5f", lectura.longitude ?: 0.0)
                            "Lat $lat - Lon $lon"
                        } else {
                            null
                        }
                    }
                )

                if (estado.ubicacion?.estado == EstadoUbicacion.LISTA) {
                    Spacer(Modifier.height(10.dp))
                    AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn(initialScale = 0.96f)) {
                        // MAPA
                        MapaUbicacion(
                            latitude = estado.ubicacion?.latitude,
                            longitude = estado.ubicacion?.longitude,
                            accuracyMeters = estado.ubicacion?.accuracyMeters
                        )
                    }
                }

                Spacer(Modifier.height(10.dp))
                // BALIZA
                ResourceStatusCard(
                    icon = Icons.Outlined.Bluetooth,
                    eyebrow = "Paso 3",
                    title = "Baliza Bluetooth",
                    detail = estado.mensajeBaliza,
                    tone = tonoBaliza(estado.baliza?.estado),
                    pillText = etiquetaBaliza(estado.baliza?.estado),
                    extra = estado.baliza?.let { baliza ->
                        if (baliza.estado == EstadoBaliza.ENCONTRADA) {
                            "${baliza.formato ?: "BLE"} - ${baliza.idVisible} - RSSI ${baliza.rssi}"
                        } else {
                            null
                        }
                    }
                )

                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // DATOS DE LA FAENA
                        SectionLabel(text = "Faena")
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = estado.faena.name,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = estado.faena.address,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "Radio ${estado.faena.radiusMeters} m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // AVISO DE UBICACION SIMULADA
                if (estado.ubicacion?.simulada == true) {
                    Spacer(Modifier.height(10.dp))
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WarningAmber,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "El sistema informó una ubicación simulada. " +
                                    "La marca quedará en revisión.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onTertiaryContainer
                            )
                        }
                    }
                }

                Spacer(Modifier.height(18.dp))
                // BOTON VERIFICAR
                Button(
                    onClick = {
                        // un solo botón: pide los permisos que falten y si ya están verifica
                        val permisos = Permisos.permisosPresencia()
                        val faltantes = permisos.filterNot { Permisos.estaConcedido(contexto, it) }
                        if (faltantes.isEmpty()) {
                            viewModel.actualizarPermiso(concedido = true, bloqueado = false)
                            viewModel.verificar()
                            solicitarNotificaciones()
                        } else {
                            lanzadorPermisos.launch(faltantes.toTypedArray())
                        }
                    },
                    enabled = !estado.verificando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    if (estado.verificando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text("Verificando presencia...")
                    } else {
                        Icon(Icons.Outlined.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (estado.ubicacion == null && estado.baliza == null) {
                                "Verificar presencia"
                            } else {
                                "Verificar de nuevo"
                            }
                        )
                    }
                }

                if (estado.permiso == EstadoPermiso.BLOQUEADO ||
                    estado.baliza?.estado == EstadoBaliza.SIN_PERMISO
                ) {
                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            contexto.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", contexto.packageName, null)
                                )
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                    ) {
                        Icon(Icons.Outlined.ErrorOutline, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Abrir ajustes de la app")
                    }
                }

                Spacer(Modifier.height(16.dp))
                // BOTON ENTRADA
                Button(
                    onClick = viewModel::registrarEntrada,
                    enabled = !estado.guardando,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary,
                        contentColor = MaterialTheme.colorScheme.onSecondary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                ) {
                    Icon(Icons.Outlined.Login, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            estado.guardando -> "Guardando..."
                            estado.jornada.tieneEntrada -> "Entrada ya registrada"
                            else -> "Registrar entrada"
                        }
                    )
                }

                Spacer(Modifier.height(8.dp))
                // BOTON SALIDA
                OutlinedButton(
                    onClick = viewModel::registrarSalida,
                    enabled = !estado.guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(Icons.Outlined.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            estado.guardando -> "Guardando..."
                            estado.jornada.tieneSalida -> "Salida ya registrada"
                            else -> "Registrar salida"
                        }
                    )
                }

                val ayuda = when {
                    !estado.puedeRegistrar ->
                        "Para marcar necesitas ubicación lista, baliza detectada y estar dentro del radio."
                    estado.jornada.completa ->
                        "La jornada de hoy ya tiene entrada y salida registradas."
                    estado.jornada.tieneEntrada ->
                        "La entrada de hoy está registrada. Puedes registrar la salida."
                    else -> null
                }
                if (ayuda != null) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                MaterialTheme.colorScheme.surfaceVariant,
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 9.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = ayuda,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(18.dp))
                // TARJETA DE SINCRONIZACION
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    color = if (estado.pendientes == 0) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.primaryContainer
                    },
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (estado.pendientes == 0) {
                                Icons.Outlined.CloudDone
                            } else {
                                Icons.Outlined.CloudOff
                            },
                            contentDescription = null
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Sincronización",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = if (estado.pendientes == 0) {
                                    "Sin registros pendientes."
                                } else {
                                    "Registros pendientes: ${estado.pendientes}."
                                },
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        StatusPill(
                            text = if (estado.pendientes == 0) "Al día" else "Pendiente",
                            tone = if (estado.pendientes == 0) StatusTone.OK else StatusTone.WARNING
                        )
                    }
                }

                // MENSAJE DE RESULTADO
                AnimatedVisibility(visible = estado.mensaje != null) {
                    Column {
                        Spacer(Modifier.height(14.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(15.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = if (estado.ultimaMarca != null) {
                                        Icons.Outlined.CheckCircle
                                    } else {
                                        Icons.Outlined.Info
                                    },
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = estado.mensaje.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }

                if (estado.ultimaMarca != null) {
                    Spacer(Modifier.height(12.dp))
                    val marca = estado.ultimaMarca!!
                    Text(
                        text = "Último registro: ${if (marca.esEntrada) "entrada" else "salida"} " +
                            "a las ${Fechas.hora(marca.timestampMillis)}.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
// Tarjeta con el resumen de la verificacion
private fun ResumenPresencia(estado: AsistenciaUiState) {
    val listo = estado.puedeRegistrar
    val fondo = if (listo) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val contenido = if (listo) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = fondo),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(
                        if (listo) MaterialTheme.colorScheme.secondary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.25f),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (listo) Icons.Outlined.CheckCircle
                    else Icons.Outlined.LocationSearching,
                    contentDescription = null,
                    tint = if (listo) MaterialTheme.colorScheme.onSecondary
                    else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                SectionLabel(
                    text = if (listo) "Presencia confirmada" else "Verificación pendiente",
                    color = contenido.copy(alpha = 0.75f)
                )
                Spacer(Modifier.height(3.dp))
                Text(
                    text = if (listo) "Todo listo para marcar" else "Verifica tu ubicación y baliza",
                    style = MaterialTheme.typography.titleMedium,
                    color = contenido
                )
                Text(
                    text = if (listo) {
                        "Ubicación y proximidad detectadas en este dispositivo."
                    } else {
                        "Necesitas ambos recursos para registrar una marca."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = contenido.copy(alpha = 0.8f)
                )
            }
        }
    }
}

// Texto que explica el estado del permiso
private fun textoPermiso(estado: EstadoPermiso): String = when (estado) {
    EstadoPermiso.NO_SOLICITADO -> "Todavía no se solicitaron los permisos."
    EstadoPermiso.CONCEDIDO -> "Permiso de ubicación concedido."
    EstadoPermiso.DENEGADO -> "Permisos denegados. Se pueden volver a solicitar."
    EstadoPermiso.BLOQUEADO -> "Permisos bloqueados. Habilítalos en los ajustes."
}

// Etiqueta corta del permiso
private fun etiquetaPermiso(estado: EstadoPermiso): String = when (estado) {
    EstadoPermiso.CONCEDIDO -> "OK"
    EstadoPermiso.NO_SOLICITADO -> "Pendiente"
    EstadoPermiso.DENEGADO -> "Denegados"
    EstadoPermiso.BLOQUEADO -> "Bloqueados"
}

// Color del permiso segun su estado
private fun tonoPermiso(estado: EstadoPermiso): StatusTone = when (estado) {
    EstadoPermiso.CONCEDIDO -> StatusTone.OK
    EstadoPermiso.NO_SOLICITADO -> StatusTone.NEUTRAL
    EstadoPermiso.DENEGADO -> StatusTone.WARNING
    EstadoPermiso.BLOQUEADO -> StatusTone.DANGER
}

// Etiqueta corta de la ubicacion
private fun etiquetaUbicacion(estado: EstadoUbicacion?): String = when (estado) {
    EstadoUbicacion.LISTA -> "Listo"
    null -> "Sin datos"
    EstadoUbicacion.SIN_PERMISO -> "Sin permiso"
    EstadoUbicacion.UBICACION_APAGADA -> "GPS apagado"
    EstadoUbicacion.TIEMPO_AGOTADO -> "Sin señal"
    EstadoUbicacion.NO_DISPONIBLE -> "No disponible"
    EstadoUbicacion.ERROR -> "Error"
}

// Color de la ubicacion segun su estado
private fun tonoUbicacion(estado: EstadoUbicacion?): StatusTone = when (estado) {
    EstadoUbicacion.LISTA -> StatusTone.OK
    null -> StatusTone.NEUTRAL
    EstadoUbicacion.SIN_PERMISO,
    EstadoUbicacion.UBICACION_APAGADA -> StatusTone.WARNING
    EstadoUbicacion.TIEMPO_AGOTADO,
    EstadoUbicacion.NO_DISPONIBLE,
    EstadoUbicacion.ERROR -> StatusTone.DANGER
}

// Etiqueta corta de la baliza
private fun etiquetaBaliza(estado: EstadoBaliza?): String = when (estado) {
    EstadoBaliza.ENCONTRADA -> "Detectada"
    null -> "Sin datos"
    EstadoBaliza.NO_ENCONTRADA -> "No encontrada"
    EstadoBaliza.BLUETOOTH_APAGADO -> "Bluetooth off"
    EstadoBaliza.TIEMPO_AGOTADO -> "Sin señal"
    EstadoBaliza.SIN_PERMISO -> "Sin permiso"
    EstadoBaliza.NO_SOPORTADA -> "No soportado"
    EstadoBaliza.ERROR -> "Error"
}

// Color de la baliza segun su estado
private fun tonoBaliza(estado: EstadoBaliza?): StatusTone = when (estado) {
    EstadoBaliza.ENCONTRADA -> StatusTone.OK
    null -> StatusTone.NEUTRAL
    EstadoBaliza.NO_ENCONTRADA,
    EstadoBaliza.BLUETOOTH_APAGADO,
    EstadoBaliza.TIEMPO_AGOTADO -> StatusTone.WARNING
    EstadoBaliza.SIN_PERMISO,
    EstadoBaliza.NO_SOPORTADA,
    EstadoBaliza.ERROR -> StatusTone.DANGER
}
