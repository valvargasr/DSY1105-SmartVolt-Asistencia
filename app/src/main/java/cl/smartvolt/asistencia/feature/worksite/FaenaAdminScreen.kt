package cl.smartvolt.asistencia.feature.worksite

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import cl.smartvolt.asistencia.ui.components.MapboxLocationCard
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar

/**
 * Pantalla del administrador para cambiar el lugar de la faena.
 *
 * Nombre y dirección se muestran en la app; las coordenadas y el radio
 * definen la zona donde se permite registrar una marca.
 */
@Composable
fun FaenaAdminScreen(
    viewModel: FaenaAdminViewModel,
    onVolver: () -> Unit,
    onGuardado: () -> Unit
) {
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado.guardado) {
        if (estado.guardado) onGuardado()
    }

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            SmartVoltTopBar(
                title = "Faena",
                subtitle = "Administración del lugar de marcación",
                onBack = onVolver
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "El perímetro define dónde pueden marcar los trabajadores. " +
                    "Si alguien está fuera del radio, la app no registra la marca.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            if (estado.cargando) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    horizontalArrangement = Arrangement.Center
                ) {
                    CircularProgressIndicator()
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        OutlinedTextField(
                            value = estado.nombre,
                            onValueChange = viewModel::onNombreChange,
                            label = { Text("Nombre de la faena") },
                            placeholder = { Text("Ej.: Faena Andes") },
                            singleLine = true,
                            isError = estado.errores.name.isNotEmpty(),
                            supportingText = {
                                if (estado.errores.name.isNotEmpty()) Text(estado.errores.name)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = estado.direccion,
                            onValueChange = viewModel::onDireccionChange,
                            label = { Text("Dirección") },
                            placeholder = { Text("Ej.: Camino a Farellones km 12") },
                            singleLine = true,
                            isError = estado.errores.address.isNotEmpty(),
                            supportingText = {
                                if (estado.errores.address.isNotEmpty()) Text(estado.errores.address)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = estado.radio,
                            onValueChange = viewModel::onRadioChange,
                            label = { Text("Radio permitido (metros)") },
                            placeholder = { Text("300") },
                            singleLine = true,
                            isError = estado.errores.radiusMeters.isNotEmpty(),
                            supportingText = {
                                if (estado.errores.radiusMeters.isNotEmpty()) {
                                    Text(estado.errores.radiusMeters)
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = estado.latitud,
                                onValueChange = viewModel::onLatitudChange,
                                label = { Text("Latitud") },
                                placeholder = { Text("-33.350000") },
                                singleLine = true,
                                isError = estado.errores.latitude.isNotEmpty(),
                                supportingText = {
                                    if (estado.errores.latitude.isNotEmpty()) {
                                        Text(estado.errores.latitude)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                            OutlinedTextField(
                                value = estado.longitud,
                                onValueChange = viewModel::onLongitudChange,
                                label = { Text("Longitud") },
                                placeholder = { Text("-70.520000") },
                                singleLine = true,
                                isError = estado.errores.longitude.isNotEmpty(),
                                supportingText = {
                                    if (estado.errores.longitude.isNotEmpty()) {
                                        Text(estado.errores.longitude)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        OutlinedButton(
                            onClick = viewModel::onUsarUbicacionActual,
                            enabled = !estado.ubicacionEnCurso,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.LocationOn,
                                contentDescription = null
                            )
                            Text(
                                text = if (estado.ubicacionEnCurso) {
                                    "Leyendo la ubicación..."
                                } else {
                                    "Usar mi ubicación actual"
                                },
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        MapboxLocationCard(
                            latitude = estado.latitudPreview,
                            longitude = estado.longitudPreview,
                            accuracyMeters = null
                        )
                    }
                }

                estado.mensaje?.let { texto ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = texto,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(20.dp))
                Button(
                    onClick = viewModel::onGuardar,
                    enabled = !estado.guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(
                        text = if (estado.guardando) "Guardando..." else "Guardar faena",
                        style = MaterialTheme.typography.titleSmall
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "El cambio se aplica a las marcas nuevas. Los registros anteriores " +
                        "conservan la faena con la que se guardaron.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
            }
        }
    }
}
