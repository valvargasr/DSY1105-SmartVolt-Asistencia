package cl.smartvolt.asistencia.feature.attendance

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.data.local.LocalAttendanceEvent
import cl.smartvolt.asistencia.native.bluetooth.BeaconScanStatus
import cl.smartvolt.asistencia.native.location.LocationStatus
import cl.smartvolt.asistencia.native.permissions.PermissionChecker
import cl.smartvolt.asistencia.native.permissions.PermissionStatus

@Composable
fun PresenciaScreen(
    onVolver: () -> Unit = {},
    presenciaViewModel: PresenciaViewModel = viewModel(factory = PresenciaViewModel.Factory)
) {
    val estado by presenciaViewModel.uiState.collectAsState()
    val contexto = LocalContext.current
    val actividad = contexto as? Activity

    val lanzadorPermisos = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { resultados ->
        val concedidos = resultados.values.all { it }
        val denegadoPermanente = resultados
            .filterValues { concedido -> !concedido }
            .keys
            .any { permiso ->
                actividad?.shouldShowRequestPermissionRationale(permiso) == false
            }

        presenciaViewModel.onPermisosResultado(concedidos, denegadoPermanente)

        if (concedidos) {
            presenciaViewModel.onVerificarPresencia()
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
            ) {
                Text(
                    text = "Registrar entrada",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Faena ${LocalAttendanceEvent.SITE_ID} · Operario ${LocalAttendanceEvent.WORKER_ID}",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(20.dp))

                TarjetaIndicador(
                    titulo = "1. Permisos",
                    detalle = estadoPermiso(estado.permissionState),
                    color = colorPermiso(estado.permissionState)
                )

                Spacer(modifier = Modifier.height(12.dp))

                TarjetaIndicador(
                    titulo = "2. Ubicación GPS",
                    detalle = estado.locationMessage,
                    color = colorUbicacion(estado.location?.status),
                    extra = estado.location?.let { ubicacion ->
                        if (ubicacion.status == LocationStatus.READY) {
                            val latitud = ubicacion.latitude ?: 0.0
                            val longitud = ubicacion.longitude ?: 0.0
                            "Lat ${"%.5f".format(latitud)} · Lon ${"%.5f".format(longitud)}"
                        } else {
                            null
                        }
                    }
                )

                Spacer(modifier = Modifier.height(12.dp))

                TarjetaIndicador(
                    titulo = "3. Baliza Bluetooth",
                    detalle = estado.beaconMessage,
                    color = colorBaliza(estado.beacon?.status),
                    extra = estado.beacon?.beaconId?.let { identificador ->
                        if (estado.beacon?.status == BeaconScanStatus.FOUND) {
                            "UUID de prueba: $identificador"
                        } else {
                            null
                        }
                    }
                )

                if (estado.mockWarning != null) {
                    Spacer(modifier = Modifier.height(12.dp))

                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer
                        )
                    ) {
                        Text(
                            text = estado.mockWarning.orEmpty(),
                            modifier = Modifier.padding(14.dp),
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                if (!estado.needsSettings) {
                    Button(
                        onClick = {
                            val permisos = PermissionChecker.presencePermissions()
                            val yaConcedidos = permisos.all { permiso ->
                                PermissionChecker.isGranted(contexto, permiso)
                            }

                            if (yaConcedidos) {
                                presenciaViewModel.onVerificarPresencia()
                            } else {
                                lanzadorPermisos.launch(permisos)
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
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Verificando presencia...")
                        } else {
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
                } else {
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
                            .height(54.dp)
                    ) {
                        Text("Abrir ajustes de la app", fontSize = 16.sp)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = presenciaViewModel::onRegistrarEntrada,
                    enabled = estado.canRegister && !estado.isSaving,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (estado.isSaving) {
                            "Guardando..."
                        } else {
                            "Registrar entrada"
                        },
                        fontSize = 16.sp
                    )
                }

                if (!estado.canRegister) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Para registrar la entrada se necesita ubicación lista y baliza detectada.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            shape = MaterialTheme.shapes.medium
                        )
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (estado.pendingSyncCount == 0) {
                                    Color(0xFF2E7D32)
                                } else {
                                    Color(0xFFB26A00)
                                },
                                shape = CircleShape
                            )
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = PresenceEvaluator.pendingSyncMessage(estado.pendingSyncCount),
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                AnimatedVisibility(visible = estado.message != null) {
                    Column {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text(
                                text = estado.message.orEmpty(),
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedButton(
                    onClick = onVolver,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Volver")
                }

                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun TarjetaIndicador(
    titulo: String,
    detalle: String,
    color: Color,
    extra: String? = null
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(color = color, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = titulo,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = detalle,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (extra != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = extra,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

private fun estadoPermiso(estado: PermissionStatus): String = when (estado) {
    PermissionStatus.NOT_REQUESTED -> "Todavía no se solicitaron los permisos necesarios."
    PermissionStatus.GRANTED -> "Permisos concedidos."
    PermissionStatus.DENIED -> "Permisos denegados. Se pueden volver a solicitar."
    PermissionStatus.PERMANENTLY_DENIED ->
        "Permisos bloqueados. Habilítalos desde los ajustes del sistema."
}

private fun colorPermiso(estado: PermissionStatus): Color = when (estado) {
    PermissionStatus.GRANTED -> Color(0xFF2E7D32)
    PermissionStatus.NOT_REQUESTED -> Color(0xFF6B6B6B)
    PermissionStatus.DENIED -> Color(0xFFB26A00)
    PermissionStatus.PERMANENTLY_DENIED -> Color(0xFFB3261E)
}

private fun colorUbicacion(estado: LocationStatus?): Color = when (estado) {
    LocationStatus.READY -> Color(0xFF2E7D32)
    null -> Color(0xFF6B6B6B)
    LocationStatus.PERMISSION_DENIED,
    LocationStatus.LOCATION_DISABLED -> Color(0xFFB26A00)
    LocationStatus.INSUFFICIENT_ACCURACY -> Color(0xFFB26A00)
    LocationStatus.TIMEOUT,
    LocationStatus.UNAVAILABLE,
    LocationStatus.ERROR -> Color(0xFFB3261E)
}

private fun colorBaliza(estado: BeaconScanStatus?): Color = when (estado) {
    BeaconScanStatus.FOUND -> Color(0xFF2E7D32)
    null -> Color(0xFF6B6B6B)
    BeaconScanStatus.NOT_FOUND,
    BeaconScanStatus.BLUETOOTH_OFF,
    BeaconScanStatus.TIMEOUT -> Color(0xFFB26A00)
    BeaconScanStatus.PERMISSION_DENIED,
    BeaconScanStatus.NOT_SUPPORTED,
    BeaconScanStatus.ERROR -> Color(0xFFB3261E)
}
