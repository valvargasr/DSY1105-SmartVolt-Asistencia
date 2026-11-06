package cl.smartvolt.asistencia.ui.view

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
import androidx.lifecycle.viewmodel.compose.viewModel
import cl.smartvolt.asistencia.ui.components.MapaUbicacion
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar
import cl.smartvolt.asistencia.viewmodel.FaenaViewModel

@Composable
// Pantalla para editar el nombre, el radio y el centro de la faena
fun FaenaView(
    onVolver: () -> Unit,
    onGuardado: () -> Unit,
    viewModel: FaenaViewModel = viewModel()
) {
    // Estado del formulario
    val estado by viewModel.uiState.collectAsState()

    LaunchedEffect(estado.guardado) {
        // cuando se guarda volvemos atrás sin marear con otro mensaje
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
            // ENCABEZADO
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

            // CARGANDO
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
                // CAMPOS
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        // NOMBRE
                        OutlinedTextField(
                            value = estado.nombre,
                            onValueChange = viewModel::onNombre,
                            label = { Text("Nombre de la faena") },
                            singleLine = true,
                            isError = estado.errores.name.isNotEmpty(),
                            supportingText = {
                                if (estado.errores.name.isNotEmpty()) Text(estado.errores.name)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        // DIRECCION
                        OutlinedTextField(
                            value = estado.direccion,
                            onValueChange = viewModel::onDireccion,
                            label = { Text("Dirección") },
                            singleLine = true,
                            isError = estado.errores.address.isNotEmpty(),
                            supportingText = {
                                if (estado.errores.address.isNotEmpty()) Text(estado.errores.address)
                            },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(Modifier.height(12.dp))
                        // RADIO
                        OutlinedTextField(
                            value = estado.radio,
                            onValueChange = viewModel::onRadio,
                            label = { Text("Radio permitido (metros)") },
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
                        // COORDENADAS
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = estado.latitud,
                                onValueChange = viewModel::onLatitud,
                                label = { Text("Latitud") },
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
                                onValueChange = viewModel::onLongitud,
                                label = { Text("Longitud") },
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
                            onClick = viewModel::usarUbicacionActual,
                            enabled = !estado.ubicacionEnCurso,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Outlined.LocationOn, contentDescription = null)
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
                        // MAPA
                        MapaUbicacion(
                            latitude = estado.latitudPreview,
                            longitude = estado.longitudPreview,
                            accuracyMeters = null
                        )
                    }
                }

                // MENSAJE
                estado.mensaje?.let { texto ->
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = texto,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(20.dp))
                // BOTON GUARDAR
                Button(
                    onClick = viewModel::guardar,
                    enabled = !estado.guardando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Text(if (estado.guardando) "Guardando..." else "Guardar faena")
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
