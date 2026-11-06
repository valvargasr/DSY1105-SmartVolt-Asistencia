package cl.smartvolt.asistencia.ui.view

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Groups
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Work
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import cl.smartvolt.asistencia.modelo.DemoAccount
import cl.smartvolt.asistencia.modelo.WorkerRole
import cl.smartvolt.asistencia.ui.components.SectionLabel
import cl.smartvolt.asistencia.ui.components.SmartVoltTopBar

@Composable
fun GestionView(
    cuenta: DemoAccount,
    onAbrirPanel: () -> Unit,
    onAbrirFaena: () -> Unit,
    onAbrirReporte: () -> Unit,
    onAbrirHorasExtra: () -> Unit,
    onCerrarSesion: () -> Unit
) {
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
                SmartVoltTopBar(
                    title = "Gestión",
                    subtitle = "Acciones para ${cuenta.roleLabel}"
                )

                Spacer(Modifier.height(16.dp))
                when (cuenta.role) {
                    WorkerRole.WORKER -> {
                        TarjetaAccion(
                            icon = Icons.Outlined.Timer,
                            titulo = "Horas extra",
                            detalle = "Envía una solicitud y revisa su estado.",
                            onClick = onAbrirHorasExtra
                        )
                    }

                    WorkerRole.SUPERVISOR -> {
                        TarjetaAccion(
                            icon = Icons.Outlined.Groups,
                            titulo = "Panel de faena",
                            detalle = "Presentes, salidas y marcas del día.",
                            onClick = onAbrirPanel
                        )
                    }

                    WorkerRole.ADMIN -> {
                        TarjetaAccion(
                            icon = Icons.Outlined.Groups,
                            titulo = "Panel de faena",
                            detalle = "Presentes, salidas y marcas del día.",
                            onClick = onAbrirPanel
                        )
                        TarjetaAccion(
                            icon = Icons.Outlined.Edit,
                            titulo = "Faena",
                            detalle = "Nombre, centro, radio, tarifa y recargo.",
                            onClick = onAbrirFaena
                        )
                        TarjetaAccion(
                            icon = Icons.Outlined.Description,
                            titulo = "Registro del mes",
                            detalle = "Horas, montos, CSV y PDF.",
                            onClick = onAbrirReporte
                        )
                        TarjetaAccion(
                            icon = Icons.Outlined.Timer,
                            titulo = "Horas extra",
                            detalle = "Revisa, aprueba o rechaza solicitudes.",
                            onClick = onAbrirHorasExtra
                        )
                    }

                    else -> Text(
                        text = "Este rol no tiene acciones adicionales en la demo.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(Modifier.height(16.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(modifier = Modifier.padding(17.dp)) {
                        SectionLabel(text = "Sesión de demostración")
                        Spacer(Modifier.height(10.dp))
                        FilaDato(Icons.Outlined.Person, cuenta.displayName)
                        FilaDato(Icons.Outlined.Badge, cuenta.workerId)
                        FilaDato(Icons.Outlined.Work, cuenta.position)
                        FilaDato(Icons.Outlined.LocationOn, cuenta.siteName)
                        FilaDato(Icons.Outlined.Schedule, cuenta.shiftLabel)
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 10.dp),
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        Text(
                            text = "Datos ficticios. Sin backend ni datos personales reales.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
                OutlinedButton(
                    onClick = onCerrarSesion,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Cerrar sesión")
                }
                Spacer(Modifier.height(20.dp))
            }
        }
    }
}

@Composable
private fun TarjetaAccion(
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
