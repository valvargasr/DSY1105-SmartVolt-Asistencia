package cl.smartvolt.asistencia.feature.attendance

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Login
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Bluetooth
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.LocationSearching
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanStatus
import cl.smartvolt.asistencia.native.location.LocationStatus
import cl.smartvolt.asistencia.native.permissions.PermissionChecker
import cl.smartvolt.asistencia.native.permissions.PermissionStatus
import cl.smartvolt.asistencia.ui.components.MapboxLocationCard
import cl.smartvolt.asistencia.ui.components.ResourceStatusCard
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.ui.components.StatusPill
import cl.smartvolt.asistencia.ui.components.StatusTone
import java.util.Locale
import kotlinx.coroutines.delay

@Composable
fun PresenciaScreen(
    workerId: String,
    siteId: String,
    siteName: String,
    roleLabel: String,
    onVolver: () -> Unit = {},
    onVerHistorial: () -> Unit = {},
    presenciaViewModel: PresenciaViewModel = viewModel(
        key = "presencia-$workerId",
        factory = PresenciaViewModel.factory(workerId = workerId, siteId = siteId)
    )
) {
    val estado by presenciaViewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val actividad = contexto as? Activity

    // El aviso de marca duplicada se cierra solo para no tapar el formulario.
    LaunchedEffect(estado.duplicateNotice) {
        if (estado.duplicateNotice != null) {
            delay(7_000)
            presenciaViewModel.onDescartarDuplicado()
        }
    }

    // Las notificaciones se solicitan por separado: no son requisito para
    // registrar asistencia y su rechazo no debe bloquear la marcación.
    val lanzadorNotificaciones = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        // El registro funciona igual aunque el usuario no acepte las notificaciones.
    }

    val solicitarNotificaciones: () -> Unit = {
        PermissionChecker.notificationPermission()?.let { permiso ->
            if (!PermissionChecker.isGranted(contexto, permiso)) {
                lanzadorNotificaciones.launch(permiso)
            }
        }
    }

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val ubicacionConcedida = PermissionChecker.isLocationGranted(contexto)
        val denegadoPermanente = resultados
            .filterValues { concedido -> !concedido }
            .keys
            .any { permiso ->
                actividad?.shouldShowRequestPermissionRationale(permiso) == false
            }

        presenciaViewModel.onPermisosResultado(
            concedidos = ubicacionConcedida,
            denegadoPermanente = denegadoPermanente && !ubicacionConcedida
        )

        // Se verifica aunque Bluetooth haya quedado denegado: así la tarjeta
        // GPS sí muestra el resultado real y la baliza explica su propio error.
        if (ubicacionConcedida) {
            presenciaViewModel.onVerificarPresencia()
            solicitarNotificaciones()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        .widthIn(max = 520.dp)
                ) {
                    SmartVoltTopBar(
                        title = "Registrar asistencia",
                        subtitle = "$roleLabel · $workerId · $siteName",
                        onBack = onVolver
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    ResumenPresencia(estado = estado)

                    AnimatedVisibility(
                        visible = estado.isChecking,
                        enter = fadeIn() + slideInVertically { -it / 2 },
                        exit = fadeOut()
                    ) {
                        Column {
                            Spacer(modifier = Modifier.height(12.dp))
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(3.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    ResourceStatusCard(
                        icon = Icons.Outlined.Security,
                        eyebrow = "Paso 1",
                        title = "Permisos",
                        detail = estadoPermiso(estado.permissionState),
                        tone = tonoPermiso(estado.permissionState),
                        pillText = textoPermiso(estado.permissionState)
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    ResourceStatusCard(
                        icon = Icons.Outlined.LocationOn,
                        eyebrow = "Paso 2",
                        title = "Ubicación GPS",
                        detail = estado.locationMessage,
                        tone = tonoUbicacion(estado.location?.status),
                        pillText = textoUbicacion(estado.location?.status),
                        extra = estado.location?.let { ubicacion ->
                            if (ubicacion.status == LocationStatus.READY) {
                                val latitud = ubicacion.latitude ?: 0.0
                                val longitud = ubicacion.longitude ?: 0.0
                                "Lat ${String.format(Locale.ROOT, "%.5f", latitud)} · " +
                                    "Lon ${String.format(Locale.ROOT, "%.5f", longitud)}"
                            } else {
                                null
                            }
                        }
                    )

                    if (estado.location?.status == LocationStatus.READY) {
                        Spacer(modifier = Modifier.height(10.dp))

                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn() + scaleIn(initialScale = 0.96f)
                        ) {
                            MapboxLocationCard(
                                latitude = estado.location?.latitude,
                                longitude = estado.location?.longitude,
                                accuracyMeters = estado.location?.accuracyMeters
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    ResourceStatusCard(
                        icon = Icons.Outlined.Bluetooth,
                        eyebrow = "Paso 3",
                        title = "Baliza Bluetooth",
                        detail = estado.beaconMessage,
                        tone = tonoBaliza(estado.beacon?.status),
                        pillText = textoBaliza(estado.beacon?.status),
                        extra = estado.beacon?.let { baliza ->
                            if (baliza.status == BeaconScanStatus.FOUND) {
                                buildString {
                                    append(baliza.format ?: "BLE")
                                    append(" · ")
                                    append(baliza.displayId)
                                    append(" · RSSI ")
                                    append(baliza.rssi)
                                }
                            } else {
                                null
                            }
                        }
                    )

                    if (estado.mockWarning != null) {
                        Spacer(modifier = Modifier.height(10.dp))

                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.tertiaryContainer
                            ),
                            border = BorderStroke(
                                1.dp,
                                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.35f)
                            )
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
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = estado.mockWarning.orEmpty(),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    // El botón de verificar se muestra siempre. Si el permiso quedó bloqueado
                    // igual se puede volver a intentar después de concederlo en los ajustes.
                    Button(
                        onClick = {
                            val permisos = PermissionChecker.presencePermissions()
                            val faltantes = permisos.filterNot { permiso ->
                                PermissionChecker.isGranted(contexto, permiso)
                            }

                            if (faltantes.isEmpty()) {
                                presenciaViewModel.onPermisosResultado(
                                    concedidos = true,
                                    denegadoPermanente = false
                                )
                                presenciaViewModel.onVerificarPresencia()
                                solicitarNotificaciones()
                            } else {
                                lanzadorPermisos.launch(faltantes.toTypedArray())
                            }
                        },
                        enabled = !estado.isChecking,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        if (estado.isChecking) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text("Verificando presencia...")
                        } else {
                            Icon(
                                imageVector = Icons.Outlined.Refresh,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (estado.location == null && estado.beacon == null) {
                                    "Verificar presencia"
                                } else {
                                    "Verificar de nuevo"
                                },
                                fontSize = 16.sp
                            )
                        }
                    }

                    if (estado.needsSettings) {
                        Spacer(modifier = Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = {
                                val intent = Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", contexto.packageName, null)
                                )
                                contexto.startActivity(intent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.ErrorOutline,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Abrir ajustes de la app", fontSize = 15.sp)
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = presenciaViewModel::onRegistrarEntrada,
                        enabled = !estado.isSaving,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.secondary,
                            contentColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(60.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Login,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                estado.isSaving -> "Guardando..."
                                estado.alreadyCheckedIn -> "Entrada ya registrada"
                                else -> "Registrar entrada"
                            },
                            fontSize = 17.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedButton(
                        onClick = presenciaViewModel::onRegistrarSalida,
                        enabled = !estado.isSaving,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Logout,
                            contentDescription = null
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = when {
                                estado.isSaving -> "Guardando..."
                                estado.alreadyCheckedOut -> "Salida ya registrada"
                                else -> "Registrar salida"
                            },
                            fontSize = 16.sp
                        )
                    }

                    val ayuda = when {
                        !estado.canRegister ->
                            "Para registrar una marca se necesita ubicación lista y baliza detectada."
                        estado.alreadyCheckedIn && estado.alreadyCheckedOut ->
                            "La jornada de hoy ya tiene entrada y salida registradas."
                        estado.alreadyCheckedIn ->
                            "La entrada de hoy ya está registrada. Puedes registrar la salida."
                        else -> null
                    }

                    if (ayuda != null) {
                        Spacer(modifier = Modifier.height(10.dp))
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
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = ayuda,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))

                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        color = if (estado.pendingSyncCount == 0) {
                            MaterialTheme.colorScheme.surface
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        },
                        border = BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (estado.pendingSyncCount == 0) {
                                    Icons.Outlined.CloudDone
                                } else {
                                    Icons.Outlined.CloudOff
                                },
                                contentDescription = null,
                                tint = if (estado.pendingSyncCount == 0) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                }
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Sincronización",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = PresenceEvaluator.pendingSyncMessage(estado.pendingSyncCount),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                            StatusPill(
                                text = if (estado.pendingSyncCount == 0) "Al día" else "Pendiente",
                                tone = if (estado.pendingSyncCount == 0) {
                                    StatusTone.OK
                                } else {
                                    StatusTone.WARNING
                                }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = estado.message != null,
                        enter = fadeIn() + androidx.compose.animation.expandVertically(),
                        exit = fadeOut() + androidx.compose.animation.shrinkVertically()
                    ) {
                        Column(modifier = Modifier.animateContentSize()) {
                            Spacer(modifier = Modifier.height(14.dp))
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
                                        imageVector = if (
                                            estado.lastEvent != null &&
                                            estado.message?.contains("registrada") == true
                                        ) {
                                            Icons.Outlined.CheckCircle
                                        } else {
                                            Icons.Outlined.Info
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = estado.message.orEmpty(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onVerHistorial,
                            modifier = Modifier
                                .weight(1f)
                                .height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Historial")
                        }

                        TextButton(
                            onClick = onVolver,
                            modifier = Modifier.height(48.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Volver")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }

        AvisoDuplicado(
            notice = estado.duplicateNotice,
            onCerrar = presenciaViewModel::onDescartarDuplicado,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(horizontal = 16.dp, vertical = 76.dp)
        )
    }
}

/** Popup flotante que avisa que la marca del día ya existía. */
@Composable
private fun AvisoDuplicado(
    notice: DuplicateNotice?,
    onCerrar: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = notice != null,
        modifier = modifier,
        enter = fadeIn() + slideInVertically { -it } + scaleIn(initialScale = 0.94f),
        exit = fadeOut() + slideOutVertically { -it } + scaleOut(targetScale = 0.94f)
    ) {
        val datos = notice ?: return@AnimatedVisibility
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 520.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.tertiaryContainer
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.tertiary.copy(alpha = 0.5f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier.padding(start = 16.dp, top = 14.dp, bottom = 14.dp, end = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel(
                        text = datos.title,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = datos.detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
                IconButton(onClick = onCerrar) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "Cerrar aviso",
                        tint = MaterialTheme.colorScheme.onTertiaryContainer
                    )
                }
            }
        }
    }
}

@Composable
private fun ResumenPresencia(estado: PresenciaUiState) {
    val listo = estado.canRegister
    val container = if (listo) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant
    }
    val content = if (listo) {
        MaterialTheme.colorScheme.onSecondaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(containerColor = container),
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
                        if (listo) {
                            MaterialTheme.colorScheme.secondary
                        } else {
                            MaterialTheme.colorScheme.outline.copy(alpha = 0.25f)
                        },
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (listo) {
                        Icons.Outlined.CheckCircle
                    } else {
                        Icons.Outlined.LocationSearching
                    },
                    contentDescription = null,
                    tint = if (listo) {
                        MaterialTheme.colorScheme.onSecondary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                SectionLabel(
                    text = if (listo) "Presencia confirmada" else "Verificación pendiente",
                    color = content.copy(alpha = 0.75f)
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = if (listo) {
                        "Todo listo para marcar"
                    } else if (estado.isChecking) {
                        "Buscando GPS y baliza..."
                    } else {
                        "Verifica tu ubicación y baliza"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = content
                )
                Text(
                    text = if (listo) {
                        "Ubicación y proximidad detectadas en este dispositivo."
                    } else {
                        "Necesitas ambos recursos para registrar una marca."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = content.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun estadoPermiso(estado: PermissionStatus): String = when (estado) {
    PermissionStatus.NOT_REQUESTED -> "Todavía no se solicitaron los permisos necesarios."
    PermissionStatus.GRANTED -> "Permiso de ubicación concedido."
    PermissionStatus.DENIED -> "Permisos denegados. Se pueden volver a solicitar."
    PermissionStatus.PERMANENTLY_DENIED ->
        "Permisos bloqueados. Habilítalos desde los ajustes del sistema."
}

private fun textoPermiso(estado: PermissionStatus): String = when (estado) {
    PermissionStatus.GRANTED -> "Ubicación OK"
    PermissionStatus.NOT_REQUESTED -> "Pendiente"
    PermissionStatus.DENIED -> "Denegados"
    PermissionStatus.PERMANENTLY_DENIED -> "Bloqueados"
}

private fun tonoPermiso(estado: PermissionStatus): StatusTone = when (estado) {
    PermissionStatus.GRANTED -> StatusTone.OK
    PermissionStatus.NOT_REQUESTED -> StatusTone.NEUTRAL
    PermissionStatus.DENIED -> StatusTone.WARNING
    PermissionStatus.PERMANENTLY_DENIED -> StatusTone.DANGER
}

private fun textoUbicacion(estado: LocationStatus?): String = when (estado) {
    LocationStatus.READY -> "Listo"
    null -> "Sin datos"
    LocationStatus.PERMISSION_DENIED -> "Sin permiso"
    LocationStatus.LOCATION_DISABLED -> "GPS apagado"
    LocationStatus.TIMEOUT -> "Sin señal"
    LocationStatus.INSUFFICIENT_ACCURACY -> "Baja precisión"
    LocationStatus.UNAVAILABLE -> "No disponible"
    LocationStatus.ERROR -> "Error"
}

private fun tonoUbicacion(estado: LocationStatus?): StatusTone = when (estado) {
    LocationStatus.READY -> StatusTone.OK
    null -> StatusTone.NEUTRAL
    LocationStatus.PERMISSION_DENIED,
    LocationStatus.LOCATION_DISABLED,
    LocationStatus.INSUFFICIENT_ACCURACY -> StatusTone.WARNING
    LocationStatus.TIMEOUT,
    LocationStatus.UNAVAILABLE,
    LocationStatus.ERROR -> StatusTone.DANGER
}

private fun textoBaliza(estado: BeaconScanStatus?): String = when (estado) {
    BeaconScanStatus.FOUND -> "Detectada"
    null -> "Sin datos"
    BeaconScanStatus.NOT_FOUND -> "No encontrada"
    BeaconScanStatus.BLUETOOTH_OFF -> "Bluetooth off"
    BeaconScanStatus.TIMEOUT -> "Sin señal"
    BeaconScanStatus.PERMISSION_DENIED -> "Sin permiso"
    BeaconScanStatus.NOT_SUPPORTED -> "No soportado"
    BeaconScanStatus.ERROR -> "Error"
}

private fun tonoBaliza(estado: BeaconScanStatus?): StatusTone = when (estado) {
    BeaconScanStatus.FOUND -> StatusTone.OK
    null -> StatusTone.NEUTRAL
    BeaconScanStatus.NOT_FOUND,
    BeaconScanStatus.BLUETOOTH_OFF,
    BeaconScanStatus.TIMEOUT -> StatusTone.WARNING
    BeaconScanStatus.PERMISSION_DENIED,
    BeaconScanStatus.NOT_SUPPORTED,
    BeaconScanStatus.ERROR -> StatusTone.DANGER
}
